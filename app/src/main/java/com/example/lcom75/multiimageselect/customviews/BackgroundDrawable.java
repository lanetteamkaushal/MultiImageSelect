package com.example.lcom75.multiimageselect.customviews;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;

/**
 * Created by lcom75 on 15/2/16.
 */
public class BackgroundDrawable extends ColorDrawable {

    public Runnable drawRunnable;

    public BackgroundDrawable(int color) {
        super(color);
    }

    @Override
    public void setAlpha(int alpha) {
//        if (parentActivity instanceof LaunchActivity) {
//            ((LaunchActivity) parentActivity).drawerLayoutContainer.setAllowDrawContent(!isVisible || alpha != 255);
//        }
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
