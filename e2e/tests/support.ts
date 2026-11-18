import { expect, type Page } from '@playwright/test'

export const ADMIN = { email: 'admin@example.com', password: 'Admin12345!' }

const MAILPIT = process.env.MAILPIT_URL ?? 'http://localhost:8025'

/** A fresh address per run, so the tests never depend on what is already in the database. */
export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`
}

interface MailpitSummary {
  ID: string
  Subject: string
}

/**
 * Waits for the newest email to `to` in Mailpit and returns the first link to `path` in it,
 * so the test follows the same path as a real user clicking the email.
 */
export async function linkFromEmail(to: string, path: '/activate' | '/reset-password'): Promise<string> {
  let message: MailpitSummary | undefined
  await expect
    .poll(
      async () => {
        const response = await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:"${to}"`)}`)
        const body = (await response.json()) as { messages: MailpitSummary[] }
        message = body.messages[0] // newest first
        return message?.ID
      },
      { message: `an email to ${to}`, timeout: 15_000 },
    )
    .toBeTruthy()

  const response = await fetch(`${MAILPIT}/api/v1/message/${message!.ID}`)
  const { HTML } = (await response.json()) as { HTML: string }
  const match = HTML.match(new RegExp(`href="([^"]*${path}\\?token=[^"]+)"`))
  expect(match, `a ${path} link in "${message!.Subject}"`).not.toBeNull()
  return match![1]!.replaceAll('&amp;', '&')
}

export async function signIn(page: Page, area: 'employee' | 'admin', email: string, password: string) {
  await page.goto(area === 'admin' ? '/admin/login' : '/login')
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
}

export async function signOut(page: Page) {
  await page.getByRole('button', { name: 'Account menu' }).click()
  await page.getByRole('menuitem', { name: 'Sign out' }).click()
}
