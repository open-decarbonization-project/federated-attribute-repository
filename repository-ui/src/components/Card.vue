<script lang="ts" setup>
defineProps<{
  title?: string
}>()
</script>

<template>
  <div class="rounded-lg border bg-card text-card-foreground shadow-sm">
    <div v-if="title || $slots.header" class="flex flex-col space-y-1.5 p-6 pb-4">
      <slot name="header">
        <h3 class="text-lg font-semibold leading-none tracking-tight">{{ title }}</h3>
      </slot>
    </div>
    <div class="p-6 pt-0">
      <slot/>
    </div>
  </div>
</template>
