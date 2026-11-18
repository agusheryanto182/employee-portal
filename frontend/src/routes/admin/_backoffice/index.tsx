import { createFileRoute, redirect } from '@tanstack/react-router'

/** `/admin` is the backoffice home; user management is its main screen. */
export const Route = createFileRoute('/admin/_backoffice/')({
  beforeLoad: () => {
    throw redirect({ to: '/admin/users', replace: true })
  },
})
