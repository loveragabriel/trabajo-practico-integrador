import { useCallback, useEffect, useState } from 'react'
import { ApiRequestError } from '@/shared/api/client'
import { getCoverage, type Coverage } from '../api/coverage'

interface CoverageState {
  attempt: number
  coverage?: Coverage
  error?: string
}

export function useCoverage() {
  const [state, setState] = useState<CoverageState>({ attempt: -1 })
  const [attempt, setAttempt] = useState(0)
  const reload = useCallback(() => setAttempt((value) => value + 1), [])

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    getCoverage(controller.signal).then((coverage) => {
      if (active) setState({ attempt, coverage })
    }).catch((error: unknown) => {
      if (!active || controller.signal.aborted) return
      const message = error instanceof ApiRequestError && error.status === 401
        ? 'Ingresá nuevamente para ver tu cobertura.'
        : 'No pudimos cargar tu cobertura. Intentá nuevamente.'
      setState({ attempt, error: message })
    })

    return () => {
      active = false
      controller.abort()
    }
  }, [attempt])

  const current = state.attempt === attempt ? state : undefined
  return { coverage: current?.coverage, error: current?.error, loading: !current, reload }
}
