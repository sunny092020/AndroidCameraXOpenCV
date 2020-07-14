package com.ami.icamdocscanner.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.helpers.Preferences;
import com.ami.icamdocscanner.models.OcrLanguage;

import java.util.List;

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
        Log.d("bind", "bind");

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

            used.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if ( isChecked ) {
                        Preferences.setlangUsed((Activity) itemView.getContext(), lang.getLang(), true);
                    } else {
                        Preferences.setlangUsed((Activity) itemView.getContext(), lang.getLang(), false);
                    }
                }
            });
            Log.d("bind", lang.getName());
        }

    }
}
