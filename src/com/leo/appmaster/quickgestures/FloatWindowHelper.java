
package com.leo.appmaster.quickgestures;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.R;
import com.leo.appmaster.applocker.manager.LockManager;
import com.leo.appmaster.eventbus.event.QuickGestureFloatWindowEvent;
import com.leo.appmaster.quickgestures.ui.QuickGesturePopupActivity;
import com.leo.appmaster.quickgestures.view.QuickGesturesAreaView;
import com.leo.appmaster.quickgestures.view.SectorQuickGestureContainer;
import com.leo.appmaster.utils.DipPixelUtil;
import com.leo.appmaster.utils.Utilities;

/**
 * QuickGestureWindowManager
 * 
 * @author run
 */
@SuppressLint("InflateParams")
public class FloatWindowHelper {
    public static final String QUICK_GESTURE_SETTING_DIALOG_LEFT_RADIO_FINISH_NOTIFICATION = "quick_gesture_setting_dialog_left_radio_finish_notification";
    public static final String QUICK_GESTURE_SETTING_DIALOG_RIGHT_RADIO_FINISH_NOTIFICATION = "quick_gesture_setting_dialog_right_radio_finish_notification";
    public static final String QUICK_GESTURE_SETTING_DIALOG_RADIO_SLIDE_TIME_SETTING_FINISH_NOTIFICATION = "quick_gesture_setting_finish_notification";
    public static final String QUICK_GESTURE_LEFT_SLIDE_AREA = "left_slide_area";
    public static final String QUICK_GESTURE_RIGHT_SLIDE_AREA = "right_slide_area";
    public static final int ONTUCH_LEFT_FLAG = -1;
    public static final int ONTUCH_RIGHT_FLAG = 1;
    public static final String QUICK_GESTURE_MSM_TIP = "quick_gesture_msm_tip";
    public static final String QUICK_GESTURE_ADD_FREE_DISTURB_NOTIFICATION = "quick_gesture_add_free_disturb_notification";
    private static QuickGesturesAreaView mLeftBottomView, mLeftCenterView, mLeftTopView,
            mLeftCenterCenterView;
    private static QuickGesturesAreaView mRightBottomView, mRightCenterView, mRightTopView,
            mRightCenterCenterView;
    private static LayoutParams mLeftBottomParams, mLeftCenterParams, mLeftTopParams,
            mLeftCenterCenterParams;
    private static LayoutParams mRightBottomParams, mRightCenterParams, mRightTopParams,
            mRightCenterCenterParams;
    private static WindowManager mWindowManager;

    public static boolean mGestureShowing = false;
    public static boolean mEditQuickAreaFlag = false;
    private static float startX;
    private static float startY;
    private static boolean isMoveIng = false;
    public static boolean isFanShowing = false;
    // 左下宽度
    private static float mLeftBottomWidth = 50;
    // 左下高度
    private static float mLeftBottomHeight = 25;
    // 左中宽度
    private static float mLeftCenterWidth = 25;
    // 左中高度
    private static float mLeftCenterHeight = 50;
    // 左侧中部高度
    private static float mLeftCenterCenterHeight = 250;
    // 左上宽度
    private static float mLeftTopWidth = 13;
    // 左上高度
    private static float mLeftTopHeight = 75;
    // 右下宽度
    private static float mRightBottomWidth = 50;
    // 右下高度
    private static float mRightBottomHeight = 25;
    // 右中宽度
    private static float mRightCenterWidth = 25;
    // 右中高度
    private static float mRightCenterHeight = 50;
    // 右侧中部高度
    private static float mRightCenterCenterHeight = 250;
    // 右上宽度
    private static float mRightTopWidth = 13;
    // 右上高度
    private static float mRightTopHeight = 75;
    // 距离底部的距离
    private static float mMarginBottom = 200;
    private static final int LEFT_BOTTOM_FLAG = 1;
    private static final int LEFT_CENTER_FLAG = 2;
    private static final int LEFT_TOP_FLAG = 3;
    private static final int LEFT_CENTER_CENTER_FLAG = 4;
    private static final int RIGHT_BOTTOM_FLAG = -1;
    private static final int RIGHT_CENTER_FLAG = -2;
    private static final int RIGHT_CENTER_CENTER_FLAG = -3;
    private static final int RIGHT_TOP_FLAG = -4;

    // 左下
    /**
     * must call in UI thread
     * 
     * @param context
     */
    public static void createFloatLeftBottomWindow(final Context mContext, int value) {
        final WindowManager windowManager = getWindowManager(mContext);
        if (mLeftBottomView == null) {
            mLeftBottomView = new QuickGesturesAreaView(mContext);
            if (QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage
                    && QuickGestureManager.getInstance(mContext).onTuchGestureFlag == -1) {
                mLeftBottomView.setIsShowReadTip(true, 1);
            }
            mLeftBottomView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_OUTSIDE:
                            break;
                        case MotionEvent.ACTION_DOWN:
                            isMoveIng = false;
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float moveX = Math.abs(startX - event.getRawX());
                            float moveY = Math.abs(startY - event.getRawY());
                            float presssure=event.getPressure();
                            if (( (moveX > mLeftBottomParams.width / 8 || moveY > mLeftBottomParams.height / 6 ||presssure>0.03)
                            && !isMoveIng)) {
                                isMoveIng = true;
                                if (!mEditQuickAreaFlag) {
                                    removeAllFloatWindow(mContext);
                                    onTouchAreaShowQuick(-1);
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            isMoveIng = false;
                            if (Math.abs(startX - event.getRawX()) < 10
                                    || Math.abs(startY - event.getRawY()) < 10) {
                                // 去除系统短信未读提示
                                if (QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage) {
                                    AppMasterPreference.getInstance(mContext).setLastTimeLayout(1);
                                    Intent intent = new Intent(mContext,
                                            QuickGesturePopupActivity.class);
                                    try {
                                        mContext.startActivity(intent);
                                        QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage = false;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                removeSwipWindow(mContext, 1);
                            }
                            break;

                        case MotionEvent.ACTION_CANCEL:
                            isMoveIng = false;
                            break;
                    }
                    return false;
                }
            });
            int width = windowManager.getDefaultDisplay().getWidth();
            int height = windowManager.getDefaultDisplay().getHeight();
            int flag = Utilities.isScreenType(mContext);
            if (mLeftBottomParams == null) {
                mLeftBottomParams = new LayoutParams();
                mLeftBottomParams.width = (int) ((DipPixelUtil.dip2px(mContext, mLeftBottomWidth) / 2) + (value / 2)) * 2;
                mLeftBottomParams.height = (int) ((DipPixelUtil.dip2px(mContext, mLeftBottomHeight) / 2) + (value)) * 2;
                mLeftBottomParams.x = (int) (-(width / 2) + (mLeftBottomParams.width / 2));
                mLeftBottomParams.y = (int) ((height / 2) - (mLeftBottomParams.height / 2));
                mLeftBottomParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
                mLeftBottomParams.format = PixelFormat.RGBA_8888;
                mLeftBottomParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | LayoutParams.FLAG_NOT_FOCUSABLE;

            } else {
                mLeftBottomParams.x = -(width / 2) + (mLeftBottomParams.width / 2);
                mLeftBottomParams.y = (height / 2) - (mLeftBottomParams.height / 2);
            }
            if (!mGestureShowing) {
                windowManager.addView(mLeftBottomView, mLeftBottomParams);
            } else {
                mLeftBottomView = null;
            }
        }
    }

