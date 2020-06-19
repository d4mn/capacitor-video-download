import { WebPlugin } from '@capacitor/core';
import { MediaPluginPlugin } from './definitions';

export class MediaPluginWeb extends WebPlugin implements MediaPluginPlugin {
  constructor() {
    super({
      name: 'MediaPlugin',
      platforms: ['web']
    });
  }

  async echo(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', options);
    return options;
  }
}

const MediaPlugin = new MediaPluginWeb();

export { MediaPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(MediaPlugin);
