// roles fijos del sistema (RN-02): se definen al registrarse y no cambian
export type Role = 'CLIENT' | 'WALKER'

// ajustamos el string para que el cliente ingrese todo en español
export const ROLE_LABELS: Record<Role, string> = {
  CLIENT: 'Dueño de mascota',
  WALKER: 'Paseador',
}
