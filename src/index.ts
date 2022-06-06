import { registerPlugin } from '@capacitor/core';

import type { CapacitorVideoDownloadPlugin } from './definitions';

const CapacitorVideoDownload = registerPlugin<CapacitorVideoDownloadPlugin>('CapacitorVideoDownload', {
  web: () => import('./web').then(m => new m.CapacitorVideoDownloadWeb()),
});

export * from './definitions';
export { CapacitorVideoDownload };
