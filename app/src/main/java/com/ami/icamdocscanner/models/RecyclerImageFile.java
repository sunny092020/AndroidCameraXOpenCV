package com.ami.icamdocscanner.models;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.Serializable;

public class RecyclerImageFile extends File implements Serializable {

    private boolean isChecked = false;

    public RecyclerImageFile(@NonNull File file) {
        super(file.getAbsolutePath());
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

}
