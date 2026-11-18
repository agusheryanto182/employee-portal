import { Link } from '@tanstack/react-router'
import { ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { User } from '@/lib/types'
import { useCreateUser, type UserInput } from './queries'
import { UserForm } from './UserForm'

/** Registers a user as "invited" and emails them a link to set their own password (ADR 0004). */
export function CreateUserPage({ onCreated }: { onCreated: (user: User) => void }) {
  const create = useCreateUser()

  async function submit(values: UserInput) {
    const user = await create.mutateAsync(values)
    toast.success(`Invite sent to ${user.email}`)
    onCreated(user)
  }

  return (
    <div className="mx-auto grid w-full max-w-xl gap-6">
      <Button variant="ghost" size="sm" className="w-fit" asChild>
        <Link to="/admin/users">
          <ArrowLeft aria-hidden />
          All users
        </Link>
      </Button>
      <Card>
        <CardHeader>
          <CardTitle>Add user</CardTitle>
          <CardDescription>
            They get an email with a link to set their password. Until then the account shows as invited.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <UserForm submitLabel="Add and send invite" onSubmit={submit} />
        </CardContent>
      </Card>
    </div>
  )
}
