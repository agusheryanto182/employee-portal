export type Role = 'ADMIN' | 'EMPLOYEE'
export type UserStatus = 'PENDING' | 'ACTIVE' | 'INACTIVE'

export interface User {
  id: string
  email: string
  fullName: string
  phone: string | null
  role: Role
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: User
}

export type LoginFailureReason =
  | 'INVALID_CREDENTIALS'
  | 'ACCOUNT_PENDING'
  | 'ACCOUNT_INACTIVE'
  | 'WRONG_PORTAL'
  | 'RATE_LIMITED'

export interface LoginHistoryEntry {
  id: string
  userId: string | null
  email: string
  success: boolean
  failureReason: LoginFailureReason | null
  ipAddress: string | null
  userAgent: string | null
  createdAt: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type AccountTokenType = 'INVITE' | 'RESET'

export interface AccountTokenInfo {
  type: AccountTokenType
  email: string
  fullName: string
}
