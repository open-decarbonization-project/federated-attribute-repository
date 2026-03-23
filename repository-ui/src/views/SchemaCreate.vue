<script lang="ts" setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {createSchema, type Field} from '@/lib/api'
import Card from '@/components/Card.vue'
import FieldEditor from '@/components/FieldEditor.vue'

const router = useRouter()
const namespace = ref('')
const name = ref('')
const description = ref('')
const fields = ref<Field[]>([])
const submitting = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!namespace.value.trim()) {
    error.value = 'Namespace is required'
    return
  }
  if (!name.value.trim()) {
    error.value = 'Name is required'
    return
  }

  submitting.value = true
  try {
    const schema = await createSchema({
      namespace: namespace.value.trim(),
      name: name.value.trim(),
      description: description.value.trim(),
      fields: fields.value,
    })
    router.push(`/schemas/${schema.id}`)
  } catch (thrown) {
    error.value = thrown instanceof Error ? thrown.message : 'Failed to create schema'
  } finally {
    submitting.value = false
  }
}

function cancel() {
  router.push('/schemas')
}
</script>

<template>
  <div class="space-y-6 max-w-3xl">
    <div>
      <h2 class="text-3xl font-bold tracking-tight">Create Schema</h2>
      <p class="text-muted-foreground mt-1">Define a certificate registration schema</p>
    </div>

    <div v-if="error" class="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
      {{ error }}
    </div>

    <form class="space-y-6" @submit.prevent="submit">
      <Card title="Details">
        <div class="space-y-4">
          <div>
            <label class="text-sm font-medium" for="namespace">Namespace</label>
            <input
                id="namespace"
                v-model="namespace"
                class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                placeholder="e.g. org.example"
                type="text"
            />
          </div>
          <div>
            <label class="text-sm font-medium" for="name">Name</label>
            <input
                id="name"
                v-model="name"
                class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                placeholder="e.g. carbon-credit"
                type="text"
            />
          </div>
          <div>
            <label class="text-sm font-medium" for="description">Description</label>
            <textarea
                id="description"
                v-model="description"
                class="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                placeholder="Optional description of this schema"
                rows="3"
            />
          </div>
        </div>
      </Card>

      <Card title="Fields">
        <FieldEditor v-model="fields"/>
      </Card>

      <div class="flex items-center gap-3">
        <button
            :disabled="submitting"
            class="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:pointer-events-none transition-colors"
            type="submit"
        >
          {{ submitting ? 'Creating...' : 'Create Schema' }}
        </button>
        <button
            class="inline-flex items-center rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
            type="button"
            @click="cancel"
        >
          Cancel
        </button>
      </div>
    </form>
  </div>
</template>
