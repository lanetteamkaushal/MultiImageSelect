package com.example.lcom75.multiimageselect.customviews;

import android.util.Log;

/**
 * Created by lcom75 on 15/2/16.
 */
public class FileLog {
    public static void e(String tmessages, Throwable e) {
        Log.e(tmessages, " :: " + e.getMessage());
        e.printStackTrace();

    }
}
