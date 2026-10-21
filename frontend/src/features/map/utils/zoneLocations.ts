import type { Zone } from '../types'

export interface LatLng {
  lat: number
  lng: number
}

// Centro aproximado de cada barrio de CABA. La tabla zones no guarda coordenadas
// (decisión de arquitectura): este catálogo existe solo para dibujar en el mapa.
// La clave es el nombre normalizado, porque los UUID cambian si se recrea la base.
const BARRIO_LOCATIONS: Record<string, LatLng> = {
  'agronomia': { lat: -34.5937, lng: -58.4915 },
  'almagro': { lat: -34.6097, lng: -58.4206 },
  'balvanera': { lat: -34.6094, lng: -58.4019 },
  'barracas': { lat: -34.6450, lng: -58.3825 },
  'belgrano': { lat: -34.5627, lng: -58.4565 },
  'boedo': { lat: -34.6302, lng: -58.4190 },
  'caballito': { lat: -34.6187, lng: -58.4400 },
  'chacarita': { lat: -34.5860, lng: -58.4540 },
  'coghlan': { lat: -34.5600, lng: -58.4750 },
  'colegiales': { lat: -34.5735, lng: -58.4490 },
  'constitucion': { lat: -34.6275, lng: -58.3850 },
  'flores': { lat: -34.6340, lng: -58.4630 },
  'floresta': { lat: -34.6290, lng: -58.4850 },
  'la boca': { lat: -34.6345, lng: -58.3631 },
  'la paternal': { lat: -34.5980, lng: -58.4680 },
  'liniers': { lat: -34.6420, lng: -58.5200 },
  'mataderos': { lat: -34.6600, lng: -58.5050 },
  'monte castro': { lat: -34.6170, lng: -58.5060 },
  'montserrat': { lat: -34.6125, lng: -58.3810 },
  'nueva pompeya': { lat: -34.6510, lng: -58.4170 },
  'nunez': { lat: -34.5450, lng: -58.4600 },
  'palermo': { lat: -34.5780, lng: -58.4250 },
  'parque avellaneda': { lat: -34.6470, lng: -58.4800 },
  'parque chacabuco': { lat: -34.6360, lng: -58.4370 },
  'parque chas': { lat: -34.5850, lng: -58.4790 },
  'parque patricios': { lat: -34.6380, lng: -58.4010 },
  'puerto madero': { lat: -34.6140, lng: -58.3630 },
  'recoleta': { lat: -34.5880, lng: -58.3970 },
  'retiro': { lat: -34.5920, lng: -58.3760 },
  'saavedra': { lat: -34.5530, lng: -58.4860 },
  'san cristobal': { lat: -34.6240, lng: -58.4020 },
  'san nicolas': { lat: -34.6030, lng: -58.3810 },
  'san telmo': { lat: -34.6210, lng: -58.3720 },
  'versalles': { lat: -34.6300, lng: -58.5210 },
  'villa crespo': { lat: -34.5990, lng: -58.4380 },
  'villa del parque': { lat: -34.6050, lng: -58.4900 },
  'villa devoto': { lat: -34.6010, lng: -58.5130 },
  'villa general mitre': { lat: -34.6110, lng: -58.4680 },
  'villa lugano': { lat: -34.6760, lng: -58.4720 },
  'villa luro': { lat: -34.6380, lng: -58.5030 },
  'villa ortuzar': { lat: -34.5800, lng: -58.4680 },
  'villa pueyrredon': { lat: -34.5810, lng: -58.5040 },
  'villa real': { lat: -34.6190, lng: -58.5250 },
  'villa riachuelo': { lat: -34.6900, lng: -58.4700 },
  'villa santa rita': { lat: -34.6150, lng: -58.4820 },
  'villa soldati': { lat: -34.6640, lng: -58.4430 },
  'villa urquiza': { lat: -34.5730, lng: -58.4870 },
  'velez sarsfield': { lat: -34.6320, lng: -58.4920 },
}

/** Centro de CABA, usado cuando no hay ninguna zona para mostrar. */
export const CABA_CENTER: LatLng = { lat: -34.6118, lng: -58.4173 }

/** "Villa  Ortúzar " → "villa ortuzar": sin acentos, minúsculas y espacios simples. */
export function normalizeZoneName(name: string): string {
  return name.normalize('NFD').replace(/[̀-ͯ]/g, '').trim().replace(/\s+/g, ' ').toLowerCase()
}

/** Devuelve null si el barrio no está en el catálogo: el mapa lo omite sin romperse. */
export function locateZone(zone: Pick<Zone, 'name'>): LatLng | null {
  return BARRIO_LOCATIONS[normalizeZoneName(zone.name)] ?? null
}
