import { WebPlugin } from '@capacitor/core';
import { MediaPluginPlugin } from './definitions';
export declare class MediaPluginWeb extends WebPlugin implements MediaPluginPlugin {
    constructor();
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
}
declare const MediaPlugin: MediaPluginWeb;
export { MediaPlugin };
