import { z } from 'zod'

/** Field rules shared by the profile and backoffice forms; they mirror the backend's `UserValidation`. */
export const fullNameSchema = z
  .string()
  .trim()
  .min(1, 'Enter a full name')
  .max(150, 'Use at most 150 characters')

/** Optional: an empty string means "no phone number". */
export const phoneSchema = z
  .string()
  .trim()
  .max(30, 'Use at most 30 characters')
  .regex(/^$|^\+?[0-9 ()-]{6,30}$/, 'Enter a valid phone number')

export const emailSchema = z.string().trim().max(255, 'Use at most 255 characters').pipe(z.email('Enter a valid email address'))
