import { useCallback, useEffect, useState } from 'react'
import { ApiRequestError } from '@/shared/api/client'
import { getPet, type Pet } from '../api/pets'

interface PetState {
  attempt: number
  pet?: Pet
  error?: string
}

// sin petId (alta) no hay nada que cargar: loading queda en false y pet en undefined
export function usePet(petId?: string) {
  const [state, setState] = useState<PetState>({ attempt: -1 })
  const [attempt, setAttempt] = useState(0)
  const reload = useCallback(() => setAttempt((value) => value + 1), [])

  useEffect(() => {
    if (!petId) return
    const controller = new AbortController()
    let active = true

    getPet(petId, controller.signal).then((pet) => {
      if (active) setState({ attempt, pet })
    }).catch((error: unknown) => {
      if (!active || controller.signal.aborted) return
      const message = error instanceof ApiRequestError && error.status === 404
        ? 'No encontramos esa mascota.'
        : 'No pudimos cargar la mascota.'
      setState({ attempt, error: message })
    })

    return () => {
      active = false
      controller.abort()
    }
  }, [petId, attempt])

  if (!petId) return { pet: undefined, error: undefined, loading: false, reload }
  const current = state.attempt === attempt ? state : undefined
  return { pet: current?.pet, error: current?.error, loading: !current, reload }
}
