
package com.leo.appmaster.bootstrap;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.airsig.airsigengmulti.ASEngine;
import com.android.internal.telephony.ITelephony;
import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.Constants;
import com.leo.appmaster.PhoneInfo;
import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.airsig.airsigsdk.ASGui;
import com.leo.appmaster.airsig.airsigsdk.ASSetting;
import com.leo.appmaster.applocker.LockScreenActivity;
import com.leo.appmaster.applocker.manager.MobvistaEngine;
import com.leo.appmaster.applocker.receiver.DeviceReceiver;
import com.leo.appmaster.applocker.receiver.LockReceiver;
import com.leo.appmaster.applocker.service.TaskProtectService;
import com.leo.appmaster.appmanage.business.AppBusinessManager;
import com.leo.appmaster.backup.AppBackupRestoreManager;
import com.leo.appmaster.cleanmemory.HomeBoostActivity;
import com.leo.appmaster.db.AppMasterDBHelper;
import com.leo.appmaster.db.LeoPreference;
import com.leo.appmaster.engine.AppLoadEngine;
import com.leo.appmaster.home.SplashActivity;
import com.leo.appmaster.mgr.DeviceManager;
import com.leo.appmaster.mgr.LockManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.WifiSecurityManager;
import com.leo.appmaster.privacy.PrivacyHelper;
import com.leo.appmaster.privacycontact.PrivacyContactReceiver;
import com.leo.appmaster.privacycontact.PrivacyContactUtils;
import com.leo.appmaster.privacycontact.PrivacyMessageContentObserver;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.sdk.update.UIHelper;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;
import com.leo.appmaster.utils.Utilities;
import com.leo.imageloader.DisplayImageOptions;
import com.leo.imageloader.ImageLoader;
import com.leo.imageloader.ImageLoaderConfiguration;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;

/**
 * 主线程核心业务初始化
 *
 * @author Jasper
 */
public class InitCoreBootstrap extends Bootstrap {
    private static final String TAG = "InitCoreBootstrap";

    InitCoreBootstrap() {
        super();
    }

    @Override
    protected boolean doStrap() {
        long start = SystemClock.elapsedRealtime();
        AppLoadEngine.getInstance(mApp);
        long end = SystemClock.elapsedRealtime();
        LeoLog.i(TAG, "cost, AppLoadEngine.getInstance: " + (end - start));

        registerPackageChangedBroadcast();

        start = SystemClock.elapsedRealtime();
        SDKWrapper.iniSDK(mApp);
        end = SystemClock.elapsedRealtime();
        LeoLog.i(TAG, "cost, iniSDK: " + (end - start));

        // init lock manager
        start = SystemClock.elapsedRealtime();
        LockManager lockManager = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);
        lockManager.init();
        end = SystemClock.elapsedRealtime();
        LeoLog.i(TAG, "cost, LockManager.getInstance.init: " + (end - start));

        AppMasterPreference preference = AppMasterPreference.getInstance(mApp);
        if (preference.getIsFirstInstallApp()) {
            SplashActivity.deleteImage();
            preference.setIsFirstInstallApp(false);
        }
        checkUpdateFinish();
        initSplashDelayTime();
        UIHelper.getInstance(mApp).mRandomCount = preference.getUnlockSucessRandom();

        PrivacyHelper.getInstance(mApp).caculateSecurityScore();

        start = SystemClock.elapsedRealtime();
        // init MobVista SDK here
        MobvistaEngine.getInstance(mApp);
        LeoLog.d(TAG, "MobvistaEngine init cost: " + (SystemClock.elapsedRealtime() - start));

