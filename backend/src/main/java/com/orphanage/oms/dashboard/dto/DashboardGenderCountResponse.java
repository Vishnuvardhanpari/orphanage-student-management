package com.orphanage.oms.dashboard.dto;

import com.orphanage.oms.student.enums.Gender;

/**
 * Active-student count for a single gender.
 */
public record DashboardGenderCountResponse(Gender gender, long count) {
}
