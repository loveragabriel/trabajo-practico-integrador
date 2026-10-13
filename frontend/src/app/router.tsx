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
import { WalkSearchPage } from '@/features/walks/pages/WalkSearchPage'
import { AppLayout } from './AppLayout'
import { HomePage } from './pages/HomePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { paths } from './paths'

// Las guardas son rutas sin path: agrupan hijas que comparten la misma regla de acceso.
export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      // públicas
      { path: paths.home, element: <HomePage /> },
      { path: paths.walks, element: <WalkSearchPage /> },
      { path: paths.walkDetail, element: <WalkDetailPage /> },
      { path: paths.walkerProfile, element: <WalkerProfilePage /> },

      // solo sin sesión
      {
        element: <GuestOnly />,
        children: [
          { path: paths.login, element: <LoginPage /> },
          { path: paths.register, element: <RegisterPage /> },
        ],
      },

      // cualquier usuario autenticado
      {
        element: <RequireAuth />,
        children: [{ path: paths.profile, element: <ProfilePage /> }],
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
        ],
      },

      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
