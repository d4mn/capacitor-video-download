package com.igliverec.capacitor.video.download;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@CapacitorPlugin(
    name = "CapacitorVideoDownload",
    permissions = {
        @Permission(
            alias = "storage",
            strings = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }
        )
    }
)
public class CapacitorVideoDownloadPlugin extends Plugin {

    public static String tag = "Capacitor/MediaPlugin";

    public static final int REQUEST_WRITE = 1986;
    AsyncTask<?, ?, ?> downloadTask;

    // @todo
    @PluginMethod()
    public void getMedias(PluginCall call) {
        call.unimplemented();
    }

    @PluginMethod()
    public void getAlbums(PluginCall call) { call.unimplemented(); }

    @PluginMethod()
    public void getPhotos(PluginCall call) { call.unimplemented(); }

    @PluginMethod()
    public void createAlbum(PluginCall call) {
        Log.d(CapacitorVideoDownloadPlugin.tag, "CREATE ALBUM");
        if (hasRequiredPermissions()) {
            Log.d(CapacitorVideoDownloadPlugin.tag, "HAS PERMISSIONS");
            _createAlbum(call);
        } else {
            Log.d(CapacitorVideoDownloadPlugin.tag, "NOT ALLOWED");
            requestAllPermissions(call, "storagePermsCallback");
        }
    }

    @PermissionCallback
    private void storagePermsCallback(PluginCall call) {
        Log.d(CapacitorVideoDownloadPlugin.tag, "PluginCall: "+ call.getMethodName());
        if(call.getMethodName() == "saveVideo") _saveMedia(call, "MOVIES");
        else _saveMedia(call, "MOVIES");
    }

    private void _createAlbum(PluginCall call) {
        Log.d(CapacitorVideoDownloadPlugin.tag, "___CREATE ALBUM");
        String folderName = call.getString("name");
        String folder = Environment.getExternalStorageDirectory() + "/" + folderName;

        File f = new File(folder);

        if (!f.exists()) {
            if (!f.mkdir()) {
                Log.d(CapacitorVideoDownloadPlugin.tag, "___ERROR ALBUM");
                call.error("Cant create album");
            } else {
                Log.d(CapacitorVideoDownloadPlugin.tag, "___SUCCESS ALBUM CREATED");
                call.success();
            }
        } else {
            Log.d(CapacitorVideoDownloadPlugin.tag, "___ERROR ALBUM ALREADY EXISTS");
            call.success();
        }

    }

    @PluginMethod()
    public void saveVideo(final PluginCall call) {
        Log.d(getLogTag(), "SAVE VIDEO to album");
        if (hasRequiredPermissions()) {
            Log.d(getLogTag(), "HAS PERMISSIONS");
            _saveMedia(call, "MOVIES");
        } else {
            Log.d(getLogTag(), "Dont have permissions. Save call and ask for permissions.");
            requestAllPermissions(call, "storagePermsCallback");
        }
    }

    @PluginMethod()
    public void cancel(PluginCall call) {
        if (!downloadTask.isCancelled()) {
            downloadTask.cancel(false);
            call.success();
        } else {
            call.error("Download task is not running");
        }
    }

    private String _getPathExtension(String path) {
        int extensionIndex = path.lastIndexOf(".");
        return extensionIndex == -1 ? "" : "." + path.substring(extensionIndex);
    }

    private void _saveMedia(final PluginCall call, String destination) {
        String album = call.getString("album");
        final String extension = call.getString("extension");

        Log.d(CapacitorVideoDownloadPlugin.tag, "___SAVE MEDIA TO ALBUM " + album);

        final String inputPath = call.getString("path");
        if (inputPath == null || (!inputPath.startsWith("http") && !inputPath.startsWith("https"))) {
            call.reject("Input file path is required");
            return;
        }

        final JSObject result = new JSObject();
        try {
            downloadTask = new DownloadMedia(this.getContext()) {
                @Override
                public void onPostExecute(AsyncTaskResult<File> expFile) {
                    if (expFile.getError() != null) {
                        call.reject(expFile.getError().getMessage());
                    } else if (isCancelled()) {
                        // cancel handling here
                    } else {
                        File downloadedFile = expFile.getResult();
                        scanPhoto(downloadedFile);
                        result.put("filePath", expFile.toString());
                        call.resolve(result);
                    }
                }

                @Override
                protected void onCancelled(AsyncTaskResult<File> fileAsyncTaskResult) {
                    super.onCancelled(fileAsyncTaskResult);
                    Log.d(CapacitorVideoDownloadPlugin.tag, "Canceled download task " + fileAsyncTaskResult.getResult());
                }
            }.execute(inputPath, album, extension);
        } catch (RuntimeException e) {
            call.reject("RuntimeException occurred", e);
        }

    }

