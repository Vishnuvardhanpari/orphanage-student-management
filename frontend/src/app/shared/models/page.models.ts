/** Spring Data page payload (backend `PageResponse` contract). */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

/** Empty page helper for error fallbacks and initial states. */
export function emptyPage<T>(size = 20): PageResponse<T> {
  return {
    content: [],
    totalElements: 0,
    totalPages: 0,
    size,
    number: 0,
    first: true,
    last: true,
    empty: true,
  };
}
