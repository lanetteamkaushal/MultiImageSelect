package com.example.lcom75.multiimageselect;

import android.content.Context;

import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by lcom75 on 26/1/16.
 */
public class CustomUniversalImageDownloader extends BaseImageDownloader {

    public CustomUniversalImageDownloader(Context context) {
        super(context);
    }

    @Override
    protected InputStream getStreamFromNetwork(String imageUri, Object extra) throws IOException {
        try {
            return super.getStreamFromNetwork(imageUri, extra);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
