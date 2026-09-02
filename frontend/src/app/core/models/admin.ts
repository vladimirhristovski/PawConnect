export interface UserSearchParams {
  active?: boolean;
  role?: string;
  page?: number;
  size?: number;
}

export interface UpdateUserStatusRequest {
  active: boolean;
}
