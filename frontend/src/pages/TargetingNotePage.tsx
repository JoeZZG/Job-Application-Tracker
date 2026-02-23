import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTargetingNote, useUpsertTargetingNote } from '../features/applications/useTargetingNote'
import type { TargetingNoteDto } from '../features/applications/types'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorAlert from '../components/ErrorAlert'

const schema = z.object({
  mustHaveKeywords: z.string().optional(),
  niceToHaveKeywords: z.string().optional(),
  customBulletIdeas: z.string().optional(),
  jobDescriptionExcerpt: z.string().optional(),
  matchNotes: z.string().optional(),
})

type FormData = z.infer<typeof schema>

const labelClass = 'block text-sm font-medium text-gray-700'
const textareaClass =
  'mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500'

export default function TargetingNotePage() {
  const { id } = useParams<{ id: string }>()
  const applicationId = id ? parseInt(id, 10) : 0

  const [savedSuccess, setSavedSuccess] = useState(false)

  const { data, isLoading, isError, error } = useTargetingNote(applicationId)
  const upsertMutation = useUpsertTargetingNote(applicationId)

  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      mustHaveKeywords: '',
      niceToHaveKeywords: '',
      customBulletIdeas: '',
      jobDescriptionExcerpt: '',
      matchNotes: '',
    },
  })

  // Pre-populate form once the note is fetched
  useEffect(() => {
    if (data) {
      reset({
        mustHaveKeywords: data.mustHaveKeywords ?? '',
        niceToHaveKeywords: data.niceToHaveKeywords ?? '',
        customBulletIdeas: data.customBulletIdeas ?? '',
        jobDescriptionExcerpt: data.jobDescriptionExcerpt ?? '',
        matchNotes: data.matchNotes ?? '',
      })
    }
  }, [data, reset])

  // Determine whether the error is a true failure (not a 404 — 404 just means no note yet)
  const is404 =
    isError &&
    (error as { response?: { status?: number } })?.response?.status === 404

  if (isLoading) return <LoadingSpinner />

  if (isError && !is404) {
    const message =
      error instanceof Error ? error.message : 'Failed to load targeting note.'
    return (
      <div className="space-y-4">
        <ErrorAlert message={message} />
        <Link
          to={`/applications/${applicationId}`}
          className="text-sm text-blue-600 hover:underline"
        >
          &larr; Back to application
        </Link>
      </div>
    )
  }

  async function onSubmit(formData: FormData) {
    setSavedSuccess(false)
    const dto: TargetingNoteDto = {
      mustHaveKeywords: formData.mustHaveKeywords || undefined,
      niceToHaveKeywords: formData.niceToHaveKeywords || undefined,
      customBulletIdeas: formData.customBulletIdeas || undefined,
      jobDescriptionExcerpt: formData.jobDescriptionExcerpt || undefined,
      matchNotes: formData.matchNotes || undefined,
    }
    await upsertMutation.mutateAsync(dto)
    setSavedSuccess(true)
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      {/* Back link */}
      <Link
        to={`/applications/${applicationId}`}
        className="text-sm text-blue-600 hover:underline"
      >
        &larr; Back to application
      </Link>

      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Resume Targeting</h1>
        <p className="mt-1 text-sm text-gray-500">
          Tailor your resume to this role by tracking key keywords and bullet ideas.
        </p>
      </div>

      {/* Success banner */}
      {savedSuccess && (
        <div
          role="status"
          className="rounded-md border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700"
        >
          Targeting note saved successfully.
        </div>
      )}

      {/* Mutation error */}
      {upsertMutation.isError && (
        <ErrorAlert
          message={
            upsertMutation.error instanceof Error
              ? upsertMutation.error.message
              : 'Failed to save targeting note. Please try again.'
          }
        />
      )}

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
        {/* Must-Have Keywords */}
        <div>
          <label htmlFor="mustHaveKeywords" className={labelClass}>
            Must-Have Keywords
          </label>
          <textarea
            id="mustHaveKeywords"
            rows={3}
            {...register('mustHaveKeywords')}
            className={textareaClass}
            placeholder="e.g. TypeScript, React, REST APIs"
          />
        </div>

        {/* Nice-to-Have Keywords */}
        <div>
          <label htmlFor="niceToHaveKeywords" className={labelClass}>
            Nice-to-Have Keywords
          </label>
          <textarea
            id="niceToHaveKeywords"
            rows={3}
            {...register('niceToHaveKeywords')}
            className={textareaClass}
            placeholder="e.g. GraphQL, AWS, Docker"
          />
        </div>

        {/* Custom Bullet Ideas */}
        <div>
          <label htmlFor="customBulletIdeas" className={labelClass}>
            Custom Bullet Ideas
          </label>
          <textarea
            id="customBulletIdeas"
            rows={4}
            {...register('customBulletIdeas')}
            className={textareaClass}
            placeholder="Draft resume bullets tailored to this role..."
          />
        </div>

        {/* Job Description Excerpt */}
        <div>
          <label htmlFor="jobDescriptionExcerpt" className={labelClass}>
            Job Description Excerpt
          </label>
          <textarea
            id="jobDescriptionExcerpt"
            rows={5}
            {...register('jobDescriptionExcerpt')}
            className={textareaClass}
            placeholder="Paste the key parts of the job description here..."
          />
        </div>

        {/* Match Notes */}
        <div>
          <label htmlFor="matchNotes" className={labelClass}>
            Match Notes
          </label>
          <textarea
            id="matchNotes"
            rows={3}
            {...register('matchNotes')}
            className={textareaClass}
            placeholder="Notes on how your experience maps to this role..."
          />
        </div>

        {/* Submit */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <Link
            to={`/applications/${applicationId}`}
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={isSubmitting || upsertMutation.isPending}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {upsertMutation.isPending ? 'Saving...' : 'Save Note'}
          </button>
        </div>
      </form>
    </div>
  )
}
