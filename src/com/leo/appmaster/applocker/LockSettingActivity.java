
package com.leo.appmaster.applocker;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.R;
import com.leo.appmaster.applocker.service.StatusBarEventService;
import com.leo.appmaster.fragment.GestureSettingFragment;
import com.leo.appmaster.fragment.PasswdSettingFragment;
import com.leo.appmaster.sdk.BaseFragmentActivity;
import com.leo.appmaster.ui.CommonTitleBar;

import android.R.integer;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class LockSettingActivity extends BaseFragmentActivity implements
        OnClickListener {

    public static final String RESET_PASSWD_FLAG = "reset_passwd";
    public static final String ROTATE_FRAGMENT = "rotate_fragment";
    public static final int LOCK_TYPE_PASSWD = 1;
    public static final int LOCK_TYPE_GESTURE = 2;
    public final int mAppLockType = 1;
    public final int mAppWeiZhuang = 2;
    public final int mPicHide = 3;
    public final int mVioHide = 4;
    public final int mPrivateSms = 5;
    public final int mFlow = 6;
    public  final int mElec = 7;
    public  final int mBackup = 8;
    public  final int mQuickGues = 9;
    public  final int mLockThem = 10;
    // private int mLockType = LOCK_TYPE_PASSWD;
    private int mLockType = LOCK_TYPE_GESTURE;
    private CommonTitleBar mTitleBar;
    private FragmentManager mFm;
    private PasswdSettingFragment mPasswd;
    private GestureSettingFragment mGesture;
    private TextView mSwitchBottom;
    private View switch_bottom_content;
    private ImageView iv_reset_icon;
    private Resources res;

    private boolean mResetFlag;
    private boolean mIsRotateFragment = false;

    public boolean mToLockList;
    public boolean mJustFinish;
    public boolean mFromQuickMode;
    public int mModeId;
    public int mFromDeskId = -1;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_lock_setting);
        handleIntent();
        initUI();
        initFragment();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            super.onRestoreInstanceState(savedInstanceState);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleIntent() {
        Intent intent = getIntent();
        mFromDeskId = intent.getIntExtra(StatusBarEventService.EXTRA_EVENT_TYPE,
                StatusBarEventService.EVENT_EMPTY);
        mResetFlag = intent.getBooleanExtra(RESET_PASSWD_FLAG, false);
        mIsRotateFragment = intent.getBooleanExtra(ROTATE_FRAGMENT, false);
        mToLockList = intent.getBooleanExtra("to_lock_list", false);
        mJustFinish = intent.getBooleanExtra("just_finish", false);
        mFromQuickMode = intent.getBooleanExtra("from_quick_mode", false);
        mModeId = intent.getIntExtra("mode_id", -1);
    }

    private void initFragment() {
        mPasswd = new PasswdSettingFragment();
        mGesture = new GestureSettingFragment();

        if (mResetFlag) {
            mTitleBar.setTitle(R.string.reset_passwd);
        }

        mFm = getSupportFragmentManager();
        FragmentTransaction tans = mFm.beginTransaction();
        int type = AppMasterPreference.getInstance(this).getLockType();

        if (!mIsRotateFragment) {
            if (type == AppMasterPreference.LOCK_TYPE_PASSWD) {
                mLockType = LOCK_TYPE_PASSWD;
                tans.replace(R.id.fragment_contain, mPasswd);
                mSwitchBottom.setText(getString(R.string.switch_gesture));
                iv_reset_icon.setBackgroundResource(
                        R.drawable.reset_pass_gesture_icon);
            } else {
                mLockType = LOCK_TYPE_GESTURE;
                tans.replace(R.id.fragment_contain, mGesture);
                mSwitchBottom.setText(getString(R.string.switch_passwd));
                iv_reset_icon.setBackgroundResource(
                        R.drawable.reset_pass_number_icon);
            }
        } else {
            if (type == AppMasterPreference.LOCK_TYPE_PASSWD) {
                mLockType = LOCK_TYPE_GESTURE;
                tans.replace(R.id.fragment_contain, mGesture);
                mSwitchBottom.setText(getString(R.string.switch_passwd));
                iv_reset_icon.setBackgroundResource(
                        R.drawable.reset_pass_number_icon);
            } else {
                mLockType = LOCK_TYPE_PASSWD;
                tans.replace(R.id.fragment_contain, mPasswd);
                mSwitchBottom.setText(getString(R.string.switch_gesture));
                iv_reset_icon.setBackgroundResource(
                        R.drawable.reset_pass_gesture_icon);
            }
        }

        // if (type == AppMasterPreference.LOCK_TYPE_GESTURE) {
        // LeoLog.d("testSetPass", "LOCK_TYPE_GESTURE");
        // mLockType = LOCK_TYPE_GESTURE;
        // tans.replace(R.id.fragment_contain, mGesture);
        // mSwitchBottom.setText(getString(R.string.switch_passwd));
        // } else {
        // LeoLog.d("testSetPass", "LOCK_TYPE_PASSWD");
        // mLockType = LOCK_TYPE_PASSWD;
        // tans.replace(R.id.fragment_contain, mPasswd);
        // mSwitchBottom.setText(getString(R.string.switch_gesture));
        // }
        tans.commit();

    }

    private void initUI() {
        res = getResources();

        mTitleBar = (CommonTitleBar) findViewById(R.id.layout_title_bar);
        //

        //
        if (mResetFlag) {
            mTitleBar.openBackView();
            mTitleBar.setTitle(R.string.reset_passwd);
            // mTitleBar.setVisibility(View.INVISIBLE);
        } else {
            // mTitleBar.openBackView();
            // mTitleBar.setTitle(R.string.passwd_setting);

            // mTitleBar.setVisibility(View.INVISIBLE);

            Display mDisplay = getWindowManager().getDefaultDisplay();
            int W = mDisplay.getWidth();
            int H = mDisplay.getHeight();
            // 使得小尺寸机型在此时去除titlebar，而不是隐藏，否则下方位置不够
            if (H > 900)
            {
                mTitleBar.setVisibility(View.INVISIBLE);
            }
            else
            {
                mTitleBar.setVisibility(View.GONE);
            }

        }

        mSwitchBottom = (TextView) this.findViewById(R.id.switch_bottom);
        switch_bottom_content = findViewById(R.id.switch_bottom_content);
        iv_reset_icon = (ImageView) findViewById(R.id.iv_reset_icon);
        switch_bottom_content.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_bottom_content:
                switchLockType();
                break;
            default:
                break;
        }
    }

    private void switchLockType() {
        FragmentTransaction tans = mFm.beginTransaction();
        if (mLockType == LOCK_TYPE_PASSWD) {
            tans.replace(R.id.fragment_contain, mGesture);
            mLockType = LOCK_TYPE_GESTURE;
            mSwitchBottom.setText(getString(R.string.switch_passwd));
            iv_reset_icon.setBackgroundResource(
                    R.drawable.reset_pass_number_icon);
        } else {
            tans.replace(R.id.fragment_contain, mPasswd);
            mLockType = LOCK_TYPE_PASSWD;
            mSwitchBottom.setText(getString(R.string.switch_gesture));
            iv_reset_icon.setBackgroundResource(
                    R.drawable.reset_pass_gesture_icon);
        }
        tans.commit();
    }

    public boolean isResetPasswd() {
        return mResetFlag;
    }

    public int getFromDeskId() {
        return mFromDeskId;
    }
}
