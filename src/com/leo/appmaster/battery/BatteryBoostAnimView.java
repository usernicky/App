package com.leo.appmaster.battery;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.leo.appmaster.R;

/**
 * Created by Jasper on 2016/2/29.
 */
public class BatteryBoostAnimView extends View {
    private static final int ROTATE_INTERVAL = 4;

    private Drawable mBgDrawable;
    private Drawable mGradientDrawable;

    private int mRotateAngel;

    public BatteryBoostAnimView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Resources res = getContext().getResources();
        mBgDrawable = getContext().getResources().getDrawable(R.drawable.bg_circular_clear);
        mBgDrawable.setBounds(0, 0, getWidth(), getHeight());

        int padding = res.getDimensionPixelSize(R.dimen.battery_boost_padding);
        mGradientDrawable = getContext().getResources().getDrawable(R.drawable.battery_boost_gradient_shape);
        mGradientDrawable.setBounds(padding, padding, getWidth() - padding, getHeight() - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.rotate(mRotateAngel, getWidth() / 2, getHeight() / 2);
        mBgDrawable.draw(canvas);
        canvas.restore();

        mRotateAngel += ROTATE_INTERVAL;
        if (mRotateAngel > 360) {
            mRotateAngel = 0;
        }

        mGradientDrawable.draw(canvas);

        invalidate();
    }
}