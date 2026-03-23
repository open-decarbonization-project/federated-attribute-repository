import {createApp} from 'vue'
import App from './App.vue'
import {router} from './router'
import {initialize} from './lib/auth'
import {initialize as initTheme} from './lib/theme'
import './style.css'

initTheme()

initialize().then(() => {
    const app = createApp(App)
    app.use(router)
    app.mount('#app')
})