        // start a protection JobScheduler service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TaskProtectService.scheduleService(mApp);
        }

        return true;
    }


    @Override
    public String getClassTag() {
        return TAG;
    }

    private void initImageLoader() {
        DisplayImageOptions options = new DisplayImageOptions.Builder().cacheOnDisk(true).build();
        ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(mApp);
        builder.taskExecutor(ThreadManager.getNetworkExecutor());
        builder.taskExecutorForCachedImages(ThreadManager.getAsyncExecutor());
        builder.threadPoolSize(Constants.MAX_THREAD_POOL_SIZE);
        builder.threadPriority(Thread.NORM_PRIORITY);
        builder.memoryCacheSizePercentage(8);
        builder.defaultDisplayImageOptions(options);
//        if (AppMasterConfig.LOGGABLE) {
//            builder.writeDebugLogs();
//        }
        builder.diskCacheSize(Constants.MAX_DISK_CACHE_SIZE); // 100 Mb
        builder.denyCacheImageMultipleSizesInMemory();
        ImageLoader.getInstance().init(builder.build());
    }

    private void registerPackageChangedBroadcast() {
        // Register intent receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        // 正在移动App是发出的广播
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        // 设备当前区域设置已更改是发出的广播
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        // recommend list change
        filter.addAction(AppLoadEngine.ACTION_RECOMMEND_LIST_CHANGE);
        mApp.registerReceiver(AppLoadEngine.getInstance(mApp), filter);
    }

    private void removeDeviceAdmin() {
        AppMasterApplication mApp = AppMasterApplication.getInstance();
        ComponentName component = new ComponentName(mApp, DeviceReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) mApp.getSystemService(Context.DEVICE_POLICY_SERVICE);
        dpm.removeActiveAdmin(component);
        LeoLog.i("stone_admin", "removeDeviceAdmin for first install");
    }

    private void checkUpdateFinish() {
        judgeLockAlert();
        AppMasterPreference pref = AppMasterPreference.getInstance(mApp);
        LeoPreference preferenceTable = LeoPreference.getInstance();
        LeoPreference leoPreference = LeoPreference.getInstance();
        String lastVercode = pref.getLastVersion();
        int versionCode = PhoneInfo.getVersionCode(mApp);
        LeoLog.i("value", "lastVercode=" + lastVercode);
        LeoLog.i("value", "versionCode=" + versionCode);
        if (TextUtils.isEmpty(lastVercode)) {
            //新安装用户，去除应用备份，应用卸载及隐私联系人相关的功能
            pref.setIsNeedCutBackupUninstallAndPrivacyContact(true);
            leoPreference.putBoolean(PrefConst.KEY_IS_OLD_USER, false);
            // first install
            // AM-2911: remove device administration at first install
            removeDeviceAdmin();
            if (versionCode == 34) {
                // remove unlock-all shortcut v2.1
                tryRemoveUnlockAllShortcut(mApp);
            } else if (versionCode >= 41) {
                installBoostShortcut();
            }
            pref.setIsUpdateQuickGestureUser(false);
            setUpdateTipData();
            /* 新用户保存引导号 */
            int currentGuideVersion = mApp.getResources().getInteger(
                    R.integer.guide_page_version);
            pref.setLastGuideVersion(currentGuideVersion);
        } else {
//            if(DeviceReceiverNewOne.isActive(AppMasterApplication.getInstance()) && (!pref.getHasAutoSwitch()) && (Integer.parseInt(lastVercode) < 70)) {
//                IntrudeSecurityManager m = (IntrudeSecurityManager)MgrContext.getManager(MgrContext.MGR_INTRUDE_SECURITY);
//                m.setSystIntruderProtectionSwitch(true);
//                pref.setHasAutoSwitch(true);
//            }

            int lastCode = Integer.parseInt(lastVercode);
            if (lastCode < versionCode) {
                // hit update
                if (versionCode == 34) {
                    // remove unlock-all shortcut v2.1
                    tryRemoveUnlockAllShortcut(mApp);
                } else if (versionCode == 41) {
                    installBoostShortcut();
                }
                int currentGuideVersion = mApp.getResources().getInteger(
                        R.integer.guide_page_version);
                int lastGuideVersion = pref.getLastGuideVersion();
                /* 升级是否需要显示引导页，需要手动配置：true-显示，false-不显示 */
                updateShowGuidePage(lastCode < 46 || (currentGuideVersion > lastGuideVersion && currentGuideVersion > 1));
                pref.setLastGuideVersion(currentGuideVersion);
                pref.setIsUpdateQuickGestureUser(true);
                // 每次升级都重新刷新googleplay提示规则
                uninitGooglePlayScorTip();
                recoveryUpdateTipDefaultData();
                if (lastCode < Utilities.LESS_THIRTY_VERSION_CODE) {
                    leoPreference.putBoolean(PrefConst.KEY_IS_OLD_USER, false);
                }
            }
        }
        pref.setLastVersion(String.valueOf(versionCode));
        tryRemoveUnlockAllShortcut(mApp);
    }

    /* case1对于老用户: 恢复“每次发现更新升级，恢复升级提示为默认值”的该方法是否执行的默认值 */
    private void recoveryUpdateTipDefaultData() {
        LeoLog.i(UIHelper.TAG, "重置‘是否恢复发现升级提示’标识的默认值");
        AppMasterPreference.getInstance(mApp)
                .setUpdateRecoveryDefaultData(false);
    }

    /**
     * case2对于新用户: 设置“每次发现更新升级，恢复升级提示为默认值”的该方法为已经执行true，
     * 这样不再去执行,因为新用户本来已经为默认值所以不用恢复数据
     */
    private void setUpdateTipData() {
        LeoLog.i(UIHelper.TAG, "设置‘是否恢复发现升级提示’标识的值为true");
        AppMasterPreference.getInstance(mApp)
                .setUpdateRecoveryDefaultData(true);
    }

    private void updateShowGuidePage(boolean flag) {
        AppMasterPreference.getInstance(mApp).setGuidePageFirstUse(flag);
    }

    private void uninitGooglePlayScorTip() {
        /* 解锁次数设置为初始状态 */
        AppMasterPreference.getInstance(mApp).setUnlockCount(0);
        /* googlepaly评分提示设置为初始状态 */
        AppMasterPreference.getInstance(mApp).setGoogleTipShowed(false);
    }

    private void tryRemoveUnlockAllShortcut(Context ctx) {
        if (!AppMasterPreference.getInstance(ctx).getRemoveUnlockAllShortcutFlag()) {
            // remove unlock all shortcut
            Intent shortcutIntent = new Intent(ctx, LockScreenActivity.class);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // 之前在创建快捷方式的时候，未加任何的action, 移除快捷方式时必须加Intent.ACTION_VIEW
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            shortcutIntent.putExtra("quick_lock_mode", true);
            shortcutIntent.putExtra("lock_mode_id", 0);
            shortcutIntent.putExtra("lock_mode_name", ctx.getString(R.string.unlock_all_mode));
            Intent shortcut = new Intent(
                    "com.android.launcher.action.UNINSTALL_SHORTCUT");
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, ctx.getString(R.string.unlock_all_mode));
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            shortcut.putExtra("duplicate", false);
            shortcut.putExtra("from_shortcut", true);
            ctx.sendBroadcast(shortcut);
            AppMasterPreference.getInstance(ctx).setRemoveUnlockAllShortcutFlag(true);
        }

    }

    private void installBoostShortcut() {
//        boolean isInstalllIswipe = ISwipUpdateRequestManager
//               .isInstallIsiwpe(AppMasterApplication.getInstance());
//        LeoPreference preferenceTable = LeoPreference.getInstance();
//        preferenceTable.putBoolean(PrefConst.IS_BOOST_CREAT, isInstalllIswipe);
//        if (!isInstalllIswipe) {
        Intent shortcutIntent = new Intent(mApp, HomeBoostActivity.class);
        ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(mApp,
                R.drawable.qh_speedup_icon);
        Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, mApp.getString(R.string.accelerate));
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
        shortcut.putExtra("duplicate", false);
        shortcut.putExtra("from_shortcut", true);
        mApp.sendBroadcast(shortcut);
