import { useCallback, useEffect, useState } from 'react'
import { loadMyPets, type Pet } from '../api/pets'

interface PetsState {
  attempt: number
  pets?: Pet[]
  error?: string
}

export function usePets() {
  const [state, setState] = useState<PetsState>({ attempt: -1 })
  const [attempt, setAttempt] = useState(0)
  const reload = useCallback(() => setAttempt((value) => value + 1), [])

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    loadMyPets(controller.signal).then((pets) => {
      if (active) setState({ attempt, pets })
    }).catch((error: unknown) => {
      if (!active || controller.signal.aborted) return
      setState({ attempt, error: error instanceof Error ? error.message : 'No pudimos cargar tus mascotas.' })
    })

    return () => {
      active = false
      controller.abort()
    }
  }, [attempt])

  const current = state.attempt === attempt ? state : undefined
  return { pets: current?.pets, error: current?.error, loading: !current, reload }
}
