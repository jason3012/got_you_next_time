import { useState } from 'react'
import * as Tabs from '@radix-ui/react-tabs'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { ArrowLeft, ArrowRight } from 'lucide-react'
import { DrawablyButton, DrawablyInput } from 'drawably/react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { ApiClientError } from '../api'
import { useAuth } from '../auth'
import { ErrorNote } from '../components/Feedback'

type Mode = 'login' | 'register'

const authSchema = z.object({
  displayName: z.string().trim(),
  email: z.string().email('Enter a valid email address.'),
  password: z.string().min(8, 'Use at least 8 characters.'),
})
type AuthFields = z.infer<typeof authSchema>

export function AuthPage() {
  const { login, register, user } = useAuth()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [mode, setMode] = useState<Mode>('register')
  const [error, setError] = useState('')
  const { register: field, handleSubmit, formState: { errors, isSubmitting } } = useForm<AuthFields>({
    resolver: zodResolver(authSchema), defaultValues: { displayName: '', email: '', password: '' },
  })

  if (user) return <Navigate to={searchParams.get('intent') === 'connect' ? '/app?connect=1' : '/app'} replace />

  const submit = handleSubmit(async ({ email, displayName, password }) => {
    setError('')
    try {
      if (mode === 'login') await login(email, password)
      else {
        if (!displayName.trim()) { setError('Tell us what your friends call you.'); return }
        await register(email, displayName, password)
      }
      navigate(searchParams.get('intent') === 'connect' ? '/app?connect=1' : '/app')
    } catch (caught) {
      setError(caught instanceof ApiClientError ? caught.message : 'We could not reach SettleUp. Check that the backend is running and try again.')
    }
  })

  return (
    <main className="auth-page">
      <Link className="back-link" to="/"><ArrowLeft /> Back to the map</Link>
      <section className="auth-sheet" aria-labelledby="auth-title">
        <div className="auth-doodle" aria-hidden="true"><span>J</span><span>N</span><span>E</span></div>
        <h1 id="auth-title">Start with what actually happened.</h1>
        <p>Connect real transactions, then choose which moments belong to the group.</p>
        <Tabs.Root value={mode} onValueChange={(value) => { setMode(value as Mode); setError('') }}>
          <Tabs.List className="auth-tabs" aria-label="Account action"><Tabs.Trigger value="register">Create account</Tabs.Trigger><Tabs.Trigger value="login">Sign in</Tabs.Trigger></Tabs.List>
          <form className="auth-form" onSubmit={submit} noValidate>
            {mode === 'register' && <label>Your name<DrawablyInput autoComplete="name" {...field('displayName')} placeholder="Jamie Chen" /></label>}
            <label>Email<DrawablyInput type="email" autoComplete="email" aria-invalid={Boolean(errors.email)} {...field('email')} placeholder="you@example.com" />{errors.email && <small className="field-error">{errors.email.message}</small>}</label>
            <label>Password<DrawablyInput type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} aria-invalid={Boolean(errors.password)} {...field('password')} placeholder="At least 8 characters" />{errors.password && <small className="field-error">{errors.password.message}</small>}</label>
            {error && <ErrorNote role="alert">{error}</ErrorNote>}
            <DrawablyButton type="submit" variant="solid" state={isSubmitting ? 'loading' : 'idle'} disabled={isSubmitting}>{mode === 'login' ? 'Open my notebook' : 'Create my notebook'} <ArrowRight /></DrawablyButton>
          </form>
        </Tabs.Root>
        <small>Bank connections are handled securely through Plaid. SettleUp never receives your bank password.</small>
      </section>
    </main>
  )
}
