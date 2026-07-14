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

export interface StudentCreatedResponse {
  id: string;
  admissionNumber: string;
  status: StudentStatus;
  createdDate: string;
}

export interface PendingDocumentUpload {
  file: File;
  documentType: DocumentType;
  previewUrl?: string;
}
