export interface CapacitorVideoDownloadPlugin {
  saveVideo(options: { path: string, album: string, extension: string }): Promise<{ value: string }>;
  cancel(): Promise<{ value: string }>;
}
