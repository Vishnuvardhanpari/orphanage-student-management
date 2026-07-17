export enum Gender {
  Male = 'MALE',
  Female = 'FEMALE',
  Other = 'OTHER',
}

export enum StudentStatus {
  Active = 'ACTIVE',
  Inactive = 'INACTIVE',
}

export enum DocumentType {
  Photograph = 'PHOTOGRAPH',
  AadhaarCard = 'AADHAAR_CARD',
  BirthCertificate = 'BIRTH_CERTIFICATE',
  MedicalCertificate = 'MEDICAL_CERTIFICATE',
  MarkSheet = 'MARK_SHEET',
  TransferCertificate = 'TRANSFER_CERTIFICATE',
  IdentityProof = 'IDENTITY_PROOF',
  Other = 'OTHER',
}

/** Supporting document types selectable on registration (photo uses the dedicated photo field). */
export const SUPPORTING_DOCUMENT_TYPES: DocumentType[] = [
  DocumentType.AadhaarCard,
  DocumentType.BirthCertificate,
  DocumentType.MedicalCertificate,
  DocumentType.MarkSheet,
  DocumentType.TransferCertificate,
  DocumentType.IdentityProof,
  DocumentType.Other,
];

export interface CreateStudentRequest {
  admissionNumber: string;
  firstName: string;
  lastName?: string | null;
  gender: Gender;
  dateOfBirth: string;
  bloodGroup?: string | null;
  religion?: string | null;
  nationality?: string | null;
  aadhaarNumber?: string | null;
  phoneNumber?: string | null;
  guardianName?: string | null;
  guardianRelationship?: string | null;
  guardianPhone?: string | null;
  guardianAddress?: string | null;
  schoolName?: string | null;
  standard?: string | null;
  medium?: string | null;
  previousSchool?: string | null;
  medicalConditions?: string | null;
  allergies?: string | null;
  disability?: string | null;
  emergencyNotes?: string | null;
  admissionDate: string;
}

/** Editable fields for PUT /students/{id} (admission number is immutable). */
export type UpdateStudentRequest = Omit<CreateStudentRequest, 'admissionNumber'>;

/**
 * Optional exit details recorded when archiving a student (Milestone 9 QA —
 * BUG-005). Every field is optional; omitting a field leaves it unchanged on
 * the backend.
 */
export interface SoftDeleteExitDetails {
  exitDate?: string | null;
  exitReason?: string | null;
  exitRemarks?: string | null;
}

export interface StudentCreatedResponse {
  id: string;
  admissionNumber: string;
  status: StudentStatus;
  createdDate: string;
}

export interface StudentDetail {
  id: string;
  admissionNumber: string;
  firstName: string;
  lastName: string | null;
  gender: Gender;
  dateOfBirth: string;
  bloodGroup: string | null;
  religion: string | null;
  nationality: string | null;
  aadhaarNumber: string | null;
  phoneNumber: string | null;
  guardianName: string | null;
  guardianRelationship: string | null;
  guardianPhone: string | null;
  guardianAddress: string | null;
  schoolName: string | null;
  standard: string | null;
  medium: string | null;
  previousSchool: string | null;
  medicalConditions: string | null;
  allergies: string | null;
  disability: string | null;
  emergencyNotes: string | null;
  admissionDate: string;
  exitDate: string | null;
  exitReason: string | null;
  exitRemarks: string | null;
  status: StudentStatus;
  hasProfilePhoto: boolean;
  createdDate: string;
  updatedDate: string;
}

/** Compact student row returned by GET /api/v1/students (Milestone 8). */
export interface StudentSummary {
  id: string;
  admissionNumber: string;
  firstName: string;
  lastName: string | null;
  gender: Gender;
  dateOfBirth: string;
  status: StudentStatus;
  schoolName: string | null;
  standard: string | null;
  admissionDate: string;
  /** Only populated for archived rows from GET /students/inactive (Milestone 9 QA — BUG-007). */
  deletedDate: string | null;
}

/** Query parameters for GET /api/v1/students. All filters are optional. */
export interface StudentListParams {
  search?: string;
  gender?: Gender | '';
  status?: StudentStatus | '';
  admissionYear?: number | null;
  school?: string;
  ageMin?: number | null;
  ageMax?: number | null;
  page?: number;
  size?: number;
  sort?: string;
}

export interface StudentDocumentMeta {
  id: string;
  documentType: DocumentType;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  uploadedDate: string;
}

export interface PendingDocumentUpload {
  file: File;
  documentType: DocumentType;
  previewUrl?: string;
}

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  [DocumentType.Photograph]: 'Photograph',
  [DocumentType.AadhaarCard]: 'Aadhaar card',
  [DocumentType.BirthCertificate]: 'Birth certificate',
  [DocumentType.MedicalCertificate]: 'Medical certificate',
  [DocumentType.MarkSheet]: 'Mark sheet',
  [DocumentType.TransferCertificate]: 'Transfer certificate',
  [DocumentType.IdentityProof]: 'Identity proof',
  [DocumentType.Other]: 'Other',
};

export const GENDER_LABELS: Record<Gender, string> = {
  [Gender.Male]: 'Male',
  [Gender.Female]: 'Female',
  [Gender.Other]: 'Other',
};
