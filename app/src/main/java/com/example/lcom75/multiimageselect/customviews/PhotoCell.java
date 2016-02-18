package com.example.lcom75.multiimageselect.customviews;

import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.Checkable;
import android.widget.FrameLayout;

import com.example.lcom75.multiimageselect.AndroidUtilities;
import com.example.lcom75.multiimageselect.R;

/**
 * Created by lcom75 on 15/2/16.
 */
public class PhotoCell extends FrameLayout {

    private BackupImageView imageView;
    private boolean noCheckBox = false;
    private boolean isLast;
    private boolean pressed;
    private static Rect rect = new Rect();
    private PhotoAttachPhotoCellDelegate delegate;
    private boolean attachedToWindow;
    private boolean isChecked;
    private float progress = 0f;
    public View checkedView;

    public PhotoCell(Context context, boolean NeedCheckBox) {
        super(context);
        imageView = new BackupImageView(context);
        addView(imageView, LayoutHelper.createFrame(80, 80));
        checkedView = new View(context);
        addView(checkedView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        checkedView.setBackgroundColor(context.getResources().getColor(R.color.checkedColor));
        checkedView.setVisibility(View.GONE);
    }

    public interface PhotoAttachPhotoCellDelegate {
        void onCheckClick(PhotoCell v);
    }

    private AndroidUtilities.PhotoEntry photoEntry;

    public PhotoCell(Context context) {
        this(context, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(80 + (isLast ? 0 : 6)), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(80), MeasureSpec.EXACTLY));
    }

    public AndroidUtilities.PhotoEntry getPhotoEntry() {
        return photoEntry;
    }

    public BackupImageView getImageView() {
        return imageView;
    }


    public void setPhotoEntry(AndroidUtilities.PhotoEntry entry, boolean last) {
        pressed = false;
        photoEntry = entry;
        isLast = last;
        if (photoEntry.thumbPath != null) {
            imageView.setImage(photoEntry.thumbPath, null, getResources().getDrawable(R.drawable.nophotos));
        } else if (photoEntry.path != null) {
            imageView.setOrientation(photoEntry.orientation, true);
            imageView.setImage("thumb://" + photoEntry.imageId + ":" + photoEntry.path, null, getResources().getDrawable(R.drawable.nophotos));
        } else {
            imageView.setImageResource(R.drawable.nophotos);
        }
        boolean showing = PhotoViewer.getInstance().isShowingImage(photoEntry.path);
        if (noCheckBox) showing = true;
        imageView.getImageReceiver().setVisible(!showing, true);
        requestLayout();
    }

//    public void setChecked(boolean value, boolean animated) {
//        checkBox.setChecked(value, animated);
//    }

    public void setDelegate(PhotoAttachPhotoCellDelegate delegate) {
        this.delegate = delegate;
    }

    public void setChecked(boolean checked, boolean animated) {
        if (checked == isChecked) {
            return;
        }
        isChecked = checked;
        checkedView.setVisibility(checked ? VISIBLE : GONE);
//        if (attachedToWindow && animated) {
//        } else {
//            setProgress(checked ? 1.0f : 0.0f);
//        }
//    }
//
//    public void setProgress(float value) {
//        if (progress == value) {
//            return;
//        }
//        progress = value;
//        invalidate();
    }

}
