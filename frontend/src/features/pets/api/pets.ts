import { apiClient } from '@/shared/api/client'

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export interface Pet {
  id: string
  name: string
  breed: string | null
  size: 'SMALL' | 'MEDIUM' | 'LARGE' | null
  notes: string | null
  createdAt: string
}

function isPet(value: unknown): value is Pet {
  if (!value || typeof value !== 'object') return false
  const p = value as Pet
  return uuid.test(p.id) && typeof p.name === 'string'
}

// mínimo necesario para elegir una mascota al reservar un paseo; el alta/edición
// de mascotas todavía no tiene cliente HTTP propio (ver PetsPage/PetFormPage)
export async function loadMyPets(signal?: AbortSignal): Promise<Pet[]> {
  const { data } = await apiClient.get<Pet[]>('/pets', { signal })
  if (!Array.isArray(data) || !data.every(isPet)) {
    throw new Error('No pudimos cargar tus mascotas.')
  }
  return data
}
