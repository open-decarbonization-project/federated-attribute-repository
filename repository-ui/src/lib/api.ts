import {login, token} from './auth'

export interface Attribute {
    value: unknown
    source?: string
    verified?: boolean
    timestamp?: string
}

export function display(value: unknown): string {
    if (value == null) return '--'
    if (typeof value === 'object' && 'amount' in (value as Record<string, unknown>)) {
        const quantity = value as { amount: number; unit?: string }
        const formatted = quantity.amount.toLocaleString()
        return quantity.unit ? `${formatted} ${quantity.unit}` : formatted
    }
    if (typeof value === 'boolean') return value ? 'Yes' : 'No'
    return String(value)
}

export interface Certificate {
    urn: string
    namespace: string
    identifier: string
    attributes: Record<string, Attribute>
    status: 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'RETIRED'
    integrity?: {
        algorithm: string
        digest: string
    }
    owner?: string
    schema?: string
    pin?: number
    created: string
    modified: string
}

export type TypeName = 'string' | 'numeric' | 'boolean' | 'datetime' | 'quantity' | 'list' | 'record'

export interface FieldType {
    name: TypeName
    unit?: string
    element?: FieldType
    fields?: Field[]
}

export type Policy =
    | { kind: 'public' }
    | { kind: 'masked' }
    | { kind: 'credential'; role: string }

export interface Field {
    name: string
    label: string
    description: string
    type: FieldType
    required: boolean
    position: number
}

export interface FieldPolicy {
    field: string
    policy: Policy
}

export interface Schema {
    id: string
    namespace: string
    name: string
    description: string
    version: number
    fields: Field[]
    active: boolean
    owner: string
    created: string
    modified: string
}

export interface SchemaDraft {
    namespace: string
    name: string
    description: string
    fields: Field[]
}

export interface SchemaRevision {
    description: string
    fields: Field[]
    active: boolean
}

export interface Submission {
    namespace: string
    identifier: string
    attributes: Record<string, { value: string; source?: string }>
    owner?: string
    schema?: string
    pin?: number
}

export interface Amendment {
    attributes: Record<string, Attribute>
    status?: 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'RETIRED'
}

export interface Document {
    id: string
    filename: string
    media: string
    size: number
    digest: string
    signature?: string
    uploaded: string
}

export interface Entry {
    id: string
    certificate: string
    ledger: string
    hash: string
    proof?: string
    published: string
}

export interface Page<T> {
    value: T[]
    count: number
}

export interface Health {
    status: string
    timestamp: string
}

function headers(extra?: Record<string, string>): Record<string, string> {
    const result: Record<string, string> = {...extra}
    const bearer = token()
    if (bearer) {
        result['Authorization'] = `Bearer ${bearer}`
    }
    return result
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
    const merged: RequestInit = {
        ...options,
        headers: headers(options?.headers as Record<string, string>),
    }
    const response = await fetch(path, merged)
    if (response.status === 401) {
        await login(window.location.pathname)
        throw new Error('Unauthorized')
    }
    if (!response.ok) {
        const body = await response.text()
        throw new Error(body || response.statusText)
    }
    if (response.status === 204 || response.headers.get('content-length') === '0') {
        return undefined as T
    }
    return response.json() as Promise<T>
}

export async function list(top = 25, skip = 0): Promise<Page<Certificate>> {
    return request<Page<Certificate>>(`/v1/certificates?$top=${top}&$skip=${skip}`)
}

export async function find(urn: string): Promise<Certificate> {
    return request<Certificate>(`/v1/certificates/${urn}`)
}

export async function create(submission: Submission): Promise<Certificate> {
    return request<Certificate>('/v1/certificates', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(submission),
    })
}

export async function update(urn: string, amendment: Amendment): Promise<Certificate> {
    return request<Certificate>(`/v1/certificates/${urn}`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(amendment),
    })
}

export async function retire(urn: string): Promise<Certificate> {
    return request<Certificate>(`/v1/certificates/${urn}`, {
        method: 'DELETE',
    })
}

export async function documents(urn?: string): Promise<Document[]> {
    if (urn) {
        return request<Document[]>(`/v1/certificates/${urn}/documents`)
    }
    return request<Document[]>('/v1/documents')
}

export async function attach(urn: string, document: string): Promise<void> {
    await request<void>(`/v1/certificates/${urn}/documents/${document}`, {
        method: 'POST',
    })
}

