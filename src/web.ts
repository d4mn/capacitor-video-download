import { WebPlugin } from '@capacitor/core';

import type { CapacitorVideoDownloadPlugin } from './definitions';

export class CapacitorVideoDownloadWeb extends WebPlugin implements CapacitorVideoDownloadPlugin {
  async saveVideo(options: { path: string; album: string; extension: string; }): Promise<{ value: string; }> {
      console.log('saveVideo', options);
      return {value:"ok"}
  }
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
