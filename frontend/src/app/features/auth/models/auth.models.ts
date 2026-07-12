import { UserRole } from '../../../core/enums/user-role';

/** Authenticated user profile from auth endpoints. */
export interface AuthUser {
  id: string;
  username: string;
  email: string;
  role: UserRole;
}

/** Login / Google / refresh response. */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface GoogleLoginRequest {
  idToken: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface LogoutRequest {
  refreshToken?: string;
}
