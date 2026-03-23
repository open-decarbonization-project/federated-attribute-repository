<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {type Certificate, health, type Health, list} from '@/lib/api'
import Card from '@/components/Card.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import {Activity, Award, CheckCircle, XCircle} from 'lucide-vue-next'

const router = useRouter()
const certificates = ref<Certificate[]>([])
const total = ref(0)
const status = ref<Health | null>(null)
const loading = ref(true)
const error = ref('')

const active = computed(() =>
    certificates.value.filter(c => c.status === 'ACTIVE').length
)

const retired = computed(() =>
    certificates.value.filter(c => c.status === 'RETIRED').length
)

const recent = computed(() =>
    [...certificates.value]
        .sort((a, b) => new Date(b.modified).getTime() - new Date(a.modified).getTime())
        .slice(0, 5)
)

onMounted(async () => {
  try {
    const [page, info] = await Promise.all([list(100, 0), health()])
    certificates.value = page.value ?? []
    total.value = page.count ?? certificates.value.length
    status.value = info
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to load dashboard'
  } finally {
    loading.value = false
  }
})

function navigate(urn: string) {
  router.push(`/certificates/${urn}`)
}
</script>

<template>
  <div class="space-y-8">
    <div>
      <h2 class="text-3xl font-bold tracking-tight">Dashboard</h2>
      <p class="text-muted-foreground mt-1">Overview of the Federated Attribute Repository</p>
    </div>

    <div v-if="error" class="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
      {{ error }}
    </div>

    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="text-muted-foreground">Loading...</div>
    </div>

    <template v-else>
      <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card title="Total Certificates">
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="text-sm font-medium text-muted-foreground">Total Certificates</h3>
              <Award class="h-4 w-4 text-muted-foreground"/>
            </div>
          </template>
          <div class="text-2xl font-bold">{{ total }}</div>
        </Card>

        <Card>
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="text-sm font-medium text-muted-foreground">Active</h3>
              <CheckCircle class="h-4 w-4 text-green-600"/>
            </div>
          </template>
          <div class="text-2xl font-bold text-green-600">{{ active }}</div>
        </Card>

        <Card>
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="text-sm font-medium text-muted-foreground">Retired</h3>
              <XCircle class="h-4 w-4 text-red-600"/>
            </div>
          </template>
          <div class="text-2xl font-bold text-red-600">{{ retired }}</div>
        </Card>

        <Card>
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="text-sm font-medium text-muted-foreground">Health</h3>
              <Activity class="h-4 w-4 text-muted-foreground"/>
            </div>
          </template>
          <div :class="status?.status === 'UP' ? 'text-green-600' : 'text-red-600'" class="text-2xl font-bold">
            {{ status?.status ?? 'UNKNOWN' }}
          </div>
        </Card>
      </div>

      <Card title="Recent Certificates">
        <div v-if="recent.length === 0" class="text-sm text-muted-foreground py-4 text-center">
          No certificates found.
        </div>
        <table v-else class="w-full text-sm">
          <thead>
          <tr class="border-b">
            <th class="text-left py-3 font-medium text-muted-foreground">URN</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Namespace</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Status</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Modified</th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="certificate in recent"
              :key="certificate.urn"
              class="border-b last:border-0 cursor-pointer hover:bg-muted/50 transition-colors"
              @click="navigate(certificate.urn)"
          >
            <td class="py-3 font-mono text-xs">{{ certificate.urn }}</td>
            <td class="py-3">{{ certificate.namespace }}</td>
            <td class="py-3">
              <StatusBadge :status="certificate.status"/>
            </td>
            <td class="py-3 text-muted-foreground">
              {{ new Date(certificate.modified).toLocaleDateString() }}
            </td>
          </tr>
          </tbody>
        </table>
      </Card>
    </template>
  </div>
</template>
