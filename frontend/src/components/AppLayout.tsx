import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../features/auth/AuthContext'
import apiClient from '../lib/apiClient'

interface Notification {
  id: number
  userId: number
  type: string
  title: string
  message: string
  isRead: boolean
  createdAt: string
}

function BellIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className="h-5 w-5"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 0 1-3.46 0" />
    </svg>
  )
}

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const { data: notifications } = useQuery<Notification[]>({
    queryKey: ['notifications'],
    queryFn: () => apiClient.get<Notification[]>('/notifications').then((r) => r.data),
    refetchInterval: 30_000,
  })

  const unreadCount = Array.isArray(notifications) ? notifications.filter((n) => !n.isRead).length : 0

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Navbar */}
      <nav className="border-b border-gray-200 bg-white">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          {/* Brand */}
          <Link
            to="/dashboard"
            className="text-lg font-bold text-blue-600 hover:text-blue-700"
          >
            JobTracker
          </Link>

          {/* Center nav links */}
          <div className="flex items-center gap-6">
            <Link
              to="/dashboard"
              className="text-sm font-medium text-gray-600 hover:text-gray-900"
            >
              Dashboard
            </Link>
            <Link
              to="/applications"
              className="text-sm font-medium text-gray-600 hover:text-gray-900"
            >
              Applications
            </Link>
            <Link
              to="/notifications"
              className="text-sm font-medium text-gray-600 hover:text-gray-900"
            >
              Notifications
            </Link>
          </div>

          {/* Right side */}
          <div className="flex items-center gap-4">
            {/* Notification bell */}
            <Link
              to="/notifications"
              className="relative text-gray-500 hover:text-gray-700"
              aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ''}`}
            >
              <BellIcon />
              {unreadCount > 0 && (
                <span className="absolute -right-1.5 -top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </Link>

            {/* User email */}
            {user && (
              <span className="hidden text-sm text-gray-600 sm:block">{user.email}</span>
            )}

            {/* Logout */}
            <button
              type="button"
              onClick={handleLogout}
              className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Logout
            </button>
          </div>
        </div>
      </nav>

      {/* Page content */}
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  )
}
