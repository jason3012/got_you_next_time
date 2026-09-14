import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import * as Dialog from '@radix-ui/react-dialog'
import { Spinner } from '@fluentui/react-components'
import { ArrowLeft, ArrowRight, Check, Plus, ReceiptText, UserPlus, X } from 'lucide-react'
import { DrawablyBadge, DrawablyButton, DrawablyInput, DrawablySelect } from 'drawably/react'
import { Link, useParams } from 'react-router-dom'
import { api, ApiClientError } from '../api'
import { EmptyNote, ErrorNote } from '../components/Feedback'
import { initials, money, shortDate } from '../format'
import type { Balance, Expense, Group, MemberRole, Transfer } from '../types'

function message(caught: unknown) {
  return caught instanceof ApiClientError ? caught.message : 'Something went wrong. Please try again.'
}

export function GroupPage() {
  const { groupId = '' } = useParams()
  const [group, setGroup] = useState<Group | null>(null)
  const [expenses, setExpenses] = useState<Expense[]>([])
  const [balances, setBalances] = useState<Balance[]>([])
  const [plan, setPlan] = useState<Transfer[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [expenseOpen, setExpenseOpen] = useState(false)
  const [memberOpen, setMemberOpen] = useState(false)
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [payerId, setPayerId] = useState('')
  const [memberEmail, setMemberEmail] = useState('')
  const [memberRole, setMemberRole] = useState<MemberRole>('MEMBER')

  const refresh = useCallback(async () => {
    setError('')
    try {
      const [nextGroup, nextExpenses, nextBalances, nextPlan] = await Promise.all([api.group(groupId), api.expenses(groupId), api.balances(groupId), api.settlementPlan(groupId)])
      setGroup(nextGroup); setExpenses(nextExpenses); setBalances(nextBalances); setPlan(nextPlan)
      setPayerId((current) => current || nextGroup.members[0]?.userId || '')
    } catch (caught) { setError(message(caught)) }
    finally { setLoading(false) }
  }, [groupId])

  useEffect(() => { void refresh() }, [refresh])

  const names = useMemo(() => new Map(group?.members.map((member) => [member.userId, member.displayName]) ?? []), [group])

  async function addExpense(event: FormEvent) {
    event.preventDefault()
    if (!group) return
    const amountCents = Math.round(Number(amount) * 100)
    if (!Number.isFinite(amountCents) || amountCents <= 0) { setError('Enter an amount greater than zero.'); return }
    try {
      await api.createExpense({ groupId, description, amountCents, payerId, splitStrategy: 'EQUAL', splits: group.members.map((member) => ({ userId: member.userId })) })
      setDescription(''); setAmount(''); setExpenseOpen(false); await refresh()
    } catch (caught) { setError(message(caught)) }
  }

  async function addMember(event: FormEvent) {
    event.preventDefault()
    try { await api.addMember(groupId, memberEmail, memberRole); setMemberEmail(''); setMemberOpen(false); await refresh() }
    catch (caught) { setError(message(caught)) }
  }

  async function settle(transfer: Transfer) {
    try { await api.recordSettlement(groupId, transfer.fromUserId, transfer.toUserId, transfer.amountCents); await refresh() }
    catch (caught) { setError(message(caught)) }
  }

  if (loading) return <div className="page-loader"><Spinner size="large" label="Following the trail" /></div>
  if (!group) return <div className="page-state"><ErrorNote>{error || 'This group could not be found.'}</ErrorNote><Link to="/app">Back to activity</Link></div>

  return (
    <div className="group-page">
      <Link className="back-link" to="/app"><ArrowLeft /> All activity</Link>
      <header className="group-heading"><div><h1>{group.name}</h1><p>{group.members.length} people, one shared trail.</p></div><div className="group-heading-actions"><DrawablyButton variant="outline" onClick={() => setMemberOpen(true)}><UserPlus /> Invite</DrawablyButton><DrawablyButton variant="solid" onClick={() => setExpenseOpen(true)}><Plus /> Add expense</DrawablyButton></div></header>
      {error && <ErrorNote>{error}</ErrorNote>}

      <section className="balance-section" aria-labelledby="balance-title"><div className="section-heading"><div><h2 id="balance-title">The group at a glance</h2></div></div><div className="balance-strip">{balances.map((balance) => <article key={balance.userId}><span className="comic-avatar comic-avatar--small">{initials(balance.displayName)}</span><strong>{balance.displayName}</strong><b className={balance.balanceCents > 0 ? 'positive' : balance.balanceCents < 0 ? 'negative' : ''}>{balance.balanceCents > 0 ? '+' : balance.balanceCents < 0 ? '−' : ''}{money(Math.abs(balance.balanceCents))}</b><small>{balance.balanceCents > 0 ? 'is owed' : balance.balanceCents < 0 ? 'owes the group' : 'all even'}</small></article>)}</div></section>

      <section className="settle-section" aria-labelledby="settle-title"><div className="section-heading"><div><h2 id="settle-title">Suggested next moves</h2></div></div>{plan.length === 0 ? <div className="all-even"><Check /><span><strong>All even for now.</strong><small>The next shared purchase can start a new trail.</small></span></div> : <div className="settlement-list">{plan.map((transfer) => <article key={`${transfer.fromUserId}-${transfer.toUserId}`}><span><strong>{names.get(transfer.fromUserId)}</strong> pays <strong>{names.get(transfer.toUserId)}</strong></span><b>{money(transfer.amountCents)}</b><DrawablyButton variant="outline" onClick={() => void settle(transfer)}>Mark paid <Check /></DrawablyButton></article>)}</div>}</section>

      <section className="expense-section" aria-labelledby="expense-title"><div className="section-heading"><div><h2 id="expense-title">Shared expenses</h2></div><span>{expenses.length} entries</span></div>{expenses.length === 0 ? <EmptyNote title="No shared expenses yet">Head back to activity and choose a bank transaction, or add one manually.</EmptyNote> : <div className="expense-list">{expenses.map((expense) => <article key={expense.id}><span className="expense-icon"><ReceiptText /></span><div><strong>{expense.description}</strong><span>{names.get(expense.payerId) ?? 'A friend'} paid · {shortDate(expense.createdAt.slice(0, 10))}</span></div><b>{money(expense.amountCents)}</b><DrawablyBadge variant="outline">{expense.splitStrategy.toLowerCase()}</DrawablyBadge></article>)}</div>}</section>

      <section className="member-section" aria-labelledby="member-title"><h2 id="member-title">In this circle</h2><div className="member-list">{group.members.map((member) => <span key={member.userId}><i>{initials(member.displayName)}</i><strong>{member.displayName}</strong><small>{member.role === 'ADMIN' ? 'Organizer' : 'Friend'}</small></span>)}</div></section>

      <Dialog.Root open={expenseOpen} onOpenChange={setExpenseOpen}><Dialog.Portal><Dialog.Overlay className="dialog-overlay" /><Dialog.Content className="dialog-sheet"><Dialog.Close className="dialog-close"><X /><span className="sr-only">Close</span></Dialog.Close><Dialog.Title>Add something manually</Dialog.Title><Dialog.Description>For cash, a missing transaction, or anything outside your bank feed.</Dialog.Description><form onSubmit={addExpense}><label>What was it?<DrawablyInput required value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Farmers market snacks" /></label><label>Amount<DrawablyInput required inputMode="decimal" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="24.00" /></label><label>Who paid?<DrawablySelect required value={payerId} onChange={(event) => setPayerId(event.target.value)}>{group.members.map((member) => <option key={member.userId} value={member.userId}>{member.displayName}</option>)}</DrawablySelect></label><p className="form-hint"><Check /> Split equally between everyone in this group.</p><DrawablyButton type="submit" variant="solid">Add expense <ArrowRight /></DrawablyButton></form></Dialog.Content></Dialog.Portal></Dialog.Root>

      <Dialog.Root open={memberOpen} onOpenChange={setMemberOpen}><Dialog.Portal><Dialog.Overlay className="dialog-overlay" /><Dialog.Content className="dialog-sheet"><Dialog.Close className="dialog-close"><X /><span className="sr-only">Close</span></Dialog.Close><Dialog.Title>Invite a friend</Dialog.Title><Dialog.Description>They need a SettleUp account using this email.</Dialog.Description><form onSubmit={addMember}><label>Email<DrawablyInput required type="email" value={memberEmail} onChange={(event) => setMemberEmail(event.target.value)} placeholder="friend@example.com" /></label><label>Role<DrawablySelect value={memberRole} onChange={(event) => setMemberRole(event.target.value as MemberRole)}><option value="MEMBER">Friend</option><option value="ADMIN">Organizer</option></DrawablySelect></label><DrawablyButton type="submit" variant="solid"><UserPlus /> Add to the circle</DrawablyButton></form></Dialog.Content></Dialog.Portal></Dialog.Root>
    </div>
  )
}
