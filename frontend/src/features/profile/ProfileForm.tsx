import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
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
import type { User } from '@/lib/types'

const profileSchema = z.object({
  fullName: z.string().trim().min(1, 'Enter your full name').max(150, 'Use at most 150 characters'),
  phone: z
    .string()
    .trim()
    .max(30, 'Use at most 30 characters')
    .regex(/^$|^\+?[0-9 ()-]{6,30}$/, 'Enter a valid phone number'),
})
type ProfileValues = z.infer<typeof profileSchema>

export function ProfileForm({ user }: { user: User }) {
  const queryClient = useQueryClient()
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { fullName: user.fullName, phone: user.phone ?? '' },
  })

  const save = useMutation({
    mutationFn: async (values: ProfileValues) => (await api.patch<User>('/me', values)).data,
    onSuccess: (updated) => {
      session.updateUser(updated)
      queryClient.setQueryData(['me'], updated)
      reset({ fullName: updated.fullName, phone: updated.phone ?? '' })
      toast.success('Profile saved')
    },
    onError: (error) => {
      const problem = toProblem(error)
      if (problem.errors?.fullName) setError('fullName', { message: problem.errors.fullName })
      if (problem.errors?.phone) setError('phone', { message: problem.errors.phone })
      if (!problem.errors) toast.error(problem.detail)
    },
  })

  return (
    <Card>
      <form onSubmit={handleSubmit((values) => save.mutate(values))} noValidate>
        <CardHeader>
          <CardTitle>Profile</CardTitle>
          <CardDescription>Your name and phone number. Ask your administrator to change your email.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 py-6">
          <FormField id="email" label="Email">
            <Input id="email" type="email" value={user.email} disabled readOnly />
          </FormField>
          <FormField id="fullName" label="Full name" error={errors.fullName?.message}>
            <Input
              id="fullName"
              autoComplete="name"
              aria-invalid={errors.fullName ? true : undefined}
              {...register('fullName')}
            />
          </FormField>
          <FormField id="phone" label="Phone" error={errors.phone?.message} hint="Optional, for example +62 812 3456 7890.">
            <Input
              id="phone"
              type="tel"
              autoComplete="tel"
              aria-invalid={errors.phone ? true : undefined}
              {...register('phone')}
            />
          </FormField>
        </CardContent>
        <CardFooter className="justify-end">
          <Button type="submit" disabled={!isDirty || save.isPending}>
            {save.isPending ? <Loader2 className="animate-spin" aria-hidden /> : null}
            Save changes
          </Button>
        </CardFooter>
      </form>
    </Card>
  )
}
