import { apiClient } from '@/shared/api/client'
import type { WalkStatus } from '@/features/walks/api/walkReadContract'

export interface Booking {
  id: string; walkId: string; petId: string; petName: string; walkerName: string; zoneName: string
  startTime: string; endTime: string; bookingStatus: 'JOINED' | 'CANCELLED'; walkStatus: WalkStatus; canCancel: boolean
}

export async function listBookings(page: number, signal: AbortSignal) {
  return (await apiClient.get<Booking[]>('/walks/bookings/mine', { params: { page }, signal })).data
}
export async function listWalkBookings(walkId: string, signal: AbortSignal) {
  return (await apiClient.get<Booking[]>(`/walks/${walkId}/bookings`, { signal })).data
}
export async function reserveWalk(walkId: string, petIds: string[]) {
  await apiClient.post(`/walks/${walkId}/bookings`, { petIds })
}
export async function cancelBooking(booking: Booking) {
  await apiClient.delete(`/walks/${booking.walkId}/bookings/${booking.id}`)
}
