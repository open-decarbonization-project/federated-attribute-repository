<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {
  type Amendment,
  attach as attachDocument,
  type Certificate,
  display,
  type Document,
  documents as loadDocuments,
  entries as loadEntries,
  type Entry,
  find,
  publish as publishCertificate,
  retire as retireCertificate,
  update,
  upload as uploadDocument,
} from '@/lib/api'
import Card from '@/components/Card.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import DocumentPreview from '@/components/DocumentPreview.vue'
import {
  ArrowLeft,
  Eye,
  File,
  FileSpreadsheet,
  FileText,
  Image,
  Pause,
  Play,
  Send,
  Trash2,
  Upload,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const certificate = ref<Certificate | null>(null)
const docs = ref<Document[]>([])
const ledger = ref<Entry[]>([])
const loading = ref(true)
const error = ref('')
const tab = ref<'attributes' | 'documents' | 'ledger'>('attributes')
const uploading = ref(false)
const acting = ref(false)
const dragging = ref(false)
const preview = ref<Document | null>(null)

function icon(media: string) {
  if (media === 'application/pdf') return FileText
  if (media.startsWith('image/')) return Image
  if (media.includes('spreadsheet') || media.includes('excel') || media === 'text/csv') return FileSpreadsheet
  return File
}

const urn = computed(() => route.params.urn as string)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [found, attached, published] = await Promise.all([
      find(urn.value),
      loadDocuments(urn.value),
      loadEntries(urn.value),
    ])
    certificate.value = found
    docs.value = attached
    ledger.value = published
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to load certificate'
  } finally {
    loading.value = false
  }
}

async function transition(status: 'ACTIVE' | 'SUSPENDED' | 'RETIRED') {
  if (!certificate.value) return
  acting.value = true
  error.value = ''
  try {
    if (status === 'RETIRED') {
      certificate.value = await retireCertificate(urn.value)
    } else {
      const amendment: Amendment = {attributes: {}, status}
      certificate.value = await update(urn.value, amendment)
    }
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to update status'
  } finally {
    acting.value = false
  }
}

async function attach(file: File) {
  uploading.value = true
  error.value = ''
  try {
    const document = await uploadDocument(file)
    await attachDocument(urn.value, document.id)
    docs.value.push(document)
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to upload document'
  } finally {
    uploading.value = false
  }
}

function select(event: Event) {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    attach(files[0])
  }
}

function drop(event: DragEvent) {
  dragging.value = false
  const files = event.dataTransfer?.files
  if (files && files.length > 0) {
    attach(files[0])
  }
}

async function anchor() {
  acting.value = true
  error.value = ''
  try {
    const published = await publishCertificate(urn.value)
    ledger.value.push(...published)
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to publish'
  } finally {
    acting.value = false
  }
}

