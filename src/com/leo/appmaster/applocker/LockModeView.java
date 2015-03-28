
package com.leo.appmaster.applocker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.leo.appmaster.R;
import com.leo.appmaster.utils.DipPixelUtil;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.Utilities;

public class LockModeView extends View {

    private final static float LOCK_COUNT_TEXT_POS_PERCENT = 0.22f;
    private final static float LOCK_MODE_TEXT_POS_PERCENT = 0.82f;
    private final static float TEXT_WIDTH_PERCENT = 0.5f;

    private Paint mPaint;
    private FontMetrics mFontMetrics = new FontMetrics();

    private Drawable mBgIcon, mMaskIcon, mMoveIcon;

    private int mDrawPadding;
    private int mTextPadding;

    private Rect mBgIconDrawBount = new Rect();
    private Rect mMoveIconDrawBount = new Rect();
    private int mMoveIconY;
    private int mMoveIconTop;
    private int mMoveIconBottom;
    private int mGap;

    private int mLockCountTextSize;
    private int mTipTextSize;
    private int mLockModeTextSize;

    private String mCountText;
    private String mModeText;
    private String mTipText;

    private Handler mHandler;
    private boolean mAnimate;

    private int mDatalY;
    private int mRefreshRate = 10;
    private int mDuration = 1000;

    private int mMoveIconHeight;

    public LockModeView(Context context) {
        this(context, null);
    }

    public LockModeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LockModeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources res = getResources();

        mDrawPadding = res.getDimensionPixelSize(R.dimen.privacy_level_padding);
        mTextPadding = res.getDimensionPixelSize(R.dimen.home_lock_text_padding);
        mLockCountTextSize = res.getDimensionPixelSize(R.dimen.home_lock_count_text_size);
        mTipTextSize = res.getDimensionPixelSize(R.dimen.home_lock_tip_size);
        mLockModeTextSize = res.getDimensionPixelSize(R.dimen.home_lock_mode_name_size);
        mGap = DipPixelUtil.dip2px(context, 18);

        mTipText = res.getString(R.string.aready_lock);

