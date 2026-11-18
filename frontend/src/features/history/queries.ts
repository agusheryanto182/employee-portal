import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { LoginHistoryEntry, Page } from '@/lib/types'

export function useMyLoginHistory(page: number, size: number) {
  return useQuery({
    queryKey: ['me', 'login-history', page, size],
    queryFn: async () =>
      (await api.get<Page<LoginHistoryEntry>>('/me/login-history', { params: { page, size } })).data,
    placeholderData: keepPreviousData, // keep the old page on screen while the next one loads
  })
}
