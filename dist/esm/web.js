import { WebPlugin } from '@capacitor/core';
export class CapacitorVideoDownloadWeb extends WebPlugin {
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
//# sourceMappingURL=web.js.map