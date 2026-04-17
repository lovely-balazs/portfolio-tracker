// sql.js requires Node.js modules that don't exist in the browser.
// Tell webpack to ignore them since the browser build doesn't use them.
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign(config.resolve.fallback || {}, {
    fs: false,
    path: false,
    crypto: false,
});

// Copy sql-wasm.wasm into webpack output so sql.js can load it at runtime.
const CopyPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyPlugin({
        patterns: [
            {
                from: require.resolve('sql.js/dist/sql-wasm.wasm'),
                to: '.',
            },
        ],
    })
);
