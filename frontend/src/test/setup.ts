import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { session } from '@/lib/session'
import { server } from './server'

// jsdom has no layout; the router's scroll restoration calls this.
window.scrollTo = () => {}

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  cleanup()
  server.resetHandlers()
  session.clear()
})
afterAll(() => server.close())
