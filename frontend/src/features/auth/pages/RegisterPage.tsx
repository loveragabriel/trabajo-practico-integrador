import { PageHeader } from '@/shared/components/PageHeader'

// TODO: formulario de registro contra POST /api/auth/register.
// - rol CLIENT o WALKER, elegido acá y fijo para siempre (RN-02)
// - zona de residencia desde el catálogo GET /api/zones, nunca texto libre (RN-06)
// - si es WALKER: precio y cupo por defecto, que solo precargan la publicación (RN-04)
export function RegisterPage() {
  return (
    <section className="mx-auto max-w-sm">
      <PageHeader title="Crear cuenta" description="Registrate como dueño de mascota o como paseador." />
    </section>
  )
}
