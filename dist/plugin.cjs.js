'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var core = require('@capacitor/core');

const CapacitorVideoDownload = core.registerPlugin('CapacitorVideoDownload', {
    web: () => Promise.resolve().then(function () { return web; }).then(m => new m.CapacitorVideoDownloadWeb()),
});

class CapacitorVideoDownloadWeb extends core.WebPlugin {
    async saveVideo(options) {
        if (options.path) {
            return { value: "ok" };
        }
        return { value: "ok" };
    }
    async cancel() {
        return { value: "ok" };
    }
    async echo(options) {
        console.log('ECHO', options);
        return options;
    }
}

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    CapacitorVideoDownloadWeb: CapacitorVideoDownloadWeb
});

exports.CapacitorVideoDownload = CapacitorVideoDownload;
//# sourceMappingURL=plugin.cjs.js.map