        mBgIcon = res.getDrawable(R.drawable.mode_bar_bg);
        mMaskIcon = res.getDrawable(R.drawable.mode_bar_bg_mask);
        mMoveIcon = res.getDrawable(R.drawable.mode_bar_move);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (mAnimate) {
                    invalidate();
                    mHandler.sendEmptyMessageDelayed(0, mRefreshRate);
                }
                super.handleMessage(msg);
            }

        };
    }

    public void startAnimation() {
        if (mAnimate == false) {
            mAnimate = true;
            mHandler.sendEmptyMessage(0);
        }
    }

    public void stopAnimation() {
        if (mAnimate) {
            mAnimate = false;
            mHandler.removeMessages(0);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        computeDrawBounds(right - left, bottom - top);
    }

    private void computeDrawBounds(int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        int contentW = width - 2 * mDrawPadding;
        int contentH = height - 2 * mDrawPadding;

        // compute bg rect
        int iconW = mBgIcon.getIntrinsicWidth();
        int iconH = mBgIcon.getIntrinsicHeight();
        float scaleW = (float) contentW / iconW;
        float scaleH = (float) contentH / iconH;
        float scale = scaleW < scaleH ? scaleW : scaleH;
        int drawW = (int) (iconW * scale);
        int drawH = (int) (iconH * scale);
        mBgIconDrawBount.left = centerX - drawW / 2;
        mBgIconDrawBount.right = mBgIconDrawBount.left + drawW;
        mBgIconDrawBount.top = centerY - drawH / 2;
        mBgIconDrawBount.bottom = mBgIconDrawBount.top + drawH;

        // cpmpute move icon rect
        mMoveIconY = centerY + drawH / 2;
        iconW = mMoveIcon.getIntrinsicWidth();
        iconH = mMoveIcon.getIntrinsicHeight();
        drawW = (int) (iconW * scale);
        mMoveIconHeight = (int) (iconH * scale);

        mGap *= scale;
        mMoveIconTop = (mBgIconDrawBount.top + mGap - drawH);
        mMoveIconBottom = mBgIconDrawBount.bottom - mGap;

        mMoveIconY = mMoveIconBottom;
        mMoveIconDrawBount.left = centerX - drawW / 2;
        mMoveIconDrawBount.right = mMoveIconDrawBount.left + drawW;
        mMoveIconDrawBount.top = mMoveIconY;
        mMoveIconDrawBount.bottom = mMoveIconDrawBount.top + mMoveIconHeight;
        mDatalY = (mMoveIconBottom - mMoveIconTop) / (mDuration / mRefreshRate);

    }

    @Override
    public void draw(Canvas canvas) {
        // bg icon
        mBgIcon.setBounds(mBgIconDrawBount);
        mBgIcon.draw(canvas);

        mMoveIconY -= mDatalY;
        if (mMoveIconY < mMoveIconTop) {
            mMoveIconY = mMoveIconBottom;
        }
        mMoveIconDrawBount.top = mMoveIconY;
        mMoveIconDrawBount.bottom = mMoveIconDrawBount.top + mMoveIconHeight;
        // move icon
        mMoveIcon.setBounds(mMoveIconDrawBount);
        mMoveIcon.draw(canvas);

        // mask icon
        mMaskIcon.setBounds(mBgIconDrawBount);
        mMaskIcon.draw(canvas);

        int drawW = mBgIconDrawBount.width();
        int drawH = mBgIconDrawBount.height();
        int maxTextWidth = (int) (drawW * TEXT_WIDTH_PERCENT);
        int countBottom = 0;
        int textWidth, realSize;
        // count text
        if (!Utilities.isEmpty(mCountText)) {
            int centerY = mBgIconDrawBount.top + drawH / 2;
            textWidth = computeTextSize(mCountText, mLockCountTextSize, maxTextWidth, mPaint);
            realSize = (int) mPaint.getTextSize();
            mPaint.getFontMetrics(mFontMetrics);
            int offset = (int) Math.abs(mFontMetrics.ascent) - 2;
            countBottom = mBgIconDrawBount.top + ((int) (drawH * LOCK_COUNT_TEXT_POS_PERCENT))
                    + offset;
            if (countBottom > centerY) {
                countBottom = centerY;
            }
            mPaint.setTextSize(realSize);
            mPaint.setStyle(Style.FILL);
            mPaint.setColor(Color.WHITE);
            canvas.drawText(mCountText, mBgIconDrawBount.left + (drawW - textWidth) / 2,
                    countBottom,
                    mPaint);
        }

        // tip text
        textWidth = computeTextSize(mTipText, mTipTextSize, maxTextWidth, mPaint);
        realSize = (int) mPaint.getTextSize();
        mPaint.getFontMetrics(mFontMetrics);
        int offset = (int) Math.ceil(mFontMetrics.descent - mFontMetrics.ascent) + 1;
        mPaint.setTextSize(realSize);
        mPaint.setStyle(Style.FILL);
        mPaint.setColor(Color.WHITE);
        canvas.drawText(mTipText, mBgIconDrawBount.left + (drawW - textWidth) / 2, countBottom
                + mTextPadding + offset, mPaint);

        // mode text
        if (!Utilities.isEmpty(mModeText)) {
            textWidth = computeTextSize(mModeText, mLockModeTextSize, maxTextWidth, mPaint);
            realSize = (int) mPaint.getTextSize();
            mPaint.setTextSize(realSize);
            mPaint.setStyle(Style.FILL);
            mPaint.setColor(Color.WHITE);
            canvas.drawText(mModeText, mBgIconDrawBount.left + (drawW - textWidth) / 2,
                    mBgIconDrawBount.top + (int) (drawH * LOCK_MODE_TEXT_POS_PERCENT), mPaint);
        }
    }

    private int computeTextSize(String text, int maxTextSize, int maxWidth, Paint paint) {
        int textSize = maxTextSize;
        paint.setTextSize(textSize);
        int textWidth = (int) paint.measureText(text);
        while (textWidth > maxWidth) {
            textSize = textSize - 1;
            paint.setTextSize(textSize);
            textWidth = (int) paint.measureText(text);
        }
        return textWidth;
    }

    public void updateMode(String mode, String count) {
        mModeText = mode;
        mCountText = count;
        invalidate(mBgIconDrawBount);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                int upX = (int) event.getX();
                int upY = (int) event.getY();
                if (mBgIconDrawBount.contains(upX, upY)) {
                    performClick();
                }
                break;
        }
        return true;
    }
    
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        try {
            super.onRestoreInstanceState(state);
        } catch (Exception e) {          
        }
    }

}
