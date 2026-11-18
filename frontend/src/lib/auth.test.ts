import { describe, expect, it } from 'vitest'
import { afterSignInPath, homeFor } from './auth'

describe('afterSignInPath', () => {
  it('sends each role to its own home by default', () => {
    expect(homeFor('EMPLOYEE')).toBe('/')
    expect(homeFor('ADMIN')).toBe('/admin')
    expect(afterSignInPath('EMPLOYEE', undefined)).toBe('/')
  })

  it('returns to the page the user came from inside their own area', () => {
    expect(afterSignInPath('EMPLOYEE', '/history?page=2')).toBe('/history?page=2')
    expect(afterSignInPath('ADMIN', '/admin/account')).toBe('/admin/account')
  })

  it('never redirects into the other area, back to a sign-in page or off the site', () => {
    expect(afterSignInPath('EMPLOYEE', '/admin/account')).toBe('/')
    expect(afterSignInPath('ADMIN', '/history')).toBe('/admin')
    expect(afterSignInPath('ADMIN', '/admin/login')).toBe('/admin')
    expect(afterSignInPath('EMPLOYEE', '//evil.example.com')).toBe('/')
    expect(afterSignInPath('EMPLOYEE', 'https://evil.example.com')).toBe('/')
  })
})
