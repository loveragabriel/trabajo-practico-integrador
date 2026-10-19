import { useEffect, useState } from 'react'
import { apiClient } from '@/shared/api/client'
import type { Zone } from '@/features/map'

// Consulta a Zona debe ser público para el registro de usuarios.
export function usePublicZones() {
  const [zones, setZones] = useState<Zone[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    apiClient
      .get<Zone[]>('/zones')
      .then(({ data }) => {
        if (active) setZones(data)
      })
      .catch(() => {
        if (active) setZones([])
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  return { zones, loading }
}
