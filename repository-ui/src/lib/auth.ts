import {type User, UserManager, WebStorageStateStore} from 'oidc-client-ts'
import {ref} from 'vue'

let manager: UserManager | null = null

export const user = ref<User | null>(null)
export const authenticated = ref(false)
export let redirectPath: string | null = null

async function settings(): Promise<{ authority: string; client_id: string }> {
    try {
        const response = await fetch('/v1/auth')
        if (response.ok) {
            const config = await response.json()
            if (config.authority) {
                console.log('[auth] authority from /v1/auth:', config.authority)
                return {authority: config.authority, client_id: config.client || 'repository-ui'}
            }
            console.warn('[auth] /v1/auth returned empty authority:', config)
        } else {
            console.warn('[auth] /v1/auth returned status:', response.status)
        }
    } catch (error) {
        console.warn('[auth] /v1/auth fetch failed:', error)
    }
    const fallback = import.meta.env.VITE_OIDC_AUTHORITY ?? 'http://localhost:8180/realms/far'
    console.log('[auth] using fallback authority:', fallback)
    return {authority: fallback, client_id: import.meta.env.VITE_OIDC_CLIENT_ID ?? 'repository-ui'}
}

export async function initialize(): Promise<void> {
    const config = await settings()

    // Clear stale OIDC session entries if authority changed
    const stored = window.sessionStorage.getItem('oidc.authority')
    if (stored && stored !== config.authority) {
        console.log('[auth] authority changed, clearing session storage')
        window.sessionStorage.clear()
    }
    window.sessionStorage.setItem('oidc.authority', config.authority)

    manager = new UserManager({
        ...config,
        redirect_uri: `${window.location.origin}/callback`,
        post_logout_redirect_uri: window.location.origin,
        response_type: 'code',
        scope: 'openid profile',
        userStore: new WebStorageStateStore({store: window.sessionStorage}),
        automaticSilentRenew: true,
    })
    try {
        if (window.location.pathname === '/callback') {
            const signed = await manager.signinRedirectCallback()
            user.value = signed
            authenticated.value = true
            redirectPath = signed.state?.toString() ?? '/'
            return
        }
        const existing = await manager.getUser()
        if (existing && !existing.expired) {
            user.value = existing
            authenticated.value = true
        }
    } catch {
        user.value = null
        authenticated.value = false
    }
}

export async function login(target?: string): Promise<void> {
    await manager?.signinRedirect({state: target ?? window.location.pathname})
}

export async function logout(): Promise<void> {
    await manager?.signoutRedirect()
}

export function token(): string | undefined {
    return user.value?.access_token
}

export function name(): string | undefined {
    return user.value?.profile?.preferred_username ?? user.value?.profile?.name
}
