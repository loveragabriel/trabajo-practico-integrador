import { useId, type ReactNode, type SelectHTMLAttributes } from 'react'
import { Icon } from './Icon'

interface SelectFieldProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  icon: string
  iconFilled?: boolean
  iconClassName?: string
  children: ReactNode
}

// mismo patrón visual que TextField, pero para <select>: label + ícono a la
// izquierda + flecha a la derecha (reemplaza la de la plataforma, por eso appearance-none)
export function SelectField({
  label, icon, iconFilled, iconClassName = 'text-on-surface-variant', children, id, className = '', ...props
}: SelectFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  return (
    <div className="space-y-1.5">
      <label htmlFor={inputId} className="block text-label-md font-semibold text-on-surface">
        {label}
      </label>
      <div className="relative flex items-center">
        <Icon name={icon} filled={iconFilled} className={`pointer-events-none absolute left-3.5 text-[20px] ${iconClassName}`} />
        <select
          id={inputId}
          className={`h-[50px] w-full appearance-none rounded-lg bg-surface-container-low pl-11 pr-10 text-body-md text-on-surface transition-all focus:bg-surface-container-lowest focus:shadow-[0_0_0_2px_var(--color-primary)] focus:outline-none ${className}`}
          {...props}
        >
          {children}
        </select>
        <Icon name="expand_more" className="pointer-events-none absolute right-3.5 text-[20px] text-on-surface-variant" />
      </div>
    </div>
  )
}
