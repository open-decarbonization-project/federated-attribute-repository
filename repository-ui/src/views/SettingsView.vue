<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue'
import {
  addPeer,
  configuration,
  type FieldPolicy,
  type NodeConfig,
  type PeerInfo,
  peers,
  policies as fetchPolicies,
  type Policy,
  removePeer,
  type Schema,
  schemas as fetchSchemas,
  updatePolicies,
} from '@/lib/api'
import Card from '@/components/Card.vue'
import {Plus, Trash2} from 'lucide-vue-next'

type PolicyKind = 'public' | 'masked' | 'credential'

interface PolicyRow {
  field: string
  kind: PolicyKind
  role: string
}

const tab = ref<'general' | 'peers' | 'policies'>('general')
const config = ref<NodeConfig | null>(null)
const peerList = ref<PeerInfo[]>([])
const loading = ref(true)
const error = ref('')
const endpoint = ref('')
const adding = ref(false)

// Policies state
const schemaList = ref<Schema[]>([])
const selected = ref('')
const rows = ref<PolicyRow[]>([])
const saving = ref(false)
const dirty = ref(false)
const active = computed(() => schemaList.value.find(s => s.id === selected.value))
const fields = computed(() => active.value?.fields.map(f => f.name) ?? [])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [c, p] = await Promise.all([configuration(), peers()])
    config.value = c
    peerList.value = p
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to load settings'
  } finally {
    loading.value = false
  }
}

async function loadSchemas() {
  try {
    schemaList.value = await fetchSchemas()
  } catch {
    schemaList.value = []
  }
}

async function loadPolicies() {
  dirty.value = false
  if (!selected.value) {
    rows.value = []
    return
  }
  error.value = ''
  try {
    const items = await fetchPolicies(selected.value)
    rows.value = fromResponse(items)
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to load policies'
  }
}

watch(selected, loadPolicies)

async function add() {
  if (!endpoint.value.trim()) return
  adding.value = true
  error.value = ''
  try {
    await addPeer(endpoint.value.trim())
    endpoint.value = ''
    peerList.value = await peers()
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to add peer'
  } finally {
    adding.value = false
  }
}

async function remove(identity: string) {
  error.value = ''
  try {
    await removePeer(identity)
    peerList.value = await peers()
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to remove peer'
  }
}

function toPolicy(kind: PolicyKind, role: string): Policy {
  if (kind === 'masked') return {kind: 'masked'}
  if (kind === 'credential') return {kind: 'credential', role}
  return {kind: 'public'}
}

function serialize(): FieldPolicy[] {
  return rows.value.map(r => ({field: r.field, policy: toPolicy(r.kind, r.role)}))
}

function fromResponse(items: FieldPolicy[]): PolicyRow[] {
  const map = new Map(items.map(fp => [fp.field, fp]))
  return fields.value.map(name => {
    const fp = map.get(name)
    if (fp) {
      return {
        field: name,
        kind: fp.policy.kind as PolicyKind,
        role: fp.policy.kind === 'credential' ? (fp.policy as { role: string }).role : '',
      }
    }
    return {field: name, kind: 'public' as PolicyKind, role: ''}
  })
}

function mark() {
  dirty.value = true
}

async function save() {
  if (!selected.value) return
  saving.value = true
  error.value = ''
  try {
    const result = await updatePolicies(selected.value, serialize())
    rows.value = fromResponse(result)
    dirty.value = false
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to save policies'
  } finally {
    saving.value = false
  }
}

