import type { PetSize } from '@/features/pets/api/pets'
import { apiClient, ApiRequestError } from '@/shared/api/client'
import { isWalkDetails, validWalkId, type AgendaFilters, type WalkAgenda, type WalkDetails, type WalkStatus } from './walkReadContract'

export interface JoinedPet {
  petId: string
  petName: string
  breed: string | null
  size: PetSize | null
  notes: string | null
  owner: { name: string; phone: string | null }
}

function isJoinedPet(value: unknown): value is JoinedPet {
  if (!value || typeof value !== 'object') return false
  const pet = value as JoinedPet
  return validWalkId(pet.petId) && typeof pet.petName === 'string' && !!pet.owner && typeof pet.owner.name === 'string'
}

// solo la ve el paseador dueño del walk: quién se sumó, con el contacto del dueño de cada mascota
export async function loadJoinedPets(walkId: string, signal?: AbortSignal): Promise<JoinedPet[]> {
  if (!validWalkId(walkId)) throw new Error('El identificador del paseo no es válido.')
  const { data } = await apiClient.get<JoinedPet[]>(`/walks/${walkId}/pets`, { signal })
  if (!Array.isArray(data) || !data.every(isJoinedPet)) {
    throw new Error('No pudimos cargar las mascotas de este paseo.')
  }
  return data
}

export async function loadWalkAgenda(filters: AgendaFilters, signal: AbortSignal): Promise<WalkAgenda> {
  const { data } = await apiClient.get<WalkAgenda>('/walks/mine', { params: { ...filters, size: 12 }, signal })
  if (!data || !Array.isArray(data.items) || !data.items.every(isWalkDetails)
    || data.page !== filters.page || data.size !== 12 || data.items.length > 12 || typeof data.hasNext !== 'boolean') {
    throw new Error('La agenda recibida no tiene un formato válido.')
  }
  return data
}

export async function loadWalkDetail(id: string, signal: AbortSignal): Promise<WalkDetails> {
  if (!validWalkId(id)) throw new Error('El identificador del paseo no es válido.')
  const { data } = await apiClient.get<WalkDetails>(`/walks/${id}`, { signal })
  if (!isWalkDetails(data) || data.id !== id) throw new Error('El detalle recibido no tiene un formato válido.')
  return data
}

// RN-29/31/32/33: solo el paseador dueño puede cancelar/iniciar/completar su paseo.
// PUBLISHED y FULL no se pueden forzar: son automáticos según los cupos (RN-30).
export async function updateWalkStatus(id: string, status: Exclude<WalkStatus, 'PUBLISHED' | 'FULL'>): Promise<void> {
  await apiClient.patch(`/walks/${id}/status`, { status })
}

// el backend ya devuelve el mensaje de negocio en español (RN-xx); no hace falta traducirlo
export function walkActionError(error: unknown): string {
  if (error instanceof ApiRequestError) {
    if (error.status === 401) return 'Ingresá nuevamente para continuar.'
    return error.message
  }
  return error instanceof Error ? error.message : 'No pudimos completar la operación.'
}