    // 左中
    /**
     * must call in UI thread
     * 
     * @param context
     */
    public static void createFloatLeftCenterWindow(final Context mContext, int value) {
        final WindowManager windowManager = getWindowManager(mContext);
        if (mLeftCenterView == null) {
            mLeftCenterView = new QuickGesturesAreaView(mContext);
            mLeftCenterView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_OUTSIDE:
                            break;
                        case MotionEvent.ACTION_DOWN:
                            isMoveIng = false;
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float moveX = Math.abs(startX - event.getRawX());
                            float moveY = Math.abs(startY - event.getRawY());
                            float presssure=event.getPressure();
                            if ((moveX > mLeftCenterParams.width / 6 || moveY > mLeftCenterParams.width / 6 || presssure>0.03)
                                    && !isMoveIng) {
                                isMoveIng = true;
                                if (!mEditQuickAreaFlag) {
                                    removeAllFloatWindow(mContext);
                                    onTouchAreaShowQuick(-1);
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            isMoveIng = false;
                            if (Math.abs(startX - event.getRawX()) < 10
                                    || Math.abs(startY - event.getRawY()) < 10) {
                                removeSwipWindow(mContext, 2);
                            }
                            break;
                    }
                    return false;
                }
            });
            int width = windowManager.getDefaultDisplay().getWidth();
            int height = windowManager.getDefaultDisplay().getHeight();
            if (mLeftCenterParams == null) {
                mLeftCenterParams = new LayoutParams();
                mLeftCenterParams.width = (int) ((DipPixelUtil.dip2px(mContext, mLeftCenterWidth) / 2) + (value / 2)) * 2;
                mLeftCenterParams.height = (int) ((DipPixelUtil.dip2px(mContext, mLeftCenterHeight) / 2) + (value)) * 2;
                mLeftCenterParams.x = (int) (-(width / 2) + (mLeftCenterParams.width / 2));
                mLeftCenterParams.y = (int) ((height / 2) - (mLeftCenterParams.height / 2)
                        - mLeftBottomParams.height - DipPixelUtil.dip2px(mContext, 12));
                mLeftCenterParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
                mLeftCenterParams.format = PixelFormat.RGBA_8888;
                mLeftCenterParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | LayoutParams.FLAG_NOT_FOCUSABLE;
            } else {
                mLeftCenterParams.x = (int) (-(width / 2) + (mLeftCenterParams.width / 2));
                mLeftCenterParams.y = (int) ((height / 2) - (mLeftCenterParams.height / 2)
                        - mLeftBottomParams.height - DipPixelUtil.dip2px(mContext, 12));
            }

