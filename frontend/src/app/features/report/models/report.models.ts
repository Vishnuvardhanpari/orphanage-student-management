/** Frontend models for report PDF export. */

export type ReportStudentScope = 'ACTIVE' | 'ARCHIVED' | 'ALL';

export interface ReportFilterRequest {
  scope?: ReportStudentScope | null;
  search?: string | null;
  gender?: string | null;
  admissionYear?: number | null;
  school?: string | null;
  ageMin?: number | null;
  ageMax?: number | null;
}

export interface ReportFileDownload {
  blob: Blob;
  fileName: string | null;
}

export interface StudentSelectionPreview {
  id: string;
  admissionNumber: string;
  firstName: string;
  lastName: string | null;
}

const SCOPE_LABELS: Record<ReportStudentScope, string> = {
  ACTIVE: 'Active students',
  ARCHIVED: 'Archived students',
  ALL: 'All students (active and archived)',
};

/** Human-readable summary of filters shown before export. */
export function summarizeReportFilters(filters: ReportFilterRequest): string[] {
  const lines: string[] = [];
  const scope = filters.scope ?? 'ACTIVE';
  lines.push(`Scope: ${SCOPE_LABELS[scope]}`);
  if (filters.search?.trim()) {
    lines.push(`Search: ${filters.search.trim()}`);
  }
  if (filters.gender) {
    lines.push(`Gender: ${filters.gender}`);
  }
  if (filters.admissionYear != null) {
    lines.push(`Admission year: ${filters.admissionYear}`);
  }
  if (filters.school?.trim()) {
    lines.push(`School: ${filters.school.trim()}`);
  }
  if (filters.ageMin != null || filters.ageMax != null) {
    const min = filters.ageMin ?? 'any';
    const max = filters.ageMax ?? 'any';
    lines.push(`Age: ${min} – ${max}`);
  }
  return lines;
}

/** Truncated selection preview with a "+N more" remainder when needed. */
export function buildSelectionPreviewDetails(
  selected: StudentSelectionPreview[],
  limit = 10,
): string[] {
  const lines = selected.slice(0, limit).map((s) => {
    const name = [s.firstName, s.lastName].filter(Boolean).join(' ');
    return `${s.admissionNumber} — ${name}`;
  });
  if (selected.length > limit) {
    lines.push(`+${selected.length - limit} more`);
  }
  return lines;
}
