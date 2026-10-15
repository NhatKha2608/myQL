import { unref, type MaybeRef } from 'vue'

/** Giá trị thật từ prop có thể là Ref (vd. truyền `ctx.classrooms` từ composable). */
export function resolveMaybeRef<T>(source: MaybeRef<T> | undefined): T | undefined {
  return source === undefined ? undefined : unref(source)
}
