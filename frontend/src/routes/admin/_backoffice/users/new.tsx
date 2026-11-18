import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { CreateUserPage } from '@/features/admin/CreateUserPage'

export const Route = createFileRoute('/admin/_backoffice/users/new')({
  component: NewUserRoute,
})

function NewUserRoute() {
  const navigate = useNavigate()
  return (
    <CreateUserPage
      onCreated={(user) => navigate({ to: '/admin/users/$userId', params: { userId: user.id }, replace: true })}
    />
  )
}
