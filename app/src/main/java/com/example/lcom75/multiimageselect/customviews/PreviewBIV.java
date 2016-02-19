package com.example.lcom75.multiimageselect.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

/**
 * Created by lcom75 on 16/2/16.
 */
public class PreviewBIV extends BackupImageView {


    public PreviewBIV(Context context) {
        super(context);
        initPaint(context);
    }

    private void initPaint(Context context) {
    }

    public PreviewBIV(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint(context);
    }

    public PreviewBIV(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (width != -1 && height != -1) {
            imageReceiver.setImageCoords((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
        } else {
            imageReceiver.setImageCoords(0, 0, getWidth(), getHeight());
        }
//        if (imageReceiver.getBitmap() != null) {
//            canvas.drawBitmap(imageReceiver.getBitmap(), 0, 0, paint);
//        } else {
            imageReceiver.draw(canvas, true);
//        }
    }
}
