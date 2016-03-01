
package com.leo.appmaster.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.text.Html;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.ad.ADEngineWrapper;
import com.leo.appmaster.ad.WrappedCampaign;
import com.leo.appmaster.animation.AnimationListenerAdapter;
import com.leo.appmaster.animation.ThreeDimensionalRotationAnimation;
import com.leo.appmaster.applocker.manager.MobvistaEngine;
import com.leo.appmaster.applocker.service.StatusBarEventService;
import com.leo.appmaster.db.PrefTableHelper;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.fragment.BaseFragment;
import com.leo.appmaster.home.DeskProxyActivity;
import com.leo.appmaster.mgr.BatteryManager;
import com.leo.appmaster.privacycontact.CircleImageView;
import com.leo.appmaster.schedule.ScreenRecommentJob;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.AdWrapperLayout;
import com.leo.appmaster.ui.CircleArroundView;
import com.leo.appmaster.ui.ResizableImageView;
import com.leo.appmaster.ui.RippleView;
import com.leo.appmaster.ui.WaveView;
import com.leo.appmaster.utils.AppUtil;
import com.leo.appmaster.utils.DipPixelUtil;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;
import com.leo.appmaster.utils.Utilities;
import com.leo.imageloader.DisplayImageOptions;
import com.leo.imageloader.ImageLoader;
import com.leo.imageloader.core.FailReason;
import com.leo.imageloader.core.ImageLoadingListener;
import com.leo.imageloader.core.ImageScaleType;
import com.leo.tools.animator.Animator;
import com.leo.tools.animator.AnimatorListenerAdapter;
import com.leo.tools.animator.ObjectAnimator;
import com.mobvista.sdk.m.core.entity.Campaign;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class BatteryViewFragment extends BaseFragment implements View.OnTouchListener, BatteryTestViewLayout.ScrollBottomListener, View.OnClickListener {

    private static final String TAG = "BatteryViewFragment";
    private final int ANIMATION_TIME = 300;
    private final int MOVE_UP = 1;
    private final int MOVE_DOWN = 2;
    private final int LOAD_DONE_INIT_PLACE = 6;
    private final int RECOMMAND_TYPE_ONE = 1;
    private final int RECOMMAND_TYPE_TWO = 2;
    private final int RECOMMAND_TYPE_THREE = 3;

    private final int AD_TYPE_MSG = 1;
    private final int SWTIFY_TYPE_MSG = 2;
    private final int EXTRA_TYPE_MSG = 3;

    private static final int AD_LOAD_TIME = 3000;


    public static boolean mShowing = false;
    public static boolean isExpand = false;
    public static boolean mIsExtraLayout = false;

    private View mTimeContent;
    private View mBatteryIcon;
    private View mRemainTimeContent;
    private View mRemainContent;
    private BatteryTestViewLayout mSlideView;

    private TextView mTvLevel;
    private TextView mTvBigTime;
    private TextView mTvSmallLeft;
    private TextView mTvSmallRight;
    private TextView mTvLeftTime;
    //    private TextView mTvTime;
    private SelfScrollView mScrollView;
    private WaveView mBottleWater;
    private TextView mTvHideTime;
    private TextView mTvHideText;
    private View mSettingView;
    private View mArrowMoveContent;
    private ImageView mIvArrowMove;
    private ImageView mIvCancel;
    private View mShowOne;
    private View mShowTwo;
    private View mShowThree;
    private TextView mTvShowOne;
    private TextView mTvShowTwo;
    private TextView mTvShowThree;
    private View mRecommandView;
    private View mRecommandContentView;
    private int mRecommandViewHeight;
    private boolean loadFastThanInit = false;
    private GradientMaskView mMaskView;

    private TextView mPhoneHour;
    private TextView mPhoneHourText;
    private TextView mPhoneMin;
    private TextView mPhoneMinText;
    private TextView mNetHour;
    private TextView mNetHourText;
    private TextView mNetMin;
    private TextView mNetMinText;
    private TextView mPlayHour;
    private TextView mPlayHourText;
    private TextView mPlayMin;
    private TextView mPlayMinText;

    private View backOneView;
    private View backTwoView;
    private View backThreeView;

    private TextView bottomOneText;
    private TextView bottomTwoText;
    private TextView bottomThreeText;


    private View mRecommandNumOne;
    private View mRecommandNumTwo;
    private View mRecommandNumThree;
    private View mRecommandNumFour;

    private CircleImageView mIvShowOne;
    private CircleImageView mIvShowTwo;
    private CircleImageView mIvShowThree;
    private CircleImageView mIvShowFour;

    private TextView mRecommandTvOne;
    private TextView mRecommandTvTwo;
    private TextView mRecommandTvThree;
    private TextView mRecommandTvFour;

    private long mInitTime;

    private int mCurrentClickType = -1;

    private BatteryManager.BatteryState newState;
    private String mChangeType = BatteryManager.SHOW_TYPE_IN;
    private int mRemainTime;
    private int[] mRemainTimeArr;
    private View mBossView;
    private boolean isSetInitPlace = false;

    /*  */
    private View mAdView = null;
    private AdWrapperLayout mAdWrapper = null;
    private Runnable mClickRunnable = null;

    /* 用于更新时间 */
    private Timer mUpdateTimer;
    private UpdateTimeTask mUpdateTask;

    /**
     * 第一个推广位
     */
    private TextView mSwiftyTitle;
    private ImageView mSwiftyImg;
    private TextView mSwiftyContent;
    private RippleView mSwiftyBtnLt;
    private RelativeLayout mSwiftyLayout;

    /**
     * 预留推广位
     */
    private TextView mExtraTitle;
    private ImageView mExtraImg;
    private TextView mExtraContent;
    private RelativeLayout mExtraLayout;

    private ImageLoader mImageLoader;

    private boolean mShowBoost = false;
    private int mAdSource = ADEngineWrapper.SOURCE_MOB; // 默认值

    private List<PackageInfo> mPackages;

    public static String[] days = AppMasterApplication.getInstance().getResources()
            .getStringArray(R.array.days_of_week);

    //开始首页动画
    private android.os.Handler mHandler = new android.os.Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MOVE_UP:
                    if (mBossView.getVisibility() == View.VISIBLE && mArrowMoveContent.getVisibility() == View.VISIBLE) {
                        SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "batterypage", "screen_up");
                        mIvArrowMove.setBackgroundResource(R.drawable.bay_arrow_down);
                        mShowing = true;
                        showMoveUp();
                        mRemainTimeContent.setVisibility(View.INVISIBLE);
                        mRemainContent.setVisibility(View.INVISIBLE);
                    }
                    break;
                case MOVE_DOWN:
                    SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "batterypage", "screen_down");
                    mIvArrowMove.setBackgroundResource(R.drawable.bay_arrow_up);
                    mShowing = true;
                    showMoveDown();
                    mRemainTimeContent.setVisibility(View.VISIBLE);
                    mRemainContent.setVisibility(View.VISIBLE);
                    break;
                case LOAD_DONE_INIT_PLACE:
                    int type = (Integer) msg.obj;

                    //是否满足忽略按钮
                    long lastIgnore = PreferenceTable.getInstance().getLong(Constants.AD_CLICK_IGNORE, 0);
                    long now = System.currentTimeMillis();
                    long internal = PrefTableHelper.getIgnoreTs() * 60 * 60 * 60;//hours change to mmin
                    LeoLog.d("locationP", "now - lastIgnore : " + (now - lastIgnore) + " . internal : " + internal);
                    if (now - lastIgnore > internal) {
                        reLocateMoveContent(type);
                    }

                    break;
            }
        }
    };

    private int mMoveDisdance;

    private void reLocateMoveContent(int type) {
        LeoLog.d("locationP", "slideview Y : " + mSlideView.getY());
        final int contentHeight = mBossView.getHeight();
        final int arrowHeight = mArrowMoveContent.getHeight();


        mMoveDisdance = contentHeight * 9 / 16;
        LeoLog.d("locationP", "mBossView.getHeight() : " + mBossView.getHeight());

        if (type == AD_TYPE_MSG) {
            if (mAdWrapper != null) {
                mMoveDisdance = contentHeight - mAdWrapper.getHeight() - arrowHeight;
                LeoLog.d("locationP", "mAdWrapper.getHeight() : " + mAdWrapper.getHeight());
                setYplace(contentHeight);
            }
        } else if (type == SWTIFY_TYPE_MSG) {
            if (mSwiftyView != null) {
                mMoveDisdance = contentHeight - mSwiftyView.getHeight() - arrowHeight;
                LeoLog.d("locationP", "mSwiftyView.getHeight() : " + mSwiftyView.getHeight());
                setYplace(contentHeight);
            }
        } else if (type == EXTRA_TYPE_MSG) {
            if (mExtraView != null && mExtraView.getHeight() > 0) {
                mMoveDisdance = contentHeight - mExtraView.getHeight() - arrowHeight;
                LeoLog.d("locationP", "mExtraView.getHeight() : " + mExtraView.getHeight());
                setYplace(contentHeight);
            }
        }
    }

    private void setYplace(int contentHeight) {
        int biggestDistance = mBossView.getHeight() / 3;
        if (mMoveDisdance < biggestDistance) {
            mMoveDisdance = mBossView.getHeight() * 4 / 9;
        }

        if (!isSetInitPlace) {
            LeoLog.d("locationP", "mMoveDisdance : " + mMoveDisdance);
//            mSlideView.setY(mMoveDisdance);

            ObjectAnimator animMoveY = ObjectAnimator.ofFloat(mSlideView,
                    "y", contentHeight, mSlideView.getTop() + mMoveDisdance);
            animMoveY.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    boolean isExpandContentShow = mRecommandView.getVisibility() == 0;
                    if (isExpandContentShow || loadFastThanInit) {
                        mCurrentClickType = 0;
                        shrinkRecommandContent();
                    }
                    mBossView.setVisibility(View.VISIBLE);
                }

            });
            animMoveY.setDuration(600);
            animMoveY.start();
            isSetInitPlace = true;
            mArrowMoveContent.setVisibility(View.INVISIBLE);
            mMaskView.hideMask();
        } else {
            mArrowMoveContent.setVisibility(View.VISIBLE);
            mMaskView.showMask();
        }
    }


    @Override
    protected int layoutResourceId() {
        return R.layout.activity_battery_view_newter;
    }

    @Override
    protected void onInitUI() {
        LeoLog.d(TAG, "INIT UI");
        mImageLoader = ImageLoader.getInstance();

        mRemainTimeContent = findViewById(R.id.remain_time);
        mRemainContent = findViewById(R.id.use_time_content);

        mTvBigTime = (TextView) findViewById(R.id.time_big);
        mTvSmallLeft = (TextView) findViewById(R.id.time_small);
        mTvSmallRight = (TextView) findViewById(R.id.time_small_right);

        mTvLevel = (TextView) findViewById(R.id.battery_num);
        mTvLeftTime = (TextView) findViewById(R.id.left_time);

        mBossView = findViewById(R.id.move_boss);
        mBossView.setOnTouchListener(this);

        mSlideView = (BatteryTestViewLayout) findViewById(R.id.slide_content);

        mScrollView = (SelfScrollView) findViewById(R.id.slide_content_sv);
        mScrollView.setParent(mSlideView);
        mSlideView.setScrollBottomListener(this);

        mBottleWater = (WaveView) findViewById(R.id.bottle_water);
//        mBottleWater.setPostInvalidateDelayMs(40);
        mBottleWater.setWaveColor(0xff0ad931);
        mBottleWater.setWave2Color(0xff0ab522);
        mBottleWater.setFactorA(DipPixelUtil.dip2px(mActivity, 3));

        mTvHideTime = (TextView) findViewById(R.id.hide_tv_one);
        mTvHideText = (TextView) findViewById(R.id.hide_tv_two);

        mSettingView = findViewById(R.id.ct_option_2_rl);
        mSettingView.setOnClickListener(this);
        mArrowMoveContent = findViewById(R.id.move_arrow);
        mArrowMoveContent.setOnTouchListener(this);
        mIvArrowMove = (ImageView) findViewById(R.id.iv_move_arrow);

        mIvCancel = (ImageView) findViewById(R.id.iv_cancle);
        mIvCancel.setOnClickListener(this);

        mRecommandNumOne = findViewById(R.id.show_one);
        mRecommandNumTwo = findViewById(R.id.show_two);
        mRecommandNumThree = findViewById(R.id.show_three);
        mRecommandNumFour = findViewById(R.id.show_four);

        mIvShowOne = (CircleImageView) findViewById(R.id.iv_show_one);
        mIvShowTwo = (CircleImageView) findViewById(R.id.iv_show_two);
        mIvShowThree = (CircleImageView) findViewById(R.id.iv_show_three);
        mIvShowFour = (CircleImageView) findViewById(R.id.iv_show_four);

        mRecommandTvOne = (TextView) findViewById(R.id.tv_show_one);
        mRecommandTvTwo = (TextView) findViewById(R.id.tv_show_two);
        mRecommandTvThree = (TextView) findViewById(R.id.tv_show_three);
        mRecommandTvFour = (TextView) findViewById(R.id.tv_show_four);

        mPhoneHour = (TextView) findViewById(R.id.tv_one_time_one);
        mPhoneHourText = (TextView) findViewById(R.id.tv_one_time_two);
        mPhoneMin = (TextView) findViewById(R.id.tv_one_time_three);
        mPhoneMinText = (TextView) findViewById(R.id.tv_one_time_four);

        mNetHour = (TextView) findViewById(R.id.tv_two_time_one);
        mNetHourText = (TextView) findViewById(R.id.tv_two_time_two);
        mNetMin = (TextView) findViewById(R.id.tv_two_time_three);
        mNetMinText = (TextView) findViewById(R.id.tv_two_time_four);

        mPlayHour = (TextView) findViewById(R.id.tv_three_time_one);
        mPlayHourText = (TextView) findViewById(R.id.tv_three_time_two);
        mPlayMin = (TextView) findViewById(R.id.tv_three_time_three);
        mPlayMinText = (TextView) findViewById(R.id.tv_three_time_four);

        backOneView = findViewById(R.id.tv_remain_one_fill);
        backTwoView = findViewById(R.id.tv_remain_two_fill);
        backThreeView = findViewById(R.id.tv_remain_three_fill);
        bottomOneText = (TextView) findViewById(R.id.tv_remain_one);
        bottomTwoText = (TextView) findViewById(R.id.tv_remain_two);
        bottomThreeText = (TextView) findViewById(R.id.tv_remain_three);

        mPackages = mActivity.getPackageManager().getInstalledPackages(0);

        mShowOne = findViewById(R.id.remain_one);
        mShowOne.setOnClickListener(this);
        mShowTwo = findViewById(R.id.remain_two);
        mShowTwo.setOnClickListener(this);
        mShowThree = findViewById(R.id.remain_three);
        mShowThree.setOnClickListener(this);

        mMaskView = (GradientMaskView) findViewById(R.id.mask_view);

        mInitTime = System.currentTimeMillis();

        mRecommandView = findViewById(R.id.three_show_content);
        mRecommandContentView = findViewById(R.id.show_small_content);
        mRecommandView.post(new Runnable() {
            @Override
            public void run() {
                mRecommandViewHeight = mRecommandView.getHeight();
                loadFastThanInit = true;
                expandRecommandContent(RECOMMAND_TYPE_TWO);
            }
        });

        if (newState != null) {
            process(mChangeType, newState, mRemainTime, mRemainTimeArr);
        }

        if (mRootView != null) {
            try {
                loadAd();
            } catch (Exception e) {
                LeoLog.e(TAG, "[loadAd Data]Catch exception happen inside Mobvista: ");
                if (e != null) {
                    LeoLog.e(TAG, e.getLocalizedMessage());
                }
            }
        }

        try {
            if (mRootView != null) {
                initSwiftyLayout(mRootView);
                initExtraLayout(mRootView);
                if (mShowBoost) {
                    initBoostLayout();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mActivity.registerReceiver(mPresentReceiver, intentFilter);
    }

    private void initBoostLayout() {
        ViewStub viewStub = (ViewStub) findViewById(R.id.boost_stub);
        final View boostView = viewStub.inflate();
        final ImageView ivShield = (ImageView) boostView.findViewById(R.id.iv_shield);
        final CircleArroundView cavCircle = (CircleArroundView) boostView.findViewById(R.id.cav_batterymain);

        //TODO 这里故意延迟1秒去开始动画，避免X Y为0,实际功能中不会一加载就进行 不会出现X Y为0的情况
        //TODO 在使用真实动画开启时机后，请删除延迟开始动画的代码

        ThreadManager.getUiThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final float centerX = ivShield.getWidth() / 2.0f;
                final float centerY = ivShield.getHeight() / 2.0f;
                Toast.makeText(mActivity, centerX + " : x   y : " + centerY, Toast.LENGTH_LONG).show();
                if (centerX != 0f && centerY != 0f) {
                    final ThreeDimensionalRotationAnimation rotation = new ThreeDimensionalRotationAnimation(-90, 0,
                            centerX, centerY, 0.0f, true);
                    rotation.setDuration(680);
                    rotation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            cavCircle.startAnim(0f, -360f, 680l, new CircleArroundView.OnArroundFinishListener() {
                                @Override
                                public void onArroundFinish() {
                                    ivShield.setVisibility(View.VISIBLE);
                                }
                            });
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            ivShield.setVisibility(View.VISIBLE);
                        }
                    });
                    rotation.setFillAfter(false);
                    ivShield.startAnimation(rotation);
                }
            }
        }, 1000);


