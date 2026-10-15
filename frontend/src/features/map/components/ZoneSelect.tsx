import { useId } from 'react'
import type { Zone } from '../types'

interface ZoneSelectProps {
  zones: Zone[]
  value: string
  onChange: (zoneId: string) => void
  label?: string
  disabled?: boolean
  required?: boolean
  name?: string
}

/** Controlled selector: IDs always come from the API, never a second static catalog. */
export function ZoneSelect({
  zones, value, onChange, label = 'Barrio', disabled = false, required = false, name = 'zoneId',
}: ZoneSelectProps) {
  const id = useId()
  const validValue = zones.some((zone) => zone.id === value) ? value : ''

  return (
    <div className="space-y-2">
      <label htmlFor={id} className="block text-sm font-medium text-slate-800">{label}</label>
      <select
        id={id}
        name={name}
        value={validValue}
        onChange={(event) => onChange(event.target.value)}
        disabled={disabled || zones.length === 0}
        required={required}
        className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 focus:border-emerald-600 focus:outline-2 focus:outline-emerald-600 disabled:bg-slate-100"
      >
        <option value="">Seleccioná un barrio</option>
        {zones.map((zone) => (
          <option key={zone.id} value={zone.id}>{zone.name} — {zone.city}</option>
        ))}
      </select>
    </div>
  )
}
