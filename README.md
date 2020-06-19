# capacitor-video-download [![npm version](https://badge.fury.io/js/capacitor-video-download.svg)](https://badge.fury.io/js/capacitor-video-download)

Capacitor plugin to activate media features such as saving videos from remote url into user's photo gallery

## API

- savePhoto
- saveVideo
- createAlbum
- getAlbums
- getMedias `only ios for now`

## Usage

```js
import { Media } from 'capacitor-media';
const media = new Media();

//
// Save video to a specfic album
media
  .saveVideo({ path: 'https://cdn.video-server/path/to/video/file.mp4', album: 'My Album', ext: '.mp4' })
  .then(console.log)
  .catch(console.log);

//
// Get a list of user albums
media
  .getAlbums()
  .then(console.log) // -> { albums: [{name:'My Album', identifier:'A1-B2-C3-D4'}, {name:'My Another Album', identifier:'E5-F6-G7-H8'}]}
  .catch(console.log);
```

## Disclaimer

Make sure you pass the correct album parameter according to the platform

```js
album: this.platform.is('ios') ? album.identifier : album.name;
```

## iOS setup

- `ionic start my-cap-app --capacitor`
- `cd my-cap-app`
- `npm install —-save capacitor-video-download`
- `mkdir www && touch www/index.html`
- `npx cap add ios`
- `npx cap open ios`
- sign your app at xcode (general tab)

> Tip: every time you change a native code you may need to clean up the cache (Product > Clean build folder) and then run the app again.

## Android setup

- `ionic start my-cap-app --capacitor`
- `cd my-cap-app`
- `npm install —-save capacitor-video-download`
- `mkdir www && touch www/index.html`
- `npx cap add android`
- `npx cap open android`
- `[extra step]` in android case we need to tell Capacitor to initialise the plugin:

> on your `MainActivity.java` file add `import io.d4mn.capacitor.video.download.MediaPlugin;` and then inside the init callback `add(MediaPlugin.class);`

Now you should be set to go. Try to run your client using `ionic cap run android --livereload`.


## License

MIT
