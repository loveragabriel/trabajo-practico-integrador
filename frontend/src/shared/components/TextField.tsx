import { useId, type InputHTMLAttributes, type ReactNode } from 'react'
import { Icon } from './Icon'

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  icon: string
  trailing?: ReactNode
}

// input con ícono a la izquierda y label arriba; trailing es para una acción
// dentro del campo (ej. mostrar/ocultar contraseña), no para texto de ayuda
export function TextField({ label, icon, trailing, id, className = '', ...props }: TextFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  return (
    <div className="space-y-1.5">
      <label htmlFor={inputId} className="block text-label-md font-semibold text-on-surface">
        {label}
      </label>
      <div className="relative flex items-center">
        <Icon name={icon} className="pointer-events-none absolute left-3.5 text-[20px] text-on-surface-variant" />
        <input
          id={inputId}
          className={`h-[50px] w-full rounded-lg bg-surface-container-low text-body-md text-on-surface placeholder:text-outline transition-all focus:bg-surface-container-lowest focus:shadow-[0_0_0_2px_var(--color-primary)] focus:outline-none ${trailing ? 'pr-12' : 'pr-4'} pl-11 ${className}`}
          {...props}
        />
        {trailing}
      </div>
    </div>
  )
}
