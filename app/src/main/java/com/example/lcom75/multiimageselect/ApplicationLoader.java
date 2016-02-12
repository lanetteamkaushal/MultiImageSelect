package com.example.lcom75.multiimageselect;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.io.File;

public class ApplicationLoader extends Application {

    // The following line should be changed to include the correct property id.
    private static final String PROPERTY_ID = "UA-61581326-1";

    // Logging TAG
    private static final String TAG = "I'm XAM";

    public static int GENERAL_TRACKER = 0;
    public static Handler applicationHandler;

    public static File getFilesDirFixed() {
        for (int a = 0; a < 10; a++) {
            File path = ApplicationLoader.applicationContext.getFilesDir();
            if (path != null) {
                return path;
            }
        }
        try {
            ApplicationInfo info = applicationContext.getApplicationInfo();
            File path = new File(info.dataDir, "files");
            path.mkdirs();
            return path;
        } catch (Exception e) {
            Log.e("tmessages", e.getMessage());
        }
        return new File("/data/data/com.Iamxam/files");
    }

    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER // Tracker used by all the apps from a company. eg:
        // roll-up tracking.
    }

    public static volatile Context applicationContext;

    public ApplicationLoader() {
        super();
    }

    private static LruCache<String, Bitmap> mMemoryCache;
    public static int IMAGE_QUALITY = 70;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
        applicationHandler = new Handler(applicationContext.getMainLooper());
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
        initImageLoader(getApplicationContext());
    }

    public static void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.imageDownloader(new CustomUniversalImageDownloader(context));
        //config.writeDebugLogs(); // Remove for release app
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }
}