            if (!mGestureShowing) {
                windowManager.addView(mLeftCenterView, mLeftCenterParams);
            } else {
                mLeftCenterView = null;
            }
        }
    }

    // 左侧中部
    /**
     * must call in UI thread
     * 
     * @param context
     */
    public static void createFloatLeftCenterCenterWindow(final Context mContext, int value) {
        final WindowManager windowManager = getWindowManager(mContext);
        if (mLeftCenterCenterView == null) {
            mLeftCenterCenterView = new QuickGesturesAreaView(mContext);
            if (QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage
                    && QuickGestureManager.getInstance(mContext).onTuchGestureFlag == -1
                    && mLeftBottomView == null) {
                mLeftCenterCenterView.setIsShowReadTip(true, 3);
            }
            mLeftCenterCenterView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_OUTSIDE:
                            break;
                        case MotionEvent.ACTION_DOWN:
                            isMoveIng = false;
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float moveX = Math.abs(startX - event.getRawX());
                            float moveY = Math.abs(startY - event.getRawY());
                            float presssure=event.getPressure();
                            if ((moveX > mLeftCenterCenterParams.width / 6 || moveY > mLeftCenterCenterParams.width / 6 || presssure>0.03)
                                    && !isMoveIng) {
                                isMoveIng = true;
                                if (!mEditQuickAreaFlag) {
                                    removeAllFloatWindow(mContext);
                                    onTouchAreaShowQuick(-2);
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            isMoveIng = false;
                            if (Math.abs(startX - event.getRawX()) < 10
                                    || Math.abs(startY - event.getRawY()) < 10) {
                                // 去除系统短信未读提示
                                if (QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage
                                        && mLeftBottomView == null) {
                                    AppMasterPreference.getInstance(mContext).setLastTimeLayout(1);
                                    Intent intent = new Intent(mContext,
                                            QuickGesturePopupActivity.class);
                                    try {
                                        mContext.startActivity(intent);
                                        QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage = false;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                removeSwipWindow(mContext, 4);
                            }
                            break;
                    }
                    return false;
                }
            });
            int width = windowManager.getDefaultDisplay().getWidth();
            int height = windowManager.getDefaultDisplay().getHeight();
            int leftBottom = (int) ((DipPixelUtil.dip2px(mContext, mLeftBottomHeight) / 2) + (value)) * 2;
            int leftCenter = (int) ((DipPixelUtil.dip2px(mContext, mLeftCenterHeight) / 2) + (value)) * 2;
            if (mLeftCenterCenterParams == null) {
                mLeftCenterCenterParams = new LayoutParams();
                mLeftCenterCenterParams.width = (int) ((DipPixelUtil.dip2px(mContext,
                        mLeftCenterWidth) / 2) + (value / 2)) * 2;
                mLeftCenterCenterParams.height = (int) ((DipPixelUtil.dip2px(mContext,
                        mLeftCenterCenterHeight) / 2) + (value)) * 2;
                mLeftCenterCenterParams.x = (int) (-(width / 2) + (mLeftCenterCenterParams.width / 2));
                if (mLeftBottomView != null) {
                    mLeftCenterCenterParams.y = (int) ((height / 2)
                            - (mLeftCenterCenterParams.height / 2) - mLeftBottomParams.height
                            - mLeftCenterParams.height - DipPixelUtil
                            .dip2px(mContext, 12));
                } else {
                    mLeftCenterCenterParams.y = (int) ((height / 2)
                            - (mLeftCenterCenterParams.height / 2)
                            - leftBottom - leftCenter - DipPixelUtil
                            .dip2px(mContext, 12));
                }
                mLeftCenterCenterParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
                mLeftCenterCenterParams.format = PixelFormat.RGBA_8888;
                mLeftCenterCenterParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | LayoutParams.FLAG_NOT_FOCUSABLE;
            } else {
                mLeftCenterCenterParams.x = (int) (-(width / 2) + (mLeftCenterCenterParams.width / 2));
                if (mLeftBottomView != null) {
                    mLeftCenterCenterParams.y = (int) ((height / 2)
                            - (mLeftCenterCenterParams.height / 2) - mLeftBottomParams.height
                            - mLeftCenterParams.height - DipPixelUtil
                            .dip2px(mContext, 12));
                } else {
                    mLeftCenterCenterParams.y = (int) ((height / 2)
                            - (mLeftCenterCenterParams.height / 2) - leftBottom - leftCenter - DipPixelUtil
                            .dip2px(mContext, 12));
                }
            }
            if (!mGestureShowing) {
                windowManager.addView(mLeftCenterCenterView, mLeftCenterCenterParams);
            } else {
                mLeftCenterCenterView = null;
            }
        }
    }

    // 左上
    /**
     * must call in UI thread
     * 
     * @param context
     */
    public static void createFloatLeftTopWindow(final Context mContext, int value) {
        final WindowManager windowManager = getWindowManager(mContext);
        if (mLeftTopView == null) {
            mLeftTopView = new QuickGesturesAreaView(mContext);
            mLeftTopView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_OUTSIDE:
                            break;
                        case MotionEvent.ACTION_DOWN:
                            isMoveIng = false;
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float moveX = Math.abs(startX - event.getRawX());
                            float moveY = Math.abs(startY - event.getRawY());
                            float presssure=event.getPressure();
                            if ((moveX > mLeftTopParams.width / 6 || moveY > mLeftTopParams.width / 6 || presssure>0.03)
                                    && !isMoveIng) {
                                isMoveIng = true;
                                if (!mEditQuickAreaFlag) {
                                    removeAllFloatWindow(mContext);
                                    onTouchAreaShowQuick(-1);
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            isMoveIng = false;
                            if (Math.abs(startX - event.getRawX()) < 10
                                    || Math.abs(startY - event.getRawY()) < 10) {
                                removeSwipWindow(mContext, 3);
                            }
                            break;
                    }
                    return false;
                }
            });
            int width = windowManager.getDefaultDisplay().getWidth();
            int height = windowManager.getDefaultDisplay().getHeight();
            if (mLeftTopParams == null) {
                mLeftTopParams = new LayoutParams();
                mLeftTopParams.width = (int) ((DipPixelUtil.dip2px(mContext, mLeftTopWidth) / 2) + (value / 2)) * 2;
                mLeftTopParams.height = (int) ((DipPixelUtil.dip2px(mContext, mLeftTopHeight) / 2) + (value)) * 2;
                mLeftTopParams.x = (int) (-(width / 2) + (mLeftTopParams.width / 2));
                mLeftTopParams.y = (int) ((height / 2) - (mLeftTopParams.height / 2)
                        - mLeftBottomParams.height - mLeftCenterParams.height - DipPixelUtil
                        .dip2px(mContext, 12));
                mLeftTopParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
                mLeftTopParams.format = PixelFormat.RGBA_8888;
                mLeftTopParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | LayoutParams.FLAG_NOT_FOCUSABLE;
            } else {
                mLeftTopParams.x = (int) (-(width / 2) + (mLeftTopParams.width / 2));
                mLeftTopParams.y = (int) ((height / 2) - (mLeftTopParams.height / 2)
                        - mLeftBottomParams.height - mLeftCenterParams.height - DipPixelUtil
                        .dip2px(mContext, 12));
            }

            if (!mGestureShowing) {
                windowManager.addView(mLeftTopView, mLeftTopParams);
            } else {
                mLeftTopView = null;
            }
        }
    }

    // 右下
    /**
     * must call in UI thread
     * 
     * @param context
     */
    public static void createFloatRightBottomWindow(final Context mContext, int value) {
        final WindowManager windowManager = getWindowManager(mContext);
        if (mRightBottomView == null) {
            mRightBottomView = new QuickGesturesAreaView(mContext);
            if (QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage
                    && QuickGestureManager.getInstance(mContext).onTuchGestureFlag == 1) {
                mRightBottomView.setIsShowReadTip(true, 2);
            }
            mRightBottomView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_OUTSIDE:
                            break;
                        case MotionEvent.ACTION_DOWN:
                            isMoveIng = false;
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float moveX = Math.abs(startX - event.getRawX());
                            float moveY = Math.abs(startY - event.getRawY());
                            float presssure=event.getPressure();
                            if ((moveX > mRightBottomParams.width / 8 || moveY > mRightBottomParams.height / 6 || presssure>0.03)
                                    && !isMoveIng) {
                                isMoveIng = true;
                                if (!mEditQuickAreaFlag) {
                                    removeAllFloatWindow(mContext);
                                    onTouchAreaShowQuick(1);
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            isMoveIng = false;
                            if (Math.abs(startX - event.getRawX()) < 10
                                    || Math.abs(startY - event.getRawY()) < 10) {
                                // 去除系统短信未读提示
                                if (QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage) {
                                    AppMasterPreference.getInstance(mContext).setLastTimeLayout(1);
                                    Intent intent = new Intent(mContext,
                                            QuickGesturePopupActivity.class);
                                    try {
                                        mContext.startActivity(intent);
                                        QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage = false;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                removeSwipWindow(mContext, -1);
                            }
                            break;
                    }
                    return false;
                }
            });
            int width = windowManager.getDefaultDisplay().getWidth();
            int height = windowManager.getDefaultDisplay().getHeight();
            if (mRightBottomParams == null) {
                mRightBottomParams = new LayoutParams();
                mRightBottomParams.width = (int) ((DipPixelUtil.dip2px(mContext, mRightBottomWidth) / 2) + (value / 2)) * 2;
                mRightBottomParams.height = (int) ((DipPixelUtil.dip2px(mContext,
                        mRightBottomHeight) / 2) + (value)) * 2;
                mRightBottomParams.x = (int) ((width / 2) + (mRightBottomParams.width / 2));
                mRightBottomParams.y = (int) ((height / 2) - (mRightBottomParams.height / 2));
                mRightBottomParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
                mRightBottomParams.format = PixelFormat.RGBA_8888;
                mRightBottomParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | LayoutParams.FLAG_NOT_FOCUSABLE;
            } else {
                mRightBottomParams.x = (int) ((width / 2) + (mRightBottomParams.width / 2));
                mRightBottomParams.y = (int) ((height / 2) - (mRightBottomParams.height / 2));
            }

            if (!mGestureShowing) {
                windowManager.addView(mRightBottomView, mRightBottomParams);
            } else {
                mRightBottomView = null;
            }
        }
    }

    // 右中
    /**
     * must call in UI thread
     * 
     * @param context
     */
    public static void createFloatRightCenterWindow(final Context mContext, int value) {
        final WindowManager windowManager = getWindowManager(mContext);
        if (mRightCenterView == null) {
            mRightCenterView = new QuickGesturesAreaView(mContext);
            mRightCenterView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_OUTSIDE:
                            break;
                        case MotionEvent.ACTION_DOWN:
                            isMoveIng = false;
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float moveX = Math.abs(startX - event.getRawX());
                            float moveY = Math.abs(startY - event.getRawY());
                            float presssure=event.getPressure();
                            if ((moveX > mRightCenterParams.width / 6 || moveY > mRightCenterParams.width / 6 || presssure>0.03)
                                    && !isMoveIng) {
                                isMoveIng = true;
                                if (!mEditQuickAreaFlag) {
                                    removeAllFloatWindow(mContext);
                                    onTouchAreaShowQuick(1);
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            isMoveIng = false;
                            if (Math.abs(startX - event.getRawX()) < 10
                                    || Math.abs(startY - event.getRawY()) < 10) {
                                removeSwipWindow(mContext, -2);
                            }
                            break;
                    }
                    return false;
                }
            });
            int width = windowManager.getDefaultDisplay().getWidth();
            int height = windowManager.getDefaultDisplay().getHeight();
            if (mRightCenterParams == null) {
                mRightCenterParams = new LayoutParams();
                mRightCenterParams.width = (int) ((DipPixelUtil.dip2px(mContext, mRightCenterWidth) / 2) + (value / 2)) * 2;
                mRightCenterParams.height = (int) ((DipPixelUtil.dip2px(mContext,
                        mRightCenterHeight) / 2) + (value)) * 2;
                mRightCenterParams.x = (int) ((width / 2) + (mRightCenterParams.width / 2));
                mRightCenterParams.y = (int) ((height / 2) - (mRightCenterParams.height / 2)
                        - mRightBottomParams.height - DipPixelUtil.dip2px(mContext, 12));
                mRightCenterParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
                mRightCenterParams.format = PixelFormat.RGBA_8888;
                mRightCenterParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | LayoutParams.FLAG_NOT_FOCUSABLE;
            } else {
                mRightCenterParams.x = (int) ((width / 2) + (mRightCenterParams.width / 2));
                mRightCenterParams.y = (int) ((height / 2) - (mRightCenterParams.height / 2)
                        - mRightBottomParams.height - DipPixelUtil.dip2px(mContext, 12));
            }

            if (!mGestureShowing) {
                windowManager.addView(mRightCenterView, mRightCenterParams);
            } else {
                mRightCenterView = null;
            }
        }
    }

    // 右侧中部
    /**
     * must call in UI thread
     * 
     * @param context
     */
    public static void createFloatRightCenterCenterWindow(final Context mContext, int value) {
        final WindowManager windowManager = getWindowManager(mContext);
        if (mRightCenterCenterView == null) {
            mRightCenterCenterView = new QuickGesturesAreaView(mContext);
            if (QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage
                    && QuickGestureManager.getInstance(mContext).onTuchGestureFlag == 1
                    && mRightBottomView == null) {
                mRightCenterCenterView.setIsShowReadTip(true, 4);
            }
            mRightCenterCenterView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_OUTSIDE:
                            break;
                        case MotionEvent.ACTION_DOWN:
                            isMoveIng = false;
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float moveX = Math.abs(startX - event.getRawX());
                            float moveY = Math.abs(startY - event.getRawY());
                            float presssure=event.getPressure();
                            if ((moveX > mRightCenterCenterParams.width / 6 || moveY > mRightCenterCenterParams.width / 6 || presssure>0.03)
                                    && !isMoveIng) {
                                isMoveIng = true;
                                if (!mEditQuickAreaFlag) {
                                    removeAllFloatWindow(mContext);
                                    onTouchAreaShowQuick(2);
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            isMoveIng = false;
                            if (Math.abs(startX - event.getRawX()) < 10
                                    || Math.abs(startY - event.getRawY()) < 10) {
                                // 去除系统短信未读提示
                                if (QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage
                                        && mRightBottomView == null) {
                                    AppMasterPreference.getInstance(mContext).setLastTimeLayout(1);
                                    Intent intent = new Intent(mContext,
                                            QuickGesturePopupActivity.class);
                                    try {
                                        mContext.startActivity(intent);
                                        QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage = false;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                removeSwipWindow(mContext, -4);
                            }
                            break;
                    }
                    return false;
                }
            });
            int width = windowManager.getDefaultDisplay().getWidth();
            int height = windowManager.getDefaultDisplay().getHeight();
            int rightBottom = (int) ((DipPixelUtil.dip2px(mContext,
                    mRightCenterHeight) / 2) + (value)) * 2;
            int rightCenter = (int) ((DipPixelUtil.dip2px(mContext,
                    mRightBottomHeight) / 2) + (value)) * 2;
            if (mRightCenterCenterParams == null) {
                mRightCenterCenterParams = new LayoutParams();
                mRightCenterCenterParams.width = (int) ((DipPixelUtil.dip2px(mContext,
                        mRightCenterWidth) / 2) + (value / 2)) * 2;
                mRightCenterCenterParams.height = (int) ((DipPixelUtil.dip2px(mContext,
                        mRightCenterCenterHeight) / 2) + (value)) * 2;
                mRightCenterCenterParams.x = (int) ((width / 2) + (mRightCenterCenterParams.width / 2));
                if (mRightBottomView != null) {
                    mRightCenterCenterParams.y = (int) ((height / 2)
                            - (mRightCenterCenterParams.height / 2) - mRightBottomParams.height
                            - mRightCenterParams.height - DipPixelUtil
                            .dip2px(mContext, 12));
                } else {
                    mRightCenterCenterParams.y = (int) ((height / 2)
                            - (mRightCenterCenterParams.height / 2) - rightBottom
                            - rightCenter - DipPixelUtil
                            .dip2px(mContext, 12));
                }
                mRightCenterCenterParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
                mRightCenterCenterParams.format = PixelFormat.RGBA_8888;
                mRightCenterCenterParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | LayoutParams.FLAG_NOT_FOCUSABLE;
            } else {
                if (mRightBottomView != null) {
                    mRightCenterCenterParams.y = (int) ((height / 2)
                            - (mRightCenterCenterParams.height / 2) - mRightBottomParams.height
                            - mRightCenterParams.height - DipPixelUtil
                            .dip2px(mContext, 12));
                } else {
                    mRightCenterCenterParams.y = (int) ((height / 2)
                            - (mRightCenterCenterParams.height / 2) - rightBottom
                            - rightCenter - DipPixelUtil
                            .dip2px(mContext, 12));
                }

            }

            if (!mGestureShowing) {
                windowManager.addView(mRightCenterCenterView, mRightCenterCenterParams);
            } else {
                mRightCenterCenterView = null;
            }
        }
    }

    // 右上
    /**
     * must call in UI thread
     * 
     * @param context
     */
    public static void createFloatRightTopWindow(final Context mContext, int value) {
        final WindowManager windowManager = getWindowManager(mContext);
        if (mRightTopView == null) {
            mRightTopView = new QuickGesturesAreaView(mContext);
            mRightTopView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_OUTSIDE:
                            break;
                        case MotionEvent.ACTION_DOWN:
                            isMoveIng = false;
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float moveX = Math.abs(startX - event.getRawX());
                            float moveY = Math.abs(startY - event.getRawY());
                            float presssure=event.getPressure();
                            if ((moveX > mRightTopParams.width / 6 || moveY > mRightTopParams.width / 6 || presssure>0.03)
                                    && !isMoveIng) {
                                isMoveIng = true;
                                if (!mEditQuickAreaFlag) {
                                    removeAllFloatWindow(mContext);
                                    onTouchAreaShowQuick(1);
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            isMoveIng = false;
                            if (Math.abs(startX - event.getRawX()) < 10
                                    || Math.abs(startY - event.getRawY()) < 10) {
                                removeSwipWindow(mContext, -3);
                            }
                            break;
                    }
                    return false;
                }
            });
            int width = windowManager.getDefaultDisplay().getWidth();
            int height = windowManager.getDefaultDisplay().getHeight();
            if (mRightTopParams == null) {
                mRightTopParams = new LayoutParams();
                mRightTopParams.width = (int) ((DipPixelUtil.dip2px(mContext, mRightTopWidth) / 2) + (value / 2)) * 2;
                mRightTopParams.height = (int) ((DipPixelUtil.dip2px(mContext, mRightTopHeight) / 2) + (value)) * 2;
                mRightTopParams.x = (int) ((width / 2) + (mRightTopParams.width / 2));
                mRightTopParams.y = (int) ((height / 2) - (mRightTopParams.height / 2)
                        - mRightBottomParams.height - mRightCenterParams.height - DipPixelUtil
                        .dip2px(mContext, 12));
                mRightTopParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
                mRightTopParams.format = PixelFormat.RGBA_8888;
                mRightTopParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | LayoutParams.FLAG_NOT_FOCUSABLE;
            } else {
                mRightTopParams.x = (int) ((width / 2) + (mRightTopParams.width / 2));
                mRightTopParams.y = (int) ((height / 2) - (mRightTopParams.height / 2)
                        - mRightBottomParams.height - mRightCenterParams.height - DipPixelUtil
                        .dip2px(mContext, 12));
            }

            if (!mGestureShowing) {
                windowManager.addView(mRightTopView, mRightTopParams);
            } else {
                mRightTopView = null;
            }
        }
    }

    /**
     * 移除悬浮窗口 must call in UI thread
     * 
     * @param context
     */
    public static void removeSwipWindow(Context context, int flag) {
        WindowManager windowManager = getWindowManager(context);
        if (LEFT_BOTTOM_FLAG == flag) {
            // 左下
            if (mLeftBottomView != null) {
                windowManager.removeView(mLeftBottomView);
                mLeftBottomView = null;
            }
        } else if (LEFT_CENTER_FLAG == flag) {
            // 左中
            if (mLeftCenterView != null) {
                windowManager.removeView(mLeftCenterView);
                mLeftCenterView = null;
            }
        } else if (LEFT_TOP_FLAG == flag) {
            // 左上
            if (mLeftTopView != null) {
                windowManager.removeView(mLeftTopView);
                mLeftTopView = null;
            }
        } else if (LEFT_CENTER_CENTER_FLAG == flag) {
            // 左侧中部
            if (mLeftCenterCenterView != null) {
                windowManager.removeView(mLeftCenterCenterView);
                mLeftCenterCenterView = null;
            }
        } else if (RIGHT_BOTTOM_FLAG == flag) {
            // 右下
            if (mRightBottomView != null) {
                windowManager.removeView(mRightBottomView);
                mRightBottomView = null;
            }
        } else if (RIGHT_CENTER_FLAG == flag) {
            // 右中
            if (mRightCenterView != null) {
                windowManager.removeView(mRightCenterView);
                mRightCenterView = null;
            }
        } else if (RIGHT_CENTER_CENTER_FLAG == flag) {
            // 右上
            if (mRightTopView != null) {
                windowManager.removeView(mRightTopView);
                mRightTopView = null;
            }
        } else if (RIGHT_TOP_FLAG == flag) {
            // 右侧中部
            if (mRightCenterCenterView != null) {
                windowManager.removeView(mRightCenterCenterView);
                mRightCenterCenterView = null;
            }
        }
    }

    // 删除所有悬浮窗
    public static void removeAllFloatWindow(Context context) {
        removeSwipWindow(context, 1);
        removeSwipWindow(context, 2);
        removeSwipWindow(context, 3);
        removeSwipWindow(context, 4);
        removeSwipWindow(context, -1);
        removeSwipWindow(context, -2);
        removeSwipWindow(context, -3);
        removeSwipWindow(context, -4);
    }

    // 删除显示红点提示的悬浮窗
    public static void removeShowReadTipWindow(Context context) {
        removeSwipWindow(context, 1);
        removeSwipWindow(context, 4);
        removeSwipWindow(context, -1);
        removeSwipWindow(context, -4);
    }

    /**
     * must call in UI thread
     * 
     * @param context
     */
    public static void updateView(Context context, int value) {
        WindowManager windowManager = getWindowManager(context);
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        int height = display.getHeight();
        int width = display.getWidth();
        int leftBottom = (int) ((DipPixelUtil.dip2px(context, mLeftBottomHeight) / 2) + (value)) * 2;
        int leftCenter = (int) ((DipPixelUtil.dip2px(context, mLeftCenterHeight) / 2) + (value)) * 2;
        int rightBottom = (int) ((DipPixelUtil.dip2px(context,
                mRightCenterHeight) / 2) + (value)) * 2;
        int rightCenter = (int) ((DipPixelUtil.dip2px(context,
                mRightBottomHeight) / 2) + (value)) * 2;
        // 左下
        if (mLeftBottomParams != null) {
            mLeftBottomParams.width = (int) ((DipPixelUtil.dip2px(context, mLeftBottomWidth) / 2) + (value / 2)) * 2;
            mLeftBottomParams.height = (int) ((DipPixelUtil.dip2px(context, mLeftBottomHeight) / 2) + (value)) * 2;
            mLeftBottomParams.x = (int) (-(width / 2) + (mLeftBottomParams.width / 2));
            mLeftBottomParams.y = (int) ((height / 2) - (mLeftBottomParams.height / 2));
        }
        // 左中
        if (mLeftCenterParams != null) {
            mLeftCenterParams.width = (int) ((DipPixelUtil.dip2px(context, mLeftCenterWidth) / 2) + (value / 2)) * 2;
            mLeftCenterParams.height = (int) ((DipPixelUtil.dip2px(context, mLeftCenterHeight) / 2) + (value)) * 2;
            mLeftCenterParams.x = (int) (-(width / 2) + (mLeftCenterParams.width / 2));
            mLeftCenterParams.y = (int) ((height / 2) - (mLeftCenterParams.height / 2)
                    - mLeftBottomParams.height - DipPixelUtil.dip2px(context, 12));
        }
        // 左边中部
        if (mLeftCenterCenterParams != null) {
            mLeftCenterCenterParams.width = (int) ((DipPixelUtil.dip2px(context, mLeftCenterWidth) / 2) + (value / 2)) * 2;
            mLeftCenterCenterParams.height = (int) ((DipPixelUtil.dip2px(context,
                    mLeftCenterCenterHeight) / 2) + (value)) * 2;
            mLeftCenterCenterParams.x = (int) (-(width / 2) + (mLeftCenterCenterParams.width / 2));
            if (mLeftBottomView != null) {
                mLeftCenterCenterParams.y = (int) ((height / 2)
                        - (mLeftCenterCenterParams.height / 2) - mLeftBottomParams.height
                        - mLeftCenterParams.height - DipPixelUtil
                        .dip2px(context, 12));
            } else {
                mLeftCenterCenterParams.y = (int) ((height / 2)
                        - (mLeftCenterCenterParams.height / 2) - leftBottom - leftCenter - DipPixelUtil
                        .dip2px(context, 12));
            }
        }
        // 左上
        if (mLeftTopParams != null) {
            mLeftTopParams.width = (int) ((DipPixelUtil.dip2px(context, mLeftTopWidth) / 2) + (value / 2)) * 2;
            mLeftTopParams.height = (int) ((DipPixelUtil.dip2px(context, mLeftTopHeight) / 2) + (value)) * 2;
            mLeftTopParams.x = (int) (-(width / 2) + (mLeftTopParams.width / 2));
            mLeftTopParams.y = (int) ((height / 2) - (mLeftTopParams.height / 2)
                    - mLeftBottomParams.height - mLeftCenterParams.height - DipPixelUtil.dip2px(
                    context, 12));
        }
        // 右下
        if (mRightBottomParams != null) {
            mRightBottomParams.width = (int) ((DipPixelUtil.dip2px(context, mRightBottomWidth) / 2) + (value / 2)) * 2;
            mRightBottomParams.height = (int) ((DipPixelUtil.dip2px(context, mRightBottomHeight) / 2) + (value)) * 2;
            mRightBottomParams.x = +(width / 2);
            mRightBottomParams.y = (height / 2) - value;
        }
        // 右中
        if (mRightCenterParams != null) {
            mRightCenterParams.width = (int) ((DipPixelUtil.dip2px(context, mRightCenterWidth) / 2) + (value / 2)) * 2;
            mRightCenterParams.height = (int) ((DipPixelUtil.dip2px(context, mRightCenterHeight) / 2) + (value)) * 2;
            mRightCenterParams.x = (int) ((width / 2) + (mRightCenterParams.width / 2));
            mRightCenterParams.y = (int) ((height / 2) - (mRightCenterParams.height / 2)
                    - mRightBottomParams.height - DipPixelUtil.dip2px(context, 12));
        }
        // 右上
        if (mRightTopParams != null) {
            mRightTopParams.width = (int) ((DipPixelUtil.dip2px(context, mRightTopWidth) / 2) + (value / 2)) * 2;
            mRightTopParams.height = (int) ((DipPixelUtil.dip2px(context, mRightTopHeight) / 2) + (value)) * 2;
            mRightTopParams.x = (int) ((width / 2) + (mRightTopParams.width / 2));
            mRightTopParams.y = (int) ((height / 2) - (mRightTopParams.height / 2)
                    - mRightBottomParams.height - mRightCenterParams.height - DipPixelUtil.dip2px(
                    context, 12));
        }
        // 右侧中部
        if (mRightCenterCenterParams != null) {
            mRightCenterCenterParams.width = (int) ((DipPixelUtil
                    .dip2px(context, mRightCenterWidth) / 2) + (value / 2)) * 2;
            mRightCenterCenterParams.height = (int) ((DipPixelUtil.dip2px(context,
                    mRightCenterCenterHeight) / 2) + (value)) * 2;
            if (mRightBottomView != null) {
                mRightCenterCenterParams.y = (int) ((height / 2)
                        - (mRightCenterCenterParams.height / 2) - mRightBottomParams.height
                        - mRightCenterParams.height - DipPixelUtil
                        .dip2px(context, 12));
            } else {
                mRightCenterCenterParams.y = (int) ((height / 2)
                        - (mRightCenterCenterParams.height / 2) - rightBottom
                        - rightCenter - DipPixelUtil.dip2px(
                        context, 12));
            }

        }
        // 更新左边
        if (mLeftBottomView != null) {
            mWindowManager.updateViewLayout(mLeftBottomView, mLeftBottomParams);
        }
        if (mLeftCenterView != null) {
            mWindowManager.updateViewLayout(mLeftCenterView, mLeftCenterParams);
        }
        if (mLeftTopView != null) {
            mWindowManager.updateViewLayout(mLeftTopView, mLeftTopParams);
        }
        if (mLeftCenterCenterView != null) {
            mWindowManager.updateViewLayout(mLeftCenterCenterView, mLeftCenterCenterParams);
        }
        // 更新右边
        if (mRightBottomView != null) {
            mWindowManager.updateViewLayout(mRightBottomView, mRightBottomParams);
        }
        if (mRightCenterView != null) {
            mWindowManager.updateViewLayout(mRightCenterView, mRightCenterParams);
        }
        if (mRightTopView != null) {
            mWindowManager.updateViewLayout(mRightTopView, mRightTopParams);
        }
        if (mRightCenterCenterView != null) {
            mWindowManager.updateViewLayout(mRightCenterCenterView, mRightCenterCenterParams);
        }
    }

    public static boolean isLeftBottomShowing() {
        return mLeftBottomView != null;
    }

    public static boolean isLeftCenterShowing() {
        return mLeftCenterView != null;
    }

    public static boolean isLeftTopShowing() {
        return mLeftTopView != null;
    }

    public static boolean isLeftCenterCenterShowing() {
        return mLeftCenterCenterView != null;
    }

    public static boolean isRightBottomShowing() {
        return mRightBottomView != null;
    }

    public static boolean isRightCenterShowing() {
        return mRightCenterView != null;
    }

    public static boolean isRightTopShowing() {
        return mRightTopView != null;
    }

    public static boolean isRightCenterCenterShowing() {
        return mRightCenterCenterView != null;
    }

    private static WindowManager getWindowManager(Context context) {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }

    /**
     * must call in UI thread
     * 
     * @param context
     */
    public static void createFloatWindow(Context context, int value) {
        if (!FloatWindowHelper.isLeftBottomShowing()
                || !FloatWindowHelper.isLeftCenterShowing()
                || !FloatWindowHelper.isLeftTopShowing()
                || !FloatWindowHelper.isRightBottomShowing()
                || !FloatWindowHelper.isRightCenterShowing()
                || !FloatWindowHelper.isRightTopShowing()
                || !FloatWindowHelper.isLeftCenterCenterShowing()
                || !FloatWindowHelper.isRightCenterCenterShowing()) {
            boolean leftBottom = QuickGestureManager
                    .getInstance(AppMasterApplication.getInstance()).isLeftBottom;
            boolean rightBottom = QuickGestureManager.getInstance(AppMasterApplication
                    .getInstance()).isRightBottom;
            boolean leftCenter = QuickGestureManager
                    .getInstance(AppMasterApplication.getInstance()).isLeftCenter;
            boolean rightCenter = QuickGestureManager.getInstance(AppMasterApplication
                    .getInstance()).isRightCenter;
            // left bottom
            if (leftBottom) {
                FloatWindowHelper
                        .createFloatLeftBottomWindow(context, value);
                FloatWindowHelper
                        .createFloatLeftCenterWindow(context, value);
                FloatWindowHelper
                        .createFloatLeftTopWindow(context, value);
            }
            // right bottom
            if (rightBottom) {
                FloatWindowHelper
                        .createFloatRightBottomWindow(context, value);
                FloatWindowHelper
                        .createFloatRightCenterWindow(context, value);
                FloatWindowHelper
                        .createFloatRightTopWindow(context, value);
            }
            // left center
            if (leftCenter) {
                FloatWindowHelper.removeSwipWindow(context, 3);
                FloatWindowHelper.createFloatLeftCenterCenterWindow(context, value);
                if (leftBottom) {
                    FloatWindowHelper
                            .createFloatLeftBottomWindow(context, value);
                    FloatWindowHelper
                            .createFloatLeftCenterWindow(context, value);
                }
            }
            // right center
            if (rightCenter) {
                FloatWindowHelper.removeSwipWindow(context, -3);
                FloatWindowHelper.createFloatRightCenterCenterWindow(context, value);
                if (rightBottom) {
                    FloatWindowHelper
                            .createFloatRightBottomWindow(context, value);
                    FloatWindowHelper
                            .createFloatRightCenterWindow(context, value);
                }
            }
        }
    }

    /**
     * updateFloatWindowBackgroudColor must call in UI thread
     * 
     * @param flag
     */
    public static void updateFloatWindowBackgroudColor(boolean flag) {
        if (flag) {
            if (mLeftBottomView != null) {
                mLeftBottomView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_show_color);
            }
            if (mLeftCenterView != null) {
                mLeftCenterView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_show_color);
            }
            if (mLeftCenterCenterView != null) {
                mLeftCenterCenterView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_show_color);
            }
            if (mLeftTopView != null) {
                mLeftTopView.setBackgroundResource(R.color.quick_gesture_switch_setting_show_color);
            }
            if (mRightBottomView != null) {
                mRightBottomView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_show_color);
            }
            if (mRightCenterView != null) {
                mRightCenterView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_show_color);
            }
            if (mRightCenterCenterView != null) {
                mRightCenterCenterView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_show_color);
            }
            if (mRightTopView != null) {
                mRightTopView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_show_color);
            }
        } else if (!flag) {
            if (mLeftBottomView != null) {
                mLeftBottomView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_hidden_color);
            }
            if (mLeftCenterView != null) {
                mLeftCenterView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_hidden_color);
            }
            if (mLeftCenterCenterView != null) {
                mLeftCenterCenterView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_hidden_color);
            }
            if (mLeftTopView != null) {
                mLeftTopView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_hidden_color);
            }
            if (mRightBottomView != null) {
                mRightBottomView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_hidden_color);
            }
            if (mRightCenterView != null) {
                mRightCenterView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_hidden_color);
            }
            if (mRightCenterCenterView != null) {
                mRightCenterCenterView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_hidden_color);
            }
            if (mRightTopView != null) {
                mRightTopView
                        .setBackgroundResource(R.color.quick_gesture_switch_setting_hidden_color);
            }
        }
    }

    public static void setShowSlideArea(Context context, String flag) {
        int value = QuickGestureManager.getInstance(context).mSlidAreaSize;
        boolean leftBottom = QuickGestureManager.getInstance(AppMasterApplication.getInstance()).isLeftBottom;
        boolean rightBottom = QuickGestureManager.getInstance(AppMasterApplication.getInstance()).isRightBottom;
        boolean leftCenter = QuickGestureManager.getInstance(AppMasterApplication.getInstance()).isLeftCenter;
        boolean rightCenter = QuickGestureManager.getInstance(AppMasterApplication.getInstance()).isRightCenter;
        if (FloatWindowHelper.QUICK_GESTURE_LEFT_SLIDE_AREA.equals(flag)) {
            if (!leftBottom) {
                FloatWindowHelper.removeSwipWindow(context, 1);
                FloatWindowHelper.removeSwipWindow(context, 2);
                FloatWindowHelper.removeSwipWindow(context, 3);
                if (!leftCenter) {
                    FloatWindowHelper.removeSwipWindow(context, 4);
                } else {
                    FloatWindowHelper.removeSwipWindow(context, 4);
                    FloatWindowHelper
                            .createFloatLeftCenterCenterWindow(context, value);
                }
            } else {
                if (leftCenter) {
                    FloatWindowHelper.removeSwipWindow(context, 4);
                    FloatWindowHelper.removeSwipWindow(context, 3);
                    FloatWindowHelper
                            .createFloatLeftBottomWindow(context, value);
                    FloatWindowHelper
                            .createFloatLeftCenterWindow(context, value);
                    FloatWindowHelper
                            .createFloatLeftCenterCenterWindow(context, value);
                } else {
                    FloatWindowHelper.removeSwipWindow(context, 4);
                    FloatWindowHelper
                            .createFloatLeftBottomWindow(context, value);
                    FloatWindowHelper
                            .createFloatLeftCenterWindow(context, value);
                    FloatWindowHelper
                            .createFloatLeftTopWindow(context, value);
                }
            }
        }
        if (FloatWindowHelper.QUICK_GESTURE_RIGHT_SLIDE_AREA.equals(flag)) {
            if (!rightBottom) {
                FloatWindowHelper.removeSwipWindow(context, -1);
                FloatWindowHelper.removeSwipWindow(context, -2);
                FloatWindowHelper.removeSwipWindow(context, -3);
                if (!rightCenter) {
                    FloatWindowHelper.removeSwipWindow(context, -4);
                } else {
                    FloatWindowHelper.removeSwipWindow(context, -4);
                    FloatWindowHelper
                            .createFloatRightCenterCenterWindow(context, value);
                }
            } else {
                if (rightCenter) {
                    FloatWindowHelper.removeSwipWindow(context, -4);
                    FloatWindowHelper.removeSwipWindow(context, -3);

                    FloatWindowHelper
                            .createFloatRightBottomWindow(context, value);
                    FloatWindowHelper
                            .createFloatRightCenterWindow(context, value);
                    FloatWindowHelper
                            .createFloatRightCenterCenterWindow(context, value);
                } else {
                    FloatWindowHelper.removeSwipWindow(context, -4);
                    FloatWindowHelper
                            .createFloatRightBottomWindow(context, value);
                    FloatWindowHelper
                            .createFloatRightCenterWindow(context, value);
                    FloatWindowHelper
                            .createFloatRightTopWindow(context, value);
                }
            }
            if (FloatWindowHelper.mEditQuickAreaFlag) {
                FloatWindowHelper
                        .updateFloatWindowBackgroudColor(true);
            }
        }
    }

    public static void closeQuickGesture(SectorQuickGestureContainer.Orientation orientation) {
        if (orientation == SectorQuickGestureContainer.Orientation.Left) {
            // sLeftPopup.dismiss();
        } else {
            // sRightPopup.dismiss();s
        }
    }

    private static void onTouchAreaShowQuick(int flag) {
        if (flag == -1 || flag == -2) {
            // cancel message tip
            if (QuickGestureManager.getInstance(AppMasterApplication.getInstance()).isShowSysNoReadMessage) {
                AppMasterPreference.getInstance(AppMasterApplication.getInstance())
                        .setLastTimeLayout(1);
                QuickGestureManager.getInstance(AppMasterApplication.getInstance()).isShowSysNoReadMessage = false;
            }
            Intent intent;
            intent = new Intent(AppMasterApplication.getInstance(), QuickGesturePopupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("show_orientation", 0);
            AppMasterApplication.getInstance().startActivity(intent);
            if (flag == -1) {
                QuickGestureManager.getInstance(AppMasterApplication.getInstance()).onTuchGestureFlag = -1;
            } else if (flag == -2) {
                QuickGestureManager.getInstance(AppMasterApplication.getInstance()).onTuchGestureFlag = -2;
            }
        } else if (flag == 1 || flag == 2) {
            // cancel message tip
            if (QuickGestureManager.getInstance(AppMasterApplication.getInstance()).isShowSysNoReadMessage) {
                AppMasterPreference.getInstance(AppMasterApplication.getInstance())
                        .setLastTimeLayout(1);
                QuickGestureManager.getInstance(AppMasterApplication.getInstance()).isShowSysNoReadMessage = false;
            }
            Intent intent;
            intent = new Intent(AppMasterApplication.getInstance(), QuickGesturePopupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("show_orientation", 2);
            AppMasterApplication.getInstance().startActivity(intent);
            if (flag == 1) {
                QuickGestureManager.getInstance(AppMasterApplication.getInstance()).onTuchGestureFlag = 1;
            } else if (flag == 2) {
                QuickGestureManager.getInstance(AppMasterApplication.getInstance()).onTuchGestureFlag = 2;
            }
        }
    }
}
