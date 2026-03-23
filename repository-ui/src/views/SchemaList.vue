<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {RouterLink} from 'vue-router'
import {type Schema, schemas as fetchSchemas} from '@/lib/api'
import Card from '@/components/Card.vue'

const items = ref<Schema[]>([])
const namespace = ref('')
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await fetchSchemas(namespace.value.trim() || undefined)
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to load schemas'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-3xl font-bold tracking-tight">Schemas</h2>
        <p class="text-muted-foreground mt-1">Certificate registration schemas</p>
      </div>
      <RouterLink
          class="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          to="/schemas/create"
      >
        Create Schema
      </RouterLink>
    </div>

    <Card title="Filter">
      <div class="flex gap-3">
        <input
            v-model="namespace"
            class="flex-1 rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
            placeholder="Filter by namespace"
            type="text"
        />
        <button
            class="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
            @click="load"
        >
          Search
        </button>
      </div>
    </Card>

    <div v-if="error" class="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
      {{ error }}
    </div>

    <div v-if="loading" class="text-sm text-muted-foreground">Loading...</div>

    <div v-else-if="items.length === 0" class="text-sm text-muted-foreground text-center py-8">
      No schemas found.
    </div>

    <div v-else class="border rounded-md">
      <table class="w-full text-sm">
        <thead>
        <tr class="border-b bg-muted/50">
          <th class="text-left p-3 font-medium">Name</th>
          <th class="text-left p-3 font-medium">Namespace</th>
          <th class="text-left p-3 font-medium">Fields</th>
          <th class="text-left p-3 font-medium">Version</th>
          <th class="text-left p-3 font-medium">Active</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="item in items" :key="item.id" class="border-b last:border-0 hover:bg-muted/25">
          <td class="p-3">
            <RouterLink :to="`/schemas/${item.id}`" class="text-primary hover:underline font-medium">
              {{ item.name }}
            </RouterLink>
          </td>
          <td class="p-3 text-muted-foreground">{{ item.namespace }}</td>
          <td class="p-3">{{ item.fields.length }}</td>
          <td class="p-3">v{{ item.version }}</td>
          <td class="p-3">
              <span
                  :class="item.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'"
                  class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
              >
                {{ item.active ? 'Active' : 'Inactive' }}
              </span>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
