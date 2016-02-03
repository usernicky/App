
package com.leo.appmaster.sdk;

/**
 * Author: stonelam@leoers.com
 * Brief: Base activity to be tracked by application so that we can finish them when completely exit is required
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.leo.appmaster.mgr.BatteryManager;
import com.leo.appmaster.mgr.CallFilterManager;
import com.leo.appmaster.mgr.LockManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.WifiSecurityManager;

public class BaseActivity extends Activity {
    protected LockManager mLockManager;
    protected WifiSecurityManager mWifiManager;
    protected CallFilterManager mCallManger;
    private ActivityLifeCircle mLifeCircle;
    protected BatteryManager mBatteryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockManager = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);
        mWifiManager = (WifiSecurityManager) MgrContext.getManager(MgrContext.MGR_WIFI_SECURITY);
        mCallManger = (CallFilterManager) MgrContext.getManager(MgrContext.MGR_CALL_FILTER);
        mBatteryManager = (BatteryManager) MgrContext.getManager(MgrContext.MGR_BATTERY);

        mLifeCircle = new ActivityLifeCircle(this);
        mLifeCircle.onCreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mLifeCircle.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mLifeCircle.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mLifeCircle.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SDKWrapper.onResume(this);

        mLifeCircle.onResume();
    }

    @Override
    protected void onPause() {
        SDKWrapper.onPause(this);
        super.onPause();

        mLifeCircle.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
}
