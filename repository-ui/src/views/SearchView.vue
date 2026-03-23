<script lang="ts" setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {type Resolution, search} from '@/lib/api'
import Card from '@/components/Card.vue'
import {ChevronLeft, ChevronRight, Database, Globe, Plus, Search, X} from 'lucide-vue-next'

interface Predicate {
  field: string
  custom: string
  operator: string
  value: string
}

const fields = [
  {value: 'identifier', label: 'Identifier'},
  {value: 'urn', label: 'URN'},
  {value: 'namespace', label: 'Certificate Type'},
]

const operators = [
  {value: 'contains', label: 'contains'},
  {value: 'eq', label: 'equals'},
]

const router = useRouter()
const results = ref<Resolution[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = ref(25)
const searched = ref(false)
const predicates = ref<Predicate[]>([
  {field: 'identifier', custom: '', operator: 'contains', value: ''}
])

function add() {
  predicates.value.push({field: 'identifier', custom: '', operator: 'contains', value: ''})
}

function resolved(predicate: Predicate): string {
  return predicate.field === '_custom' ? predicate.custom.trim() : predicate.field
}

function remove(index: number) {
  if (predicates.value.length > 1) {
    predicates.value.splice(index, 1)
  }
}

function escape(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/'/g, "\\'")
}

function filter(): string {
  const parts = predicates.value
      .filter(p => resolved(p) && p.value.trim())
      .map(p => {
        const field = resolved(p)
        const sanitized = escape(p.value.trim())
        if (p.operator === 'contains') {
          return `contains(${field},'${sanitized}')`
        }
        return `${field} eq '${sanitized}'`
      })
  if (parts.length === 0) return ''
  if (parts.length === 1) return parts[0]
  return parts.join(' and ')
}

function valid(): boolean {
  return predicates.value.some(p => resolved(p) && p.value.trim())
}

async function load() {
  const expression = filter()
  if (!expression) return
  loading.value = true
  error.value = ''
  try {
    const result = await search(expression, size.value, page.value * size.value)
    const items = result.value ?? []
    items.sort((a, b) => {
      const r = (a.resolver ?? '').localeCompare(b.resolver ?? '')
      return r !== 0 ? r : (a.identifier ?? '').localeCompare(b.identifier ?? '')
    })
    results.value = items
    total.value = result.count ?? items.length
    searched.value = true
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Search failed'
  } finally {
    loading.value = false
  }
}

function execute() {
  page.value = 0
  load()
}

function navigate(resolution: Resolution) {
  if (resolution.resolver === 'repository') {
    router.push(`/certificates/${resolution.urn}`)
  } else {
    router.push({path: `/resources/${resolution.urn}`, query: {resolver: resolution.resolver}})
  }
}

function previous() {
  if (page.value > 0) {
    page.value--
    load()
  }
}

function next() {
  if ((page.value + 1) * size.value < total.value) {
    page.value++
    load()
  }
}

function label(resolution: Resolution): string {
  if (resolution.resolver === 'repository') return 'Local'
  try {
    const url = new URL(resolution.resolver)
    return url.hostname
  } catch {
    return resolution.resolver
  }
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h2 class="text-3xl font-bold tracking-tight">Search</h2>
      <p class="text-muted-foreground mt-1">Search across the FAR network</p>
    </div>

    <div v-if="error" class="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
      {{ error }}
    </div>

    <Card>
      <form @submit.prevent="execute">
        <div class="space-y-3 mb-4">
          <div
              v-for="(predicate, index) in predicates"
              :key="index"
              class="flex items-center gap-2"
          >
            <select
                v-model="predicate.field"
                class="w-36 rounded-md border bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
            >
              <option v-for="f in fields" :key="f.value" :value="f.value">{{ f.label }}</option>
              <option value="_custom">Other...</option>
            </select>
            <input
                v-if="predicate.field === '_custom'"
                v-model="predicate.custom"
                class="w-36 rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                placeholder="Attribute name..."
                type="text"
            />
            <select
                v-model="predicate.operator"
                class="rounded-md border bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
            >
              <option v-for="op in operators" :key="op.value" :value="op.value">{{ op.label }}</option>
            </select>
            <input
                v-model="predicate.value"
                class="flex-1 rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                placeholder="Value..."
                type="text"
            />
            <button
                :disabled="predicates.length <= 1"
                class="inline-flex items-center justify-center rounded-md border p-2 text-muted-foreground hover:text-foreground hover:bg-accent disabled:opacity-30 disabled:pointer-events-none transition-colors"
                type="button"
                @click="remove(index)"
            >
              <X class="h-4 w-4"/>
            </button>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button
              class="inline-flex items-center rounded-md border px-3 py-2 text-sm font-medium hover:bg-accent transition-colors"
              type="button"
              @click="add"
          >
            <Plus class="h-4 w-4 mr-1"/>
            Add predicate
          </button>
          <div class="flex-1"/>
          <span v-if="valid()" :title="filter()" class="text-xs text-muted-foreground font-mono truncate max-w-md">
            {{ filter() }}
          </span>
          <button
              :disabled="!valid()"
              class="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:pointer-events-none transition-colors"
              type="submit"
          >
            <Search class="h-4 w-4 mr-1.5"/>
            Search
          </button>
        </div>
      </form>
    </Card>

    <Card v-if="loading || searched">
      <div v-if="loading" class="flex items-center justify-center py-12">
        <div class="text-muted-foreground">Searching...</div>
      </div>

      <template v-else>
        <div v-if="results.length === 0" class="text-sm text-muted-foreground py-8 text-center">
          No results found.
        </div>

        <table v-else class="w-full text-sm">
          <thead>
          <tr class="border-b">
            <th class="text-left py-3 font-medium text-muted-foreground">Source</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Identifier</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Namespace</th>
            <th class="text-left py-3 font-medium text-muted-foreground">URN</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Modified</th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="resolution in results"
              :key="resolution.urn"
              class="border-b last:border-0 transition-colors cursor-pointer hover:bg-muted/50"
              @click="navigate(resolution)"
          >
            <td class="py-3">
                <span v-if="resolution.resolver === 'repository'"
                      class="inline-flex items-center gap-1.5 text-xs font-medium text-green-600">
                  <Database class="h-3 w-3"/>
                  Local
                </span>
              <span v-else :title="resolution.resolver"
                    class="inline-flex items-center gap-1.5 text-xs font-medium text-blue-600">
                  <Globe class="h-3 w-3"/>
                  {{ label(resolution) }}
                </span>
            </td>
            <td class="py-3 font-medium">{{ resolution.identifier }}</td>
            <td class="py-3">
                <span class="inline-flex items-center rounded-full bg-secondary px-2.5 py-0.5 text-xs font-medium">
                  {{ resolution.namespace }}
                </span>
            </td>
            <td class="py-3 font-mono text-xs text-muted-foreground">{{ resolution.urn }}</td>
            <td class="py-3 text-muted-foreground">
              {{ resolution.timestamp ? new Date(resolution.timestamp).toLocaleDateString() : '--' }}
            </td>
          </tr>
          </tbody>
        </table>

        <div v-if="total > 0" class="flex items-center justify-between pt-4 border-t mt-4">
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

    <Card v-else>
      <div class="text-sm text-muted-foreground py-8 text-center">
        Add search predicates and click Search to query across local and federated registries.
      </div>
    </Card>
  </div>
</template>
