package com.orphanage.oms.student.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.security.UserPrincipal;
import com.orphanage.oms.storage.StorageService;
import com.orphanage.oms.student.dto.CreateStudentRequest;
import com.orphanage.oms.student.dto.SoftDeleteStudentRequest;
import com.orphanage.oms.student.dto.StudentCreatedResponse;
import com.orphanage.oms.student.dto.StudentDetailResponse;
import com.orphanage.oms.student.dto.StudentDocumentResponse;
import com.orphanage.oms.student.dto.StudentSummaryResponse;
import com.orphanage.oms.student.dto.StoredFilePayload;
import com.orphanage.oms.student.dto.UpdateStudentRequest;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.entity.StudentDocument;
import com.orphanage.oms.student.enums.DocumentType;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import com.orphanage.oms.student.mapper.StudentMapper;
import com.orphanage.oms.student.repository.StudentDocumentRepository;
import com.orphanage.oms.student.repository.StudentRepository;
import com.orphanage.oms.user.entity.Role;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private com.orphanage.oms.student.repository.StudentDeletedQuery studentDeletedQuery;
    @Mock
    private StudentDocumentRepository studentDocumentRepository;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private StudentFileValidator fileValidator;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private StudentService studentService;

    private UUID actorId;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
        Role staffRole = Role.builder().id(UUID.randomUUID()).name(RoleName.STAFF).description("Staff").build();
        User actor = User.builder()
                .id(actorId)
                .username("staff")
                .email("staff@oms.local")
                .passwordHash("hash")
                .authProvider(AuthProvider.LOCAL)
                .role(staffRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        UserPrincipal principal = new UserPrincipal(actor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listMapsMatchingStudentsToSummaries() {
        Student student = Student.builder()
                .id(UUID.randomUUID())
                .admissionNumber("ADM-100")
                .firstName("Anita")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2014, 3, 15))
                .admissionDate(LocalDate.of(2024, 6, 1))
                .status(StudentStatus.ACTIVE)
                .build();
        StudentSummaryResponse summary = new StudentSummaryResponse(
                student.getId(),
                "ADM-100",
                "Anita",
                null,
                Gender.FEMALE,
                LocalDate.of(2014, 3, 15),
                StudentStatus.ACTIVE,
                null,
                null,
                LocalDate.of(2024, 6, 1),
                null);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "admissionDate"));

        when(studentRepository.findAll(ArgumentMatchers.<Specification<Student>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(student), pageable, 1));
        when(studentMapper.toSummaryResponse(student)).thenReturn(summary);

        Page<StudentSummaryResponse> page = studentService.list(
                "  anita  ", null, Gender.FEMALE, StudentStatus.ACTIVE, 2024, " Green Valley ", 8, 14, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).containsExactly(summary);
    }

    @Test
    void listWithoutFiltersDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "firstName"));
        when(studentRepository.findAll(ArgumentMatchers.<Specification<Student>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<StudentSummaryResponse> page = studentService.list(
                "   ", null, null, null, null, "   ", null, null, pageable);

        assertThat(page.getTotalElements()).isZero();
        verify(studentRepository).findAll(ArgumentMatchers.<Specification<Student>>any(), eq(pageable));
    }

    @Test
    void listRejectsUnsupportedSortProperty() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("aadhaarNumber"));

        assertThatThrownBy(() -> studentService.list(
                        null, null, null, null, null, null, null, null, pageable))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unsupported sort property: aadhaarNumber");
        verify(studentRepository, never())
                .findAll(ArgumentMatchers.<Specification<Student>>any(), any(Pageable.class));
    }

    // Regression for QA BUG-002: schoolName/standard are grid-sortable columns
    // on the active list and must not be rejected by the sort whitelist.
    @Test
    void listAcceptsSchoolNameAndStandardSort() {
        Pageable schoolSort = PageRequest.of(0, 20, Sort.by("schoolName"));
        Pageable standardSort = PageRequest.of(0, 20, Sort.by("standard"));
        when(studentRepository.findAll(ArgumentMatchers.<Specification<Student>>any(), eq(schoolSort)))
                .thenReturn(new PageImpl<>(List.of(), schoolSort, 0));
        when(studentRepository.findAll(ArgumentMatchers.<Specification<Student>>any(), eq(standardSort)))
                .thenReturn(new PageImpl<>(List.of(), standardSort, 0));

        assertThat(studentService.list(null, null, null, null, null, null, null, null, schoolSort)
                .getTotalElements()).isZero();
        assertThat(studentService.list(null, null, null, null, null, null, null, null, standardSort)
                .getTotalElements()).isZero();
    }

    @Test
    void listRejectsNegativeAgeBounds() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> studentService.list(
                        null, null, null, null, null, null, -1, null, pageable))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Age filters must not be negative.");
        assertThatThrownBy(() -> studentService.list(
                        null, null, null, null, null, null, null, -3, pageable))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Age filters must not be negative.");
    }

    @Test
    void listRejectsAgeMinGreaterThanAgeMax() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> studentService.list(
                        null, null, null, null, null, null, 12, 8, pageable))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("ageMin must be less than or equal to ageMax.");
    }

    @Test
    void listExactAdmissionNumberDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 1);
        when(studentRepository.findAll(ArgumentMatchers.<Specification<Student>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(studentService
                        .list(null, "ADM-100", null, null, null, null, null, null, pageable)
                        .getTotalElements())
                .isZero();
        verify(studentRepository).findAll(ArgumentMatchers.<Specification<Student>>any(), eq(pageable));
    }

    @Test
    void createPersistsStudentWithoutFiles() {
        CreateStudentRequest request = sampleRequest("ADM-001", null);
        Student entity = Student.builder().build();
        UUID studentId = UUID.randomUUID();
        Instant created = Instant.parse("2026-01-01T00:00:00Z");

        when(studentRepository.existsByAdmissionNumberIgnoreCaseIncludingDeleted("ADM-001")).thenReturn(false);
        when(studentMapper.toEntity(request)).thenReturn(entity);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(studentId);
            }
            s.setCreatedDate(created);
            s.setUpdatedDate(created);
            return s;
        });
        when(studentMapper.toCreatedResponse(any(Student.class))).thenReturn(
                new StudentCreatedResponse(studentId, "ADM-001", StudentStatus.ACTIVE, created));

        StudentCreatedResponse response = studentService.create(request, null, null, null);

        assertThat(response.admissionNumber()).isEqualTo("ADM-001");
        assertThat(response.status()).isEqualTo(StudentStatus.ACTIVE);
        verify(storageService, never()).store(anyString(), anyString(), any(), anyLong());
        verify(studentDocumentRepository, never()).save(any());

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(actorId);
        assertThat(captor.getValue().getStatus()).isEqualTo(StudentStatus.ACTIVE);
    }

    @Test
    void createRejectsDuplicateAdmissionNumber() {
        CreateStudentRequest request = sampleRequest("ADM-001", null);
        when(studentRepository.existsByAdmissionNumberIgnoreCaseIncludingDeleted("ADM-001")).thenReturn(true);

        assertThatThrownBy(() -> studentService.create(request, null, List.of(), List.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Admission number");
    }

    @Test
    void createStoresPhotoAndDocuments() throws Exception {
        CreateStudentRequest request = sampleRequest("ADM-002", null);
        Student entity = Student.builder().build();
        UUID studentId = UUID.randomUUID();

        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MockMultipartFile document = new MockMultipartFile(
                "documents", "aadhaar.pdf", "application/pdf", "%PDF".getBytes());

        when(studentRepository.existsByAdmissionNumberIgnoreCaseIncludingDeleted("ADM-002")).thenReturn(false);
        when(studentMapper.toEntity(request)).thenReturn(entity);
        when(fileValidator.extensionOf("photo.jpg")).thenReturn("jpg");
        when(fileValidator.extensionOf("aadhaar.pdf")).thenReturn("pdf");
        when(storageService.store(anyString(), anyString(), any(), anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(studentId);
            }
            s.setCreatedDate(Instant.now());
            s.setUpdatedDate(Instant.now());
            return s;
        });
        when(studentDocumentRepository.save(any(StudentDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentMapper.toCreatedResponse(any(Student.class))).thenReturn(
                new StudentCreatedResponse(studentId, "ADM-002", StudentStatus.ACTIVE, Instant.now()));

        studentService.create(request, photo, List.of(document), List.of("AADHAAR_CARD"));

        verify(fileValidator).validatePhoto(photo);
        verify(fileValidator).validateDocument(document);
        verify(storageService).store(
                org.mockito.ArgumentMatchers.startsWith("student-documents/"),
                eq("image/jpeg"),
                any(),
                eq(3L));
        verify(storageService).store(
                org.mockito.ArgumentMatchers.contains("aadhaar_card-"),
                eq("application/pdf"),
                any(),
                eq(4L));
        verify(studentDocumentRepository).save(any(StudentDocument.class));
    }

    @Test
    void createRejectsDateOfBirthAfterAdmissionDate() {
        CreateStudentRequest request = new CreateStudentRequest(
                "ADM-004",
                "Ravi",
                "Kumar",
                Gender.MALE,
                LocalDate.of(2020, 1, 1),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2019, 1, 1));

        assertThatThrownBy(() -> studentService.create(request, null, List.of(), List.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Date of birth must be on or before admission date");
    }

    @Test
    void createRejectsMismatchedDocumentTypes() {
        CreateStudentRequest request = sampleRequest("ADM-003", null);
        MockMultipartFile document = new MockMultipartFile(
                "documents", "aadhaar.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> studentService.create(request, null, List.of(document), List.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("documentTypes");
    }

    @Test
    void getByIdReturnsDetail() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .admissionNumber("ADM-010")
                .firstName("Anita")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2014, 3, 15))
                .admissionDate(LocalDate.of(2024, 6, 1))
                .status(StudentStatus.ACTIVE)
                .profilePhotoPath("student-documents/" + studentId + "/profile-photo.jpg")
                .createdDate(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedDate(Instant.parse("2026-01-02T00:00:00Z"))
                .build();
        StudentDetailResponse detail = new StudentDetailResponse(
                studentId,
                "ADM-010",
                "Anita",
                null,
                Gender.FEMALE,
                LocalDate.of(2014, 3, 15),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2024, 6, 1),
                null,
                null,
                null,
                StudentStatus.ACTIVE,
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"));

        when(studentRepository.findIncludingDeletedById(studentId)).thenReturn(Optional.of(student));
        when(studentMapper.toDetailResponse(student)).thenReturn(detail);

        assertThat(studentService.getById(studentId).hasProfilePhoto()).isTrue();
        assertThat(studentService.getById(studentId).admissionNumber()).isEqualTo("ADM-010");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findIncludingDeletedById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getById(studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void listDocumentsRequiresStudent() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findIncludingDeletedById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.listDocuments(studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void listDocumentsMapsActiveDocuments() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder().id(studentId).admissionNumber("ADM-011").build();
        StudentDocument document = StudentDocument.builder()
                .id(UUID.randomUUID())
                .student(student)
                .documentType(DocumentType.AADHAAR_CARD)
                .originalFileName("aadhaar.pdf")
                .storedFileName("aadhaar.pdf")
                .storagePath("student-documents/" + studentId + "/aadhaar.pdf")
                .contentType("application/pdf")
                .fileSize(10L)
                .uploadedDate(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        StudentDocumentResponse mapped = new StudentDocumentResponse(
                document.getId(),
                DocumentType.AADHAAR_CARD,
                "aadhaar.pdf",
                "application/pdf",
                10L,
                Instant.parse("2026-01-01T00:00:00Z"));

        when(studentRepository.findIncludingDeletedById(studentId)).thenReturn(Optional.of(student));
        when(studentDocumentRepository.findByStudent_IdOrderByUploadedDateDesc(studentId))
                .thenReturn(List.of(document));
        when(studentMapper.toDocumentResponse(document)).thenReturn(mapped);

        assertThat(studentService.listDocuments(studentId)).containsExactly(mapped);
    }

    @Test
    void downloadDocumentThrowsWhenOwnershipMismatch() {
        UUID studentId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(studentRepository.findIncludingDeletedById(studentId))
                .thenReturn(Optional.of(Student.builder().id(studentId).build()));
        when(studentDocumentRepository.findByIdAndStudent_Id(documentId, studentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.downloadDocument(studentId, documentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void downloadDocumentReturnsPayload() {
        UUID studentId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Student student = Student.builder().id(studentId).build();
        StudentDocument document = StudentDocument.builder()
                .id(documentId)
                .student(student)
                .documentType(DocumentType.BIRTH_CERTIFICATE)
                .originalFileName("birth.pdf")
                .storedFileName("birth.pdf")
                .storagePath("student-documents/" + studentId + "/birth.pdf")
                .contentType("application/pdf")
                .fileSize(4L)
                .build();
        byte[] bytes = "%PDF".getBytes();

        when(studentRepository.findIncludingDeletedById(studentId)).thenReturn(Optional.of(student));
        when(studentDocumentRepository.findByIdAndStudent_Id(documentId, studentId))
                .thenReturn(Optional.of(document));
        when(storageService.load(document.getStoragePath()))
                .thenReturn(new ByteArrayInputStream(bytes));

        StoredFilePayload payload = studentService.downloadDocument(studentId, documentId);
        assertThat(payload.fileName()).isEqualTo("birth.pdf");
        assertThat(payload.contentType()).isEqualTo("application/pdf");
        assertThat(payload.contentLength()).isEqualTo(4L);
    }

    @Test
    void loadProfilePhotoThrowsWhenMissing() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findIncludingDeletedById(studentId))
                .thenReturn(Optional.of(Student.builder().id(studentId).profilePhotoPath(null).build()));

        assertThatThrownBy(() -> studentService.loadProfilePhoto(studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Profile photo not found");
    }

    @Test
    void loadProfilePhotoReturnsJpegPayload() {
        UUID studentId = UUID.randomUUID();
        String path = "student-documents/" + studentId + "/profile-photo.jpg";
        Student student = Student.builder().id(studentId).profilePhotoPath(path).build();
        byte[] bytes = new byte[] {(byte) 0xFF, (byte) 0xD8};

        when(studentRepository.findIncludingDeletedById(studentId)).thenReturn(Optional.of(student));
        when(storageService.load(path)).thenReturn(new ByteArrayInputStream(bytes));

        StoredFilePayload payload = studentService.loadProfilePhoto(studentId);
        assertThat(payload.contentType()).isEqualTo("image/jpeg");
        assertThat(payload.fileName()).isEqualTo("profile-photo.jpg");
    }

    @Test
    void softDeleteSetsFlagsAndInactiveStatus() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .admissionNumber("ADM-DEL")
                .status(StudentStatus.ACTIVE)
                .deleted(false)
                .build();
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        studentService.softDelete(studentId, null);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        Student saved = captor.getValue();
        assertThat(saved.isDeleted()).isTrue();
        assertThat(saved.getDeletedBy()).isEqualTo(actorId);
        assertThat(saved.getDeletedDate()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(StudentStatus.INACTIVE);
        assertThat(saved.getExitDate()).isNull();
        assertThat(saved.getExitReason()).isNull();
    }

    // Regression for QA BUG-005: soft delete must record optional exit details
    // when the caller supplies them, without requiring them.
    @Test
    void softDeleteRecordsExitDetailsWhenProvided() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .admissionNumber("ADM-DEL-2")
                .status(StudentStatus.ACTIVE)
                .admissionDate(LocalDate.of(2024, 6, 1))
                .deleted(false)
                .build();
        SoftDeleteStudentRequest exitDetails = new SoftDeleteStudentRequest(
                LocalDate.of(2026, 1, 10), "Family relocated", "Handed over to guardian");
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        studentService.softDelete(studentId, exitDetails);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        Student saved = captor.getValue();
        assertThat(saved.isDeleted()).isTrue();
        assertThat(saved.getExitDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(saved.getExitReason()).isEqualTo("Family relocated");
        assertThat(saved.getExitRemarks()).isEqualTo("Handed over to guardian");
    }

    @Test
    void softDeleteRejectsExitDateBeforeAdmissionDate() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .admissionNumber("ADM-DEL-3")
                .status(StudentStatus.ACTIVE)
                .admissionDate(LocalDate.of(2024, 6, 1))
                .deleted(false)
                .build();
        SoftDeleteStudentRequest exitDetails =
                new SoftDeleteStudentRequest(LocalDate.of(2023, 1, 1), null, null);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studentService.softDelete(studentId, exitDetails))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Exit date must be on or after the admission date");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void softDeleteThrowsWhenAlreadyDeleted() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.softDelete(studentId, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void restoreClearsFlagsAndSetsActive() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .admissionNumber("ADM-RES")
                .status(StudentStatus.INACTIVE)
                .deleted(true)
                .deletedBy(actorId)
                .deletedDate(Instant.parse("2026-01-01T00:00:00Z"))
                .exitDate(LocalDate.of(2025, 12, 1))
                .exitReason("Left")
                .build();
        StudentDetailResponse detail = new StudentDetailResponse(
                studentId,
                "ADM-RES",
                "Anita",
                null,
                Gender.FEMALE,
                LocalDate.of(2014, 3, 15),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2025, 12, 1),
                "Left",
                null,
                StudentStatus.ACTIVE,
                false,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"));

        when(studentRepository.findIncludingDeletedById(studentId)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentMapper.toDetailResponse(any(Student.class))).thenReturn(detail);

        StudentDetailResponse response = studentService.restore(studentId);

        assertThat(response.status()).isEqualTo(StudentStatus.ACTIVE);
        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        Student saved = captor.getValue();
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getDeletedBy()).isNull();
        assertThat(saved.getDeletedDate()).isNull();
        assertThat(saved.getStatus()).isEqualTo(StudentStatus.ACTIVE);
        assertThat(saved.getExitDate()).isEqualTo(LocalDate.of(2025, 12, 1));
        assertThat(saved.getExitReason()).isEqualTo("Left");
    }

    @Test
    void restoreThrowsConflictWhenNotDeleted() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .deleted(false)
                .status(StudentStatus.ACTIVE)
                .build();
        when(studentRepository.findIncludingDeletedById(studentId)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studentService.restore(studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not archived");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void listInactiveMapsDeletedStudents() {
        Student student = Student.builder()
                .id(UUID.randomUUID())
                .admissionNumber("ADM-IN")
                .firstName("Ghost")
                .gender(Gender.OTHER)
                .dateOfBirth(LocalDate.of(2014, 1, 1))
                .admissionDate(LocalDate.of(2024, 1, 1))
                .status(StudentStatus.INACTIVE)
                .deleted(true)
                .build();
        StudentSummaryResponse summary = new StudentSummaryResponse(
                student.getId(),
                "ADM-IN",
                "Ghost",
                null,
                Gender.OTHER,
                LocalDate.of(2014, 1, 1),
                StudentStatus.INACTIVE,
                null,
                null,
                LocalDate.of(2024, 1, 1),
                Instant.parse("2026-01-05T00:00:00Z"));
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "deletedDate"));

        when(studentDeletedQuery.findDeletedMatching(
                        eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(student), pageable, 1));
        when(studentMapper.toSummaryResponse(student)).thenReturn(summary);

        Page<StudentSummaryResponse> page =
                studentService.listInactive(null, null, null, null, null, null, pageable);

        assertThat(page.getContent()).containsExactly(summary);
    }

    // Regression for QA BUG-002: sorting the archived list by School used to
    // 400 because schoolName was missing from the inactive-list sort whitelist.
    @Test
    void listInactiveAcceptsSchoolNameSort() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "schoolName"));
        when(studentDeletedQuery.findDeletedMatching(
                        eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(studentService
                        .listInactive(null, null, null, null, null, null, pageable)
                        .getTotalElements())
                .isZero();
    }

    @Test
    void listInactiveRejectsUnsupportedSort() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("aadhaarNumber"));

        assertThatThrownBy(
                        () -> studentService.listInactive(null, null, null, null, null, null, pageable))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unsupported sort property: aadhaarNumber");
        verify(studentDeletedQuery, never())
                .findDeletedMatching(
                        org.mockito.ArgumentMatchers.<String>any(),
                        org.mockito.ArgumentMatchers.nullable(Gender.class),
                        org.mockito.ArgumentMatchers.nullable(Integer.class),
                        org.mockito.ArgumentMatchers.<String>any(),
                        org.mockito.ArgumentMatchers.nullable(LocalDate.class),
                        org.mockito.ArgumentMatchers.nullable(LocalDate.class),
                        any(Pageable.class));
    }

    @Test
    void updateAppliesFieldsAndAudit() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .admissionNumber("ADM-020")
                .firstName("Old")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(2015, 5, 1))
                .admissionDate(LocalDate.of(2024, 6, 1))
                .status(StudentStatus.ACTIVE)
                .build();
        UpdateStudentRequest request = sampleUpdateRequest("123456789012");
        StudentDetailResponse detail = new StudentDetailResponse(
                studentId,
                "ADM-020",
                "Ravi",
                "Kumar",
                Gender.MALE,
                LocalDate.of(2015, 5, 1),
                null,
                null,
                null,
                "123456789012",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2024, 6, 1),
                null,
                null,
                null,
                StudentStatus.ACTIVE,
                false,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"));

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentRepository.existsByAadhaarNumberIncludingDeletedExcludingId("123456789012", studentId))
                .thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentMapper.toDetailResponse(any(Student.class))).thenReturn(detail);

        StudentDetailResponse response = studentService.update(studentId, request);

        assertThat(response.firstName()).isEqualTo("Ravi");
        assertThat(student.getAdmissionNumber()).isEqualTo("ADM-020");
        assertThat(student.getUpdatedBy()).isEqualTo(actorId);
        verify(studentMapper).updateFromDto(request, student);
    }

    @Test
    void updateRejectsDuplicateAadhaarOnAnotherStudent() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder().id(studentId).admissionNumber("ADM-021").build();
        UpdateStudentRequest request = sampleUpdateRequest("123456789012");

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentRepository.existsByAadhaarNumberIncludingDeletedExcludingId("123456789012", studentId))
                .thenReturn(true);

        assertThatThrownBy(() -> studentService.update(studentId, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Aadhaar number");
    }

    @Test
    void updateRejectsDateOfBirthAfterAdmissionDate() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(Student.builder().id(studentId).build()));

        UpdateStudentRequest request = new UpdateStudentRequest(
                "Ravi",
                "Kumar",
                Gender.MALE,
                LocalDate.of(2020, 1, 1),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2019, 1, 1));

        assertThatThrownBy(() -> studentService.update(studentId, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Date of birth must be on or before admission date");
    }

    @Test
    void replacePhotoStoresNewAndDeletesOld() throws Exception {
        UUID studentId = UUID.randomUUID();
        String oldPath = "student-documents/" + studentId + "/profile-photo.jpg";
        Student student = Student.builder()
                .id(studentId)
                .profilePhotoPath(oldPath)
                .build();
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "new.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(fileValidator.extensionOf("new.png")).thenReturn("png");
        when(storageService.store(anyString(), anyString(), any(), anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.replacePhoto(studentId, photo);

        verify(fileValidator).validatePhoto(photo);
        verify(storageService).store(
                eq("student-documents/" + studentId + "/profile-photo.png"),
                eq("image/png"),
                any(),
                eq(4L));
        verify(storageService).delete(oldPath);
        assertThat(student.getProfilePhotoPath())
                .isEqualTo("student-documents/" + studentId + "/profile-photo.png");
        assertThat(student.getUpdatedBy()).isEqualTo(actorId);
    }

    @Test
    void replacePhotoRejectsMissingFile() {
        UUID studentId = UUID.randomUUID();
        assertThatThrownBy(() -> studentService.replacePhoto(studentId, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Photo is required");
    }

    @Test
    void addDocumentsCreatesMetadata() throws Exception {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder().id(studentId).build();
        MockMultipartFile document = new MockMultipartFile(
                "documents", "birth.pdf", "application/pdf", "%PDF".getBytes());
        StudentDocumentResponse mapped = new StudentDocumentResponse(
                UUID.randomUUID(),
                DocumentType.BIRTH_CERTIFICATE,
                "birth.pdf",
                "application/pdf",
                4L,
                Instant.now());

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(fileValidator.extensionOf("birth.pdf")).thenReturn("pdf");
        when(storageService.store(anyString(), anyString(), any(), anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentDocumentRepository.save(any(StudentDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentMapper.toDocumentResponse(any(StudentDocument.class))).thenReturn(mapped);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<StudentDocumentResponse> result = studentService.addDocuments(
                studentId, List.of(document), List.of("BIRTH_CERTIFICATE"));

        assertThat(result).containsExactly(mapped);
        verify(fileValidator).validateDocument(document);
        verify(studentDocumentRepository).save(any(StudentDocument.class));
        assertThat(student.getUpdatedBy()).isEqualTo(actorId);
    }

    @Test
    void replaceDocumentUpdatesMetadataAndDeletesOld() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        String oldPath = "student-documents/" + studentId + "/old.pdf";
        Student student = Student.builder().id(studentId).build();
        StudentDocument existing = StudentDocument.builder()
                .id(documentId)
                .student(student)
                .documentType(DocumentType.AADHAAR_CARD)
                .originalFileName("old.pdf")
                .storedFileName("old.pdf")
                .storagePath(oldPath)
                .contentType("application/pdf")
                .fileSize(4L)
                .uploadedDate(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        MockMultipartFile replacement = new MockMultipartFile(
                "document", "new.pdf", "application/pdf", "%PDF-new".getBytes());
        StudentDocumentResponse mapped = new StudentDocumentResponse(
                documentId,
                DocumentType.IDENTITY_PROOF,
                "new.pdf",
                "application/pdf",
                8L,
                Instant.now());

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentDocumentRepository.findByIdAndStudent_Id(documentId, studentId))
                .thenReturn(Optional.of(existing));
        when(fileValidator.extensionOf("new.pdf")).thenReturn("pdf");
        when(storageService.store(anyString(), anyString(), any(), anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentDocumentRepository.save(any(StudentDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentMapper.toDocumentResponse(any(StudentDocument.class))).thenReturn(mapped);

        StudentDocumentResponse response = studentService.replaceDocument(
                studentId, documentId, replacement, "IDENTITY_PROOF");

        assertThat(response.documentType()).isEqualTo(DocumentType.IDENTITY_PROOF);
        assertThat(existing.getDocumentType()).isEqualTo(DocumentType.IDENTITY_PROOF);
        assertThat(existing.getOriginalFileName()).isEqualTo("new.pdf");
        verify(storageService).delete(oldPath);
    }

    @Test
    void replaceDocumentThrowsWhenOwnershipMismatch() {
        UUID studentId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        MockMultipartFile document = new MockMultipartFile(
                "document", "file.pdf", "application/pdf", "%PDF".getBytes());

        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(Student.builder().id(studentId).build()));
        when(studentDocumentRepository.findByIdAndStudent_Id(documentId, studentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.replaceDocument(studentId, documentId, document, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void deletePhotoClearsPathAndRemovesStorageObject() {
        UUID studentId = UUID.randomUUID();
        String path = "student-documents/" + studentId + "/profile-photo.jpg";
        Student student = Student.builder().id(studentId).profilePhotoPath(path).build();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.deletePhoto(studentId);

        assertThat(student.getProfilePhotoPath()).isNull();
        assertThat(student.getUpdatedBy()).isEqualTo(actorId);
        verify(storageService).delete(path);
    }

    @Test
    void deletePhotoThrowsWhenNoPhotoOnFile() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(Student.builder().id(studentId).profilePhotoPath(null).build()));

        assertThatThrownBy(() -> studentService.deletePhoto(studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Profile photo not found");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void deletePhotoThrowsWhenStudentMissing() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.deletePhoto(studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void deletePhotoSucceedsWhenStorageDeleteFails() {
        UUID studentId = UUID.randomUUID();
        String path = "student-documents/" + studentId + "/profile-photo.jpg";
        Student student = Student.builder().id(studentId).profilePhotoPath(path).build();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("storage down"))
                .when(storageService).delete(path);

        studentService.deletePhoto(studentId);

        assertThat(student.getProfilePhotoPath()).isNull();
    }

    @Test
    void deleteDocumentSoftDeletesAndRetainsStorageObject() {
        UUID studentId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Student student = Student.builder().id(studentId).build();
        StudentDocument document = StudentDocument.builder()
                .id(documentId)
                .student(student)
                .documentType(DocumentType.AADHAAR_CARD)
                .originalFileName("aadhaar.pdf")
                .storedFileName("aadhaar.pdf")
                .storagePath("student-documents/" + studentId + "/aadhaar.pdf")
                .contentType("application/pdf")
                .fileSize(4L)
                .uploadedDate(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentDocumentRepository.findByIdAndStudent_Id(documentId, studentId))
                .thenReturn(Optional.of(document));
        when(studentDocumentRepository.save(any(StudentDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.deleteDocument(studentId, documentId);

        assertThat(document.isDeleted()).isTrue();
        assertThat(document.getDeletedDate()).isNotNull();
        assertThat(student.getUpdatedBy()).isEqualTo(actorId);
        verify(storageService, never()).delete(anyString());
    }

    @Test
    void deleteDocumentThrowsWhenOwnershipMismatch() {
        UUID studentId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(Student.builder().id(studentId).build()));
        when(studentDocumentRepository.findByIdAndStudent_Id(documentId, studentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.deleteDocument(studentId, documentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Document not found");
        verify(studentDocumentRepository, never()).save(any());
    }

    private CreateStudentRequest sampleRequest(String admissionNumber, String aadhaar) {
        return new CreateStudentRequest(
                admissionNumber,
                "Ravi",
                "Kumar",
                Gender.MALE,
                LocalDate.of(2015, 5, 1),
                null,
                null,
                null,
                aadhaar,
                null,
                "Guardian",
                "Uncle",
                "9876543210",
                "Address",
                "School",
                "5",
                "English",
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2024, 6, 1));
    }

    private UpdateStudentRequest sampleUpdateRequest(String aadhaar) {
        return new UpdateStudentRequest(
                "Ravi",
                "Kumar",
                Gender.MALE,
                LocalDate.of(2015, 5, 1),
                null,
                null,
                null,
                aadhaar,
                null,
                "Guardian",
                "Uncle",
                "9876543210",
                "Address",
                "School",
                "5",
                "English",
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2024, 6, 1));
    }
}
