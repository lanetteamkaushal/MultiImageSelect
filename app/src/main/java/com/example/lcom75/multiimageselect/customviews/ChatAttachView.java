/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.example.lcom75.multiimageselect.customviews;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.example.lcom75.multiimageselect.AndroidUtilities;
import com.example.lcom75.multiimageselect.ApplicationLoader;
import com.example.lcom75.multiimageselect.NotificationCenter;
import com.example.lcom75.multiimageselect.R;
import com.example.lcom75.multiimageselect.adapter.PhotoAttachAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatAttachView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {
    private String TAG = ChatAttachView.class.getSimpleName();

    public interface ChatAttachViewDelegate {
        void didPressedButton(int button);

        void didItemClicked(int position);
    }

    private LinearLayoutManager attachPhotoLayoutManager;
    private PhotoAttachAdapter photoAttachAdapter;
    private Activity baseFragment;
    private View views[] = new View[20];
    private RecyclerListView attachPhotoRecyclerView;
    private View lineView;
    private ProgressBar progressView;

    public void setParentForPhoto(ViewGroup parentForPhoto) {
        this.parentForPhoto = parentForPhoto;
    }

    boolean firstTime = true;
    private ViewGroup parentForPhoto;
    private float[] distCache = new float[20];

    private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();

    private boolean loading;

    private ChatAttachViewDelegate delegate;
    HashMap<Integer, AndroidUtilities.PhotoEntry> mSelectedPhotos = new HashMap<>();

    public ChatAttachView(Context context) {
        super(context);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.albumsDidLoaded);
        if (AndroidUtilities.allPhotosAlbumEntry == null) {
            if (Build.VERSION.SDK_INT >= 21) {
                AndroidUtilities.loadGalleryPhotosAlbums(0);
            }
            loading = true;
        }
        views[8] = attachPhotoRecyclerView = new RecyclerListView(context);
        attachPhotoRecyclerView.setVerticalScrollBarEnabled(true);
        mSelectedPhotos = ApplicationLoader.selectedPhotos;
        attachPhotoRecyclerView.setAdapter(photoAttachAdapter = new PhotoAttachAdapter(context, mSelectedPhotos));
        attachPhotoRecyclerView.setClipToPadding(false);
        attachPhotoRecyclerView.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        attachPhotoRecyclerView.setItemAnimator(null);
        attachPhotoRecyclerView.setLayoutAnimation(null);
        if (Build.VERSION.SDK_INT >= 9) {
            attachPhotoRecyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        }
        addView(attachPhotoRecyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80));
        attachPhotoLayoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        attachPhotoLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        attachPhotoRecyclerView.setLayoutManager(attachPhotoLayoutManager);
        photoAttachAdapter.setDelegate(new PhotoAttachAdapter.PhotoAttachAdapterDelegate() {
            @Override
            public void selectedPhotosChanged() {
                updatePhotosButton();
            }
        });
        attachPhotoRecyclerView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onItemClick(View view, int position) {
                if (baseFragment == null || baseFragment == null) {
                    return;
                }
                if (position < 0 || position >= mSelectedPhotos.size()) {
                    return;
                }
                photoAttachAdapter.setSelectedItem(position);
                photoAttachAdapter.notifyDataSetChanged();
                if (delegate != null)
                    delegate.didItemClicked(position);
            }
        });

        views[9] = progressView = new ProgressBar(context);
        views[10] = lineView = new View(getContext());
        lineView.setBackgroundColor(0xffd2d2d2);
        addView(lineView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1, Gravity.TOP | Gravity.LEFT));
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.albumsDidLoaded) {
            if (photoAttachAdapter != null) {
                loading = false;
                photoAttachAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(100), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int t = AndroidUtilities.dp(8);
        attachPhotoRecyclerView.layout(0, t, width, t + attachPhotoRecyclerView.getMeasuredHeight());
    }

    public void updatePhotosButton() {
        int count = photoAttachAdapter.getSelectedPhotos().size();
        Log.d(TAG, "SEt count :" + count);
    }

    public void setDelegate(ChatAttachViewDelegate chatAttachViewDelegate) {
        delegate = chatAttachViewDelegate;
    }

    public void onRevealAnimationEnd(boolean open) {
        if (open && Build.VERSION.SDK_INT <= 19 && AndroidUtilities.allPhotosAlbumEntry == null) {
            AndroidUtilities.loadGalleryPhotosAlbums(0);
        }
    }

    @SuppressLint("NewApi")
    public void onRevealAnimationStart(boolean open) {
        if (!open) {
            return;
        }
        int count = Build.VERSION.SDK_INT <= 19 ? 11 : 8;
        for (int a = 0; a < count; a++) {
            if (Build.VERSION.SDK_INT <= 19) {
                if (a < 8) {
                    views[a].setScaleX(0.1f);
                    views[a].setScaleY(0.1f);
                }
                views[a].setAlpha(0.0f);
            } else {
                views[a].setScaleX(0.7f);
                views[a].setScaleY(0.7f);
            }
            views[a].setTag(R.string.app_name, null);
            distCache[a] = 0;
        }
    }

    @SuppressLint("NewApi")
    public void onRevealAnimationProgress(boolean open, float radius, int x, int y) {
        if (!open) {
            return;
        }
        int count = Build.VERSION.SDK_INT <= 19 ? 11 : 8;
        for (int a = 0; a < count; a++) {
            if (views[a].getTag(R.string.app_name) == null) {
                if (distCache[a] == 0) {
                    int buttonX = views[a].getLeft() + views[a].getMeasuredWidth() / 2;
                    int buttonY = views[a].getTop() + views[a].getMeasuredHeight() / 2;
                    distCache[a] = (float) Math.sqrt((x - buttonX) * (x - buttonX) + (y - buttonY) * (y - buttonY));
                    float vecX = (x - buttonX) / distCache[a];
                    float vecY = (y - buttonY) / distCache[a];
                    views[a].setPivotX(views[a].getMeasuredWidth() / 2 + vecX * AndroidUtilities.dp(20));
                    views[a].setPivotY(views[a].getMeasuredHeight() / 2 + vecY * AndroidUtilities.dp(20));
                }
                if (distCache[a] > radius + AndroidUtilities.dp(27)) {
                    continue;
                }

                views[a].setTag(R.string.app_name, 1);
                final ArrayList<Animator> animators = new ArrayList<>();
                final ArrayList<Animator> animators2 = new ArrayList<>();
                if (a < 8) {
                    animators.add(ObjectAnimator.ofFloat(views[a], "scaleX", 0.7f, 1.05f));
                    animators.add(ObjectAnimator.ofFloat(views[a], "scaleY", 0.7f, 1.05f));
                    animators2.add(ObjectAnimator.ofFloat(views[a], "scaleX", 1.0f));
                    animators2.add(ObjectAnimator.ofFloat(views[a], "scaleY", 1.0f));
                }
                if (Build.VERSION.SDK_INT <= 19) {
                    animators.add(ObjectAnimator.ofFloat(views[a], "alpha", 1.0f));
                }
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(animators);
                animatorSet.setDuration(150);
                animatorSet.setInterpolator(decelerateInterpolator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.playTogether(animators2);
                        animatorSet.setDuration(100);
                        animatorSet.setInterpolator(decelerateInterpolator);
                        animatorSet.start();
                    }
                });
                animatorSet.start();
            }
        }
    }

    public void init(Activity parentFragment) {
//        if (AndroidUtilities.allPhotosAlbumEntry != null) {
//            for (int a = 0; a < Math.min(14, AndroidUtilities.allPhotosAlbumEntry.photos.size()); a++) {
//                AndroidUtilities.PhotoEntry photoEntry = AndroidUtilities.allPhotosAlbumEntry.photos.get(a);
//                photoEntry.caption = null;
//                photoEntry.imagePath = null;
//                photoEntry.thumbPath = null;
//            }
//        }
        attachPhotoLayoutManager.scrollToPositionWithOffset(0, 1000000);
//        photoAttachAdapter.clearSelectedPhotos();
        baseFragment = parentFragment;
        updatePhotosButton();
    }

    public HashMap<Integer, AndroidUtilities.PhotoEntry> getSelectedPhotos() {
        return photoAttachAdapter.getSelectedPhotos();
    }

    public void onDestroy() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.albumsDidLoaded);
        baseFragment = null;
    }

    public void replace(HashMap<Integer, AndroidUtilities.PhotoEntry> mSelectedPhotos) {
        if (photoAttachAdapter != null) {
            this.mSelectedPhotos = mSelectedPhotos;
            photoAttachAdapter.replace(mSelectedPhotos);
            photoAttachAdapter.notifyDataSetChanged();
        }
    }
}
