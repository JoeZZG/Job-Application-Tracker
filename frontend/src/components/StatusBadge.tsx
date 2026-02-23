export type ApplicationStatus = 'SAVED' | 'APPLIED' | 'OA' | 'INTERVIEW' | 'REJECTED' | 'OFFER'

const colours: Record<ApplicationStatus, string> = {
  SAVED:     'bg-gray-100 text-gray-700',
  APPLIED:   'bg-blue-100 text-blue-700',
  OA:        'bg-yellow-100 text-yellow-700',
  INTERVIEW: 'bg-purple-100 text-purple-700',
  REJECTED:  'bg-red-100 text-red-700',
  OFFER:     'bg-green-100 text-green-700',
}

interface StatusBadgeProps {
  status: ApplicationStatus
}

export default function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${colours[status]}`}
    >
      {status}
    </span>
  )
}
