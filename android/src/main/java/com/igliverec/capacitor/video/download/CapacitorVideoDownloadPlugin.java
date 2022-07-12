package com.igliverec.capacitor.video.download;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.text.SimpleDateFormat;
import java.util.Date;

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

    public static String tag = "Capacitor/VideoDownloadPlugin";
    private long downloadId;

    private boolean beginDownload(String url, String subPath, String fileName) {
        DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        Log.d(CapacitorVideoDownloadPlugin.tag, "beginDownload: " + url + " album: "+subPath + " filename: "+fileName);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)// Visibility of the download Notification
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, subPath+"/"+fileName)
                .setTitle(fileName)// Title of the Download Notification
                .setDescription("Downloading recording")// Description of the Download Notification
                .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                .setAllowedOverRoaming(true);
        request.allowScanningByMediaScanner();
        downloadId = downloadManager.enqueue(request);

        JSObject st = new JSObject();
        st.put("id", downloadId);
        st.put("started", true);
        notifyListeners("status", st);

        boolean finishDownload = false;
        int progress = 0;


        while (!finishDownload) {
            Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status) {
                    case DownloadManager.STATUS_FAILED: {
                        finishDownload = true;
                        int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                        String failedReason = "";
                        switch(reason){
                            case DownloadManager.ERROR_CANNOT_RESUME:
                                failedReason = "ERROR_CANNOT_RESUME";
                                break;
                            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                                failedReason = "ERROR_DEVICE_NOT_FOUND";
                                break;
                            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                                failedReason = "ERROR_FILE_ALREADY_EXISTS";
                                break;
                            case DownloadManager.ERROR_FILE_ERROR:
                                failedReason = "ERROR_FILE_ERROR";
                                break;
                            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                                failedReason = "ERROR_HTTP_DATA_ERROR";
                                break;
                            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                                failedReason = "ERROR_INSUFFICIENT_SPACE";
                                break;
                            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                                failedReason = "ERROR_TOO_MANY_REDIRECTS";
                                break;
                            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                                failedReason = "ERROR_UNHANDLED_HTTP_CODE";
                                break;
                            case DownloadManager.ERROR_UNKNOWN:
                                failedReason = "ERROR_UNKNOWN";
                                break;
                        }
                        JSObject ret = new JSObject();
                        ret.put("canceled",true);
                        ret.put("reason", failedReason);
                        notifyListeners("status", ret, true);
                        break;
                    }
                    case DownloadManager.STATUS_PAUSED:
                    case DownloadManager.STATUS_PENDING:
                        break;
                    case DownloadManager.STATUS_RUNNING: {
                        final long total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        final long downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int _progress = (int) ((downloaded * 100L) / total);
                        if (total >= 0 && _progress > progress) {
                            progress = _progress;
                            JSObject ret = new JSObject();
                            ret.put("id", downloadId);
                            ret.put("progress", progress);
                            ret.put("size", downloaded);
                            ret.put("total", total);
                            notifyListeners("progress", ret);
                        }
                        break;
                    }
                    case DownloadManager.STATUS_SUCCESSFUL: {
                        progress = 100;
                        JSObject ret = new JSObject();
                        ret.put("id", downloadId);
                        notifyListeners("complete", ret, true);
                        finishDownload = true;
                        break;
                    }
                }
            } else {
                finishDownload = true;
                JSObject ret = new JSObject();
                ret.put("canceled", true);
                ret.put("reason", "CANCELED");
                notifyListeners("status", ret, true);
                return false;
            }
            cursor.close();
        }
        return true;
    }

    @PermissionCallback
    private void storagePermsCallback(PluginCall call) {
        Log.d(CapacitorVideoDownloadPlugin.tag, "PluginCall: " + call.getMethodName());
        if (getPermissionState("storage") == PermissionState.GRANTED) {
            _saveMedia(call, "MOVIES");
        } else {
            call.reject("Permission is required to download videos.");
        }
    }

    @PluginMethod()
    public void saveVideo(final PluginCall call) {
        Log.d(getLogTag(), "SAVE VIDEO to album");

        if (getPermissionState("storage") != PermissionState.GRANTED) {
            Log.d(getLogTag(), "Dont have permissions. Save call and ask for permissions.");
            requestPermissionForAlias("storage", call, "storagePermsCallback");
        } else {
            _saveMedia(call, "MOVIES");
        }
    }

    @PluginMethod()
    public void cancel(PluginCall call) {
        DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.remove(downloadId);
        call.resolve();
    }

    private void _saveMedia(final PluginCall call, String destination) {
        String album = call.getString("album");
        final String extension = call.getString("extension");

        Log.d(CapacitorVideoDownloadPlugin.tag, "___SAVE MEDIA TO ALBUM " + album);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        String fileName = "VID_" + timeStamp + extension;

        final String inputPath = call.getString("path");
        if (inputPath == null || (!inputPath.startsWith("http") && !inputPath.startsWith("https"))) {
            call.reject("Input file path is required");
            return;
        }
        boolean st = beginDownload(inputPath, album, fileName);
        JSObject ret = new JSObject();
        ret.put("status", st);
        call.resolve(ret);
    }
}