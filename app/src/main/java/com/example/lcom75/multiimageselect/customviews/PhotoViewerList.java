package com.example.lcom75.multiimageselect.customviews;
/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;

import com.example.lcom75.multiimageselect.AndroidUtilities;
import com.example.lcom75.multiimageselect.ApplicationLoader;
import com.example.lcom75.multiimageselect.MainActivity;
import com.example.lcom75.multiimageselect.NotificationCenter;
import com.example.lcom75.multiimageselect.R;
import com.example.lcom75.multiimageselect.Utilities;
import com.example.lcom75.multiimageselect.tgnet.ConnectionsManager;
import com.example.lcom75.multiimageselect.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

@SuppressWarnings("unchecked")
public class PhotoViewerList implements NotificationCenter.NotificationCenterDelegate, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private int classGuid;
    private PhotoViewer.PhotoViewerProvider placeProvider;
    private boolean isVisible;

    private Activity parentActivity;

    private ActionBar actionBar;
    private boolean isActionBarVisible = true;

    private static Drawable[] progressDrawables;

    private WindowManager.LayoutParams windowLayoutParams;
    private FrameLayout.LayoutParams fwindowLayoutParams;
    private FrameLayoutDrawer containerView;
    private FrameLayoutTouchListener windowView;
    private ClippingImageView animatingImageView;
    private FrameLayout bottomLayout;
    private TextView nameTextView;
    private TextView dateTextView;
    private MenuItem menuItem;
    private ImageView shareButton;
    private BackgroundDrawable backgroundDrawable = new BackgroundDrawable(0xff000000);
    private CheckBox checkImageView;
    //    private View pickerView;
//    private View editorDoneLayout;
    private RadialProgressView radialProgressViews[] = new RadialProgressView[3];
    private MenuItem cropItem;
    private MenuItem tuneItem;
    private MenuItem captionItem;
    private MenuItem captionDoneItem;
    private AnimatorSet currentActionBarAnimation;
    private PhotoCropView photoCropView;
    private View photoFilterView;
    private AlertDialog visibleDialog;
    private TextView captionTextView;
    private TextView captionTextViewOld;
    private TextView captionTextViewNew;

    private boolean canShowBottom = true;
    private int sendPhotoType = 0;
    private boolean needCaptionLayout;

    private float animationValues[][] = new float[2][8];

    private Activity parentChatActivity;
    private LinearLayoutManager mentionLayoutManager;
    private AnimatorSet mentionListAnimation;
    private boolean allowMentions;

    private int animationInProgress = 0;
    private long transitionAnimationStartTime = 0;
    private Runnable animationEndRunnable = null;
    private PhotoViewer.PlaceProviderObject showAfterAnimation;
    private PhotoViewer.PlaceProviderObject hideAfterAnimation;
    private boolean disableShowCheck = false;

    private String lastTitle;

    private int currentEditMode;

    private ImageReceiver leftImage = new ImageReceiver();
    private ImageReceiver centerImage = new ImageReceiver();
    private ImageReceiver rightImage = new ImageReceiver();
    private int currentIndex;
    private Object currentObject;
    private TLRPC.FileLocation currentFileLocation;
    private String currentFileNames[] = new String[3];
    private PhotoViewer.PlaceProviderObject currentPlaceObject;
    private String currentPathObject;
    private Bitmap currentThumb = null;

    private int avatarsUserId;
    private long currentDialogId;
    private long mergeDialogId;
    private int totalImagesCount;
    private int totalImagesCountMerge;
    private boolean isFirstLoading;
    private boolean needSearchImageInArr;
    private boolean loadingMoreImages;
    private boolean endReached[] = new boolean[]{false, true};
    private boolean opennedFromMedia;

    private boolean draggingDown = false;
    private float dragY;
    private float translationX = 0;
    private float translationY = 0;
    private float scale = 1;
    private float animateToX;
    private float animateToY;
    private float animateToScale;
    private float animationValue;
    private long animationStartTime;
    private AnimatorSet imageMoveAnimation;
    private AnimatorSet changeModeAnimation;
    private GestureDetector gestureDetector;
    private DecelerateInterpolator interpolator = new DecelerateInterpolator(1.5f);
    private float pinchStartDistance = 0;
    private float pinchStartScale = 1;
    private float pinchCenterX;
    private float pinchCenterY;
    private float pinchStartX;
    private float pinchStartY;
    private float moveStartX;
    private float moveStartY;
    private float minX;
    private float maxX;
    private float minY;
    private float maxY;
    private boolean canZoom = true;
    private boolean changingPage = false;
    private boolean zooming = false;
    private boolean moving = false;
    private boolean doubleTap = false;
    private boolean invalidCoords = false;
    private boolean canDragDown = true;
    private boolean zoomAnimation = false;
    private boolean discardTap = false;
    private int switchImageAfterAnimation = 0;
    private VelocityTracker velocityTracker = null;
    private Scroller scroller = null;

    private ArrayList<Object> imagesArrTemp = new ArrayList<>();
    private HashMap<Integer, Object>[] imagesByIdsTemp = new HashMap[]{new HashMap<>(), new HashMap<>()};
    private ArrayList<Object> imagesArr = new ArrayList<>();
    private HashMap<Integer, Object>[] imagesByIds = new HashMap[]{new HashMap<>(), new HashMap<>()};
    private ArrayList<TLRPC.FileLocation> imagesArrLocations = new ArrayList<>();
    private ArrayList<TLRPC.Photo> avatarsArr = new ArrayList<>();
    private ArrayList<Integer> imagesArrLocationsSizes = new ArrayList<>();
    private ArrayList<Object> imagesArrLocals = new ArrayList<>();
    private TLRPC.FileLocation currentUserAvatarLocation = null;

    private final static int gallery_menu_save = 1;
    private final static int gallery_menu_showall = 2;
    private final static int gallery_menu_send = 3;
    private final static int gallery_menu_crop = 4;
    private final static int gallery_menu_delete = 6;
    private final static int gallery_menu_tune = 7;
    private final static int gallery_menu_caption = 8;
    private final static int gallery_menu_caption_done = 9;

    private final static int PAGE_SPACING = AndroidUtilities.dp(30);

    private static DecelerateInterpolator decelerateInterpolator = null;
    private static Paint progressPaint = null;

    private class BackgroundDrawable extends ColorDrawable {

        private Runnable drawRunnable;

        public BackgroundDrawable(int color) {
            super(color);
        }

        @Override
        public void setAlpha(int alpha) {
            if (parentActivity instanceof MainActivity) {
//                ((MainActivity) parentActivity).drawerLayoutContainer.setAllowDrawContent(!isVisible || alpha != 255);
            }
            super.setAlpha(alpha);
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            if (getAlpha() != 0) {
                if (drawRunnable != null) {
                    drawRunnable.run();
                    drawRunnable = null;
                }
            }
        }
    }

    private class RadialProgressView {

        private long lastUpdateTime = 0;
        private float radOffset = 0;
        private float currentProgress = 0;
        private float animationProgressStart = 0;
        private long currentProgressTime = 0;
        private float animatedProgressValue = 0;
        private RectF progressRect = new RectF();
        private int backgroundState = -1;
        private View parent = null;
        private int size = AndroidUtilities.dp(64);
        private int previousBackgroundState = -2;
        private float animatedAlphaValue = 1.0f;
        private float alpha = 1.0f;
        private float scale = 1.0f;

        public RadialProgressView(Context context, View parentView) {
            if (decelerateInterpolator == null) {
                decelerateInterpolator = new DecelerateInterpolator(1.5f);
                progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                progressPaint.setStyle(Paint.Style.STROKE);
                progressPaint.setStrokeCap(Paint.Cap.ROUND);
                progressPaint.setStrokeWidth(AndroidUtilities.dp(2));
                progressPaint.setColor(0xffffffff);
            }
            parent = parentView;
        }

        private void updateAnimation() {
            long newTime = System.currentTimeMillis();
            long dt = newTime - lastUpdateTime;
            lastUpdateTime = newTime;

            if (animatedProgressValue != 1) {
                radOffset += 360 * dt / 3000.0f;
                float progressDiff = currentProgress - animationProgressStart;
                if (progressDiff > 0) {
                    currentProgressTime += dt;
                    if (currentProgressTime >= 300) {
                        animatedProgressValue = currentProgress;
                        animationProgressStart = currentProgress;
                        currentProgressTime = 0;
                    } else {
                        animatedProgressValue = animationProgressStart + progressDiff * decelerateInterpolator.getInterpolation(currentProgressTime / 300.0f);
                    }
                }
                parent.invalidate();
            }
            if (animatedProgressValue >= 1 && previousBackgroundState != -2) {
                animatedAlphaValue -= dt / 200.0f;
                if (animatedAlphaValue <= 0) {
                    animatedAlphaValue = 0.0f;
                    previousBackgroundState = -2;
                }
                parent.invalidate();
            }
        }

        public void setProgress(float value, boolean animated) {
            if (!animated) {
                animatedProgressValue = value;
                animationProgressStart = value;
            } else {
                animationProgressStart = animatedProgressValue;
            }
            currentProgress = value;
            currentProgressTime = 0;
        }

        public void setBackgroundState(int state, boolean animated) {
            lastUpdateTime = System.currentTimeMillis();
            if (animated && backgroundState != state) {
                previousBackgroundState = backgroundState;
                animatedAlphaValue = 1.0f;
            } else {
                previousBackgroundState = -2;
            }
            backgroundState = state;
            parent.invalidate();
        }

        public void setAlpha(float value) {
            alpha = value;
        }

        public void setScale(float value) {
            scale = value;
        }

        public void onDraw(Canvas canvas) {
            int sizeScaled = (int) (size * scale);
            int x = (getContainerViewWidth() - sizeScaled) / 2;
            int y = (getContainerViewHeight() - sizeScaled) / 2;

            if (previousBackgroundState >= 0 && previousBackgroundState < 4) {
                Drawable drawable = progressDrawables[previousBackgroundState];
                if (drawable != null) {
                    drawable.setAlpha((int) (255 * animatedAlphaValue * alpha));
                    drawable.setBounds(x, y, x + sizeScaled, y + sizeScaled);
                    drawable.draw(canvas);
                }
            }

            if (backgroundState >= 0 && backgroundState < 4) {
                Drawable drawable = progressDrawables[backgroundState];
                if (drawable != null) {
                    if (previousBackgroundState != -2) {
                        drawable.setAlpha((int) (255 * (1.0f - animatedAlphaValue) * alpha));
                    } else {
                        drawable.setAlpha((int) (255 * alpha));
                    }
                    drawable.setBounds(x, y, x + sizeScaled, y + sizeScaled);
                    drawable.draw(canvas);
                }
            }

            if (backgroundState == 0 || backgroundState == 1 || previousBackgroundState == 0 || previousBackgroundState == 1) {
                int diff = AndroidUtilities.dp(1);
                if (previousBackgroundState != -2) {
                    progressPaint.setAlpha((int) (255 * animatedAlphaValue * alpha));
                } else {
                    progressPaint.setAlpha((int) (255 * alpha));
                }
                progressRect.set(x + diff, y + diff, x + sizeScaled - diff, y + sizeScaled - diff);
                canvas.drawArc(progressRect, -90 + radOffset, Math.max(4, 360 * animatedProgressValue), false, progressPaint);
                updateAnimation();
            }
        }
    }

    private class FrameLayoutTouchListener extends FrameLayout {
        private boolean attachedToWindow;
        private Runnable attachRunnable;
        private String TAG = FrameLayoutTouchListener.class.getSimpleName();

        public FrameLayoutTouchListener(Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            Log.d(TAG, "OnTouch :" + event.getAction());
            getInstance().onTouchEvent(event);
            return true;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            getInstance().onLayout(changed, left, top, right, bottom);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attachedToWindow = true;
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            attachedToWindow = false;
        }
    }

    private class FrameLayoutDrawer extends SizeNotifierFrameLayoutPhoto {
        public FrameLayoutDrawer(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            if (heightSize > AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight) {
                heightSize = AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight;
            }

            setMeasuredDimension(widthSize, heightSize);

            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            final int count = getChildCount();

            int paddingBottom = getKeyboardHeight() <= AndroidUtilities.dp(20) ? 0 : 0;

            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = Gravity.TOP | Gravity.LEFT;
                }

                final int absoluteGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = (r - l - width) / 2 + lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        childLeft = r - width - lp.rightMargin;
                        break;
                    case Gravity.LEFT:
                    default:
                        childLeft = lp.leftMargin;
                }

                switch (verticalGravity) {
                    case Gravity.TOP:
                        childTop = lp.topMargin;
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = ((b - paddingBottom) - t - height) / 2 + lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = ((b - paddingBottom) - t) - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = lp.topMargin;
                }

