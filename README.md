# capacitor-video-download

Download video from web to phone

## Install

```bash
npm install capacitor-video-download
npx cap sync
```

## API

<docgen-index>

* [`saveVideo(...)`](#savevideo)
* [`cancel()`](#cancel)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### saveVideo(...)

```typescript
saveVideo(options: { path: string; album: string; extension: string; }) => Promise<{ value: string; }>
```

| Param         | Type                                                             |
| ------------- | ---------------------------------------------------------------- |
| **`options`** | <code>{ path: string; album: string; extension: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### cancel()

```typescript
cancel() => Promise<{ value: string; }>
```

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------

</docgen-api>
