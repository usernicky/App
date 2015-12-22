package com.leo.appmaster.home;

import android.view.View;
import android.view.animation.LinearInterpolator;

import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.ui.MaterialRippleLayout;
import com.leo.appmaster.ui.ScanningImageView;
import com.leo.appmaster.ui.ScanningTextView;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;
import com.leo.tools.animator.Animator;
import com.leo.tools.animator.AnimatorListenerAdapter;
import com.leo.tools.animator.AnimatorSet;
import com.leo.tools.animator.ObjectAnimator;
import com.leo.tools.animator.ValueAnimator;

/**
 * Created by Jasper on 2015/11/20.
 */
public class HomeScanningController {
    private static final String TAG = "HomeScanningController";
    private static final int PER_APP = 30;
    private static final int PER_PIC = 70;
    private static final int PER_VID = 90;
    private static final int PER_PRI = 100;

    private static final int DURATION_ROTATE = 400;

    private static final int UP_LIMIT_APP = 3000;
    private static final int UP_LIMIT_PIC = 5000;
    private static final int UP_LIMIT_PIC_PROCESSED = 3000;
    private static final int UP_LIMIT_VID = 2000;

    private ScanningImageView mAppImg;
    private ScanningTextView mAppText;
    private ScanningImageView mPicImg;
    private ScanningTextView mPicText;
    private ScanningImageView mVidImg;
    private ScanningTextView mVidText;

    private ScanningImageView mPrivacyImg;
    private ScanningTextView mPrivacyText;

    private AnimEntry mAppAnim;
    private AnimEntry mPicAnim;
    private AnimEntry mVidAnim;
    private AnimEntry mPrivacyAnim;

    private HomeScanningFragment mFragment;
    private HomeActivity mActivity;

    private int mDurationPrivacy;

    private MaterialRippleLayout mNewAppLayout;
    private MaterialRippleLayout mNewPicLayout;
    private MaterialRippleLayout mNewVidLayout;
    private MaterialRippleLayout mNewInstructLayout;
    private MaterialRippleLayout mNewWifiLayout;
    private ObjectAnimator mNewAppAnim;
    private ObjectAnimator mNewPicAnim;
    private ObjectAnimator mNewVidAnim;
    private ObjectAnimator mNewInstructAnim;
    private ObjectAnimator mNewWifiAnim;
    private static final int NEW_PER_APP = 20;
    private static final int NEW_PER_PIC = 40;
    private static final int NEW_PER_VID = 60;
    private static final int NEW_PER_INS = 80;
    private static final int NEW_PER_PRI = 100;

    private static final int NEW_UP_LIMIT_APP = 3500;
    private static final int NEW_UP_LIMIT_PIC = 3500;
    private static final int NEW_UP_LIMIT_PIC_PROCESSED = 5000;
    private static final int NEW_UP_LIMIT_VID = 1500;
    private static final int NEW_UP_LIMIT_INS = 1500;

    public HomeScanningController(HomeActivity activity, HomeScanningFragment fragment,
                                  ScanningImageView appImg, ScanningTextView appText,
                                  ScanningImageView picImg, ScanningTextView picText,
                                  ScanningImageView vidImg, ScanningTextView vidText,
                                  ScanningImageView privacyImg, ScanningTextView privacyText) {
        mActivity = activity;
        mFragment = fragment;

        mAppImg = appImg;
        mAppText = appText;
        mPicImg = picImg;
        mPicText = picText;
        mVidImg = vidImg;
        mVidText = vidText;
        mPrivacyImg = privacyImg;
        mPrivacyText = privacyText;
    }

//    public HomeScanningController(HomeActivity activity, HomeScanningFragment fragment,
//           MaterialRippleLayout newAppLayout, MaterialRippleLayout newPicLayout,
//           MaterialRippleLayout newVidLayout, MaterialRippleLayout newInstructLayout,
//           MaterialRippleLayout newWifiLayout) {
//
//        mActivity = activity;
//        mFragment = fragment;
//
//        mNewAppLayout = newAppLayout;
//        mNewPicLayout = newPicLayout;
//        mNewVidLayout = newVidLayout;
//        mNewInstructLayout = newInstructLayout;
//        mNewWifiLayout = newWifiLayout;
//    }

