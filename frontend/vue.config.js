const { defineConfig } = require('@vue/cli-service')
const path = require('path')

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    host: '0.0.0.0',
    port: 3000,
    hot: true,
    liveReload: true,
    // Allow connections from any host (needed for Docker)
    allowedHosts: 'all',
    // Enable client overlay for errors
    client: {
      overlay: {
        errors: true,
        warnings: false,
      },
    },
    // Use watchFiles instead of watchOptions for newer webpack-dev-server
    watchFiles: {
      paths: ['src/**/*', 'public/**/*'],
      options: {
        usePolling: true,
        interval: 1000,
      },
    },
  },
  // Configure webpack for better file watching
  chainWebpack: config => {
    // Fix for y-protocols subpath exports issue with webpack 4
    // Webpack 4 doesn't support package.json "exports" field. Make imports
    // like `y-protocols/awareness` resolve to a local re-export shim so that
    // node_modules packages (e.g. y-prosemirror) can import the protocol.
    // We point them to `src/y-protocols-fix.js` which re-exports the real .js
    // entrypoints (awareness.js / sync.js).
    config.resolve.alias
      .set('y-protocols', path.resolve(__dirname, 'src/y-protocols-fix.js'))
      .set('y-protocols/awareness', path.resolve(__dirname, 'src/y-protocols-fix.js'))
      .set('y-protocols/awareness.js', path.resolve(__dirname, 'src/y-protocols-fix.js'))
      .set('y-protocols/sync', path.resolve(__dirname, 'src/y-protocols-fix.js'))
      .set('y-protocols/sync.js', path.resolve(__dirname, 'src/y-protocols-fix.js'))

    // As an additional fallback, replace any import that starts with
    // 'y-protocols' to our shim. NormalModuleReplacementPlugin is more
    // aggressive and will catch imports inside node_modules where aliases
    // sometimes don't apply.
    const webpack = require('webpack')
    config.plugin('y-protocols-replace')
      .use(webpack.NormalModuleReplacementPlugin, [/^y-protocols(\/.*)?$/, resource => {
        resource.request = path.resolve(__dirname, 'src/y-protocols-fix.js')
      }])
  },
  configureWebpack: {
    watchOptions: {
      poll: 1000,
      aggregateTimeout: 300,
      ignored: /node_modules/,
    },
    module: {
      rules: [
        {
          test: /\.m?js$/,
          resolve: {
            fullySpecified: false,
          },
        },
      ],
    },
  },
})