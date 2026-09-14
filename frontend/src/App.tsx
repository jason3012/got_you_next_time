import { Spinner } from '@fluentui/react-components'
import { lazy, Suspense } from 'react'
import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'
import { AppShell } from './components/AppShell'

const AuthPage = lazy(() => import('./pages/AuthPage').then((module) => ({ default: module.AuthPage })))
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((module) => ({ default: module.DashboardPage })))
const GroupPage = lazy(() => import('./pages/GroupPage').then((module) => ({ default: module.GroupPage })))
const LandingPage = lazy(() => import('./pages/LandingPage').then((module) => ({ default: module.LandingPage })))

function ProtectedRoute() {
  const { loading, user } = useAuth()
  if (loading) return <div className="page-loader"><Spinner size="large" label="Opening your notebook" /></div>
  return user ? <Outlet /> : <Navigate to="/auth" replace />
}

export default function App() {
  return (
    <AuthProvider>
      <Suspense fallback={<div className="page-loader"><Spinner size="large" label="Turning the page" /></div>}>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/auth" element={<AuthPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route path="/app" element={<DashboardPage />} />
              <Route path="/app/groups/:groupId" element={<GroupPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </AuthProvider>
  )
}
