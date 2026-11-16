import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ConfirmDialog } from './ConfirmDialog'

describe('ConfirmDialog', () => {
  it('names the consequence and requires an explicit action', () => {
    const confirm = vi.fn()
    render(<ConfirmDialog open title="Delete dinner?" description="This recalculates every balance." confirmLabel="Delete expense" onConfirm={confirm} onOpenChange={() => {}} />)

    expect(screen.getByRole('dialog', { name: 'Delete dinner?' })).toHaveTextContent('This recalculates every balance.')
    fireEvent.click(screen.getByRole('button', { name: 'Delete expense' }))
    expect(confirm).toHaveBeenCalledOnce()
  })
})
