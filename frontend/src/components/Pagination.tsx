import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  disabled?: boolean
}

/** Previous / next controls for the 0-based pages returned by the API. */
export function Pagination({ page, totalPages, onPageChange, disabled }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }
  return (
    <nav aria-label="Pagination" className="flex items-center justify-between gap-2">
      <Button variant="outline" size="sm" disabled={disabled || page <= 0} onClick={() => onPageChange(page - 1)}>
        <ChevronLeft aria-hidden />
        Previous
      </Button>
      <span className="text-sm text-muted-foreground">
        Page {page + 1} of {totalPages}
      </span>
      <Button
        variant="outline"
        size="sm"
        disabled={disabled || page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Next
        <ChevronRight aria-hidden />
      </Button>
    </nav>
  )
}
