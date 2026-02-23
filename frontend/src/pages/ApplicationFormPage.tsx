import { useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  useApplication,
  useCreateApplication,
  useUpdateApplication,
} from '../features/applications/useApplications'
import { ALL_STATUSES } from '../features/applications/types'
import type { CreateApplicationDto } from '../features/applications/types'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorAlert from '../components/ErrorAlert'

const schema = z.object({
  company: z.string().min(1, 'Company is required'),
  jobTitle: z.string().min(1, 'Job title is required'),
  location: z.string().optional(),
  jobPostUrl: z
    .string()
    .optional()
    .refine(
      (val) => !val || val === '' || z.string().url().safeParse(val).success,
      { message: 'Must be a valid URL' }
    ),
  deadline: z.string().optional(),
  appliedDate: z.string().optional(),
  status: z.enum(['SAVED', 'APPLIED', 'OA', 'INTERVIEW', 'REJECTED', 'OFFER']),
  notes: z.string().optional(),
})

type FormData = z.infer<typeof schema>

interface FieldErrorProps {
  message?: string
}

function FieldError({ message }: FieldErrorProps) {
  if (!message) return null
  return <p className="mt-1 text-xs text-red-600">{message}</p>
}

export default function ApplicationFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEditMode = Boolean(id)
  const applicationId = id ? parseInt(id, 10) : 0

  const navigate = useNavigate()

  const {
    data: existing,
    isLoading: isLoadingExisting,
    isError: isLoadError,
    error: loadError,
  } = useApplication(applicationId)

  const createMutation = useCreateApplication()
  const updateMutation = useUpdateApplication()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      company: '',
      jobTitle: '',
      location: '',
      jobPostUrl: '',
      deadline: '',
      appliedDate: '',
      status: 'SAVED',
      notes: '',
    },
  })

  // Pre-fill form in edit mode once data is loaded
  useEffect(() => {
    if (isEditMode && existing) {
      reset({
        company: existing.company,
        jobTitle: existing.jobTitle,
        location: existing.location ?? '',
        jobPostUrl: existing.jobPostUrl ?? '',
        deadline: existing.deadline ?? '',
        appliedDate: existing.appliedDate ?? '',
        status: existing.status,
        notes: existing.notes ?? '',
      })
    }
  }, [existing, isEditMode, reset])

  async function onSubmit(formData: FormData) {
    // Build the DTO — strip empty optional strings to undefined
    const dto: CreateApplicationDto = {
      company: formData.company,
      jobTitle: formData.jobTitle,
      location: formData.location || undefined,
      jobPostUrl: formData.jobPostUrl || undefined,
      deadline: formData.deadline || undefined,
      appliedDate: formData.appliedDate || undefined,
      status: formData.status,
      notes: formData.notes || undefined,
    }

    if (isEditMode) {
      await updateMutation.mutateAsync({ id: applicationId, data: dto })
      navigate(`/applications/${applicationId}`)
    } else {
      const created = await createMutation.mutateAsync(dto)
      navigate(`/applications/${created.id}`)
    }
  }

  // In edit mode: show spinner while loading the existing application
  if (isEditMode && isLoadingExisting) return <LoadingSpinner />

  if (isEditMode && isLoadError) {
    const message =
      loadError instanceof Error ? loadError.message : 'Failed to load application.'
    return (
      <div className="space-y-4">
        <ErrorAlert message={message} />
        <Link to="/applications" className="text-sm text-blue-600 hover:underline">
          &larr; Back to Applications
        </Link>
      </div>
    )
  }

  const mutationError = isEditMode ? updateMutation.error : createMutation.error
  const isPending = isEditMode ? updateMutation.isPending : createMutation.isPending

  const labelClass = 'block text-sm font-medium text-gray-700'
  const inputClass =
    'mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500'

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      {/* Header */}
      <div>
        <Link to="/applications" className="text-sm text-blue-600 hover:underline">
          &larr; Back to Applications
        </Link>
        <h1 className="mt-3 text-2xl font-bold text-gray-900">
          {isEditMode ? 'Edit Application' : 'New Application'}
        </h1>
      </div>

      {/* Mutation error */}
      {mutationError && (
        <ErrorAlert
          message={
            mutationError instanceof Error
              ? mutationError.message
              : 'Something went wrong. Please try again.'
          }
        />
      )}

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
        {/* Company */}
        <div>
          <label htmlFor="company" className={labelClass}>
            Company <span className="text-red-500">*</span>
          </label>
          <input
            id="company"
            type="text"
            {...register('company')}
            className={inputClass}
            placeholder="Acme Corp"
          />
          <FieldError message={errors.company?.message} />
        </div>

        {/* Job Title */}
        <div>
          <label htmlFor="jobTitle" className={labelClass}>
            Job Title <span className="text-red-500">*</span>
          </label>
          <input
            id="jobTitle"
            type="text"
            {...register('jobTitle')}
            className={inputClass}
            placeholder="Software Engineer"
          />
          <FieldError message={errors.jobTitle?.message} />
        </div>

        {/* Location */}
        <div>
          <label htmlFor="location" className={labelClass}>
            Location
          </label>
          <input
            id="location"
            type="text"
            {...register('location')}
            className={inputClass}
            placeholder="San Francisco, CA (or Remote)"
          />
          <FieldError message={errors.location?.message} />
        </div>

        {/* Job Post URL */}
        <div>
          <label htmlFor="jobPostUrl" className={labelClass}>
            Job Post URL
          </label>
          <input
            id="jobPostUrl"
            type="url"
            {...register('jobPostUrl')}
            className={inputClass}
            placeholder="https://example.com/jobs/123"
          />
          <FieldError message={errors.jobPostUrl?.message} />
        </div>

        {/* Deadline + Applied Date */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="deadline" className={labelClass}>
              Deadline
            </label>
            <input
              id="deadline"
              type="date"
              {...register('deadline')}
              className={inputClass}
            />
            <FieldError message={errors.deadline?.message} />
          </div>
          <div>
            <label htmlFor="appliedDate" className={labelClass}>
              Applied Date
            </label>
            <input
              id="appliedDate"
              type="date"
              {...register('appliedDate')}
              className={inputClass}
            />
            <FieldError message={errors.appliedDate?.message} />
          </div>
        </div>

        {/* Status */}
        <div>
          <label htmlFor="status" className={labelClass}>
            Status <span className="text-red-500">*</span>
          </label>
          <select id="status" {...register('status')} className={inputClass}>
            {ALL_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <FieldError message={errors.status?.message} />
        </div>

        {/* Notes */}
        <div>
          <label htmlFor="notes" className={labelClass}>
            Notes
          </label>
          <textarea
            id="notes"
            rows={4}
            {...register('notes')}
            className={inputClass}
            placeholder="Any relevant notes about this application..."
          />
          <FieldError message={errors.notes?.message} />
        </div>

        {/* Submit */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <Link
            to="/applications"
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={isSubmitting || isPending}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {isPending
              ? isEditMode
                ? 'Saving...'
                : 'Creating...'
              : isEditMode
              ? 'Save Changes'
              : 'Create Application'}
          </button>
        </div>
      </form>
    </div>
  )
}
