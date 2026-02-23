import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import apiClient from '../../lib/apiClient'
import type { Application, ApplicationStatus, CreateApplicationDto, UpdateApplicationDto } from './types'

export function useApplications(status?: ApplicationStatus) {
  return useQuery({
    queryKey: ['applications', status ?? 'all'],
    queryFn: () =>
      apiClient.get<Application[]>('/applications', { params: status ? { status } : {} }).then(r => r.data),
  })
}

export function useApplication(id: number) {
  return useQuery({
    queryKey: ['applications', id],
    queryFn: () => apiClient.get<Application>(`/applications/${id}`).then(r => r.data),
  })
}

export function useCreateApplication() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateApplicationDto) =>
      apiClient.post<Application>('/applications', data).then(r => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['applications'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useUpdateApplication() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateApplicationDto }) =>
      apiClient.put<Application>(`/applications/${id}`, data).then(r => r.data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ['applications'] })
      qc.invalidateQueries({ queryKey: ['applications', id] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useDeleteApplication() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => apiClient.delete(`/applications/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['applications'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}
