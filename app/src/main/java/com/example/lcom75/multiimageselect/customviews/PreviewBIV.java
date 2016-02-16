package com.example.lcom75.multiimageselect.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/**
 * Created by lcom75 on 16/2/16.
 */
public class PreviewBIV extends BackupImageView {
    public PreviewBIV(Context context) {
        super(context);
    }

    public PreviewBIV(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreviewBIV(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (width != -1 && height != -1) {
            imageReceiver.setImageCoords((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
        } else {
            imageReceiver.setImageCoords(0, 0, getWidth(), getHeight());
        }
        if (imageReceiver.getBitmap() != null) {
            Paint paint = new Paint();
            canvas.drawBitmap(imageReceiver.getBitmap(), 0, 0, paint);
        } else {
            imageReceiver.draw(canvas, true);
        }


    }
}
