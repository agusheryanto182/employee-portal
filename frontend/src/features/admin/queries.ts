import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { localDayRange } from '@/lib/format'
import { session } from '@/lib/session'
import type { LoginHistoryEntry, Page, Role, User, UserStatus } from '@/lib/types'

export const ADMIN_PAGE_SIZE = 20

export interface UserFilters {
  q?: string
  status?: UserStatus
  role?: Role
  page?: number
}

export interface HistoryFilters {
  userId?: string
  outcome?: 'success' | 'failed'
  /** Local days, `yyyy-mm-dd`. */
  from?: string
  to?: string
  page?: number
}

export interface UserInput {
  email: string
  fullName: string
  phone: string
  role: Role
}

/** Query keys in one place, so mutations know what to refresh. */
export const adminKeys = {
  users: ['admin', 'users'] as const,
  userList: (filters: UserFilters) => ['admin', 'users', 'list', filters] as const,
  user: (id: string) => ['admin', 'users', 'detail', id] as const,
  history: ['admin', 'login-history'] as const,
  historyList: (filters: HistoryFilters, size: number) => ['admin', 'login-history', filters, size] as const,
}

export function useUsers(filters: UserFilters) {
  return useQuery({
    queryKey: adminKeys.userList(filters),
    queryFn: async () =>
      (
        await api.get<Page<User>>('/admin/users', {
          params: {
            search: filters.q || undefined,
            status: filters.status,
            role: filters.role,
            page: filters.page ?? 0,
            size: ADMIN_PAGE_SIZE,
          },
        })
      ).data,
    placeholderData: keepPreviousData,
  })
}

export function useUser(id: string | undefined) {
  return useQuery({
    queryKey: adminKeys.user(id ?? ''),
    queryFn: async () => (await api.get<User>(`/admin/users/${id}`)).data,
    enabled: id !== undefined,
  })
}

export function useLoginHistory(filters: HistoryFilters, size = ADMIN_PAGE_SIZE) {
  return useQuery({
    queryKey: adminKeys.historyList(filters, size),
    queryFn: async () => {
      const { from, to } = localDayRange(filters.from, filters.to)
      return (
        await api.get<Page<LoginHistoryEntry>>('/admin/login-history', {
          params: {
            userId: filters.userId,
            success: filters.outcome === undefined ? undefined : filters.outcome === 'success',
            from,
            to,
            page: filters.page ?? 0,
            size,
          },
        })
      ).data
    },
    placeholderData: keepPreviousData,
  })
}

/** Stores the changed user and refreshes every list that may show it. */
function useUserChanged() {
  const queryClient = useQueryClient()
  return (user: User) => {
    queryClient.setQueryData(adminKeys.user(user.id), user)
    void queryClient.invalidateQueries({ queryKey: [...adminKeys.users, 'list'] })
    if (session.get()?.user.id === user.id) {
      session.updateUser(user) // the admin edited their own record: keep the top bar in sync
    }
  }
}

export function useCreateUser() {
  const changed = useUserChanged()
  return useMutation({
    mutationFn: async (input: UserInput) =>
      (await api.post<User>('/admin/users', { ...input, phone: input.phone || null })).data,
    onSuccess: changed,
  })
}

export function useUpdateUser(id: string) {
  const changed = useUserChanged()
  return useMutation({
    mutationFn: async (input: Partial<UserInput>) => (await api.patch<User>(`/admin/users/${id}`, input)).data,
    onSuccess: changed,
  })
}

export function useSetActive(id: string) {
  const changed = useUserChanged()
  return useMutation({
    mutationFn: async (active: boolean) =>
      (await api.post<User>(`/admin/users/${id}/${active ? 'activate' : 'deactivate'}`)).data,
    onSuccess: changed,
  })
}

/** Resend invite (PENDING) or password reset (ACTIVE); both email the user and return the link. */
export function useSendLink(id: string) {
  return useMutation({
    mutationFn: async (kind: 'invite' | 'reset') =>
      (await api.post<{ link: string }>(`/admin/users/${id}/${kind === 'invite' ? 'resend-invite' : 'reset-password'}`))
        .data.link,
  })
}
