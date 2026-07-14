package com.orphanage.oms.student.service;

import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.security.UserPrincipal;
import com.orphanage.oms.storage.StorageService;
import com.orphanage.oms.student.dto.CreateStudentRequest;
import com.orphanage.oms.student.dto.StudentCreatedResponse;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.entity.StudentDocument;
import com.orphanage.oms.student.enums.DocumentType;
import com.orphanage.oms.student.enums.StudentStatus;
import com.orphanage.oms.student.mapper.StudentMapper;
import com.orphanage.oms.student.repository.StudentDocumentRepository;
import com.orphanage.oms.student.repository.StudentRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Student registration: create student record with optional photo and documents.
 */
@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

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

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
