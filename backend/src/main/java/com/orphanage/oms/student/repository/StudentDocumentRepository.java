package com.orphanage.oms.student.repository;

import com.orphanage.oms.student.entity.StudentDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for student document metadata.
 */
public interface StudentDocumentRepository extends JpaRepository<StudentDocument, UUID> {

    List<StudentDocument> findByStudent_IdOrderByUploadedDateDesc(UUID studentId);

    Optional<StudentDocument> findByIdAndStudent_Id(UUID id, UUID studentId);
}
