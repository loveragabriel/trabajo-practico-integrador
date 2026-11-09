import { apiClient } from '@/shared/api/client'

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export type PetSize = 'SMALL' | 'MEDIUM' | 'LARGE'

export const petSizeLabels: Record<PetSize, string> = {
  SMALL: 'Pequeño',
  MEDIUM: 'Mediano',
  LARGE: 'Grande',
}

export interface Pet {
  id: string
  name: string
  breed: string | null
  size: PetSize | null
  notes: string | null
  createdAt: string
}

export interface PetInput {
  name: string
  breed?: string
  size?: PetSize
  notes?: string
}

function isPet(value: unknown): value is Pet {
  if (!value || typeof value !== 'object') return false
  const p = value as Pet
  return uuid.test(p.id) && typeof p.name === 'string'
}

export async function loadMyPets(signal?: AbortSignal): Promise<Pet[]> {
  const { data } = await apiClient.get<Pet[]>('/pets', { signal })
  if (!Array.isArray(data) || !data.every(isPet)) {
    throw new Error('No pudimos cargar tus mascotas.')
  }
  return data
}

export async function getPet(id: string, signal?: AbortSignal): Promise<Pet> {
  const { data } = await apiClient.get<Pet>(`/pets/${id}`, { signal })
  return data
}

export async function createPet(payload: PetInput): Promise<Pet> {
  const { data } = await apiClient.post<Pet>('/pets', payload)
  return data
}

export async function updatePet(id: string, payload: PetInput): Promise<Pet> {
  const { data } = await apiClient.put<Pet>(`/pets/${id}`, payload)
  return data
}
