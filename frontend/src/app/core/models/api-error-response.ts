/**
 * Standard API error payload matching backend ApiErrorResponse.
 * @see docs/07_API_Design.md
 */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
