export default defineNuxtRouteMiddleware(async (to) => {
  const publicPaths = ['/login']
  if (publicPaths.includes(to.path)) {
    const token = useCookie('qlhs_access_token')
    if (token.value) {
      const selected = useCookie<number | null>('qlhs_selected_classroom')
      const classId = Number(selected.value)
      return navigateTo(Number.isFinite(classId) && classId > 0 ? '/dashboard' : '/classrooms')
    }
    return
  }

  const auth = useAuth()
  if (!auth.accessToken) {
    return navigateTo({
      path: '/login',
      query: { redirect: to.fullPath },
    })
  }

  if (!auth.user && !auth.initialized) {
    await auth.fetchMe()
    if (!auth.user) {
      return navigateTo('/login')
    }
  }
})
