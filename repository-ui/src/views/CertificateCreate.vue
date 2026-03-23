<script lang="ts" setup>
import {ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {create, schema as fetchSchema, type Schema, schemas as fetchSchemas} from '@/lib/api'
import Card from '@/components/Card.vue'
import AttributeEditor from '@/components/AttributeEditor.vue'
import SchemaForm from '@/components/SchemaForm.vue'

const router = useRouter()
const namespace = ref('')
const identifier = ref('')
const owner = ref('')
const attributes = ref<Record<string, { value: string; source?: string }>>({})
const submitting = ref(false)
const error = ref('')

const available = ref<Schema[]>([])
const selected = ref('')
const active = ref<Schema | null>(null)
const loading = ref(false)

async function loadSchemas() {
  loading.value = true
  try {
    const all = await fetchSchemas()
    available.value = all.filter(s => s.active)
  } catch {
    available.value = []
  } finally {
    loading.value = false
  }
}

loadSchemas()

watch(selected, async () => {
  if (!selected.value) {
    active.value = null
    return
  }
  try {
    active.value = await fetchSchema(selected.value)
    attributes.value = {}
    if (active.value) {
      namespace.value = active.value.namespace
      identifier.value = active.value.name + '-'
    }
  } catch {
    active.value = null
  }
})

async function submit() {
  error.value = ''
  if (!namespace.value.trim()) {
    error.value = 'Namespace is required'
    return
  }
  if (!identifier.value.trim()) {
    error.value = 'Identifier is required'
    return
  }

  submitting.value = true
  try {
    const named: Record<string, { name: string; value: string; source?: string }> = {}
    for (const [key, attr] of Object.entries(attributes.value)) {
      named[key] = {name: key, ...attr}
    }
    const certificate = await create({
      namespace: namespace.value.trim(),
      identifier: identifier.value.trim(),
      attributes: named,
      owner: owner.value.trim() || undefined,
      schema: selected.value || undefined,
      pin: active.value?.version,
    })
    router.push(`/certificates/${certificate.urn}`)
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to create certificate'
  } finally {
    submitting.value = false
  }
}

function cancel() {
  router.push('/certificates')
}
</script>

<template>
  <div class="space-y-6 max-w-3xl">
    <div>
      <h2 class="text-3xl font-bold tracking-tight">Create Certificate</h2>
      <p class="text-muted-foreground mt-1">Define a new certificate with attributes</p>
    </div>

    <div v-if="error" class="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
      {{ error }}
    </div>

    <form class="space-y-6" @submit.prevent="submit">
      <Card title="Schema">
        <div class="space-y-2">
          <label class="text-sm font-medium">Schema (optional)</label>
          <select
              v-model="selected"
              class="w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          >
            <option value="">None (free-form attributes)</option>
            <option v-for="s in available" :key="s.id" :value="s.id">
              {{ s.name }} (v{{ s.version }}) — {{ s.namespace }}
            </option>
          </select>
          <p v-if="loading" class="text-xs text-muted-foreground">Loading schemas...</p>
        </div>
      </Card>

      <Card title="Details">
        <div class="space-y-4">
          <div>
            <label class="text-sm font-medium" for="namespace">Namespace</label>
            <input
                id="namespace"
                v-model="namespace"
                class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                placeholder="e.g. carbon"
                type="text"
            />
          </div>
          <div>
            <label class="text-sm font-medium" for="identifier">Identifier</label>
            <input
                id="identifier"
                v-model="identifier"
                class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                placeholder="e.g. VCS-2847-2024"
                type="text"
            />
          </div>
          <div>
            <label class="text-sm font-medium" for="owner">Owner</label>
            <input
                id="owner"
                v-model="owner"
                class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                placeholder="e.g. admin@example.org (optional)"
                type="text"
            />
          </div>
        </div>
      </Card>

      <Card title="Attributes">
        <SchemaForm v-if="active" v-model="attributes" :schema="active"/>
        <AttributeEditor v-else v-model="attributes"/>
      </Card>

      <div class="flex items-center gap-3">
        <button
            :disabled="submitting"
            class="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:pointer-events-none transition-colors"
            type="submit"
        >
          {{ submitting ? 'Creating...' : 'Create Certificate' }}
        </button>
        <button
            class="inline-flex items-center rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
            type="button"
            @click="cancel"
        >
          Cancel
        </button>
      </div>
    </form>
  </div>
</template>
