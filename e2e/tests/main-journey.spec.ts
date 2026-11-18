import { expect, test } from '@playwright/test'
import { ADMIN, linkFromEmail, signIn, signOut, uniqueEmail } from './support'

/**
 * The main journey from the project plan, end to end through the real email:
 * the admin invites an employee, who activates the account from the email, signs in, checks the
 * login history (including failed attempts), edits the profile and signs out. Then the admin
 * sees those attempts and deactivates the account.
 */
test.describe.serial('main journey', () => {
  const employee = { email: '', name: 'Dewi Lestari', password: 'Employee123!' }

  test.beforeAll(({}, testInfo) => {
    employee.email = uniqueEmail(`e2e-${testInfo.project.name}`) // one employee per project and retry
  })

  test('the admin invites an employee', async ({ page }) => {
    await signIn(page, 'admin', ADMIN.email, ADMIN.password)
    await expect(page).toHaveURL('/admin/users')

    await page.getByRole('link', { name: 'Add user' }).click()
    await page.getByLabel('Email', { exact: true }).fill(employee.email)
    await page.getByLabel('Full name').fill(employee.name)
    await page.getByRole('button', { name: 'Add and send invite' }).click()

    await expect(page).toHaveURL(/\/admin\/users\/[0-9a-f-]{36}$/)
    await expect(page.getByText(`Invite sent to ${employee.email}`)).toBeVisible()
    await expect(page.getByText('Invited', { exact: true }).first()).toBeVisible()
  })

  test('the employee activates the account from the invite email', async ({ page }) => {
    const link = await linkFromEmail(employee.email, '/activate')
    await page.goto(link)

    await page.getByLabel('New password').fill(employee.password)
    await page.getByLabel('Confirm password').fill(employee.password)
    await page.getByRole('button', { name: 'Activate account' }).click()
    await expect(page.getByText('All set')).toBeVisible()

    // The link works once.
    await page.goto(link)
    await expect(page.getByText('This link is invalid or has expired')).toBeVisible()
  })

  test('the employee signs in, and failed attempts show in the history', async ({ page }) => {
    // The backoffice sign-in page does not accept employees.
    await signIn(page, 'admin', employee.email, employee.password)
    await expect(page.getByText('Invalid email or password.')).toBeVisible()

    await signIn(page, 'employee', employee.email, 'wrong-password')
    await expect(page.getByText('Invalid email or password.')).toBeVisible()

    await page.getByLabel('Password', { exact: true }).fill(employee.password)
    await page.getByRole('button', { name: 'Sign in' }).click()
    await expect(page).toHaveURL('/')
    await expect(page.getByText('Welcome back, Dewi')).toBeVisible()

    await page.goto('/history')
    const history = page.getByRole('main')
    await expect(history.getByText('Signed in')).toBeVisible()
    await expect(history.getByText('Wrong email or password')).toBeVisible()
    await expect(history.getByText('Used the other sign-in page')).toBeVisible()

    // Employees stay out of the backoffice.
    await page.goto('/admin')
    await expect(page).toHaveURL('/')
  })

  test('the employee edits the profile and signs out', async ({ page }) => {
    await signIn(page, 'employee', employee.email, employee.password)
    await expect(page).toHaveURL('/')

    await page.goto('/profile')
    await page.getByLabel('Full name').fill('Dewi Lestari Putri')
    await page.getByLabel('Phone').fill('+62 812 3456 7890')
    await page.getByRole('button', { name: 'Save changes' }).click()
    await expect(page.getByText('Profile saved')).toBeVisible()

    await page.reload() // saved on the server, and the session survives a reload
    await expect(page.getByLabel('Full name')).toHaveValue('Dewi Lestari Putri')
    await expect(page.getByLabel('Phone')).toHaveValue('+62 812 3456 7890')

    await signOut(page)
    await expect(page).toHaveURL('/login')

    // After signing out, a protected page asks to sign in again.
    await page.goto('/history')
    await expect(page).toHaveURL(/\/login\?redirect=/)
  })

  test('the admin sees the attempts and deactivates the employee', async ({ page }) => {
    await signIn(page, 'admin', ADMIN.email, ADMIN.password)
    await expect(page).toHaveURL('/admin/users')

    await page.getByLabel('Search by name or email').fill(employee.email)
    await page.getByRole('row').filter({ hasText: employee.email }).getByRole('link', { name: 'Dewi Lestari Putri' }).click()
    await expect(page.getByText('Wrong email or password')).toBeVisible()

    await page.getByRole('button', { name: 'Deactivate' }).click()
    await page.getByRole('alertdialog').getByRole('button', { name: 'Deactivate' }).click()
    await expect(page.getByRole('button', { name: 'Reactivate' })).toBeVisible()
    await signOut(page)

    await signIn(page, 'employee', employee.email, employee.password)
    await expect(page.getByText('Invalid email or password.')).toBeVisible()
  })
})
