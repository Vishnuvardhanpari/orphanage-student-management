export const MAX_FILE_BYTES = 10 * 1024 * 1024;
export const PHOTO_FILE_TYPES: ReadonlySet<string> = new Set(['image/jpeg', 'image/png']);
export const DOCUMENT_FILE_TYPES: ReadonlySet<string> = new Set([
  'image/jpeg',
  'image/png',
  'application/pdf',
]);

/** Returns a user-facing error message, or null when the photo file is acceptable. */
export function photoFileError(file: File): string | null {
  if (!PHOTO_FILE_TYPES.has(file.type) || file.size > MAX_FILE_BYTES) {
    return 'Photo must be JPG or PNG and at most 10 MB.';
  }
  return null;
}

/** Returns a user-facing error message, or null when the document file is acceptable. */
export function documentFileError(file: File): string | null {
  if (!DOCUMENT_FILE_TYPES.has(file.type) || file.size > MAX_FILE_BYTES) {
    return `${file.name} must be PDF/JPG/PNG and at most 10 MB.`;
  }
  return null;
}
