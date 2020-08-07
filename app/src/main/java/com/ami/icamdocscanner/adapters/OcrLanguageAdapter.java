package com.ami.icamdocscanner.adapters;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.helpers.Downloader;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.Preferences;
import com.ami.icamdocscanner.models.OcrLanguage;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OcrLanguageAdapter extends RecyclerView.Adapter<OcrLanguageAdapter.ViewHolder> {
    public List<OcrLanguage> getLangs() {
        return langs;
    }

    public void setLangs(List<OcrLanguage> langs) {
        this.langs = langs;
    }

    private List<OcrLanguage> langs;
    private LayoutInflater mInflater;

    public OcrLanguageAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public OcrLanguageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("onCreateViewHolder", "onCreateViewHolder");
        View view = mInflater.inflate(R.layout.item_ocr_language, parent, false);
        return new OcrLanguageAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OcrLanguageAdapter.ViewHolder holder, int position) {
        OcrLanguage lang = langs.get(position);
        holder.bind(lang);
    }

    @Override
    public int getItemCount() {
        return langs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private CheckBox used;
        private View itemView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.langName);
            used = itemView.findViewById(R.id.used);
            this.itemView = itemView;
        }

        void bind(OcrLanguage lang) {
            name.setText(lang.getName());
            used.setChecked(Preferences.getLangUsed((Activity) itemView.getContext(), lang.getLang()));

            used.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if ( isChecked ) {
                    Preferences.setlangUsed((Activity) itemView.getContext(), lang.getLang(), true);
                    Downloader.downloadFile(lang.getLang(), itemView.getContext());
                } else {
                    Preferences.setlangUsed((Activity) itemView.getContext(), lang.getLang(), false);
                    File trainFile = new File(FileUtils.ocrFile(itemView.getContext(), lang.getLang()));
                    if(trainFile.exists()) trainFile.delete();

                    Long downloadId = Downloader.downloadIdLangMap.get(lang.getLang());
                    if(downloadId==null) return;
                    Downloader.downloadManager.remove(downloadId);
                    Downloader.downloadIdLangMap.remove(lang.getLang());
                }
            });
        }

    }

}
