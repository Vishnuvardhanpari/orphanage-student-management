package com.orphanage.oms.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orphanage.oms.auth.repository.RefreshTokenRepository;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import com.orphanage.oms.student.repository.StudentRepository;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import com.orphanage.oms.user.repository.RoleRepository;
import com.orphanage.oms.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM student_documents");
        jdbcTemplate.update("DELETE FROM students");
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        var adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        userRepository.save(User.builder()
                .username("admin")
                .email("admin@oms.local")
                .passwordHash(passwordEncoder.encode("ChangeMeAdmin123!"))
                .authProvider(AuthProvider.LOCAL)
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build());

        var staffRole = roleRepository.findByName(RoleName.STAFF).orElseThrow();
        userRepository.save(User.builder()
                .username("staff")
                .email("staff@oms.local")
                .passwordHash(passwordEncoder.encode("StaffPass123!"))
                .authProvider(AuthProvider.LOCAL)
                .role(staffRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build());

        adminToken = loginAccessToken("admin", "ChangeMeAdmin123!");
        staffToken = loginAccessToken("staff", "StaffPass123!");
    }

    @Test
    void unauthenticatedDashboardRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/admissions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/gender")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/status")).andExpect(status().isUnauthorized());
    }

    @Test
    void emptyDatabaseReturnsZeros() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(0))
                .andExpect(jsonPath("$.activeStudents").value(0))
                .andExpect(jsonPath("$.inactiveStudents").value(0))
                .andExpect(jsonPath("$.newAdmissions").value(0))
                .andExpect(jsonPath("$.maleStudents").value(0))
                .andExpect(jsonPath("$.femaleStudents").value(0))
                .andExpect(jsonPath("$.recentAdmissions").isEmpty())
                .andExpect(jsonPath("$.recentUpdates").isEmpty());

        mockMvc.perform(get("/api/v1/dashboard/admissions")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].count").value(0))
                .andExpect(jsonPath("$[11].count").value(0));

        mockMvc.perform(get("/api/v1/dashboard/gender")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].gender").value("MALE"))
                .andExpect(jsonPath("$[0].count").value(0));

        mockMvc.perform(get("/api/v1/dashboard/status")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].count").value(0))
                .andExpect(jsonPath("$[1].status").value("INACTIVE"))
                .andExpect(jsonPath("$[1].count").value(0));
    }

    @Test
    void summaryAndChartsReflectMixedActiveInactiveAndGender() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth currentMonth = YearMonth.from(today);

        persistStudent(b -> b.admissionNumber("ADM-D-001")
                .firstName("MaleActive")
                .gender(Gender.MALE)
                .admissionDate(today.withDayOfMonth(1)));
        persistStudent(b -> b.admissionNumber("ADM-D-002")
                .firstName("FemaleActive")
                .gender(Gender.FEMALE)
                .admissionDate(today.minusMonths(2).withDayOfMonth(1)));
        persistStudent(b -> b.admissionNumber("ADM-D-003")
                .firstName("OtherActive")
                .gender(Gender.OTHER)
                .admissionDate(currentMonth.minusMonths(1).atDay(1)));

        UUID leftId = persistStudent(b -> b.admissionNumber("ADM-D-004")
                .firstName("LeftStudent")
                .gender(Gender.FEMALE)
                .admissionDate(today.withDayOfMonth(Math.min(15, today.lengthOfMonth()))));

        mockMvc.perform(delete("/api/v1/students/" + leftId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(4))
                .andExpect(jsonPath("$.activeStudents").value(3))
                .andExpect(jsonPath("$.inactiveStudents").value(1))
                .andExpect(jsonPath("$.newAdmissions").value(2))
                .andExpect(jsonPath("$.maleStudents").value(1))
                .andExpect(jsonPath("$.femaleStudents").value(1))
                .andExpect(jsonPath("$.recentAdmissions.length()").value(3));

        mockMvc.perform(get("/api/v1/dashboard/gender")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.gender=='MALE')].count").value(1))
                .andExpect(jsonPath("$[?(@.gender=='FEMALE')].count").value(1))
                .andExpect(jsonPath("$[?(@.gender=='OTHER')].count").value(1));

        mockMvc.perform(get("/api/v1/dashboard/status")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.status=='ACTIVE')].count").value(3))
                .andExpect(jsonPath("$[?(@.status=='INACTIVE')].count").value(1));

        mockMvc.perform(get("/api/v1/dashboard/admissions")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[11].yearMonth").value(currentMonth.toString()))
                .andExpect(jsonPath("$[11].count").value(2));
    }

    private UUID persistStudent(Consumer<Student.StudentBuilder> customizer) {
        Student.StudentBuilder builder = Student.builder()
                .firstName("Student")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2014, 3, 15))
                .admissionDate(LocalDate.of(2024, 6, 1))
                .status(StudentStatus.ACTIVE)
                .deleted(false);
        customizer.accept(builder);
        return studentRepository.save(builder.build()).getId();
    }

    private String loginAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
