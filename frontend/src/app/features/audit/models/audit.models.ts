export type AuditModule = 'AUTH' | 'STUDENT' | 'DOCUMENT' | 'REPORT';

export type AuditAction =
  | 'LOGIN'
  | 'LOGOUT'
  | 'CREATED'
  | 'UPDATED'
  | 'DELETED'
  | 'RESTORED'
  | 'UPLOADED'
  | 'REPLACED'
  | 'GENERATED';

export interface AuditLog {
  id: string;
  module: AuditModule;
  action: AuditAction;
  entityId: string | null;
  description: string;
  username: string;
  ipAddress: string | null;
  createdDate: string;
}

export interface AuditListParams {
  search?: string;
  module?: AuditModule | '';
  action?: AuditAction | '';
  username?: string;
  entityId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export const AUDIT_MODULES: AuditModule[] = ['AUTH', 'STUDENT', 'DOCUMENT', 'REPORT'];

export const AUDIT_ACTIONS: AuditAction[] = [
  'LOGIN',
  'LOGOUT',
  'CREATED',
  'UPDATED',
  'DELETED',
  'RESTORED',
  'UPLOADED',
  'REPLACED',
  'GENERATED',
];
