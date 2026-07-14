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
import com.orphanage.oms.student.dto.StudentCreatedResponse;
import com.orphanage.oms.student.dto.StudentDetailResponse;
import com.orphanage.oms.student.dto.StudentDocumentResponse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
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

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentMapper.toDetailResponse(student)).thenReturn(detail);

        assertThat(studentService.getById(studentId).hasProfilePhoto()).isTrue();
        assertThat(studentService.getById(studentId).admissionNumber()).isEqualTo("ADM-010");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getById(studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void listDocumentsRequiresStudent() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

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

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentDocumentRepository.findByStudent_IdOrderByUploadedDateDesc(studentId))
                .thenReturn(List.of(document));
        when(studentMapper.toDocumentResponse(document)).thenReturn(mapped);

        assertThat(studentService.listDocuments(studentId)).containsExactly(mapped);
    }

    @Test
    void downloadDocumentThrowsWhenOwnershipMismatch() {
        UUID studentId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(studentRepository.findById(studentId))
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

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
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
        when(studentRepository.findById(studentId))
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

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(storageService.load(path)).thenReturn(new ByteArrayInputStream(bytes));

        StoredFilePayload payload = studentService.loadProfilePhoto(studentId);
        assertThat(payload.contentType()).isEqualTo("image/jpeg");
        assertThat(payload.fileName()).isEqualTo("profile-photo.jpg");
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
