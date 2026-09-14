import { useState, type FormEvent } from 'react'
import * as Tabs from '@radix-ui/react-tabs'
import { ArrowLeft, ArrowRight } from 'lucide-react'
import { DrawablyButton, DrawablyInput } from 'drawably/react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { ApiClientError } from '../api'
import { useAuth } from '../auth'
import { ErrorNote } from '../components/Feedback'

type Mode = 'login' | 'register'

export function AuthPage() {
  const { login, register, user } = useAuth()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [mode, setMode] = useState<Mode>('register')
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={searchParams.get('intent') === 'connect' ? '/app?connect=1' : '/app'} replace />

  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setBusy(true)
    try {
      if (mode === 'login') await login(email, password)
      else await register(email, displayName, password)
      navigate(searchParams.get('intent') === 'connect' ? '/app?connect=1' : '/app')
    } catch (caught) {
      setError(caught instanceof ApiClientError ? caught.message : 'We could not reach SettleUp. Check that the backend is running and try again.')
    } finally { setBusy(false) }
  }

  return (
    <main className="auth-page">
      <Link className="back-link" to="/"><ArrowLeft /> Back to the map</Link>
      <section className="auth-sheet" aria-labelledby="auth-title">
        <div className="auth-doodle" aria-hidden="true"><span>J</span><span>N</span><span>E</span></div>
        <h1 id="auth-title">Start with what actually happened.</h1>
        <p>Connect real transactions, then choose which moments belong to the group.</p>
        <Tabs.Root value={mode} onValueChange={(value) => { setMode(value as Mode); setError('') }}>
          <Tabs.List className="auth-tabs" aria-label="Account action"><Tabs.Trigger value="register">Create account</Tabs.Trigger><Tabs.Trigger value="login">Sign in</Tabs.Trigger></Tabs.List>
          <form className="auth-form" onSubmit={submit}>
            {mode === 'register' && <label>Your name<DrawablyInput required autoComplete="name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} placeholder="Jamie Chen" /></label>}
            <label>Email<DrawablyInput required type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" /></label>
            <label>Password<DrawablyInput required minLength={8} type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" /></label>
            {error && <ErrorNote>{error}</ErrorNote>}
            <DrawablyButton type="submit" variant="solid" state={busy ? 'loading' : 'idle'} disabled={busy}>{mode === 'login' ? 'Open my notebook' : 'Create my notebook'} <ArrowRight /></DrawablyButton>
          </form>
        </Tabs.Root>
        <small>Bank connections are handled securely through Plaid. SettleUp never receives your bank password.</small>
      </section>
    </main>
  )
}
