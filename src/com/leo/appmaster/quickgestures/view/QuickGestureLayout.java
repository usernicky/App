
package com.leo.appmaster.quickgestures.view;

import com.leo.appmaster.R;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class QuickGestureLayout extends ViewGroup {
    // private QuickLauncherLayoutContainer mParent;

    private int mTotalWidth, mTotalHeight;
    private int mItemSize;
    private int mInnerRadius, mOuterRadius;
    private int mRingCount;

    /*
     * 0: left; 1: right
     */
    private int mType;
    private static final int INNER_RING_MAX_COUNT = 4;

    public QuickGestureLayout(Context context) {
        this(context, null);
    }

    public QuickGestureLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

        init();
    }

    private void init() {
        Resources res = getContext().getResources();
        mItemSize = res.getDimensionPixelSize(R.dimen.qg_item_size);
        mInnerRadius = res.getDimensionPixelSize(R.dimen.qg_layout_inner_radius);
        mOuterRadius = res.getDimensionPixelSize(R.dimen.qg_layout_outer_radius);
        mRingCount = 1;
        mType = 0;
    }

    public QuickGestureLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int count = getChildCount();
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mItemSize, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mItemSize, MeasureSpec.EXACTLY);
        // mParent = (QuickLauncherLayoutContainer) this.getParent();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }

        mTotalWidth = getMeasuredWidth();
        mTotalHeight = getMeasuredHeight();

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        // Laying out the child views
        final int childCount = getChildCount();
        if (childCount == 0) {
            return;
        }

        /*
         * first ring max count is 4
         */
        if (childCount > INNER_RING_MAX_COUNT) {
            mRingCount = 2;
        }

        int innerRingCount, outerRingCount;
        float innertAngleInterval, outerAngleInterval;

        if (mRingCount == 1) {
            innerRingCount = childCount;
            outerRingCount = 0;
            innertAngleInterval = 90f / innerRingCount;
            outerAngleInterval = 0;
        } else {
            innerRingCount = INNER_RING_MAX_COUNT;
            outerRingCount = childCount - innerRingCount;
            innertAngleInterval = 90f / innerRingCount;
            outerAngleInterval = 90f / outerRingCount;
        }

        int left = 0, top = 0;

        float innerStartAngle, outerStartAngle;
        innerStartAngle = 90 - innertAngleInterval / 2;
        outerStartAngle = 90 - outerAngleInterval / 2;

        float halfItemSize = mItemSize / 2.0f;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            LayoutParams params = (LayoutParams) child.getLayoutParams();

            if (params.position < 0)
                continue;

            if (params.position < innerRingCount) { // match first ring
                if (mType == 0) {
                    left = (int) (mInnerRadius
                            * Math.cos(Math.toRadians(innerStartAngle - params.position
                                    * innertAngleInterval)) - halfItemSize);
                } else {
                    // TODO right
                    left = mTotalWidth - (int) (mInnerRadius
                            * Math.cos(Math.toRadians(innerStartAngle - params.position
                                    * innertAngleInterval)) - halfItemSize);
                }

                top = (int) (mTotalHeight - mInnerRadius
                        * Math.sin(Math.toRadians(innerStartAngle - params.position
                                * innertAngleInterval)) - halfItemSize);
            } else { // match second ring
                if (mType == 0) {
                    left = (int) (mOuterRadius
                            * Math.cos(Math.toRadians(outerStartAngle
                                    - (params.position - innerRingCount)
                                    * outerAngleInterval)) - halfItemSize);
                } else {
                    // TODO right
                    left = mTotalWidth - (int) (mOuterRadius
                            * Math.cos(Math.toRadians(outerStartAngle
                                    - (params.position - innerRingCount)
                                    * outerAngleInterval)) - halfItemSize);
                }

                top = (int) (mTotalHeight
                        - mOuterRadius
                        * Math.sin(Math.toRadians(outerStartAngle
                                - (params.position - innerRingCount)
                                * outerAngleInterval)) - halfItemSize);
            }

            child.layout(left, top, left + mItemSize, top + mItemSize);
        }

        
        setPivotX(0f);
        setPivotY(mTotalHeight);
    }

    @Override
    public void removeView(View view) {
        LayoutParams params = null;
        int removePosition = ((LayoutParams) view.getLayoutParams()).position;
        for (int i = 0; i < getChildCount(); i++) {
            params = (LayoutParams) getChildAt(i).getLayoutParams();
            if (params.position > removePosition) {
                params.position--;
            }
        }
        super.removeView(view);
    }

    @Override
    public void addView(View child) {
        int addPosition = ((LayoutParams) child.getLayoutParams()).position;
        LayoutParams params = null;
        for (int i = 0; i < getChildCount(); i++) {
            params = (LayoutParams) getChildAt(i).getLayoutParams();
            if (params.position >= addPosition) {
                params.position++;
            }
        }
        super.addView(child);
    }

    public int pointToPosition(float x, float y) {
        for (int i = 0; i < getChildCount(); i++) {
            View item = (View) getChildAt(i);
            if (item.getLeft() < x && item.getRight() > x & item.getTop() < y
                    && item.getBottom() > y) {
                return i;
            }
        }
        return -1;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        public int position = -1;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

    }

}
