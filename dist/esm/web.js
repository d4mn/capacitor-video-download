import { WebPlugin } from '@capacitor/core';
export class CapacitorVideoDownloadWeb extends WebPlugin {
    async saveVideo(options) {
        console.log('saveVideo', options);
        return { value: "ok" };
    }
    async echo(options) {
        console.log('ECHO', options);
        return options;
    }
}
//# sourceMappingURL=web.js.map