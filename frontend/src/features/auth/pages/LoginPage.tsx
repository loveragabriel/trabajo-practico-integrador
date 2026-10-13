import { Link, useLocation, useNavigate, type Location } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { PageHeader } from '@/shared/components/PageHeader'
import type { Role } from '@/shared/types/user'

// TODO: formulario email + contraseña contra POST /api/auth/login.
// Al responder, signIn({ token, user }) y volver a la ruta que pidió el login.
export function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: Location } | null)?.from?.pathname ?? paths.home

  // solo en desarrollo, hasta que exista el endpoint de login: permite recorrer las rutas protegidas
  function devSignIn(role: Role) {
    signIn({ token: 'dev', user: { id: 'dev', name: role === 'CLIENT' ? 'Cliente demo' : 'Paseador demo', role } })
    navigate(from, { replace: true })
  }

  return (
    <section className="mx-auto max-w-sm">
      <PageHeader title="Ingresar" />
      <p className="text-slate-600">
        ¿No tenés cuenta?{' '}
        <Link to={paths.register} className="text-emerald-700 underline">
          Registrate
        </Link>
      </p>

      {import.meta.env.DEV && (
        <div className="mt-8 space-y-2 rounded-md border border-dashed border-amber-400 p-4">
          <p className="text-sm text-amber-700">Acceso de desarrollo (no se incluye en el build)</p>
          <button type="button" onClick={() => devSignIn('CLIENT')} className="block underline">
            Entrar como cliente
          </button>
          <button type="button" onClick={() => devSignIn('WALKER')} className="block underline">
            Entrar como paseador
          </button>
        </div>
      )}
    </section>
  )
}
