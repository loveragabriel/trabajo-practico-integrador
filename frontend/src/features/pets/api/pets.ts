import { apiClient } from '@/shared/api/client'

export const petSizeLabels = { SMALL: 'Pequeño', MEDIUM: 'Mediano', LARGE: 'Grande' } as const
export type PetSize = keyof typeof petSizeLabels
export interface PetInput { name: string; breed: string | null; size: PetSize | null; notes: string | null }
export interface Pet extends PetInput { id: string; createdAt: string }

export async function listPets(signal: AbortSignal) {
  return (await apiClient.get<Pet[]>('/pets', { signal })).data
}

export async function getPet(id: string, signal: AbortSignal) {
  return (await apiClient.get<Pet>(`/pets/${id}`, { signal })).data
}

export async function savePet(pet: PetInput, id?: string) {
  return id ? (await apiClient.put<Pet>(`/pets/${id}`, pet)).data
    : (await apiClient.post<Pet>('/pets', pet)).data
}
