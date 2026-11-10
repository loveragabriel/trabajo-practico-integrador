import { createBrowserRouter } from 'react-router-dom'
import { GuestOnly, RequireAuth } from '@/features/auth'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { MyBookingsPage } from '@/features/bookings/pages/MyBookingsPage'
import { PetFormPage } from '@/features/pets/pages/PetFormPage'
import { PetsPage } from '@/features/pets/pages/PetsPage'
import { ProfilePage } from '@/features/profile/pages/ProfilePage'
import { WalkerProfilePage } from '@/features/profile/pages/WalkerProfilePage'
import { MyWalksPage } from '@/features/walks/pages/MyWalksPage'
import { PublishWalkPage } from '@/features/walks/pages/PublishWalkPage'
import { WalkDetailPage } from '@/features/walks/pages/WalkDetailPage'
import { WalkPublishedPage } from '@/features/walks/pages/WalkPublishedPage'
import { WalkSearchPage } from '@/features/walks/pages/WalkSearchPage'
import { AppLayout } from './AppLayout'
import { HomePage } from './pages/HomePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { paths } from './paths'

// Las guardas son rutas sin path: agrupan hijas que comparten la misma regla de acceso.
export const router = createBrowserRouter([
  // login/register controlan toda la pantalla (diseño propio, sin el nav de escritorio
  // de AppLayout): no tiene sentido mostrar la navegación de la app antes de tener sesión
  {
    element: <GuestOnly />,
    children: [
      { path: paths.login, element: <LoginPage /> },
      { path: paths.register, element: <RegisterPage /> },
    ],
  },
  {
    element: <AppLayout />,
    children: [
      // públicas
      { path: paths.home, element: <HomePage /> },

      // cualquier usuario autenticado: ver paseos y perfiles de paseador requiere cuenta
      // (GET /api/walks y GET /api/walkers/{id} ya no son públicos en el backend)
      {
        element: <RequireAuth />,
        children: [
          { path: paths.profile, element: <ProfilePage /> },
          { path: paths.walks, element: <WalkSearchPage /> },
          { path: paths.walkDetail, element: <WalkDetailPage /> },
          { path: paths.walkerProfile, element: <WalkerProfilePage /> },
        ],
      },

      // CLIENT
      {
        element: <RequireAuth roles={['CLIENT']} />,
        children: [
          { path: paths.pets, element: <PetsPage /> },
          { path: paths.newPet, element: <PetFormPage /> },
          { path: paths.editPet, element: <PetFormPage /> },
          { path: paths.bookings, element: <MyBookingsPage /> },
        ],
      },

      // WALKER
      {
        element: <RequireAuth roles={['WALKER']} />,
        children: [
          { path: paths.myWalks, element: <MyWalksPage /> },
          { path: paths.publishWalk, element: <PublishWalkPage /> },
          { path: paths.walkPublished, element: <WalkPublishedPage /> },
        ],
      },

      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
