import { Check, Copy, KeyRound, Loader2, Mail, UserCheck, UserX } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { toProblem } from '@/lib/api'
import type { User } from '@/lib/types'
import { useSendLink, useSetActive } from './queries'

interface UserActionsProps {
  user: User
  isSelf: boolean
}

interface SentLink {
  kind: 'invite' | 'reset'
  link: string
}

/**
 * What an admin can do with an account depends on its status (see docs/database.md):
 * invited users get a new invite, active users a password reset, deactivated users can come back.
 */
export function UserActions({ user, isSelf }: UserActionsProps) {
  const setActive = useSetActive(user.id)
  const sendLink = useSendLink(user.id)
  const [sent, setSent] = useState<SentLink | null>(null)

  function send(kind: SentLink['kind']) {
    sendLink.mutate(kind, {
      onSuccess: (link) => {
        setSent({ kind, link })
        toast.success(kind === 'invite' ? `Invite sent to ${user.email}` : `Reset link sent to ${user.email}`)
      },
      onError: (error) => toast.error(toProblem(error).detail),
    })
  }

  function changeActive(active: boolean) {
    setActive.mutate(active, {
      onSuccess: (updated) => {
        setSent(null) // deactivating invalidated the links; a reactivated user needs a fresh one
        toast.success(active ? `${updated.fullName} can sign in again` : `${updated.fullName} has been deactivated`)
      },
      onError: (error) => toast.error(toProblem(error).detail),
    })
  }

  return (
    <div className="grid gap-4">
      <p className="text-sm text-muted-foreground">{describeStatus(user)}</p>

      <div className="flex flex-wrap gap-2">
        {user.status === 'PENDING' ? (
          <Button variant="outline" onClick={() => send('invite')} disabled={sendLink.isPending}>
            {sendLink.isPending ? <Loader2 className="animate-spin" aria-hidden /> : <Mail aria-hidden />}
            Resend invite
          </Button>
        ) : null}
        {user.status === 'ACTIVE' ? (
          <Button variant="outline" onClick={() => send('reset')} disabled={sendLink.isPending}>
            {sendLink.isPending ? <Loader2 className="animate-spin" aria-hidden /> : <KeyRound aria-hidden />}
            Send password reset
          </Button>
        ) : null}
        {user.status === 'INACTIVE' ? (
          <Button variant="outline" onClick={() => changeActive(true)} disabled={setActive.isPending}>
            {setActive.isPending ? <Loader2 className="animate-spin" aria-hidden /> : <UserCheck aria-hidden />}
            Reactivate
          </Button>
        ) : isSelf ? null : (
          <DeactivateButton user={user} pending={setActive.isPending} onConfirm={() => changeActive(false)} />
        )}
      </div>

      {isSelf ? (
        <p className="text-sm text-muted-foreground">
          This is your own account. To change your password, use My account.
        </p>
      ) : null}

      {sent ? <CopyLink sent={sent} email={user.email} /> : null}
    </div>
  )
}

function describeStatus(user: User): string {
  switch (user.status) {
    case 'PENDING':
      return 'Invited and has not set a password yet. Resending the invite makes earlier links stop working.'
    case 'ACTIVE':
      return 'Can sign in. A password reset emails a link to choose a new password.'
    case 'INACTIVE':
      return 'Deactivated: cannot sign in. Reactivating lets them sign in again with their old password.'
  }
}

function DeactivateButton({ user, pending, onConfirm }: { user: User; pending: boolean; onConfirm: () => void }) {
  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant="destructive" disabled={pending}>
          {pending ? <Loader2 className="animate-spin" aria-hidden /> : <UserX aria-hidden />}
          Deactivate
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Deactivate {user.fullName}?</AlertDialogTitle>
          <AlertDialogDescription>
            They are signed out on every device and cannot sign in until you reactivate the account. Their login
            history is kept.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={onConfirm}>
            Deactivate
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

/** The "copy link" fallback for when the email does not arrive (ADR 0004). */
function CopyLink({ sent, email }: { sent: SentLink; email: string }) {
  const [copied, setCopied] = useState(false)

  async function copy() {
    try {
      await navigator.clipboard.writeText(sent.link)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      toast.error('Could not copy. Select the link and copy it manually.')
    }
  }

  return (
    <Alert>
      <AlertTitle>{sent.kind === 'invite' ? 'Invite link' : 'Password reset link'}</AlertTitle>
      <AlertDescription className="grid gap-2">
        <p>
          Emailed to {email}. If it does not arrive, send them this link another way. It works once and expires
          like the emailed one.
        </p>
        <div className="flex gap-2">
          <Input
            readOnly
            value={sent.link}
            aria-label="Link"
            onFocus={(event) => event.currentTarget.select()}
            className="font-mono text-xs"
          />
          <Button type="button" variant="outline" onClick={copy}>
            {copied ? <Check aria-hidden /> : <Copy aria-hidden />}
            {copied ? 'Copied' : 'Copy'}
          </Button>
        </div>
      </AlertDescription>
    </Alert>
  )
}
