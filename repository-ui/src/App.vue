<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {RouterLink, RouterView} from 'vue-router'
import {
  Award,
  FileText,
  LayoutDashboard,
  LogIn,
  LogOut,
  Monitor,
  Moon,
  Search,
  Settings,
  Sun,
  User
} from 'lucide-vue-next'
import {authenticated, login, logout, name} from '@/lib/auth'
import {theme} from '@/lib/theme'
import {configuration} from '@/lib/api'

const hostname = ref('')

onMounted(async () => {
  try {
    const config = await configuration()
    if (config.identity) {
      hostname.value = new URL(config.identity).hostname
    }
  } catch { /* ignore */
  }
})

function cycle() {
  const modes = ['light', 'dark', 'system'] as const
  const index = modes.indexOf(theme.value)
  theme.value = modes[(index + 1) % modes.length]
}
</script>

<template>
  <div class="flex h-screen">
    <aside class="w-64 border-r bg-card flex flex-col">
      <div class="p-6 border-b flex items-center gap-3">
        <svg aria-hidden="true" class="h-8 w-8 shrink-0" viewBox="0 0 32 32">
          <!-- Mesh network logo -->
          <circle class="fill-primary" cx="16" cy="4" r="2.5"/>
          <circle class="fill-primary" cx="4" cy="14" r="2.5"/>
          <circle class="fill-primary" cx="28" cy="14" r="2.5"/>
          <circle class="fill-primary" cx="8" cy="26" r="2.5"/>
          <circle class="fill-primary" cx="24" cy="26" r="2.5"/>
          <circle class="fill-primary opacity-60" cx="16" cy="16" r="3"/>
          <!-- Mesh connections -->
          <line class="stroke-primary opacity-40" stroke-width="1" x1="16" x2="4" y1="4" y2="14"/>
          <line class="stroke-primary opacity-40" stroke-width="1" x1="16" x2="28" y1="4" y2="14"/>
          <line class="stroke-primary opacity-40" stroke-width="1" x1="16" x2="16" y1="4" y2="16"/>
          <line class="stroke-primary opacity-40" stroke-width="1" x1="4" x2="28" y1="14" y2="14"/>
          <line class="stroke-primary opacity-40" stroke-width="1" x1="4" x2="16" y1="14" y2="16"/>
          <line class="stroke-primary opacity-40" stroke-width="1" x1="4" x2="8" y1="14" y2="26"/>
          <line class="stroke-primary opacity-40" stroke-width="1" x1="28" x2="16" y1="14" y2="16"/>
          <line class="stroke-primary opacity-40" stroke-width="1" x1="28" x2="24" y1="14" y2="26"/>
          <line class="stroke-primary opacity-40" stroke-width="1" x1="8" x2="24" y1="26" y2="26"/>
          <line class="stroke-primary opacity-40" stroke-width="1" x1="8" x2="16" y1="26" y2="16"/>
          <line class="stroke-primary opacity-40" stroke-width="1" x1="24" x2="16" y1="26" y2="16"/>
        </svg>
        <div>
          <h1 class="text-lg font-bold tracking-tight">FAR Repository</h1>
          <p class="text-xs text-muted-foreground">Federated Attribute Repository</p>
        </div>
      </div>
      <nav class="flex-1 p-4 space-y-1">
        <RouterLink
            active-class="bg-accent text-accent-foreground"
            class="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground transition-colors"
            to="/"
        >
          <LayoutDashboard class="h-4 w-4"/>
          Dashboard
        </RouterLink>
        <RouterLink
            active-class="bg-accent text-accent-foreground"
            class="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground transition-colors"
            to="/search"
        >
          <Search class="h-4 w-4"/>
          Search
        </RouterLink>
        <RouterLink
            active-class="bg-accent text-accent-foreground"
            class="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground transition-colors"
            to="/certificates"
        >
          <Award class="h-4 w-4"/>
          Certificates
        </RouterLink>
        <RouterLink
            active-class="bg-accent text-accent-foreground"
            class="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground transition-colors"
            to="/schemas"
        >
          <FileText class="h-4 w-4"/>
          Schemas
        </RouterLink>
        <RouterLink
            active-class="bg-accent text-accent-foreground"
            class="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground transition-colors"
            to="/settings"
        >
          <Settings class="h-4 w-4"/>
          Settings
        </RouterLink>
      </nav>
      <div class="p-4 border-t space-y-3">
        <div v-if="authenticated" class="space-y-2">
          <div class="flex items-center gap-2 text-sm text-muted-foreground">
            <User class="h-4 w-4"/>
            <span class="truncate">{{ name() }}</span>
          </div>
          <button
              class="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium w-full hover:bg-accent hover:text-accent-foreground transition-colors"
              @click="logout()"
          >
            <LogOut class="h-4 w-4"/>
            Sign out
          </button>
        </div>
        <div v-else>
          <button
              class="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium w-full hover:bg-accent hover:text-accent-foreground transition-colors"
              @click="login()"
          >
            <LogIn class="h-4 w-4"/>
            Sign in
          </button>
        </div>
        <div class="text-xs text-muted-foreground flex items-center justify-between">
          <span>v0.1.0</span>
          <span v-if="hostname">{{ hostname }}</span>
        </div>
      </div>
    </aside>
    <div class="flex-1 flex flex-col overflow-hidden">
      <header class="flex items-center justify-end px-6 py-3 border-b bg-card">
        <button
            :title="`Theme: ${theme}`"
            class="inline-flex items-center justify-center rounded-md border p-2 text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
            @click="cycle"
        >
          <Sun v-if="theme === 'light'" class="h-4 w-4"/>
          <Moon v-else-if="theme === 'dark'" class="h-4 w-4"/>
          <Monitor v-else class="h-4 w-4"/>
        </button>
      </header>
      <main class="flex-1 overflow-auto">
        <div class="p-8">
          <RouterView/>
        </div>
      </main>
    </div>
  </div>
</template>
