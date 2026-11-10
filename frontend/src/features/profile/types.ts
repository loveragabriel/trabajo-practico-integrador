import type { Role } from '@/shared/types/user'

export interface UserProfile {
  id: string
  fullName: string
  email: string
  phone: string | null
  role: Role
  zoneId: string
  active: boolean
  defaultPrice: number | null
  defaultCapacity: number | null
  ratingAvg: number
  reviewCount: number
  createdAt: string
}
