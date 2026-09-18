import { DrawablyAlert } from 'drawably/react'
import { AlertCircle, Inbox } from 'lucide-react'
import type { ReactNode } from 'react'

export function ErrorNote({ children, role }: { children: ReactNode; role?: 'alert' | 'status' }) {
  return <DrawablyAlert className="feedback feedback--error" role={role}><AlertCircle /> <span>{children}</span></DrawablyAlert>
}

export function EmptyNote({ title, children }: { title: string; children: ReactNode }) {
  return <div className="empty-note"><Inbox aria-hidden="true" /><strong>{title}</strong><p>{children}</p></div>
}
