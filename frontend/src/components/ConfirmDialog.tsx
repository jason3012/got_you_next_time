import * as Dialog from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import { DrawablyButton } from 'drawably/react'

type Props = {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  busy?: boolean
  onConfirm: () => void | Promise<void>
  onOpenChange: (open: boolean) => void
}

export function ConfirmDialog({ open, title, description, confirmLabel, busy = false, onConfirm, onOpenChange }: Props) {
  return <Dialog.Root open={open} onOpenChange={onOpenChange}><Dialog.Portal>
    <Dialog.Overlay className="dialog-overlay" />
    <Dialog.Content className="dialog-sheet dialog-sheet--confirm">
      <Dialog.Close className="dialog-close" disabled={busy}><X /><span className="sr-only">Close</span></Dialog.Close>
      <Dialog.Title>{title}</Dialog.Title>
      <Dialog.Description>{description}</Dialog.Description>
      <div className="confirmation-actions">
        <Dialog.Close asChild><DrawablyButton variant="outline" disabled={busy}>Cancel</DrawablyButton></Dialog.Close>
        <DrawablyButton variant="solid" className="danger-action" state={busy ? 'loading' : 'idle'} disabled={busy} onClick={() => void onConfirm()}>{confirmLabel}</DrawablyButton>
      </div>
    </Dialog.Content>
  </Dialog.Portal></Dialog.Root>
}