    private File copyFile(File inputFile, File albumDir, String extension) {


        // generate image file name using current date and time
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        File newFile = new File(albumDir, "IMG_" + timeStamp + extension);

        // Read and write image files
        FileChannel inChannel = null;
        FileChannel outChannel = null;

        try {
            inChannel = new FileInputStream(inputFile).getChannel();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Source file not found: " + inputFile + ", error: " + e.getMessage());
        }
        try {
            outChannel = new FileOutputStream(newFile).getChannel();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Copy file not found: " + newFile + ", error: " + e.getMessage());
        }

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw new RuntimeException("Error transfering file, error: " + e.getMessage());
        } finally {
            if (inChannel != null) {
                try {
                    inChannel.close();
                } catch (IOException e) {
                    Log.d("SaveImage", "Error closing input file channel: " + e.getMessage());
                    // does not harm, do nothing
                }
            }
            if (outChannel != null) {
                try {
                    outChannel.close();
                } catch (IOException e) {
                    Log.d("SaveImage", "Error closing output file channel: " + e.getMessage());
                    // does not harm, do nothing
                }
            }
        }

        return newFile;
    }

    private void scanPhoto(File imageFile) {
        Log.i("MyMediaScan", "Scanned folder: " + imageFile.getParent());
        MediaScannerConnection.scanFile(this.getContext(),
                new String[]{imageFile.getParent()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned file" + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(imageFile);
        mediaScanIntent.setData(contentUri);
        this.getContext().sendBroadcast(mediaScanIntent);
    }

    public class AsyncTaskResult<T> {
        private T result;
        private Exception error;

        public T getResult() {
            return result;
        }

        public Exception getError() {
            return error;
        }

        public AsyncTaskResult(T result) {
            super();
            this.result = result;
        }

        public AsyncTaskResult(Exception error) {
            super();
            this.error = error;
        }
    }

    private class DownloadMedia extends AsyncTask<String, File, AsyncTaskResult<File>> {

        private WeakReference<Context> contextRef;

        public DownloadMedia(Context context) {
            contextRef = new WeakReference<>(context);
        }

        @Override
        protected AsyncTaskResult doInBackground(String... strings) {
            Log.d(CapacitorVideoDownloadPlugin.tag, "Starting download of media: " + strings[0]);
            try {
                JSObject ret = new JSObject();
                URL remoteURL = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) remoteURL.openConnection();
                connection.setDoInput(true);
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(CapacitorVideoDownloadPlugin.tag, "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
                    return new AsyncTaskResult(new RuntimeException("File not found."));
                }
                int fileLength = connection.getContentLength();

                InputStream inputStream = new BufferedInputStream(connection.getInputStream());

                OutputStream outputStream;
                ContentResolver resolver;
                Uri videoUri;
                File videoFile;
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
                String name = "VID_" + timeStamp + strings[2];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Context context = contextRef.get();
                    resolver = context.getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/" + strings[1]);
                    videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
                    outputStream = resolver.openOutputStream(Objects.requireNonNull(videoUri));
                    videoFile = new File(videoUri.toString());

                } else {
                    String moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES + "/" + strings[1]).toString();
                    File albumDir = new File(moviesDir);
                    // if destination folder does not exist, create it
                    if (!albumDir.exists()) {
                        if (!albumDir.mkdir()) {
                            throw new RuntimeException("Destination folder does not exist and cannot be created.");
                        }
                    }
                    videoFile = new File(moviesDir, name);
                    outputStream = new BufferedOutputStream(new FileOutputStream(videoFile));
                }

                ret.put("started", true);
                notifyListeners("status", ret);
                ret.remove("started");

                byte[] buffer = new byte[1024 * 50];
                int read;
                long total = 0;
                long progress = 0;

                ret.put("size", fileLength);
                ret.put("total", total);
                ret.put("progress", progress);
                ret.put("finished", false);
                notifyListeners("progress", ret);
                while ((read = inputStream.read(buffer)) != -1) {
                    if (isCancelled()) {
                        inputStream.close();
                        outputStream.flush();
                        outputStream.close();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Context context = contextRef.get();
                            resolver = context.getContentResolver();
                            resolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DISPLAY_NAME + "=?", new String[]{name});
                        } else {
                            videoFile.delete();
                        }
                        Log.i("CancelDownload", videoFile.getAbsolutePath());
                        return new AsyncTaskResult<>(videoFile);
                    }
                    total += read;
                    if (fileLength > 0) {
                        long prog = (int) (total * 100 / fileLength);
                        if (prog > progress) {
                            progress = prog;
                            ret.put("total", total);
                            ret.put("progress", progress);
                            Log.d(CapacitorVideoDownloadPlugin.tag, "Progress:: " + ret.toString());
                            notifyListeners("progress", ret);
                        }
                    }
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();
                ret.put("finished", true);
                notifyListeners("progress", ret);
                return new AsyncTaskResult<>(videoFile);
            } catch (MalformedURLException e) {
                return new AsyncTaskResult<>(new RuntimeException("Bad url"));
            } catch (IOException e) {
                return new AsyncTaskResult<>(e);
            }
        }
    }
}
