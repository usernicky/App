package com.leo.appmaster.home;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.SecurityScoreEvent;
import com.leo.appmaster.privacy.PrivacyHelper;
import com.leo.appmaster.ui.HomeAnimLoadingLayer;
import com.leo.appmaster.ui.HomeAnimShieldLayer;
import com.leo.appmaster.ui.HomeAnimView;
import com.leo.appmaster.utils.LeoLog;
import com.leo.tools.animator.Animator;
import com.leo.tools.animator.AnimatorSet;
import com.leo.tools.animator.ObjectAnimator;
import com.leo.tools.animator.ValueAnimator;

/**
 * 隐私等级扫描
 * @author lishuai
 */
public class HomePrivacyFragment extends Fragment {
    private static final String TAG = "HomePrivacyFragment";
    private static final int ID_FAST_ARROW = 1000;
    // 每一分耗时50ms
    private static final int TPS = 50;
    private static final int MIN_TIME = 1000;

    public static int sScreenWidth = 0;
    public static int sScreenHeight = 0;
    private HomeAnimView mHomeAnimView;
    private AnimatorSet mAnimatorSet;
    private int mWidth;

    private FastHandler mHandler;

    private ObjectAnimator mFastArrowAnim;

    private boolean mStopped;
    private boolean mFistAnimFinished;
    private int mCurrentScore;

    private Animator mScanningAnimator;

    private HomeActivity mActivity;
    private AnimatorSet mFinalAnim;
    private boolean mProgressAnimating = true;
    private Runnable mScoreChangeRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHandler = new FastHandler(this);
        mHomeAnimView = (HomeAnimView) view.findViewById(R.id.home_wave_tv);
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mWidth = metrics.widthPixels;

        sScreenWidth = mWidth;
        sScreenHeight = metrics.heightPixels;

