import { useMemo, useState, type FormEvent } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import * as Dialog from '@radix-ui/react-dialog'
import { zodResolver } from '@hookform/resolvers/zod'
import { Spinner } from '@fluentui/react-components'
import { ArrowLeft, ArrowRight, Check, Plus, ReceiptText, Trash2, UserMinus, UserPlus, X } from 'lucide-react'
import { DrawablyBadge, DrawablyButton, DrawablyInput, DrawablySelect } from 'drawably/react'
import { useForm } from 'react-hook-form'
import { Link, useParams } from 'react-router-dom'
import { z } from 'zod'
import { api, ApiClientError } from '../api'
import { EmptyNote, ErrorNote } from '../components/Feedback'
import { initials, money, shortDate } from '../format'
import type { Group, MemberRole, SplitInput, Transfer } from '../types'

function message(caught: unknown) {
  return caught instanceof ApiClientError ? caught.message : 'Something went wrong. Please try again.'
}

const expenseSchema = z.object({
  description: z.string().trim().min(1, 'Describe what the expense was for.').max(255),
  amount: z.string().min(1, 'Enter the expense amount.'),
  payerId: z.string().min(1, 'Choose who paid.'),
  strategy: z.enum(['EQUAL', 'EXACT', 'PERCENTAGE']),
  shares: z.record(z.string(), z.string()),
})
type ExpenseFields = z.infer<typeof expenseSchema>

function ExpenseDialog({ group, open, onOpenChange, onSaved }: { group: Group; open: boolean; onOpenChange: (open: boolean) => void; onSaved: () => Promise<void> }) {
  const [formError, setFormError] = useState('')
  const { register, handleSubmit, watch, reset, formState: { errors, isSubmitting } } = useForm<ExpenseFields>({
    resolver: zodResolver(expenseSchema),
    defaultValues: { description: '', amount: '', payerId: group.members[0]?.userId ?? '', strategy: 'EQUAL', shares: {} },
  })
  const strategy = watch('strategy')

  const submit = handleSubmit(async (fields) => {
    setFormError('')
    const amountCents = Math.round(Number(fields.amount) * 100)
    if (!Number.isFinite(amountCents) || amountCents <= 0) { setFormError('Enter an amount greater than zero.'); return }
    let splits: SplitInput[] = group.members.map((member) => ({ userId: member.userId }))
    if (fields.strategy === 'EXACT') {
      splits = group.members.map((member) => ({ userId: member.userId, amountCents: Math.round(Number(fields.shares[member.userId] || 0) * 100) }))
      if (splits.some((split) => !Number.isFinite(split.amountCents) || (split.amountCents ?? -1) < 0) || splits.reduce((sum, split) => sum + (split.amountCents ?? 0), 0) !== amountCents) {
        setFormError(`Exact shares must add up to ${money(amountCents)}.`); return
      }
    }
    if (fields.strategy === 'PERCENTAGE') {
      splits = group.members.map((member) => ({ userId: member.userId, percentage: Number(fields.shares[member.userId] || 0) }))
      if (splits.some((split) => !Number.isInteger(split.percentage) || (split.percentage ?? -1) < 0 || (split.percentage ?? 101) > 100) || splits.reduce((sum, split) => sum + (split.percentage ?? 0), 0) !== 100) {
        setFormError('Percentage shares must be whole numbers that add up to 100%.'); return
      }
    }
    try {
      await api.createExpense({ groupId: group.id, description: fields.description, amountCents, payerId: fields.payerId, splitStrategy: fields.strategy, splits })
      reset(); onOpenChange(false); await onSaved()
    } catch (caught) { setFormError(message(caught)) }
  })

  return <Dialog.Root open={open} onOpenChange={onOpenChange}><Dialog.Portal><Dialog.Overlay className="dialog-overlay" /><Dialog.Content className="dialog-sheet"><Dialog.Close className="dialog-close"><X /><span className="sr-only">Close</span></Dialog.Close><Dialog.Title>Add something manually</Dialog.Title><Dialog.Description>For cash, a missing transaction, or anything outside your bank feed.</Dialog.Description><form onSubmit={submit} noValidate>
    <label>What was it?<DrawablyInput aria-invalid={Boolean(errors.description)} {...register('description')} placeholder="Farmers market snacks" />{errors.description && <small className="field-error">{errors.description.message}</small>}</label>
    <label>Amount<DrawablyInput inputMode="decimal" aria-invalid={Boolean(errors.amount)} {...register('amount')} placeholder="24.00" />{errors.amount && <small className="field-error">{errors.amount.message}</small>}</label>
    <label>Who paid?<DrawablySelect {...register('payerId')}>{group.members.map((member) => <option key={member.userId} value={member.userId}>{member.displayName}</option>)}</DrawablySelect></label>
    <label>How should it split?<DrawablySelect {...register('strategy')}><option value="EQUAL">Equally</option><option value="EXACT">Exact amounts</option><option value="PERCENTAGE">Percentages</option></DrawablySelect></label>
    {strategy !== 'EQUAL' && <fieldset className="split-fields"><legend>{strategy === 'EXACT' ? 'Each person’s share' : 'Each person’s percentage'}</legend>{group.members.map((member) => <label key={member.userId}><span>{member.displayName}</span><DrawablyInput inputMode={strategy === 'EXACT' ? 'decimal' : 'numeric'} {...register(`shares.${member.userId}`)} placeholder={strategy === 'EXACT' ? '0.00' : '0'} /><small>{strategy === 'EXACT' ? 'USD' : '%'}</small></label>)}</fieldset>}
    {strategy === 'EQUAL' && <p className="form-hint"><Check /> Split equally between everyone in this group.</p>}
    {formError && <ErrorNote role="alert">{formError}</ErrorNote>}
    <DrawablyButton type="submit" variant="solid" state={isSubmitting ? 'loading' : 'idle'} disabled={isSubmitting}>Add expense <ArrowRight /></DrawablyButton>
  </form></Dialog.Content></Dialog.Portal></Dialog.Root>
}

