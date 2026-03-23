<script lang="ts" setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {Download, File, FileSpreadsheet, FileText, Image, Loader2, X} from 'lucide-vue-next'
import type {Document} from '@/lib/api'
import {token} from '@/lib/auth'

const props = defineProps<{ document: Document }>()
const emit = defineEmits<{ close: [] }>()

const blob = ref<string | null>(null)
const loading = ref(true)
const failed = ref(false)

const category = computed(() => {
  const media = props.document.media
  if (media === 'application/pdf') return 'pdf'
  if (media.startsWith('image/')) return 'image'
  if (
      media === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
      media === 'application/vnd.ms-excel' ||
      media === 'text/csv'
  ) return 'spreadsheet'
  return 'other'
})

const icon = computed(() => {
  switch (category.value) {
    case 'pdf':
      return FileText
    case 'image':
      return Image
    case 'spreadsheet':
      return FileSpreadsheet
    default:
      return File
  }
})

async function load() {
  loading.value = true
  failed.value = false
  try {
    const headers: Record<string, string> = {}
    const bearer = token()
    if (bearer) {
      headers['Authorization'] = `Bearer ${bearer}`
    }
    const response = await fetch(`/v1/documents/${props.document.id}`, {headers})
    if (!response.ok) throw new Error(response.statusText)
    const data = await response.blob()
    blob.value = URL.createObjectURL(data)
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

function download() {
  if (!blob.value) return
  const anchor = window.document.createElement('a')
  anchor.href = blob.value
  anchor.download = props.document.filename
  anchor.click()
}

function close(event: MouseEvent) {
  if (event.target === event.currentTarget) {
    emit('close')
  }
}

onMounted(load)

onUnmounted(() => {
  if (blob.value) {
    URL.revokeObjectURL(blob.value)
  }
})
</script>

<template>
  <div
      class="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4"
      @click="close"
  >
    <div class="bg-card rounded-lg shadow-lg w-full max-w-5xl max-h-[90vh] flex flex-col">
      <div class="flex items-center justify-between p-4 border-b">
        <div class="flex items-center gap-2 min-w-0">
          <component :is="icon" class="h-4 w-4 shrink-0 text-muted-foreground"/>
          <span class="text-sm font-medium truncate">{{ document.filename }}</span>
          <span class="text-xs text-muted-foreground shrink-0">
            {{ (document.size / 1024).toFixed(1) }} KB
          </span>
        </div>
        <div class="flex items-center gap-2">
          <button
              :disabled="!blob"
              class="inline-flex items-center gap-1 rounded-md border px-3 py-1.5 text-xs font-medium hover:bg-accent disabled:opacity-50 transition-colors"
              @click="download"
          >
            <Download class="h-3 w-3"/>
            Download
          </button>
          <button
              class="inline-flex items-center rounded-md border p-1.5 hover:bg-accent transition-colors"
              @click="emit('close')"
          >
            <X class="h-4 w-4"/>
          </button>
        </div>
      </div>

      <div class="flex-1 overflow-auto min-h-0">
        <!-- Loading -->
        <div v-if="loading" class="flex items-center justify-center py-16">
          <Loader2 class="h-8 w-8 text-muted-foreground animate-spin"/>
        </div>

        <!-- Error -->
        <div v-else-if="failed" class="flex flex-col items-center justify-center py-16 gap-4">
          <component :is="icon" class="h-16 w-16 text-muted-foreground"/>
          <p class="text-sm text-destructive">Failed to load document</p>
        </div>

        <!-- PDF -->
        <iframe
            v-else-if="category === 'pdf' && blob"
            :src="blob"
            class="w-full h-full min-h-[70vh]"
            frameborder="0"
        />

        <!-- Image -->
        <div v-else-if="category === 'image' && blob" class="flex items-center justify-center p-4">
          <img
              :alt="document.filename"
              :src="blob"
              class="max-w-full max-h-[70vh] object-contain rounded"
          />
        </div>

        <!-- Spreadsheet / Other -->
        <div v-else class="flex flex-col items-center justify-center py-16 gap-4">
          <component :is="icon" class="h-16 w-16 text-muted-foreground"/>
          <p class="text-sm text-muted-foreground">
            Preview not available for {{ document.media }}
          </p>
          <button
              :disabled="!blob"
              class="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
              @click="download"
          >
            <Download class="h-4 w-4"/>
            Download File
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
