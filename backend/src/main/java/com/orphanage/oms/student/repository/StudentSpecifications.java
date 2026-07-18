package com.orphanage.oms.student.repository;

import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

/**
 * Dynamic predicates for the student list/search API and report filter export.
 *
 * <p>Soft-deleted rows are excluded automatically by {@code @SQLRestriction} on
 * {@link Student}; no predicate here needs to repeat that rule.
 */
public final class StudentSpecifications {

    private static final char LIKE_ESCAPE_CHAR = '\\';

    private StudentSpecifications() {
    }

    /**
     * Builds the combined list/search specification used by {@code GET /students}
     * and filtered PDF export. Validates age bounds before translating them into
     * a date-of-birth window.
     *
     * @param admissionNumber when non-blank, exact case-insensitive match (unique)
     */
    public static Specification<Student> buildListSpecification(
            String search,
            String admissionNumber,
            Gender gender,
            StudentStatus status,
            Integer admissionYear,
            String school,
            Integer ageMin,
            Integer ageMax) {
        validateAgeRange(ageMin, ageMax);

        List<Specification<Student>> specs = new ArrayList<>();

        String normalizedSearch = blankToNull(search);
        if (normalizedSearch != null) {
            specs.add(matchesSearch(normalizedSearch));
        }
        String normalizedAdmissionNumber = blankToNull(admissionNumber);
        if (normalizedAdmissionNumber != null) {
            specs.add(hasAdmissionNumberIgnoreCase(normalizedAdmissionNumber));
        }
        if (gender != null) {
            specs.add(hasGender(gender));
        }
        if (status != null) {
            specs.add(hasStatus(status));
        }
        if (admissionYear != null) {
            specs.add(admittedInYear(admissionYear));
        }
        String normalizedSchool = blankToNull(school);
        if (normalizedSchool != null) {
            specs.add(schoolContains(normalizedSchool));
        }
        if (ageMin != null || ageMax != null) {
            LocalDate[] bounds = ageToDateOfBirthBounds(ageMin, ageMax);
            specs.add(bornBetween(bounds[0], bounds[1]));
        }

        return Specification.allOf(specs);
    }

    /**
     * Translates age filters into a date-of-birth window.
     *
     * @return {@code [minExclusive, maxInclusive]} — either element may be null
     */
    public static LocalDate[] ageToDateOfBirthBounds(Integer ageMin, Integer ageMax) {
        LocalDate today = LocalDate.now();
        LocalDate maxInclusive = ageMin != null ? today.minusYears(ageMin) : null;
        LocalDate minExclusive = ageMax != null ? today.minusYears(ageMax + 1L) : null;
        return new LocalDate[] {minExclusive, maxInclusive};
    }

    /**
     * Exact case-insensitive match on admission number.
     */
    public static Specification<Student> hasAdmissionNumberIgnoreCase(String admissionNumber) {
        String normalized = admissionNumber.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("admissionNumber")), normalized);
    }

    public static void validateAgeRange(Integer ageMin, Integer ageMax) {
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

    /**
     * Case-insensitive contains match across name (first, last, and full name),
     * admission number, Aadhaar number, guardian name, and phone number.
     */
    public static Specification<Student> matchesSearch(String search) {
        return (root, query, cb) -> {
            String pattern = containsPattern(search);
            List<Predicate> matches = new ArrayList<>();
            matches.add(likeIgnoreCase(cb, root.get("firstName"), pattern));
            matches.add(likeIgnoreCase(cb, root.get("lastName"), pattern));
            matches.add(likeIgnoreCase(cb, fullName(root, cb), pattern));
            matches.add(likeIgnoreCase(cb, root.get("admissionNumber"), pattern));
            matches.add(likeIgnoreCase(cb, root.get("aadhaarNumber"), pattern));
            matches.add(likeIgnoreCase(cb, root.get("guardianName"), pattern));
            matches.add(likeIgnoreCase(cb, root.get("phoneNumber"), pattern));
            return cb.or(matches.toArray(Predicate[]::new));
        };
    }

    public static Specification<Student> hasGender(Gender gender) {
        return (root, query, cb) -> cb.equal(root.get("gender"), gender);
    }

    public static Specification<Student> hasStatus(StudentStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Admission date within the given calendar year, expressed as a sargable
     * range so the {@code admission_date} index remains usable.
     */
    public static Specification<Student> admittedInYear(int year) {
        LocalDate startInclusive = LocalDate.of(year, 1, 1);
        LocalDate endExclusive = startInclusive.plusYears(1);
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("admissionDate"), startInclusive),
                cb.lessThan(root.get("admissionDate"), endExclusive));
    }

    public static Specification<Student> schoolContains(String school) {
        return (root, query, cb) ->
                likeIgnoreCase(cb, root.get("schoolName"), containsPattern(school));
    }

    /**
     * Date-of-birth window derived from an age range. Either bound may be null.
     *
     * @param minExclusive students must be born strictly after this date (from ageMax)
     * @param maxInclusive students must be born on or before this date (from ageMin)
     */
    public static Specification<Student> bornBetween(
            LocalDate minExclusive, LocalDate maxInclusive) {
        return (root, query, cb) -> {
            List<Predicate> bounds = new ArrayList<>();
            if (minExclusive != null) {
                bounds.add(cb.greaterThan(root.get("dateOfBirth"), minExclusive));
            }
            if (maxInclusive != null) {
                bounds.add(cb.lessThanOrEqualTo(root.get("dateOfBirth"), maxInclusive));
            }
            return cb.and(bounds.toArray(Predicate[]::new));
        };
    }

    private static Expression<String> fullName(Root<Student> root, CriteriaBuilder cb) {
        Expression<String> last = cb.coalesce(root.get("lastName"), cb.literal(""));
        return cb.concat(cb.concat(root.get("firstName"), cb.literal(" ")), last);
    }

    private static Predicate likeIgnoreCase(
            CriteriaBuilder cb, Expression<String> expression, String pattern) {
        return cb.like(cb.lower(expression), pattern, LIKE_ESCAPE_CHAR);
    }

    private static String containsPattern(String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
