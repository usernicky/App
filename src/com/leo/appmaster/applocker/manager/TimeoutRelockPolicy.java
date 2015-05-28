
package com.leo.appmaster.applocker.manager;

import java.util.HashMap;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.utils.LeoLog;

import android.content.Context;

public class TimeoutRelockPolicy implements ILockPolicy {

    private static final String TAG = "TimeoutRelockPolicy";

    Context mContext;

    private HashMap<String, UnlockTimeHolder> mLockapp = new HashMap<String, UnlockTimeHolder>();

    public TimeoutRelockPolicy(Context mContext) {
        super();
        this.mContext = mContext.getApplicationContext();

    }

    public int getRelockTime() {
        return AppMasterPreference.getInstance(mContext).getRelockTimeout();
    }

    @Override
    public boolean onHandleLock(String pkg) {
        long curTime = System.nanoTime() / 1000000;
        if (mLockapp.containsKey(pkg)) {
            long lastLockTime = mLockapp.get(pkg).lastUnlockTime;

            LeoLog.d(TAG, " curTime -  lastLockTime = "
                    + (curTime - lastLockTime) + "       mRelockTimeout =  "
                    + getRelockTime());
            if ((curTime - lastLockTime) < getRelockTime())
                return true;
        }
//        else {
//            UnlockTimeHolder holder = new UnlockTimeHolder();
//            holder.lastUnlockTime = curTime;
//            holder.secondUnlockTime = 0;
//            holder.firstUnlockTime = 0;
//            mLockapp.put(pkg, holder);
//        }
        return false;
    }

    public void clearLockApp() {
        mLockapp.clear();
    }

    @Override
    public void onUnlocked(String pkg) {
        long curTime =System.nanoTime() / 1000000;
        UnlockTimeHolder holder = mLockapp.get(pkg);
        if (holder == null) {
            holder = new UnlockTimeHolder();
            holder.lastUnlockTime = curTime;
            holder.secondUnlockTime = 0;
            holder.firstUnlockTime = 0;
            mLockapp.put(pkg, holder);
        } else {
            holder.firstUnlockTime = holder.secondUnlockTime;
            holder.secondUnlockTime = holder.lastUnlockTime;
            holder.lastUnlockTime = curTime;
        }

    }

    private static class UnlockTimeHolder {
        long firstUnlockTime;
        long secondUnlockTime;
        long lastUnlockTime;
    }

}
