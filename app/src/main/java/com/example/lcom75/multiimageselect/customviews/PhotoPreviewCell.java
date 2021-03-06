/*
 * This is the source code of Telegram for Android v. 2.0.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 *
 */

package com.example.lcom75.multiimageselect.customviews;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.example.lcom75.multiimageselect.AndroidUtilities;
import com.example.lcom75.multiimageselect.R;

/***
 * Do Not change This class. It is used in
 * Image Preview Fragment.
 */
public class PhotoPreviewCell extends FrameLayout {

    public PreviewBIV photoImage;
    public FrameLayout checkFrame;
    public CheckBox checkBox;
    public int itemWidth;
//    public View checkedView;

    public PhotoPreviewCell(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        photoImage = new PreviewBIV(context);
        addView(photoImage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

//        checkedView = new View(context);
//        addView(checkedView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//        checkedView.setBackgroundColor(context.getResources().getColor(R.color.checkedColor));
        checkFrame = new FrameLayout(context);
        addView(checkFrame, LayoutHelper.createFrame(42, 42, Gravity.RIGHT | Gravity.TOP));

        checkBox = new CheckBox(context, R.drawable.checkbig);
        checkBox.setSize(30);
        checkBox.setCheckOffset(AndroidUtilities.dp(1));
        checkBox.setDrawBackground(true);
        checkBox.setColor(0xff3ccaef);
        addView(checkBox, LayoutHelper.createFrame(30, 30, Gravity.RIGHT | Gravity.TOP, 0, 4, 4, 0));
    }

    public PhotoPreviewCell(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PhotoPreviewCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PhotoPreviewCell(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (itemWidth > 0)
            super.onMeasure(MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY));
        else
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setChecked(final boolean checked, boolean animated) {
        checkBox.setChecked(checked, false);
//        setBackgroundColor(checked ? 0xff0A0A0A : 0);
//        setBackgroundColor(checked ? 0xff0A0A0A : 0);
//        checkedView.setVisibility(checked ? VISIBLE : GONE);
//        ViewProxy.setScaleX(photoImage, checked ? 0.85f : 1.0f);
//        ViewProxy.setScaleY(photoImage, checked ? 0.85f : 1.0f);
    }

    public BackupImageView getImageView() {
        return photoImage;
    }
}
