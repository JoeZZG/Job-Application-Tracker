export type ApplicationStatus = 'SAVED' | 'APPLIED' | 'OA' | 'INTERVIEW' | 'REJECTED' | 'OFFER'

export const ALL_STATUSES: ApplicationStatus[] = ['SAVED', 'APPLIED', 'OA', 'INTERVIEW', 'REJECTED', 'OFFER']

export interface Application {
  id: number
  userId: number
  company: string
  jobTitle: string
  location: string | null
  jobPostUrl: string | null
  deadline: string | null
  appliedDate: string | null
  status: ApplicationStatus
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateApplicationDto {
  company: string
  jobTitle: string
  location?: string
  jobPostUrl?: string
  deadline?: string
  appliedDate?: string
  status?: ApplicationStatus
  notes?: string
}

export type UpdateApplicationDto = Partial<CreateApplicationDto>

export interface TargetingNote {
  id: number
  applicationId: number
  mustHaveKeywords: string | null
  niceToHaveKeywords: string | null
  customBulletIdeas: string | null
  jobDescriptionExcerpt: string | null
  matchNotes: string | null
  createdAt: string
  updatedAt: string
}

export interface TargetingNoteDto {
  mustHaveKeywords?: string
  niceToHaveKeywords?: string
  customBulletIdeas?: string
  jobDescriptionExcerpt?: string
  matchNotes?: string
}

export interface DashboardSummary {
  total: number
  byStatus: Record<ApplicationStatus, number>
  upcomingDeadlines: Array<{ id: number; company: string; jobTitle: string; deadline: string }>
  recentlyUpdated: Array<{ id: number; company: string; jobTitle: string; status: ApplicationStatus; updatedAt: string }>
}
