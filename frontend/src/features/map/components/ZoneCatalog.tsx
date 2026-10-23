import { useZones } from '../hooks/useZones'
import { ZoneSelect } from './ZoneSelect'

interface ZoneCatalogProps {
  token?: string | null
  value: string
  onChange: (zoneId: string) => void
  required?: boolean
}

export function ZoneCatalog({ token = null, value, onChange, required = false }: ZoneCatalogProps) {
  const { zones, loading, error, needsAuthentication, reload } = useZones(token)

  if (needsAuthentication) {
    return <p role="status" className="text-sm text-slate-600">{error ?? 'Ingresá para consultar los barrios disponibles.'}</p>
  }
  if (loading) return <p role="status" className="text-sm text-slate-600">Cargando barrios…</p>
  if (error) {
    return (
      <div role="alert" className="space-y-2 rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
        <p>{error}</p>
        <button type="button" onClick={reload} className="font-medium underline">Reintentar</button>
      </div>
    )
  }
  if (zones.length === 0) {
    return <p role="status" className="text-sm text-slate-600">No hay barrios disponibles en este momento.</p>
  }

  return (
    <div className="space-y-2">
      <ZoneSelect zones={zones} value={value} onChange={onChange} required={required} />
      {value && !zones.some((zone) => zone.id === value) && (
        <p role="status" className="text-sm text-amber-800">El barrio elegido ya no está disponible. Seleccioná otro.</p>
      )}
    </div>
  )
}
