<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {display, type Document, type Resolution, resolve, resourceDocuments, resourceDocumentUrl,} from '@/lib/api'
import {token} from '@/lib/auth'
import Card from '@/components/Card.vue'
import {ArrowLeft, Download, Eye, File, FileSpreadsheet, FileText, Globe, Image, Loader2, X,} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const resolution = ref<Resolution | null>(null)
const docs = ref<Document[]>([])
const loading = ref(true)
const error = ref('')
const tab = ref<'attributes' | 'documents'>('attributes')
const preview = ref<Document | null>(null)
const blob = ref<string | null>(null)
const previewing = ref(false)
const failed = ref(false)

const urn = computed(() => route.params.urn as string)
const source = computed(() => (route.query.resolver as string) || '')

function label(resolver: string): string {
  try {
    const url = new URL(resolver)
    return url.hostname
  } catch {
    return resolver
  }
}

function icon(media: string) {
  if (media === 'application/pdf') return FileText
  if (media.startsWith('image/')) return Image
  if (media.includes('spreadsheet') || media.includes('excel') || media === 'text/csv') return FileSpreadsheet
  return File
}

function category(media: string): string {
  if (media === 'application/pdf') return 'pdf'
  if (media.startsWith('image/')) return 'image'
  return 'other'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [resolved, attached] = await Promise.all([
      resolve(urn.value),
      resourceDocuments(urn.value),
    ])
    resolution.value = resolved
    docs.value = attached
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to load resource'
  } finally {
    loading.value = false
  }
}

async function open(document: Document) {
  preview.value = document
  previewing.value = true
  failed.value = false
  if (blob.value) {
    URL.revokeObjectURL(blob.value)
    blob.value = null
  }
  try {
    const headers: Record<string, string> = {}
    const bearer = token()
    if (bearer) {
      headers['Authorization'] = `Bearer ${bearer}`
    }
    const response = await fetch(resourceDocumentUrl(urn.value, document.id), {headers})
    if (!response.ok) throw new Error(response.statusText)
    const data = await response.blob()
    blob.value = URL.createObjectURL(data)
  } catch {
    failed.value = true
  } finally {
    previewing.value = false
  }
}

function close() {
  preview.value = null
  if (blob.value) {
    URL.revokeObjectURL(blob.value)
    blob.value = null
  }
}

function download() {
  if (!blob.value || !preview.value) return
  const anchor = window.document.createElement('a')
  anchor.href = blob.value
  anchor.download = preview.value.filename
  anchor.click()
}

