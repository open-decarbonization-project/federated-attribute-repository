<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import {type Field, type Schema, schema as fetchSchema, updateSchema, versions as fetchVersions} from '@/lib/api'
import Card from '@/components/Card.vue'
import FieldEditor from '@/components/FieldEditor.vue'

const route = useRoute()
const item = ref<Schema | null>(null)
const loading = ref(false)
const error = ref('')
const editing = ref(false)
const description = ref('')
const active = ref(true)
const fields = ref<Field[]>([])
const saving = ref(false)
const history = ref<Schema[]>([])
const viewing = ref<Schema | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const id = route.params.id as string
    const [loaded, vers] = await Promise.all([fetchSchema(id), fetchVersions(id)])
    item.value = loaded
    description.value = loaded.description
    active.value = loaded.active
    fields.value = [...loaded.fields]
    history.value = vers
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to load schema'
  } finally {
    loading.value = false
  }
}

function edit() {
  if (!item.value) return
  fields.value = [...item.value.fields]
  description.value = item.value.description
  active.value = item.value.active
  editing.value = true
}

async function save() {
  if (!item.value) return
  saving.value = true
  error.value = ''
  try {
    item.value = await updateSchema(item.value.id, {
      description: description.value,
      fields: fields.value,
      active: active.value,
    })
    editing.value = false
    history.value = await fetchVersions(item.value.id)
    fields.value = [...item.value.fields]
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to update schema'
  } finally {
    saving.value = false
  }
}

const typeLabels: Record<string, string> = {
  string: 'String',
  numeric: 'Numeric',
  boolean: 'Boolean',
  datetime: 'DateTime',
  quantity: 'Quantity',
  list: 'List',
  record: 'Record',
}

onMounted(load)
</script>

<template>
  <div class="space-y-6 max-w-3xl">
    <div v-if="loading" class="text-sm text-muted-foreground">Loading...</div>

    <div v-if="error" class="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
      {{ error }}
    </div>

    <template v-if="item">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-3xl font-bold tracking-tight">{{ item.name }}</h2>
          <p class="text-muted-foreground mt-1">{{ item.namespace }} &middot; v{{ item.version }}</p>
        </div>
        <div class="flex items-center gap-2">
          <span
              :class="item.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'"
              class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
          >
            {{ item.active ? 'Active' : 'Inactive' }}
          </span>
          <button
              v-if="!editing"
              class="inline-flex items-center rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
              @click="edit"
          >
            Edit
          </button>
        </div>
      </div>

      <Card title="Details">
        <div class="space-y-3 text-sm">
          <div v-if="!editing">
            <span class="text-muted-foreground">Description:</span>
            <p>{{ item.description || 'No description' }}</p>
          </div>
          <div v-else class="space-y-3">
            <div>
              <label class="text-sm font-medium">Description</label>
              <textarea
                  v-model="description"
                  class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                  rows="3"
              />
            </div>
            <label class="flex items-center gap-2 text-sm">
              <input v-model="active" class="rounded border" type="checkbox"/>
              Active
            </label>
            <div class="flex gap-2">
              <button
                  :disabled="saving"
                  class="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
                  @click="save"
              >
                {{ saving ? 'Saving...' : 'Save' }}
              </button>
              <button
                  class="inline-flex items-center rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
                  @click="editing = false"
              >
                Cancel
              </button>
            </div>
          </div>
          <div>
            <span class="text-muted-foreground">Owner:</span> {{ item.owner || 'Unknown' }}
          </div>
          <div>
            <span class="text-muted-foreground">Created:</span> {{ new Date(item.created).toLocaleString() }}
          </div>
          <div>
            <span class="text-muted-foreground">Modified:</span> {{ new Date(item.modified).toLocaleString() }}
          </div>
        </div>
      </Card>

      <Card title="Fields">
        <template v-if="editing">
          <FieldEditor v-model="fields"/>
        </template>
        <template v-else>
          <div v-if="item.fields.length === 0" class="text-sm text-muted-foreground text-center py-4">
            No fields defined.
          </div>
          <div v-else class="border rounded-md">
            <table class="w-full text-sm">
              <thead>
              <tr class="border-b bg-muted/50">
                <th class="text-left p-3 font-medium">Name</th>
                <th class="text-left p-3 font-medium">Label</th>
                <th class="text-left p-3 font-medium">Type</th>
                <th class="text-left p-3 font-medium">Required</th>
                <th class="text-left p-3 font-medium">Unit</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="field in item.fields" :key="field.name" class="border-b last:border-0">
                <td class="p-3 font-mono">{{ field.name }}</td>
                <td class="p-3">{{ field.label }}</td>
                <td class="p-3">{{ typeLabels[field.type.name] || field.type.name }}</td>
                <td class="p-3">{{ field.required ? 'Yes' : 'No' }}</td>
                <td class="p-3">{{ field.type.name === 'quantity' ? (field.type.unit || '-') : '-' }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </template>
      </Card>

      <Card title="Version History">
        <div v-if="history.length === 0" class="text-sm text-muted-foreground text-center py-4">
          No version history.
        </div>
        <div v-else class="border rounded-md">
          <table class="w-full text-sm">
            <thead>
            <tr class="border-b bg-muted/50">
              <th class="text-left p-3 font-medium">Version</th>
              <th class="text-left p-3 font-medium">Description</th>
              <th class="text-left p-3 font-medium">Fields</th>
              <th class="text-left p-3 font-medium">Status</th>
              <th class="text-left p-3 font-medium">Date</th>
              <th class="text-left p-3 font-medium"></th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="ver in history" :key="ver.version" class="border-b last:border-0">
              <td class="p-3 font-mono">v{{ ver.version }}</td>
              <td class="p-3 text-muted-foreground">{{ ver.description || '-' }}</td>
              <td class="p-3">{{ ver.fields.length }}</td>
              <td class="p-3">
                  <span
                      :class="ver.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'"
                      class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
                  >
                    {{ ver.active ? 'Active' : 'Inactive' }}
                  </span>
              </td>
              <td class="p-3 text-muted-foreground text-xs">{{ new Date(ver.modified).toLocaleString() }}</td>
              <td class="p-3">
                <button
                    class="text-xs text-primary hover:underline"
                    @click="viewing = viewing?.version === ver.version ? null : ver"
                >
                  {{ viewing?.version === ver.version ? 'Hide' : 'View' }}
                </button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div v-if="viewing" class="mt-4 border rounded-md p-4">
          <h4 class="text-sm font-medium mb-2">v{{ viewing.version }} Fields</h4>
          <div v-if="viewing.fields.length === 0" class="text-sm text-muted-foreground">No fields.</div>
          <table v-else class="w-full text-sm">
            <thead>
            <tr class="border-b">
              <th class="text-left py-2 font-medium text-muted-foreground">Name</th>
              <th class="text-left py-2 font-medium text-muted-foreground">Type</th>
              <th class="text-left py-2 font-medium text-muted-foreground">Required</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="field in viewing.fields" :key="field.name" class="border-b last:border-0">
              <td class="py-2 font-mono">{{ field.name }}</td>
              <td class="py-2">{{ typeLabels[field.type.name] || field.type.name }}</td>
              <td class="py-2">{{ field.required ? 'Yes' : 'No' }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </Card>
    </template>
  </div>
</template>