export async function detach(urn: string, document: string): Promise<void> {
    await request<void>(`/v1/certificates/${urn}/documents/${document}`, {
        method: 'DELETE',
    })
}

export async function upload(file: File): Promise<Document> {
    const data = new FormData()
    data.append('filename', file.name)
    data.append('media', file.type || 'application/octet-stream')
    data.append('content', file)
    const bearer = token()
    const opts: RequestInit = {method: 'POST', body: data}
    if (bearer) {
        opts.headers = {'Authorization': `Bearer ${bearer}`}
    }
    const response = await fetch('/v1/documents', opts)
    if (!response.ok) {
        const body = await response.text()
        throw new Error(body || response.statusText)
    }
    return response.json() as Promise<Document>
}

export async function publish(urn: string): Promise<Entry[]> {
    return request<Entry[]>(`/v1/certificates/${urn}/entries`, {
        method: 'POST',
    })
}

export async function entries(urn: string): Promise<Entry[]> {
    return request<Entry[]>(`/v1/certificates/${urn}/entries`)
}

export async function health(): Promise<Health> {
    return request<Health>('/v1/health')
}

export async function schemas(namespace?: string): Promise<Schema[]> {
    const query = namespace ? `?namespace=${encodeURIComponent(namespace)}` : ''
    return request<Schema[]>(`/v1/schemas${query}`)
}

export async function schema(id: string): Promise<Schema> {
    return request<Schema>(`/v1/schemas/${id}`)
}

export async function createSchema(draft: SchemaDraft): Promise<Schema> {
    return request<Schema>('/v1/schemas', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(draft),
    })
}

export async function updateSchema(id: string, revision: SchemaRevision): Promise<Schema> {
    return request<Schema>(`/v1/schemas/${id}`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(revision),
    })
}

export async function versions(id: string): Promise<Schema[]> {
    return request<Schema[]>(`/v1/schemas/${id}/versions`)
}

export async function version(id: string, v: number): Promise<Schema> {
    return request<Schema>(`/v1/schemas/${id}/versions/${v}`)
}

export async function policies(id: string): Promise<FieldPolicy[]> {
    return request<FieldPolicy[]>(`/v1/schemas/${id}/policies`)
}

export async function updatePolicies(id: string, items: FieldPolicy[]): Promise<FieldPolicy[]> {
    return request<FieldPolicy[]>(`/v1/schemas/${id}/policies`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(items),
    })
}

// --- FAR Protocol types ---

export interface Resolution {
    urn: string
    namespace: string
    identifier: string
    attributes: Record<string, Attribute>
    integrity?: { algorithm: string; digest: string }
    resolver: string
    timestamp: string
}

export interface FarPage {
    value: Resolution[]
    count: number
    skip: number
    top: number
}

export interface PeerInfo {
    identity: string
    endpoint: string
    namespaces: string[]
    last_seen?: string
}

export interface NodeConfig {
    identity: string
    namespaces: string[]
    version: string
    key: string
}

// --- FAR Protocol functions ---

export async function search(query?: string, top = 25, skip = 0): Promise<FarPage> {
    const params = new URLSearchParams()
    if (query) params.set('$filter', query)
    params.set('$top', String(top))
    params.set('$skip', String(skip))
    return request<FarPage>(`/v1/resources?${params}`)
}

export async function resolve(urn: string): Promise<Resolution> {
    return request<Resolution>(`/v1/resources/${urn}`)
}

export async function peers(): Promise<PeerInfo[]> {
    const response = await request<{ peers: PeerInfo[] }>('/v1/peers')
    return response.peers
}

export async function configuration(): Promise<NodeConfig> {
    return request<NodeConfig>('/v1/peers/configuration')
}

export async function addPeer(endpoint: string): Promise<PeerInfo> {
    return request<PeerInfo>('/v1/peers', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({endpoint}),
    })
}

export async function removePeer(identity: string): Promise<void> {
    await request<void>(`/v1/peers/${encodeURIComponent(identity)}`, {
        method: 'DELETE',
    })
}

export async function resourceDocuments(urn: string): Promise<Document[]> {
    return request<Document[]>(`/v1/resources/${urn}/documents`)
}

export function resourceDocumentUrl(urn: string, id: string): string {
    return `/v1/resources/${urn}/documents/${id}`
}
