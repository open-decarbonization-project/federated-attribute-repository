import {ref, watch} from 'vue'

export type Theme = 'light' | 'dark' | 'system'

const KEY = 'far-theme'

function stored(): Theme {
    const value = localStorage.getItem(KEY)
    if (value === 'light' || value === 'dark' || value === 'system') return value
    return 'system'
}

function preferred(): boolean {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function apply(theme: Theme) {
    const dark = theme === 'dark' || (theme === 'system' && preferred())
    document.documentElement.classList.toggle('dark', dark)
}

export const theme = ref<Theme>(stored())

export function initialize() {
    apply(theme.value)

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if (theme.value === 'system') {
            apply('system')
        }
    })

    watch(theme, (value) => {
        localStorage.setItem(KEY, value)
        apply(value)
    })
}
