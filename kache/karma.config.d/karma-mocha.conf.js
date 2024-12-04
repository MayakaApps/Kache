module.exports = function(config) {
    config.set({
        client: {
            mocha: {
                timeout: 10000
            }
        }
    });
}
