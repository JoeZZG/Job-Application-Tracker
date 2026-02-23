import { Link } from 'react-router-dom'
import { useDashboard } from '../features/applications/useDashboard'
import { ALL_STATUSES } from '../features/applications/types'
import type { ApplicationStatus } from '../features/applications/types'
import StatusBadge from '../components/StatusBadge'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorAlert from '../components/ErrorAlert'
import EmptyState from '../components/EmptyState'

interface SummaryCardProps {
  label: string
  count: number
  colorClass: string
}

function SummaryCard({ label, count, colorClass }: SummaryCardProps) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
      <p className="text-sm font-medium text-gray-500">{label}</p>
      <p className={`mt-1 text-3xl font-bold ${colorClass}`}>{count}</p>
    </div>
  )
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

export default function DashboardPage() {
  const { data, isLoading, isError, error } = useDashboard()

  if (isLoading) return <LoadingSpinner />

  if (isError) {
    const message =
      error instanceof Error ? error.message : 'Failed to load dashboard. Please try again.'
    return <ErrorAlert message={message} />
  }

  if (!data) return null

  const byStatus = data.byStatus

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-1 text-sm text-gray-500">Your job search at a glance.</p>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <SummaryCard label="Total Applications" count={data.total} colorClass="text-gray-900" />
        <SummaryCard
          label="Interviews"
          count={byStatus['INTERVIEW'] ?? 0}
          colorClass="text-purple-600"
        />
        <SummaryCard label="Offers" count={byStatus['OFFER'] ?? 0} colorClass="text-green-600" />
        <SummaryCard
          label="Rejected"
          count={byStatus['REJECTED'] ?? 0}
          colorClass="text-red-600"
        />
      </div>

      {/* By Status breakdown */}
      <section>
        <h2 className="mb-3 text-lg font-semibold text-gray-900">By Status</h2>
        <div className="flex flex-wrap gap-3">
          {ALL_STATUSES.map((status: ApplicationStatus) => (
            <div
              key={status}
              className="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 shadow-sm"
            >
              <StatusBadge status={status} />
              <span className="text-sm font-semibold text-gray-700">
                {byStatus[status] ?? 0}
              </span>
            </div>
          ))}
        </div>
      </section>

      {/* Upcoming Deadlines */}
      <section>
        <h2 className="mb-3 text-lg font-semibold text-gray-900">Upcoming Deadlines</h2>
        {data.upcomingDeadlines.length === 0 ? (
          <EmptyState message="No upcoming deadlines." />
        ) : (
          <ul className="divide-y divide-gray-100 rounded-lg border border-gray-200 bg-white shadow-sm">
            {data.upcomingDeadlines.slice(0, 5).map((item) => (
              <li key={item.id}>
                <Link
                  to={`/applications/${item.id}`}
                  className="flex items-center justify-between px-4 py-3 hover:bg-gray-50"
                >
                  <div>
                    <p className="text-sm font-medium text-gray-900">{item.company}</p>
                    <p className="text-xs text-gray-500">{item.jobTitle}</p>
                  </div>
                  <span className="text-xs font-medium text-orange-600">
                    {formatDate(item.deadline)}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Recently Updated */}
      <section>
        <h2 className="mb-3 text-lg font-semibold text-gray-900">Recently Updated</h2>
        {data.recentlyUpdated.length === 0 ? (
          <EmptyState message="No recent activity." />
        ) : (
          <ul className="divide-y divide-gray-100 rounded-lg border border-gray-200 bg-white shadow-sm">
            {data.recentlyUpdated.slice(0, 5).map((item) => (
              <li key={item.id}>
                <Link
                  to={`/applications/${item.id}`}
                  className="flex items-center justify-between px-4 py-3 hover:bg-gray-50"
                >
                  <div>
                    <p className="text-sm font-medium text-gray-900">{item.company}</p>
                    <p className="text-xs text-gray-500">{item.jobTitle}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <StatusBadge status={item.status} />
                    <span className="text-xs text-gray-400">{formatDate(item.updatedAt)}</span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
