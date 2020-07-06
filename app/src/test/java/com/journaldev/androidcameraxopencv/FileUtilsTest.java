package com.journaldev.androidcameraxopencv;

import com.ami.icamdocscanner.helpers.FileUtils;

import org.junit.Test;
import org.opencv.core.Point;

import static org.junit.Assert.*;

public class FileUtilsTest {
    @Test
    public void fileNameWithoutExtension_isCorrect() {
        assertEquals("abc", FileUtils.fileNameWithoutExtension("abc.pdf"));
        assertEquals("def", FileUtils.fileNameWithoutExtension("def.jpg"));
        assertEquals("def", FileUtils.fileNameWithoutExtension("def"));
    }

    @Test
    public void fileExtension_isCorrect() {
        assertEquals("pdf", FileUtils.fileExtension("abc.pdf"));
        assertEquals("jpg", FileUtils.fileExtension("def.jpg"));
        assertEquals("", FileUtils.fileExtension("def"));
    }

    @Test
    public void isFileType_isCorrect() {
        assertEquals(true, FileUtils.isFileType("abc.pdf", "pdf"));
        assertEquals(true, FileUtils.isFileType("def.jpg", "jpg"));
        assertEquals(false, FileUtils.isFileType("def.jpg", "pdf"));
        assertEquals(false, FileUtils.isFileType("def", "pdf"));
    }
}
