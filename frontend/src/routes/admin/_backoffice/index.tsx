import { createFileRoute } from '@tanstack/react-router'
import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export const Route = createFileRoute('/admin/_backoffice/')({
  component: () => (
    <Card>
      <CardHeader>
        <CardTitle>Backoffice</CardTitle>
        <CardDescription>User management and the login history of every account arrive in the next sprint.</CardDescription>
      </CardHeader>
    </Card>
  ),
})
