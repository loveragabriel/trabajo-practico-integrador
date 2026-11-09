import type { ButtonHTMLAttributes, ReactNode } from 'react'

interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode
  tone?: 'default' | 'muted'
}

// botón circular chico (header de pantallas tipo app: volver, ayuda, etc.)
export function IconButton({ children, tone = 'default', className = '', type = 'button', ...props }: IconButtonProps) {
  const toneClass = tone === 'muted' ? 'text-on-surface-variant' : 'text-on-surface'
  return (
    <button
      type={type}
      className={`flex h-10 w-10 items-center justify-center rounded-full bg-surface-container-lowest shadow-sm transition-transform active:scale-95 ${toneClass} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