    private AnimEntry createScanningAnim(final ScanningImageView imageView, ScanningTextView textView) {
        AnimEntry entry = new AnimEntry();
        final AnimatorSet appAnimSet = new AnimatorSet();
        // 外环放大
        ObjectAnimator scaleImgAnim = ObjectAnimator.ofFloat(imageView, "scaleRatio", 0f, 1f);
        scaleImgAnim.setInterpolator(new LinearInterpolator());
        scaleImgAnim.setDuration(300);

        ObjectAnimator scaleTextAnim = ObjectAnimator.ofFloat(textView, "scaleRatio", 0f, 1f);
        scaleTextAnim.setInterpolator(new LinearInterpolator());
        scaleTextAnim.setDuration(300);

        AnimatorSet scaleAnim = new AnimatorSet();
        scaleAnim.playTogether(scaleImgAnim, scaleTextAnim);

        ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(imageView, "rotateDegree", 1f, 360f);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setDuration(DURATION_ROTATE);
        rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnim.addListener(new SimpleAnimatorListener() {

            @Override
            public void onAnimationRepeat(Animator animation) {
                onAnimatorRepeat(animation);
            }
        });

        appAnimSet.playSequentially(scaleAnim, rotateAnim);
        entry.totalAnim = appAnimSet;
        entry.rotateAnim = rotateAnim;

        ObjectAnimator innerScaleAnim = ObjectAnimator.ofFloat(imageView, "innerDrawableScale",
                ScanningImageView.INNER_SCALE, 1f);
        innerScaleAnim.setInterpolator(new LinearInterpolator());
        innerScaleAnim.setDuration(300);
        innerScaleAnim.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onAnimatorEnd(animation);
            }
        });

        entry.innerScaleAnim = innerScaleAnim;
        return entry;
    }


    private AnimEntry createPrivacyAnim(final ScanningImageView imageView, ScanningTextView textView) {
        AnimEntry entry = new AnimEntry();
        final AnimatorSet appAnimSet = new AnimatorSet();
        // 外环放大
        ObjectAnimator scaleImgAnim = ObjectAnimator.ofFloat(imageView, "scaleRatio", 0f, 1f);
        scaleImgAnim.setInterpolator(new LinearInterpolator());
        scaleImgAnim.setDuration(300);

        ObjectAnimator scaleTextAnim = ObjectAnimator.ofFloat(textView, "scaleRatio", 0f, 1f);
        scaleTextAnim.setInterpolator(new LinearInterpolator());
        scaleTextAnim.setDuration(300);

        AnimatorSet scaleAnim = new AnimatorSet();
        scaleAnim.playTogether(scaleImgAnim, scaleTextAnim);

        int repeatCount = 1;
        int duration = (repeatCount + 1) * DURATION_ROTATE;
        ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(imageView, "rotateDegree", 1f, 360f);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setDuration(DURATION_ROTATE);
        rotateAnim.setRepeatCount(repeatCount);

        ObjectAnimator innerScaleAnim = ObjectAnimator.ofFloat(imageView, "innerDrawableScale",
                ScanningImageView.INNER_SCALE, 1f);
        innerScaleAnim.setInterpolator(new LinearInterpolator());
        innerScaleAnim.setDuration(300);
        innerScaleAnim.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onAnimatorEnd(animation);
            }
        });

        appAnimSet.playSequentially(scaleAnim, rotateAnim, innerScaleAnim);
        entry.totalAnim = appAnimSet;
        entry.rotateAnim = rotateAnim;
        entry.innerScaleAnim = innerScaleAnim;

        mDurationPrivacy = 300 + duration + 300;
        return entry;
    }

    public void startScanning() {
        LeoLog.d(TAG, "startScanning...");
        mAppAnim = createScanningAnim(mAppImg, mAppText);

        mAppAnim.totalAnim.start();
        mActivity.scanningFromPercent(UP_LIMIT_APP, 0, PER_APP);
//        mNewAppAnim = getItemAnimation(mNewAppLayout);
//        mNewAppAnim.setStartDelay(600);
//        mNewAppAnim.start();
//        mActivity.scanningFromPercent(NEW_UP_LIMIT_APP, 0, NEW_PER_APP);
    }

    private void onAnimatorEnd(Animator animation) {
        if (mFragment.isRemoving() || mFragment.isDetached()) return;

        LeoLog.d(TAG, "onAnimatorEnd...");
        if (animation == mAppAnim.innerScaleAnim) {
            mFragment.onAnimatorEnd(mAppImg);

            mPicAnim = createScanningAnim(mPicImg, mPicText);
            int currPct = mActivity.getScanningPercent();
            PreferenceTable preferenceTable = PreferenceTable.getInstance();
            boolean picConsumed = preferenceTable.getBoolean(PrefConst.KEY_PIC_COMSUMED, false);
            int duration = picConsumed ? UP_LIMIT_PIC_PROCESSED : UP_LIMIT_PIC;
            mActivity.scanningFromPercent(duration, currPct, PER_PIC);
            mPicAnim.totalAnim.start();
        } else if (animation == mPicAnim.innerScaleAnim) {
            mFragment.onAnimatorEnd(mPicImg);

            mVidAnim = createScanningAnim(mVidImg, mVidText);
            int currPct = mActivity.getScanningPercent();
            mActivity.scanningFromPercent(UP_LIMIT_VID, currPct, PER_VID);
            mVidAnim.totalAnim.start();
        } else if (animation == mVidAnim.innerScaleAnim) {
            mFragment.onAnimatorEnd(mVidImg);

            mPrivacyAnim = createPrivacyAnim(mPrivacyImg, mPrivacyText);
            int currPct = mActivity.getScanningPercent();
            mActivity.scanningFromPercent(mDurationPrivacy, currPct, PER_PRI + 1);
            mPrivacyAnim.totalAnim.start();
        } else {
            mFragment.onAnimatorEnd(mPrivacyImg);
        }
    }

    private void onAnimatorRepeat(Animator animator) {
        if (mFragment.isRemoving() || mFragment.isDetached()) return;

        LeoLog.d(TAG, "onAnimatorRepeat...");
        if (animator == mAppAnim.rotateAnim) {
            if (mFragment.isScanFinish(mAppImg)) {
                cancelAnimatorDelay(animator);
                mAppAnim.innerScaleAnim.start();
            }
        } else if (animator == mPicAnim.rotateAnim) {
            if (mFragment.isScanFinish(mPicImg)) {
                cancelAnimatorDelay(animator);
                mPicAnim.innerScaleAnim.start();
            }
        } else if (animator == mVidAnim.rotateAnim) {
            if (mFragment.isScanFinish(mVidImg)) {
                cancelAnimatorDelay(animator);
                mVidAnim.innerScaleAnim.start();
            }
        }
    }

    private void cancelAnimatorDelay(final Animator animator) {
        ThreadManager.getUiThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                animator.end();
            }
        }, 100);
    }

    public void detachController() {
        AnimEntry entry = mAppAnim;
        endAnimEntry(entry);

        entry = mPicAnim;
        endAnimEntry(entry);

        entry = mVidAnim;
        endAnimEntry(entry);

        entry = mPrivacyAnim;
        endAnimEntry(entry);
    }

    private void endAnimEntry(AnimEntry entry) {
        if (entry != null) {
            if (entry.totalAnim != null) {
                entry.totalAnim.end();
            }
            if (entry.rotateAnim != null) {
                entry.rotateAnim.end();
            }
            if (entry.innerScaleAnim != null) {
                entry.innerScaleAnim.end();
            }
        }
    }

    private class AnimEntry {
        public Animator totalAnim;
        public Animator rotateAnim;
        public Animator innerScaleAnim;
    }

    private ObjectAnimator getItemAnimation(final MaterialRippleLayout layout) {
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(layout, "alpha", 0f, 1f);
        alphaAnim.setDuration(1000);
        alphaAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                layout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onItemAnimationEnd(animation);
            }
        });

        return alphaAnim;
    }

    private void onItemAnimationEnd(Animator animation) {
        if (mFragment.isRemoving() || mFragment.isDetached()) return;

        LeoLog.d(TAG, "onAnimatorEnd...");
        if (animation == mNewAppAnim) {
            mFragment.OnItemAnimationEnd(mNewAppLayout);

            mNewPicAnim = getItemAnimation(mNewPicLayout);
            int currPct = mActivity.getScanningPercent();
            PreferenceTable preferenceTable = PreferenceTable.getInstance();
            boolean picConsumed = preferenceTable.getBoolean(PrefConst.KEY_PIC_COMSUMED, false);
            int duration = picConsumed ? NEW_UP_LIMIT_PIC_PROCESSED : NEW_UP_LIMIT_PIC;
            mActivity.scanningFromPercent(duration, currPct, NEW_PER_PIC);
            LeoLog.e(TAG, "mNewAppAnim end");
            mNewPicAnim.start();
        } else if (animation == mNewPicAnim) {
            mFragment.OnItemAnimationEnd(mNewPicLayout);

            mNewVidAnim = getItemAnimation(mNewVidLayout);
            int currPct = mActivity.getScanningPercent();
            mActivity.scanningFromPercent(NEW_UP_LIMIT_VID, currPct, NEW_PER_VID);
            LeoLog.e(TAG, "mNewPicAnim end");
            mNewVidAnim.start();
        } else if (animation == mNewVidAnim) {
            mFragment.OnItemAnimationEnd(mNewVidLayout);

            mNewInstructAnim = getItemAnimation(mNewInstructLayout);
            int currPct = mActivity.getScanningPercent();
            mActivity.scanningFromPercent(NEW_UP_LIMIT_INS, currPct, NEW_PER_INS);
            LeoLog.e(TAG, "mNewVidAnim end");
            mNewInstructAnim.start();
        } else if (animation == mNewInstructAnim) {
            mFragment.OnItemAnimationEnd(mNewInstructLayout);

            mNewWifiAnim = getItemAnimation(mNewWifiLayout);
            int currPct = mActivity.getScanningPercent();
            mActivity.scanningFromPercent(1000, currPct, NEW_PER_PRI + 1);
            LeoLog.e(TAG, "mNewInstructAnim end");
            mNewWifiAnim.start();
        } else {
            mFragment.OnItemAnimationEnd(mNewWifiLayout);
        }
    }

    public void detachTheController() {
         endAnim(mNewAppAnim);
         endAnim(mNewPicAnim);
         endAnim(mNewVidAnim);
         endAnim(mNewInstructAnim);
         endAnim(mNewWifiAnim);
    }

    private void endAnim(Animator animator) {
        if (animator != null) {
            animator.end();
            animator = null;
        }
    }
}
