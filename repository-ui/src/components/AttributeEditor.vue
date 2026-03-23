<script lang="ts" setup>
import {ref, watch} from 'vue'
import {Plus, Trash2} from 'lucide-vue-next'

interface Row {
  key: string
  value: string
  source: string
  type: string
}

const props = defineProps<{
  modelValue: Record<string, { value: string; source?: string }>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, { value: string; source?: string }>]
}>()

const rows = ref<Row[]>(toRows(props.modelValue))

function toRows(attributes: Record<string, { value: string; source?: string }>): Row[] {
  return Object.entries(attributes).map(([key, attribute]) => ({
    key,
    value: attribute.value,
    source: attribute.source ?? '',
    type: 'text',
  }))
}

function serialize(): Record<string, { value: string; source?: string }> {
  const result: Record<string, { value: string; source?: string }> = {}
  for (const row of rows.value) {
    if (row.key.trim()) {
      result[row.key.trim()] = {
        value: row.value,
        ...(row.source ? {source: row.source} : {}),
      }
    }
  }
  return result
}

function add() {
  rows.value.push({key: '', value: '', source: '', type: 'text'})
}

function remove(index: number) {
  rows.value.splice(index, 1)
  emit('update:modelValue', serialize())
}

watch(rows, () => {
  emit('update:modelValue', serialize())
}, {deep: true})
</script>

<template>
  <div class="space-y-3">
    <div class="flex items-center justify-between">
      <label class="text-sm font-medium">Attributes</label>
      <button
          class="inline-flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          type="button"
          @click="add"
      >
        <Plus class="h-3 w-3"/>
        Add
      </button>
    </div>

    <div v-if="rows.length === 0"
         class="text-sm text-muted-foreground py-4 text-center border rounded-md border-dashed">
      No attributes defined. Click Add to create one.
    </div>

    <div v-for="(row, index) in rows" :key="index" class="flex items-start gap-2">
      <div class="flex-1">
        <input
            v-model="row.key"
            class="w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
            placeholder="Name"
            type="text"
        />
      </div>
      <div class="flex-1">
        <input
            v-model="row.value"
            class="w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
            placeholder="Value"
            type="text"
        />
      </div>
      <div class="w-32">
        <select
            v-model="row.type"
            class="w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
        >
          <option value="text">Text</option>
          <option value="numeric">Numeric</option>
          <option value="flag">Flag</option>
          <option value="quantity">Quantity</option>
        </select>
      </div>
      <div class="flex-1">
        <input
            v-model="row.source"
            class="w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
            placeholder="Source (optional)"
            type="text"
        />
      </div>
      <button
          class="inline-flex items-center justify-center rounded-md p-2 text-destructive hover:bg-destructive/10 transition-colors"
          type="button"
          @click="remove(index)"
      >
        <Trash2 class="h-4 w-4"/>
      </button>
    </div>
  </div>
</template>
