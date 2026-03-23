<script lang="ts" setup>
import {computed} from 'vue'
import {cn} from '@/lib/utils'

const props = defineProps<{
  status: string
}>()

const classes = computed(() => {
  const base = 'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold'
  switch (props.status) {
    case 'DRAFT':
      return cn(base, 'bg-yellow-100 text-yellow-800')
    case 'ACTIVE':
      return cn(base, 'bg-green-100 text-green-800')
    case 'SUSPENDED':
      return cn(base, 'bg-orange-100 text-orange-800')
    case 'RETIRED':
      return cn(base, 'bg-red-100 text-red-800')
    default:
      return cn(base, 'bg-gray-100 text-gray-800')
  }
})
</script>

<template>
  <span :class="classes">{{ status }}</span>
</template>
