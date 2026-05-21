import { configure } from 'quasar/wrappers'

export default configure(function (/* ctx */) {
  return {
    boot: ['axios'],

    css: ['app.scss'],

    extras: ['material-icons'],

    build: {
      vueRouterMode: 'hash'
    },

    devServer: {
      proxy: {
        '/v1': {
          target: 'http://localhost:8081',
          changeOrigin: true
        }
      }
    },

    framework: {
      config: {},
      plugins: ['Notify']
    },

    animations: []
  }
})