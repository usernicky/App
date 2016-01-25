package com.leo.appmaster.battery;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import com.leo.appmaster.R;
import com.leo.appmaster.utils.LeoLog;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Taolin
 * @date Dec 03, 2013
 * @since v1.0
 */

public class BatteryProtectSlideView extends View {


    private static final int MSG_REDRAW = 1;
    private static final int DRAW_INTERVAL = 80;
    private static final int DRAW_INTERVAL_All = 400;
    private static final int STEP_LENGTH = 5;
    private final int mPaddingSize;
    private final int mIconSize;

    private Paint mPaint;
    private LinearGradient mGradient;
    private int[] mGradientColors;
    private int mGradientIndex;
    private float mDensity;
    private Matrix mMatrix;

    private String mText;
    private Rect mTextRect;
    private int mTextSize;
    private int mTextLeft;
    private int mTextTop;

    private int mSlidableLength;
    private int mScreenHeight;
    private int mScreenWidth;

    private BitmapDrawable mSlideArrow;
    private Rect mSlideRect;
    private Paint mSlidePaint;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private String mIconText;

    private static final Xfermode FER_MODE = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);

//    private Handler mHandler = new Handler() {
//
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            switch (msg.what) {
//                case MSG_REDRAW:
//                    mMatrix.setTranslate(mGradientIndex, 0);
//                    mGradient.setLocalMatrix(mMatrix);
//                    invalidate();
//                    mGradientIndex += STEP_LENGTH * mDensity;
//                    if (mGradientIndex > mSlidableLength) {
//                        mGradientIndex = 0;
//                        mHandler.sendEmptyMessageDelayed(MSG_REDRAW, DRAW_INTERVAL_All);
//                    } else {
//                        mHandler.sendEmptyMessageDelayed(MSG_REDRAW, DRAW_INTERVAL);
//                    }
//                    break;
//            }
//        }
//    };

    public BatteryProtectSlideView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mDensity = getResources().getDisplayMetrics().density;

        TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.SlideView);
        mText = typeArray.getString(R.styleable.SlideView_maskText);
        mTextSize = typeArray.getDimensionPixelSize(R.styleable.SlideView_maskTextSize, R.dimen.mask_text_size);
        try {
            if ("i-mobile I-STYLE 217".equals(android.os.Build.MODEL)) {
                mTextSize = (int) (mTextSize * 0.85f);
            }
        } catch (Throwable e) {
        }
//        mTextLeft = typeArray.getDimensionPixelSize(R.styleable.SlideView_maskTextMarginLeft, R.dimen.mask_text_margin_left);
//        mTextTop = typeArray.getDimensionPixelSize(R.styleable.SlideView_maskTextMarginTop, R.dimen.mask_text_margin_top);
        mSlidableLength = typeArray.getDimensionPixelSize(R.styleable.SlideView_slidableLength, R.dimen.slidable_length);

        typeArray.recycle();

        mGradientColors = new int[]{Color.argb(255, 120, 120, 120),
                Color.argb(255, 120, 120, 120), Color.argb(255, 255, 255, 255)};

        mGradient = new LinearGradient(0, 0, 100 * mDensity, 0, mGradientColors,
                new float[]{0, 0.7f, 1}, TileMode.MIRROR);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mScreenHeight = wm.getDefaultDisplay().getHeight();
        mScreenWidth = wm.getDefaultDisplay().getWidth();


        mGradientIndex = 0;
        mPaint = new Paint();
        mMatrix = new Matrix();
        mPaint.setTextSize(mTextSize);
        mPaint.setTextAlign(Paint.Align.CENTER);

        mTextRect = new Rect();
        mPaint.getTextBounds(mText, 0, mText.length(), mTextRect);

        mIconSize = context.getResources().getDimensionPixelSize(R.dimen.mask_text_size_icon);
        mPaddingSize = context.getResources().getDimensionPixelSize(R.dimen.mask_text_icon_pd);
        mSlidePaint = new Paint();
        mSlidePaint.setTextSize(mIconSize);
        mSlidePaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "app_custom.ttf"));

//        mHandler.sendEmptyMessageDelayed(MSG_REDRAW, DRAW_INTERVAL);
        mIconText = context.getString(R.string.sliding_icon);

        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                fillColor();
            }
        };
        mTimer.schedule(mTimerTask, 0, 100);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.bay_arrow_slide);
        mSlideRect = new Rect(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LeoLog.d("testMesure", "f height : " + getHeight());
        LeoLog.d("testMesure", "f width : " + getWidth());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPaint.getShader() == null) {
            mPaint.setShader(mGradient);
        }
        int centerX = getWidth() / 2;
        int baseline = getHeight() / 2 + mTextSize / 2;
        canvas.drawText(mText, centerX, baseline, mPaint);

        if (mSlidePaint.getShader() == null) {
            mSlidePaint.setShader(mGradient);
        }

        int left = (getWidth() - mTextRect.width()) / 2 - mSlideRect.width() - mPaddingSize;
        int base = baseline - (mTextSize - mIconSize);
        canvas.drawText(mIconText, left, base, mSlidePaint);
    }

    private void fillColor() {
        mMatrix.setTranslate(mGradientIndex, 0);
        mGradient.setLocalMatrix(mMatrix);
        postInvalidate();
//        invalidate();
        mGradientIndex += STEP_LENGTH * mDensity;
        if (mGradientIndex > mSlidableLength) {
            mGradientIndex = 0;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mTimer.cancel();
        mTimerTask.cancel();
    }
}
