import { Link } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'

/** After activating or resetting, the user picks the sign-in page of their area (ADR 0012). */
export function SignInLinks() {
  return (
    <div className="grid gap-2 text-center text-sm">
      <Button asChild className="w-full">
        <Link to="/login">Sign in</Link>
      </Button>
      <span className="text-muted-foreground">
        Administrator?{' '}
        <Link to="/admin/login" className="font-medium text-foreground underline-offset-4 hover:underline">
          Sign in to the backoffice
        </Link>
      </span>
    </div>
  )
}
