import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import * as Dialog from '@radix-ui/react-dialog'
import { zodResolver } from '@hookform/resolvers/zod'
import { Spinner } from '@fluentui/react-components'
import { ArrowLeft, ArrowRight, Check, Pencil, Plus, ReceiptText, Trash2, UserMinus, UserPlus, X } from 'lucide-react'
import { DrawablyBadge, DrawablyButton, DrawablyInput, DrawablySelect } from 'drawably/react'
import { useForm } from 'react-hook-form'
import { Link, useParams } from 'react-router-dom'
import { z } from 'zod'
import { api, ApiClientError } from '../api'
import { EmptyNote, ErrorNote } from '../components/Feedback'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { initials, money, parseMoneyToCents, shortDate } from '../format'
import { useAuth } from '../auth'
import type { Expense, Group, MemberRole, SettlementRecord, SplitInput, Transfer } from '../types'

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

function ExpenseDialog({ group, expense, open, onOpenChange, onSaved }: { group: Group; expense?: Expense | null; open: boolean; onOpenChange: (open: boolean) => void; onSaved: () => Promise<void> }) {
  const [formError, setFormError] = useState('')
  const { register, handleSubmit, watch, reset, formState: { errors, isSubmitting } } = useForm<ExpenseFields>({
    resolver: zodResolver(expenseSchema),
    defaultValues: { description: '', amount: '', payerId: group.members[0]?.userId ?? '', strategy: 'EQUAL', shares: {} },
  })
  const strategy = watch('strategy')

  useEffect(() => {
    if (!open) return
    reset(expense ? {
      description: expense.description,
      amount: (expense.amountCents / 100).toFixed(2),
      payerId: expense.payerId,
      strategy: expense.splitStrategy === 'PERCENTAGE' ? 'EXACT' : expense.splitStrategy,
      shares: Object.fromEntries(expense.splits.map((split) => [split.userId,
        (split.shareCents / 100).toFixed(2)])),
    } : { description: '', amount: '', payerId: group.members[0]?.userId ?? '', strategy: 'EQUAL', shares: {} })
  }, [expense, group.members, open, reset])

  const submit = handleSubmit(async (fields) => {
    setFormError('')
    const amountCents = parseMoneyToCents(fields.amount)
    if (amountCents === null || amountCents <= 0) { setFormError('Enter a positive amount with no more than two decimal places.'); return }
    let splits: SplitInput[] = group.members.map((member) => ({ userId: member.userId }))
    if (fields.strategy === 'EXACT') {
      splits = group.members.map((member) => ({ userId: member.userId, amountCents: parseMoneyToCents(fields.shares[member.userId] || '0') ?? -1 }))
      if (splits.some((split) => (split.amountCents ?? -1) < 0) || splits.reduce((sum, split) => sum + (split.amountCents ?? 0), 0) !== amountCents) {
        setFormError(`Exact shares must add up to ${money(amountCents, group.currencyCode)}.`); return
      }
    }
    if (fields.strategy === 'PERCENTAGE') {
      splits = group.members.map((member) => ({ userId: member.userId, percentage: Number(fields.shares[member.userId] || 0) }))
      if (splits.some((split) => !Number.isInteger(split.percentage) || (split.percentage ?? -1) < 0 || (split.percentage ?? 101) > 100) || splits.reduce((sum, split) => sum + (split.percentage ?? 0), 0) !== 100) {
        setFormError('Percentage shares must be whole numbers that add up to 100%.'); return
      }
    }
    try {
      const input = { groupId: group.id, description: fields.description, amountCents, payerId: fields.payerId, splitStrategy: fields.strategy, splits }
      if (expense) await api.updateExpense(expense.id, input)
      else await api.createExpense(input)
      reset(); onOpenChange(false); await onSaved()
    } catch (caught) { setFormError(message(caught)) }
  })

  return <Dialog.Root open={open} onOpenChange={onOpenChange}><Dialog.Portal><Dialog.Overlay className="dialog-overlay" /><Dialog.Content className="dialog-sheet"><Dialog.Close className="dialog-close"><X /><span className="sr-only">Close</span></Dialog.Close><Dialog.Title>{expense ? 'Edit shared expense' : 'Add something manually'}</Dialog.Title><Dialog.Description>{expense ? 'Update the payer, amount, or split. Everyone’s balance will recalculate.' : 'For cash, a missing transaction, or anything outside your bank feed.'}</Dialog.Description><form onSubmit={submit} noValidate>
    <label>What was it?<DrawablyInput aria-invalid={Boolean(errors.description)} {...register('description')} placeholder="Farmers market snacks" />{errors.description && <small className="field-error">{errors.description.message}</small>}</label>
    <label>Amount ({group.currencyCode})<DrawablyInput inputMode="decimal" aria-invalid={Boolean(errors.amount)} aria-describedby={errors.amount ? 'expense-amount-error' : undefined} {...register('amount')} placeholder="24.00" />{errors.amount && <small id="expense-amount-error" className="field-error">{errors.amount.message}</small>}</label>
    <label>Who paid?<DrawablySelect {...register('payerId')}>{group.members.map((member) => <option key={member.userId} value={member.userId}>{member.displayName}</option>)}</DrawablySelect></label>
    <label>How should it split?<DrawablySelect {...register('strategy')}><option value="EQUAL">Equally</option><option value="EXACT">Exact amounts</option><option value="PERCENTAGE">Percentages</option></DrawablySelect></label>
    {strategy !== 'EQUAL' && <fieldset className="split-fields"><legend>{strategy === 'EXACT' ? 'Each person’s share' : 'Each person’s percentage'}</legend>{group.members.map((member) => <label key={member.userId}><span>{member.displayName}</span><DrawablyInput inputMode={strategy === 'EXACT' ? 'decimal' : 'numeric'} {...register(`shares.${member.userId}`)} placeholder={strategy === 'EXACT' ? '0.00' : '0'} /><small>{strategy === 'EXACT' ? group.currencyCode : '%'}</small></label>)}</fieldset>}
    {strategy === 'EQUAL' && <p className="form-hint"><Check /> Split equally between everyone in this group.</p>}
    {formError && <ErrorNote role="alert">{formError}</ErrorNote>}
    <DrawablyButton type="submit" variant="solid" state={isSubmitting ? 'loading' : 'idle'} disabled={isSubmitting}>{expense ? 'Save expense' : 'Add expense'} <ArrowRight /></DrawablyButton>
  </form></Dialog.Content></Dialog.Portal></Dialog.Root>
}