        initAnim();
        if (mHomeAnimView.isLayouted()) {
            startWaveDelay();
        } else {
            mHomeAnimView.startAfterLayout(new Runnable() {
                @Override
                public void run() {
                    startWaveDelay();
                }
            });
        }
    }

    public void showProcessProgress(int type) {
        if (type == PrivacyHelper.PRIVACY_APP_LOCK) {
            mHomeAnimView.setShowProcessLoading(true, HomeAnimLoadingLayer.LOAD_LOCK_APP);
        } else if (type == PrivacyHelper.PRIVACY_HIDE_PIC) {
            mHomeAnimView.setShowProcessLoading(true, HomeAnimLoadingLayer.LOAD_HIDE_PIC);
        } else if (type == PrivacyHelper.PRIVACY_HIDE_VID) {
            mHomeAnimView.setShowProcessLoading(true, HomeAnimLoadingLayer.LOAD_HIDE_VID);
        } else {
            mHomeAnimView.setShowProcessLoading(false, 0);
        }
    }

    /**
     * 开始上升动画及后续动画
     * 点击处理之后的场景
     * @param increaseScore
     */
    public void startLoadingRiseAnim(final int increaseScore) {
        mHomeAnimView.getLoadingLayer().setRiseHeight(0);
        int height = getActivity().getResources().getDimensionPixelSize(R.dimen.home_loading_rise);
        ObjectAnimator riseAnim = ObjectAnimator.ofInt(mHomeAnimView.getLoadingLayer(), "riseHeight", 0, height);
        riseAnim.setDuration(320);
        riseAnim.setInterpolator(new LinearInterpolator());
        riseAnim.addListener(new SimpleAnimatorListener() {

            @Override
            public void onAnimationEnd(Animator animation) {
                mHomeAnimView.setShowProcessLoading(false, 0);
                startShieldBeatAnim(increaseScore);
            }
        });

        riseAnim.start();
    }

    private ObjectAnimator getWaveAnimator(String propertyName, long delay, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(mHomeAnimView.getShieldLayer(), propertyName,
                0f, 1f);
        if (delay > 0) {
            animator.setStartDelay(delay);
        }
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());

        return animator;
    }

    private void initAnim() {
        PrivacyHelper helper = PrivacyHelper.getInstance(getActivity());
        int currentScore = helper.getSecurityScore();
        int time = (100 - currentScore) * TPS;
        time = time < MIN_TIME ? MIN_TIME : time;

        mHomeAnimView.getBgLayer().setTargetScore(currentScore);
        mHomeAnimView.getBgLayer().setIncrease(false);
        mCurrentScore = currentScore;

        AnimatorSet firstAnimSet = new AnimatorSet();
        // 时间减少
        ObjectAnimator timeAnim = ObjectAnimator.ofInt(mHomeAnimView, "securityScore", 100, currentScore);
        timeAnim.setDuration(time);
        timeAnim.setInterpolator(new LinearInterpolator());

        // 进度条
        ObjectAnimator progressAnim = ObjectAnimator.ofInt(mHomeAnimView, "progress", 0, mWidth);
        progressAnim.setDuration(time);
        progressAnim.setInterpolator(new LinearInterpolator());
        progressAnim.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mProgressAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mProgressAnimating = false;
            }
        });
        firstAnimSet.playTogether(timeAnim, progressAnim);

        List<Animator> animators = new ArrayList<Animator>();
        // 外环放大
        ObjectAnimator outCircleScaleAnim = ObjectAnimator.ofFloat(mHomeAnimView, "outCircleScaleRatio",
                HomeAnimShieldLayer.MIN_OUT_CIRCLE_SCALE_RATIO, HomeAnimShieldLayer.MAX_OUT_CIRCLE_SCALE_RATIO);
        outCircleScaleAnim.setInterpolator(new LinearInterpolator());
        outCircleScaleAnim.setDuration(600);
        animators.add(outCircleScaleAnim);

        // 内环放大
        ObjectAnimator inCircleScaleAnim = ObjectAnimator.ofFloat(mHomeAnimView, "inCircleScaleRatio",
                HomeAnimShieldLayer.MIN_IN_CIRCLE_SCALE_RATIO, HomeAnimShieldLayer.MAX_IN_CIRCLE_SCALE_RATIO);
        inCircleScaleAnim.setInterpolator(new LinearInterpolator());
        inCircleScaleAnim.setDuration(600);
        animators.add(inCircleScaleAnim);

        // 内环、外环旋转
        ObjectAnimator outCircleRotateAnim = ObjectAnimator.ofFloat(mHomeAnimView, "circleRotateRatio", 0f, 360f);
        outCircleRotateAnim.setDuration(3500);
        outCircleRotateAnim.setRepeatCount(ValueAnimator.INFINITE);
        outCircleRotateAnim.setInterpolator(new LinearInterpolator());
        animators.add(outCircleRotateAnim);

        // 盾牌缩小
        ObjectAnimator shieldScaleAnim = ObjectAnimator.ofFloat(mHomeAnimView, "shieldScaleRatio",
                HomeAnimShieldLayer.MAX_SHIELD_SCALE_RATIO, HomeAnimShieldLayer.MIN_SHIELD_SCALE_RATIO);
        shieldScaleAnim.setInterpolator(new LinearInterpolator());
        shieldScaleAnim.setDuration(600);
        animators.add(shieldScaleAnim);

        // 内环、外环透明度
        ObjectAnimator shieldAlphaAnim = ObjectAnimator.ofInt(mHomeAnimView, "circleAlpha", 0, 255);
        shieldScaleAnim.setInterpolator(new LinearInterpolator());
        shieldScaleAnim.setDuration(600);
        animators.add(shieldAlphaAnim);

        AnimatorSet secondAnimSet = new AnimatorSet();
        secondAnimSet.playTogether(animators);

        firstAnimSet.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mHandler.sendEmptyMessageDelayed(ID_FAST_ARROW, 2000);
                mFistAnimFinished = true;
            }
        });

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playSequentially(firstAnimSet, secondAnimSet);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (HomeActivity) activity;
        LeoEventBus.getDefaultBus().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LeoEventBus.getDefaultBus().unregister(this);

        endWaveAnim();
    }

    @Override
    public void onStart() {
        super.onStart();
//        if (mStopped) {
//            if (!mFistAnimFinished) {
//                initAnim();
//            } else {
//                initResumedAnim();
//            }
//        }

    }
    
    private void startAnim(long duration){
        ThreadManager.getUiThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startWaveAnim();
            }
        }, duration);
    }

    private void startWaveDelay() {
        if("HUAWEI P6-T00".equals(android.os.Build.MODEL)){
            LeoLog.i("dhdh", "in if");
            startAnim(500);
        }else{
            startAnim(400);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
//        endWaveAnim();
//        mStopped = true;
    }

    public void onEventMainThread(SecurityScoreEvent event) {
        final int oldScore = mCurrentScore;
        final int newScore = event.securityScore;
        mCurrentScore = newScore;

        LeoLog.i(TAG, "onEventMainThread, oldScore: " + oldScore + " | newScore: " + newScore +
                " | mProgressAnimating: " + mProgressAnimating);
        if (!mProgressAnimating) {
            doScoreChangeAnimation(oldScore, newScore);
        } else {
            mScoreChangeRunnable = new Runnable() {
                @Override
                public void run() {
                    doScoreChangeAnimation(oldScore, newScore);
                }
            };
        }
    }

    private void doScoreChangeAnimation(int fromScore, int toScore) {
        LeoLog.i(TAG, "doScoreChangeAnimation, fromScore: " + fromScore + " | toScore: " + toScore);
        mHomeAnimView.getBgLayer().setTargetScore(mCurrentScore);
        mHomeAnimView.getBgLayer().setIncrease(fromScore < toScore);

        // 时间减少
        ObjectAnimator timeAnim = ObjectAnimator.ofInt(mHomeAnimView, "securityScore", fromScore, toScore);
        timeAnim.setDuration(600);
        timeAnim.setInterpolator(new LinearInterpolator());
        timeAnim.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mScoreChangeRunnable = null;
            }
        });
        timeAnim.start();
    }

    private ObjectAnimator getIncreaseScoreAnim(int score) {
        int oldScore = mCurrentScore;
        int newScore = oldScore + score;
        mCurrentScore = newScore;

        mHomeAnimView.getBgLayer().setTargetScore(mCurrentScore);
        mHomeAnimView.getBgLayer().setIncrease(true);
        ObjectAnimator timeAnim = ObjectAnimator.ofInt(mHomeAnimView, "securityScore", oldScore, newScore);
        timeAnim.setDuration(360);
        timeAnim.setInterpolator(new LinearInterpolator());
        timeAnim.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mActivity.jumpToNextFragment();
                mHomeAnimView.increaseCurrentStep();
                startStepAnim();
            }
        });
        return timeAnim;
    }

    public void startIncreaseSocreAnim(int score) {
        int oldScore = mCurrentScore;
        int newScore = oldScore + score;
        mCurrentScore = newScore;

        mHomeAnimView.getBgLayer().setIncrease(true);
        ObjectAnimator timeAnim = ObjectAnimator.ofInt(mHomeAnimView, "securityScore", oldScore, newScore);
        timeAnim.setDuration(360);
        timeAnim.setInterpolator(new LinearInterpolator());
        timeAnim.start();
    }

    private void startStepAnim() {
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator lineAnim = ObjectAnimator.ofFloat(mHomeAnimView.getStepLayer(), "lineRatio", 0f, 1f);
        lineAnim.setDuration(920);
        lineAnim.setInterpolator(new LinearInterpolator());

        ObjectAnimator circleBigAnim = ObjectAnimator.ofFloat(mHomeAnimView.getStepLayer(), "circleRatio", 1f, 1.3f);
        circleBigAnim.setDuration(160);
        circleBigAnim.setInterpolator(new LinearInterpolator());

        ObjectAnimator circleSmallAnim = ObjectAnimator.ofFloat(mHomeAnimView.getStepLayer(), "circleRatio", 1.3f, 1f);
        circleSmallAnim.setDuration(240);
        circleSmallAnim.setInterpolator(new LinearInterpolator());

        animatorSet.playSequentially(lineAnim, circleBigAnim, circleSmallAnim);
        animatorSet.start();
    }

    public void increaseStepAnim() {
        mHomeAnimView.increaseCurrentStep();
        startStepAnim();
    }

    public void startShieldBeatAnim(final int increaseScore) {
        ObjectAnimator shieldBeatAnim = ObjectAnimator.ofFloat(mHomeAnimView, "shieldScaleRatio",
                HomeAnimShieldLayer.MIN_SHIELD_SCALE_RATIO, 0.84f, HomeAnimShieldLayer.MIN_SHIELD_SCALE_RATIO);
        shieldBeatAnim.setInterpolator(new LinearInterpolator());
        shieldBeatAnim.setDuration(320);

        ObjectAnimator wave1 = getWaveAnimator("firstWaveRatio", 80, 1200);
        ObjectAnimator wave2 = getWaveAnimator("secondWaveRatio", 480, 1200);
        ObjectAnimator wave3 = getWaveAnimator("thirdWaveRatio", 880, 1200);

        ObjectAnimator increaseScoreAnim = getIncreaseScoreAnim(increaseScore);
        increaseScoreAnim.setStartDelay(600);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(shieldBeatAnim, wave1, wave2, wave3, increaseScoreAnim);
        animatorSet.start();
    }

    /**
     * 设置是否显示多彩进度条
     */
    public void setShowColorProgress(boolean show) {
        mHomeAnimView.setShowColorProgress(show);
    }

    /**
     * 显示进度百分比动画
     * @param duration 时常，-1则不显示
     */
    public void showScanningPercent(int duration) {
        if (duration == -1) {
            if (mScanningAnimator != null) {
                mScanningAnimator.cancel();
            }
            mHomeAnimView.setScanningPercent(-1);
        } else {
            mScanningAnimator = ObjectAnimator.ofInt(mHomeAnimView, "scanningPercent", 0, 101);
            mScanningAnimator.setDuration(duration);
            mScanningAnimator.setInterpolator(new LinearInterpolator());
            mScanningAnimator.start();
        }
    }

    /**
     * 开始盾牌上移、外环框隐藏动画
     */
    public void startProcessing(int stepCount) {
        if (isRemoving() || isDetached() || getActivity() == null
                || mHomeAnimView == null
                || mHomeAnimView.getShieldLayer() == null) {
            return;
        }

        showScanningPercent(-1);

        mHomeAnimView.setTotalStepCount(stepCount);
        int offsetY = mHomeAnimView.getShieldLayer().getMaxOffsetY();
        ObjectAnimator shieldOffsetYAnim = ObjectAnimator.ofInt(mHomeAnimView, "shieldOffsetY", 0, offsetY);
        int duration = getActivity().getResources().getInteger(android.R.integer.config_mediumAnimTime);
        shieldOffsetYAnim.setDuration(duration);
        shieldOffsetYAnim.setInterpolator(new LinearInterpolator());
        shieldOffsetYAnim.start();
    }

    /**
     * 开启隐私完成页面的盾牌上移、分数放大动画
     */
    public void startFinalAnim() {
        ObjectAnimator shieldRatio = ObjectAnimator.ofFloat(mHomeAnimView.getShieldLayer(),
                "finalShieldRatio", 0f, 1f);
        shieldRatio.setDuration(480);
        shieldRatio.setInterpolator(new LinearInterpolator());

        ObjectAnimator textRatioBig1 = ObjectAnimator.ofFloat(mHomeAnimView.getShieldLayer(),
                "finalTextRatio", 0.76f, 1.34f);
        textRatioBig1.setDuration(720);
        textRatioBig1.setInterpolator(new LinearInterpolator());

        ObjectAnimator textRatioBig2 = ObjectAnimator.ofFloat(mHomeAnimView.getShieldLayer(),
                "finalTextRatio", 1.34f, 0.76f);
        textRatioBig2.setDuration(320);
        textRatioBig2.setInterpolator(new LinearInterpolator());

        float maxRatio = 1.1f;
        if (mHomeAnimView.getStepLayer().getTotalStepCount() == 1) {
            maxRatio = 1.6f;
        }
        ObjectAnimator textRatioSmall = ObjectAnimator.ofFloat(mHomeAnimView.getShieldLayer(),
                "finalTextRatio", 0.76f, maxRatio);
        textRatioSmall.setDuration(200);
        textRatioSmall.setInterpolator(new LinearInterpolator());

        AnimatorSet textAnim = new AnimatorSet();
        textAnim.playSequentially(textRatioBig1, textRatioBig2, textRatioSmall);

        AnimatorSet finalAnim = new AnimatorSet();
        finalAnim.playTogether(shieldRatio, textAnim);
        finalAnim.start();
        mFinalAnim = finalAnim;
    }

    public void stopFinalAnim() {
        if (mFinalAnim != null) {
            mFinalAnim.cancel();
            mFinalAnim = null;
        }
    }

    private void startWaveAnim() {
        if (!mHandler.hasMessages(ID_FAST_ARROW) && mStopped && mFistAnimFinished) {
            mHandler.sendEmptyMessageDelayed(ID_FAST_ARROW, 3000);
        }
        if (mAnimatorSet != null) {
            mAnimatorSet.start();
        }
    }

    private void endWaveAnim() {
        mHandler.removeMessages(ID_FAST_ARROW);
        if (mAnimatorSet != null) {
            mAnimatorSet.end();
            mAnimatorSet = null;
        }
    }

    private void initResumedAnim() {
        // 内环、外环旋转
        mAnimatorSet = new AnimatorSet();
        ObjectAnimator outCircleRotateAnim = ObjectAnimator.ofFloat(mHomeAnimView, "circleRotateRatio", 0f, 360f);
        outCircleRotateAnim.setDuration(3500);
        outCircleRotateAnim.setRepeatCount(ValueAnimator.INFINITE);
        outCircleRotateAnim.setInterpolator(new LinearInterpolator());
        mAnimatorSet.play(outCircleRotateAnim);

        mHomeAnimView.setCircleAlpha(255);
        mHomeAnimView.setShieldScaleRatio(HomeAnimShieldLayer.MIN_SHIELD_SCALE_RATIO);
    }

    private void postFastArrowAnim() {
        // 快速滚动条
        int width = mHomeAnimView.getFastArrowWidth() + mWidth;
        ObjectAnimator fastArrowAnim = ObjectAnimator.ofInt(mHomeAnimView, "fastProgress", 0, width);
        fastArrowAnim.setDuration(1000);
        fastArrowAnim.setInterpolator(new LinearInterpolator());
        fastArrowAnim.start();
        mHandler.sendEmptyMessageDelayed(ID_FAST_ARROW, 3000);
    }

    public void reset() {
        showScanningPercent(-1);
        stopFinalAnim();
        mHomeAnimView.setShowProcessLoading(false, 0);
        mHomeAnimView.setShieldOffsetY(0);
        mHomeAnimView.getShieldLayer().setFinalShieldRatio(0);
        mHomeAnimView.getShieldLayer().setFinalTextRatio(HomeAnimShieldLayer.MIN_SHIELD_SCALE_RATIO);
    }

    public int getToolbarColor() {
        return mHomeAnimView.getToolbarColor();
    }

    private static class FastHandler extends Handler {
        WeakReference<HomePrivacyFragment> weakRef;
        FastHandler(HomePrivacyFragment fragment) {
            weakRef = new WeakReference<HomePrivacyFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ID_FAST_ARROW:
                    HomePrivacyFragment fragment = weakRef.get();
                    if (fragment == null) break;

                    fragment.postFastArrowAnim();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }
}