//        }
    }

    private void judgeLockAlert() {
        AppMasterPreference pref = AppMasterPreference.getInstance(mApp);
        if (pref.isReminded()) {
            return;
        }
        Calendar calendar;
        Intent intent;
        AlarmManager am = (AlarmManager) mApp.getSystemService(Context.ALARM_SERVICE);
        if (!pref.getLastVersion().equals(PhoneInfo.getVersionCode(mApp))) { // is
            // new
            // version
            pref.setHaveEverAppLoaded(false);
            intent = new Intent(mApp, LockReceiver.class);
            intent.setAction(LockReceiver.ALARM_LOCK_ACTION);

            calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            pref.setLastAlarmSetTime(calendar.getTimeInMillis());
            calendar.add(Calendar.DATE, Constants.LOCK_TIP_INTERVAL_OF_DATE);
            PendingIntent pi = PendingIntent.getBroadcast(mApp, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        } else { // not new install
            calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            long detal = calendar.getTimeInMillis() - pref.getInstallTime();
            intent = new Intent(mApp, LockReceiver.class);
            intent.setAction(LockReceiver.ALARM_LOCK_ACTION);
            if (detal < Constants.LOCK_TIP_INTERVAL_OF_MS) {
                PendingIntent pi = PendingIntent.getBroadcast(mApp, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                        + Constants.LOCK_TIP_INTERVAL_OF_MS - detal, pi);
                pref.setLastAlarmSetTime(calendar.getTimeInMillis());
            } else {
                mApp.sendBroadcast(intent);
            }
        }
    }

    /* 初始化闪屏时间,需要在app启动时初始化 */
    private void initSplashDelayTime() {
        AppMasterApplication mApp = AppMasterApplication.getInstance();
        SplashBootstrap.mIsEmptyForSplashUrl = isEmptySplashUrl();
        /* 闪屏延时时间 */
        SplashBootstrap.mSplashDelayTime = AppMasterPreference.getInstance(mApp)
                .getSplashDelayTime();
    }

    /* 闪屏跳转连接是否为空：true-链接为空，false-链接不为空 */
    private boolean isEmptySplashUrl() {
        AppMasterApplication mApp = AppMasterApplication.getInstance();
        String splashSkipUrl = AppMasterPreference.getInstance(mApp).getSplashSkipUrl();
        String splashSkipToClient = AppMasterPreference.getInstance(mApp).getSplashSkipToClient();

        return TextUtils.isEmpty(splashSkipUrl) && TextUtils.isEmpty(splashSkipToClient);
    }

}
