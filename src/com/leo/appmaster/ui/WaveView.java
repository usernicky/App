package com.leo.appmaster.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.view.View;


public class WaveView extends View {  
    // 波纹颜色  
    private static final int WAVE_PAINT_COLOR = 0x8800aa00;  //TODO 两个波浪分别使用不同颜色
   
    // y = Asin(wx+b)+h  
    
    private static final float STRETCH_FACTOR_A = 10;  //公式中的A值，波的高点和低点与水平中线的距离
    private static final int OFFSET_Y = 0;  
    // 第一条水波移动速度  
    private static final int TRANSLATE_X_SPEED_ONE = 9;  
    // 第二条水波移动速度  
    private static final int TRANSLATE_X_SPEED_TWO = 7;  
    
    private float mCycleFactorW;  //完整波的周期
    private float mPercent = 0;	//波纹高度占满View的百分比
    private int mTotalWidth, mTotalHeight;  //整体宽高
    private float[] mYPositions;  
    private float[] mResetOneYPositions;  
    private float[] mResetTwoYPositions;  
    private int mXOffsetSpeedOne;  
    private int mXOffsetSpeedTwo;  
    private int mXOneOffset;  
    private int mXTwoOffset;  
  
    private Paint mWavePaint;  
    private DrawFilter mDrawFilter;  
  
    public WaveView(Context context, AttributeSet attrs) {  
        super(context, attrs);  
        // 将dp转化为px，用于控制不同分辨率上移动速度基本一致  
        mXOffsetSpeedOne = TRANSLATE_X_SPEED_ONE;  //TODO dip2px
        mXOffsetSpeedTwo = TRANSLATE_X_SPEED_TWO;  //TODO dip2px
  
        // 初始绘制波纹的画笔  
        mWavePaint = new Paint();  
        // 去除画笔锯齿  
        mWavePaint.setAntiAlias(true);  
        // 设置风格为实线  
        mWavePaint.setStyle(Style.FILL);  
        // 设置画笔颜色  
        mWavePaint.setColor(WAVE_PAINT_COLOR);  
        mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);  
    }  
  
    public void setPercent(float percent) {
    	if (percent < 0) {
    		percent = 0;
    	}
    	if (percent > 100) {
    		percent = 100;
    	}
    	mPercent = percent;
    }
    
    public float getPercent() {
    	return mPercent;
    }
    @Override  
    protected void onDraw(Canvas canvas) {  
        super.onDraw(canvas);  
        // 从canvas层面去除绘制时锯齿  
        canvas.setDrawFilter(mDrawFilter);  
        resetPositonY();  
        for (int i = 0; i < mTotalWidth; i++) {  
        	if (mPercent != 0) {
        		// 绘制第一条水波纹  
        		canvas.drawLine(i, mTotalHeight - mResetOneYPositions[i] - (STRETCH_FACTOR_A + 7) - (mPercent/100 * (mTotalHeight - mResetOneYPositions[i] - (STRETCH_FACTOR_A + 7))), i, mTotalHeight, mWavePaint);  
        		// 绘制第二条水波纹
        		canvas.drawLine(i, mTotalHeight - mResetTwoYPositions[i] - (STRETCH_FACTOR_A + 8) - (mPercent/100 * (mTotalHeight - mResetOneYPositions[i] - (STRETCH_FACTOR_A + 8))), i, mTotalHeight, mWavePaint);  
        	}
        }  
  
        // 改变两条波纹的移动点  
        mXOneOffset += mXOffsetSpeedOne;  
        mXTwoOffset += mXOffsetSpeedTwo;  
  
        // 如果已经移动到结尾处，则重头记录  
        if (mXOneOffset >= mTotalWidth) {  
            mXOneOffset = 0;  
        }  
        if (mXTwoOffset > mTotalWidth) {  
            mXTwoOffset = 0;  
        }  
        postInvalidateDelayed(12);  
    }  
  
    private void resetPositonY() {  
        // mXOneOffset代表当前第一条水波纹要移动的距离  
        int yOneInterval = mYPositions.length - mXOneOffset;  
        // 使用System.arraycopy方式重新填充第一条波纹的数据  
        System.arraycopy(mYPositions, mXOneOffset, mResetOneYPositions, 0, yOneInterval);  
        System.arraycopy(mYPositions, 0, mResetOneYPositions, yOneInterval, mXOneOffset);  
  
        int yTwoInterval = mYPositions.length - mXTwoOffset;  
        System.arraycopy(mYPositions, mXTwoOffset, mResetTwoYPositions, 0,  
                yTwoInterval);  
        System.arraycopy(mYPositions, 0, mResetTwoYPositions, yTwoInterval, mXTwoOffset);  
    }  
  
    @Override  
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {  
        super.onSizeChanged(w, h, oldw, oldh);  
        // 记录下view的宽高  
        mTotalWidth = w;  
        mTotalHeight = h;  
        // 用于保存原始波纹的y值  
        mYPositions = new float[mTotalWidth];  
        // 用于保存波纹一的y值  
        mResetOneYPositions = new float[mTotalWidth];  
        // 用于保存波纹二的y值  
        mResetTwoYPositions = new float[mTotalWidth];  
        // 将周期定为view总宽度  
        mCycleFactorW = (float) (2 * Math.PI / mTotalWidth);  
        // 根据view总宽度得出所有对应的y值  
        for (int i = 0; i < mTotalWidth; i++) {  
            mYPositions[i] = (float) (STRETCH_FACTOR_A * Math.sin(mCycleFactorW * i) + OFFSET_Y);  
        }  
    }  
}