import { useCallback, useEffect, useState } from 'react'
import { ApiRequestError } from '@/shared/api/client'
import { getMyProfile } from '../api/profile'
import type { UserProfile } from '../types'

interface ProfileState {
  attempt: number
  profile?: UserProfile
  error?: string
}

export function useProfile() {
  const [state, setState] = useState<ProfileState>({ attempt: -1 })
  const [attempt, setAttempt] = useState(0)
  const reload = useCallback(() => setAttempt((value) => value + 1), [])

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    getMyProfile(controller.signal).then((profile) => {
      if (active) setState({ attempt, profile })
    }).catch((error: unknown) => {
      if (!active || controller.signal.aborted) return
      const message = error instanceof ApiRequestError && error.status === 401
        ? 'Ingresá nuevamente para ver tu perfil.'
        : 'No pudimos cargar tu perfil. Intentá nuevamente.'
      setState({ attempt, error: message })
    })

    return () => {
      active = false
      controller.abort()
    }
  }, [attempt])

  const current = state.attempt === attempt ? state : undefined
  return { profile: current?.profile, error: current?.error, loading: !current, reload }
}
