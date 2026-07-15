package com.orphanage.oms.student.mapper;

import com.orphanage.oms.student.dto.CreateStudentRequest;
import com.orphanage.oms.student.dto.StudentCreatedResponse;
import com.orphanage.oms.student.dto.StudentDetailResponse;
import com.orphanage.oms.student.dto.StudentDocumentResponse;
import com.orphanage.oms.student.dto.StudentSummaryResponse;
import com.orphanage.oms.student.dto.UpdateStudentRequest;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.entity.StudentDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Maps student registration and update DTOs and entities.
 */
@Mapper(componentModel = "spring")
public interface StudentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "profilePhotoPath", ignore = true)
    @Mapping(target = "exitDate", ignore = true)
    @Mapping(target = "exitReason", ignore = true)
    @Mapping(target = "exitRemarks", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "documents", ignore = true)
    Student toEntity(CreateStudentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "admissionNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "profilePhotoPath", ignore = true)
    @Mapping(target = "exitDate", ignore = true)
    @Mapping(target = "exitReason", ignore = true)
    @Mapping(target = "exitRemarks", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "documents", ignore = true)
    void updateFromDto(UpdateStudentRequest request, @MappingTarget Student student);

    StudentCreatedResponse toCreatedResponse(Student student);

    @Mapping(
            target = "hasProfilePhoto",
            expression = "java(student.getProfilePhotoPath() != null"
                    + " && !student.getProfilePhotoPath().isBlank())")
    StudentDetailResponse toDetailResponse(Student student);

    StudentDocumentResponse toDocumentResponse(StudentDocument document);

    StudentSummaryResponse toSummaryResponse(Student student);
}
