import { DrawablyDivider } from 'drawably/react'
import { BookOpen, Inbox, LogOut, Users } from 'lucide-react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth'

export function AppShell() {
  const { logout, user } = useAuth()
  const location = useLocation()
  const onDashboard = location.pathname === '/app'
  return (
    <div className="app-shell">
      <header className="app-header">
        <NavLink className="app-wordmark" to="/app">SettleUp</NavLink>
        <div className="app-person"><span>{user?.displayName}</span><button className="icon-button" type="button" onClick={logout} aria-label="Sign out"><LogOut /></button></div>
        <DrawablyDivider roughness={0.65} boil={0.12} />
      </header>
      <main className="app-content"><Outlet /></main>
      <nav className="bottom-nav" aria-label="App navigation">
        <Link className={onDashboard && !location.hash ? 'active' : ''} to="/app"><BookOpen /><span>Activity</span></Link>
        <Link className={onDashboard && location.hash === '#suggestions' ? 'active' : ''} to="/app#suggestions"><Inbox /><span>Inbox</span></Link>
        <Link className={onDashboard && location.hash === '#groups' ? 'active' : ''} to="/app#groups"><Users /><span>Groups</span></Link>
      </nav>
    </div>
  )
}
