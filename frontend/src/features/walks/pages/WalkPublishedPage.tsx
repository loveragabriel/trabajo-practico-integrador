import { useCallback, useState } from 'react'
import { generatePath, Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth'
import { Icon } from '@/shared/components/Icon'
import { formatWalkDate } from '../api/search'
import { type WalkDetails } from '../api/walkReadContract'
import { loadWalkDetail } from '../api/walks'
import { useWalkResource } from '../hooks/useWalkResource'

const MAX_PAW_SLOTS = 12

function shareUrlFor(walk: WalkDetails) {
  return `${window.location.origin}${generatePath(paths.walkDetail, { walkId: walk.id })}`
}

function shareTextFor(walk: WalkDetails, shareUrl: string) {
  return `¡Hola! Abrí un nuevo paseo grupal en ${walk.zoneName} para el ${formatWalkDate(walk.startTime)}. Podés reservar el cupo de tu mascota acá: ${shareUrl}`
}

export function WalkPublishedPage() {
  const { walkId } = useParams<{ walkId: string }>()
  const { session } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const request = useCallback((signal: AbortSignal) => loadWalkDetail(walkId ?? '', signal), [walkId])
  const { data: fetchedWalk, loading, error } = useWalkResource(`${session?.token ?? ''}:${walkId}`, !!session, request)
  // igual que WalkDetailPage: si venimos de publicar, ya tenemos el paseo recién creado y
  // evitamos el flash de "Cargando…"; el hook igual revalida contra el servidor
  const preloaded = (location.state as { walk?: WalkDetails } | null)?.walk
  const walk = fetchedWalk ?? (preloaded?.id === walkId ? preloaded : undefined)
  const [copied, setCopied] = useState(false)

  if (!walk) {
    return (
      <section className="mx-auto max-w-md space-y-4 pb-8">
        {loading && <p role="status" className="text-body-sm text-on-surface-variant">Cargando la confirmación…</p>}
        {error && <p role="alert" className="rounded-xl bg-error-container p-4 text-body-sm text-on-error-container">{error}</p>}
      </section>
    )
  }

  const shareUrl = shareUrlFor(walk)
  const shareText = shareTextFor(walk, shareUrl)
  const pawSlots = Math.min(walk.capacity, MAX_PAW_SLOTS)

  function openWhatsApp() {
    window.open(`https://api.whatsapp.com/send?text=${encodeURIComponent(shareText)}`, '_blank')
  }

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(shareUrl)
      setCopied(true)
      setTimeout(() => setCopied(false), 2500)
    } catch {
      // el navegador puede negar el acceso al portapapeles; no hay nada más que hacer acá
    }
  }

  async function handleShare() {
    if (navigator.share) {
      try {
        await navigator.share({ title: 'Paseo grupal', text: shareText, url: shareUrl })
        return
      } catch {
        // el usuario canceló el share nativo, o el navegador no lo soporta del todo
      }
    }
    openWhatsApp()
  }

  return (
    <section className="mx-auto max-w-md space-y-6 pb-8">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-primary">
          <span className="flex h-7 w-7 items-center justify-center rounded-full bg-primary-fixed">
            <Icon name="verified" className="text-[18px]" />
          </span>
          <span className="text-label-md font-semibold uppercase tracking-wide">Paseadores · Salidas</span>
        </div>
        <button type="button" aria-label="Cerrar confirmación" onClick={() => navigate(paths.myWalks)}
          className="flex h-10 w-10 items-center justify-center rounded-full bg-surface-container text-on-surface-variant shadow-sm transition-colors hover:bg-surface-container-high">
          <Icon name="close" className="text-[20px]" />
        </button>
      </div>

      <div className="flex flex-col items-center text-center">
        <div className="mb-5 flex h-24 w-24 items-center justify-center rounded-full bg-gradient-to-br from-primary-fixed via-primary/10 to-secondary-fixed/40 shadow-[0_8px_24px_rgba(47,93,80,0.14)]">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary-container text-on-primary shadow-inner">
            <Icon name="check_circle" filled className="text-[36px]" />
          </div>
        </div>
        <div className="mb-2 inline-flex items-center gap-1.5 rounded-full bg-primary-fixed/60 px-3 py-1 text-on-primary-fixed-variant">
          <Icon name="task_alt" className="text-[16px]" />
          <span className="text-label-sm font-semibold">Salida grupal confirmada</span>
        </div>
        <h1 className="mb-2 text-headline-md font-bold text-on-surface">¡Tu salida ya está activa!</h1>
        <p className="max-w-[320px] text-body-md text-on-surface-variant">
          Los dueños de <span className="font-semibold text-on-surface">{walk.zoneName}</span> ya pueden descubrir tu paseo grupal y reservar cupos al instante.
        </p>
      </div>

      <div className="flex flex-col gap-4 rounded-xl bg-surface-container-lowest p-5 shadow-sm">
        <div className="flex items-start justify-between gap-3">
          <div className="flex flex-col">
            <span className="text-label-sm uppercase tracking-wide text-on-surface-variant">Paseo grupal organizado</span>
            <h2 className="text-title-md font-semibold text-on-surface">Paseo en {walk.zoneName}</h2>
          </div>
          <span className="flex shrink-0 items-center gap-1 rounded-full bg-primary-fixed px-2.5 py-1 text-label-sm font-semibold text-primary">
            <span className="h-2 w-2 rounded-full bg-primary" />
            Publicado y visible
          </span>
        </div>

        <div className="flex flex-col gap-2.5 rounded-lg bg-surface-container-low p-3.5">
          <div className="flex items-center gap-3">
            <Icon name="calendar_today" className="shrink-0 text-[20px] text-primary-container" />
            <div className="flex flex-col">
              <span className="text-body-md font-medium text-on-surface">{formatWalkDate(walk.startTime)}</span>
              <span className="text-caption text-on-surface-variant">Duración: {walk.durationMinutes} min</span>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <Icon name="location_on" className="shrink-0 text-[20px] text-primary-container" />
            <div className="flex flex-col">
              <span className="break-words text-body-md font-medium text-on-surface">{walk.meetingPoint}</span>
              <span className="text-caption text-on-surface-variant">Zona: {walk.zoneName}</span>
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-label-md font-semibold text-on-surface">Capacidad de la manada</span>
            <span className="rounded-full bg-secondary-fixed/50 px-2.5 py-0.5 text-label-sm font-semibold text-on-secondary-fixed-variant">
              ¡{walk.availableSpots} cupos disponibles!
            </span>
          </div>
          <div className="flex items-center justify-between rounded-lg bg-surface-container-low/70 p-3">
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1.5 text-on-surface-variant">
                {Array.from({ length: pawSlots }, (_, index) => (
                  <Icon key={index} name="pets" className="text-[20px] opacity-40" />
                ))}
              </div>
              <span className="ml-1 text-body-sm font-medium text-on-surface-variant">{walk.bookedSpots} de {walk.capacity} reservados</span>
            </div>
            <span className="text-caption font-semibold text-primary">Cupo máx. {walk.capacity}</span>
          </div>
        </div>

        <div className="flex items-center justify-between rounded-lg bg-gradient-to-r from-surface-container-low to-surface-container p-3">
          <div>
            <p className="text-caption text-on-surface-variant">Tarifa por mascota</p>
            <p className="text-title-sm font-bold text-on-surface">{walk.pricePerPet.toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })}</p>
          </div>
          <div className="text-right">
            <p className="text-caption text-on-surface-variant">Ganancia potencial estimada</p>
            <p className="text-title-sm font-bold text-primary">{(walk.pricePerPet * walk.capacity).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })}</p>
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-3.5">
        <h3 className="px-0.5 text-title-sm font-semibold text-on-surface">Acelerá tus reservas en el barrio</h3>
        <div className="flex flex-col gap-3 rounded-xl bg-surface-container-lowest p-4 shadow-sm">
          <div className="flex items-start gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-fixed/80 text-primary">
              <Icon name="send" className="text-[22px]" />
            </div>
            <div className="flex flex-col">
              <p className="text-title-sm font-semibold text-on-surface">Enviá la salida a tus clientes</p>
              <p className="text-body-sm text-on-surface-variant">Compartí el link en tus grupos de WhatsApp vecinales para que reserven su lugar en un toque.</p>
            </div>
          </div>
          <div className="flex items-center gap-2 pt-1">
            <button type="button" onClick={openWhatsApp}
              className="flex h-[46px] flex-1 items-center justify-center gap-2 rounded-full bg-[#25D366]/15 text-label-md font-medium text-[#128C7E] transition-all hover:bg-[#25D366]/25 active:scale-[0.98]">
              <Icon name="chat" className="text-[20px]" />
              Compartir por WhatsApp
            </button>
            <button type="button" onClick={handleCopy} title="Copiar enlace"
              className="flex h-[46px] w-12 items-center justify-center rounded-full bg-surface-container text-on-surface transition-all hover:bg-surface-container-high active:scale-[0.96]">
              <Icon name="content_copy" className="text-[20px]" />
            </button>
          </div>
          {copied && <span className="text-center text-caption font-medium text-primary">¡Enlace copiado al portapapeles!</span>}
        </div>
      </div>

      <div className="flex flex-col gap-3">
        <Link to={paths.myWalks}
          className="flex h-[52px] w-full items-center justify-center gap-2 rounded-full bg-primary-container text-label-lg font-semibold text-on-primary shadow-sm transition-all active:scale-[0.98]">
          <Icon name="view_timeline" className="text-[20px]" />
          Ver en Mis paseos
        </Link>
        <button type="button" onClick={handleShare}
          className="flex h-[52px] w-full items-center justify-center gap-2 rounded-full bg-primary-fixed text-label-lg font-semibold text-primary transition-all hover:bg-primary-fixed-dim active:scale-[0.98]">
          <Icon name="share" className="text-[20px]" />
          Compartir enlace del paseo
        </button>
        <Link to={paths.publishWalk}
          className="mx-auto flex items-center justify-center gap-1.5 py-2.5 text-center text-label-md text-on-surface-variant transition-colors hover:text-on-surface">
          <Icon name="add_circle_outline" className="text-[18px]" />
          Crear otra salida
        </Link>
      </div>
    </section>
  )
}
