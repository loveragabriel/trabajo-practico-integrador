import { useAuth } from '@/features/auth'
import { PageHeader } from '@/shared/components/PageHeader'

// Perfil propio: datos de contacto y zona de residencia (el rol no se edita, RN-02).
// TODO solo WALKER: zonas de servicio (walker_zones, al menos una para publicar, RN-08)
// y precio/cupo por defecto (RN-04).
export function ProfilePage() {
  const { session } = useAuth()
  const isWalker = session?.user.role === 'WALKER'

  return (
    <section>
      <PageHeader
        title="Mi perfil"
        description={isWalker ? 'Tus datos, zonas de servicio y valores por defecto.' : 'Tus datos de contacto y zona.'}
      />
    </section>
  )
}
