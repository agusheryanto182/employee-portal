import { describe, expect, it } from 'vitest'
import { describeFailure, describeUserAgent, initials, localDayRange } from './format'

describe('format', () => {
  it('describes common user agents', () => {
    expect(
      describeUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36'),
    ).toBe('Chrome on Windows')
    expect(
      describeUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1'),
    ).toBe('Safari on iOS')
    expect(describeUserAgent(null)).toBe('Unknown device')
  })

  it('explains failure reasons in plain words', () => {
    expect(describeFailure('RATE_LIMITED')).toBe('Blocked after too many attempts')
  })

  it('builds initials from the first two names', () => {
    expect(initials('Budi Santoso Wijaya')).toBe('BS')
  })
})

describe('localDayRange', () => {
  it('covers whole local days: from the start of the first to the start of the day after the last', () => {
    const { from, to } = localDayRange('2026-11-17', '2026-11-18')
    expect(new Date(from!)).toEqual(new Date(2026, 10, 17))
    expect(new Date(to!)).toEqual(new Date(2026, 10, 19))
  })

  it('leaves out a side that is not set', () => {
    expect(localDayRange(undefined, undefined)).toEqual({ from: undefined, to: undefined })
    expect(localDayRange('2026-12-31').to).toBeUndefined()
  })
})
