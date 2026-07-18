package com.orphanage.oms.dashboard.dto;

import java.util.List;

/**
 * Executive dashboard summary counts and recent activity.
 */
public record DashboardSummaryResponse(
        long totalStudents,
        long activeStudents,
        long inactiveStudents,
        long newAdmissions,
        long maleStudents,
        long femaleStudents,
        List<DashboardRecentStudentResponse> recentAdmissions,
        List<DashboardRecentStudentResponse> recentUpdates
) {
}