//        boostView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                final float centerX = ivShield.getWidth() / 2.0f;
//                final float centerY = ivShield.getHeight() / 2.0f;
//                Toast.makeText(mActivity, centerX + " : x   y : " + centerY, Toast.LENGTH_LONG).show();
//
//                if (centerX != 0f && centerY != 0f) {
//                    final ThreeDimensionalRotationAnimation rotation = new ThreeDimensionalRotationAnimation(-90, 0,
//                            centerX, centerY, 0.0f, true);
//                    rotation.setDuration(680);
//                    rotation.setAnimationListener(new Animation.AnimationListener() {
//                        @Override
//                        public void onAnimationStart(Animation animation) {
//                            cavCircle.startAnim(0f, -360f, 680l, new CircleArroundView.OnArroundFinishListener() {
//                                @Override
//                                public void onArroundFinish() {
//                                    ivShield.setVisibility(View.VISIBLE);
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animation animation) {
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animation animation) {
//                            ivShield.setVisibility(View.VISIBLE);
//                        }
//                    });
//                    rotation.setFillAfter(false);
//                    ivShield.startAnimation(rotation);
//                }
//
//                boostView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//            }
//        });


        mRemainContent.setVisibility(View.GONE);
        mBossView.setVisibility(View.GONE);
    }

    private void expandRecommandContent(final int recommandTypeThree) {
        turnDark(recommandTypeThree);
        mRecommandView.clearAnimation();
        mRecommandContentView.setVisibility(View.INVISIBLE);
        Animation expand = AnimationUtils.
                loadAnimation(mActivity, R.anim.file_floder_expand_anim);
        expand.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mRecommandView.setVisibility(View.VISIBLE);
                boolean isSlideContentShow = mBossView.getVisibility() == 0;
                if (isSlideContentShow) {
                    mSlideView.setVisibility(View.INVISIBLE);
                }
                fillShowContentData(recommandTypeThree);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mRecommandContentView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mRecommandView.setAnimation(expand);
    }

    private void fillShowContentData(int recommandTypeThree) {
        if (recommandTypeThree == RECOMMAND_TYPE_ONE) {
            List<BatteryAppItem> phoneList = ScreenRecommentJob.getBatteryCallList();
            LeoLog.d("testGetList", "phoneList size is : " + phoneList.size());

            //fill the local ,  size of : 3
            mIvShowOne.setImageDrawable(getResources().getDrawable(R.drawable.icon_time_contacts));
            mRecommandTvOne.setText(getString(R.string.battery_protect_show_num_contact));
            mRecommandNumOne.setVisibility(View.VISIBLE);
            mRecommandNumOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(mActivity, "contact", Toast.LENGTH_SHORT).show();
                }
            });

            mIvShowTwo.setImageDrawable(getResources().getDrawable(R.drawable.icon_time_phone));
            mRecommandTvTwo.setText(getString(R.string.battery_protect_show_num_call));
            mRecommandNumTwo.setVisibility(View.VISIBLE);
            mRecommandNumTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(mActivity, "call", Toast.LENGTH_SHORT).show();
                }
            });

            mIvShowThree.setImageDrawable(getResources().getDrawable(R.drawable.icon_time_message));
            mRecommandTvThree.setText(getString(R.string.battery_protect_show_num_msm));
            mRecommandNumThree.setVisibility(View.VISIBLE);
            mRecommandNumThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            String name = null;
            Drawable map = null;
            BatteryAppItem info = phoneList.get(0);
            for (int i = 0; i < mPackages.size(); i++) {
                PackageInfo packageInfo = mPackages.get(i);
                if (packageInfo.applicationInfo.packageName.equals(info.pkg)) {
                    name = packageInfo.applicationInfo.loadLabel(mActivity.getPackageManager()).toString();
                    map = packageInfo.applicationInfo.loadIcon(mActivity.getPackageManager());
                    break;
                }
            }
            if (name != null && map != null) {
                mRecommandNumFour.setVisibility(View.VISIBLE);
                mIvShowFour.setImageDrawable(map);
                mRecommandTvFour.setText(name);
                mRecommandNumFour.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
            } else {
                mRecommandNumFour.setVisibility(View.GONE);
            }


        } else if (recommandTypeThree == RECOMMAND_TYPE_TWO) {
            List<BatteryAppItem> netList = ScreenRecommentJob.getBatteryNetList();
            LeoLog.d("testGetList", "netList size is : " + netList.size());

            //fill the local ,  size of : 1
            mIvShowOne.setImageDrawable(getResources().getDrawable(R.drawable.icon_time_browser));
            mRecommandTvOne.setText(getString(R.string.battery_protect_show_num_browser));
            mRecommandNumOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(mActivity, "browser", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            List<BatteryAppItem> playList = ScreenRecommentJob.getBatteryVideoList();
            LeoLog.d("testGetList", "playList size is : " + playList.size());
        }
    }

    private void shrinkRecommandContent() {
        turnLight();
        mRecommandView.clearAnimation();
        mRecommandContentView.setVisibility(View.INVISIBLE);
        Animation shrink = AnimationUtils.
                loadAnimation(mActivity, R.anim.file_floder_shrink_anim);

        shrink.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mRecommandView.setVisibility(View.INVISIBLE);

                boolean isSlideContentShow = mBossView.getVisibility() == 0;
                if (isSlideContentShow) {
                    mSlideView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mRecommandView.setAnimation(shrink);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releaseAd();
        mActivity.unregisterReceiver(mPresentReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        scheduleUpdateTimer();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mUpdateTimer = null;
        }
        if (mUpdateTask != null) {
            mUpdateTask.cancel();
            mUpdateTask = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isExpand = false;
        mShowing = false;
        mIsExtraLayout = false;
    }

    static class UpdateTimeTask extends TimerTask {
        WeakReference<BatteryViewFragment> mFragmentRef;

        public UpdateTimeTask(BatteryViewFragment fragment) {
            mFragmentRef = new WeakReference<BatteryViewFragment>(fragment);
        }

        @Override
        public void run() {
            final BatteryViewFragment fragment = mFragmentRef.get();
            if (fragment != null) {
                ThreadManager.executeOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fragment.updateTime();
                    }
                });
            }
        }
    }

    private void scheduleUpdateTimer() {
        if (mUpdateTimer == null) {
            mUpdateTimer = new Timer();
        }
        if (mUpdateTask == null) {
            mUpdateTask = new UpdateTimeTask(this);
        }
        mUpdateTimer.schedule(mUpdateTask, 0, 10 * 1000);
    }

    public void initCreate(String type, BatteryManager.BatteryState state, int remainTime, int[] remainTimeArr) {
        mChangeType = type;
        newState = state;
        mRemainTime = remainTime;
        mRemainTimeArr = remainTimeArr;
        LeoLog.d("testBatteryEvent", "initCreate : " + mRemainTimeArr[0] + " ; " + mRemainTimeArr[1] + " ; " + mRemainTimeArr[2]);
    }

    public void process(String type, BatteryManager.BatteryState state, int remainTime, int[] remainTimeArr) {
        mChangeType = type;
        newState = state;
        mRemainTime = remainTime;
        mRemainTimeArr = remainTimeArr;
        LeoLog.d("testBatteryEvent", "process : " + mRemainTimeArr[0] + " ; " + mRemainTimeArr[1] + " ; " + mRemainTimeArr[2]);
        notifyUI(mChangeType);
    }

    private BroadcastReceiver mPresentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LeoLog.d("stone_test_browser", "action=" + action);
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (mClickRunnable != null) {
                    mActivity.finish();
                }
            } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
                if (mClickRunnable != null) {
                    mClickRunnable.run();
                    mClickRunnable = null;
                }
            }
        }
    };

    public void notifyUI(String type) {

        setBatteryPercent();
        setBottleWater();
        setTime(mRemainTime, isExpand);
        setThreeRemainTime(mRemainTimeArr);

        if (BatteryManager.SHOW_TYPE_OUT.equals(type)) {
            if (!isExpand) {
//                mSlideView.setScrollable(true);

//                expandContent(true);
                boolean isShowContentShow = mRecommandView.getVisibility() == 0;
                if (!isShowContentShow) {
                    expandRecommandContent(RECOMMAND_TYPE_TWO);
                    mCurrentClickType = RECOMMAND_TYPE_TWO;
                }
//                showRecommandContent(RECOMMAND_TYPE_TWO);
            } else {
                expandContent(false);
                boolean isShowContentShow = mRecommandView.getVisibility() == 0;
                if (!isShowContentShow) {
                    expandRecommandContent(RECOMMAND_TYPE_TWO);
                    mCurrentClickType = RECOMMAND_TYPE_TWO;
                }
            }
        }

        if (newState != null && mIvArrowMove != null && mBottleWater != null) {
            if (newState.plugged == 0) {
//                mIvArrowMove.setVisibility(View.INVISIBLE);
                mBottleWater.setIsNeedWave(false);
            } else {
                mBottleWater.setIsNeedWave(true);
                if (isExpand) {
                    mIvArrowMove.setVisibility(View.VISIBLE);
                    mIvArrowMove.setBackgroundResource(R.drawable.bay_arrow_down);
                } else {
                    mIvArrowMove.setVisibility(View.VISIBLE);
                    mIvArrowMove.setBackgroundResource(R.drawable.bay_arrow_up);
                }
            }
        }

    }

    private void setThreeRemainTime(int[] timeArr) {
        int phoneTime = timeArr[0];
        int netTime = timeArr[1];
        int playTime = timeArr[2];

        List<String> phoneTimeArr = getTimes(phoneTime);
        String phoneHourStr = phoneTimeArr.get(0);
        String phoneMinStr = phoneTimeArr.get(1);
        if (phoneHourStr.equals("0")) {
            mPhoneHour.setVisibility(View.GONE);
            mPhoneHourText.setVisibility(View.GONE);
            mPhoneMin.setVisibility(View.VISIBLE);
            mPhoneMinText.setVisibility(View.VISIBLE);

            mPhoneMin.setText(phoneMinStr);
        } else {
            if (phoneMinStr.equals("0")) {
                mPhoneHour.setVisibility(View.VISIBLE);
                mPhoneHourText.setVisibility(View.VISIBLE);
                mPhoneMin.setVisibility(View.GONE);
                mPhoneMinText.setVisibility(View.GONE);

                mPhoneHour.setText(phoneHourStr);
            } else {
                mPhoneHour.setVisibility(View.VISIBLE);
                mPhoneHourText.setVisibility(View.VISIBLE);
                mPhoneMin.setVisibility(View.VISIBLE);
                mPhoneMinText.setVisibility(View.VISIBLE);

                mPhoneHour.setText(phoneHourStr);
                mPhoneMin.setText(phoneMinStr);
            }
        }

        List<String> netTimeArr = getTimes(netTime);
        String netHourStr = netTimeArr.get(0);
        String netMinStr = netTimeArr.get(1);
        if (netHourStr.equals("0")) {
            mNetHour.setVisibility(View.GONE);
            mNetHourText.setVisibility(View.GONE);
            mNetMin.setVisibility(View.VISIBLE);
            mNetMinText.setVisibility(View.VISIBLE);
            mNetMin.setText(netMinStr);
        } else {
            if (netMinStr.equals("0")) {
                mNetHour.setVisibility(View.VISIBLE);
                mNetHourText.setVisibility(View.VISIBLE);
                mNetMin.setVisibility(View.GONE);
                mNetMinText.setVisibility(View.GONE);
                mNetHour.setText(netHourStr);
            } else {
                mNetHour.setVisibility(View.VISIBLE);
                mNetHourText.setVisibility(View.VISIBLE);
                mNetMin.setVisibility(View.VISIBLE);
                mNetMinText.setVisibility(View.VISIBLE);
                mNetHour.setText(netHourStr);
                mNetMin.setText(netMinStr);
            }
        }

        List<String> playTimeArr = getTimes(playTime);
        String playHourStr = playTimeArr.get(0);
        String playMinStr = playTimeArr.get(1);
        if (playHourStr.equals("0")) {
            mPlayHour.setVisibility(View.GONE);
            mPlayHourText.setVisibility(View.GONE);
            mPlayMin.setVisibility(View.VISIBLE);
            mPlayMinText.setVisibility(View.VISIBLE);
            mPlayMin.setText(playMinStr);
        } else {
            if (playMinStr.equals("0")) {
                mPlayHour.setVisibility(View.VISIBLE);
                mPlayHourText.setVisibility(View.VISIBLE);
                mPlayMin.setVisibility(View.GONE);
                mPlayMinText.setVisibility(View.GONE);
                mPlayHour.setText(playHourStr);
            } else {
                mPlayHour.setVisibility(View.VISIBLE);
                mPlayHourText.setVisibility(View.VISIBLE);
                mPlayMin.setVisibility(View.VISIBLE);
                mPlayMinText.setVisibility(View.VISIBLE);
                mPlayHour.setText(playHourStr);
                mPlayMin.setText(playMinStr);
            }
        }
    }

    private void updateTime() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int day_of_week = c.get(Calendar.DAY_OF_WEEK);
        LeoLog.d(TAG, year + ":" + month + ":" + day + ":" + hour + ":" + minute + ":" + day_of_week);

        mTvBigTime.setText(hour + ":" + String.format(Locale.ENGLISH, "%02d", minute));
        mTvSmallLeft.setText((month + 1) + "/" + day);

        // 资源应该从周日 - 周六 这样的顺序
        if (day_of_week >= 2 && day_of_week - 2 < days.length) {
            mTvSmallRight.setText(days[day_of_week - 2]);
        } else {
            mTvSmallRight.setText(days[6]);
        }

        LeoLog.d(TAG, "updateTime@" + hour + ":" + minute);
    }

    private void setBottleWater() {
        if (newState != null && mBottleWater != null) {
            int level = newState.level;
            mBottleWater.setPercent(level);
        }
    }


    private void setBatteryPercent() {
        if (mTvLevel != null && newState != null) {
            mTvLevel.setText(newState.level + "%");

            if (newState.level < 70) {
                mBottleWater.setPostInvalidateDelayMs(50);
            } else if (newState.level < 85) {
                mBottleWater.setPostInvalidateDelayMs(70);
            } else {
                mBottleWater.setPostInvalidateDelayMs(80);
            }
        }
    }

    private List<String> getTimes(int second) {
        List<String> strings = new ArrayList<String>();
        int h = 0;
        int d = 0;
        int s = 0;
        int temp = second % 3600;
        if (second > 3600) {
            h = second / 3600;
            if (temp != 0) {
                if (temp > 60) {
                    d = temp / 60;
                    if (temp % 60 != 0) {
                        s = temp % 60;
                    }
                } else {
                    s = temp;
                }
            }
        } else {
            d = second / 60;
            if (second % 60 != 0) {
                s = second % 60;
            }
        }

        String hString, dString;
        hString = String.valueOf(h);
        dString = String.valueOf(d);

        strings.add(hString);
        strings.add(dString);
        return strings;
    }

    public void setTime(int second, boolean isExpandContent) {
        if (mActivity == null) {
            return;
        }

        List<String> timeStr = getTimes(second);
        String hString, dString;
        hString = timeStr.get(0);
        dString = timeStr.get(1);
        LeoLog.d("testBatteryView", "hString : " + hString + "dString : " + dString);
        boolean isCharing = newState.plugged != 0 ? true : false;

        if (newState.level >= 100) {
            String text = mActivity.getString(R.string.screen_protect_charing_text_four);
            mTvHideText.setText(text);
            mTvHideTime.setVisibility(View.GONE);
        } else {
            if (isCharing) {
                if (hString.equals("0")) {
                    if (!dString.equals("0")) {
                        String text = mActivity.getString(R.string.screen_protect_time_right_two, dString);
                        String text2 = mActivity.getString(R.string.screen_protect_time);
                        mTvHideText.setVisibility(View.VISIBLE);
                        mTvHideTime.setVisibility(View.VISIBLE);
                        mTvHideText.setText(text2);
                        mTvHideTime.setText(Html.fromHtml(text));
                    } else {
                        String text = mActivity.getString(R.string.screen_protect_charing_text_two);
                        mTvHideText.setText(text);
                        mTvHideTime.setVisibility(View.GONE);
                    }
                } else {
                    String text = mActivity.getString(R.string.screen_protect_time_right, hString, dString);
                    String text2 = mActivity.getString(R.string.screen_protect_time);
                    mTvHideText.setVisibility(View.VISIBLE);
                    mTvHideTime.setVisibility(View.VISIBLE);
                    mTvHideText.setText(text2);
                    mTvHideTime.setText(Html.fromHtml(text));
                }
            } else {
                String text2 = mActivity.getString(R.string.screen_protect_charing_text_one);
                mTvHideText.setText(text2);
                mTvHideTime.setVisibility(View.GONE);
            }

        }


    }

    private int staryY;
    private boolean isMissionDone = false;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (view == mArrowMoveContent) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    staryY = (int) event.getRawY();

                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!isMissionDone) {
                        int newY = (int) event.getRawY();
                        int moveY = newY - staryY;

                        if (isExpand) {
                            if (!mShowing && moveY > 0) {
                                isMissionDone = true;
                                expandContent(false);
                            }
                        } else {
                            if (!mShowing && moveY < 0) {
                                isMissionDone = true;
                                expandContent(true);
                            }
                        }

                    }

                    break;
                case MotionEvent.ACTION_UP:
                    isMissionDone = false;
                    int upY = (int) event.getRawY();
                    if (staryY == upY) {
                        if (isExpand) {
                            if (!mShowing) {
                                expandContent(false);
                            }
                        } else {
                            if (!mShowing) {
                                expandContent(true);
                            }
                        }
                    }
                    break;
            }
        } else if (view == mBossView) {
            return false;   //TODO 取消遮盖popupwindow的点击，如果后续有影响，请取消
        }
        return true;
    }

    private void showMoveUp() {

//        ObjectAnimator animMoveY = ObjectAnimator.ofFloat(mSlideView,
//                "y", mSlideView.getTop() + mBossView.getHeight() * 9 / 16, mSlideView.getTop());
        ObjectAnimator animMoveY = ObjectAnimator.ofFloat(mSlideView,
                "y", mSlideView.getTop() + mMoveDisdance, mSlideView.getTop());
        animMoveY.setDuration(ANIMATION_TIME);
        animMoveY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isExpand = true;
                mShowing = false;
                mScrollView.setScrollY(0);
//                mSlideView.setScrollable(false);
            }
        });
        animMoveY.start();
    }

    private void showMoveDown() {
//        ObjectAnimator animMoveY = ObjectAnimator.ofFloat(mSlideView,
//                "y", mSlideView.getTop(), mSlideView.getTop() + mBossView.getHeight() * 9 / 16);
        ObjectAnimator animMoveY = ObjectAnimator.ofFloat(mSlideView,
                "y", mSlideView.getTop(), mSlideView.getTop() + mMoveDisdance);
        animMoveY.setDuration(ANIMATION_TIME);
        animMoveY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isExpand = false;
                mShowing = false;
                mScrollView.setScrollY(0);
//                mSlideView.setScrollable(false);
            }
        });
        animMoveY.start();
    }

    private void expandContent(boolean expand) {
        if (expand) {
            mHandler.sendEmptyMessage(MOVE_UP);
        } else {
            mHandler.sendEmptyMessage(MOVE_DOWN);
        }

    }

    @Override
    public void scrollBottom() {
        expandContent(false);
    }

    @Override
    public void scrollTop() {
        expandContent(true);
    }

    @Override
    public void onClick(View view) {
        if (mActivity == null) {
            return;
        }
        switch (view.getId()) {
            case R.id.ct_option_2_rl:
                mLockManager.filterPackage(mActivity.getPackageName(), 5000);
                mBatteryManager.markSettingClick();

                Intent dlIntent = new Intent(mActivity, BatterySettingActivity.class);
                dlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                dlIntent.putExtra(Constants.BATTERY_FROM, Constants.FROM_BATTERY_PROTECT);
                dlIntent.putExtra(BatteryManager.REMAIN_TIME, mRemainTime);
                dlIntent.putExtra(BatteryManager.ARR_REMAIN_TIME, mRemainTimeArr);
                Bundle bundle = new Bundle();
                bundle.putSerializable(BatteryManager.SEND_BUNDLE, newState);
                dlIntent.putExtras(bundle);
                mActivity.startActivity(dlIntent);
                SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "batterypage", "screen_setting");
                mActivity.finish();
                break;
            case R.id.ad_wrapper:
                mClickRunnable = new Runnable() {
                    @Override
                    public void run() {
                        LeoLog.d("stone_test_browser", "fire touch event!");
                        MotionEvent eventDown = MotionEvent.obtain(SystemClock.elapsedRealtime(),
                                SystemClock.elapsedRealtime(), MotionEvent.ACTION_DOWN,
                                10.0f, 10.0f, 0);
                        mAdView.dispatchTouchEvent(eventDown);
                        MotionEvent eventUp = MotionEvent.obtain(SystemClock.elapsedRealtime(),
                                SystemClock.elapsedRealtime(), MotionEvent.ACTION_UP,
                                10.0f, 10.0f, 0);
                        mAdView.dispatchTouchEvent(eventUp);
                    }
                };
                if (mAdWrapper != null) {
                    mAdWrapper.setNeedIntercept(false);
                }
                handleRunnable();
                break;
            case R.id.parent_layout:
                if (view == mSwiftyLayout) {
                    mClickRunnable = new Runnable() {
                        @Override
                        public void run() {
                            SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "batterypage", "screen_promote1");
                            PreferenceTable preferenceTable = PreferenceTable.getInstance();
                            Utilities.selectType(preferenceTable, PrefConst.KEY_CHARGE_SWIFTY_TYPE,
                                    PrefConst.KEY_CHARGE_SWIFTY_GP_URL, PrefConst.KEY_CHARGE_SWIFTY_URL,
                                    "", mActivity);
                        }
                    };
                    handleRunnable();
                } else if (view == mExtraLayout) {
                    mClickRunnable = new Runnable() {
                        @Override
                        public void run() {
                            SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "batterypage", "screen_promote2");
                            PreferenceTable preferenceTable = PreferenceTable.getInstance();
                            Utilities.selectType(preferenceTable, PrefConst.KEY_CHARGE_EXTRA_TYPE,
                                    PrefConst.KEY_CHARGE_EXTRA_GP_URL, PrefConst.KEY_CHARGE_EXTRA_URL,
                                    "", mActivity);
                        }
                    };
                    handleRunnable();
                }
                break;
            case R.id.iv_cancle:
                BatteryShowViewActivity activity = (BatteryShowViewActivity) mActivity;
                activity.onFinishActivity();
                break;
            case R.id.remain_one:
                showRecommandContent(RECOMMAND_TYPE_ONE);
                break;
            case R.id.remain_two:
                showRecommandContent(RECOMMAND_TYPE_TWO);
                break;
            case R.id.remain_three:
                showRecommandContent(RECOMMAND_TYPE_THREE);
                break;
        }
    }


    private void turnLight() {
        backOneView.getBackground().setAlpha(255);
        mPhoneHour.setTextColor(getResources().getColor(R.color.white));
        mPhoneHourText.setTextColor(getResources().getColor(R.color.green_back_normal));
        mPhoneMin.setTextColor(getResources().getColor(R.color.white));
        mPhoneMinText.setTextColor(getResources().getColor(R.color.green_back_normal));
        bottomOneText.setTextColor(getResources().getColor(R.color.white));

        backTwoView.getBackground().setAlpha(255);
        mNetHour.setTextColor(getResources().getColor(R.color.white));
        mNetHourText.setTextColor(getResources().getColor(R.color.blue_back_normal));
        mNetMin.setTextColor(getResources().getColor(R.color.white));
        mNetMinText.setTextColor(getResources().getColor(R.color.blue_back_normal));
        bottomTwoText.setTextColor(getResources().getColor(R.color.white));

        backThreeView.getBackground().setAlpha(255);
        mPlayHour.setTextColor(getResources().getColor(R.color.white));
        mPlayHourText.setTextColor(getResources().getColor(R.color.yellow_back_normal));
        mPlayMin.setTextColor(getResources().getColor(R.color.white));
        mPlayMinText.setTextColor(getResources().getColor(R.color.yellow_back_normal));
        bottomThreeText.setTextColor(getResources().getColor(R.color.white));
    }

    private void turnDark(int recommandType) {
        if (recommandType == RECOMMAND_TYPE_ONE) {
            //light
            backOneView.getBackground().setAlpha(255);
            mPhoneHour.setTextColor(getResources().getColor(R.color.white));
            mPhoneHourText.setTextColor(getResources().getColor(R.color.green_back_normal));
            mPhoneMin.setTextColor(getResources().getColor(R.color.white));
            mPhoneMinText.setTextColor(getResources().getColor(R.color.green_back_normal));
            bottomOneText.setTextColor(getResources().getColor(R.color.white));

            //dark
            backTwoView.getBackground().setAlpha(78);
            mNetHour.setTextColor(getResources().getColor(R.color.white_20));
            mNetHourText.setTextColor(getResources().getColor(R.color.white_20));
            mNetMin.setTextColor(getResources().getColor(R.color.white_20));
            mNetMinText.setTextColor(getResources().getColor(R.color.white_20));
            bottomTwoText.setTextColor(getResources().getColor(R.color.white_20));

            //dark
            backThreeView.getBackground().setAlpha(78);
            mPlayHour.setTextColor(getResources().getColor(R.color.white_20));
            mPlayHourText.setTextColor(getResources().getColor(R.color.white_20));
            mPlayMin.setTextColor(getResources().getColor(R.color.white_20));
            mPlayMinText.setTextColor(getResources().getColor(R.color.white_20));
            bottomThreeText.setTextColor(getResources().getColor(R.color.white_20));

        } else if (recommandType == RECOMMAND_TYPE_TWO) {
            //dark
            backOneView.getBackground().setAlpha(78);
            mPhoneHour.setTextColor(getResources().getColor(R.color.white_20));
            mPhoneHourText.setTextColor(getResources().getColor(R.color.white_20));
            mPhoneMin.setTextColor(getResources().getColor(R.color.white_20));
            mPhoneMinText.setTextColor(getResources().getColor(R.color.white_20));
            bottomOneText.setTextColor(getResources().getColor(R.color.white_20));

            //light
            backTwoView.getBackground().setAlpha(255);
            mNetHour.setTextColor(getResources().getColor(R.color.white));
            mNetHourText.setTextColor(getResources().getColor(R.color.blue_back_normal));
            mNetMin.setTextColor(getResources().getColor(R.color.white));
            mNetMinText.setTextColor(getResources().getColor(R.color.blue_back_normal));
            bottomTwoText.setTextColor(getResources().getColor(R.color.white));

            //dark
            backThreeView.getBackground().setAlpha(78);
            mPlayHour.setTextColor(getResources().getColor(R.color.white_20));
            mPlayHourText.setTextColor(getResources().getColor(R.color.white_20));
            mPlayMin.setTextColor(getResources().getColor(R.color.white_20));
            mPlayMinText.setTextColor(getResources().getColor(R.color.white_20));
            bottomThreeText.setTextColor(getResources().getColor(R.color.white_20));
        } else {
            //dark
            backOneView.getBackground().setAlpha(78);
            mPhoneHour.setTextColor(getResources().getColor(R.color.white_20));
            mPhoneHourText.setTextColor(getResources().getColor(R.color.white_20));
            mPhoneMin.setTextColor(getResources().getColor(R.color.white_20));
            mPhoneMinText.setTextColor(getResources().getColor(R.color.white_20));
            bottomOneText.setTextColor(getResources().getColor(R.color.white_20));

            //dark
            backTwoView.getBackground().setAlpha(78);
            mNetHour.setTextColor(getResources().getColor(R.color.white_20));
            mNetHourText.setTextColor(getResources().getColor(R.color.white_20));
            mNetMin.setTextColor(getResources().getColor(R.color.white_20));
            mNetMinText.setTextColor(getResources().getColor(R.color.white_20));
            bottomTwoText.setTextColor(getResources().getColor(R.color.white_20));

            //light
            backThreeView.getBackground().setAlpha(255);
            mPlayHour.setTextColor(getResources().getColor(R.color.white));
            mPlayHourText.setTextColor(getResources().getColor(R.color.yellow_back_normal));
            mPlayMin.setTextColor(getResources().getColor(R.color.white));
            mPlayMinText.setTextColor(getResources().getColor(R.color.yellow_back_normal));
            bottomThreeText.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private void showRecommandContent(int recommandTypeThree) {
        if (mCurrentClickType == recommandTypeThree) {
            mCurrentClickType = 0;
//            turnLight();  // put it insideshrinkRecommandContent
            shrinkRecommandContent();
        } else {
            if (mCurrentClickType == 0) {
                expandRecommandContent(recommandTypeThree);
            } else if (mCurrentClickType == -1) {
                mCurrentClickType = 0;
                shrinkRecommandContent();
                return;
            } else {
                //TODO 展示内容替换
                mCurrentClickType = 0;
                shrinkRecommandContent();
                return;
            }
            mCurrentClickType = recommandTypeThree;
        }
    }


    /* AM-3889: 三星5.x的手机从系统锁之外跳转有问题，需要特殊处理 */
    private boolean samsungLolipopDevice() {
        LeoLog.d(TAG, "MANUFACTURER: " + Build.MANUFACTURER);
        LeoLog.d(TAG, "SDK_INT: " + Build.VERSION.SDK_INT);
        return (Build.MANUFACTURER.equalsIgnoreCase("samsung")
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT < 23 /*Marshmallow*/);
    }

    private void handleRunnable() {
        if (mClickRunnable == null) {
            return;
        }

        if (AppUtil.isScreenLocked(mActivity)) {
            // 默认浏览器是chrome而且系统锁住了，让user_present receiver处理
            if (samsungLolipopDevice()) {
                mLockManager.filterPackage(mActivity.getPackageName(), 2000);
                LeoLog.d(TAG, "Samsung 5.x device, launch specific activity");
                Intent intent = new Intent(mActivity, EmptyForJumpActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mActivity.startActivity(intent);
            } else {
                Intent intent = new Intent(mActivity, DeskProxyActivity.class);
                intent.putExtra(StatusBarEventService.EXTRA_EVENT_TYPE, DeskProxyActivity.IDX_BATTERY_PROTECT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mActivity.startActivity(intent);
            }

        } else {
            // 没有锁或者默认浏览器不是chrome，直接跑
            mClickRunnable.run();
            mClickRunnable = null;
        }
    }

    /* 广告相关 - 开始 */
    private boolean mShouldLoadAd = false;

    private void loadAd() {
        LeoLog.d(TAG, "loadAd called");
        mShouldLoadAd = AppMasterPreference.getInstance(mActivity).getADOnScreenSaver() == 1;
        if (mShouldLoadAd) {
            mAdSource = AppMasterPreference.getInstance(mActivity).getChargingAdConfig();
            ADEngineWrapper.getInstance(mActivity).loadAd(mAdSource, Constants.UNIT_ID_CHARGING, new ADEngineWrapper.WrappedAdListener() {
                @Override
                public void onWrappedAdLoadFinished(int code, WrappedCampaign campaign, String msg) {
                    if (code == MobvistaEngine.ERR_OK) {
                        LeoLog.d(TAG, "Ad data ready");
                        sAdImageListener = new AdPreviewLoaderListener(BatteryViewFragment.this, campaign);
                        ImageLoader.getInstance().loadImage(campaign.getImageUrl(), sAdImageListener);
                    }
                }

                @Override
                public void onWrappedAdClick(WrappedCampaign campaign, String unitID) {
                    SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "ad_cli", "adv_cnts_screen");
                    LeoLog.d(TAG, "Ad clicked");
                }
            });
            /*MobvistaEngine.getInstance(mActivity).loadMobvista(Constants.UNIT_ID_CHARGING,
                    new MobvistaEngine.MobvistaListener() {
                        @Override
                        public void onMobvistaFinished(int code, Campaign campaign, String msg) {
                            if (code == MobvistaEngine.ERR_OK) {
                                LeoLog.d(TAG, "Ad data ready");
                                sAdImageListener = new AdPreviewLoaderListener(BatteryViewFragment.this, campaign);
                                ImageLoader.getInstance().loadImage(campaign.getImageUrl(), sAdImageListener);
                            }
                        }

                        @Override
                        public void onMobvistaClick(Campaign campaign, String unitID) {
                            SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "ad_cli", "adv_cnts_screen");
                            LeoLog.d(TAG, "Ad clicked");
                        }
                    });*/
        }
    }

    private void releaseAd() {
        if (mShouldLoadAd) {
            LeoLog.d(TAG, "release ad");
            ADEngineWrapper.getInstance(mActivity).releaseAd(mAdSource, Constants.UNIT_ID_CHARGING);
            //MobvistaEngine.getInstance(mActivity).release(Constants.UNIT_ID_CHARGING);
        }
    }

    public static class AdPreviewLoaderListener implements ImageLoadingListener {
        WeakReference<BatteryViewFragment> mFragment;
        WrappedCampaign mCampaign;

        public AdPreviewLoaderListener(BatteryViewFragment fragment, final WrappedCampaign campaign) {
            mFragment = new WeakReference<BatteryViewFragment>(fragment);
            mCampaign = campaign;
        }

        @Override
        public void onLoadingStarted(String imageUri, View view) {

        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            LeoLog.e(TAG, "failed to load AD preview!");
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            LeoLog.d(TAG, "Ad preview image ready");
            BatteryViewFragment fragment = mFragment.get();
            if (loadedImage != null && fragment != null) {
                try {
                    LeoLog.d(TAG, "load done: " + imageUri);

                    fragment.initAdLayout(fragment.mRootView, mCampaign, loadedImage);

                } catch (Exception e) {
                    LeoLog.e(TAG, "[Impression]Catch exception happen inside Mobvista: ");
                    if (e != null) {
                        LeoLog.e(TAG, e.getLocalizedMessage());
                    }
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {

        }
    }

    private static AdPreviewLoaderListener sAdImageListener;

    private void initAdLayout(View rootView, WrappedCampaign campaign, Bitmap previewImage) {
        View adView = rootView.findViewById(R.id.ad_content);
        mAdWrapper = (AdWrapperLayout) rootView.findViewById(R.id.ad_wrapper);
        mAdWrapper.setNeedIntercept(true);

        final Button ignoreBtn = (Button) rootView.findViewById(R.id.ignore_button);
        boolean isShowIgnoreBtn = PrefTableHelper.showIgnoreBtn();
//        if (isShowIgnoreBtn) {
        if(true){
            ignoreBtn.setVisibility(View.VISIBLE);
            ignoreBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO 用户点击了ignore右上角的菜单
                    LeoLog.d("stone_test_ignore", "ignore!");
                    showPopUp(ignoreBtn);
                }
            });
        } else {
            ignoreBtn.setVisibility(View.INVISIBLE);
        }

        TextView tvTitle = (TextView) adView.findViewById(R.id.item_title);
        tvTitle.setText(campaign.getAppName());
        TextView tvDesc = (TextView) adView.findViewById(R.id.item_description);
        tvDesc.setText(campaign.getDescription());
        Button btnCTA = (Button) adView.findViewById(R.id.ad_result_cta);
        btnCTA.setText(campaign.getAdCall());
        ResizableImageView preview = (ResizableImageView) adView.findViewById(R.id.item_ad_preview);
        preview.setImageBitmap(previewImage);
        ImageView iconView = (ImageView) adView.findViewById(R.id.ad_icon);
        ImageLoader.getInstance().displayImage(campaign.getIconUrl(), iconView);
        SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "ad_act", "adv_shws_screen");

        View saverAdView = rootView.findViewById(R.id.screen_saver_id);
        saverAdView.setVisibility(View.VISIBLE);

        adView.setVisibility(View.VISIBLE);

        int delayTime;
        if (System.currentTimeMillis() - mInitTime > AD_LOAD_TIME) {
            delayTime = 300;
        } else {
            delayTime = AD_LOAD_TIME;
        }

        LeoLog.d("testDelayTime", "System.currentTimeMillis() - mInitTime : " + (System.currentTimeMillis() - mInitTime));
        LeoLog.d("testDelayTime", "delayTime : " + delayTime);

        mAdWrapper.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message msg = Message.obtain();
                msg.what = LOAD_DONE_INIT_PLACE;
                msg.obj = AD_TYPE_MSG;
                mHandler.sendMessage(msg);
            }
        }, delayTime);

        mAdView = adView;

        // make the count correct
        //MobvistaEngine.getInstance(mActivity).registerView(Constants.UNIT_ID_CHARGING, mAdView);
        ADEngineWrapper.getInstance(mActivity).registerView(mAdSource, mAdView, Constants.UNIT_ID_CHARGING);
        mAdWrapper.setOnClickListener(this);
    }
    /* 广告相关 - 结束 */

    private View mSwiftyView;

    private void initSwiftyLayout(View view) {
        ViewStub viewStub = (ViewStub) view.findViewById(R.id.content_type_1);
        if (viewStub == null) {
            return;
        }

        PreferenceTable preferenceTable = PreferenceTable.getInstance();

        boolean isContentEmpty = TextUtils.isEmpty(
                preferenceTable.getString(PrefConst.KEY_CHARGE_SWIFTY_CONTENT));

        boolean isImgUrlEmpty = TextUtils.isEmpty(
                preferenceTable.getString(PrefConst.KEY_CHARGE_SWIFTY_IMG_URL));

        boolean isTypeEmpty = TextUtils.isEmpty(
                preferenceTable.getString(PrefConst.KEY_CHARGE_SWIFTY_TYPE));

        boolean isGpUrlEmpty = TextUtils.isEmpty(
                preferenceTable.getString(PrefConst.KEY_CHARGE_SWIFTY_GP_URL));

        boolean isBrowserUrlEmpty = TextUtils.isEmpty(
                preferenceTable.getString(PrefConst.KEY_CHARGE_SWIFTY_URL));

        boolean isUrlEmpty = isGpUrlEmpty && isBrowserUrlEmpty; //判断两个地址是否都为空

        if (!isContentEmpty && !isImgUrlEmpty && !isTypeEmpty && !isUrlEmpty) {
//        if (false) {
            mSwiftyView = viewStub.inflate();

            mSwiftyImg = (ImageView) mSwiftyView.findViewById(R.id.card_img);
            mSwiftyContent = (TextView) mSwiftyView.findViewById(R.id.card_content);
            mSwiftyLayout = (RelativeLayout) mSwiftyView.findViewById(R.id.parent_layout);
            mSwiftyLayout.setOnClickListener(this);
            mSwiftyContent.setText(preferenceTable.getString(PrefConst.KEY_CHARGE_SWIFTY_CONTENT));
            String imgUrl = preferenceTable.getString(PrefConst.KEY_CHARGE_SWIFTY_IMG_URL);
            mImageLoader.displayImage(imgUrl, mSwiftyImg, getOptions(R.drawable.online_theme_loading));
            mSwiftyTitle = (TextView) mSwiftyView.findViewById(R.id.card_title);
            boolean isTitleEmpty = TextUtils.isEmpty(
                    preferenceTable.getString(PrefConst.KEY_CHARGE_SWIFTY_TITLE));
            if (!isTitleEmpty) {
                mSwiftyTitle.setText(preferenceTable.getString(
                        PrefConst.KEY_CHARGE_SWIFTY_TITLE));
            }

            int delayTime;
            if (System.currentTimeMillis() - mInitTime > AD_LOAD_TIME) {
                delayTime = 300;
            } else {
                delayTime = AD_LOAD_TIME;
            }

            mIsExtraLayout = true;
            mSwiftyView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Message msg = Message.obtain();
                    msg.what = LOAD_DONE_INIT_PLACE;
                    msg.obj = SWTIFY_TYPE_MSG;
                    mHandler.sendMessage(msg);
                }
            }, delayTime);
        }
    }

    private View mExtraView;

    private void initExtraLayout(View view) {
        ViewStub viewStub = (ViewStub) view.findViewById(R.id.content_type_2);
        if (viewStub == null) {
            return;
        }

        PreferenceTable preferenceTable = PreferenceTable.getInstance();

        boolean isContentEmpty = TextUtils.isEmpty(
                preferenceTable.getString(PrefConst.KEY_CHARGE_EXTRA_CONTENT));

        boolean isImgUrlEmpty = TextUtils.isEmpty(
                preferenceTable.getString(PrefConst.KEY_CHARGE_EXTRA_IMG_URL));

        boolean isTypeEmpty = TextUtils.isEmpty(
                preferenceTable.getString(PrefConst.KEY_CHARGE_EXTRA_TYPE));

        boolean isGpUrlEmpty = TextUtils.isEmpty(
                preferenceTable.getString(PrefConst.KEY_CHARGE_EXTRA_GP_URL));

        boolean isBrowserUrlEmpty = TextUtils.isEmpty(
                preferenceTable.getString(PrefConst.KEY_CHARGE_EXTRA_URL));

        boolean isUrlEmpty = isGpUrlEmpty && isBrowserUrlEmpty; //判断两个地址是否都为空

        if (!isContentEmpty && !isImgUrlEmpty && !isTypeEmpty && !isUrlEmpty) {
//        if (false) {
            mExtraView = viewStub.inflate();
            mExtraTitle = (TextView) mExtraView.findViewById(R.id.card_title);
            mExtraImg = (ImageView) mExtraView.findViewById(R.id.card_img);
            mExtraContent = (TextView) mExtraView.findViewById(R.id.card_content);
            mExtraLayout = (RelativeLayout) mExtraView.findViewById(R.id.parent_layout);
            mExtraLayout.setOnClickListener(this);
            mExtraContent.setText(preferenceTable.getString(PrefConst.KEY_CHARGE_EXTRA_CONTENT));
            String imgUrl = preferenceTable.getString(PrefConst.KEY_CHARGE_EXTRA_IMG_URL);
            mImageLoader.displayImage(imgUrl, mExtraImg, getOptions(R.drawable.online_theme_loading));
            boolean isTitleEmpty = TextUtils.isEmpty(
                    preferenceTable.getString(PrefConst.KEY_CHARGE_EXTRA_TITLE));
            if (!isTitleEmpty) {
                mExtraTitle.setText(preferenceTable.getString(
                        PrefConst.KEY_CHARGE_EXTRA_TITLE));
            }

            int delayTime;
            if (System.currentTimeMillis() - mInitTime > AD_LOAD_TIME) {
                delayTime = 300;
            } else {
                delayTime = AD_LOAD_TIME;
            }

            mIsExtraLayout = true;
            mExtraView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Message msg = Message.obtain();
                    msg.what = LOAD_DONE_INIT_PLACE;
                    msg.obj = EXTRA_TYPE_MSG;
                    mHandler.sendMessage(msg);
                }
            }, delayTime);
        }

    }

    public DisplayImageOptions getOptions(int drawble) {  //需要提供默认图
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(drawble)
                .showImageForEmptyUri(drawble)
                .showImageOnFail(drawble)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
                .build();

        return options;
    }

    private PopupWindow popupWindow;

    private void showPopUp(View v) {
        View contentView = LayoutInflater.from(mActivity).inflate(
                R.layout.popmenu_battery_list_item, null);

        popupWindow = new PopupWindow(contentView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);//TODO 原本是用height，为啥？
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());

        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBossView.setVisibility(View.INVISIBLE);
                popupWindow.dismiss();

                PreferenceTable.getInstance().putLong(Constants.AD_CLICK_IGNORE, System.currentTimeMillis());
            }
        });


        int[] location = new int[2];
        v.getLocationOnScreen(location);
        int x = location[0] - v.getWidth() * 2;
        int y = location[1] + v.getHeight();
        popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, x, y);

        //        TextView text = (TextView) contentView.findViewById(R.id.menu_text);
//        float textWidth = getTextViewLength(text, str);
//        LeoLog.d("testPop", "width : " + textWidth);
//
//        text.setText(str);
//        int size = (int) getResources().getDimension(R.dimen.popu_txt_size);
//        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
//        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(AppMasterApplication.sScreenWidth, View.MeasureSpec.AT_MOST);
//        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
//        text.measure(widthMeasureSpec, heightMeasureSpec);


//        int height = DipPixelUtil.dip2px(mActivity, 50);
//        if (textWidth > CHANGE_LINE_INT) {
//            x = location[0];
//            height = DipPixelUtil.dip2px(mActivity, 70);
//        } else if (textWidth > MID_WIDTH) {
//            x = location[0] - 80;
//        } else {
//            x = location[0] - 100;
//        }

    }

//    public float getTextViewLength(TextView textView, String text) {
//        TextPaint paint = textView.getPaint();
//        // 得到使用该paint写上text的时候,像素为多少
//        float textLength = paint.measureText(text);
//        return textLength;
//    }

}