function back() {
  router.push('/search')
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center gap-4">
      <button
          class="inline-flex items-center rounded-md border p-2 hover:bg-accent transition-colors"
          @click="back"
      >
        <ArrowLeft class="h-4 w-4"/>
      </button>
      <div class="flex-1">
        <h2 class="text-3xl font-bold tracking-tight">Remote Resource</h2>
        <p class="text-muted-foreground mt-1 font-mono text-sm">{{ urn }}</p>
      </div>
      <span
          v-if="source"
          :title="source"
          class="inline-flex items-center gap-1.5 text-sm font-medium text-blue-600"
      >
        <Globe class="h-4 w-4"/>
        {{ label(source) }}
      </span>
    </div>

    <div v-if="error" class="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
      {{ error }}
    </div>

    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="text-muted-foreground">Loading...</div>
    </div>

    <template v-else-if="resolution">
      <div class="grid gap-4 md:grid-cols-3">
        <Card title="Identity">
          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span class="text-muted-foreground">URN</span>
              <span class="font-mono text-xs">{{ resolution.urn }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Namespace</span>
              <span>{{ resolution.namespace }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Identifier</span>
              <span>{{ resolution.identifier }}</span>
            </div>
          </div>
        </Card>

        <Card title="Source">
          <div class="space-y-2 text-sm">
            <div class="flex items-center gap-2">
              <Globe class="h-4 w-4 text-blue-600"/>
              <span :title="source" class="font-medium">{{ label(source) }}</span>
            </div>
            <div class="text-xs text-muted-foreground break-all">{{ source }}</div>
          </div>
        </Card>

        <Card title="Integrity">
          <div class="space-y-2 text-sm">
            <div v-if="resolution.integrity">
              <span class="text-muted-foreground block mb-1">Digest</span>
              <span class="font-mono text-xs break-all">{{ resolution.integrity.digest }}</span>
            </div>
            <div v-else class="text-muted-foreground">No integrity data</div>
            <div v-if="resolution.timestamp" class="text-xs text-muted-foreground pt-2 border-t">
              Modified {{ new Date(resolution.timestamp).toLocaleDateString() }}
            </div>
          </div>
        </Card>
      </div>

      <div class="border-b">
        <nav class="flex gap-4">
          <button
              :class="[
              'px-3 py-2 text-sm font-medium border-b-2 transition-colors',
              tab === 'attributes'
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground',
            ]"
              @click="tab = 'attributes'"
          >
            Attributes
          </button>
          <button
              :class="[
              'px-3 py-2 text-sm font-medium border-b-2 transition-colors',
              tab === 'documents'
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground',
            ]"
              @click="tab = 'documents'"
          >
            Documents ({{ docs.length }})
          </button>
        </nav>
      </div>

      <Card v-if="tab === 'attributes'" title="Attributes">
        <div v-if="Object.keys(resolution.attributes).length === 0"
             class="text-sm text-muted-foreground py-4 text-center">
          No attributes.
        </div>
        <table v-else class="w-full text-sm">
          <thead>
          <tr class="border-b">
            <th class="text-left py-3 font-medium text-muted-foreground">Name</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Value</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Source</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Verified</th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="(attribute, name) in resolution.attributes"
              :key="name"
              class="border-b last:border-0"
          >
            <td class="py-3 font-medium">{{ name }}</td>
            <td class="py-3 font-mono text-xs">{{ display(attribute.value) }}</td>
            <td class="py-3 text-muted-foreground">{{ attribute.source ?? '--' }}</td>
            <td class="py-3">
              <span v-if="attribute.verified" class="text-green-600">Yes</span>
              <span v-else class="text-muted-foreground">No</span>
            </td>
          </tr>
          </tbody>
        </table>
      </Card>

      <Card v-if="tab === 'documents'" title="Documents">
        <div v-if="docs.length === 0" class="text-sm text-muted-foreground py-4 text-center">
          No documents attached.
        </div>
        <table v-else class="w-full text-sm">
          <thead>
          <tr class="border-b">
            <th class="text-left py-3 font-medium text-muted-foreground w-8"></th>
            <th class="text-left py-3 font-medium text-muted-foreground">Filename</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Type</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Size</th>
            <th class="text-left py-3 font-medium text-muted-foreground w-8"></th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="document in docs"
              :key="document.id"
              class="border-b last:border-0 cursor-pointer hover:bg-muted/50 transition-colors"
              @click="open(document)"
          >
            <td class="py-3">
              <component :is="icon(document.media)" class="h-4 w-4 text-muted-foreground"/>
            </td>
            <td class="py-3 font-medium">{{ document.filename }}</td>
            <td class="py-3 text-muted-foreground">{{ document.media }}</td>
            <td class="py-3 text-muted-foreground">{{ (document.size / 1024).toFixed(1) }} KB</td>
            <td class="py-3">
              <Eye class="h-4 w-4 text-muted-foreground"/>
            </td>
          </tr>
          </tbody>
        </table>
      </Card>
    </template>

    <!-- Document Preview Modal -->
    <div
        v-if="preview"
        class="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4"
        @click.self="close"
    >
      <div class="bg-card rounded-lg shadow-lg w-full max-w-5xl max-h-[90vh] flex flex-col">
        <div class="flex items-center justify-between p-4 border-b">
          <div class="flex items-center gap-2 min-w-0">
            <component :is="icon(preview.media)" class="h-4 w-4 shrink-0 text-muted-foreground"/>
            <span class="text-sm font-medium truncate">{{ preview.filename }}</span>
            <span class="text-xs text-muted-foreground shrink-0">
              {{ (preview.size / 1024).toFixed(1) }} KB
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
                @click="close"
            >
              <X class="h-4 w-4"/>
            </button>
          </div>
        </div>

        <div class="flex-1 overflow-auto min-h-0">
          <div v-if="previewing" class="flex items-center justify-center py-16">
            <Loader2 class="h-8 w-8 text-muted-foreground animate-spin"/>
          </div>
          <div v-else-if="failed" class="flex flex-col items-center justify-center py-16 gap-4">
            <component :is="icon(preview.media)" class="h-16 w-16 text-muted-foreground"/>
            <p class="text-sm text-destructive">Failed to load document</p>
          </div>
          <iframe
              v-else-if="category(preview.media) === 'pdf' && blob"
              :src="blob"
              class="w-full h-full min-h-[70vh]"
              frameborder="0"
          />
          <div v-else-if="category(preview.media) === 'image' && blob" class="flex items-center justify-center p-4">
            <img
                :alt="preview.filename"
                :src="blob"
                class="max-w-full max-h-[70vh] object-contain rounded"
            />
          </div>
          <div v-else class="flex flex-col items-center justify-center py-16 gap-4">
            <component :is="icon(preview.media)" class="h-16 w-16 text-muted-foreground"/>
            <p class="text-sm text-muted-foreground">
              Preview not available for {{ preview.media }}
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
  </div>
</template>
