package com.orphanage.oms.student.repository;

import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.enums.Gender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * Native SQL for soft-deleted student list/filter (bypasses {@code @SQLRestriction}).
 */
@Repository
public class StudentDeletedQueryImpl implements StudentDeletedQuery {

    private static final Map<String, String> SORT_PROPERTY_TO_COLUMN = Map.ofEntries(
            Map.entry("admissionNumber", "admission_number"),
            Map.entry("firstName", "first_name"),
            Map.entry("lastName", "last_name"),
            Map.entry("gender", "gender"),
            Map.entry("dateOfBirth", "date_of_birth"),
            Map.entry("admissionDate", "admission_date"),
            Map.entry("status", "status"),
            Map.entry("createdDate", "created_date"),
            Map.entry("deletedDate", "deleted_date"),
            Map.entry("schoolName", "school_name"),
            Map.entry("standard", "standard"),
            // Native pageable may already use SQL column names
            Map.entry("admission_number", "admission_number"),
            Map.entry("first_name", "first_name"),
            Map.entry("last_name", "last_name"),
            Map.entry("date_of_birth", "date_of_birth"),
            Map.entry("admission_date", "admission_date"),
            Map.entry("created_date", "created_date"),
            Map.entry("deleted_date", "deleted_date"),
            Map.entry("school_name", "school_name"));

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Student> findDeletedMatching(
            String search,
            Gender gender,
            Integer admissionYear,
            String school,
            LocalDate dateOfBirthMinExclusive,
            LocalDate dateOfBirthMaxInclusive,
            Pageable pageable) {
        long total = countDeletedMatching(
                search, gender, admissionYear, school, dateOfBirthMinExclusive, dateOfBirthMaxInclusive);
        if (total == 0) {
            return Page.empty(pageable);
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM students WHERE deleted = true");
        Map<String, Object> params = new HashMap<>();
        appendFilters(
                sql,
                params,
                search,
                gender,
                admissionYear,
                school,
                dateOfBirthMinExclusive,
                dateOfBirthMaxInclusive);
        appendOrderBy(sql, pageable.getSort());

        Query query = entityManager.createNativeQuery(sql.toString(), Student.class);
        bindParams(query, params);
        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        @SuppressWarnings("unchecked")
        List<Student> content = query.getResultList();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Student> findDeletedMatching(
            String search,
            Gender gender,
            Integer admissionYear,
            String school,
            LocalDate dateOfBirthMinExclusive,
            LocalDate dateOfBirthMaxInclusive,
            Sort sort) {
        StringBuilder sql = new StringBuilder("SELECT * FROM students WHERE deleted = true");
        Map<String, Object> params = new HashMap<>();
        appendFilters(
                sql,
                params,
                search,
                gender,
                admissionYear,
                school,
                dateOfBirthMinExclusive,
                dateOfBirthMaxInclusive);
        appendOrderBy(sql, sort);

        Query query = entityManager.createNativeQuery(sql.toString(), Student.class);
        bindParams(query, params);

        @SuppressWarnings("unchecked")
        List<Student> content = query.getResultList();
        return content;
    }

    @Override
    public long countDeletedMatching(
            String search,
            Gender gender,
            Integer admissionYear,
            String school,
            LocalDate dateOfBirthMinExclusive,
            LocalDate dateOfBirthMaxInclusive) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM students WHERE deleted = true");
        Map<String, Object> params = new HashMap<>();
        appendFilters(
                sql,
                params,
                search,
                gender,
                admissionYear,
                school,
                dateOfBirthMinExclusive,
                dateOfBirthMaxInclusive);

        Query query = entityManager.createNativeQuery(sql.toString());
        bindParams(query, params);
        Number count = (Number) query.getSingleResult();
        return count.longValue();
    }

    private static void appendFilters(
            StringBuilder sql,
            Map<String, Object> params,
            String search,
            Gender gender,
            Integer admissionYear,
            String school,
            LocalDate dateOfBirthMinExclusive,
            LocalDate dateOfBirthMaxInclusive) {
        if (search != null && !search.isBlank()) {
            String pattern = containsPattern(search);
            sql.append(
                    """
                     AND (
                       LOWER(first_name) LIKE :searchPattern ESCAPE '\\'
                       OR LOWER(COALESCE(last_name, '')) LIKE :searchPattern ESCAPE '\\'
                       OR LOWER(CONCAT(first_name, ' ', COALESCE(last_name, ''))) LIKE :searchPattern ESCAPE '\\'
                       OR LOWER(admission_number) LIKE :searchPattern ESCAPE '\\'
                       OR LOWER(COALESCE(aadhaar_number, '')) LIKE :searchPattern ESCAPE '\\'
                       OR LOWER(COALESCE(guardian_name, '')) LIKE :searchPattern ESCAPE '\\'
                       OR LOWER(COALESCE(phone_number, '')) LIKE :searchPattern ESCAPE '\\'
                     )
                    """);
            params.put("searchPattern", pattern);
        }
        if (gender != null) {
            sql.append(" AND gender = :gender");
            params.put("gender", gender.name());
        }
        if (admissionYear != null) {
            sql.append(" AND admission_date >= :yearStart AND admission_date < :yearEnd");
            params.put("yearStart", LocalDate.of(admissionYear, 1, 1));
            params.put("yearEnd", LocalDate.of(admissionYear + 1, 1, 1));
        }
        if (school != null && !school.isBlank()) {
            sql.append(" AND LOWER(COALESCE(school_name, '')) LIKE :schoolPattern ESCAPE '\\'");
            params.put("schoolPattern", containsPattern(school));
        }
        if (dateOfBirthMinExclusive != null) {
            sql.append(" AND date_of_birth > :dobMinExclusive");
            params.put("dobMinExclusive", dateOfBirthMinExclusive);
        }
        if (dateOfBirthMaxInclusive != null) {
            sql.append(" AND date_of_birth <= :dobMaxInclusive");
            params.put("dobMaxInclusive", dateOfBirthMaxInclusive);
        }
    }

    private static void appendOrderBy(StringBuilder sql, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            sql.append(" ORDER BY deleted_date DESC");
            return;
        }
        sql.append(" ORDER BY ");
        boolean first = true;
        for (Sort.Order order : sort) {
            String column = SORT_PROPERTY_TO_COLUMN.get(order.getProperty());
            if (column == null) {
                continue;
            }
            if (!first) {
                sql.append(", ");
            }
            sql.append(column).append(order.isAscending() ? " ASC" : " DESC");
            first = false;
        }
        if (first) {
            sql.append("deleted_date DESC");
        }
    }

    private static void bindParams(Query query, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private static String containsPattern(String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