function back() {
  router.push('/certificates')
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
        <h2 class="text-3xl font-bold tracking-tight">Certificate Detail</h2>
        <p class="text-muted-foreground mt-1 font-mono text-sm">{{ urn }}</p>
      </div>
    </div>

    <div v-if="error" class="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
      {{ error }}
    </div>

    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="text-muted-foreground">Loading...</div>
    </div>

    <template v-else-if="certificate">
      <div class="grid gap-4 md:grid-cols-3">
        <Card title="Identity">
          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span class="text-muted-foreground">URN</span>
              <span class="font-mono text-xs">{{ certificate.urn }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Namespace</span>
              <span>{{ certificate.namespace }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Identifier</span>
              <span>{{ certificate.identifier }}</span>
            </div>
            <div v-if="certificate.schema" class="flex justify-between">
              <span class="text-muted-foreground">Schema</span>
              <RouterLink :to="`/schemas/${certificate.schema}`" class="text-primary hover:underline text-xs font-mono">
                v{{ certificate.pin }}
              </RouterLink>
            </div>
          </div>
        </Card>

        <Card title="Status">
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <StatusBadge :status="certificate.status"/>
              <span class="text-xs text-muted-foreground">{{ certificate.owner ?? 'No owner' }}</span>
            </div>
            <div class="flex gap-2 flex-wrap">
              <button
                  v-if="certificate.status === 'DRAFT' || certificate.status === 'SUSPENDED'"
                  :disabled="acting"
                  class="inline-flex items-center gap-1 rounded-md bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50 transition-colors"
                  @click="transition('ACTIVE')"
              >
                <Play class="h-3 w-3"/>
                Activate
              </button>
              <button
                  v-if="certificate.status === 'ACTIVE'"
                  :disabled="acting"
                  class="inline-flex items-center gap-1 rounded-md bg-orange-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-orange-700 disabled:opacity-50 transition-colors"
                  @click="transition('SUSPENDED')"
              >
                <Pause class="h-3 w-3"/>
                Suspend
              </button>
              <button
                  v-if="certificate.status !== 'RETIRED'"
                  :disabled="acting"
                  class="inline-flex items-center gap-1 rounded-md bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
                  @click="transition('RETIRED')"
              >
                <Trash2 class="h-3 w-3"/>
                Retire
              </button>
            </div>
          </div>
        </Card>

        <Card title="Integrity">
          <div class="space-y-2 text-sm">
            <div v-if="certificate.integrity">
              <span class="text-muted-foreground block mb-1">Digest</span>
              <span class="font-mono text-xs break-all">{{ certificate.integrity.digest }}</span>
            </div>
            <div v-else class="text-muted-foreground">No integrity data</div>
            <div class="flex justify-between text-xs text-muted-foreground pt-2 border-t">
              <span>Created {{ new Date(certificate.created).toLocaleDateString() }}</span>
              <span>Modified {{ new Date(certificate.modified).toLocaleDateString() }}</span>
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
          <button
              :class="[
              'px-3 py-2 text-sm font-medium border-b-2 transition-colors',
              tab === 'ledger'
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground',
            ]"
              @click="tab = 'ledger'"
          >
            Ledger ({{ ledger.length }})
          </button>
        </nav>
      </div>

      <!-- Attributes Tab -->
      <Card v-if="tab === 'attributes'" title="Attributes">
        <div v-if="Object.keys(certificate.attributes).length === 0"
             class="text-sm text-muted-foreground py-4 text-center">
          No attributes defined.
        </div>
        <table v-else class="w-full text-sm">
          <thead>
          <tr class="border-b">
            <th class="text-left py-3 font-medium text-muted-foreground">Name</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Value</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Source</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Verified</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Timestamp</th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="(attribute, name) in certificate.attributes"
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
            <td class="py-3 text-muted-foreground text-xs">{{ attribute.timestamp ?? '--' }}</td>
          </tr>
          </tbody>
        </table>
      </Card>

      <!-- Documents Tab -->
      <Card v-if="tab === 'documents'" title="Documents">
        <template #header>
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-semibold leading-none tracking-tight">Documents</h3>
            <label
                class="inline-flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90 cursor-pointer transition-colors"
            >
              <Upload class="h-3 w-3"/>
              Upload
              <input class="hidden" type="file" @change="select"/>
            </label>
          </div>
        </template>

        <div
            :class="dragging ? 'border-primary bg-primary/5' : 'border-muted'"
            class="border-2 border-dashed rounded-md p-6 text-center mb-4 transition-colors"
            @dragleave="dragging = false"
            @dragover.prevent="dragging = true"
            @drop.prevent="drop"
        >
          <FileText class="h-8 w-8 mx-auto text-muted-foreground mb-2"/>
          <p class="text-sm text-muted-foreground">
            {{ uploading ? 'Uploading...' : 'Drag and drop a file here, or use the Upload button' }}
          </p>
        </div>

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
            <th class="text-left py-3 font-medium text-muted-foreground">Uploaded</th>
            <th class="text-left py-3 font-medium text-muted-foreground w-8"></th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="document in docs"
              :key="document.id"
              class="border-b last:border-0 cursor-pointer hover:bg-muted/50 transition-colors"
              @click="preview = document"
          >
            <td class="py-3">
              <component :is="icon(document.media)" class="h-4 w-4 text-muted-foreground"/>
            </td>
            <td class="py-3 font-medium">{{ document.filename }}</td>
            <td class="py-3 text-muted-foreground">{{ document.media }}</td>
            <td class="py-3 text-muted-foreground">{{ (document.size / 1024).toFixed(1) }} KB</td>
            <td class="py-3 text-muted-foreground text-xs">
              {{ new Date(document.uploaded).toLocaleDateString() }}
            </td>
            <td class="py-3">
              <Eye class="h-4 w-4 text-muted-foreground"/>
            </td>
          </tr>
          </tbody>
        </table>
      </Card>

      <!-- Document Preview Modal -->
      <DocumentPreview
          v-if="preview"
          :document="preview"
          @close="preview = null"
      />

      <!-- Ledger Tab -->
      <Card v-if="tab === 'ledger'" title="Ledger Entries">
        <template #header>
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-semibold leading-none tracking-tight">Ledger Entries</h3>
            <button
                :disabled="acting"
                class="inline-flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
                @click="anchor"
            >
              <Send class="h-3 w-3"/>
              Publish
            </button>
          </div>
        </template>

        <div v-if="ledger.length === 0" class="text-sm text-muted-foreground py-4 text-center">
          No ledger entries. Click Publish to anchor this certificate.
        </div>
        <table v-else class="w-full text-sm">
          <thead>
          <tr class="border-b">
            <th class="text-left py-3 font-medium text-muted-foreground">Ledger</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Hash</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Proof</th>
            <th class="text-left py-3 font-medium text-muted-foreground">Published</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="entry in ledger" :key="entry.id" class="border-b last:border-0">
            <td class="py-3 font-medium">{{ entry.ledger }}</td>
            <td class="py-3 font-mono text-xs truncate max-w-[200px]">{{ entry.hash }}</td>
            <td class="py-3 font-mono text-xs truncate max-w-[200px]">{{ entry.proof ?? '--' }}</td>
            <td class="py-3 text-muted-foreground text-xs">
              {{ new Date(entry.published).toLocaleDateString() }}
            </td>
          </tr>
          </tbody>
        </table>
      </Card>
    </template>
  </div>
</template>
