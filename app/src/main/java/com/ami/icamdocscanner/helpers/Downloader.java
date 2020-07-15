package com.ami.icamdocscanner.helpers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.ami.icamdocscanner.libraries.Line;
import com.ami.icamdocscanner.libraries.LinePolar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.DOWNLOAD_SERVICE;

public class Downloader {
    public static DownloadManager downloadManager = null;
    public static HashMap<String, Long> downloadIdLangMap = new HashMap<>();

    public static void downloadFile(String lang, Context context) {
        context.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        downloadManager = (DownloadManager)context.getSystemService(DOWNLOAD_SERVICE);

        String Url = "https://github.com/tesseract-ocr/tessdata/raw/4.0.0/" + lang + ".traineddata";

        String fileName = Url.substring(Url.lastIndexOf('/') + 1);
        try {
            Uri Download_Uri = Uri.parse(Url);
            DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setAllowedOverRoaming(false);
            request.setTitle(fileName);
            request.setDescription("Downloading...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            long downloadReference = downloadManager.enqueue(request);
            downloadIdLangMap.put(lang, downloadReference);
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
