import { useCallback, useEffect, useState } from 'react'
import { loadZones, ZoneRequestError } from '../api/zones'
import type { Zone } from '../types'

interface CatalogState {
  token: string | null
  attempt: number
  zones: Zone[]
  loading: boolean
  error: string | null
  needsAuthentication: boolean
}

const idle: CatalogState = {
  token: null, attempt: 0, zones: [], loading: false, error: null, needsAuthentication: true,
}

export function useZones(token: string | null) {
  const [state, setState] = useState<CatalogState>(idle)
  const [attempt, setAttempt] = useState(0)
  const reload = useCallback(() => setAttempt((value) => value + 1), [])

  useEffect(() => {
    if (!token) return

    const controller = new AbortController()
    let active = true

    loadZones(token, controller.signal).then((zones) => {
      if (active) setState({ token, attempt, zones, loading: false, error: null, needsAuthentication: false })
    }).catch((error: unknown) => {
      if (!active || controller.signal.aborted) return
      setState({
        token, attempt, zones: [], loading: false,
        error: error instanceof ZoneRequestError ? error.message : 'No pudimos conectar con el catálogo de barrios.',
        needsAuthentication: error instanceof ZoneRequestError && error.status === 401,
      })
    })

    return () => {
      active = false
      controller.abort()
    }
  }, [token, attempt])

  // Never expose a previous account's result while an effect for a new token starts.
  const current = !token ? idle : state.token === token && state.attempt === attempt ? state : {
    ...idle, token, attempt, loading: true, needsAuthentication: false,
  }
  return {
    zones: current.zones,
    loading: current.loading,
    error: current.error,
    needsAuthentication: current.needsAuthentication,
    reload,
  }
}
