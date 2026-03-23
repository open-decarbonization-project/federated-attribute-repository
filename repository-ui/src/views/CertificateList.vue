<script lang="ts" setup>
import {onMounted, ref, watch} from 'vue'
import {RouterLink, useRouter} from 'vue-router'
import {type Certificate, list} from '@/lib/api'
import Card from '@/components/Card.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import {ChevronLeft, ChevronRight, Search} from 'lucide-vue-next'

const router = useRouter()
const certificates = ref<Certificate[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const query = ref('')
const page = ref(0)
const size = ref(25)

const filtered = ref<Certificate[]>([])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await list(size.value, page.value * size.value)
    certificates.value = result.value ?? []
    total.value = result.count ?? certificates.value.length
    filter()
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to load certificates'
  } finally {
    loading.value = false
  }
}

function filter() {
  const term = query.value.toLowerCase().trim()
  if (!term) {
    filtered.value = certificates.value
    return
  }
  filtered.value = certificates.value.filter(c =>
      c.urn.toLowerCase().includes(term) ||
      c.namespace.toLowerCase().includes(term) ||
      c.identifier.toLowerCase().includes(term) ||
      (c.owner?.toLowerCase().includes(term) ?? false) ||
      c.status.toLowerCase().includes(term)
  )
}

function navigate(urn: string) {
  router.push(`/certificates/${urn}`)
}

function previous() {
  if (page.value > 0) {
    page.value--
  }
}

function next() {
  if ((page.value + 1) * size.value < total.value) {
    page.value++
  }
}

watch(query, filter)
watch(page, load)
onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-3xl font-bold tracking-tight">Certificates</h2>
        <p class="text-muted-foreground mt-1">Manage certificate attributes</p>
      </div>
      <RouterLink
          class="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          to="/certificates/create"
      >
        Create Certificate
      </RouterLink>
    </div>

    <div v-if="error" class="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
      {{ error }}
    </div>

    <Card>
      <div class="mb-4">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"/>
          <input
              v-model="query"
              class="w-full rounded-md border bg-background py-2 pl-10 pr-4 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
              placeholder="Search by URN, namespace, owner, or status..."
              type="text"
          />
        </div>
      </div>

      <div v-if="loading" class="flex items-center justify-center py-12">
        <div class="text-muted-foreground">Loading...</div>
      </div>

      <template v-else>
        <div v-if="filtered.length === 0" class="text-sm text-muted-foreground py-8 text-center">
          No certificates found.
        </div>

        <table v-else class="w-full text-sm">
          <thead>
          <tr class="border-b">
            <th class="text-left py-3 font-medium text-muted-foreground">URN</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Namespace</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Status</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Owner</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Modified</th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="certificate in filtered"
              :key="certificate.urn"
              class="border-b last:border-0 cursor-pointer hover:bg-muted/50 transition-colors"
              @click="navigate(certificate.urn)"
          >
            <td class="py-3 font-mono text-xs">{{ certificate.urn }}</td>
            <td class="py-3">{{ certificate.namespace }}</td>
            <td class="py-3">
              <StatusBadge :status="certificate.status"/>
            </td>
            <td class="py-3 text-muted-foreground">{{ certificate.owner ?? '--' }}</td>
            <td class="py-3 text-muted-foreground">
              {{ new Date(certificate.modified).toLocaleDateString() }}
            </td>
          </tr>
          </tbody>
        </table>

        <div class="flex items-center justify-between pt-4 border-t mt-4">
          <div class="text-sm text-muted-foreground">
            Showing {{ page * size + 1 }}-{{ Math.min((page + 1) * size, total) }} of {{ total }}
          </div>
          <div class="flex items-center gap-2">
            <button
                :disabled="page === 0"
                class="inline-flex items-center rounded-md border px-3 py-1.5 text-sm font-medium hover:bg-accent disabled:opacity-50 disabled:pointer-events-none transition-colors"
                @click="previous"
            >
              <ChevronLeft class="h-4 w-4 mr-1"/>
              Previous
            </button>
            <button
                :disabled="(page + 1) * size >= total"
                class="inline-flex items-center rounded-md border px-3 py-1.5 text-sm font-medium hover:bg-accent disabled:opacity-50 disabled:pointer-events-none transition-colors"
                @click="next"
            >
              Next
              <ChevronRight class="h-4 w-4 ml-1"/>
            </button>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>
