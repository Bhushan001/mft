import { UserRole } from './auth.models';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';

export interface UserResponse {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  status: UserStatus;
  tenantId: string;
  phone?: string;
  avatarUrl?: string;
  timezone?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  role: UserRole;
  phone?: string;
  timezone?: string;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  timezone?: string;
  role?: UserRole;
  status?: UserStatus;
}
