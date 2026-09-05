import { Role } from './user';

export interface UserSearchParams {
  active?: boolean;
  role?: Role;
  page?: number;
  size?: number;
}

export interface UpdateUserStatusRequest {
  active: boolean;
}
