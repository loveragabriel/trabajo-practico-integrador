import { useEffect, useMemo } from 'react'
import { Circle, MapContainer, TileLayer, Tooltip, useMap } from 'react-leaflet'
import { latLngBounds } from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { Zone } from '../types'
import { CABA_CENTER, locateZone, type LatLng } from '../utils/zoneLocations'

// Radio aproximado de un barrio: alcanza para mostrar cobertura, no es un límite real.
const BARRIO_RADIUS_METERS = 700
const CABA_ZOOM = 11
const SINGLE_ZONE_ZOOM = 14

interface LocatedZone {
  zone: Zone
  position: LatLng
}

interface ZoneCoverageMapProps {
  zones: Zone[]
  className?: string
}

/** Muestra las zonas recibidas sobre OpenStreetMap. Solo lectura: sin GPS ni edición. */
export function ZoneCoverageMap({ zones, className = 'h-80' }: ZoneCoverageMapProps) {
  const located = useMemo(() => zones.flatMap((zone) => {
    const position = locateZone(zone)
    return position ? [{ zone, position }] : []
  }), [zones])

  return (
    // isolate evita que los z-index internos de Leaflet tapen el resto del layout.
    <div className={`isolate overflow-hidden rounded-md border border-slate-300 ${className}`}>
      <MapContainer
        center={CABA_CENTER}
        zoom={CABA_ZOOM}
        scrollWheelZoom={false}
        className="h-full w-full"
        aria-label="Mapa de barrios"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {located.map(({ zone, position }) => (
          <Circle
            key={zone.id}
            center={position}
            radius={BARRIO_RADIUS_METERS}
            pathOptions={{ color: '#059669', fillColor: '#10b981', fillOpacity: 0.3, weight: 2 }}
          >
            <Tooltip>{zone.name}</Tooltip>
          </Circle>
        ))}
        <FitToZones located={located} />
      </MapContainer>
    </div>
  )
}

/** Reencuadra el mapa cada vez que cambian las zonas: MapContainer solo usa center/zoom al montarse. */
function FitToZones({ located }: { located: LocatedZone[] }) {
  const map = useMap()

  useEffect(() => {
    if (located.length === 0) {
      map.setView(CABA_CENTER, CABA_ZOOM)
    } else if (located.length === 1) {
      map.setView(located[0].position, SINGLE_ZONE_ZOOM)
    } else {
      map.fitBounds(latLngBounds(located.map(({ position }) => [position.lat, position.lng])), { padding: [24, 24] })
    }
  }, [map, located])

  return null
}
