declare module "@capacitor/core" {
  interface PluginRegistry {
    MediaPlugin: MediaPluginPlugin;
  }
}

export interface MediaPluginPlugin {
  echo(options: { value: string }): Promise<{value: string}>;
}
