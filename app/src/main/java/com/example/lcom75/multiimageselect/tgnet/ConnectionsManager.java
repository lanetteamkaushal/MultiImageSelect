package com.example.lcom75.multiimageselect.tgnet;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.example.lcom75.multiimageselect.ApplicationLoader;
import com.example.lcom75.multiimageselect.Utilities;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

public class ConnectionsManager {

    public final static int ConnectionTypeGeneric = 1;
    public final static int ConnectionTypeDownload = 2;
    public final static int ConnectionTypeUpload = 4;
    public final static int ConnectionTypePush = 8;
    public final static int ConnectionTypeDownload2 = ConnectionTypeDownload | (1 << 16);

    public final static int RequestFlagEnableUnauthorized = 1;
    public final static int RequestFlagFailOnServerErrors = 2;
    public final static int RequestFlagCanCompress = 4;
    public final static int RequestFlagWithoutLogin = 8;
    public final static int RequestFlagTryDifferentDc = 16;
    public final static int RequestFlagForceDownload = 32;
    public final static int RequestFlagInvokeAfter = 64;
    public final static int RequestFlagNeedQuickAck = 128;

    public final static int ConnectionStateConnecting = 1;
    public final static int ConnectionStateWaitingForNetwork = 2;
    public final static int ConnectionStateConnected = 3;
    public final static int ConnectionStateUpdating = 4;

    public final static int DEFAULT_DATACENTER_ID = Integer.MAX_VALUE;

    private long lastPauseTime = System.currentTimeMillis();
    private boolean appPaused = true;
    private int lastClassGuid = 1;
    private boolean isUpdating = false;
//    private int connectionState = native_getConnectionState();
    private volatile int lastRequestToken = 1;
    private PowerManager.WakeLock wakeLock = null;

    private static volatile ConnectionsManager Instance = null;

