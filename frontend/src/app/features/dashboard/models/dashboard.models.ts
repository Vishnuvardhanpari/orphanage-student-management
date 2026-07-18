export type StudentGender = 'MALE' | 'FEMALE' | 'OTHER';
export type StudentStatus = 'ACTIVE' | 'INACTIVE';

export interface DashboardRecentStudent {
  id: string;
  firstName: string;
  lastName: string;
  admissionNumber: string;
  admissionDate: string;
  updatedDate: string;
}

export interface DashboardSummary {
  totalStudents: number;
  activeStudents: number;
  inactiveStudents: number;
  newAdmissions: number;
  maleStudents: number;
  femaleStudents: number;
  recentAdmissions: DashboardRecentStudent[];
  recentUpdates: DashboardRecentStudent[];
}

export interface DashboardMonthlyAdmission {
  yearMonth: string;
  count: number;
}

export interface DashboardGenderCount {
  gender: StudentGender;
  count: number;
}

export interface DashboardStatusCount {
  status: StudentStatus;
  count: number;
}

export function studentDisplayName(student: DashboardRecentStudent): string {
  return [student.firstName, student.lastName].filter(Boolean).join(' ').trim();
}
