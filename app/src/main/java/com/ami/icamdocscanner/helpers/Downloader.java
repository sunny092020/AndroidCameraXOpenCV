package com.ami.icamdocscanner.helpers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import static android.content.Context.DOWNLOAD_SERVICE;

public class Downloader {
    public static DownloadManager downloadManager = null;
    public static HashMap<String, Long> downloadIdLangMap = new HashMap<>();

    public static void downloadFile(String lang, Context context) {
        context.getApplicationContext().registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        downloadManager = (DownloadManager)context.getSystemService(DOWNLOAD_SERVICE);

        String Url = "https://github.com/tesseract-ocr/tessdata/raw/4.0.0/" + lang + ".traineddata";

        FileUtils.ensureOcrDir(context);

        String fileName = Url.substring(Url.lastIndexOf('/') + 1);
        try {
            Uri Download_Uri = Uri.parse(Url);
            DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setAllowedOverRoaming(false);
            request.setTitle(fileName);
            request.setDescription("Downloading...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(context, null, "ocr/tessdata/" + fileName);

            long downloadReference = downloadManager.enqueue(request);
            downloadIdLangMap.put(lang, downloadReference);

            new Thread(() -> {

                boolean downloading = true;

                while (downloading) {

                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadReference);

                    Cursor cursor = downloadManager.query(q);
                    cursor.moveToFirst();

                    // if user cancel downloading
                    if(cursor.getCount() == 0) {
                        cursor.close();
                        break;
                    }

                    int bytes_downloaded = cursor.getInt(cursor
                            .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false;
                    }

                    final int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);

                    Log.d("dl_progress", lang + " " + dl_progress);
                    cursor.close();
                }

            }).start();

        } catch (IllegalArgumentException e) {}
    }

    private static BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Long completedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            String lang = "";
            for (Map.Entry me : Downloader.downloadIdLangMap.entrySet()) {
                if(completedDownloadId == me.getValue()) {
                    lang = (String) me.getKey();
                    break;
                }
            }
            Downloader.downloadIdLangMap.remove(lang);
        }
    };

}
