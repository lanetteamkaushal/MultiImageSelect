/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.example.lcom75.multiimageselect.customviews;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.example.lcom75.multiimageselect.ApplicationLoader;

import java.io.File;

public class UserConfig {

    private static TLRPC.User currentUser;
    public static boolean registeredForPush = false;
    public static String pushString = "";
    public static int lastSendMessageId = -210000;
    public static int lastLocalId = -210000;
    public static int lastBroadcastId = -1;
    public static String contactsHash = "";
    public static String importHash = "";
    public static boolean blockedUsersLoaded = false;
    private final static Object sync = new Object();
    public static boolean saveIncomingPhotos = false;
    public static int contactsVersion = 1;
    public static String passcodeHash = "";
    public static byte[] passcodeSalt = new byte[0];
    public static boolean appLocked = false;
    public static int passcodeType = 0;
    public static int autoLockIn = 60 * 60;
    public static int lastPauseTime = 0;
    public static boolean isWaitingForPasscodeEnter = false;
    public static boolean useFingerprint = true;
    public static int lastUpdateVersion;
    public static int lastContactsSyncTime;

    public static int migrateOffsetId = -1;
    public static int migrateOffsetDate = -1;
    public static int migrateOffsetUserId = -1;
    public static int migrateOffsetChatId = -1;
    public static int migrateOffsetChannelId = -1;
    public static long migrateOffsetAccess = -1;

    public static int getNewMessageId() {
        int id;
        synchronized (sync) {
            id = lastSendMessageId;
            lastSendMessageId--;
        }
        return id;
    }

    public static void saveConfig(boolean withFile) {
        saveConfig(withFile, null);
    }

    public static void saveConfig(boolean withFile, File oldFile) {
        synchronized (sync) {
            try {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("registeredForPush", registeredForPush);
                editor.putString("pushString2", pushString);
                editor.putInt("lastSendMessageId", lastSendMessageId);
                editor.putInt("lastLocalId", lastLocalId);
                editor.putString("contactsHash", contactsHash);
                editor.putString("importHash", importHash);
                editor.putBoolean("saveIncomingPhotos", saveIncomingPhotos);
                editor.putInt("contactsVersion", contactsVersion);
                editor.putInt("lastBroadcastId", lastBroadcastId);
                editor.putBoolean("blockedUsersLoaded", blockedUsersLoaded);
                editor.putString("passcodeHash1", passcodeHash);
                editor.putString("passcodeSalt", passcodeSalt.length > 0 ? Base64.encodeToString(passcodeSalt, Base64.DEFAULT) : "");
                editor.putBoolean("appLocked", appLocked);
                editor.putInt("passcodeType", passcodeType);
                editor.putInt("autoLockIn", autoLockIn);
                editor.putInt("lastPauseTime", lastPauseTime);
                editor.putInt("lastUpdateVersion", lastUpdateVersion);
                editor.putInt("lastContactsSyncTime", lastContactsSyncTime);
                editor.putBoolean("useFingerprint", useFingerprint);

                editor.putInt("migrateOffsetId", migrateOffsetId);
                if (migrateOffsetId != -1) {
                    editor.putInt("migrateOffsetDate", migrateOffsetDate);
                    editor.putInt("migrateOffsetUserId", migrateOffsetUserId);
                    editor.putInt("migrateOffsetChatId", migrateOffsetChatId);
                    editor.putInt("migrateOffsetChannelId", migrateOffsetChannelId);
                    editor.putLong("migrateOffsetAccess", migrateOffsetAccess);
                }

                if (currentUser != null) {
                    if (withFile) {
                        SerializedData data = new SerializedData();
                        currentUser.serializeToStream(data);
                        String userString = Base64.encodeToString(data.toByteArray(), Base64.DEFAULT);
                        editor.putString("user", userString);
                        data.cleanup();
                    }
                } else {
                    editor.remove("user");
                }

                editor.commit();
                if (oldFile != null) {
                    oldFile.delete();
                }
            } catch (Exception e) {
                Log.e("tmessages", e.getMessage());
            }
        }
    }

    public static boolean isClientActivated() {
        synchronized (sync) {
            return currentUser != null;
        }
    }

    public static int getClientUserId() {
        synchronized (sync) {
            return currentUser != null ? currentUser.id : 0;
        }
    }

    public static TLRPC.User getCurrentUser() {
        synchronized (sync) {
            return currentUser;
        }
    }

    public static void setCurrentUser(TLRPC.User user) {
        synchronized (sync) {
            currentUser = user;
        }
    }

    public static void clearConfig() {
        currentUser = null;
        registeredForPush = false;
        contactsHash = "";
        importHash = "";
        lastSendMessageId = -210000;
        contactsVersion = 1;
        lastBroadcastId = -1;
        saveIncomingPhotos = false;
        blockedUsersLoaded = false;
        migrateOffsetId = -1;
        migrateOffsetDate = -1;
        migrateOffsetUserId = -1;
        migrateOffsetChatId = -1;
        migrateOffsetChannelId = -1;
        migrateOffsetAccess = -1;
        appLocked = false;
        passcodeType = 0;
        passcodeHash = "";
        passcodeSalt = new byte[0];
        autoLockIn = 60 * 60;
        lastPauseTime = 0;
        useFingerprint = true;
        isWaitingForPasscodeEnter = false;
        lastUpdateVersion = 45;
        lastContactsSyncTime = (int) (System.currentTimeMillis() / 1000) - 23 * 60 * 60;
        saveConfig(true);
    }
}
