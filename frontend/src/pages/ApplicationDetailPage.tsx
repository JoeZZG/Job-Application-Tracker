import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useApplication, useDeleteApplication } from '../features/applications/useApplications'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorAlert from '../components/ErrorAlert'
import StatusBadge from '../components/StatusBadge'
import ConfirmDialog from '../components/ConfirmDialog'

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

interface DetailRowProps {
  label: string
  value: React.ReactNode
}

function DetailRow({ label, value }: DetailRowProps) {
  return (
    <div className="grid grid-cols-3 gap-4 py-3">
      <dt className="text-sm font-medium text-gray-500">{label}</dt>
      <dd className="col-span-2 text-sm text-gray-900">{value}</dd>
    </div>
  )
}

export default function ApplicationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const applicationId = id ? parseInt(id, 10) : 0
  const navigate = useNavigate()

  const [confirmOpen, setConfirmOpen] = useState(false)

  const { data, isLoading, isError, error } = useApplication(applicationId)
  const deleteMutation = useDeleteApplication()

  if (isLoading) return <LoadingSpinner />

  if (isError) {
    const message =
      error instanceof Error ? error.message : 'Failed to load application. Please try again.'
    return (
      <div className="space-y-4">
        <ErrorAlert message={message} />
        <Link to="/applications" className="text-sm text-blue-600 hover:underline">
          &larr; Back to Applications
        </Link>
      </div>
    )
  }

  if (!data) {
    return (
      <div className="space-y-4">
        <ErrorAlert message="Application not found." />
        <Link to="/applications" className="text-sm text-blue-600 hover:underline">
          &larr; Back to Applications
        </Link>
      </div>
    )
  }

  async function handleDelete() {
    await deleteMutation.mutateAsync(applicationId)
    navigate('/applications')
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      {/* Back link */}
      <Link to="/applications" className="text-sm text-blue-600 hover:underline">
        &larr; Back to Applications
      </Link>

      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{data.jobTitle}</h1>
          <p className="mt-1 text-base text-gray-600">{data.company}</p>
        </div>
        <StatusBadge status={data.status} />
      </div>

      {/* Delete error */}
      {deleteMutation.isError && (
        <ErrorAlert
          message={
            deleteMutation.error instanceof Error
              ? deleteMutation.error.message
              : 'Failed to delete application. Please try again.'
          }
        />
      )}

      {/* Detail card */}
      <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
        <dl className="divide-y divide-gray-100 px-6">
          <DetailRow label="Company" value={data.company} />
          <DetailRow label="Job Title" value={data.jobTitle} />
          <DetailRow label="Status" value={<StatusBadge status={data.status} />} />
          <DetailRow label="Location" value={data.location ?? '—'} />
          <DetailRow
            label="Job Post URL"
            value={
              data.jobPostUrl ? (
                <a
                  href={data.jobPostUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="truncate text-blue-600 hover:underline"
                >
                  {data.jobPostUrl}
                </a>
              ) : (
                '—'
              )
            }
          />
          <DetailRow label="Deadline" value={formatDate(data.deadline)} />
          <DetailRow label="Applied Date" value={formatDate(data.appliedDate)} />
          <DetailRow
            label="Notes"
            value={
              data.notes ? (
                <p className="whitespace-pre-wrap">{data.notes}</p>
              ) : (
                '—'
              )
            }
          />
          <DetailRow label="Created" value={formatDate(data.createdAt)} />
          <DetailRow label="Last Updated" value={formatDate(data.updatedAt)} />
        </dl>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-3">
        <Link
          to={`/applications/${applicationId}/edit`}
          className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          Edit
        </Link>
        <Link
          to={`/applications/${applicationId}/targeting-note`}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          Resume Targeting
        </Link>
        <button
          type="button"
          onClick={() => setConfirmOpen(true)}
          className="ml-auto rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
        >
          Delete
        </button>
      </div>

      {/* Delete confirmation dialog */}
      <ConfirmDialog
        isOpen={confirmOpen}
        title="Delete Application"
        message={`Are you sure you want to delete the application for ${data.jobTitle} at ${data.company}? This action cannot be undone.`}
        onConfirm={handleDelete}
        onCancel={() => setConfirmOpen(false)}
        isLoading={deleteMutation.isPending}
      />
    </div>
  )
}
