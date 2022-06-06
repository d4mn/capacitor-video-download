import { registerPlugin } from '@capacitor/core';
const CapacitorVideoDownload = registerPlugin('CapacitorVideoDownload', {
    web: () => import('./web').then(m => new m.CapacitorVideoDownloadWeb()),
});
export * from './definitions';
export { CapacitorVideoDownload };
//# sourceMappingURL=index.js.map