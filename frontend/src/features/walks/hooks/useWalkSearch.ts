import { useCallback, useEffect, useState } from 'react'
import { loadWalks, readWalkFilters, type WalkSearchResult } from '../api/search'

interface SearchState { key: string; attempt: number; result?: WalkSearchResult; error?: string }

export function useWalkSearch(query: string) {
  const [state, setState] = useState<SearchState>({ key: '', attempt: -1 })
  const [attempt, setAttempt] = useState(0)
  const reload = useCallback(() => setAttempt((value) => value + 1), [])

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    async function search() {
      try {
        const filters = readWalkFilters(new URLSearchParams(query))
        const result = await loadWalks(filters, controller.signal)
        if (active) setState({ key: query, attempt, result })
      } catch (error) {
        if (active && !controller.signal.aborted) setState({ key: query, attempt,
          error: error instanceof Error ? error.message : 'No pudimos conectar con el servidor.' })
      }
    }
    void search()
    return () => { active = false; controller.abort() }
  }, [query, attempt])

  const current = state.key === query && state.attempt === attempt ? state : undefined
  return { result: current?.result, error: current?.error, loading: !current, reload }
}
