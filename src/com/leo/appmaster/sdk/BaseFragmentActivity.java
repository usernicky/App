
package com.leo.appmaster.sdk;

/**
 * Author: stonelam@leoers.com
 * Brief: all FragmentActivity should extends this class for SDK event track
 * */

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.mgr.LockManager;
import com.leo.appmaster.mgr.MgrContext;

public class BaseFragmentActivity extends FragmentActivity {
    protected LockManager mLockManager;
    private ActivityLifeCircle mLifeCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
        } catch (Exception e) {          
        } catch (Error error) {            
        }
        mLockManager = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);

        mLifeCircle = new ActivityLifeCircle(this);
        mLifeCircle.onCreate();
    }
    
    @Override
    protected void onStart() {
        try {
            super.onStart();
        } catch (Exception e) {            
        } catch (Error error) {            
        }

        mLifeCircle.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mLifeCircle.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mLifeCircle.onDestroy();
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
    
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            super.onRestoreInstanceState(savedInstanceState);
        } catch (Exception e) {
        }
    }

}
