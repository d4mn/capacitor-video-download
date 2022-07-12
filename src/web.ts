import { WebPlugin } from '@capacitor/core';

import type { CapacitorVideoDownloadPlugin } from './definitions';

export class CapacitorVideoDownloadWeb extends WebPlugin implements CapacitorVideoDownloadPlugin {
  async saveVideo(options: { path: string; album: string; extension: string; }): Promise<{ value: string; }> {
    if(options.path) {
      return {value:"ok"}
    }
      return {value:"ok"}
  }

  async cancel(): Promise<{ value: string; }> {
    return {value:"ok"}
  }
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
