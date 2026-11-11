import { Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { Icon } from '@/shared/components/Icon'
import { useAuth } from '@/features/auth'
import { PageHeader } from '@/shared/components/PageHeader'
import { ClientOwnProfile } from '../components/ClientOwnProfile'
import { WalkerOwnProfile } from '../components/WalkerOwnProfile'
import { useProfile } from '../hooks/useProfile'

// despachador fino: solo carga el perfil y decide qué componente renderizar. El contenido real
// (muy distinto entre roles) vive en ClientOwnProfile / WalkerOwnProfile.
export function ProfilePage() {
  const { session, signIn } = useAuth()
  const { profile, error, loading, reload } = useProfile()

  function handleSaved(updated: { fullName: string }) {
    if (session) signIn({ ...session, user: { ...session.user, name: updated.fullName } })
  }

  return (
    <section>
      {profile?.role !== 'WALKER' && (
        <PageHeader
          title="Mi perfil"
          description={profile?.role === 'CLIENT' ? 'Tus datos de contacto y zona.' : undefined}
        />
      )}

      {session?.user.role === 'CLIENT' && (
        <Link to={paths.pets} className="mt-4 flex items-center gap-2 rounded-2xl bg-surface-container-lowest p-4 font-semibold text-primary shadow-sm">
          <Icon name="pets" className="text-xl" /> Mis mascotas <Icon name="chevron_right" className="ml-auto text-xl" />
        </Link>
      )}

      {loading && <p role="status" className="mt-6 text-slate-600">Cargando tu perfil…</p>}
      {error && (
        <div role="alert" className="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <p>{error}</p>
          <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar</button>
        </div>
      )}
      {profile && (
        profile.role === 'WALKER'
          ? <WalkerOwnProfile profile={profile} onSaved={handleSaved} />
          : <ClientOwnProfile profile={profile} onSaved={handleSaved} />
      )}
    </section>
  )
}
