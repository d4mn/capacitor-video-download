var capacitorCapacitorVideoDownload = (function (exports, core) {
    'use strict';

    const CapacitorVideoDownload = core.registerPlugin('CapacitorVideoDownload', {
        web: () => Promise.resolve().then(function () { return web; }).then(m => new m.CapacitorVideoDownloadWeb()),
    });

    class CapacitorVideoDownloadWeb extends core.WebPlugin {
        async saveVideo(options) {
            console.log('saveVideo', options);
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

    Object.defineProperty(exports, '__esModule', { value: true });

    return exports;

})({}, capacitorExports);
//# sourceMappingURL=plugin.js.map
