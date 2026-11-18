import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { CircleCheck, Loader2, TriangleAlert } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { FormField } from '@/components/FormField'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { api, toProblem } from '@/lib/api'
import type { AccountTokenInfo, AccountTokenType } from '@/lib/types'
import { newPasswordSchema, PASSWORD_MIN, type NewPasswordValues } from './schemas'

const SUBMIT_PATH: Record<AccountTokenType, string> = {
  INVITE: '/auth/activate',
  RESET: '/auth/reset-password',
}

const COPY: Record<AccountTokenType, { intro: string; submit: string; done: string }> = {
  INVITE: {
    intro: 'Choose a password to activate your account',
    submit: 'Activate account',
    done: 'Your account is active. You can now sign in with your new password.',
  },
  RESET: {
    intro: 'Choose a new password for',
    submit: 'Set new password',
    done: 'Your password has been changed and you have been signed out on every device. Sign in with your new password.',
  },
}

interface SetPasswordFormProps {
  /** Which kind of link this page expects; a link of the other kind is treated as invalid. */
  type: AccountTokenType
  token: string | undefined
  /** Shown after success, for example links to the sign-in pages. */
  signInLinks: ReactNode
}

/** Checks an invite or reset link first, then asks for the new password (docs/auth-flows.md). */
export function SetPasswordForm({ type, token, signInLinks }: SetPasswordFormProps) {
  const [done, setDone] = useState(false)
  const info = useQuery({
    queryKey: ['account-token', token],
    queryFn: async () => (await api.get<AccountTokenInfo>(`/auth/tokens/${encodeURIComponent(token!)}`)).data,
    enabled: Boolean(token),
    retry: false,
    staleTime: Infinity,
  })

  if (done) {
    return (
      <div className="grid gap-4">
        <Alert>
          <CircleCheck aria-hidden />
          <AlertTitle>All set</AlertTitle>
          <AlertDescription>{COPY[type].done}</AlertDescription>
        </Alert>
        {signInLinks}
      </div>
    )
  }

  if (token && info.isPending) {
    return (
      <div className="grid gap-3" aria-label="Checking your link">
        <Skeleton className="h-5 w-3/4" />
        <Skeleton className="h-9 w-full" />
        <Skeleton className="h-9 w-full" />
      </div>
    )
  }

  if (!token || info.isError || info.data?.type !== type) {
    const unreachable = info.isError && toProblem(info.error).status === 0
    return (
      <Alert variant="destructive">
        <TriangleAlert aria-hidden />
        <AlertTitle>{unreachable ? 'Cannot check this link' : 'This link is invalid or has expired'}</AlertTitle>
        <AlertDescription>
          {unreachable
            ? 'Check your connection and reload the page.'
            : 'Links work once and expire after a while. Ask your administrator to send a new one.'}
        </AlertDescription>
      </Alert>
    )
  }

  return <PasswordFields type={type} token={token} info={info.data} onDone={() => setDone(true)} />
}

function PasswordFields({
  type,
  token,
  info,
  onDone,
}: {
  type: AccountTokenType
  token: string
  info: AccountTokenInfo
  onDone: () => void
}) {
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<NewPasswordValues>({
    resolver: zodResolver(newPasswordSchema),
    defaultValues: { password: '', confirmPassword: '' },
  })

  const onSubmit = handleSubmit(async ({ password }) => {
    setFormError(null)
    try {
      await api.post(SUBMIT_PATH[type], { token, password })
      onDone()
    } catch (error) {
      const problem = toProblem(error)
      if (problem.errors?.password) {
        setError('password', { message: problem.errors.password })
      } else {
        setFormError(problem.detail)
      }
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate className="grid gap-4">
      <p className="text-sm text-muted-foreground">
        Hi <span className="font-medium text-foreground">{info.fullName}</span>. {COPY[type].intro}{' '}
        <span className="font-medium text-foreground">{info.email}</span>.
      </p>
      {formError ? (
        <Alert variant="destructive">
          <AlertDescription>{formError}</AlertDescription>
        </Alert>
      ) : null}
      {/* Lets password managers save the new password under the right account. */}
      <input type="email" autoComplete="username" value={info.email} readOnly hidden />
      <FormField
        id="password"
        label="New password"
        error={errors.password?.message}
        hint={`At least ${PASSWORD_MIN} characters.`}
      >
        <Input
          id="password"
          type="password"
          autoComplete="new-password"
          autoFocus
          aria-invalid={errors.password ? true : undefined}
          {...register('password')}
        />
      </FormField>
      <FormField id="confirmPassword" label="Confirm password" error={errors.confirmPassword?.message}>
        <Input
          id="confirmPassword"
          type="password"
          autoComplete="new-password"
          aria-invalid={errors.confirmPassword ? true : undefined}
          {...register('confirmPassword')}
        />
      </FormField>
      <Button type="submit" disabled={isSubmitting} className="w-full">
        {isSubmitting ? <Loader2 className="animate-spin" aria-hidden /> : null}
        {COPY[type].submit}
      </Button>
    </form>
  )
}
