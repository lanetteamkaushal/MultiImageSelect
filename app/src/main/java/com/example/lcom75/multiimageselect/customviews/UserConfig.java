package com.example.lcom75.multiimageselect.customviews;

/**
 * Created by lcom75 on 12/2/16.
 */
public class UserConfig {
    private final static Object sync = new Object();
    public static int lastSendMessageId = -210000;
    public static int lastLocalId = -210000;

    public static int getNewMessageId() {
        int id;
        synchronized (sync) {
            id = lastSendMessageId;
            lastSendMessageId--;
        }
        return id;
    }
}
