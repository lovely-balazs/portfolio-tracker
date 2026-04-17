// sql.js requires Node.js modules that don't exist in the browser.
// Tell webpack to ignore them since the browser build of sql.js doesn't actually use them.
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign(config.resolve.fallback || {}, {
    fs: false,
    path: false,
    crypto: false,
});