export function GroupPage() {
  const { groupId = '' } = useParams()
  const queryClient = useQueryClient()
  const [error, setError] = useState('')
  const [expenseOpen, setExpenseOpen] = useState(false)
  const [memberOpen, setMemberOpen] = useState(false)
  const [memberEmail, setMemberEmail] = useState('')
  const [memberRole, setMemberRole] = useState<MemberRole>('MEMBER')
  const [settling, setSettling] = useState('')
  const detail = useQuery({
    queryKey: ['group', groupId], enabled: Boolean(groupId),
    queryFn: async () => {
      const [group, expenses, balances, plan] = await Promise.all([api.group(groupId), api.expenses(groupId), api.balances(groupId), api.settlementPlan(groupId)])
      return { group, expenses, balances, plan }
    },
  })
  const refresh = async () => { await Promise.all([queryClient.invalidateQueries({ queryKey: ['group', groupId] }), queryClient.invalidateQueries({ queryKey: ['dashboard'] })]) }
  const group = detail.data?.group
  const expenses = detail.data?.expenses ?? []
  const balances = detail.data?.balances ?? []
  const plan = detail.data?.plan ?? []
  const names = useMemo(() => new Map(group?.members.map((member) => [member.userId, member.displayName]) ?? []), [group])

  async function addMember(event: FormEvent) {
    event.preventDefault(); setError('')
    try { await api.addMember(groupId, memberEmail, memberRole); setMemberEmail(''); setMemberOpen(false); await refresh() }
    catch (caught) { setError(message(caught)) }
  }
  async function settle(transfer: Transfer) {
    const key = `${transfer.fromUserId}-${transfer.toUserId}`; setSettling(key); setError('')
    try { await api.recordSettlement(groupId, transfer.fromUserId, transfer.toUserId, transfer.amountCents); await refresh() }
    catch (caught) { setError(message(caught)) } finally { setSettling('') }
  }
  async function removeExpense(expenseId: string) {
    setError(''); try { await api.deleteExpense(groupId, expenseId); await refresh() } catch (caught) { setError(message(caught)) }
  }
  async function removeMember(userId: string) {
    setError(''); try { await api.removeMember(groupId, userId); await refresh() } catch (caught) { setError(message(caught)) }
  }

  if (detail.isLoading) return <div className="page-loader"><Spinner size="large" label="Following the trail" /></div>
  if (!group) return <div className="page-state"><ErrorNote role="alert">{message(detail.error)}</ErrorNote><Link to="/app">Back to activity</Link></div>

  return <div className="group-page">
    <Link className="back-link" to="/app"><ArrowLeft /> All activity</Link>
    <header className="group-heading"><div><h1>{group.name}</h1><p>{group.members.length} people, one shared trail.</p></div><div className="group-heading-actions"><DrawablyButton variant="outline" onClick={() => setMemberOpen(true)}><UserPlus /> Invite</DrawablyButton><DrawablyButton variant="solid" onClick={() => setExpenseOpen(true)}><Plus /> Add expense</DrawablyButton></div></header>
    {error && <ErrorNote role="alert">{error}</ErrorNote>}
    <section className="balance-section" aria-labelledby="balance-title"><div className="section-heading"><div><h2 id="balance-title">The group at a glance</h2></div></div><div className="balance-strip">{balances.map((balance) => <article key={balance.userId}><span className="comic-avatar comic-avatar--small">{initials(balance.displayName)}</span><strong>{balance.displayName}</strong><b className={balance.balanceCents > 0 ? 'positive' : balance.balanceCents < 0 ? 'negative' : ''}>{balance.balanceCents > 0 ? '+' : balance.balanceCents < 0 ? '−' : ''}{money(Math.abs(balance.balanceCents))}</b><small>{balance.balanceCents > 0 ? 'is owed' : balance.balanceCents < 0 ? 'owes the group' : 'all even'}</small></article>)}</div></section>
    <section className="settle-section" aria-labelledby="settle-title"><div className="section-heading"><div><h2 id="settle-title">Suggested next moves</h2></div></div>{plan.length === 0 ? <div className="all-even"><Check /><span><strong>All even for now.</strong><small>The next shared purchase can start a new trail.</small></span></div> : <div className="settlement-list">{plan.map((transfer) => { const key = `${transfer.fromUserId}-${transfer.toUserId}`; return <article key={key}><span><strong>{names.get(transfer.fromUserId)}</strong> pays <strong>{names.get(transfer.toUserId)}</strong></span><b>{money(transfer.amountCents)}</b><DrawablyButton variant="outline" state={settling === key ? 'loading' : 'idle'} disabled={Boolean(settling)} onClick={() => void settle(transfer)}>Mark paid <Check /></DrawablyButton></article> })}</div>}</section>
    <section className="expense-section" aria-labelledby="expense-title"><div className="section-heading"><div><h2 id="expense-title">Shared expenses</h2></div><span>{expenses.length} entries</span></div>{expenses.length === 0 ? <EmptyNote title="No shared expenses yet">Head back to activity and choose a bank transaction, or add one manually.</EmptyNote> : <div className="expense-list">{expenses.map((expense) => <article key={expense.id}><span className="expense-icon"><ReceiptText /></span><div><strong>{expense.description}</strong><span>{names.get(expense.payerId) ?? 'A friend'} paid · {shortDate(expense.createdAt.slice(0, 10))}</span></div><b>{money(expense.amountCents)}</b><DrawablyBadge variant="outline">{expense.splitStrategy.toLowerCase()}</DrawablyBadge><button className="icon-button" type="button" aria-label={`Delete ${expense.description}`} onClick={() => void removeExpense(expense.id)}><Trash2 /></button></article>)}</div>}</section>
    <section className="member-section" aria-labelledby="member-title"><h2 id="member-title">In this circle</h2><div className="member-list">{group.members.map((member) => <span key={member.userId}><i>{initials(member.displayName)}</i><strong>{member.displayName}</strong><small>{member.role === 'ADMIN' ? 'Organizer' : 'Friend'}</small>{member.userId !== group.createdBy && <button className="icon-button member-remove" type="button" aria-label={`Remove ${member.displayName}`} onClick={() => void removeMember(member.userId)}><UserMinus /></button>}</span>)}</div></section>
    <ExpenseDialog group={group} open={expenseOpen} onOpenChange={setExpenseOpen} onSaved={refresh} />
    <Dialog.Root open={memberOpen} onOpenChange={setMemberOpen}><Dialog.Portal><Dialog.Overlay className="dialog-overlay" /><Dialog.Content className="dialog-sheet"><Dialog.Close className="dialog-close"><X /><span className="sr-only">Close</span></Dialog.Close><Dialog.Title>Invite a friend</Dialog.Title><Dialog.Description>They need a SettleUp account using this email.</Dialog.Description><form onSubmit={addMember}><label>Email<DrawablyInput required type="email" value={memberEmail} onChange={(event) => setMemberEmail(event.target.value)} placeholder="friend@example.com" /></label><label>Role<DrawablySelect value={memberRole} onChange={(event) => setMemberRole(event.target.value as MemberRole)}><option value="MEMBER">Friend</option><option value="ADMIN">Organizer</option></DrawablySelect></label><DrawablyButton type="submit" variant="solid"><UserPlus /> Add to the circle</DrawablyButton></form></Dialog.Content></Dialog.Portal></Dialog.Root>
  </div>
}
