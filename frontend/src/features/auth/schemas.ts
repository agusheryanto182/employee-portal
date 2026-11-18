import { z } from 'zod'

/** Mirrors the backend rules so most mistakes are caught before a request is sent. */
export const PASSWORD_MIN = 8
export const PASSWORD_MAX = 72

export const signInSchema = z.object({
  email: z.email('Enter a valid email address'),
  password: z.string().min(1, 'Enter your password'),
})
export type SignInValues = z.infer<typeof signInSchema>

export const newPasswordSchema = z
  .object({
    password: z
      .string()
      .min(PASSWORD_MIN, `Use at least ${PASSWORD_MIN} characters`)
      .max(PASSWORD_MAX, `Use at most ${PASSWORD_MAX} characters`),
    confirmPassword: z.string(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  })
export type NewPasswordValues = z.infer<typeof newPasswordSchema>
