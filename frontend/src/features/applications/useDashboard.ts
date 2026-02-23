import { useQuery } from '@tanstack/react-query'
import apiClient from '../../lib/apiClient'
import type { DashboardSummary } from './types'

export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard'],
    queryFn: () => apiClient.get<DashboardSummary>('/applications/dashboard/summary').then(r => r.data),
  })
}
