export enum AuthProvider {
  Local = 'LOCAL',
  Google = 'GOOGLE',
  LocalGoogle = 'LOCAL_GOOGLE',
}

/** User management profile returned by /api/v1/users. */
export interface ManagedUser {
  id: string;
  username: string;
  email: string;
  role: string;
  enabled: boolean;
  authProvider: AuthProvider | string;
  accountNonLocked: boolean;
  lastLoginAt: string | null;
  createdDate: string;
  updatedDate: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  role: string;
  authProvider: AuthProvider | string;
  password?: string | null;
}

export interface UpdateUserRequest {
  username: string;
  email: string;
  role: string;
}

export interface ResetPasswordRequest {
  newPassword: string;
}

export interface UserListParams {
  search?: string;
  role?: string;
  enabled?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
