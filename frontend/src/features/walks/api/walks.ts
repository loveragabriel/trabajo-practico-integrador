import { apiClient } from '@/shared/api/client'
import { isWalkDetails, validWalkId, type AgendaFilters, type WalkAgenda, type WalkDetails } from './walkReadContract'

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
