/*
 * This is the source code of IamXam for Android v. 2.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2015.
 */

package com.example.lcom75.multiimageselect.customviews;


import android.os.Environment;
import android.util.Log;

import com.example.lcom75.multiimageselect.AndroidUtilities;
import com.example.lcom75.multiimageselect.MediaController;
import com.example.lcom75.multiimageselect.tgnet.TLObject;
import com.example.lcom75.multiimageselect.tgnet.TLRPC;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class FileLoader {

    public static final int MEDIA_DIR_IMAGE = 0;
    public static final int MEDIA_DIR_AUDIO = 1;
    public static final int MEDIA_DIR_VIDEO = 2;
    public static final int MEDIA_DIR_DOCUMENT = 3;
    public static final int MEDIA_DIR_CACHE = 4;

    private HashMap<Integer, File> mediaDirs = null;
    private File telegramPath = null;
    private LinkedList<FileUploadOperation> uploadOperationQueue = new LinkedList<>();
    private LinkedList<FileUploadOperation> uploadSmallOperationQueue = new LinkedList<>();
    private LinkedList<FileLoadOperation> loadOperationQueue = new LinkedList<>();
    private LinkedList<FileLoadOperation> audioLoadOperationQueue = new LinkedList<>();
    private LinkedList<FileLoadOperation> photoLoadOperationQueue = new LinkedList<>();
    private volatile DispatchQueue fileLoaderQueue = new DispatchQueue("fileUploadQueue");
    private ConcurrentHashMap<String, FileUploadOperation> uploadOperationPaths = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, FileUploadOperation> uploadOperationPathsEnc = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, FileLoadOperation> loadOperationPaths = new ConcurrentHashMap<>();
    private HashMap<String, Long> uploadSizes = new HashMap<>();
    private int currentLoadOperationsCount = 0;
    private int currentAudioLoadOperationsCount = 0;
    private int currentPhotoLoadOperationsCount = 0;
    private int currentUploadOperationsCount = 0;
    private int currentUploadSmallOperationsCount = 0;

    public File getDirectory(int type) {
        File dir = mediaDirs.get(type);
        if (dir == null && type != MEDIA_DIR_CACHE) {
            dir = mediaDirs.get(MEDIA_DIR_CACHE);
        }
        try {
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            //don't promt
            Log.e(FileLoader.class.getSimpleName(), "  :: " + e.getMessage());
        }
        return dir;
    }


    private static volatile FileLoader Instance = null;

    public static FileLoader getInstance() {
        FileLoader localInstance = Instance;
        if (localInstance == null) {
            synchronized (FileLoader.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new FileLoader();
                }
            }
        }
        return localInstance;
    }

    public FileLoader() {
        setMediaDirs(createMediaPaths());
    }

    public void setMediaDirs(HashMap<Integer, File> dirs) {
        mediaDirs = dirs;
    }

    private HashMap<Integer, File> createMediaPaths() {
        HashMap<Integer, File> mediaDirs = new HashMap<>();
        File cachePath = AndroidUtilities.getCacheDir();
        if (!cachePath.isDirectory()) {
            try {
                cachePath.mkdirs();
            } catch (Exception e) {
                Log.e("tmessages", e.getMessage());
            }
        }
        try {
            new File(cachePath, ".nomedia").createNewFile();
        } catch (Exception e) {
            Log.e("tmessages", e.getMessage());
        }

        mediaDirs.put(FileLoader.MEDIA_DIR_CACHE, cachePath);
        Log.e("tmessages", "cache path = " + cachePath);

        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                telegramPath = new File(Environment.getExternalStorageDirectory(), "IamXam");
                telegramPath.mkdirs();

                if (telegramPath.isDirectory()) {
                    try {
                        File imagePath = new File(telegramPath, "IamXam Images");
                        imagePath.mkdir();
                        if (imagePath.isDirectory() && canMoveFiles(cachePath, imagePath)) {
                            mediaDirs.put(FileLoader.MEDIA_DIR_IMAGE, imagePath);
                            Log.e("tmessages", "image path = " + imagePath);
                        }
                    } catch (Exception e) {
                        Log.e("tmessages", e.getMessage());
                    }

                    try {
                        File videoPath = new File(telegramPath, "IamXam Video");
                        videoPath.mkdir();
                        if (videoPath.isDirectory() && canMoveFiles(cachePath, videoPath)) {
                            mediaDirs.put(FileLoader.MEDIA_DIR_VIDEO, videoPath);
                            Log.e("tmessages", "video path = " + videoPath);
                        }
                    } catch (Exception e) {
                        Log.e("tmessages", e.getMessage());
                    }

                    try {
                        File audioPath = new File(telegramPath, "IamXam Audio");
                        audioPath.mkdir();
                        if (audioPath.isDirectory() && canMoveFiles(cachePath, audioPath)) {
                            new File(audioPath, ".nomedia").createNewFile();
                            mediaDirs.put(FileLoader.MEDIA_DIR_AUDIO, audioPath);
                            Log.e("tmessages", "audio path = " + audioPath);
                        }
                    } catch (Exception e) {
                        Log.e("tmessages", e.getMessage());
                    }

                    try {
                        File documentPath = new File(telegramPath, "IamXam Documents");
                        documentPath.mkdir();
                        if (documentPath.isDirectory() && canMoveFiles(cachePath, documentPath)) {
                            new File(documentPath, ".nomedia").createNewFile();
                            mediaDirs.put(FileLoader.MEDIA_DIR_DOCUMENT, documentPath);
                            Log.e("tmessages", "documents path = " + documentPath);
                        }
                    } catch (Exception e) {
                        Log.e("tmessages", e.getMessage());
                    }
                }
            } else {
                Log.e("tmessages", "this Android can't rename files");
            }
            MediaController.getInstance().checkSaveToGalleryFiles();
        } catch (Exception e) {
            Log.e("tmessages", e.getMessage());
        }

        return mediaDirs;
    }

    private boolean canMoveFiles(File from, File to) {
        RandomAccessFile file = null;
        try {
            for (int a = 0; a < 5; a++) {
                File srcFile = new File(from, "temp.file");
                srcFile.createNewFile();
                file = new RandomAccessFile(srcFile, "rws");
                file.write(1);
                file.close();
                file = null;
                File dstFile = new File(to, "temp.file");
                boolean canRename = srcFile.renameTo(dstFile);
                srcFile.delete();
                dstFile.delete();
                if (canRename) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e("tmessages", e.getMessage());
        } finally {
            try {
                if (file != null) {
                    file.close();
                }
            } catch (Exception e) {
                Log.e("tmessages", e.getMessage());
            }
        }
        return false;
    }

    public static String getAttachFileName(TLObject attach) {
        return getAttachFileName(attach, null);
    }

    public static String getAttachFileName(TLObject attach, String ext) {
        if (attach instanceof TLRPC.Video) {
            TLRPC.Video video = (TLRPC.Video) attach;
            return video.dc_id + "_" + video.id + "." + (ext != null ? ext : "mp4");
        } else if (attach instanceof TLRPC.Document) {
            TLRPC.Document document = (TLRPC.Document) attach;
            String docExt = getDocumentFileName(document);
            int idx;
            if (docExt == null || (idx = docExt.lastIndexOf(".")) == -1) {
                docExt = "";
            } else {
                docExt = docExt.substring(idx);
            }
            if (docExt.length() > 1) {
                return document.dc_id + "_" + document.id + docExt;
            } else {
                return document.dc_id + "_" + document.id;
            }
        } else if (attach instanceof TLRPC.PhotoSize) {
            TLRPC.PhotoSize photo = (TLRPC.PhotoSize) attach;
            if (photo.location == null) {
                return "";
            }
            return photo.location.volume_id + "_" + photo.location.local_id + "." + (ext != null ? ext : "jpg");
        } else if (attach instanceof TLRPC.Audio) {
            TLRPC.Audio audio = (TLRPC.Audio) attach;
            return audio.dc_id + "_" + audio.id + "." + (ext != null ? ext : "ogg");
        } else if (attach instanceof TLRPC.FileLocation) {
            TLRPC.FileLocation location = (TLRPC.FileLocation) attach;
            return location.volume_id + "_" + location.local_id + "." + (ext != null ? ext : "jpg");
        }
        return "";
    }


    public static String getDocumentFileName(TLRPC.Document document) {
        if (document != null) {
            if (document.file_name != null) {
                return document.file_name;
            }
            for (TLRPC.DocumentAttribute documentAttribute : document.attributes) {
                if (documentAttribute instanceof TLRPC.TL_documentAttributeFilename) {
                    return documentAttribute.file_name;
                }
            }
        }
        return "";
    }

    public void checkUploadNewDataAvailable(final String location, final boolean encrypted, final long finalSize) {
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                FileUploadOperation operation;
                if (encrypted) {
                    operation = uploadOperationPathsEnc.get(location);
                } else {
                    operation = uploadOperationPaths.get(location);
                }
                if (operation != null) {
                    operation.checkNewDataAvailable(finalSize);
                } else if (finalSize != 0) {
                    uploadSizes.put(location, finalSize);
                }
            }
        });
    }

    public void cancelLoadFile(TLRPC.Video video) {
        cancelLoadFile(video, null, null, null, null);
    }

    public void cancelLoadFile(TLRPC.Document document) {
        cancelLoadFile(null, document, null, null, null);
    }

    public void cancelLoadFile(TLRPC.Audio audio) {
        cancelLoadFile(null, null, audio, null, null);
    }

    public void cancelLoadFile(TLRPC.PhotoSize photo) {
        cancelLoadFile(null, null, null, photo.location, null);
    }

    public void cancelLoadFile(TLRPC.FileLocation location, String ext) {
        cancelLoadFile(null, null, null, location, ext);
    }

    private void cancelLoadFile(final TLRPC.Video video, final TLRPC.Document document, final TLRPC.Audio audio, final TLRPC.FileLocation location, final String locationExt) {
        if (video == null && location == null && document == null && audio == null) {
            return;
        }
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                String fileName = null;
                if (video != null) {
                    fileName = getAttachFileName(video);
                } else if (location != null) {
                    fileName = getAttachFileName(location, locationExt);
                } else if (document != null) {
                    fileName = getAttachFileName(document);
                } else if (audio != null) {
                    fileName = getAttachFileName(audio);
                }
                if (fileName == null) {
                    return;
                }
                FileLoadOperation operation = loadOperationPaths.get(fileName);
                if (operation != null) {
                    loadOperationPaths.remove(fileName);
                    if (audio != null) {
                        audioLoadOperationQueue.remove(operation);
                    } else if (location != null) {
                        photoLoadOperationQueue.remove(operation);
                    } else {
                        loadOperationQueue.remove(operation);
                    }
                    operation.cancel();
                }
            }
        });
    }

    public void setDelegate(FileLoaderDelegate delegate) {
        this.delegate = delegate;
    }

    public interface FileLoaderDelegate {
        void fileUploadProgressChanged(String location, float progress, boolean isEncrypted);

        void fileDidUploaded(String location, TLRPC.InputFile inputFile, TLRPC.InputEncryptedFile inputEncryptedFile, byte[] key, byte[] iv, long totalFileSize);

        void fileDidFailedUpload(String location, boolean isEncrypted);

        void fileDidLoaded(String location, File finalFile, int type);

        void fileDidFailedLoad(String location, int state);

        void fileLoadProgressChanged(String location, float progress);
    }

    private FileLoaderDelegate delegate = null;

    public void loadFile(TLRPC.Video video, boolean force) {
        loadFile(video, null, null, null, null, 0, force, video != null && video.key != null);
    }

    public void loadFile(TLRPC.PhotoSize photo, String ext, boolean cacheOnly) {
        loadFile(null, null, null, photo.location, ext, photo.size, false, cacheOnly || (photo != null && photo.size == 0 || photo.location.key != null));
    }

    public void loadFile(TLRPC.Document document, boolean force, boolean cacheOnly) {
        loadFile(null, document, null, null, null, 0, force, cacheOnly || document != null && document.key != null);
    }

    public void loadFile(TLRPC.Audio audio, boolean force) {
        loadFile(null, null, audio, null, null, 0, false, audio != null && audio.key != null);
    }

    public void loadFile(TLRPC.FileLocation location, String ext, int size, boolean cacheOnly) {
        loadFile(null, null, null, location, ext, size, true, cacheOnly || size == 0 || (location != null && location.key != null));
    }

    private void loadFile(final TLRPC.Video video, final TLRPC.Document document, final TLRPC.Audio audio, final TLRPC.FileLocation location, final String locationExt, final int locationSize, final boolean force, final boolean cacheOnly) {
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                String fileName = null;
                if (video != null) {
                    fileName = getAttachFileName(video);
                } else if (location != null) {
                    fileName = getAttachFileName(location, locationExt);
                } else if (document != null) {
                    fileName = getAttachFileName(document);
                } else if (audio != null) {
                    fileName = getAttachFileName(audio);
                }
                if (fileName == null || fileName.contains("" + Integer.MIN_VALUE)) {
                    return;
                }

                FileLoadOperation operation;
                operation = loadOperationPaths.get(fileName);
                if (operation != null) {
                    if (force) {
                        LinkedList<FileLoadOperation> downloadQueue;
                        if (audio != null) {
                            downloadQueue = audioLoadOperationQueue;
                        } else if (location != null) {
                            downloadQueue = photoLoadOperationQueue;
                        } else {
                            downloadQueue = loadOperationQueue;
                        }
                        if (downloadQueue != null) {
                            int index = downloadQueue.indexOf(operation);
                            if (index != -1) {
                                downloadQueue.remove(index);
                                downloadQueue.add(0, operation);
                                operation.setForceRequest(true);
                            }
                        }
                    }
                    return;
                }

                File tempDir = getDirectory(MEDIA_DIR_CACHE);
                File storeDir = tempDir;
                int type = MEDIA_DIR_CACHE;

                if (video != null) {
                    operation = new FileLoadOperation(video);
                    type = MEDIA_DIR_VIDEO;
                } else if (location != null) {
                    operation = new FileLoadOperation(location, locationExt, locationSize);
                    type = MEDIA_DIR_IMAGE;
                } else if (document != null) {
                    operation = new FileLoadOperation(document);
                    type = MEDIA_DIR_DOCUMENT;
                } else if (audio != null) {
                    operation = new FileLoadOperation(audio);
                    type = MEDIA_DIR_AUDIO;
                }
                if (!cacheOnly) {
                    storeDir = getDirectory(type);
                }
                operation.setPaths(storeDir, tempDir);

                final String finalFileName = fileName;
                final int finalType = type;
                loadOperationPaths.put(fileName, operation);
                operation.setDelegate(new FileLoadOperation.FileLoadOperationDelegate() {
                    @Override
                    public void didFinishLoadingFile(FileLoadOperation operation, File finalFile) {
                        if (delegate != null) {
                            delegate.fileDidLoaded(finalFileName, finalFile, finalType);
                        }
                        checkDownloadQueue(audio, location, finalFileName);
                    }

                    @Override
                    public void didFailedLoadingFile(FileLoadOperation operation, int canceled) {
                        checkDownloadQueue(audio, location, finalFileName);
                        if (delegate != null) {
                            delegate.fileDidFailedLoad(finalFileName, canceled);
                        }
                    }

                    @Override
                    public void didChangedLoadProgress(FileLoadOperation operation, float progress) {
                        if (delegate != null) {
                            delegate.fileLoadProgressChanged(finalFileName, progress);
                        }
                    }
                });
                int maxCount = force ? 3 : 1;
                if (audio != null) {
                    if (currentAudioLoadOperationsCount < maxCount) {
                        currentAudioLoadOperationsCount++;
                        operation.start();
                    } else {
                        if (force) {
                            audioLoadOperationQueue.add(0, operation);
                        } else {
                            audioLoadOperationQueue.add(operation);
                        }
                    }
                } else if (location != null) {
                    if (currentPhotoLoadOperationsCount < maxCount) {
                        currentPhotoLoadOperationsCount++;
                        operation.start();
                    } else {
                        if (force) {
                            photoLoadOperationQueue.add(0, operation);
                        } else {
                            photoLoadOperationQueue.add(operation);
                        }
                    }
                } else {
                    if (currentLoadOperationsCount < maxCount) {
                        currentLoadOperationsCount++;
                        operation.start();
                    } else {
                        if (force) {
                            loadOperationQueue.add(0, operation);
                        } else {
                            loadOperationQueue.add(operation);
                        }
                    }
                }
            }
        });
    }

    public static File getPathToAttach(TLObject attach) {
        return getPathToAttach(attach, null, false);
    }

    public static File getPathToAttach(TLObject attach, boolean forceCache) {
        return getPathToAttach(attach, null, forceCache);
    }

    public static File getPathToAttach(TLObject attach, String ext, boolean forceCache) {
        File dir = null;
        if (forceCache) {
            dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
        } else {
            if (attach instanceof TLRPC.Video) {
                TLRPC.Video video = (TLRPC.Video) attach;
                if (video.key != null) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    dir = getInstance().getDirectory(MEDIA_DIR_VIDEO);
                }
            } else if (attach instanceof TLRPC.Document) {
                TLRPC.Document document = (TLRPC.Document) attach;
                if (document.key != null) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    dir = getInstance().getDirectory(MEDIA_DIR_DOCUMENT);
                }
            } else if (attach instanceof TLRPC.PhotoSize) {
                TLRPC.PhotoSize photoSize = (TLRPC.PhotoSize) attach;
                if (photoSize.location == null || photoSize.location.key != null || photoSize.location.volume_id == Integer.MIN_VALUE && photoSize.location.local_id < 0 || photoSize.size < 0) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    dir = getInstance().getDirectory(MEDIA_DIR_IMAGE);
                }
            } else if (attach instanceof TLRPC.Audio) {
                TLRPC.Audio audio = (TLRPC.Audio) attach;
                if (audio.key != null) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    dir = getInstance().getDirectory(MEDIA_DIR_AUDIO);
                }
            } else if (attach instanceof TLRPC.FileLocation) {
                TLRPC.FileLocation fileLocation = (TLRPC.FileLocation) attach;
                if (fileLocation.key != null || fileLocation.volume_id == Integer.MIN_VALUE && fileLocation.local_id < 0) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    dir = getInstance().getDirectory(MEDIA_DIR_IMAGE);
                }
            }
        }
        if (dir == null) {
            return new File("");
        }
        return new File(dir, getAttachFileName(attach, ext));
    }

    private void checkDownloadQueue(final TLRPC.Audio audio, final TLRPC.FileLocation location, final String arg1) {
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                loadOperationPaths.remove(arg1);
                FileLoadOperation operation;
                if (audio != null) {
                    currentAudioLoadOperationsCount--;
                    if (!audioLoadOperationQueue.isEmpty()) {
                        operation = audioLoadOperationQueue.get(0);
                        int maxCount = operation.isForceRequest() ? 3 : 1;
                        if (currentAudioLoadOperationsCount < maxCount) {
                            operation = audioLoadOperationQueue.poll();
                            if (operation != null) {
                                currentAudioLoadOperationsCount++;
                                operation.start();
                            }
                        }
                    }
                } else if (location != null) {
                    currentPhotoLoadOperationsCount--;
                    if (!photoLoadOperationQueue.isEmpty()) {
                        operation = photoLoadOperationQueue.get(0);
                        int maxCount = operation.isForceRequest() ? 3 : 1;
                        if (currentPhotoLoadOperationsCount < maxCount) {
                            operation = photoLoadOperationQueue.poll();
                            if (operation != null) {
                                currentPhotoLoadOperationsCount++;
                                operation.start();
                            }
                        }
                    }
                } else {
                    currentLoadOperationsCount--;
                    if (!loadOperationQueue.isEmpty()) {
                        operation = loadOperationQueue.get(0);
                        int maxCount = operation.isForceRequest() ? 3 : 1;
                        if (currentLoadOperationsCount < maxCount) {
                            operation = loadOperationQueue.poll();
                            if (operation != null) {
                                currentLoadOperationsCount++;
                                operation.start();
                            }
                        }
                    }
                }
            }
        });
    }
    public static TLRPC.PhotoSize getClosestPhotoSizeWithSize(ArrayList<TLRPC.PhotoSize> sizes, int side) {
        return getClosestPhotoSizeWithSize(sizes, side, false);
    }

    public static TLRPC.PhotoSize getClosestPhotoSizeWithSize(ArrayList<TLRPC.PhotoSize> sizes, int side, boolean byMinSide) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        int lastSide = 0;
        TLRPC.PhotoSize closestObject = null;
        for (TLRPC.PhotoSize obj : sizes) {
            if (obj == null) {
                continue;
            }
            if (byMinSide) {
                int currentSide = obj.h >= obj.w ? obj.w : obj.h;
                if (closestObject == null || side > 100 && closestObject.location != null && closestObject.location.dc_id == Integer.MIN_VALUE || obj instanceof TLRPC.TL_photoCachedSize || side > lastSide && lastSide < currentSide) {
                    closestObject = obj;
                    lastSide = currentSide;
                }
            } else {
                int currentSide = obj.w >= obj.h ? obj.w : obj.h;
                if (closestObject == null || side > 100 && closestObject.location != null && closestObject.location.dc_id == Integer.MIN_VALUE || obj instanceof TLRPC.TL_photoCachedSize || currentSide <= side && lastSide < currentSide) {
                    closestObject = obj;
                    lastSide = currentSide;
                }
            }
        }
        return closestObject;
    }
}
