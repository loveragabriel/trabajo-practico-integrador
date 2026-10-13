// Única fuente de verdad de las URLs de la app.
// Para rutas con parámetros usar generatePath(paths.walkDetail, { walkId }).
export const paths = {
  home: '/',
  login: '/login',
  register: '/register',

  // búsqueda de paseos por zona y detalle (públicos)
  walks: '/walks',
  walkDetail: '/walks/:walkId',
  walkerProfile: '/walkers/:walkerId',

  // cualquier usuario autenticado
  profile: '/profile',

  // CLIENT
  pets: '/pets',
  newPet: '/pets/new',
  editPet: '/pets/:petId/edit',
  bookings: '/bookings',

  // WALKER
  myWalks: '/my-walks',
  publishWalk: '/my-walks/new',
} as const
