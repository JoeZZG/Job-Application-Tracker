# frontend/

## Purpose

React 18 + TypeScript SPA for the Job Application Tracker. Served in production from a private S3 bucket via CloudFront. Communicates exclusively with the API gateway at `VITE_API_BASE_URL` — never directly with individual microservices.

## Key Files

| File | Role |
|---|---|
| `src/lib/apiClient.ts` | Single Axios instance. JWT injected from localStorage. 401 triggers logout callback. |
| `src/features/auth/AuthContext.tsx` | `AuthContext` + `useAuth` hook. `user`, `token`, `login()`, `logout()`. JWT persisted to `localStorage`. |
| `src/components/ProtectedRoute.tsx` | Checks `useAuth().token`; redirects to `/login` if absent. |
| `src/App.tsx` | React Router v6 route definitions. `<AuthProvider>` wraps entire tree. |
| `src/main.tsx` | Vite entry point. Mounts `<QueryClientProvider>` + `<AuthProvider>`. |
| `.env.example` | `VITE_API_BASE_URL` — set to ALB/CloudFront URL for production, `http://localhost:8080` for local dev. |
| `vite.config.ts` | Vite config. Local dev proxy not used — services run on separate ports. |

## Structure

```
frontend/
├── src/
│   ├── App.tsx                       # Route tree
│   ├── main.tsx                      # Entry point
│   ├── lib/
│   │   └── apiClient.ts              # Axios singleton
│   ├── features/                     # Feature co-location pattern
│   │   ├── auth/
│   │   │   └── AuthContext.tsx       # Auth state + JWT lifecycle
│   │   ├── applications/
│   │   │   ├── types.ts              # JobApplication, TargetingNote interfaces
│   │   │   ├── useApplications.ts    # CRUD mutations + list query
│   │   │   ├── useDashboard.ts       # Dashboard summary query
│   │   │   └── useTargetingNote.ts   # Resume note get/upsert
│   │   └── notifications/
│   │       ├── types.ts              # Notification interface
│   │       └── useNotifications.ts   # List + mark-as-read mutation
│   ├── pages/                        # Route-level components
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── ApplicationListPage.tsx
│   │   ├── ApplicationDetailPage.tsx
│   │   ├── ApplicationFormPage.tsx
│   │   ├── TargetingNotePage.tsx
│   │   └── NotificationsPage.tsx
│   └── components/                   # Shared UI primitives
│       ├── AppLayout.tsx             # Navbar (unread bell badge) + <Outlet />
│       ├── ProtectedRoute.tsx
│       ├── StatusBadge.tsx
│       ├── ConfirmDialog.tsx
│       ├── EmptyState.tsx
│       ├── ErrorAlert.tsx
│       └── LoadingSpinner.tsx
├── .env.example
├── index.html
├── package.json
├── tailwind.config.js
├── tsconfig.json
└── vite.config.ts
```

## Implementation Overview

- **Auth flow**: `LoginPage` → `AuthContext.login()` stores `token` + `user` to `localStorage` → Axios interceptor attaches `Authorization: Bearer <token>` on every request → on 401, `logout()` is called and user is redirected to `/login`.
- **Server state**: All API calls go through React Query hooks in `features/`. Query keys follow the pattern `['applications']`, `['application', id]`, `['dashboard']`, `['notifications']`. Mutations invalidate the relevant key after success.
- **Forms**: React Hook Form + Zod. Schema field names must exactly match backend DTO field names (camelCase). Validation errors display inline per-field.
- **Routing**: `<ProtectedRoute>` wraps all authenticated pages. Unauthenticated users are redirected to `/login` with the intended path preserved in `location.state`.
- **Production URL**: CloudFront distribution fronts both S3 (SPA) and ALB (API). The SPA uses relative-looking paths, and CloudFront behaviors forward `/auth/*`, `/applications*`, `/notifications*` to the ALB. No CORS issues between SPA and API.

## Implementation Details & Gotchas

- **`Array.isArray()` guard on API responses**: If CloudFront serves a non-API path (e.g., returning `index.html` for a missed path pattern), Axios returns an HTML string. Any `.filter()` or `.map()` call on the non-array string throws a TypeError and crashes the React tree with a blank page. Always guard: `Array.isArray(data) ? data.filter(...) : []`.
- **CloudFront path patterns**: `/applications*` matches `/applications` (exact) and `/applications/123`. `/applications/*` does NOT match `/applications` alone. Never use `/*` for paths whose base is a valid endpoint.
- **`VITE_API_BASE_URL`**: In production `.env.production`, set to `""` (empty string) so Axios uses relative URLs. CloudFront then routes API calls to the ALB via path behaviors. For local dev, use `http://localhost:8080`.
- **Notification polling**: `AppLayout` polls `GET /notifications` every 30 seconds via React Query `refetchInterval`. The unread badge count guards with `Array.isArray()`.
- **No `any` types**: All API responses have typed interfaces in `features/*/types.ts`. Use `unknown` + type narrowing instead of `any`.

## Dependencies

```json
{
  "react": "^18.2.0",
  "react-dom": "^18.2.0",
  "react-router-dom": "^6.22.0",
  "axios": "^1.6.5",
  "@tanstack/react-query": "^5.18.0",
  "react-hook-form": "^7.50.1",
  "@hookform/resolvers": "^3.3.4",
  "zod": "^3.22.4"
}
```

Dev: Vite 5, TypeScript 5, Tailwind CSS 3, PostCSS.

## Usage

```bash
# Local dev (backend must be running on :8080)
cp .env.example .env.local   # set VITE_API_BASE_URL=http://localhost:8080
npm install
npm run dev                  # → http://localhost:5173

# Production build
npm run build                # outputs to dist/
# dist/ is synced to S3 by GitHub Actions; CloudFront invalidation follows
```

## Testing

No automated frontend tests currently exist. Manual test checklist:
1. Register → login → redirected to dashboard
2. Create application → appears in list
3. Update status → dashboard counts refresh
4. Add targeting note → persists on reload
5. Notification bell badge reflects unread count
6. Logout → redirected to login; refreshing returns to login (token cleared)

## Related

- `src/lib/apiClient.ts` — Axios singleton
- `services/gateway-service/` — all frontend API calls route through here
- `infra/terraform/cloudfront.tf` — CloudFront behaviors (path routing)
- Root `CLAUDE.md` → Frontend Conventions section