//                if (child == mentionListView) {
//                    if (!captionEditText.isPopupShowing() && !captionEditText.isKeyboardVisible() && captionEditText.getEmojiPadding() == 0) {
//                        childTop += AndroidUtilities.dp(400);
//                    } else {
//                        childTop -= captionEditText.getMeasuredHeight();
//                    }
//                } else if (child == captionEditText) {
//                    if (!captionEditText.isPopupShowing() && !captionEditText.isKeyboardVisible() && captionEditText.getEmojiPadding() == 0) {
//                        childTop += AndroidUtilities.dp(400);
//                    }
//                } else if (child == pickerView || child == captionTextViewNew || child == captionTextViewOld) {
//                    if (captionEditText.isPopupShowing() || captionEditText.isKeyboardVisible()) {
//                        childTop += AndroidUtilities.dp(400);
//                    }
//                } else if (captionEditText.isPopupView(child)) {
//                    childTop = captionEditText.getBottom();
//                }
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }

            notifyHeightChanged();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            getInstance().onDraw(canvas);
        }
    }

    private static volatile PhotoViewerList Instance = null;

    public static PhotoViewerList getInstance() {
        PhotoViewerList localInstance = Instance;
        if (localInstance == null) {
            synchronized (PhotoViewerList.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new PhotoViewerList();
                }
            }
        }
        return localInstance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.FileDidFailedLoad) {
            String location = (String) args[0];
            for (int a = 0; a < 3; a++) {
                if (currentFileNames[a] != null && currentFileNames[a].equals(location)) {
                    radialProgressViews[a].setProgress(1.0f, true);
                    checkProgress(a, true);
                    break;
                }
            }
        } else if (id == NotificationCenter.FileDidLoaded) {
            String location = (String) args[0];
            for (int a = 0; a < 3; a++) {
                if (currentFileNames[a] != null && currentFileNames[a].equals(location)) {
                    radialProgressViews[a].setProgress(1.0f, true);
                    checkProgress(a, true);
                    break;
                }
            }
        } else if (id == NotificationCenter.FileLoadProgressChanged) {
            String location = (String) args[0];
            for (int a = 0; a < 3; a++) {
                if (currentFileNames[a] != null && currentFileNames[a].equals(location)) {
                    Float progress = (Float) args[1];
                    radialProgressViews[a].setProgress(progress, true);
                }
            }
        } else if (id == NotificationCenter.userPhotosLoaded) {
            int guid = (Integer) args[4];
            int uid = (Integer) args[0];
            if (avatarsUserId == uid && classGuid == guid) {
                boolean fromCache = (Boolean) args[3];

                int setToImage = -1;
                ArrayList<TLRPC.Photo> photos = (ArrayList<TLRPC.Photo>) args[5];
                if (photos.isEmpty()) {
                    return;
                }
                imagesArrLocations.clear();
                imagesArrLocationsSizes.clear();
                avatarsArr.clear();
                for (TLRPC.Photo photo : photos) {
                    if (photo == null || photo instanceof TLRPC.TL_photoEmpty || photo.sizes == null) {
                        continue;
                    }
                    TLRPC.PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 640);
                    if (sizeFull != null) {
                        if (currentFileLocation != null) {
                            for (TLRPC.PhotoSize size : photo.sizes) {
                                if (size.location.local_id == currentFileLocation.local_id && size.location.volume_id == currentFileLocation.volume_id) {
                                    setToImage = imagesArrLocations.size();
                                    break;
                                }
                            }
                        }
                        imagesArrLocations.add(sizeFull.location);
                        imagesArrLocationsSizes.add(sizeFull.size);
                        avatarsArr.add(photo);
                    }
                }
                if (!avatarsArr.isEmpty()) {
//                    menuItem.showSubItem(gallery_menu_delete);
                } else {
//                    menuItem.hideSubItem(gallery_menu_delete);
                }
                needSearchImageInArr = false;
                currentIndex = -1;
                if (setToImage != -1) {
                    setImageIndex(setToImage, true);
                } else {
                    avatarsArr.add(0, new TLRPC.TL_photoEmpty());
                    imagesArrLocations.add(0, currentFileLocation);
                    imagesArrLocationsSizes.add(0, 0);
                    setImageIndex(0, true);
                }
                if (fromCache) {
//                    MessagesController.getInstance().loadUserPhotos(avatarsUserId, 0, 80, 0, false, classGuid);
                }
            }
        } else if (id == NotificationCenter.mediaCountDidLoaded) {
            long uid = (Long) args[0];
            if (uid == currentDialogId || uid == mergeDialogId) {
                if (uid == currentDialogId) {
                    totalImagesCount = (Integer) args[1];
                    /*if ((Boolean) args[2]) {
                        SharedMediaQuery.getMediaCount(currentDialogId, SharedMediaQuery.MEDIA_PHOTOVIDEO, classGuid, false);
                    }*/
                } else if (uid == mergeDialogId) {
                    totalImagesCountMerge = (Integer) args[1];
                    /*if ((Boolean) args[2]) {
                        SharedMediaQuery.getMediaCount(mergeDialogId, SharedMediaQuery.MEDIA_PHOTOVIDEO, classGuid, false);
                    }*/
                }
                if (needSearchImageInArr && isFirstLoading) {
                    isFirstLoading = false;
                    loadingMoreImages = true;
//                    SharedMediaQuery.loadMedia(currentDialogId, 0, 80, 0, SharedMediaQuery.MEDIA_PHOTOVIDEO, true, classGuid);
                } else if (!imagesArr.isEmpty()) {
//                    if (opennedFromMedia) {
//                        actionBar.setTitle(LocaleController.formatString("Of", R.string.Of, currentIndex + 1, totalImagesCount + totalImagesCountMerge));
//                    } else {
//                        actionBar.setTitle(LocaleController.formatString("Of", R.string.Of, (totalImagesCount + totalImagesCountMerge - imagesArr.size()) + currentIndex + 1, totalImagesCount + totalImagesCountMerge));
//                    }
                }
            }
        } else if (id == NotificationCenter.mediaDidLoaded) {
            long uid = (Long) args[0];
            int guid = (Integer) args[3];
            if ((uid == currentDialogId || uid == mergeDialogId) && guid == classGuid) {
                loadingMoreImages = false;
                int loadIndex = uid == currentDialogId ? 0 : 1;
                ArrayList<Object> arr = (ArrayList<Object>) args[2];
                endReached[loadIndex] = (Boolean) args[5];
            }
        } else if (id == NotificationCenter.emojiDidLoaded) {
            if (captionTextView != null) {
                captionTextView.invalidate();
            }
        }
    }

    public void setParentActivity(final Activity activity) {
        if (parentActivity == activity) {
            return;
        }
        parentActivity = activity;

        if (progressDrawables == null) {
            progressDrawables = new Drawable[4];
        }

        scroller = new Scroller(activity);

        windowView = new FrameLayoutTouchListener(activity) {

            @Override
            public boolean dispatchKeyEventPreIme(KeyEvent event) {
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    PhotoViewerList.getInstance().closePhoto(true, false);
                    return true;
                }
                return super.dispatchKeyEventPreIme(event);
            }
        };
        windowView.setBackgroundDrawable(backgroundDrawable);
        windowView.setFocusable(false);

        animatingImageView = new ClippingImageView(activity);
        animatingImageView.setAnimationValues(animationValues);
        windowView.addView(animatingImageView, LayoutHelper.createFrame(40, 40));

        containerView = new FrameLayoutDrawer(activity);
        containerView.setFocusable(false);
        windowView.addView(containerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        windowLayoutParams = new WindowManager.LayoutParams();
        windowLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        windowLayoutParams.format = PixelFormat.TRANSLUCENT;
        windowLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        windowLayoutParams.gravity = Gravity.TOP;
        windowLayoutParams.type = WindowManager.LayoutParams.LAST_APPLICATION_WINDOW;
        windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        fwindowLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        fwindowLayoutParams.gravity = Gravity.TOP;

        bottomLayout = new FrameLayout(parentActivity);
        bottomLayout.setBackgroundColor(0x7f000000);
        containerView.addView(bottomLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.LEFT));

        captionTextViewOld = new TextView(parentActivity);
        captionTextViewOld.setMaxLines(10);
        captionTextViewOld.setBackgroundColor(0x7f000000);
        captionTextViewOld.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        captionTextViewOld.setLinkTextColor(0xffffffff);
        captionTextViewOld.setTextColor(0xffffffff);
        captionTextViewOld.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        captionTextViewOld.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        captionTextViewOld.setVisibility(View.INVISIBLE);
        containerView.addView(captionTextViewOld, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 0, 0, 0, 48));

        captionTextView = captionTextViewNew = new TextView(parentActivity);
        captionTextViewNew.setMaxLines(10);
        captionTextViewNew.setBackgroundColor(0x7f000000);
        captionTextViewNew.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        captionTextViewNew.setLinkTextColor(0xffffffff);
        captionTextViewNew.setTextColor(0xffffffff);
        captionTextViewNew.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        captionTextViewNew.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        captionTextViewNew.setVisibility(View.INVISIBLE);
        containerView.addView(captionTextViewNew, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 0, 0, 0, 48));

        radialProgressViews[0] = new RadialProgressView(containerView.getContext(), containerView);
        radialProgressViews[0].setBackgroundState(0, false);
        radialProgressViews[1] = new RadialProgressView(containerView.getContext(), containerView);
        radialProgressViews[1].setBackgroundState(0, false);
        radialProgressViews[2] = new RadialProgressView(containerView.getContext(), containerView);
        radialProgressViews[2].setBackgroundState(0, false);

        shareButton = new ImageView(containerView.getContext());
        shareButton.setScaleType(ImageView.ScaleType.CENTER);
        bottomLayout.addView(shareButton, LayoutHelper.createFrame(50, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.RIGHT));
        nameTextView = new TextView(containerView.getContext());
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);

        nameTextView.setSingleLine(true);
        nameTextView.setMaxLines(1);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        nameTextView.setTextColor(0xffffffff);
        nameTextView.setGravity(Gravity.LEFT);
        bottomLayout.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 16, 5, 60, 0));

        dateTextView = new TextView(containerView.getContext());
        dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        dateTextView.setSingleLine(true);
        dateTextView.setMaxLines(1);
        dateTextView.setEllipsize(TextUtils.TruncateAt.END);
        dateTextView.setTextColor(0xffffffff);
        dateTextView.setGravity(Gravity.LEFT);
        bottomLayout.addView(dateTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 16, 25, 50, 0));

        gestureDetector = new GestureDetector(containerView.getContext(), this);
        gestureDetector.setOnDoubleTapListener(this);

        centerImage.setParentView(containerView);
        centerImage.setCrossfadeAlpha((byte) 2);
        centerImage.setInvalidateAll(true);
        leftImage.setParentView(containerView);
        leftImage.setCrossfadeAlpha((byte) 2);
        leftImage.setInvalidateAll(true);
        rightImage.setParentView(containerView);
        rightImage.setCrossfadeAlpha((byte) 2);
        rightImage.setInvalidateAll(true);

        WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();

        checkImageView = new CheckBox(containerView.getContext(), R.drawable.nophotos);
        checkImageView.setDrawBackground(true);
        checkImageView.setSize(45);
        checkImageView.setCheckOffset(AndroidUtilities.dp(1));
        checkImageView.setColor(0xff3ccaef);
        checkImageView.setVisibility(View.GONE);
        containerView.addView(checkImageView, LayoutHelper.createFrame(45, 45, Gravity.RIGHT | Gravity.TOP, 0, rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90 ? 58 : 68, 10, 0));
        checkImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (placeProvider != null) {
                    placeProvider.setPhotoChecked(currentIndex);
                    checkImageView.setChecked(placeProvider.isPhotoChecked(currentIndex), true);
                    updateSelectedCount();
                }
            }
        });
    }

    private void switchToEditMode(final int mode) {
        if (currentEditMode == mode || centerImage.getBitmap() == null || changeModeAnimation != null || imageMoveAnimation != null || radialProgressViews[0].backgroundState != -1) {
            return;
        }
        if (mode == 0) {
            Bitmap bitmap = centerImage.getBitmap();
            if (bitmap != null) {
                int bitmapWidth = centerImage.getBitmapWidth();
                int bitmapHeight = centerImage.getBitmapHeight();

                float scaleX = (float) getContainerViewWidth() / (float) bitmapWidth;
                float scaleY = (float) getContainerViewHeight() / (float) bitmapHeight;
                float newScaleX = (float) getContainerViewWidth(0) / (float) bitmapWidth;
                float newScaleY = (float) getContainerViewHeight(0) / (float) bitmapHeight;
                float scale = scaleX > scaleY ? scaleY : scaleX;
                float newScale = newScaleX > newScaleY ? newScaleY : newScaleX;

                animateToScale = newScale / scale;
                animateToX = 0;
                if (currentEditMode == 1) {
                    animateToY = AndroidUtilities.dp(24);
                } else if (currentEditMode == 2) {
                    animateToY = AndroidUtilities.dp(62);
                }
                animationStartTime = System.currentTimeMillis();
                zoomAnimation = true;
            }

            imageMoveAnimation = new AnimatorSet();
            if (currentEditMode == 1) {
                imageMoveAnimation.playTogether(
                        ObjectAnimator.ofFloat(PhotoViewerList.this, "animationValue", 0, 1),
                        ObjectAnimator.ofFloat(photoCropView, "alpha", 0)
                );
            } else if (currentEditMode == 2) {
            }
            imageMoveAnimation.setDuration(200);
            imageMoveAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (currentEditMode == 1) {
                        photoCropView.clearAnimation();
                        photoCropView.setVisibility(View.GONE);
                    } else if (currentEditMode == 2) {
                        containerView.removeView(photoFilterView);
                        photoFilterView = null;
                    }
                    imageMoveAnimation = null;
                    currentEditMode = mode;
                    animateToScale = 1;
                    animateToX = 0;
                    animateToY = 0;
                    scale = 1;
                    updateMinMax(scale);
                    containerView.invalidate();

                    AnimatorSet animatorSet = new AnimatorSet();
                    ArrayList<Animator> arrayList = new ArrayList<>();
                    arrayList.add(ObjectAnimator.ofFloat(actionBar, "translationY", 0));
                    if (needCaptionLayout) {
                        arrayList.add(ObjectAnimator.ofFloat(captionTextView, "translationY", 0));
                    }
                    if (sendPhotoType == 0) {
                        arrayList.add(ObjectAnimator.ofFloat(checkImageView, "alpha", 1));
                    }
                    animatorSet.playTogether(arrayList);
                    animatorSet.setDuration(200);
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            if (needCaptionLayout) {
                                captionTextView.setVisibility(captionTextView.getTag() != null ? View.VISIBLE : View.INVISIBLE);
                            }
                            if (sendPhotoType == 0) {
                                checkImageView.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (needCaptionLayout) {
                                captionTextView.clearAnimation();
                            }
                            if (sendPhotoType == 0) {
                                checkImageView.clearAnimation();
                            }
                        }
                    });
                    animatorSet.start();
                }
            });
            imageMoveAnimation.start();
        } else if (mode == 1) {
            if (photoCropView == null) {
                photoCropView = new PhotoCropView(parentActivity);
                photoCropView.setVisibility(View.GONE);
                containerView.addView(photoCropView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 0, 0, 48));
                photoCropView.setDelegate(new PhotoCropView.PhotoCropViewDelegate() {
                    @Override
                    public void needMoveImageTo(float x, float y, float s, boolean animated) {
                        if (animated) {
                            animateTo(s, x, y, true);
                        } else {
                            translationX = x;
                            translationY = y;
                            scale = s;
                            containerView.invalidate();
                        }
                    }
                });
            }

            changeModeAnimation = new AnimatorSet();
            ArrayList<Animator> arrayList = new ArrayList<>();
            arrayList.add(ObjectAnimator.ofFloat(actionBar, "translationY", 0, -actionBar.getHeight()));
            if (needCaptionLayout) {
                arrayList.add(ObjectAnimator.ofFloat(captionTextView, "translationY", 0, AndroidUtilities.dp(96)));
            }
            if (sendPhotoType == 0) {
                arrayList.add(ObjectAnimator.ofFloat(checkImageView, "alpha", 1, 0));
            }
            changeModeAnimation.playTogether(arrayList);
            changeModeAnimation.setDuration(200);
            changeModeAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    changeModeAnimation = null;
                    if (needCaptionLayout) {
                        captionTextView.clearAnimation();
                        captionTextView.setVisibility(View.INVISIBLE);
                    }
                    if (sendPhotoType == 0) {
                        checkImageView.clearAnimation();
                        checkImageView.setVisibility(View.GONE);
                    }

                    Bitmap bitmap = centerImage.getBitmap();
                    if (bitmap != null) {
                        photoCropView.setBitmap(bitmap, centerImage.getOrientation(), sendPhotoType != 1);
                        int bitmapWidth = centerImage.getBitmapWidth();
                        int bitmapHeight = centerImage.getBitmapHeight();

                        float scaleX = (float) getContainerViewWidth() / (float) bitmapWidth;
                        float scaleY = (float) getContainerViewHeight() / (float) bitmapHeight;
                        float newScaleX = (float) getContainerViewWidth(1) / (float) bitmapWidth;
                        float newScaleY = (float) getContainerViewHeight(1) / (float) bitmapHeight;
                        float scale = scaleX > scaleY ? scaleY : scaleX;
                        float newScale = newScaleX > newScaleY ? newScaleY : newScaleX;

                        animateToScale = newScale / scale;
                        animateToX = 0;
                        animateToY = -AndroidUtilities.dp(24);
                        animationStartTime = System.currentTimeMillis();
                        zoomAnimation = true;
                    }

                    imageMoveAnimation = new AnimatorSet();
                    imageMoveAnimation.playTogether(
                            ObjectAnimator.ofFloat(PhotoViewerList.this, "animationValue", 0, 1),
                            ObjectAnimator.ofFloat(photoCropView, "alpha", 0, 1)
                    );
                    imageMoveAnimation.setDuration(200);
                    imageMoveAnimation.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            photoCropView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            imageMoveAnimation = null;
                            currentEditMode = mode;
                            animateToScale = 1;
                            animateToX = 0;
                            animateToY = 0;
                            scale = 1;
                            updateMinMax(scale);
                            containerView.invalidate();
                            photoCropView.clearAnimation();
                        }
                    });
                    imageMoveAnimation.start();
                }
            });
            changeModeAnimation.start();
        } else if (mode == 2) {
        }
    }

    private void toggleActionBar(boolean show, final boolean animated) {
        if (show) {
            if (canShowBottom) {
                bottomLayout.setVisibility(View.VISIBLE);
                if (captionTextView.getTag() != null) {
                    captionTextView.setVisibility(View.VISIBLE);
                }
            }
        }
        isActionBarVisible = show;
        bottomLayout.setEnabled(show);
        if (animated) {
            ArrayList<Animator> arrayList = new ArrayList<>();
            arrayList.add(ObjectAnimator.ofFloat(actionBar, "alpha", show ? 1.0f : 0.0f));
            arrayList.add(ObjectAnimator.ofFloat(bottomLayout, "alpha", show ? 1.0f : 0.0f));
            if (captionTextView.getTag() != null) {
                arrayList.add(ObjectAnimator.ofFloat(captionTextView, "alpha", show ? 1.0f : 0.0f));
            }
            currentActionBarAnimation = new AnimatorSet();
            currentActionBarAnimation.playTogether(arrayList);
            if (!show) {
                currentActionBarAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (currentActionBarAnimation != null && currentActionBarAnimation.equals(animation)) {
//                            actionBar.setVisibility(View.GONE);
                            if (canShowBottom) {
                                bottomLayout.clearAnimation();
                                bottomLayout.setVisibility(View.GONE);
                                if (captionTextView.getTag() != null) {
                                    captionTextView.clearAnimation();
                                    captionTextView.setVisibility(View.INVISIBLE);
                                }
                            }
                            currentActionBarAnimation = null;
                        }
                    }
                });
            }

            currentActionBarAnimation.setDuration(200);
            currentActionBarAnimation.start();
        } else {
            ViewProxy.setAlpha(bottomLayout, show ? 1.0f : 0.0f);
            if (captionTextView.getTag() != null) {
                ViewProxy.setAlpha(captionTextView, show ? 1.0f : 0.0f);
            }
            if (!show) {
                if (canShowBottom) {
                    bottomLayout.clearAnimation();
                    bottomLayout.setVisibility(View.GONE);
                    if (captionTextView.getTag() != null) {
                        captionTextView.clearAnimation();
                        captionTextView.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }
    }

    private String getFileName(int index) {
        if (index < 0) {
            return null;
        }
        if (!imagesArrLocals.isEmpty()) {
            if (index >= imagesArrLocals.size()) {
                return null;
            }
            Object object = imagesArrLocals.get(index);
            if (object instanceof AndroidUtilities.SearchImage) {
                AndroidUtilities.SearchImage searchImage = ((AndroidUtilities.SearchImage) object);
                if (searchImage.document != null) {
                    return FileLoader.getAttachFileName(searchImage.document);
                } else if (searchImage.type != 1 && searchImage.localUrl != null && searchImage.localUrl.length() > 0) {
                    File file = new File(searchImage.localUrl);
                    if (file.exists()) {
                        return file.getName();
                    } else {
                        searchImage.localUrl = "";
                    }
                }
                return Utilities.MD5(searchImage.imageUrl) + "." + ImageLoader.getHttpUrlExtension(searchImage.imageUrl);
            }
        }
        return null;
    }

    private void updateSelectedCount() {
        if (placeProvider == null) {
            return;
        }
    }

    private void onPhotoShow(final Object messageObject,
                             final TLRPC.FileLocation fileLocation,
                             final ArrayList<Object> messages,
                             final ArrayList<Object> photos,
                             int index,
                             final PhotoViewer.PlaceProviderObject object) {
        classGuid = ConnectionsManager.getInstance().generateClassGuid();
        currentObject = null;
        currentFileLocation = null;
        currentPathObject = null;
        currentIndex = -1;
        currentFileNames[0] = null;
        currentFileNames[1] = null;
        currentFileNames[2] = null;
        avatarsUserId = 0;
        totalImagesCount = 0;
        totalImagesCountMerge = 0;
        currentEditMode = 0;
        isFirstLoading = true;
        needSearchImageInArr = false;
        loadingMoreImages = false;
        endReached[0] = false;
        endReached[1] = mergeDialogId == 0;
        opennedFromMedia = false;
        needCaptionLayout = false;
        canShowBottom = true;
        imagesArr.clear();
        imagesArrLocations.clear();
        imagesArrLocationsSizes.clear();
        avatarsArr.clear();
        imagesArrLocals.clear();
        for (int a = 0; a < 2; a++) {
            imagesByIds[a].clear();
            imagesByIdsTemp[a].clear();
        }
        imagesArrTemp.clear();
        currentUserAvatarLocation = null;
        containerView.setPadding(0, 0, 0, 0);
        currentThumb = object != null ? object.thumb : null;
        bottomLayout.setVisibility(View.VISIBLE);
        shareButton.setVisibility(View.GONE);
        ViewProxy.setAlpha(checkImageView, 1.0f);
        checkImageView.clearAnimation();
        checkImageView.setVisibility(View.GONE);
        captionTextView.setTag(null);
        captionTextView.clearAnimation();
        captionTextView.setVisibility(View.INVISIBLE);
        if (photoCropView != null) {
            photoCropView.clearAnimation();
            photoCropView.setVisibility(View.GONE);
        }
        if (photoFilterView != null) {
            photoFilterView.clearAnimation();
            photoFilterView.setVisibility(View.GONE);
        }

        for (int a = 0; a < 3; a++) {
            if (radialProgressViews[a] != null) {
                radialProgressViews[a].setBackgroundState(-1, false);
            }
        }

        if (photos != null) {
            if (sendPhotoType == 0) {
                checkImageView.setVisibility(View.VISIBLE);
            }
            imagesArrLocals.addAll(photos);
            setImageIndex(index, true);
            bottomLayout.clearAnimation();
            bottomLayout.setVisibility(View.GONE);
            canShowBottom = false;
            updateSelectedCount();
        }
    }

    private void setImages() {
        if (animationInProgress == 0) {
            setIndexToImage(centerImage, currentIndex);
            setIndexToImage(rightImage, currentIndex + 1);
            setIndexToImage(leftImage, currentIndex - 1);
        }
    }

    public void setImageIndex(int index, boolean init) {
        if (currentIndex == index) {
            return;
        }
        if (!init) {
            currentThumb = null;
        }
        currentFileNames[0] = getFileName(index);
        currentFileNames[1] = getFileName(index + 1);
        currentFileNames[2] = getFileName(index - 1);
        placeProvider.willSwitchFromPhoto(currentObject, currentFileLocation, currentIndex);
        int prevIndex = currentIndex;
        currentIndex = index;

        boolean sameImage = false;

        if (!imagesArr.isEmpty()) {
            if (currentIndex < 0 || currentIndex >= imagesArr.size()) {
                closePhoto(false, false);
                return;
            }
            currentObject = imagesArr.get(currentIndex);
        }


        if (currentPlaceObject != null) {
            if (animationInProgress == 0) {
                currentPlaceObject.imageReceiver.setVisible(true, true);
            } else {
                showAfterAnimation = currentPlaceObject;
            }
        }
        currentPlaceObject = placeProvider.getPlaceForPhoto(currentObject, currentFileLocation, currentIndex);
        if (currentPlaceObject != null) {
            if (animationInProgress == 0) {
                currentPlaceObject.imageReceiver.setVisible(false, true);
            } else {
                hideAfterAnimation = currentPlaceObject;
            }
        }

        if (!sameImage) {
            draggingDown = false;
            translationX = 0;
            translationY = 0;
            scale = 1;
            animateToX = 0;
            animateToY = 0;
            animateToScale = 1;
            animationStartTime = 0;
            imageMoveAnimation = null;
            changeModeAnimation = null;

            pinchStartDistance = 0;
            pinchStartScale = 1;
            pinchCenterX = 0;
            pinchCenterY = 0;
            pinchStartX = 0;
            pinchStartY = 0;
            moveStartX = 0;
            moveStartY = 0;
            zooming = false;
            moving = false;
            doubleTap = false;
            invalidCoords = false;
            canDragDown = true;
            changingPage = false;
            switchImageAfterAnimation = 0;
            canZoom = !imagesArrLocals.isEmpty() || (currentFileNames[0] != null && !currentFileNames[0].endsWith("mp4") && radialProgressViews[0].backgroundState != 0);
            updateMinMax(scale);
        }

        if (prevIndex == -1) {
            setImages();

            for (int a = 0; a < 3; a++) {
                checkProgress(a, false);
            }
        } else {
            checkProgress(0, false);
            if (prevIndex > currentIndex) {
                ImageReceiver temp = rightImage;
                rightImage = centerImage;
                centerImage = leftImage;
                leftImage = temp;

                RadialProgressView tempProgress = radialProgressViews[0];
                radialProgressViews[0] = radialProgressViews[2];
                radialProgressViews[2] = tempProgress;
                setIndexToImage(leftImage, currentIndex - 1);

                checkProgress(1, false);
                checkProgress(2, false);
            } else if (prevIndex < currentIndex) {
                ImageReceiver temp = leftImage;
                leftImage = centerImage;
                centerImage = rightImage;
                rightImage = temp;

                RadialProgressView tempProgress = radialProgressViews[0];
                radialProgressViews[0] = radialProgressViews[1];
                radialProgressViews[1] = tempProgress;
                setIndexToImage(rightImage, currentIndex + 1);

                checkProgress(1, false);
                checkProgress(2, false);
            }
        }
    }

    private void checkProgress(int a, boolean animated) {
        if (currentFileNames[a] != null) {
            int index = currentIndex;
            if (a == 1) {
                index += 1;
            } else if (a == 2) {
                index -= 1;
            }
            File f = null;
            if (a == 0) {
                canZoom = !imagesArrLocals.isEmpty() || (currentFileNames[0] != null && !currentFileNames[0].endsWith("mp4") && radialProgressViews[0].backgroundState != 0);
            }
        } else {
            radialProgressViews[a].setBackgroundState(-1, animated);
        }
    }

    private void setIndexToImage(ImageReceiver imageReceiver, int index) {
        imageReceiver.setOrientation(0, false);
        if (!imagesArrLocals.isEmpty()) {
            imageReceiver.setParentObject(null);
            if (index >= 0 && index < imagesArrLocals.size()) {
                Object object = imagesArrLocals.get(index);
                int size = (int) (AndroidUtilities.getPhotoSize() / AndroidUtilities.density);
                Bitmap placeHolder = null;
                if (currentThumb != null && imageReceiver == centerImage) {
                    placeHolder = currentThumb;
                }
                if (placeHolder == null) {
                    placeHolder = placeProvider.getThumbForPhoto(null, null, index);
                }
                String path = null;
                TLRPC.Document document = null;
                int imageSize = 0;
                String filter = null;
                if (object instanceof AndroidUtilities.PhotoEntry) {
                    AndroidUtilities.PhotoEntry photoEntry = (AndroidUtilities.PhotoEntry) object;
                    if (photoEntry.imagePath != null) {
                        path = photoEntry.imagePath;
                    } else {
                        imageReceiver.setOrientation(photoEntry.orientation, false);
                        path = photoEntry.path;
                    }
                    filter = String.format(Locale.US, "%d_%d", size, size);
                } else if (object instanceof AndroidUtilities.SearchImage) {
                    AndroidUtilities.SearchImage photoEntry = (AndroidUtilities.SearchImage) object;
                    if (photoEntry.imagePath != null) {
                        path = photoEntry.imagePath;
                    } else if (photoEntry.document != null) {
                        document = photoEntry.document;
                        imageSize = photoEntry.document.size;
                    } else {
                        path = photoEntry.imageUrl;
                        imageSize = photoEntry.size;
                    }
                    filter = "d";
                }
                if (document != null) {
                    imageReceiver.setImage(document, null, "d", placeHolder != null ? new BitmapDrawable(null, placeHolder) : null, placeHolder == null ? document.thumb.location : null, String.format(Locale.US, "%d_%d", size, size), imageSize, null, false);
                } else {
                    imageReceiver.setImage(path, filter, placeHolder != null ? new BitmapDrawable(null, placeHolder) : null, null, imageSize);
                }
            } else {
                imageReceiver.setImageBitmap((Bitmap) null);
            }
        }
    }

    public void openPhotoForSelect(final ArrayList<Object> photos, final int index, int type, final PhotoViewer.PhotoViewerProvider provider, Activity chatActivity) {
        sendPhotoType = type;
        openPhoto(null, null, null, photos, index, provider, chatActivity, 0, 0);
    }

    public void openPhotoForSelect(final ArrayList<Object> photos, final int index, int type, final PhotoViewer.PhotoViewerProvider provider, Activity chatActivity, ViewGroup parentToAdd) {
        sendPhotoType = type;
        openPhoto(null, null, null, photos, index, provider, chatActivity, 0, 0, parentToAdd);
    }

    private boolean checkAnimation() {
        if (animationInProgress != 0) {
            if (Math.abs(transitionAnimationStartTime - System.currentTimeMillis()) >= 500) {
                if (animationEndRunnable != null) {
                    animationEndRunnable.run();
                    animationEndRunnable = null;
                }
                animationInProgress = 0;
            }
        }
        return animationInProgress != 0;
    }

    public void openPhoto(final Object messageObject, final TLRPC.FileLocation fileLocation, final ArrayList<Object> messages, final ArrayList<Object> photos, final int index, final PhotoViewer.PhotoViewerProvider provider, Activity chatActivity, long dialogId, long mDialogId) {
        if (parentActivity == null || isVisible || provider == null && checkAnimation() || messageObject == null && fileLocation == null && messages == null && photos == null) {
            return;
        }

        final PhotoViewer.PlaceProviderObject object = provider.getPlaceForPhoto(messageObject, fileLocation, index);
        if (object == null && photos == null) {
            return;
        }

        WindowManager wm = (WindowManager) parentActivity.getSystemService(Context.WINDOW_SERVICE);
        if (windowView.attachedToWindow) {
            try {
                wm.removeView(windowView);
            } catch (Exception e) {
            }
        }

        try {
            windowLayoutParams.type = WindowManager.LayoutParams.LAST_APPLICATION_WINDOW;
            windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            windowLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED;
            windowView.setFocusable(false);
            containerView.setFocusable(false);
            wm.addView(windowView, windowLayoutParams);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
            return;
        }

        parentChatActivity = chatActivity;
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidFailedLoad);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileLoadProgressChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.mediaCountDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.mediaDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.userPhotosLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);

        placeProvider = provider;
        mergeDialogId = mDialogId;
        currentDialogId = dialogId;

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }

        isVisible = true;
        toggleActionBar(true, false);

        if (object != null) {
            disableShowCheck = true;
            animationInProgress = 1;
            onPhotoShow(messageObject, fileLocation, messages, photos, index, object);

            final Rect drawRegion = object.imageReceiver.getDrawRegion();
            int orientation = object.imageReceiver.getOrientation();

            animatingImageView.setVisibility(View.VISIBLE);
            animatingImageView.setRadius(object.radius);
            animatingImageView.setOrientation(orientation);
            animatingImageView.setNeedRadius(object.radius != 0);
            animatingImageView.setImageBitmap(object.thumb);

            ViewProxy.setAlpha(animatingImageView, 1.0f);
            ViewProxy.setPivotX(animatingImageView, 0.0f);
            ViewProxy.setPivotY(animatingImageView, 0.0f);
            ViewProxy.setScaleX(animatingImageView, object.scale);
            ViewProxy.setScaleY(animatingImageView, object.scale);
            ViewProxy.setTranslationX(animatingImageView, object.viewX + drawRegion.left * object.scale);
            ViewProxy.setTranslationY(animatingImageView, object.viewY + drawRegion.top * object.scale);
            final ViewGroup.LayoutParams layoutParams = animatingImageView.getLayoutParams();
            layoutParams.width = (drawRegion.right - drawRegion.left);
            layoutParams.height = (drawRegion.bottom - drawRegion.top);
            animatingImageView.setLayoutParams(layoutParams);

            float scaleX = (float) AndroidUtilities.displaySize.x / layoutParams.width;
            float scaleY = (float) (AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight) / layoutParams.height;
            float scale = scaleX > scaleY ? scaleY : scaleX;
            float width = layoutParams.width * scale;
            float height = layoutParams.height * scale;
            float xPos = (AndroidUtilities.displaySize.x - width) / 2.0f;
            float yPos = (AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight - height) / 2.0f;
            int clipHorizontal = Math.abs(drawRegion.left - object.imageReceiver.getImageX());
            int clipVertical = Math.abs(drawRegion.top - object.imageReceiver.getImageY());

            int coords2[] = new int[2];
            object.parentView.getLocationInWindow(coords2);
            int clipTop = coords2[1] - AndroidUtilities.statusBarHeight - (object.viewY + drawRegion.top) + object.clipTopAddition;
            if (clipTop < 0) {
                clipTop = 0;
            }
            int clipBottom = (object.viewY + drawRegion.top + layoutParams.height) - (coords2[1] + object.parentView.getHeight() - AndroidUtilities.statusBarHeight) + object.clipBottomAddition;
            if (clipBottom < 0) {
                clipBottom = 0;
            }
            clipTop = Math.max(clipTop, clipVertical);
            clipBottom = Math.max(clipBottom, clipVertical);

            animationValues[0][0] = ViewProxy.getScaleX(animatingImageView);
            animationValues[0][1] = ViewProxy.getScaleY(animatingImageView);
            animationValues[0][2] = ViewProxy.getTranslationX(animatingImageView);
            animationValues[0][3] = ViewProxy.getTranslationY(animatingImageView);
            animationValues[0][4] = clipHorizontal * object.scale;
            animationValues[0][5] = clipTop * object.scale;
            animationValues[0][6] = clipBottom * object.scale;
            animationValues[0][7] = animatingImageView.getRadius();

            animationValues[1][0] = scale;
            animationValues[1][1] = scale;
            animationValues[1][2] = xPos;
            animationValues[1][3] = yPos;
            animationValues[1][4] = 0;
            animationValues[1][5] = 0;
            animationValues[1][6] = 0;
            animationValues[1][7] = 0;

            animatingImageView.setAnimationProgress(0);
            backgroundDrawable.setAlpha(0);
            ViewProxy.setAlpha(containerView, 0);

            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(animatingImageView, "animationProgress", 0.0f, 1.0f),
                    ObjectAnimator.ofInt(backgroundDrawable, "alpha", 0, 255),
                    ObjectAnimator.ofFloat(containerView, "alpha", 0.0f, 1.0f)
            );

            animationEndRunnable = new Runnable() {
                @Override
                public void run() {
                    if (containerView == null) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 18) {
                        containerView.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                    animationInProgress = 0;
                    transitionAnimationStartTime = 0;
                    setImages();
                    containerView.invalidate();
                    animatingImageView.setVisibility(View.GONE);
                    if (showAfterAnimation != null) {
                        showAfterAnimation.imageReceiver.setVisible(true, true);
                    }
                    if (hideAfterAnimation != null) {
                        hideAfterAnimation.imageReceiver.setVisible(false, true);
                    }
                    if (photos != null) {
                        windowLayoutParams.flags = 0;
                        windowLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                        WindowManager wm = (WindowManager) parentActivity.getSystemService(Context.WINDOW_SERVICE);
                        wm.updateViewLayout(windowView, windowLayoutParams);
                        windowView.setFocusable(true);
                        containerView.setFocusable(true);
                    }
                }
            };

            animatorSet.setDuration(200);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().setAnimationInProgress(false);
                            if (animationEndRunnable != null) {
                                animationEndRunnable.run();
                                animationEndRunnable = null;
                            }
                        }
                    });
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    onAnimationEnd(animation);
                }
            });
            transitionAnimationStartTime = System.currentTimeMillis();
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().setAnimationInProgress(true);
                    animatorSet.start();
                }
            });
            if (Build.VERSION.SDK_INT >= 18) {
                containerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            backgroundDrawable.drawRunnable = new Runnable() {
                @Override
                public void run() {
                    disableShowCheck = false;
                    object.imageReceiver.setVisible(false, true);
                }
            };
        } else {
            if (photos != null) {
                windowLayoutParams.flags = 0;
                windowLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                wm.updateViewLayout(windowView, windowLayoutParams);
                windowView.setFocusable(true);
                containerView.setFocusable(true);
            }

            backgroundDrawable.setAlpha(255);
            ViewProxy.setAlpha(containerView, 1.0f);
            onPhotoShow(messageObject, fileLocation, messages, photos, index, object);
        }
    }

    public void openPhoto(final Object messageObject, final TLRPC.FileLocation fileLocation, final ArrayList<Object> messages, final ArrayList<Object> photos, final int index, final PhotoViewer.PhotoViewerProvider provider, Activity chatActivity, long dialogId, long mDialogId, final ViewGroup parentToAdd) {
        if (parentActivity == null || isVisible || provider == null && checkAnimation() || messageObject == null && fileLocation == null && messages == null && photos == null) {
            return;
        }

        final PhotoViewer.PlaceProviderObject object = provider.getPlaceForPhoto(messageObject, fileLocation, index);
        if (object == null && photos == null) {
            return;
        }

//        WindowManager wm = (WindowManager) parentActivity.getSystemService(Context.WINDOW_SERVICE);
        if (windowView.attachedToWindow) {
            try {
                parentToAdd.removeView(windowView);
            } catch (Exception e) {
            }
        }

        try {
//            fwindowLayoutParams.type = WindowManager.LayoutParams.LAST_APPLICATION_WINDOW;
//            fwindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
//            fwindowLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED;
            windowView.setFocusable(false);
            containerView.setFocusable(false);
            parentToAdd.addView(windowView, fwindowLayoutParams);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
            return;
        }

        parentChatActivity = chatActivity;
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidFailedLoad);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileLoadProgressChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.mediaCountDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.mediaDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.userPhotosLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);

        placeProvider = provider;
        mergeDialogId = mDialogId;
        currentDialogId = dialogId;

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }

        isVisible = true;
        toggleActionBar(true, false);

        if (object != null) {
            disableShowCheck = true;
            animationInProgress = 1;
            onPhotoShow(messageObject, fileLocation, messages, photos, index, object);

            final Rect drawRegion = object.imageReceiver.getDrawRegion();
            int orientation = object.imageReceiver.getOrientation();

            animatingImageView.setVisibility(View.VISIBLE);
            animatingImageView.setRadius(object.radius);
            animatingImageView.setOrientation(orientation);
            animatingImageView.setNeedRadius(object.radius != 0);
            animatingImageView.setImageBitmap(object.thumb);

            ViewProxy.setAlpha(animatingImageView, 1.0f);
            ViewProxy.setPivotX(animatingImageView, 0.0f);
            ViewProxy.setPivotY(animatingImageView, 0.0f);
            ViewProxy.setScaleX(animatingImageView, object.scale);
            ViewProxy.setScaleY(animatingImageView, object.scale);
            ViewProxy.setTranslationX(animatingImageView, object.viewX + drawRegion.left * object.scale);
            ViewProxy.setTranslationY(animatingImageView, object.viewY + drawRegion.top * object.scale);
            final ViewGroup.LayoutParams layoutParams = animatingImageView.getLayoutParams();
            layoutParams.width = (drawRegion.right - drawRegion.left);
            layoutParams.height = (drawRegion.bottom - drawRegion.top);
            animatingImageView.setLayoutParams(layoutParams);

            float scaleX = (float) AndroidUtilities.displaySize.x / layoutParams.width;
            float scaleY = (float) (AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight) / layoutParams.height;
            float scale = scaleX > scaleY ? scaleY : scaleX;
            float width = layoutParams.width * scale;
            float height = layoutParams.height * scale;
            float xPos = (AndroidUtilities.displaySize.x - width) / 2.0f;
            float yPos = (AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight - height) / 2.0f;
            int clipHorizontal = Math.abs(drawRegion.left - object.imageReceiver.getImageX());
            int clipVertical = Math.abs(drawRegion.top - object.imageReceiver.getImageY());

            int coords2[] = new int[2];
            object.parentView.getLocationInWindow(coords2);
            int clipTop = coords2[1] - AndroidUtilities.statusBarHeight - (object.viewY + drawRegion.top) + object.clipTopAddition;
            if (clipTop < 0) {
                clipTop = 0;
            }
            int clipBottom = (object.viewY + drawRegion.top + layoutParams.height) - (coords2[1] + object.parentView.getHeight() - AndroidUtilities.statusBarHeight) + object.clipBottomAddition;
            if (clipBottom < 0) {
                clipBottom = 0;
            }
            clipTop = Math.max(clipTop, clipVertical);
            clipBottom = Math.max(clipBottom, clipVertical);

            animationValues[0][0] = ViewProxy.getScaleX(animatingImageView);
            animationValues[0][1] = ViewProxy.getScaleY(animatingImageView);
            animationValues[0][2] = ViewProxy.getTranslationX(animatingImageView);
            animationValues[0][3] = ViewProxy.getTranslationY(animatingImageView);
            animationValues[0][4] = clipHorizontal * object.scale;
            animationValues[0][5] = clipTop * object.scale;
            animationValues[0][6] = clipBottom * object.scale;
            animationValues[0][7] = animatingImageView.getRadius();

            animationValues[1][0] = scale;
            animationValues[1][1] = scale;
            animationValues[1][2] = xPos;
            animationValues[1][3] = yPos;
            animationValues[1][4] = 0;
            animationValues[1][5] = 0;
            animationValues[1][6] = 0;
            animationValues[1][7] = 0;

            animatingImageView.setAnimationProgress(0);
            backgroundDrawable.setAlpha(0);
            ViewProxy.setAlpha(containerView, 0);

            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(animatingImageView, "animationProgress", 0.0f, 1.0f),
                    ObjectAnimator.ofInt(backgroundDrawable, "alpha", 0, 255),
                    ObjectAnimator.ofFloat(containerView, "alpha", 0.0f, 1.0f)
            );

            animationEndRunnable = new Runnable() {
                @Override
                public void run() {
                    if (containerView == null) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 18) {
                        containerView.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                    animationInProgress = 0;
                    transitionAnimationStartTime = 0;
                    setImages();
                    containerView.invalidate();
                    animatingImageView.setVisibility(View.GONE);
                    if (showAfterAnimation != null) {
                        showAfterAnimation.imageReceiver.setVisible(true, true);
                    }
                    if (hideAfterAnimation != null) {
                        hideAfterAnimation.imageReceiver.setVisible(false, true);
                    }
                    if (photos != null) {
//                        fwindowLayoutParams.flags = 0;
//                        fwindowLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
//                        WindowManager wm = (WindowManager) parentActivity.getSystemService(Context.WINDOW_SERVICE);
                        parentToAdd.updateViewLayout(windowView, fwindowLayoutParams);
                        windowView.setFocusable(true);
                        containerView.setFocusable(true);
                    }
                }
            };

            animatorSet.setDuration(200);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().setAnimationInProgress(false);
                            if (animationEndRunnable != null) {
                                animationEndRunnable.run();
                                animationEndRunnable = null;
                            }
                        }
                    });
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    onAnimationEnd(animation);
                }
            });
            transitionAnimationStartTime = System.currentTimeMillis();
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().setAnimationInProgress(true);
                    animatorSet.start();
                }
            });
            if (Build.VERSION.SDK_INT >= 18) {
                containerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            backgroundDrawable.drawRunnable = new Runnable() {
                @Override
                public void run() {
                    disableShowCheck = false;
                    object.imageReceiver.setVisible(false, true);
                }
            };
        } else {
            if (photos != null) {
//                windowLayoutParams.flags = 0;
//                windowLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                parentToAdd.updateViewLayout(windowView, fwindowLayoutParams);
                windowView.setFocusable(true);
                containerView.setFocusable(true);
            }

            backgroundDrawable.setAlpha(255);
            ViewProxy.setAlpha(containerView, 1.0f);
            onPhotoShow(messageObject, fileLocation, messages, photos, index, object);
        }
    }

    public void closePhoto(boolean animated, boolean fromEditMode) {
        if (!fromEditMode && currentEditMode != 0) {
            if (currentEditMode == 1) {
                photoCropView.cancelAnimationRunnable();
            }
            switchToEditMode(0);
            return;
        }
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }

        if (currentEditMode != 0) {
            if (currentEditMode == 2) {
//                photoFilterView.shutdown();
                containerView.removeView(photoFilterView);
                photoFilterView = null;
            } else if (currentEditMode == 1) {
//                editorDoneLayout.setVisibility(View.GONE);
                photoCropView.setVisibility(View.GONE);
            }
            currentEditMode = 0;
        }

        if (parentActivity == null || !isVisible || checkAnimation() || placeProvider == null) {
            return;
        }