export function GroupPage() {
  const { user } = useAuth()
  const { groupId = '' } = useParams()
  const queryClient = useQueryClient()
  const [error, setError] = useState('')
  const [expenseOpen, setExpenseOpen] = useState(false)
  const [editingExpense, setEditingExpense] = useState<Expense | null>(null)
  const [memberOpen, setMemberOpen] = useState(false)
  const [memberEmail, setMemberEmail] = useState('')
  const [memberRole, setMemberRole] = useState<MemberRole>('MEMBER')
  const [settling, setSettling] = useState('')
  const [pendingTransfer, setPendingTransfer] = useState<Transfer | null>(null)
  const [pendingDelete, setPendingDelete] = useState<Expense | null>(null)
  const [pendingMember, setPendingMember] = useState<{ userId: string; displayName: string } | null>(null)
  const [lastSettlement, setLastSettlement] = useState<SettlementRecord | null>(null)
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
  const currentMembership = group?.members.find((member) => member.userId === user?.id)
  const isAdmin = currentMembership?.role === 'ADMIN'

  async function addMember(event: FormEvent) {
    event.preventDefault(); setError('')
    try { await api.addMember(groupId, memberEmail, memberRole); setMemberEmail(''); setMemberOpen(false); await refresh() }
    catch (caught) { setError(message(caught)) }
  }
  async function settle(transfer: Transfer) {
    const key = `${transfer.fromUserId}-${transfer.toUserId}`; setSettling(key); setError('')
    try { setLastSettlement(await api.recordSettlement(groupId, transfer.fromUserId, transfer.toUserId, transfer.amountCents)); setPendingTransfer(null); await refresh() }
    catch (caught) { setError(message(caught)) } finally { setSettling('') }
  }
  async function removeExpense(expenseId: string) {
    setError(''); try { await api.deleteExpense(groupId, expenseId); setPendingDelete(null); await refresh() } catch (caught) { setError(message(caught)) }
  }
  async function removeMember(userId: string) {
    setError(''); try { await api.removeMember(groupId, userId); setPendingMember(null); await refresh() } catch (caught) { setError(message(caught)) }
  }
  async function undoSettlement() {
    if (!lastSettlement) return
    setError(''); try { await api.reverseSettlement(groupId, lastSettlement.id); setLastSettlement(null); await refresh() }
    catch (caught) { setError(message(caught)) }
  }

  if (detail.isLoading) return <div className="page-loader"><Spinner size="large" label="Following the trail" /></div>
  if (!group) return <div className="page-state"><ErrorNote role="alert">{message(detail.error)}</ErrorNote><Link to="/app">Back to activity</Link></div>

  return <div className="group-page">
    <Link className="back-link" to="/app"><ArrowLeft /> All activity</Link>
    <header className="group-heading"><div><h1>{group.name}</h1><p>{group.members.length} people · {group.currencyCode} ledger</p></div><div className="group-heading-actions">{isAdmin && <DrawablyButton variant="outline" onClick={() => setMemberOpen(true)}><UserPlus /> Add member</DrawablyButton>}<DrawablyButton variant="solid" onClick={() => { setEditingExpense(null); setExpenseOpen(true) }}><Plus /> Add expense</DrawablyButton></div></header>
    {error && <ErrorNote role="alert">{error}</ErrorNote>}
    {lastSettlement && <div className="undo-note" role="status"><span>Payment recorded.</span><button type="button" onClick={() => void undoSettlement()}>Undo</button></div>}
    <section className="balance-section" aria-labelledby="balance-title"><div className="section-heading"><div><h2 id="balance-title">The group at a glance</h2></div></div><div className="balance-strip">{balances.map((balance) => <article key={balance.userId}><span className="comic-avatar comic-avatar--small">{initials(balance.displayName)}</span><strong>{balance.displayName}</strong><b className={balance.balanceCents > 0 ? 'positive' : balance.balanceCents < 0 ? 'negative' : ''}>{balance.balanceCents > 0 ? '+' : balance.balanceCents < 0 ? '−' : ''}{money(Math.abs(balance.balanceCents), group.currencyCode)}</b><small>{balance.balanceCents > 0 ? 'is owed' : balance.balanceCents < 0 ? 'owes the group' : 'all even'}</small></article>)}</div></section>
    <section className="settle-section" aria-labelledby="settle-title"><div className="section-heading"><div><h2 id="settle-title">Suggested next moves</h2></div></div>{plan.length === 0 ? <div className="all-even"><Check /><span><strong>All even for now.</strong><small>The next shared purchase can start a new trail.</small></span></div> : <div className="settlement-list">{plan.map((transfer) => { const key = `${transfer.fromUserId}-${transfer.toUserId}`; const canRecord = transfer.fromUserId === user?.id || isAdmin; return <article key={key}><span><strong>{names.get(transfer.fromUserId)}</strong> pays <strong>{names.get(transfer.toUserId)}</strong></span><b>{money(transfer.amountCents, group.currencyCode)}</b>{canRecord ? <DrawablyButton variant="outline" state={settling === key ? 'loading' : 'idle'} disabled={Boolean(settling)} onClick={() => setPendingTransfer(transfer)}>Mark paid <Check /></DrawablyButton> : <small>Waiting for {names.get(transfer.fromUserId)}</small>}</article> })}</div>}</section>
    <section className="expense-section" aria-labelledby="expense-title"><div className="section-heading"><div><h2 id="expense-title">Shared expenses</h2></div><span>{expenses.length} entries</span></div>{expenses.length === 0 ? <EmptyNote title="No shared expenses yet">Head back to activity and choose a bank transaction, or add one manually.</EmptyNote> : <div className="expense-list">{expenses.map((expense) => { const canManage = expense.createdBy === user?.id || isAdmin; return <article key={expense.id}><span className="expense-icon"><ReceiptText /></span><div><strong>{expense.description}</strong><span>{names.get(expense.payerId) ?? 'A friend'} paid · {shortDate(expense.createdAt.slice(0, 10))}</span></div><b>{money(expense.amountCents, expense.currencyCode)}</b><DrawablyBadge variant="outline">{expense.splitStrategy.toLowerCase()}</DrawablyBadge>{canManage && <div className="row-actions"><button className="icon-button" type="button" aria-label={`Edit ${expense.description}`} onClick={() => { setEditingExpense(expense); setExpenseOpen(true) }}><Pencil /></button><button className="icon-button" type="button" aria-label={`Delete ${expense.description}`} onClick={() => setPendingDelete(expense)}><Trash2 /></button></div>}</article> })}</div>}</section>
    <section className="member-section" aria-labelledby="member-title"><h2 id="member-title">In this circle</h2><div className="member-list">{group.members.map((member) => <span key={member.userId}><i>{initials(member.displayName)}</i><strong>{member.displayName}</strong><small>{member.role === 'ADMIN' ? 'Organizer' : 'Friend'}</small>{isAdmin && member.userId !== group.createdBy && <button className="icon-button member-remove" type="button" aria-label={`Remove ${member.displayName}`} onClick={() => setPendingMember({ userId: member.userId, displayName: member.displayName })}><UserMinus /></button>}</span>)}</div></section>
    <ExpenseDialog group={group} expense={editingExpense} open={expenseOpen} onOpenChange={(open) => { setExpenseOpen(open); if (!open) setEditingExpense(null) }} onSaved={refresh} />
    <Dialog.Root open={memberOpen} onOpenChange={setMemberOpen}><Dialog.Portal><Dialog.Overlay className="dialog-overlay" /><Dialog.Content className="dialog-sheet"><Dialog.Close className="dialog-close"><X /><span className="sr-only">Close</span></Dialog.Close><Dialog.Title>Add an existing member</Dialog.Title><Dialog.Description>Enter the email address they use for SettleUp. They’ll join the group immediately.</Dialog.Description><form onSubmit={addMember}><label>Email<DrawablyInput required type="email" value={memberEmail} onChange={(event) => setMemberEmail(event.target.value)} placeholder="friend@example.com" /></label><label>Role<DrawablySelect value={memberRole} onChange={(event) => setMemberRole(event.target.value as MemberRole)}><option value="MEMBER">Friend</option><option value="ADMIN">Organizer</option></DrawablySelect></label><DrawablyButton type="submit" variant="solid"><UserPlus /> Add member</DrawablyButton></form></Dialog.Content></Dialog.Portal></Dialog.Root>
    <ConfirmDialog open={Boolean(pendingTransfer)} onOpenChange={(open) => { if (!open && !settling) setPendingTransfer(null) }} title="Record this payment?" description={pendingTransfer ? `${names.get(pendingTransfer.fromUserId)} paid ${names.get(pendingTransfer.toUserId)} ${money(pendingTransfer.amountCents, group.currencyCode)}. This changes everyone’s balance.` : ''} confirmLabel="Record payment" busy={Boolean(settling)} onConfirm={() => { if (pendingTransfer) return settle(pendingTransfer) }} />
    <ConfirmDialog open={Boolean(pendingDelete)} onOpenChange={(open) => { if (!open) setPendingDelete(null) }} title={`Delete ${pendingDelete?.description ?? 'this expense'}?`} description="This removes the expense and recalculates every group balance. This cannot be undone." confirmLabel="Delete expense" onConfirm={() => { if (pendingDelete) return removeExpense(pendingDelete.id) }} />
    <ConfirmDialog open={Boolean(pendingMember)} onOpenChange={(open) => { if (!open) setPendingMember(null) }} title={`Remove ${pendingMember?.displayName ?? 'this member'}?`} description="They can only be removed if they have no expense or payment history in this group." confirmLabel="Remove member" onConfirm={() => { if (pendingMember) return removeMember(pendingMember.userId) }} />
  </div>
}
