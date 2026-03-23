import {createRouter, createWebHistory} from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import CertificateList from '@/views/CertificateList.vue'
import CertificateCreate from '@/views/CertificateCreate.vue'
import CertificateDetail from '@/views/CertificateDetail.vue'
import SchemaList from '@/views/SchemaList.vue'
import SchemaCreate from '@/views/SchemaCreate.vue'
import SchemaDetail from '@/views/SchemaDetail.vue'
import SearchView from '@/views/SearchView.vue'
import ResourceView from '@/views/ResourceView.vue'
import SettingsView from '@/views/SettingsView.vue'
import {authenticated, login, redirectPath} from '@/lib/auth'

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'dashboard',
            component: Dashboard,
        },
        {
            path: '/search',
            name: 'search',
            component: SearchView,
        },
        {
            path: '/resources/:urn(.*)',
            name: 'resource',
            component: ResourceView,
        },
        {
            path: '/settings',
            name: 'settings',
            component: SettingsView,
        },
        {
            path: '/certificates',
            name: 'certificates',
            component: CertificateList,
        },
        {
            path: '/certificates/create',
            name: 'create',
            component: CertificateCreate,
        },
        {
            path: '/certificates/:urn(.*)',
            name: 'detail',
            component: CertificateDetail,
        },
        {
            path: '/schemas',
            name: 'schemas',
            component: SchemaList,
        },
        {
            path: '/schemas/create',
            name: 'schema-create',
            component: SchemaCreate,
        },
        {
            path: '/schemas/:id',
            name: 'schema-detail',
            component: SchemaDetail,
        },
        {
            path: '/callback',
            name: 'callback',
            component: {template: '<div>Signing in...</div>'},
        },
    ],
})

router.beforeEach((to) => {
    if (to.name === 'callback') {
        if (redirectPath) {
            return redirectPath
        }
        return '/'
    }
    if (!authenticated.value) {
        login(to.fullPath)
        return false
    }
    return true
})
