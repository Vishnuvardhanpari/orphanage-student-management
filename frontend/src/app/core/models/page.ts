/**
 * Generic server-side page shape for list endpoints (Milestone 8+).
 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
