import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '@/features/auth'
import { loadWalks, readWalkFilters, WalkSearchAuthError, type WalkSearchResult } from '../api/search'

interface SearchState { key: string; attempt: number; result?: WalkSearchResult; error?: string }

export function useWalkSearch(query: string) {
  const { session, signOut } = useAuth()
  const token = session?.token
  const key = `${token ?? ''}:${query}`
  const [state, setState] = useState<SearchState>({ key: '', attempt: -1 })
  const [attempt, setAttempt] = useState(0)
  const reload = useCallback(() => setAttempt((value) => value + 1), [])

  useEffect(() => {
    if (!token) return
    const controller = new AbortController()
    let active = true
    async function search() {
      try {
        const filters = readWalkFilters(new URLSearchParams(query))
        const result = await loadWalks(filters, controller.signal, token)
        if (active) setState({ key, attempt, result })
      } catch (error) {
        if (active && error instanceof WalkSearchAuthError) signOut()
        if (active && !controller.signal.aborted) setState({ key, attempt,
          error: error instanceof Error ? error.message : 'No pudimos conectar con el servidor.' })
      }
    }
    void search()
    return () => { active = false; controller.abort() }
  }, [key, query, token, attempt, signOut])

  const current = state.key === key && state.attempt === attempt ? state : undefined
  return { result: current?.result, error: current?.error, loading: !current, reload }
}
