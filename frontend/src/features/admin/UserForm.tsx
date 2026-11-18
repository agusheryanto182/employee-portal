import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2 } from 'lucide-react'
import type { ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { FormField } from '@/components/FormField'
import { NativeSelect } from '@/components/NativeSelect'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { toProblem } from '@/lib/api'
import { emailSchema, fullNameSchema, phoneSchema } from '@/lib/validation'
import type { UserInput } from './queries'
import { ROLE_LABELS } from './labels'

const userSchema = z.object({
  email: emailSchema,
  fullName: fullNameSchema,
  phone: phoneSchema,
  role: z.enum(['EMPLOYEE', 'ADMIN']),
})

const FIELDS = ['email', 'fullName', 'phone', 'role'] as const

interface UserFormProps {
  defaultValues?: UserInput
  submitLabel: string
  /** Resolves when saved; a rejected promise's API errors are shown next to the fields. */
  onSubmit: (values: UserInput) => Promise<unknown>
  /** An admin cannot change their own role. */
  roleLocked?: boolean
  emailHint?: string
  footer?: ReactNode
}

/** Email, name, phone and role: used to add a user and to edit one. */
export function UserForm({ defaultValues, submitLabel, onSubmit, roleLocked, emailHint, footer }: UserFormProps) {
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<UserInput>({
    resolver: zodResolver(userSchema),
    defaultValues: defaultValues ?? { email: '', fullName: '', phone: '', role: 'EMPLOYEE' },
  })

  async function submit(values: UserInput) {
    try {
      await onSubmit(values)
      reset(values) // the saved values are the new baseline for "unsaved changes"
    } catch (error) {
      const problem = toProblem(error)
      let shown = false
      for (const field of FIELDS) {
        const message = problem.errors?.[field]
        if (message) {
          setError(field, { message: `${message.charAt(0).toUpperCase()}${message.slice(1)}` })
          shown = true
        }
      }
      if (problem.status === 409 && /email/i.test(problem.detail)) {
        setError('email', { message: problem.detail })
        shown = true
      }
      if (!shown) toast.error(problem.detail)
    }
  }

  const editing = defaultValues !== undefined

  return (
    <form onSubmit={handleSubmit(submit)} noValidate className="grid gap-4">
      <FormField id="email" label="Email" error={errors.email?.message} hint={emailHint}>
        <Input
          id="email"
          type="email"
          autoComplete="off"
          aria-invalid={errors.email ? true : undefined}
          {...register('email')}
        />
      </FormField>
      <FormField id="fullName" label="Full name" error={errors.fullName?.message}>
        <Input id="fullName" autoComplete="off" aria-invalid={errors.fullName ? true : undefined} {...register('fullName')} />
      </FormField>
      <FormField id="phone" label="Phone" error={errors.phone?.message} hint="Optional.">
        <Input id="phone" type="tel" autoComplete="off" aria-invalid={errors.phone ? true : undefined} {...register('phone')} />
      </FormField>
      <FormField
        id="role"
        label="Role"
        error={errors.role?.message}
        hint={
          roleLocked
            ? 'You cannot change your own role.'
            : 'Employees use the dashboard; admins use only this backoffice.'
        }
      >
        <NativeSelect id="role" disabled={roleLocked} {...register('role')}>
          {(Object.keys(ROLE_LABELS) as UserInput['role'][]).map((role) => (
            <option key={role} value={role}>
              {ROLE_LABELS[role]}
            </option>
          ))}
        </NativeSelect>
      </FormField>
      <div className="flex flex-wrap items-center justify-end gap-2">
        {footer}
        <Button type="submit" disabled={isSubmitting || (editing && !isDirty)}>
          {isSubmitting ? <Loader2 className="animate-spin" aria-hidden /> : null}
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}
