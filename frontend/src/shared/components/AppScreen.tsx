import type { ReactNode } from 'react'

interface AppScreenProps {
  children: ReactNode
  className?: string
}

// pantallas que no comparten el nav de AppLayout (login, register): fondo a lo
// ancho de la ventana, contenido centrado con el ancho de un mobile aunque se
// abra en desktop. El diseño de Stitch es mobile-first, no pensado para ancho libre.
export function AppScreen({ children, className = '' }: AppScreenProps) {
  return (
    <main className="flex min-h-screen w-full flex-col items-center bg-surface pt-safe pb-safe">
      <div className={`flex w-full max-w-md flex-col px-5 pb-safe ${className}`}>{children}</div>
    </main>
  )
}
