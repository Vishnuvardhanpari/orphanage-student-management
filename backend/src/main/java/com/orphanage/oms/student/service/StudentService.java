package com.orphanage.oms.student.service;

import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.security.UserPrincipal;
import com.orphanage.oms.storage.StorageService;
import com.orphanage.oms.student.dto.CreateStudentRequest;
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
import com.orphanage.oms.student.repository.StudentSpecifications;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Student registration, profile read, and update operations.
 */
@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "admissionNumber",
            "firstName",
            "lastName",
            "gender",
            "dateOfBirth",
            "admissionDate",
            "status",
            "createdDate");

    private final StudentRepository studentRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentMapper studentMapper;
    private final StudentFileValidator fileValidator;
    private final StorageService storageService;

    public StudentService(
            StudentRepository studentRepository,
            StudentDocumentRepository studentDocumentRepository,
            StudentMapper studentMapper,
            StudentFileValidator fileValidator,
            StorageService storageService) {
        this.studentRepository = studentRepository;
        this.studentDocumentRepository = studentDocumentRepository;
        this.studentMapper = studentMapper;
        this.fileValidator = fileValidator;
        this.storageService = storageService;
    }

    /**
     * Paginated student list with optional global search and combinable filters.
     * Age bounds are translated into a date-of-birth window; age is never persisted.
     */
    @Transactional(readOnly = true)
    public Page<StudentSummaryResponse> list(
            String search,
            Gender gender,
            StudentStatus status,
            Integer admissionYear,
            String school,
            Integer ageMin,
            Integer ageMax,
            Pageable pageable) {
        validateSort(pageable.getSort());
        validateAgeRange(ageMin, ageMax);

        List<Specification<Student>> specs = new ArrayList<>();

        String normalizedSearch = blankToNull(search);
        if (normalizedSearch != null) {
            specs.add(StudentSpecifications.matchesSearch(normalizedSearch));
        }
        if (gender != null) {
            specs.add(StudentSpecifications.hasGender(gender));
        }
        if (status != null) {
            specs.add(StudentSpecifications.hasStatus(status));
        }
        if (admissionYear != null) {
            specs.add(StudentSpecifications.admittedInYear(admissionYear));
        }
        String normalizedSchool = blankToNull(school);
        if (normalizedSchool != null) {
            specs.add(StudentSpecifications.schoolContains(normalizedSchool));
        }
        if (ageMin != null || ageMax != null) {
            LocalDate today = LocalDate.now();
            LocalDate maxDateOfBirth = ageMin != null ? today.minusYears(ageMin) : null;
            LocalDate minDateOfBirthExclusive =
                    ageMax != null ? today.minusYears(ageMax + 1L) : null;
            specs.add(StudentSpecifications.bornBetween(minDateOfBirthExclusive, maxDateOfBirth));
        }

        return studentRepository
                .findAll(Specification.allOf(specs), pageable)
                .map(studentMapper::toSummaryResponse);
    }

    private static void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Validation Error",
                        "Unsupported sort property: " + order.getProperty());
            }
        }
    }

    private static void validateAgeRange(Integer ageMin, Integer ageMax) {
        if ((ageMin != null && ageMin < 0) || (ageMax != null && ageMax < 0)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Age filters must not be negative.");
        }
        if (ageMin != null && ageMax != null && ageMin > ageMax) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "ageMin must be less than or equal to ageMax.");
        }
    }

    @Transactional(readOnly = true)
    public StudentDetailResponse getById(UUID id) {
        Student student = requireStudent(id);
        log.info("Student profile viewed id={}", id);
        return studentMapper.toDetailResponse(student);
    }

    @Transactional(readOnly = true)
    public List<StudentDocumentResponse> listDocuments(UUID studentId) {
        requireStudent(studentId);
        return studentDocumentRepository.findByStudent_IdOrderByUploadedDateDesc(studentId).stream()
                .map(studentMapper::toDocumentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoredFilePayload downloadDocument(UUID studentId, UUID documentId) {
        requireStudent(studentId);
        StudentDocument document = studentDocumentRepository
                .findByIdAndStudent_Id(documentId, studentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Not Found", "Document not found."));

        InputStream content = storageService.load(document.getStoragePath());
        log.info("Student document downloaded studentId={} documentId={}", studentId, documentId);
        return new StoredFilePayload(
                content,
                document.getContentType(),
                document.getOriginalFileName(),
                document.getFileSize());
    }

    @Transactional(readOnly = true)
    public StoredFilePayload loadProfilePhoto(UUID studentId) {
        Student student = requireStudent(studentId);
        String photoPath = student.getProfilePhotoPath();
        if (photoPath == null || photoPath.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Not Found", "Profile photo not found.");
        }

        InputStream content = storageService.load(photoPath);
        String contentType = guessPhotoContentType(photoPath);
        String fileName = photoPath.contains("/")
                ? photoPath.substring(photoPath.lastIndexOf('/') + 1)
                : photoPath;
        log.info("Student photo viewed id={}", studentId);
        return new StoredFilePayload(content, contentType, fileName, -1L);
    }

    @Transactional
    public StudentCreatedResponse create(
            CreateStudentRequest request,
            MultipartFile photo,
            List<MultipartFile> documents,
            List<String> documentTypes) {
        List<MultipartFile> docs = documents == null ? List.of() : documents.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        List<String> types = documentTypes == null ? List.of() : documentTypes;

        if (docs.size() != types.size()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "documents and documentTypes must have the same number of entries.");
        }

        fileValidator.validatePhoto(photo);
        List<DocumentType> parsedTypes = new ArrayList<>(docs.size());
        for (int i = 0; i < docs.size(); i++) {
            fileValidator.validateDocument(docs.get(i));
            DocumentType type = parseDocumentType(types.get(i));
            if (type == DocumentType.PHOTOGRAPH) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Validation Error",
                        "Use the photo part for the student photograph; PHOTOGRAPH is not valid as a supporting document type.");
            }
            parsedTypes.add(type);
        }

        if (request.dateOfBirth().isAfter(request.admissionDate())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Date of birth must be on or before admission date.");
        }

        String admissionNumber = request.admissionNumber().trim();
        String aadhaar = blankToNull(request.aadhaarNumber());

        if (studentRepository.existsByAdmissionNumberIgnoreCaseIncludingDeleted(admissionNumber)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "Admission number is already in use.");
        }
        if (aadhaar != null && studentRepository.existsByAadhaarNumberIncludingDeleted(aadhaar)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "Aadhaar number is already in use.");
        }

        UUID actorId = currentUserId();
        Student student = studentMapper.toEntity(request);
        student.setId(UUID.randomUUID());
        student.setAdmissionNumber(admissionNumber);
        student.setFirstName(request.firstName().trim());
        student.setLastName(blankToNull(request.lastName()));
        student.setAadhaarNumber(aadhaar);
        student.setPhoneNumber(blankToNull(request.phoneNumber()));
        student.setBloodGroup(blankToNull(request.bloodGroup()));
        student.setReligion(blankToNull(request.religion()));
        student.setNationality(blankToNull(request.nationality()));
        student.setGuardianName(blankToNull(request.guardianName()));
        student.setGuardianRelationship(blankToNull(request.guardianRelationship()));
        student.setGuardianPhone(blankToNull(request.guardianPhone()));
        student.setGuardianAddress(blankToNull(request.guardianAddress()));
        student.setSchoolName(blankToNull(request.schoolName()));
        student.setStandard(blankToNull(request.standard()));
        student.setMedium(blankToNull(request.medium()));
        student.setPreviousSchool(blankToNull(request.previousSchool()));
        student.setMedicalConditions(blankToNull(request.medicalConditions()));
        student.setAllergies(blankToNull(request.allergies()));
        student.setDisability(blankToNull(request.disability()));
        student.setEmergencyNotes(blankToNull(request.emergencyNotes()));
        student.setStatus(StudentStatus.ACTIVE);
        student.setDeleted(false);
        student.setCreatedBy(actorId);
        student.setUpdatedBy(actorId);

        Student saved = studentRepository.save(student);
        List<String> storedPaths = new ArrayList<>();

        try {
            if (photo != null && !photo.isEmpty()) {
                String extension = fileValidator.extensionOf(photo.getOriginalFilename());
                String relativePath = "student-documents/" + saved.getId() + "/profile-photo." + extension;
                String contentType = photo.getContentType() != null ? photo.getContentType() : "application/octet-stream";
                storeFile(relativePath, contentType, photo, storedPaths);
                saved.setProfilePhotoPath(relativePath);
                saved = studentRepository.save(saved);
            }

            for (int i = 0; i < docs.size(); i++) {
                MultipartFile document = docs.get(i);
                DocumentType documentType = parsedTypes.get(i);
                String extension = fileValidator.extensionOf(document.getOriginalFilename());
                String storedFileName = documentType.name().toLowerCase(Locale.ROOT)
                        + "-" + UUID.randomUUID() + "." + extension;
                String relativePath = "student-documents/" + saved.getId() + "/" + storedFileName;
                String contentType = document.getContentType() != null
                        ? document.getContentType()
                        : "application/octet-stream";
                storeFile(relativePath, contentType, document, storedPaths);

                String originalName = document.getOriginalFilename() != null
                        ? document.getOriginalFilename()
                        : storedFileName;

                StudentDocument metadata = StudentDocument.builder()
                        .student(saved)
                        .documentType(documentType)
                        .originalFileName(originalName)
                        .storedFileName(storedFileName)
                        .storagePath(relativePath)
                        .contentType(contentType)
                        .fileSize(document.getSize())
                        .uploadedBy(actorId)
                        .deleted(false)
                        .build();
                studentDocumentRepository.save(metadata);
            }
        } catch (RuntimeException ex) {
            compensateStoredFiles(storedPaths);
            throw ex;
        }

        log.info(
                "Student created id={} admissionNumber={} by={}",
                saved.getId(),
                saved.getAdmissionNumber(),
                actorId);
        return studentMapper.toCreatedResponse(saved);
    }

    @Transactional
    public StudentDetailResponse update(UUID id, UpdateStudentRequest request) {
        Student student = requireStudent(id);

        if (request.dateOfBirth().isAfter(request.admissionDate())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Date of birth must be on or before admission date.");
        }

        String aadhaar = blankToNull(request.aadhaarNumber());
        if (aadhaar != null
                && studentRepository.existsByAadhaarNumberIncludingDeletedExcludingId(
                        aadhaar, id)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "Aadhaar number is already in use.");
        }

        UUID actorId = currentUserId();
        studentMapper.updateFromDto(request, student);
        student.setFirstName(request.firstName().trim());
        student.setLastName(blankToNull(request.lastName()));
        student.setAadhaarNumber(aadhaar);
        student.setPhoneNumber(blankToNull(request.phoneNumber()));
        student.setBloodGroup(blankToNull(request.bloodGroup()));
        student.setReligion(blankToNull(request.religion()));
        student.setNationality(blankToNull(request.nationality()));
        student.setGuardianName(blankToNull(request.guardianName()));
        student.setGuardianRelationship(blankToNull(request.guardianRelationship()));
        student.setGuardianPhone(blankToNull(request.guardianPhone()));
        student.setGuardianAddress(blankToNull(request.guardianAddress()));
        student.setSchoolName(blankToNull(request.schoolName()));
        student.setStandard(blankToNull(request.standard()));
        student.setMedium(blankToNull(request.medium()));
        student.setPreviousSchool(blankToNull(request.previousSchool()));
        student.setMedicalConditions(blankToNull(request.medicalConditions()));
        student.setAllergies(blankToNull(request.allergies()));
        student.setDisability(blankToNull(request.disability()));
        student.setEmergencyNotes(blankToNull(request.emergencyNotes()));
        student.setUpdatedBy(actorId);

        Student saved = studentRepository.save(student);
        log.info("Student updated id={} by={}", saved.getId(), actorId);
        return studentMapper.toDetailResponse(saved);
    }

    @Transactional
    public void replacePhoto(UUID id, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "Validation Error", "Photo is required.");
        }
        fileValidator.validatePhoto(photo);

        Student student = requireStudent(id);
        UUID actorId = currentUserId();
        String previousPath = student.getProfilePhotoPath();
        List<String> storedPaths = new ArrayList<>();

        String extension = fileValidator.extensionOf(photo.getOriginalFilename());
        String relativePath = "student-documents/" + student.getId() + "/profile-photo." + extension;
        String contentType =
                photo.getContentType() != null ? photo.getContentType() : "application/octet-stream";

        try {
            storeFile(relativePath, contentType, photo, storedPaths);
            student.setProfilePhotoPath(relativePath);
            student.setUpdatedBy(actorId);
            studentRepository.save(student);
        } catch (RuntimeException ex) {
            compensateStoredFiles(storedPaths);
            throw ex;
        }

        if (previousPath != null
                && !previousPath.isBlank()
                && !previousPath.equals(relativePath)) {
            try {
                storageService.delete(previousPath);
            } catch (RuntimeException ex) {
                log.warn("Failed to delete previous profile photo path={}", previousPath);
            }
        }

        log.info("Student photo replaced id={} by={}", id, actorId);
    }

    @Transactional
    public List<StudentDocumentResponse> addDocuments(
            UUID studentId, List<MultipartFile> documents, List<String> documentTypes) {
        Student student = requireStudent(studentId);
        List<MultipartFile> docs = documents == null ? List.of() : documents.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        List<String> types = documentTypes == null ? List.of() : documentTypes;

        if (docs.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "At least one document is required.");
        }
        if (docs.size() != types.size()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "documents and documentTypes must have the same number of entries.");
        }

        List<DocumentType> parsedTypes = new ArrayList<>(docs.size());
        for (int i = 0; i < docs.size(); i++) {
            fileValidator.validateDocument(docs.get(i));
            DocumentType type = parseDocumentType(types.get(i));
            if (type == DocumentType.PHOTOGRAPH) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Validation Error",
                        "Use the photo endpoint for the student photograph; PHOTOGRAPH is not valid as a supporting document type.");
            }
            parsedTypes.add(type);
        }

        UUID actorId = currentUserId();
        List<String> storedPaths = new ArrayList<>();
        List<StudentDocumentResponse> created = new ArrayList<>();

        try {
            for (int i = 0; i < docs.size(); i++) {
                MultipartFile document = docs.get(i);
                DocumentType documentType = parsedTypes.get(i);
                String extension = fileValidator.extensionOf(document.getOriginalFilename());
                String storedFileName = documentType.name().toLowerCase(Locale.ROOT)
                        + "-" + UUID.randomUUID() + "." + extension;
                String relativePath = "student-documents/" + student.getId() + "/" + storedFileName;
                String contentType = document.getContentType() != null
                        ? document.getContentType()
                        : "application/octet-stream";
                storeFile(relativePath, contentType, document, storedPaths);

                String originalName = document.getOriginalFilename() != null
                        ? document.getOriginalFilename()
                        : storedFileName;

                StudentDocument metadata = StudentDocument.builder()
                        .student(student)
                        .documentType(documentType)
                        .originalFileName(originalName)
                        .storedFileName(storedFileName)
                        .storagePath(relativePath)
                        .contentType(contentType)
                        .fileSize(document.getSize())
                        .uploadedBy(actorId)
                        .deleted(false)
                        .build();
                StudentDocument saved = studentDocumentRepository.save(metadata);
                created.add(studentMapper.toDocumentResponse(saved));
            }

            student.setUpdatedBy(actorId);
            studentRepository.save(student);
        } catch (RuntimeException ex) {
            compensateStoredFiles(storedPaths);
            throw ex;
        }

        log.info(
                "Student documents uploaded studentId={} count={} by={}",
                studentId,
                created.size(),
                actorId);
        return created;
    }

    @Transactional
    public StudentDocumentResponse replaceDocument(
            UUID studentId, UUID documentId, MultipartFile document, String documentTypeRaw) {
        if (document == null || document.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "Validation Error", "Document is required.");
        }
        fileValidator.validateDocument(document);

        Student student = requireStudent(studentId);
        StudentDocument existing = studentDocumentRepository
                .findByIdAndStudent_Id(documentId, studentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Not Found", "Document not found."));

        DocumentType documentType = existing.getDocumentType();
        if (documentTypeRaw != null && !documentTypeRaw.isBlank()) {
            documentType = parseDocumentType(documentTypeRaw);
            if (documentType == DocumentType.PHOTOGRAPH) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Validation Error",
                        "Use the photo endpoint for the student photograph; PHOTOGRAPH is not valid as a supporting document type.");
            }
        }

        UUID actorId = currentUserId();
        String previousPath = existing.getStoragePath();
        List<String> storedPaths = new ArrayList<>();

        String extension = fileValidator.extensionOf(document.getOriginalFilename());
        String storedFileName = documentType.name().toLowerCase(Locale.ROOT)
                + "-" + UUID.randomUUID() + "." + extension;
        String relativePath = "student-documents/" + student.getId() + "/" + storedFileName;
        String contentType = document.getContentType() != null
                ? document.getContentType()
                : "application/octet-stream";

        try {
            storeFile(relativePath, contentType, document, storedPaths);

            existing.setDocumentType(documentType);
            existing.setOriginalFileName(
                    document.getOriginalFilename() != null
                            ? document.getOriginalFilename()
                            : storedFileName);
            existing.setStoredFileName(storedFileName);
            existing.setStoragePath(relativePath);
            existing.setContentType(contentType);
            existing.setFileSize(document.getSize());
            existing.setUploadedBy(actorId);
            existing.setUploadedDate(Instant.now());

            student.setUpdatedBy(actorId);
            studentRepository.save(student);
            StudentDocument saved = studentDocumentRepository.save(existing);

            if (previousPath != null
                    && !previousPath.isBlank()
                    && !previousPath.equals(relativePath)) {
                try {
                    storageService.delete(previousPath);
                } catch (RuntimeException ex) {
                    log.warn("Failed to delete previous document path={}", previousPath);
                }
            }

            log.info(
                    "Student document replaced studentId={} documentId={} by={}",
                    studentId,
                    documentId,
                    actorId);
            return studentMapper.toDocumentResponse(saved);
        } catch (RuntimeException ex) {
            compensateStoredFiles(storedPaths);
            throw ex;
        }
    }

    @Transactional
    public void deletePhoto(UUID id) {
        Student student = requireStudent(id);
        String photoPath = student.getProfilePhotoPath();
        if (photoPath == null || photoPath.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Not Found", "Profile photo not found.");
        }

        UUID actorId = currentUserId();
        student.setProfilePhotoPath(null);
        student.setUpdatedBy(actorId);
        studentRepository.save(student);

        try {
            storageService.delete(photoPath);
        } catch (RuntimeException ex) {
            log.warn("Failed to delete profile photo storage object path={}", photoPath);
        }

        log.info("Student photo deleted id={} by={}", id, actorId);
    }

    @Transactional
    public void deleteDocument(UUID studentId, UUID documentId) {
        Student student = requireStudent(studentId);
        StudentDocument document = studentDocumentRepository
                .findByIdAndStudent_Id(documentId, studentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Not Found", "Document not found."));

        UUID actorId = currentUserId();
        // Logical removal only: the storage object is retained for historical recovery.
        document.setDeleted(true);
        document.setDeletedDate(Instant.now());
        studentDocumentRepository.save(document);

        student.setUpdatedBy(actorId);
        studentRepository.save(student);

        log.info(
                "Student document deleted studentId={} documentId={} by={}",
                studentId,
                documentId,
                actorId);
    }

    private void storeFile(
            String relativePath,
            String contentType,
            MultipartFile file,
            List<String> storedPaths) {
        try (InputStream inputStream = file.getInputStream()) {
            storageService.store(relativePath, contentType, inputStream, file.getSize());
            storedPaths.add(relativePath);
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Storage Error",
                    "Failed to read uploaded file.");
        }
    }

    private void compensateStoredFiles(List<String> storedPaths) {
        for (String path : storedPaths) {
            try {
                storageService.delete(path);
            } catch (RuntimeException ex) {
                log.warn("Failed to compensate stored file path={}", path);
            }
        }
    }

    private DocumentType parseDocumentType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "documentTypes entries must not be blank.");
        }
        try {
            return DocumentType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Invalid document type: " + raw);
        }
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required.");
        }
        return principal.getId();
    }

    private Student requireStudent(UUID id) {
        return studentRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not Found", "Student not found."));
    }

    private static String guessPhotoContentType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
