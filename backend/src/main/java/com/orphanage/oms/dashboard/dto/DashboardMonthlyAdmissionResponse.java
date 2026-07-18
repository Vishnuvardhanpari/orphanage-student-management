package com.orphanage.oms.dashboard.dto;

/**
 * Admission count for a calendar month ({@code yearMonth} as {@code YYYY-MM}).
 */
public record DashboardMonthlyAdmissionResponse(String yearMonth, long count) {
}