    public static ConnectionsManager getInstance() {
        ConnectionsManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (ConnectionsManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ConnectionsManager();
                }
            }
        }
        return localInstance;
    }

    public int generateClassGuid() {
        return lastClassGuid++;
    }

    public ConnectionsManager() {
        try {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lock");
            wakeLock.setReferenceCounted(false);
        } catch (Exception e) {
           Log.e("tmessages",e.getMessage());
        }
    }

    public long getCurrentTimeMillis() {
        return native_getCurrentTimeMillis();
    }

    public int sendRequest(TLObject object, RequestDelegate completionBlock, QuickAckDelegate quickAckBlock, int flags) {
        return sendRequest(object, completionBlock, quickAckBlock, flags, DEFAULT_DATACENTER_ID, ConnectionTypeGeneric, true);
    }
    public int sendRequest(TLObject object, RequestDelegate completionBlock, int flags, int connetionType) {
        return sendRequest(object, completionBlock, null, flags, DEFAULT_DATACENTER_ID, connetionType, true);
    }
    public int sendRequest(final TLObject object, final RequestDelegate onComplete, final QuickAckDelegate onQuickAck, final int flags, final int datacenterId, final int connetionType, final boolean immediate) {
        final int requestToken = lastRequestToken++;
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
               Log.d("tmessages", "send request " + object);
                NativeByteBuffer buffer = new NativeByteBuffer(object.getObjectSize());
                object.serializeToStream(buffer);
                object.freeResources();

                native_sendRequest(buffer.address, new RequestDelegateInternal() {
                    @Override
                    public void run(int response, int errorCode, String errorText) {
                        try {
                            TLObject resp = null;
                            TLRPC.TL_error error = null;
                            if (response != 0) {
                                NativeByteBuffer buff = NativeByteBuffer.wrap(response);
                                resp = object.deserializeResponse(buff, buff.readInt32(true), true);
                            } else if (errorText != null) {
                                error = new TLRPC.TL_error();
                                error.code = errorCode;
                                error.text = errorText;
                               Log.e("tmessages", object + " got error " + error.code + " " + error.text);
                            }
                           Log.d("tmessages", "java received " + resp + " error = " + error);
                            final TLObject finalResponse = resp;
                            final TLRPC.TL_error finalError = error;
                            Utilities.stageQueue.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    onComplete.run(finalResponse, finalError);
                                    if (finalResponse != null) {
                                        finalResponse.freeResources();
                                    }
                                }
                            });
                        } catch (Exception e) {
                           Log.e("tmessages",e.getMessage());
                        }
                    }
                }, onQuickAck, flags, datacenterId, connetionType, immediate, requestToken);
            }
        });
        return requestToken;
    }

    private void checkConnection() {
        native_setUseIpv6(useIpv6Address());
        native_setNetworkAvailable(isNetworkOnline());
    }

    public void init(int version, int layer, int apiId, String deviceModel, String systemVersion, String appVersion, String langCode, String configPath, int userId) {
        native_init(version, layer, apiId, deviceModel, systemVersion, appVersion, langCode, configPath, userId);
        checkConnection();
        BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkConnection();
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        ApplicationLoader.applicationContext.registerReceiver(networkStateReceiver, filter);
    }

    public static native void native_switchBackend();
    public static native void native_pauseNetwork();
    public static native void native_setUseIpv6(boolean value);
    public static native void native_updateDcSettings();
    public static native void native_setNetworkAvailable(boolean value);
    public static native void native_resumeNetwork(boolean partial);
    public static native long native_getCurrentTimeMillis();
    public static native int native_getCurrentTime();
    public static native int native_getTimeDifference();
    public static native void native_sendRequest(int object, RequestDelegateInternal onComplete, QuickAckDelegate onQuickAck, int flags, int datacenterId, int connetionType, boolean immediate, int requestToken);
    public static native void native_cancelRequest(int token, boolean notifyServer);
    public static native void native_cleanUp();
    public static native void native_cancelRequestsForGuid(int guid);
    public static native void native_bindRequestToGuid(int requestToken, int guid);
    public static native void native_applyDatacenterAddress(int datacenterId, String ipAddress, int port);
    public static native int native_getConnectionState();
    public static native void native_setUserId(int id);
    public static native void native_init(int version, int layer, int apiId, String deviceModel, String systemVersion, String appVersion, String langCode, String configPath, int userId);
    public static native void native_setJava(boolean useJavaByteBuffers);

    @SuppressLint("NewApi")
    protected static boolean useIpv6Address() {
        if (Build.VERSION.SDK_INT < 19) {
            return false;
        }
        if (true) {
            try {
                NetworkInterface networkInterface;
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    networkInterface = networkInterfaces.nextElement();
                    if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.getInterfaceAddresses().isEmpty()) {
                        continue;
                    }
                   Log.e("tmessages", "valid interface: " + networkInterface);
                    List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                    for (int a = 0; a < interfaceAddresses.size(); a++) {
                        InterfaceAddress address = interfaceAddresses.get(a);
                        InetAddress inetAddress = address.getAddress();
                        if (true) {
                           Log.e("tmessages", "address: " + inetAddress.getHostAddress());
                        }
                        if (inetAddress.isLinkLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isMulticastAddress()) {
                            continue;
                        }
                        if (true) {
                           Log.e("tmessages", "address is good");
                        }
                    }
                }
            } catch (Throwable e) {
               Log.e("tmessages", e.getMessage());
            }
        }
        try {
            NetworkInterface networkInterface;
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            boolean hasIpv4 = false;
            boolean hasIpv6 = false;
            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                for (int a = 0; a < interfaceAddresses.size(); a++) {
                    InterfaceAddress address = interfaceAddresses.get(a);
                    InetAddress inetAddress = address.getAddress();
                    if (inetAddress.isLinkLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isMulticastAddress()) {
                        continue;
                    }
                    if (inetAddress instanceof Inet6Address) {
                        hasIpv6 = true;
                    } else if (inetAddress instanceof Inet4Address) {
                        String addrr = inetAddress.getHostAddress();
                        if (!addrr.startsWith("192.0.0.")) {
                            hasIpv4 = true;
                        }
                    }
                }
            }
            if (!hasIpv4 && hasIpv6) {
                return true;
            }
        } catch (Throwable e) {
           Log.e("tmessages", e.getMessage());
        }

        return false;
    }

    public static boolean isNetworkOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && (netInfo.isConnectedOrConnecting() || netInfo.isAvailable())) {
                return true;
            }

            netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                return true;
            } else {
                netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                    return true;
                }
            }
        } catch (Exception e) {
           Log.e("tmessages", e.getMessage());
            return true;
        }
        return false;
    }

    public void cancelRequest(int token, boolean notifyServer) {
        native_cancelRequest(token, notifyServer);
    }

    public void cancelRequestsForGuid(int classGuid) {
    }
}
