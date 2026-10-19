import axios, { type AxiosError } from 'axios'
import { readSession } from './session'

export interface ApiFieldError {
  field: string
  message: string
}

// misma forma que el ApiError del backend (shared/exception/ApiError.java)
interface ApiErrorBody {
  message?: string
  fieldErrors?: ApiFieldError[] | null
}

export class ApiRequestError extends Error {
  readonly status: number
  readonly fieldErrors?: ApiFieldError[]

  constructor(message: string, status: number, fieldErrors?: ApiFieldError[]) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

type Listener = () => void
const unauthorizedListeners = new Set<Listener>()

/** Se suscribe a "la sesión actual dejó de ser válida para la API". Devuelve una función para desuscribirse. */
export function onUnauthorized(listener: Listener): () => void {
  unauthorizedListeners.add(listener)
  return () => unauthorizedListeners.delete(listener)
}

export const apiClient = axios.create({ baseURL: '/api' })

apiClient.interceptors.request.use((config) => {
  const session = readSession()
  if (session) {
    config.headers.set('Authorization', `Bearer ${session.token}`)
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorBody>) => {
    if (!error.response) {
      return Promise.reject(new ApiRequestError('No pudimos conectar con el servidor.', 0))
    }

    const { status, data } = error.response
    // un 401 mientras creíamos tener sesión significa que esa sesión ya no es válida
    if (status === 401 && readSession()) {
      unauthorizedListeners.forEach((listener) => listener())
    }

    const message = data?.message ?? 'Ocurrió un error inesperado.'
    return Promise.reject(new ApiRequestError(message, status, data?.fieldErrors ?? undefined))
  },
)
