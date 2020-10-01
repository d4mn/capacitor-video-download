package io.d4mn.capacitor.video.download;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.provider.MediaStore;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;


@NativePlugin(permissions = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
},
        requestCodes = {
                MediaPlugin.REQUEST_WRITE
        })
public class MediaPlugin extends Plugin {
    public static String tag = "Capacitor/MediaPlugin";

    public static final int REQUEST_WRITE = 1986;
    AsyncTask<?, ?, ?> downloadTask;

    // @todo
    @PluginMethod()
    public void getMedias(PluginCall call) {
        call.unimplemented();
    }

    @PluginMethod()
    public void getAlbums(PluginCall call) {
        Log.d(MediaPlugin.tag, "GET ALBUMS");
        if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Log.d(MediaPlugin.tag, "HAS PERMISSIONS");
            _getAlbums(call);
        } else {
            Log.d(MediaPlugin.tag, "NOT ALLOWED");
            saveCall(call);
            pluginRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 1986);
        }
    }

    private void _getAlbums(PluginCall call) {
        Log.d(MediaPlugin.tag, "___GET ALBUMS");

        JSObject response = new JSObject();
        JSArray albums = new JSArray();
        StringBuffer list = new StringBuffer();

        String[] projection = new String[]{"DISTINCT " + MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME};
        Cursor cur = getContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);

        while (cur.moveToNext()) {
            String albumName = cur.getString((cur.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)));
            JSObject album = new JSObject();

            list.append(albumName + "\n");

            album.put("name", albumName);
            albums.put(album);
        }

        response.put("albums", albums);
        Log.d(MediaPlugin.tag, String.valueOf(response));
        Log.d(MediaPlugin.tag, "___GET ALBUMS FINISHED");

        call.resolve(response);
    }


    @PluginMethod()
    public void getPhotos(PluginCall call) {
        call.unimplemented();
    }

    @PluginMethod()
    public void createAlbum(PluginCall call) {
        Log.d(MediaPlugin.tag, "CREATE ALBUM");
        if (hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.d(MediaPlugin.tag, "HAS PERMISSIONS");
            _createAlbum(call);
        } else {
            Log.d(MediaPlugin.tag, "NOT ALLOWED");
            saveCall(call);
            pluginRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1986);
        }
    }

    private void _createAlbum(PluginCall call) {
        Log.d(MediaPlugin.tag, "___CREATE ALBUM");
        String folderName = call.getString("name");
        String folder = Environment.getExternalStorageDirectory() + "/" + folderName;

        File f = new File(folder);

        if (!f.exists()) {
            if (!f.mkdir()) {
                Log.d(MediaPlugin.tag, "___ERROR ALBUM");
                call.error("Cant create album");
            } else {
                Log.d(MediaPlugin.tag, "___SUCCESS ALBUM CREATED");
                call.success();
            }
        } else {
            Log.d(MediaPlugin.tag, "___ERROR ALBUM ALREADY EXISTS");
            call.error("Album already exists");
        }

    }


    @PluginMethod()
    public void savePhoto(PluginCall call) {
        Log.d(MediaPlugin.tag, "SAVE VIDEO TO ALBUM");
        if (hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.d(MediaPlugin.tag, "HAS PERMISSIONS");
            _saveMedia(call, "PICTURES");
        } else {
            Log.d(MediaPlugin.tag, "NOT ALLOWED");
            saveCall(call);
            pluginRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1986);
        }
    }

    @PluginMethod()
    public void saveVideo(final PluginCall call) {
        Log.d(getLogTag(), "SAVE VIDEO to album");
        if (hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.d(getLogTag(), "HAS PERMISSIONS");
            _saveMedia(call, "MOVIES");
        } else {
            Log.d(getLogTag(), "Dont have permissions. Save call and ask for permissions.");
            saveCall(call);
            pluginRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1986);
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
        String dest;
        if (destination == "MOVIES") {
            dest = Environment.DIRECTORY_MOVIES;
        } else {
            dest = Environment.DIRECTORY_PICTURES;
        }

        String album = call.getString("album");
        final String extension = call.getString("extension");
        File mainDir;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            mainDir = Environment.getExternalStoragePublicDirectory(dest);
        } else {
            mainDir = this.getContext().getExternalFilesDir(dest);
        }
        File albumDir = mainDir;
        if (album != null) {
            albumDir = new File(mainDir, album);
            // if destination folder does not exist, create it
            if (!albumDir.exists()) {
                if (!albumDir.mkdir()) {
                    //throw new RuntimeException("Destination folder does not exist and cannot be created.");
                    albumDir = mainDir;
                }
            }
        }

        Log.d(MediaPlugin.tag, "___SAVE MEDIA TO ALBUM " + albumDir.getPath());

        final String inputPath = call.getString("path");
        if (inputPath == null) {
            call.reject("Input file path is required");
            return;
        }

        final JSObject result = new JSObject();

        if (inputPath.startsWith("http") || inputPath.startsWith("https")) {
            try {
                final File finalAlbumDir = albumDir;
                downloadTask = new DownloadMedia() {
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
                        Log.d(MediaPlugin.tag,"Canceled download task "+fileAsyncTaskResult.getResult());
                        fileAsyncTaskResult.getResult().delete();
                    }
                }.execute(inputPath, albumDir.getPath(), extension);
            } catch (RuntimeException e) {
                call.reject("RuntimeException occurred", e);
            }

        } else {
            String fileLocalPath;
            fileLocalPath = Uri.parse(inputPath).getPath();
            if (fileLocalPath == "") {
                call.reject("File save failed");
                return;
            }
            File inputFile = new File(fileLocalPath);
            try {
                final File expFile;
                expFile = copyFile(inputFile, albumDir, extension);
                scanPhoto(expFile);
                result.put("filePath", expFile.toString());
                call.resolve(result);
            } catch (RuntimeException e) {
                call.reject("RuntimeException occurred", e);
            }
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
        MediaScannerConnection.scanFile(this.getContext(),
                new String[] { imageFile.getPath() }, new String[] { "video/mp4" },
                new MediaScannerConnection.OnScanCompletedListener() {
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


    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(getLogTag(), "handleRequestPermissionsResult function");
        if (getSavedCall() == null) {
            Log.d(getLogTag(), "No stored plugin call for permissions request result");
            return;
        }
        PluginCall savedCall = getSavedCall();
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Log.d(getLogTag(), "Permission not granted by the user");
                savedCall.error("Permission denied");
                this.freeSavedCall();
                return;
            }
        }

        if (requestCode == 1986) {
            this.saveVideo(savedCall);
        }
        this.freeSavedCall();
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

        @Override
        protected AsyncTaskResult doInBackground(String... strings) {
            Log.d(MediaPlugin.tag, "Starting download of media: " + strings[0]);
            try {
                JSObject ret = new JSObject();
                URL remoteURL = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) remoteURL.openConnection();
                connection.setDoInput(true);
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(MediaPlugin.tag, "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
                    return new AsyncTaskResult(new RuntimeException("File not found."));
                }
                int fileLength = connection.getContentLength();

                InputStream inputStream = new BufferedInputStream(connection.getInputStream());

                // generate image file name using current date and time
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
                File newFile = new File(strings[1], "VID_" + timeStamp + strings[2]);

                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(newFile));

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
                    if(isCancelled()) {
                        inputStream.close();
                        outputStream.flush();
                        outputStream.close();
                        newFile.delete();
                        return new AsyncTaskResult<>(newFile);
                    }
                    total += read;
                    if (fileLength > 0) {
                        long prog = (int) (total * 100 / fileLength);
                        if (prog > progress) {
                            progress = prog;
                            ret.put("total", total);
                            ret.put("progress", progress);
                            Log.d(MediaPlugin.tag, "Progress:: " + ret.toString());
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
                return new AsyncTaskResult<>(newFile);
            } catch (MalformedURLException e) {
                return new AsyncTaskResult<>(new RuntimeException("Bad url"));
            } catch (IOException e) {
                return new AsyncTaskResult<>(e);
            }
        }
    }
}