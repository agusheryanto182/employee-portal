import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { FormField } from '@/components/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { api, toProblem } from '@/lib/api'
import { session } from '@/lib/session'
import type { AuthResponse } from '@/lib/types'
import { PASSWORD_MAX, PASSWORD_MIN } from '@/features/auth/schemas'

const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Enter your current password'),
    newPassword: z
      .string()
      .min(PASSWORD_MIN, `Use at least ${PASSWORD_MIN} characters`)
      .max(PASSWORD_MAX, `Use at most ${PASSWORD_MAX} characters`),
    confirmPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  })
type ChangePasswordValues = z.infer<typeof changePasswordSchema>

const FIELD_LABELS: Record<string, string> = {
  currentPassword: 'Current password',
  newPassword: 'New password',
}

export function ChangePasswordForm() {
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<ChangePasswordValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  })

  const change = useMutation({
    mutationFn: async ({ currentPassword, newPassword }: ChangePasswordValues) =>
      (await api.put<AuthResponse>('/me/password', { currentPassword, newPassword })).data,
    onSuccess: (response) => {
      // The API signed out every device and returned a fresh session for this one.
      session.set(response)
      reset()
      toast.success('Password changed. Other devices have been signed out.')
    },
    onError: (error) => {
      const problem = toProblem(error)
      for (const field of ['currentPassword', 'newPassword'] as const) {
        const message = problem.errors?.[field]
        if (message) setError(field, { message: `${FIELD_LABELS[field]} ${message}` })
      }
      if (!problem.errors) toast.error(problem.detail)
    },
  })

  return (
    <Card>
      <form onSubmit={handleSubmit((values) => change.mutate(values))} noValidate>
        <CardHeader>
          <CardTitle>Change password</CardTitle>
          <CardDescription>You stay signed in here. Every other device is signed out.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 py-6">
          <FormField id="currentPassword" label="Current password" error={errors.currentPassword?.message}>
            <Input
              id="currentPassword"
              type="password"
              autoComplete="current-password"
              aria-invalid={errors.currentPassword ? true : undefined}
              {...register('currentPassword')}
            />
          </FormField>
          <FormField
            id="newPassword"
            label="New password"
            error={errors.newPassword?.message}
            hint={`At least ${PASSWORD_MIN} characters.`}
          >
            <Input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              aria-invalid={errors.newPassword ? true : undefined}
              {...register('newPassword')}
            />
          </FormField>
          <FormField id="confirmPassword" label="Confirm new password" error={errors.confirmPassword?.message}>
            <Input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              aria-invalid={errors.confirmPassword ? true : undefined}
              {...register('confirmPassword')}
            />
          </FormField>
        </CardContent>
        <CardFooter className="justify-end">
          <Button type="submit" disabled={change.isPending}>
            {change.isPending ? <Loader2 className="animate-spin" aria-hidden /> : null}
            Change password
          </Button>
        </CardFooter>
      </form>
    </Card>
  )
}