function switchTab(target: 'general' | 'peers' | 'policies') {
  tab.value = target
  if (target === 'policies' && schemaList.value.length === 0) {
    loadSchemas()
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div>
      <h2 class="text-3xl font-bold tracking-tight">Settings</h2>
      <p class="text-muted-foreground mt-1">Node configuration, peer management, and field policies</p>
    </div>

    <div v-if="error" class="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
      {{ error }}
    </div>

    <div class="border-b">
      <nav class="flex gap-4">
        <button
            :class="tab === 'general' ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground'"
            class="pb-2 text-sm font-medium transition-colors border-b-2"
            @click="switchTab('general')"
        >
          General
        </button>
        <button
            :class="tab === 'peers' ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground'"
            class="pb-2 text-sm font-medium transition-colors border-b-2"
            @click="switchTab('peers')"
        >
          Peers
        </button>
        <button
            :class="tab === 'policies' ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground'"
            class="pb-2 text-sm font-medium transition-colors border-b-2"
            @click="switchTab('policies')"
        >
          Policies
        </button>
      </nav>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="text-muted-foreground">Loading...</div>
    </div>

    <template v-else>
      <!-- General tab -->
      <Card v-if="tab === 'general' && config">
        <h3 class="text-lg font-semibold mb-4">Node Configuration</h3>
        <dl class="space-y-4">
          <div>
            <dt class="text-sm font-medium text-muted-foreground">Identity</dt>
            <dd class="mt-1 font-mono text-sm">{{ config.identity }}</dd>
          </div>
          <div>
            <dt class="text-sm font-medium text-muted-foreground">Supported Namespaces</dt>
            <dd class="mt-1 flex flex-wrap gap-2">
              <span
                  v-for="namespace in config.namespaces"
                  :key="namespace"
                  class="inline-flex items-center rounded-full bg-secondary px-2.5 py-0.5 text-xs font-medium"
              >
                {{ namespace }}
              </span>
            </dd>
          </div>
          <div>
            <dt class="text-sm font-medium text-muted-foreground">Protocol Version</dt>
            <dd class="mt-1 text-sm">{{ config.version }}</dd>
          </div>
          <div>
            <dt class="text-sm font-medium text-muted-foreground">Public Key</dt>
            <dd class="mt-1 font-mono text-xs break-all">{{ config.key }}</dd>
          </div>
        </dl>
      </Card>

      <!-- Peers tab -->
      <div v-if="tab === 'peers'" class="space-y-4">
        <Card>
          <h3 class="text-lg font-semibold mb-4">Add Peer</h3>
          <form class="flex gap-2" @submit.prevent="add">
            <input
                v-model="endpoint"
                class="flex-1 rounded-md border bg-background py-2 px-3 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                placeholder="https://peer.example.com:8081"
                type="text"
            />
            <button
                :disabled="adding || !endpoint.trim()"
                class="inline-flex items-center gap-1 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:pointer-events-none transition-colors"
                type="submit"
            >
              <Plus class="h-4 w-4"/>
              {{ adding ? 'Adding...' : 'Add Peer' }}
            </button>
          </form>
        </Card>

        <Card>
          <h3 class="text-lg font-semibold mb-4">Connected Peers</h3>
          <div v-if="peerList.length === 0" class="text-sm text-muted-foreground py-4 text-center">
            No peers configured.
          </div>
          <table v-else class="w-full text-sm">
            <thead>
            <tr class="border-b">
              <th class="text-left py-3 font-medium text-muted-foreground">Identity</th>
              <th class="text-left py-3 font-medium text-muted-foreground">Endpoint</th>
              <th class="text-left py-3 font-medium text-muted-foreground">Namespaces</th>
              <th class="text-left py-3 font-medium text-muted-foreground">Last Seen</th>
              <th class="text-right py-3 font-medium text-muted-foreground"></th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="peer in peerList" :key="peer.identity" class="border-b last:border-0">
              <td class="py-3 font-mono text-xs">{{ peer.identity }}</td>
              <td class="py-3 text-muted-foreground">{{ peer.endpoint }}</td>
              <td class="py-3">
                <div class="flex flex-wrap gap-1">
                    <span
                        v-for="namespace in peer.namespaces"
                        :key="namespace"
                        class="inline-flex items-center rounded-full bg-secondary px-2 py-0.5 text-xs font-medium"
                    >
                      {{ namespace }}
                    </span>
                </div>
              </td>
              <td class="py-3 text-muted-foreground">
                {{ peer.last_seen ? new Date(peer.last_seen).toLocaleString() : 'Never' }}
              </td>
              <td class="py-3 text-right">
                <button
                    class="inline-flex items-center rounded-md p-1.5 text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
                    @click="remove(peer.identity)"
                >
                  <Trash2 class="h-4 w-4"/>
                </button>
              </td>
            </tr>
            </tbody>
          </table>
        </Card>
      </div>

      <!-- Policies tab -->
      <div v-if="tab === 'policies'" class="space-y-4">
        <Card>
          <h3 class="text-lg font-semibold mb-4">Schema</h3>
          <select
              v-model="selected"
              class="w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          >
            <option value="">Select a schema</option>
            <option v-for="s in schemaList" :key="s.id" :value="s.id">
              {{ s.name }} ({{ s.namespace }})
            </option>
          </select>
        </Card>

        <Card v-if="active">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold">Field Policies</h3>
            <button
                :disabled="saving || !dirty"
                class="inline-flex items-center gap-1 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:pointer-events-none transition-colors"
                @click="save()"
            >
              {{ saving ? 'Saving...' : 'Save' }}
            </button>
          </div>
          <div v-if="rows.length === 0" class="text-sm text-muted-foreground py-4 text-center">
            No fields defined in this schema.
          </div>
          <table v-else class="w-full text-sm">
            <thead>
            <tr class="border-b">
              <th class="text-left py-3 font-medium text-muted-foreground">Field</th>
              <th class="text-left py-3 font-medium text-muted-foreground">Policy</th>
              <th class="text-left py-3 font-medium text-muted-foreground">Role</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="row in rows" :key="row.field" class="border-b last:border-0">
              <td class="py-3 font-mono">{{ row.field }}</td>
              <td class="py-3">
                <select
                    v-model="row.kind"
                    class="rounded-md border bg-background px-2 py-1 text-xs focus:outline-none focus:ring-2 focus:ring-ring"
                    @change="mark()"
                >
                  <option value="public">Public</option>
                  <option value="masked">Masked</option>
                  <option value="credential">Credential</option>
                </select>
              </td>
              <td class="py-3">
                <input
                    v-if="row.kind === 'credential'"
                    v-model="row.role"
                    class="rounded-md border bg-background px-2 py-1 text-xs w-32 focus:outline-none focus:ring-2 focus:ring-ring"
                    placeholder="role"
                    type="text"
                    @input="mark()"
                />
                <span v-else class="text-muted-foreground text-xs">-</span>
              </td>
            </tr>
            </tbody>
          </table>
        </Card>
      </div>
    </template>
  </div>
</template>
