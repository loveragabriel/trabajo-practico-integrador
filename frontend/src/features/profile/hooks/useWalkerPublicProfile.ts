import { useCallback, useEffect, useState } from 'react'
import { ApiRequestError } from '@/shared/api/client'
import { getWalkerPublicProfile, type WalkerPublicProfile } from '../api/walkerPublicProfile'

interface State {
  attempt: number
  profile?: WalkerPublicProfile
  error?: string
}

export function useWalkerPublicProfile(walkerId: string) {
  const [state, setState] = useState<State>({ attempt: -1 })
  const [attempt, setAttempt] = useState(0)
  const reload = useCallback(() => setAttempt((value) => value + 1), [])

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    getWalkerPublicProfile(walkerId, controller.signal).then((profile) => {
      if (active) setState({ attempt, profile })
    }).catch((error: unknown) => {
      if (!active || controller.signal.aborted) return
      const message = error instanceof ApiRequestError && error.status === 404
        ? 'No encontramos a este paseador.'
        : 'No pudimos cargar el perfil del paseador.'
      setState({ attempt, error: message })
    })

    return () => {
      active = false
      controller.abort()
    }
  }, [walkerId, attempt])

  const current = state.attempt === attempt ? state : undefined
  return { profile: current?.profile, error: current?.error, loading: !current, reload }
}
