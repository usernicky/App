package com.leo.appmaster.home;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.R;
import com.leo.appmaster.applocker.AppLockListActivity;
import com.leo.appmaster.applocker.RecommentAppLockListActivity;
import com.leo.appmaster.applocker.model.LockMode;
import com.leo.appmaster.intruderprotection.IntruderprotectionActivity;
import com.leo.appmaster.mgr.LockManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.impl.LostSecurityManagerImpl;
import com.leo.appmaster.phoneSecurity.PhoneSecurityActivity;
import com.leo.appmaster.phoneSecurity.PhoneSecurityConstants;
import com.leo.appmaster.phoneSecurity.PhoneSecurityGuideActivity;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.RippleView;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.wifiSecurity.WifiSecurityActivity;

/**
 * 首页下方4个tab
 *
 * @author Jasper
 */
public class HomeTabFragment extends Fragment implements RippleView.OnRippleCompleteListener {
    private static final String TAG = "HomeTabFragment";

    private ImageView mRedDot;
    // 首页4个tab
    private RippleView mAppLockView;
    private RippleView mIntruderView;
    private RippleView mWifiSecurityView;
    private RippleView mLostSecurityView;

    private View mRootView;
    private HomeActivity mActivity;

    private boolean mAnimating;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (HomeActivity) activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkNewTheme();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_tab, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRootView = view;

        mAppLockView = (RippleView) view.findViewById(R.id.home_app_lock_tv);
        mAppLockView.setOnRippleCompleteListener(this);

        mIntruderView = (RippleView) view.findViewById(R.id.home_intruder_tv);
        mIntruderView.setOnRippleCompleteListener(this);

        mWifiSecurityView = (RippleView) view.findViewById(R.id.home_wifi_tab);
        mWifiSecurityView.setOnRippleCompleteListener(this);

        LeoLog.d("testString", "wifi tab is : " + this.getString(R.string.home_tab_wifi));

        mLostSecurityView = (RippleView) view.findViewById(R.id.home_lost_tab);
        mLostSecurityView.setOnRippleCompleteListener(this);

        mRedDot = (ImageView) view.findViewById(R.id.have_theme_red_dot);
    }

    public void dismissTab() {
        if (mRootView.getVisibility() == View.GONE) return;

        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.anim_up_to_down);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mRootView.setVisibility(View.GONE);
                mActivity.onTabAnimationFinish();
                mAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mRootView.startAnimation(animation);
    }

    public boolean isAnimating() {
        return mAnimating;
    }

    public void showTab() {
        if (mRootView.getVisibility() == View.VISIBLE) return;

        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.anim_down_to_up);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mRootView.setVisibility(View.VISIBLE);
                mActivity.onTabAnimationFinish();
                mAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mRootView.startAnimation(animation);
    }

    public boolean isTabDismiss() {
        return mRootView.getVisibility() == View.GONE;
    }

    @Override
    public void onRippleComplete(RippleView rippleView) {
        switch (rippleView.getId()) {
            case R.id.home_app_lock_tv:
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "lock");
                LockManager mLockManager = (LockManager) MgrContext.
                        getManager(MgrContext.MGR_APPLOCKER);
                LockMode curMode = mLockManager.getCurLockMode();
                if (curMode != null && curMode.defaultFlag == 1 &&
                        !curMode.haveEverOpened) {
                    startRcommendLock(0);
                    curMode.haveEverOpened = true;
                    mLockManager.updateMode(curMode);
                } else {
                    Intent intent = new Intent(getActivity(), AppLockListActivity.class);
                    startActivity(intent);
                }
                break;
            case R.id.home_intruder_tv:
                // 入侵者防护
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "home_intruder");
                Intent intent = new Intent(getActivity(), IntruderprotectionActivity.class);
                startActivity(intent);
                break;
            case R.id.home_wifi_tab:
                // wifi安全
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "home_wifi");
                Intent mIntent = new Intent(getActivity(), WifiSecurityActivity.class);
                startActivity(mIntent);
                break;
            case R.id.home_lost_tab:
                // 手机防盗
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "home_theft");
                startPhoneSecurity();
                break;
        }
    }

    /*进入手机防盗*/
    private void startPhoneSecurity() {
        LostSecurityManagerImpl manager = (LostSecurityManagerImpl) MgrContext.getManager(MgrContext.MGR_LOST_SECURITY);
        boolean flag = manager.isUsePhoneSecurity();
        Intent intent = null;
        if (!flag) {
            intent = new Intent(getActivity(), PhoneSecurityGuideActivity.class);
            intent.putExtra(PhoneSecurityConstants.KEY_FORM_HOME_SECUR, true);
        } else {
            intent = new Intent(getActivity(), PhoneSecurityActivity.class);
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRcommendLock(int target) {
        Intent intent = new Intent(getActivity(), RecommentAppLockListActivity.class);
        intent.putExtra("target", target);
        startActivity(intent);
    }

    private void checkNewTheme() {
//        boolean isClickLockTab = PreferenceTable.getInstance().
//                getBoolean(Constants.IS_CLICK_LOCK_TAB, false);
        String locSerial = AppMasterPreference.getInstance(mActivity)
                .getLocalThemeSerialNumber();
        String onlineSerial = AppMasterPreference.getInstance(mActivity)
                .getOnlineThemeSerialNumber();
        if (mRedDot != null) {
            if (!locSerial.equals(onlineSerial)) {
                mRedDot.setVisibility(View.VISIBLE);
            } else {
                mRedDot.setVisibility(View.GONE);
            }
        }
    }

}
