import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import * as Dialog from '@radix-ui/react-dialog'
import * as Tabs from '@radix-ui/react-tabs'
import { Spinner } from '@fluentui/react-components'
import { ArrowRight, Check, Inbox, Landmark, Plus, RefreshCw, Users, X } from 'lucide-react'
import { DrawablyBadge, DrawablyButton, DrawablyInput, DrawablySelect } from 'drawably/react'
import { usePlaidLink } from 'react-plaid-link'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { api, ApiClientError } from '../api'
import { EmptyNote, ErrorNote } from '../components/Feedback'
import { money, shortDate } from '../format'
import type { BankConnection, BankTransaction, Group } from '../types'

function message(caught: unknown) {
  return caught instanceof ApiClientError ? caught.message : 'Something went wrong. Please try again.'
}

export function DashboardPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [linkToken, setLinkToken] = useState<string | null>(null)
  const [wantLink, setWantLink] = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()
  const [shareTransaction, setShareTransaction] = useState<BankTransaction | null>(null)
  const [shareGroupId, setShareGroupId] = useState('')
  const [sharing, setSharing] = useState(false)
  const [newGroupOpen, setNewGroupOpen] = useState(false)
  const [newGroupName, setNewGroupName] = useState('')
  const queryClient = useQueryClient()

  const dashboard = useQuery({
    queryKey: ['dashboard'],
    queryFn: async () => {
      const [connections, transactions, groups, suggestions] = await Promise.all([
        api.bankConnections(), api.bankTransactions(), api.groups(), api.suggestions(),
      ])
      return { connections, transactions, groups, suggestions }
    },
  })
  const { connections = [], transactions = [], groups = [], suggestions = [] } = dashboard.data ?? {}
  const refresh = useCallback(async () => { await queryClient.invalidateQueries({ queryKey: ['dashboard'] }) }, [queryClient])

  const onSuccess = useCallback(async (publicToken: string | null, metadata: { institution: { institution_id: string; name: string } | null }) => {
    if (!publicToken) return
    try {
      await api.exchangePublicToken(publicToken, metadata.institution?.institution_id, metadata.institution?.name)
      await refresh()
    } catch (caught) { setError(message(caught)) }
  }, [refresh])

  const plaid = usePlaidLink({ token: linkToken, onSuccess, onExit: (plaidError) => { if (plaidError) setError(plaidError.display_message ?? plaidError.error_message) } })

  const connectBank = useCallback(async () => {
    setError(''); setWantLink(true)
    try { setLinkToken((await api.createLinkToken()).linkToken) }
    catch (caught) { setWantLink(false); setError(message(caught)) }
  }, [])

  useEffect(() => {
    if (wantLink && linkToken && plaid.ready) { plaid.open(); setWantLink(false) }
  }, [wantLink, linkToken, plaid])

  useEffect(() => {
    if (searchParams.get('connect') === '1') {
      void connectBank()
      const next = new URLSearchParams(searchParams); next.delete('connect'); setSearchParams(next, { replace: true })
    }
  }, [connectBank, searchParams, setSearchParams])

  const availableTransactions = useMemo(() => transactions.filter((transaction) => !transaction.expenseId), [transactions])

  async function sync(connection: BankConnection) {
    setError('')
    try { await api.syncBankConnection(connection.id); await refresh() }
    catch (caught) { setError(message(caught)) }
  }

  async function importSharedTransaction(event: FormEvent) {
    event.preventDefault()
    const group = groups.find((candidate) => candidate.id === shareGroupId)
    if (!shareTransaction || !group) return
    setSharing(true); setError('')
    try {
      await api.importTransaction(shareTransaction.id, { groupId: group.id, description: shareTransaction.merchantName ?? shareTransaction.name, splitStrategy: 'EQUAL', splits: group.members.map((member) => ({ userId: member.userId })) })
      setShareTransaction(null); setShareGroupId(''); await refresh()
    } catch (caught) { setError(message(caught)) }
    finally { setSharing(false) }
  }

  async function createGroup(event: FormEvent) {
    event.preventDefault(); setError('')
    try { await api.createGroup(newGroupName); setNewGroupName(''); setNewGroupOpen(false); await refresh() }
    catch (caught) { setError(message(caught)) }
  }

  if (dashboard.isLoading) return <div className="page-loader"><Spinner size="large" label="Reading your activity" /></div>

  async function decideSuggestion(suggestionId: string, decision: 'confirm' | 'reject') {
    setError('')
    try {
      if (decision === 'confirm') await api.confirmSuggestion(suggestionId)
      else await api.rejectSuggestion(suggestionId)
      await refresh()
    } catch (caught) { setError(message(caught)) }
  }

  return (
    <div className="dashboard-page">
      <section className="dashboard-intro"><h1>Start with the transaction.</h1><p>Choose what belonged to the group. We’ll keep the bigger picture, so nobody has to count every cent.</p></section>
      {(error || dashboard.error) && <ErrorNote role="alert">{error || message(dashboard.error)}</ErrorNote>}

      <Tabs.Root className="dashboard-tabs" value={location.hash === '#groups' ? 'groups' : location.hash === '#suggestions' ? 'suggestions' : 'activity'} onValueChange={(value) => navigate(value === 'activity' ? '/app' : `/app#${value}`)}>
        <Tabs.List aria-label="Dashboard views"><Tabs.Trigger value="activity">Activity</Tabs.Trigger><Tabs.Trigger value="suggestions">Suggestions {suggestions.length ? `(${suggestions.length})` : ''}</Tabs.Trigger><Tabs.Trigger value="groups">Friend groups</Tabs.Trigger></Tabs.List>
        <Tabs.Content value="activity">
          <section className="bank-strip" id="banks" aria-labelledby="banks-title">
            <div><span className="section-icon"><Landmark /></span><div><h2 id="banks-title">Connected banks</h2><p>{connections.length ? `${connections.length} connection${connections.length === 1 ? '' : 's'} keeping the trail current.` : 'Your real purchases are the beginning of the story.'}</p></div></div>
            <DrawablyButton variant="solid" onClick={connectBank} disabled={wantLink}>{connections.length ? 'Add another bank' : 'Connect a bank'} <ArrowRight /></DrawablyButton>
            {connections.map((connection) => <div className="connection-row" key={connection.id}><div><strong>{connection.institutionName}</strong><span>{connection.accounts.map((account) => `${account.name}${account.mask ? ` · ${account.mask}` : ''}`).join(', ')}</span></div><DrawablyBadge className={connection.status === 'HEALTHY' ? 'status-good' : 'status-error'} variant="outline">{connection.status === 'HEALTHY' ? 'Connected' : 'Needs attention'}</DrawablyBadge><button className="icon-button" type="button" onClick={() => void sync(connection)} aria-label={`Sync ${connection.institutionName}`}><RefreshCw /></button></div>)}
          </section>

          <section className="transaction-section" aria-labelledby="transactions-title">
            <div className="section-heading"><div><h2 id="transactions-title">Recent transactions</h2></div><span>{availableTransactions.length} ready to review</span></div>
            {connections.length === 0 ? <EmptyNote title="Connect a bank to begin">Your purchases will appear here, ready to add to a friend group.</EmptyNote> : availableTransactions.length === 0 ? <EmptyNote title="The page is clean">Sync your bank to check for new transactions.</EmptyNote> : <div className="transaction-list">
              {availableTransactions.map((transaction) => <article className="transaction-row" key={transaction.id}><span className="merchant-mark">{(transaction.merchantName ?? transaction.name)[0]}</span><div className="transaction-main"><strong>{transaction.merchantName ?? transaction.name}</strong><span>{transaction.accountName} · {shortDate(transaction.postedDate)}{transaction.pending ? ' · pending' : ''}</span></div><b>{money(transaction.amountCents, transaction.isoCurrencyCode)}</b><DrawablyButton variant="outline" onClick={() => { setShareTransaction(transaction); setShareGroupId(groups[0]?.id ?? '') }}>Share <ArrowRight /></DrawablyButton></article>)}
            </div>}
          </section>
        </Tabs.Content>

        <Tabs.Content value="suggestions">
          <section className="suggestion-section" id="suggestions" aria-labelledby="suggestions-title">
            <div className="section-heading"><div><h2 id="suggestions-title">Suggestion inbox</h2><p>We found purchases that may belong to a group. You make the final call.</p></div><span>{suggestions.length} to review</span></div>
            {suggestions.length === 0 ? <EmptyNote title="Nothing waiting">New suggestions will appear after your bank transactions sync.</EmptyNote> : <div className="suggestion-list">{suggestions.map((suggestion) => <article key={suggestion.id}>
              <span className="suggestion-icon"><Inbox /></span>
              <div className="suggestion-copy"><strong>{suggestion.merchantName}</strong><span>{suggestion.groupName} · {shortDate(suggestion.postedDate)}</span><small>{suggestion.reasons.join(' · ')}</small></div>
              <b>{money(suggestion.amountCents, suggestion.isoCurrencyCode)}</b>
              <div className="suggestion-actions"><DrawablyButton variant="outline" onClick={() => void decideSuggestion(suggestion.id, 'reject')}>Not this one</DrawablyButton><DrawablyButton variant="solid" onClick={() => void decideSuggestion(suggestion.id, 'confirm')}><Check /> Add to {suggestion.groupName}</DrawablyButton></div>
            </article>)}</div>}
          </section>
        </Tabs.Content>

        <Tabs.Content value="groups">
          <section className="groups-section" id="groups" aria-labelledby="groups-title"><div className="section-heading"><div><h2 id="groups-title">Friend groups</h2></div><DrawablyButton variant="solid" onClick={() => setNewGroupOpen(true)}><Plus /> New group</DrawablyButton></div>
            {groups.length === 0 ? <EmptyNote title="Make your first circle">Create a group, invite friends, then share a bank transaction with them.</EmptyNote> : <div className="group-list">{groups.map((group) => <Link className="group-row" to={`/app/groups/${group.id}`} key={group.id}><span className="group-faces">{group.members.slice(0, 3).map((member) => <i key={member.userId}>{member.displayName[0]}</i>)}</span><span><strong>{group.name}</strong><small>{group.members.length} friends</small></span><ArrowRight /></Link>)}</div>}
          </section>
        </Tabs.Content>
      </Tabs.Root>

      <Dialog.Root open={Boolean(shareTransaction)} onOpenChange={(open) => { if (!open) setShareTransaction(null) }}><Dialog.Portal><Dialog.Overlay className="dialog-overlay" /><Dialog.Content className="dialog-sheet"><Dialog.Close className="dialog-close"><X /><span className="sr-only">Close</span></Dialog.Close><Dialog.Title>Who shared this?</Dialog.Title><Dialog.Description>{shareTransaction ? `${shareTransaction.merchantName ?? shareTransaction.name} · ${money(shareTransaction.amountCents, shareTransaction.isoCurrencyCode)}` : ''}</Dialog.Description>{groups.length ? <form onSubmit={importSharedTransaction}><label>Friend group<DrawablySelect required value={shareGroupId} onChange={(event) => setShareGroupId(event.target.value)}><option value="">Choose a group</option>{groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}</DrawablySelect></label><p className="form-hint"><Check /> This will split the purchase equally for now. You can adjust the expense later.</p><DrawablyButton type="submit" variant="solid" state={sharing ? 'loading' : 'idle'} disabled={!shareGroupId || sharing}>Add to the shared trail</DrawablyButton></form> : <EmptyNote title="Create a group first">A transaction needs a friend group before it can become a shared expense.</EmptyNote>}</Dialog.Content></Dialog.Portal></Dialog.Root>

      <Dialog.Root open={newGroupOpen} onOpenChange={setNewGroupOpen}><Dialog.Portal><Dialog.Overlay className="dialog-overlay" /><Dialog.Content className="dialog-sheet"><Dialog.Close className="dialog-close"><X /><span className="sr-only">Close</span></Dialog.Close><Dialog.Title>Start a friend group</Dialog.Title><Dialog.Description>Give the circle a name. You can invite people next.</Dialog.Description><form onSubmit={createGroup}><label>Group name<DrawablyInput required value={newGroupName} onChange={(event) => setNewGroupName(event.target.value)} placeholder="Sunday dinner crew" /></label><DrawablyButton type="submit" variant="solid"><Users /> Create group</DrawablyButton></form></Dialog.Content></Dialog.Portal></Dialog.Root>
    </div>
  )
}
