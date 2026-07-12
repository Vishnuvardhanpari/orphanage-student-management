/** Browser storage keys used across the application. */
export const STORAGE_KEYS = {
  /** JWT access token. */
  accessToken: 'oms.access_token',
  /** Opaque refresh token. */
  refreshToken: 'oms.refresh_token',
  /** Cached authenticated user profile. */
  currentUser: 'oms.current_user',
  /** Theme preference: 'light' | 'dark'. */
  themeMode: 'oms.theme_mode',
} as const;
