<script lang="ts" setup>
import {ref, watch} from 'vue'
import type {Attribute, Schema} from '@/lib/api'

const props = defineProps<{
  schema: Schema
  modelValue: Record<string, Attribute>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, Attribute>]
}>()

const values = ref<Record<string, string>>({})
const flags = ref<Record<string, boolean>>({})

function init() {
  for (const field of props.schema.fields) {
    const existing = props.modelValue[field.name]
    if (field.type.name === 'boolean') {
      flags.value[field.name] = existing?.value === 'true'
    } else {
      values.value[field.name] = String(existing?.value ?? '')
    }
  }
}

init()

function serialize() {
  const result: Record<string, Attribute> = {}
  for (const field of props.schema.fields) {
    let raw: string
    if (field.type.name === 'boolean') {
      raw = String(flags.value[field.name] ?? false)
    } else {
      raw = values.value[field.name] ?? ''
    }
    if (raw || field.required) {
      result[field.name] = {value: raw}
    }
  }
  return result
}

watch([values, flags], () => {
  emit('update:modelValue', serialize())
}, {deep: true})
</script>

<template>
  <div class="space-y-4">
    <div v-for="field in schema.fields" :key="field.name" class="space-y-1">
      <label class="text-sm font-medium">
        {{ field.label || field.name }}
        <span v-if="field.required" class="text-destructive">*</span>
      </label>
      <p v-if="field.description" class="text-xs text-muted-foreground">{{ field.description }}</p>

      <input
          v-if="field.type.name === 'string'"
          v-model="values[field.name]"
          class="w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          type="text"
      />

      <input
          v-else-if="field.type.name === 'numeric'"
          v-model="values[field.name]"
          class="w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          type="number"
      />

      <label v-else-if="field.type.name === 'boolean'" class="flex items-center gap-2 text-sm">
        <input v-model="flags[field.name]" class="rounded border" type="checkbox"/>
        {{ field.label || field.name }}
      </label>

      <input
          v-else-if="field.type.name === 'datetime'"
          v-model="values[field.name]"
          class="w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          type="date"
      />

      <div v-else-if="field.type.name === 'quantity'" class="flex gap-2">
        <input
            v-model="values[field.name]"
            class="flex-1 rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
            type="number"
        />
        <span class="inline-flex items-center rounded-md border bg-muted px-3 py-2 text-sm text-muted-foreground">
          {{ field.type.unit || 'unit' }}
        </span>
      </div>

      <input
          v-else
          v-model="values[field.name]"
          class="w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          placeholder="Comma-separated values"
          type="text"
      />
    </div>
  </div>
</template>
