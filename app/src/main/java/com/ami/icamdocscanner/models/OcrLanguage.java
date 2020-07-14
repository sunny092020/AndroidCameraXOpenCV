package com.ami.icamdocscanner.models;

import com.ami.icamdocscanner.helpers.OcrUtils;

public class OcrLanguage {
    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String lang, name;

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    private boolean isUsed = false;

    public OcrLanguage(String lang, String name) {
        this.lang = lang;
        this.name = name;
    }

}
