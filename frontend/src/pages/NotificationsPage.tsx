import { useNotifications, useMarkNotificationRead } from '../features/notifications/useNotifications'
import type { Notification } from '../features/notifications/types'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorAlert from '../components/ErrorAlert'
import EmptyState from '../components/EmptyState'

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

interface NotificationCardProps {
  notification: Notification
  onMarkRead: (id: number) => void
  isMarking: boolean
}

function NotificationCard({ notification, onMarkRead, isMarking }: NotificationCardProps) {
  return (
    <div
      className={`flex items-start justify-between gap-4 rounded-lg border bg-white p-4 shadow-sm ${
        notification.isRead ? 'border-gray-200' : 'border-blue-200'
      }`}
    >
      <div className="min-w-0 flex-1 space-y-1">
        <div className="flex items-center gap-2">
          {!notification.isRead && (
            <span
              className="inline-block h-2 w-2 flex-shrink-0 rounded-full bg-blue-500"
              aria-label="Unread"
            />
          )}
          <p
            className={`text-sm font-medium ${
              notification.isRead ? 'text-gray-700' : 'text-gray-900'
            }`}
          >
            {notification.title}
          </p>
        </div>
        <p className="text-sm text-gray-600">{notification.message}</p>
        <p className="text-xs text-gray-400">{formatDateTime(notification.createdAt)}</p>
      </div>

      <button
        type="button"
        onClick={() => onMarkRead(notification.id)}
        disabled={notification.isRead || isMarking}
        className="flex-shrink-0 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
        aria-label={notification.isRead ? 'Already read' : 'Mark as read'}
      >
        {notification.isRead ? 'Read' : 'Mark as read'}
      </button>
    </div>
  )
}

export default function NotificationsPage() {
  const { data, isLoading, isError, error } = useNotifications()
  const markReadMutation = useMarkNotificationRead()

  const sorted = data
    ? [...data].sort((a, b) => {
        // Unread first
        if (a.isRead !== b.isRead) return a.isRead ? 1 : -1
        // Then by createdAt descending
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      })
    : []

  const unreadCount = data?.filter((n) => !n.isRead).length ?? 0

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Notifications</h1>
        <p className="mt-1 text-sm text-gray-500">
          {unreadCount > 0
            ? `${unreadCount} unread notification${unreadCount === 1 ? '' : 's'}`
            : 'All caught up.'}
        </p>
      </div>

      {/* Mark-as-read error */}
      {markReadMutation.isError && (
        <ErrorAlert
          message={
            markReadMutation.error instanceof Error
              ? markReadMutation.error.message
              : 'Failed to mark notification as read. Please try again.'
          }
        />
      )}

      {/* Async states */}
      {isLoading && <LoadingSpinner />}

      {isError && (
        <ErrorAlert
          message={
            error instanceof Error
              ? error.message
              : 'Failed to load notifications. Please try again.'
          }
        />
      )}

      {!isLoading && !isError && sorted.length === 0 && (
        <EmptyState message="No notifications yet. Check back after applying to jobs." />
      )}

      {!isLoading && !isError && sorted.length > 0 && (
        <ul className="space-y-3" aria-label="Notifications list">
          {sorted.map((notification) => (
            <li key={notification.id}>
              <NotificationCard
                notification={notification}
                onMarkRead={(id) => markReadMutation.mutate(id)}
                isMarking={markReadMutation.isPending}
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
