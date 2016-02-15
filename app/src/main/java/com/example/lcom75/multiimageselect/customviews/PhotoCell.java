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
public class PhotoCell extends FrameLayout implements Checkable {
    private BackupImageView imageView;
    private FrameLayout checkFrame;
    private boolean isLast;
    private boolean pressed;
    private static Rect rect = new Rect();
    private PhotoAttachPhotoCellDelegate delegate;
    public View checkedView;

    public PhotoCell(Context context, boolean NeedCheckBox) {
        super(context);
        imageView = new BackupImageView(context);
        addView(imageView, LayoutHelper.createFrame(80, 80));
        checkedView = new View(context);
        addView(checkedView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        checkedView.setBackgroundColor(context.getResources().getColor(R.color.checkedColor));
        checkFrame = new FrameLayout(context);
        addView(checkFrame, LayoutHelper.createFrame(42, 42, Gravity.LEFT | Gravity.TOP, 38, 0, 0, 0));
    }

    @Override
    public void setChecked(boolean checked) {

    }

    @Override
    public boolean isChecked() {
        return false;
    }

    @Override
    public void toggle() {

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
        imageView.getImageReceiver().setVisible(!showing, true);
        requestLayout();
    }

    public void setOnCheckClickLisnener(OnClickListener onCheckClickLisnener) {
        checkFrame.setOnClickListener(onCheckClickLisnener);
    }

    public void setDelegate(PhotoAttachPhotoCellDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;

        checkFrame.getHitRect(rect);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (rect.contains((int) event.getX(), (int) event.getY())) {
                pressed = true;
                invalidate();
                result = true;
            }
        } else if (pressed) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                getParent().requestDisallowInterceptTouchEvent(true);
                pressed = false;
                playSoundEffect(SoundEffectConstants.CLICK);
                delegate.onCheckClick(this);
                invalidate();
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                pressed = false;
                invalidate();
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (!(rect.contains((int) event.getX(), (int) event.getY()))) {
                    pressed = false;
                    invalidate();
                }
            }
        }
        if (!result) {
            result = super.onTouchEvent(event);
        }

        return result;
    }
    public void setChecked(boolean value, boolean animated) {
        setChecked(value);
        checkedView.setVisibility(value ? VISIBLE : GONE);
    }
}
