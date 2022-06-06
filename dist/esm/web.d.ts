import { WebPlugin } from '@capacitor/core';
import type { CapacitorVideoDownloadPlugin } from './definitions';
export declare class CapacitorVideoDownloadWeb extends WebPlugin implements CapacitorVideoDownloadPlugin {
    saveVideo(options: {
        path: string;
        album: string;
        extension: string;
    }): Promise<{
        value: string;
    }>;
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
}
