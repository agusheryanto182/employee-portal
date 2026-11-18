import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2 } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { FormField } from '@/components/FormField'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { toProblem } from '@/lib/api'
import { signIn, type Portal } from '@/lib/auth'
import type { Session } from '@/lib/session'
import { signInSchema, type SignInValues } from './schemas'

function signInError(error: unknown): string {
  const problem = toProblem(error)
  if (problem.status === 429) {
    const minutes = problem.retryAfterSeconds ? Math.ceil(problem.retryAfterSeconds / 60) : undefined
    return minutes
      ? `Too many failed attempts. Try again in ${minutes} minute${minutes === 1 ? '' : 's'}.`
      : 'Too many failed attempts. Try again later.'
  }
  if (problem.status === 401) {
    return 'Invalid email or password.'
  }
  return problem.detail
}

interface SignInFormProps {
  portal: Portal
  onSignedIn: (session: Session) => void
}

export function SignInForm({ portal, onSignedIn }: SignInFormProps) {
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignInValues>({ resolver: zodResolver(signInSchema), defaultValues: { email: '', password: '' } })

  const onSubmit = handleSubmit(async ({ email, password }) => {
    setFormError(null)
    try {
      onSignedIn(await signIn(portal, email, password))
    } catch (error) {
      setFormError(signInError(error))
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate className="grid gap-4">
      {formError ? (
        <Alert variant="destructive">
          <AlertDescription>{formError}</AlertDescription>
        </Alert>
      ) : null}
      <FormField id="email" label="Email" error={errors.email?.message}>
        <Input
          id="email"
          type="email"
          autoComplete="username"
          autoFocus
          aria-invalid={errors.email ? true : undefined}
          aria-describedby={errors.email ? 'email-error' : undefined}
          {...register('email')}
        />
      </FormField>
      <FormField id="password" label="Password" error={errors.password?.message}>
        <Input
          id="password"
          type="password"
          autoComplete="current-password"
          aria-invalid={errors.password ? true : undefined}
          aria-describedby={errors.password ? 'password-error' : undefined}
          {...register('password')}
        />
      </FormField>
      <Button type="submit" disabled={isSubmitting} className="w-full">
        {isSubmitting ? <Loader2 className="animate-spin" aria-hidden /> : null}
        Sign in
      </Button>
    </form>
  )
}
