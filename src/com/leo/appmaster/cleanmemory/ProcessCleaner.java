package com.leo.appmaster.cleanmemory;

import java.util.ArrayList;
import java.util.List;

import com.leo.appmaster.model.AppDetailInfo;
import com.leo.appmaster.utils.ProcessUtils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class ProcessCleaner {

	private Context mContext;
	private ActivityManager mAm;
	private static ProcessCleaner mInstance;

	private long mLastCleanTime;

	private long mMemTotal;
	private long mLastCleanMem;

	private long mLastMemUsed;
	private long mCurUsedMem;

	private static int CLEAN_INTERVAL = 3 * 1000;

	public static synchronized ProcessCleaner getInstance(Context ctx) {
		if (mInstance == null) {
			mInstance = new ProcessCleaner(
					(ActivityManager) ctx
							.getSystemService(Context.ACTIVITY_SERVICE),
					ctx);
		}

		return mInstance;
	}

	public long getTotalMem() {
		if (mMemTotal == 0) {
			mMemTotal = ProcessUtils.getTotalMem();
		}
		return mMemTotal;
	}

	public long getLastCleanMem() {
		return mLastCleanMem;
	}

	public long getUsedMem() {
		long curTime = System.currentTimeMillis();
		if ((curTime - mLastCleanTime) > CLEAN_INTERVAL) {
			mLastMemUsed = ProcessUtils.getUsedMem(mContext);
		}
		return mLastMemUsed;
	}

	public long getCurUsedMem() {
		return mCurUsedMem;
	}

	public boolean allowClean() {
		long curTime = System.currentTimeMillis();
		return (curTime - mLastCleanTime) > CLEAN_INTERVAL;
	}

	public ProcessCleaner(ActivityManager mAm, Context ctx) {
		this.mAm = mAm;
		this.mContext = ctx.getApplicationContext();
	}

	public long tryClean(Context ctx) {
		long resault = -1;
		long curTime = System.currentTimeMillis();
		if ((curTime - mLastCleanTime) > CLEAN_INTERVAL) {
			mLastCleanTime = curTime;
			cleanAllProcess(ctx);
			resault = mLastCleanMem = Math.abs(mCurUsedMem - mLastMemUsed);
			mLastMemUsed = mCurUsedMem;
		}
		return resault;
	}

	/**
	 * clean one process
	 * 
	 * @param pkg
	 */
	public void cleanProcess(String pkg) {
		mAm.killBackgroundProcesses(pkg);
	}

	/**
	 * clean multi processes
	 * 
	 * @param pkg
	 */
	public void cleanProcess(List<String> pkgs) {
		if (pkgs != null) {
			for (String pkg : pkgs) {
				mAm.killBackgroundProcesses(pkg);
			}
		}
	}

	/**
	 * clean all running process
	 */
	public void cleanAllProcess(Context cxt) {
		List<RunningAppProcessInfo> list = mAm.getRunningAppProcesses();
		List<String> launchers = getLauncherPkgs(cxt);
		for (RunningAppProcessInfo runningAppProcessInfo : list) {
			if (runningAppProcessInfo.importance > RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE) {
				if (!launchers.contains(runningAppProcessInfo.processName)) {
					mAm.killBackgroundProcesses(runningAppProcessInfo.processName);
				}
			}
		}
		mCurUsedMem = ProcessUtils.getUsedMem(cxt);
	}

	private List<String> getLauncherPkgs(Context ctx) {
		PackageManager pm = ctx.getPackageManager();
		List<String> pkgs = new ArrayList<String>();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_HOME);
		List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
		for (ResolveInfo resolveInfo : apps) {
			ApplicationInfo applicationInfo = resolveInfo.activityInfo.applicationInfo;
			String packageName = applicationInfo.packageName;
			pkgs.add(packageName);
		}
		return pkgs;
	}
}