//        captionEditText.onDestroy();
        parentChatActivity = null;
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.FileDidFailedLoad);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.FileDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.FileLoadProgressChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.mediaCountDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.mediaDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.userPhotosLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
        ConnectionsManager.getInstance().cancelRequestsForGuid(classGuid);

        isActionBarVisible = false;

        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
        ConnectionsManager.getInstance().cancelRequestsForGuid(classGuid);

        final PhotoViewer.PlaceProviderObject object = placeProvider.getPlaceForPhoto(currentObject, currentFileLocation, currentIndex);

        if (animated) {
            animationInProgress = 1;
            animatingImageView.setVisibility(View.VISIBLE);
            containerView.invalidate();

            AnimatorSet animatorSet = new AnimatorSet();

            final ViewGroup.LayoutParams layoutParams = animatingImageView.getLayoutParams();
            Rect drawRegion = null;
            animatingImageView.setOrientation(centerImage.getOrientation());
            if (object != null) {
                animatingImageView.setNeedRadius(object.radius != 0);
                drawRegion = object.imageReceiver.getDrawRegion();
                layoutParams.width = drawRegion.right - drawRegion.left;
                layoutParams.height = drawRegion.bottom - drawRegion.top;
                animatingImageView.setImageBitmap(object.thumb);
            } else {
                animatingImageView.setNeedRadius(false);
                layoutParams.width = centerImage.getImageWidth();
                layoutParams.height = centerImage.getImageHeight();
                animatingImageView.setImageBitmap(centerImage.getBitmap());
            }
            animatingImageView.setLayoutParams(layoutParams);

            float scaleX = (float) AndroidUtilities.displaySize.x / layoutParams.width;
            float scaleY = (float) (AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight) / layoutParams.height;
            float scale2 = scaleX > scaleY ? scaleY : scaleX;
            float width = layoutParams.width * scale * scale2;
            float height = layoutParams.height * scale * scale2;
            float xPos = (AndroidUtilities.displaySize.x - width) / 2.0f;
            float yPos = (AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight - height) / 2.0f;
            ViewProxy.setTranslationX(animatingImageView, xPos + translationX);
            ViewProxy.setTranslationY(animatingImageView, yPos + translationY);
            ViewProxy.setScaleX(animatingImageView, scale * scale2);
            ViewProxy.setScaleY(animatingImageView, scale * scale2);

            if (object != null) {
                object.imageReceiver.setVisible(false, true);
                int clipHorizontal = Math.abs(drawRegion.left - object.imageReceiver.getImageX());
                int clipVertical = Math.abs(drawRegion.top - object.imageReceiver.getImageY());

                int coords2[] = new int[2];
                object.parentView.getLocationInWindow(coords2);
                int clipTop = coords2[1] - AndroidUtilities.statusBarHeight - (object.viewY + drawRegion.top) + object.clipTopAddition;
                if (clipTop < 0) {
                    clipTop = 0;
                }
                int clipBottom = (object.viewY + drawRegion.top + (drawRegion.bottom - drawRegion.top)) - (coords2[1] + object.parentView.getHeight() - AndroidUtilities.statusBarHeight) + object.clipBottomAddition;
                if (clipBottom < 0) {
                    clipBottom = 0;
                }

                clipTop = Math.max(clipTop, clipVertical);
                clipBottom = Math.max(clipBottom, clipVertical);

                animationValues[0][0] = ViewProxy.getScaleX(animatingImageView);
                animationValues[0][1] = ViewProxy.getScaleY(animatingImageView);
                animationValues[0][2] = ViewProxy.getTranslationX(animatingImageView);
                animationValues[0][3] = ViewProxy.getTranslationY(animatingImageView);
                animationValues[0][4] = 0;
                animationValues[0][5] = 0;
                animationValues[0][6] = 0;
                animationValues[0][7] = 0;

                animationValues[1][0] = object.scale;
                animationValues[1][1] = object.scale;
                animationValues[1][2] = object.viewX + drawRegion.left * object.scale;
                animationValues[1][3] = object.viewY + drawRegion.top * object.scale;
                animationValues[1][4] = clipHorizontal * object.scale;
                animationValues[1][5] = clipTop * object.scale;
                animationValues[1][6] = clipBottom * object.scale;
                animationValues[1][7] = object.radius;

                animatorSet.playTogether(
                        ObjectAnimator.ofFloat(animatingImageView, "animationProgress", 0.0f, 1.0f),
                        ObjectAnimator.ofInt(backgroundDrawable, "alpha", 0),
                        ObjectAnimator.ofFloat(containerView, "alpha", 0.0f)
                );
            } else {
                animatorSet.playTogether(
                        ObjectAnimator.ofInt(backgroundDrawable, "alpha", 0),
                        ObjectAnimator.ofFloat(animatingImageView, "alpha", 0.0f),
                        ObjectAnimator.ofFloat(animatingImageView, "translationY", translationY >= 0 ? AndroidUtilities.displaySize.y : -AndroidUtilities.displaySize.y),
                        ObjectAnimator.ofFloat(containerView, "alpha", 0.0f)
                );
            }

            animationEndRunnable = new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 18) {
                        containerView.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                    animationInProgress = 0;
                    onPhotoClosed(object);
                }
            };

            animatorSet.setDuration(200);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (animationEndRunnable != null) {
                                animationEndRunnable.run();
                                animationEndRunnable = null;
                            }
                        }
                    });
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    onAnimationEnd(animation);
                }
            });
            transitionAnimationStartTime = System.currentTimeMillis();
            if (Build.VERSION.SDK_INT >= 18) {
                containerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            animatorSet.start();
        } else {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(containerView, "scaleX", 0.9f),
                    ObjectAnimator.ofFloat(containerView, "scaleY", 0.9f),
                    ObjectAnimator.ofInt(backgroundDrawable, "alpha", 0),
                    ObjectAnimator.ofFloat(containerView, "alpha", 0.0f)
            );
            animationInProgress = 2;
            animationEndRunnable = new Runnable() {
                @Override
                public void run() {
                    if (containerView == null) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 18) {
                        containerView.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                    animationInProgress = 0;
                    onPhotoClosed(object);
                    ViewProxy.setScaleX(containerView, 1.0f);
                    ViewProxy.setScaleY(containerView, 1.0f);
                    containerView.clearAnimation();
                }
            };
            animatorSet.setDuration(200);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animationEndRunnable != null) {
                        animationEndRunnable.run();
                        animationEndRunnable = null;
                    }
                }
            });
            transitionAnimationStartTime = System.currentTimeMillis();
            if (Build.VERSION.SDK_INT >= 18) {
                containerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            animatorSet.start();
        }
    }

    private void onPhotoClosed(PhotoViewer.PlaceProviderObject object) {
        isVisible = false;
        disableShowCheck = true;
        currentObject = null;
        currentFileLocation = null;
        currentPathObject = null;
        currentThumb = null;
        for (int a = 0; a < 3; a++) {
            if (radialProgressViews[a] != null) {
                radialProgressViews[a].setBackgroundState(-1, false);
            }
        }
        centerImage.setImageBitmap((Bitmap) null);
        leftImage.setImageBitmap((Bitmap) null);
        rightImage.setImageBitmap((Bitmap) null);
        containerView.post(new Runnable() {
            @Override
            public void run() {
                animatingImageView.setImageBitmap(null);
                try {
                    if (windowView.getParent() != null) {
                        WindowManager wm = (WindowManager) parentActivity.getSystemService(Context.WINDOW_SERVICE);
                        wm.removeView(windowView);
                    }
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
            }
        });
        if (placeProvider != null) {
            placeProvider.willHidePhotoViewer();
        }
        placeProvider = null;
        disableShowCheck = false;
        if (object != null) {
            object.imageReceiver.setVisible(true, true);
        }
    }

    private void redraw(final int count) {
        if (count < 6) {
            if (containerView != null) {
                containerView.invalidate();
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        redraw(count + 1);
                    }
                }, 100);
            }
        }
    }

    public void onResume() {
        redraw(0); //workaround for camera bug
    }

    public boolean isVisible() {
        return isVisible && placeProvider != null;
    }

    private void updateMinMax(float scale) {
        int maxW = (int) (centerImage.getImageWidth() * scale - getContainerViewWidth()) / 2;
        int maxH = (int) (centerImage.getImageHeight() * scale - getContainerViewHeight()) / 2;
        if (maxW > 0) {
            minX = -maxW;
            maxX = maxW;
        } else {
            minX = maxX = 0;
        }
        if (maxH > 0) {
            minY = -maxH;
            maxY = maxH;
        } else {
            minY = maxY = 0;
        }
        if (currentEditMode == 1) {
            maxX += photoCropView.getLimitX();
            maxY += photoCropView.getLimitY();
            minX -= photoCropView.getLimitWidth();
            minY -= photoCropView.getLimitHeight();
        }
    }

    private int getAdditionX() {
        if (currentEditMode != 0) {
            return AndroidUtilities.dp(14);
        }
        return 0;
    }

    private int getAdditionY() {
        if (currentEditMode != 0) {
            return AndroidUtilities.dp(14);
        }
        return 0;
    }

    private int getContainerViewWidth() {
        return getContainerViewWidth(currentEditMode);
    }

    private int getContainerViewWidth(int mode) {
        int width = containerView.getWidth();
        if (mode != 0) {
            width -= AndroidUtilities.dp(28);
        }
        return width;
    }

    private int getContainerViewHeight() {
        return getContainerViewHeight(currentEditMode);
    }

    private int getContainerViewHeight(int mode) {
        int height = containerView.getHeight();
//        int height = AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight;
        if (mode == 1) {
            height -= AndroidUtilities.dp(76);
        } else if (mode == 2) {
            height -= AndroidUtilities.dp(154);
        }
        return height;
    }

    private boolean onTouchEvent(MotionEvent ev) {
        if (animationInProgress != 0 || animationStartTime != 0) {
            return false;
        }

//        if (currentEditMode == 2) {
//            photoFilterView.onTouch(ev);
//            return true;
//        }

        if (currentEditMode == 1) {
            if (ev.getPointerCount() == 1) {
                if (photoCropView.onTouch(ev)) {
                    updateMinMax(scale);
                    return true;
                }
            } else {
                photoCropView.onTouch(null);
            }
        }

//        if (captionEditText.isPopupShowing() || captionEditText.isKeyboardVisible()) {
//            return true;
//        }

        if (currentEditMode == 0 && ev.getPointerCount() == 1 && gestureDetector.onTouchEvent(ev)) {
            if (doubleTap) {
                doubleTap = false;
                moving = false;
                zooming = false;
                checkMinMax(false);
                return true;
            }
        }

        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN || ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            if (currentEditMode == 1) {
                photoCropView.cancelAnimationRunnable();
            }
            discardTap = false;
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }
            if (!draggingDown && !changingPage) {
                if (canZoom && ev.getPointerCount() == 2) {
                    pinchStartDistance = (float) Math.hypot(ev.getX(1) - ev.getX(0), ev.getY(1) - ev.getY(0));
                    pinchStartScale = scale;
                    pinchCenterX = (ev.getX(0) + ev.getX(1)) / 2.0f;
                    pinchCenterY = (ev.getY(0) + ev.getY(1)) / 2.0f;
                    pinchStartX = translationX;
                    pinchStartY = translationY;
                    zooming = true;
                    moving = false;
                    if (velocityTracker != null) {
                        velocityTracker.clear();
                    }
                } else if (ev.getPointerCount() == 1) {
                    moveStartX = ev.getX();
                    dragY = moveStartY = ev.getY();
                    draggingDown = false;
                    canDragDown = true;
                    if (velocityTracker != null) {
                        velocityTracker.clear();
                    }
                }
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (currentEditMode == 1) {
                photoCropView.cancelAnimationRunnable();
            }
            if (canZoom && ev.getPointerCount() == 2 && !draggingDown && zooming && !changingPage) {
                discardTap = true;
                scale = (float) Math.hypot(ev.getX(1) - ev.getX(0), ev.getY(1) - ev.getY(0)) / pinchStartDistance * pinchStartScale;
                translationX = (pinchCenterX - getContainerViewWidth() / 2) - ((pinchCenterX - getContainerViewWidth() / 2) - pinchStartX) * (scale / pinchStartScale);
                translationY = (pinchCenterY - getContainerViewHeight() / 2) - ((pinchCenterY - getContainerViewHeight() / 2) - pinchStartY) * (scale / pinchStartScale);
                updateMinMax(scale);
                containerView.invalidate();
            } else if (ev.getPointerCount() == 1) {
                if (velocityTracker != null) {
                    velocityTracker.addMovement(ev);
                }
                float dx = Math.abs(ev.getX() - moveStartX);
                float dy = Math.abs(ev.getY() - dragY);
                if (dx > AndroidUtilities.dp(3) || dy > AndroidUtilities.dp(3)) {
                    discardTap = true;
                }
                if (!(placeProvider instanceof PhotoViewer.EmptyPhotoViewerProvider) && currentEditMode == 0 && canDragDown && !draggingDown && scale == 1 && dy >= AndroidUtilities.dp(30) && dy / 2 > dx) {
                    draggingDown = true;
                    moving = false;
                    dragY = ev.getY();
                    if (isActionBarVisible && canShowBottom) {
                        toggleActionBar(false, true);
                    }
//                    else if (pickerView.getVisibility() == View.VISIBLE) {
//                        toggleActionBar(false, true);
//                        toggleCheckImageView(false);
//                    }
                    return true;
                } else if (draggingDown) {
                    translationY = ev.getY() - dragY;
                    containerView.invalidate();
                } else if (!invalidCoords && animationStartTime == 0) {
                    float moveDx = moveStartX - ev.getX();
                    float moveDy = moveStartY - ev.getY();
                    if (moving || currentEditMode != 0 || scale == 1 && Math.abs(moveDy) + AndroidUtilities.dp(12) < Math.abs(moveDx) || scale != 1) {
                        if (!moving) {
                            moveDx = 0;
                            moveDy = 0;
                            moving = true;
                            canDragDown = false;
                        }

                        moveStartX = ev.getX();
                        moveStartY = ev.getY();
                        updateMinMax(scale);
                        if (translationX < minX && (currentEditMode != 0 || !rightImage.hasImage()) || translationX > maxX && (currentEditMode != 0 || !leftImage.hasImage())) {
                            moveDx /= 3.0f;
                        }
                        if (maxY == 0 && minY == 0 && currentEditMode == 0) {
                            if (translationY - moveDy < minY) {
                                translationY = minY;
                                moveDy = 0;
                            } else if (translationY - moveDy > maxY) {
                                translationY = maxY;
                                moveDy = 0;
                            }
                        } else {
                            if (translationY < minY || translationY > maxY) {
                                moveDy /= 3.0f;
                            }
                        }

                        translationX -= moveDx;
                        if (scale != 1 || currentEditMode != 0) {
                            translationY -= moveDy;
                        }

                        containerView.invalidate();
                    }
                } else {
                    invalidCoords = false;
                    moveStartX = ev.getX();
                    moveStartY = ev.getY();
                }
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_CANCEL || ev.getActionMasked() == MotionEvent.ACTION_UP || ev.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
            if (currentEditMode == 1) {
                photoCropView.startAnimationRunnable();
            }
            if (zooming) {
                invalidCoords = true;
                if (scale < 1.0f) {
                    updateMinMax(1.0f);
                    animateTo(1.0f, 0, 0, true);
                } else if (scale > 3.0f) {
                    float atx = (pinchCenterX - getContainerViewWidth() / 2) - ((pinchCenterX - getContainerViewWidth() / 2) - pinchStartX) * (3.0f / pinchStartScale);
                    float aty = (pinchCenterY - getContainerViewHeight() / 2) - ((pinchCenterY - getContainerViewHeight() / 2) - pinchStartY) * (3.0f / pinchStartScale);
                    updateMinMax(3.0f);
                    if (atx < minX) {
                        atx = minX;
                    } else if (atx > maxX) {
                        atx = maxX;
                    }
                    if (aty < minY) {
                        aty = minY;
                    } else if (aty > maxY) {
                        aty = maxY;
                    }
                    animateTo(3.0f, atx, aty, true);
                } else {
                    checkMinMax(true);
                }
                zooming = false;
            } else if (draggingDown) {
                if (Math.abs(dragY - ev.getY()) > getContainerViewHeight() / 6.0f) {
                    closePhoto(true, false);
                } else {

                    animateTo(1, 0, 0, false);
                }
                draggingDown = false;
            } else if (moving) {
                float moveToX = translationX;
                float moveToY = translationY;
                updateMinMax(scale);
                moving = false;
                canDragDown = true;
                float velocity = 0;
                if (velocityTracker != null && scale == 1) {
                    velocityTracker.computeCurrentVelocity(1000);
                    velocity = velocityTracker.getXVelocity();
                }

                if (currentEditMode == 0) {
                    if ((translationX < minX - getContainerViewWidth() / 3 || velocity < -AndroidUtilities.dp(650)) && rightImage.hasImage()) {
                        goToNext();
                        return true;
                    }
                    if ((translationX > maxX + getContainerViewWidth() / 3 || velocity > AndroidUtilities.dp(650)) && leftImage.hasImage()) {
                        goToPrev();
                        return true;
                    }
                }

                if (translationX < minX) {
                    moveToX = minX;
                } else if (translationX > maxX) {
                    moveToX = maxX;
                }
                if (translationY < minY) {
                    moveToY = minY;
                } else if (translationY > maxY) {
                    moveToY = maxY;
                }
                animateTo(scale, moveToX, moveToY, false);
            }
        }
        return false;
    }

    private void checkMinMax(boolean zoom) {
        float moveToX = translationX;
        float moveToY = translationY;
        updateMinMax(scale);
        if (translationX < minX) {
            moveToX = minX;
        } else if (translationX > maxX) {
            moveToX = maxX;
        }
        if (translationY < minY) {
            moveToY = minY;
        } else if (translationY > maxY) {
            moveToY = maxY;
        }
        animateTo(scale, moveToX, moveToY, zoom);
    }

    private void goToNext() {
        float extra = 0;
        if (scale != 1) {
            extra = (getContainerViewWidth() - centerImage.getImageWidth()) / 2 * scale;
        }
        switchImageAfterAnimation = 1;
        animateTo(scale, minX - getContainerViewWidth() - extra - PAGE_SPACING / 2, translationY, false);
    }

    private void goToPrev() {
        float extra = 0;
        if (scale != 1) {
            extra = (getContainerViewWidth() - centerImage.getImageWidth()) / 2 * scale;
        }
        switchImageAfterAnimation = 2;
        animateTo(scale, maxX + getContainerViewWidth() + extra + PAGE_SPACING / 2, translationY, false);
    }

    private void animateTo(float newScale, float newTx, float newTy, boolean isZoom) {
        animateTo(newScale, newTx, newTy, isZoom, 250);
    }

    private void animateTo(float newScale, float newTx, float newTy, boolean isZoom, int duration) {
        if (scale == newScale && translationX == newTx && translationY == newTy) {
            return;
        }
        zoomAnimation = isZoom;
        animateToScale = newScale;
        animateToX = newTx;
        animateToY = newTy;
        animationStartTime = System.currentTimeMillis();
        imageMoveAnimation = new AnimatorSet();
        imageMoveAnimation.playTogether(
                ObjectAnimator.ofFloat(this, "animationValue", 0, 1)
        );
        imageMoveAnimation.setInterpolator(interpolator);
        imageMoveAnimation.setDuration(duration);
        imageMoveAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                imageMoveAnimation = null;
                containerView.invalidate();
            }
        });
        imageMoveAnimation.start();
    }

    private void onDraw(Canvas canvas) {
        if (animationInProgress == 1 || !isVisible && animationInProgress != 2) {
            return;
        }

        float currentTranslationY;
        float currentTranslationX;
        float currentScale;
        float aty = -1;

        if (imageMoveAnimation != null) {
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }

            float ts = scale + (animateToScale - scale) * animationValue;
            float tx = translationX + (animateToX - translationX) * animationValue;
            float ty = translationY + (animateToY - translationY) * animationValue;
            if (currentEditMode == 1) {
                photoCropView.setAnimationProgress(animationValue);
            }

            if (animateToScale == 1 && scale == 1 && translationX == 0) {
                aty = ty;
            }
            currentScale = ts;
            currentTranslationY = ty;
            currentTranslationX = tx;
            containerView.invalidate();
        } else {
            if (animationStartTime != 0) {
                translationX = animateToX;
                translationY = animateToY;
                scale = animateToScale;
                animationStartTime = 0;
                if (currentEditMode == 1) {
                    photoCropView.setAnimationProgress(1);
                }
                updateMinMax(scale);
                zoomAnimation = false;
            }
            if (!scroller.isFinished()) {
                if (scroller.computeScrollOffset()) {
                    if (scroller.getStartX() < maxX && scroller.getStartX() > minX) {
                        translationX = scroller.getCurrX();
                    }
                    if (scroller.getStartY() < maxY && scroller.getStartY() > minY) {
                        translationY = scroller.getCurrY();
                    }
                    containerView.invalidate();
                }
            }
            if (switchImageAfterAnimation != 0) {
                if (switchImageAfterAnimation == 1) {
                    setImageIndex(currentIndex + 1, false);
                } else if (switchImageAfterAnimation == 2) {
                    setImageIndex(currentIndex - 1, false);
                }
                switchImageAfterAnimation = 0;
            }
            currentScale = scale;
            currentTranslationY = translationY;
            currentTranslationX = translationX;
            if (!moving) {
                aty = translationY;
            }
        }

        if (currentEditMode == 0 && scale == 1 && aty != -1 && !zoomAnimation) {
            float maxValue = getContainerViewHeight() / 4.0f;
            backgroundDrawable.setAlpha((int) Math.max(127, 255 * (1.0f - (Math.min(Math.abs(aty), maxValue) / maxValue))));
        } else {
            backgroundDrawable.setAlpha(255);
        }

        ImageReceiver sideImage = null;
        if (currentEditMode == 0) {
            if (scale >= 1.0f && !zoomAnimation && !zooming) {
                if (currentTranslationX > maxX + AndroidUtilities.dp(5)) {
                    sideImage = leftImage;
                } else if (currentTranslationX < minX - AndroidUtilities.dp(5)) {
                    sideImage = rightImage;
                }
            }
            changingPage = sideImage != null;
        }

        if (sideImage == rightImage) {
            float tranlateX = currentTranslationX;
            float scaleDiff = 0;
            float alpha = 1;
            if (!zoomAnimation && tranlateX < minX) {
                alpha = Math.min(1.0f, (minX - tranlateX) / canvas.getWidth());
                scaleDiff = (1.0f - alpha) * 0.3f;
                tranlateX = -canvas.getWidth() - PAGE_SPACING / 2;
            }

            if (sideImage.hasBitmapImage()) {
                canvas.save();
                canvas.translate(getContainerViewWidth() / 2, getContainerViewHeight() / 2);
                canvas.translate(canvas.getWidth() + PAGE_SPACING / 2 + tranlateX, 0);
                canvas.scale(1.0f - scaleDiff, 1.0f - scaleDiff);
                int bitmapWidth = sideImage.getBitmapWidth();
                int bitmapHeight = sideImage.getBitmapHeight();

                float scaleX = (float) getContainerViewWidth() / (float) bitmapWidth;
                float scaleY = (float) getContainerViewHeight() / (float) bitmapHeight;
                float scale = scaleX > scaleY ? scaleY : scaleX;
                int width = (int) (bitmapWidth * scale);
                int height = (int) (bitmapHeight * scale);

                sideImage.setAlpha(alpha);
                sideImage.setImageCoords(-width / 2, -height / 2, width, height);
                sideImage.draw(canvas);
                canvas.restore();
            }

            canvas.save();
            canvas.translate(tranlateX, currentTranslationY / currentScale);
            canvas.translate((canvas.getWidth() * (scale + 1) + PAGE_SPACING) / 2, -currentTranslationY / currentScale);
            radialProgressViews[1].setScale(1.0f - scaleDiff);
            radialProgressViews[1].setAlpha(alpha);
            radialProgressViews[1].onDraw(canvas);
            canvas.restore();
        }

        float tranlateX = currentTranslationX;
        float scaleDiff = 0;
        float alpha = 1;
        if (!zoomAnimation && tranlateX > maxX && currentEditMode == 0) {
            alpha = Math.min(1.0f, (tranlateX - maxX) / canvas.getWidth());
            scaleDiff = alpha * 0.3f;
            alpha = 1.0f - alpha;
            tranlateX = maxX;
        }
        if (centerImage.hasBitmapImage()) {
            canvas.save();
            canvas.translate(getContainerViewWidth() / 2 + getAdditionX(), getContainerViewHeight() / 2 + getAdditionY());
            canvas.translate(tranlateX, currentTranslationY);
            canvas.scale(currentScale - scaleDiff, currentScale - scaleDiff);

            if (currentEditMode == 1) {
                photoCropView.setBitmapParams(currentScale, tranlateX, currentTranslationY);
            }

            int bitmapWidth = centerImage.getBitmapWidth();
            int bitmapHeight = centerImage.getBitmapHeight();

            float scaleX = (float) getContainerViewWidth() / (float) bitmapWidth;
            float scaleY = (float) getContainerViewHeight() / (float) bitmapHeight;
            float scale = scaleX > scaleY ? scaleY : scaleX;
            int width = (int) (bitmapWidth * scale);
            int height = (int) (bitmapHeight * scale);

            centerImage.setAlpha(alpha);
            centerImage.setImageCoords(-width / 2, -height / 2, width, height);
            centerImage.draw(canvas);
            canvas.restore();
        }
        canvas.save();
        canvas.translate(tranlateX, currentTranslationY / currentScale);
        radialProgressViews[0].setScale(1.0f - scaleDiff);
        radialProgressViews[0].setAlpha(alpha);
        radialProgressViews[0].onDraw(canvas);
        canvas.restore();

        if (sideImage == leftImage) {
            if (sideImage.hasBitmapImage()) {
                canvas.save();
                canvas.translate(getContainerViewWidth() / 2, getContainerViewHeight() / 2);
                canvas.translate(-(canvas.getWidth() * (scale + 1) + PAGE_SPACING) / 2 + currentTranslationX, 0);
                int bitmapWidth = sideImage.getBitmapWidth();
                int bitmapHeight = sideImage.getBitmapHeight();

                float scaleX = (float) getContainerViewWidth() / (float) bitmapWidth;
                float scaleY = (float) getContainerViewHeight() / (float) bitmapHeight;
                float scale = scaleX > scaleY ? scaleY : scaleX;
                int width = (int) (bitmapWidth * scale);
                int height = (int) (bitmapHeight * scale);

                sideImage.setAlpha(1.0f);
                sideImage.setImageCoords(-width / 2, -height / 2, width, height);
                sideImage.draw(canvas);
                canvas.restore();
            }

            canvas.save();
            canvas.translate(currentTranslationX, currentTranslationY / currentScale);
            canvas.translate(-(canvas.getWidth() * (scale + 1) + PAGE_SPACING) / 2, -currentTranslationY / currentScale);
            radialProgressViews[2].setScale(1.0f);
            radialProgressViews[2].setAlpha(1.0f);
            radialProgressViews[2].onDraw(canvas);
            canvas.restore();
        }
    }

    @SuppressLint("DrawAllocation")
    private void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            scale = 1;
            translationX = 0;
            translationY = 0;
            updateMinMax(scale);

            if (checkImageView != null) {
                checkImageView.post(new Runnable() {
                    @Override
                    public void run() {
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) checkImageView.getLayoutParams();
                        WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
                        int rotation = manager.getDefaultDisplay().getRotation();
                        layoutParams.topMargin = AndroidUtilities.dp(rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90 ? 58 : 68);
                        checkImageView.setLayoutParams(layoutParams);
                    }
                });
            }
        }
    }

    private void onActionClick() {
        if (currentObject == null || currentFileNames[0] == null) {
            return;
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (scale != 1) {
            scroller.abortAnimation();
            scroller.fling(Math.round(translationX), Math.round(translationY), Math.round(velocityX), Math.round(velocityY), (int) minX, (int) maxX, (int) minY, (int) maxY);
            containerView.postInvalidate();
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (discardTap) {
            return false;
        }
        if (canShowBottom) {
            if (radialProgressViews[0] != null && containerView != null) {
                int state = radialProgressViews[0].backgroundState;
                if (state > 0 && state <= 3) {
                    float x = e.getX();
                    float y = e.getY();
                    if (x >= (getContainerViewWidth() - AndroidUtilities.dp(64)) / 2.0f && x <= (getContainerViewWidth() + AndroidUtilities.dp(64)) / 2.0f &&
                            y >= (getContainerViewHeight() - AndroidUtilities.dp(64)) / 2.0f && y <= (getContainerViewHeight() + AndroidUtilities.dp(64)) / 2.0f) {
                        onActionClick();
                        checkProgress(0, true);
                        return true;
                    }
                }
            }
            toggleActionBar(!isActionBarVisible, true);
        } else if (sendPhotoType == 0) {
            checkImageView.performClick();
        }
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (!canZoom || scale == 1.0f && (translationY != 0 || translationX != 0)) {
            return false;
        }
        if (animationStartTime != 0 || animationInProgress != 0) {
            return false;
        }
        if (scale == 1.0f) {
            float atx = (e.getX() - getContainerViewWidth() / 2) - ((e.getX() - getContainerViewWidth() / 2) - translationX) * (3.0f / scale);
            float aty = (e.getY() - getContainerViewHeight() / 2) - ((e.getY() - getContainerViewHeight() / 2) - translationY) * (3.0f / scale);
            updateMinMax(3.0f);
            if (atx < minX) {
                atx = minX;
            } else if (atx > maxX) {
                atx = maxX;
            }
            if (aty < minY) {
                aty = minY;
            } else if (aty > maxY) {
                aty = maxY;
            }
            animateTo(3.0f, atx, aty, true);
        } else {
            animateTo(1.0f, 0, 0, true);
        }
        doubleTap = true;
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    MessageObject currentMessageObject;

    public boolean isShowingImage(MessageObject object) {
        return isVisible && !disableShowCheck && object != null && currentMessageObject != null && currentMessageObject.getId() == object.getId();
    }

    public boolean isShowingImage(TLRPC.FileLocation object) {
        return isVisible && !disableShowCheck && object != null && currentFileLocation != null && object.local_id == currentFileLocation.local_id && object.volume_id == currentFileLocation.volume_id && object.dc_id == currentFileLocation.dc_id;
    }

    public boolean isShowingImage(String object) {
        return isVisible && !disableShowCheck && object != null && currentPathObject != null && object.equals(currentPathObject);
    }
}
