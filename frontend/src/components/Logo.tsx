import { Building2 } from 'lucide-react'

export function Logo({ subtitle }: { subtitle?: string }) {
  return (
    <div className="flex items-center gap-2 font-semibold">
      <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
        <Building2 className="size-4" aria-hidden />
      </span>
      <span>
        Employee Portal
        {subtitle ? <span className="ml-1.5 font-normal text-muted-foreground">{subtitle}</span> : null}
      </span>
    </div>
  )
}
