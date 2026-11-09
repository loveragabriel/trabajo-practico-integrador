import { registerHooks } from 'node:module'
import { existsSync, statSync } from 'node:fs'
import { fileURLToPath, pathToFileURL } from 'node:url'
import path from 'node:path'

// Hace que node --test entienda el alias '@/' y los imports sin extensión, como Vite.
const srcDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../src')
const EXTENSIONS = ['.ts', '.tsx', '/index.ts', '/index.tsx']

function resolveOnDisk(absPath) {
  if (existsSync(absPath) && statSync(absPath).isFile()) return absPath
  for (const ext of EXTENSIONS) {
    if (existsSync(absPath + ext)) return absPath + ext
  }
  return null
}

registerHooks({
  // solo entra en juego cuando la resolución normal de Node falla (p. ej. node_modules
  // sigue resolviéndose como siempre); evita tocar nada fuera de nuestro propio src/
  resolve(specifier, context, nextResolve) {
    try {
      return nextResolve(specifier, context)
    } catch (error) {
      let absPath = null
      if (specifier.startsWith('@/')) {
        absPath = path.join(srcDir, specifier.slice(2))
      } else if ((specifier.startsWith('./') || specifier.startsWith('../')) && context.parentURL?.startsWith('file://')) {
        absPath = path.resolve(path.dirname(fileURLToPath(context.parentURL)), specifier)
      }

      const resolved = absPath && resolveOnDisk(absPath)
      if (resolved) return nextResolve(pathToFileURL(resolved).href, context)
      throw error
    }
  },
})
