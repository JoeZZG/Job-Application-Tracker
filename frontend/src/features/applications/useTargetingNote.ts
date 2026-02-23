import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import apiClient from '../../lib/apiClient'
import type { TargetingNote, TargetingNoteDto } from './types'

export function useTargetingNote(applicationId: number) {
  return useQuery({
    queryKey: ['targeting-note', applicationId],
    queryFn: () =>
      apiClient.get<TargetingNote>(`/applications/${applicationId}/targeting-note`).then(r => r.data),
    // 404 means no note yet — treat as null data
    retry: (failureCount, error: unknown) => {
      const status = (error as { response?: { status?: number } })?.response?.status
      if (status === 404) return false
      return failureCount < 1
    },
  })
}

export function useUpsertTargetingNote(applicationId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: TargetingNoteDto) =>
      apiClient.put<TargetingNote>(`/applications/${applicationId}/targeting-note`, data).then(r => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['targeting-note', applicationId] })
    },
  })
}
