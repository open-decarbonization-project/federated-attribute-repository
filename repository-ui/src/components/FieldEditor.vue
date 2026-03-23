<script lang="ts" setup>
import {ref, watch} from 'vue'
import {ArrowDown, ArrowUp, Plus, Trash2} from 'lucide-vue-next'
import type {Field, FieldType, TypeName} from '@/lib/api'

interface Row {
  name: string
  label: string
  description: string
  typeName: TypeName
  required: boolean
  unit: string
  position: number
}

const props = defineProps<{
  modelValue: Field[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Field[]]
}>()

const types: { value: TypeName; label: string }[] = [
  {value: 'string', label: 'String'},
  {value: 'numeric', label: 'Numeric'},
  {value: 'boolean', label: 'Boolean'},
  {value: 'datetime', label: 'DateTime'},
  {value: 'quantity', label: 'Quantity'},
  {value: 'list', label: 'List'},
  {value: 'record', label: 'Record'},
]

const rows = ref<Row[]>(props.modelValue.map(toRow))

function toRow(field: Field): Row {
  return {
    name: field.name,
    label: field.label,
    description: field.description,
    typeName: field.type.name,
    required: field.required,
    unit: field.type.name === 'quantity' ? (field.type.unit ?? '') : '',
    position: field.position,
  }
}

function toType(row: Row): FieldType {
  if (row.typeName === 'quantity') {
    return {name: 'quantity', unit: row.unit || undefined}
  }
  if (row.typeName === 'list') {
    return {name: 'list', element: {name: 'string'}}
  }
  if (row.typeName === 'record') {
    return {name: 'record', fields: []}
  }
  return {name: row.typeName}
}

function serialize(): Field[] {
  return rows.value.map((row, index) => ({
    name: row.name,
    label: row.label || row.name,
    description: row.description,
    type: toType(row),
    required: row.required,
    position: index,
  }))
}

function add() {
  rows.value.push({
    name: '',
    label: '',
    description: '',
    typeName: 'string',
    required: false,
    unit: '',
    position: rows.value.length,
  })
}

function remove(index: number) {
  rows.value.splice(index, 1)
  emit('update:modelValue', serialize())
}

function up(index: number) {
  if (index === 0) return
  const item = rows.value.splice(index, 1)[0]
  rows.value.splice(index - 1, 0, item)
  emit('update:modelValue', serialize())
}

function down(index: number) {
  if (index >= rows.value.length - 1) return
  const item = rows.value.splice(index, 1)[0]
  rows.value.splice(index + 1, 0, item)
  emit('update:modelValue', serialize())
}

watch(rows, () => {
  emit('update:modelValue', serialize())
}, {deep: true})
</script>

<template>
  <div class="space-y-3">
    <div class="flex items-center justify-between">
      <label class="text-sm font-medium">Fields</label>
      <button
          class="inline-flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          type="button"
          @click="add"
      >
        <Plus class="h-3 w-3"/>
        Add Field
      </button>
    </div>

    <div v-if="rows.length === 0"
         class="text-sm text-muted-foreground py-4 text-center border rounded-md border-dashed">
      No fields defined. Click Add Field to create one.
    </div>

    <div v-for="(row, index) in rows" :key="index" class="border rounded-md p-3 space-y-3">
      <div class="flex items-center justify-between">
        <span class="text-xs text-muted-foreground">Field {{ index + 1 }}</span>
        <div class="flex items-center gap-1">
          <button class="p-1 hover:bg-accent rounded transition-colors" type="button" @click="up(index)">
            <ArrowUp class="h-3 w-3"/>
          </button>
          <button class="p-1 hover:bg-accent rounded transition-colors" type="button" @click="down(index)">
            <ArrowDown class="h-3 w-3"/>
          </button>
          <button class="p-1 text-destructive hover:bg-destructive/10 rounded transition-colors" type="button"
                  @click="remove(index)">
            <Trash2 class="h-3 w-3"/>
          </button>
        </div>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="text-xs text-muted-foreground">Name</label>
          <input
              v-model="row.name"
              class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
              placeholder="e.g. volume"
              type="text"
          />
        </div>
        <div>
          <label class="text-xs text-muted-foreground">Label</label>
          <input
              v-model="row.label"
              class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
              placeholder="e.g. Volume"
              type="text"
          />
        </div>
      </div>
      <div>
        <label class="text-xs text-muted-foreground">Description</label>
        <input
            v-model="row.description"
            class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
            placeholder="Optional description"
            type="text"
        />
      </div>
      <div class="grid grid-cols-3 gap-3">
        <div>
          <label class="text-xs text-muted-foreground">Type</label>
          <select
              v-model="row.typeName"
              class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          >
            <option v-for="t in types" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
        </div>
        <div v-if="row.typeName === 'quantity'">
          <label class="text-xs text-muted-foreground">Unit</label>
          <input
              v-model="row.unit"
              class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
              placeholder="e.g. kg, m"
              type="text"
          />
        </div>
        <div class="flex items-end pb-1">
          <label class="flex items-center gap-2 text-sm">
            <input v-model="row.required" class="rounded border" type="checkbox"/>
            Required
          </label>
        </div>
      </div>
    </div>
  </div>
</template>
