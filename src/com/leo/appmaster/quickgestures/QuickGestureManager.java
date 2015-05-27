
package com.leo.appmaster.quickgestures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import android.R.integer;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.R;
import com.leo.appmaster.applocker.manager.LockManager;
import com.leo.appmaster.appmanage.business.AppBusinessManager;
import com.leo.appmaster.engine.AppLoadEngine;
import com.leo.appmaster.model.AppItemInfo;
import com.leo.appmaster.model.BaseInfo;
import com.leo.appmaster.model.BusinessItemInfo;
import com.leo.appmaster.privacycontact.ContactCallLog;
import com.leo.appmaster.privacycontact.MessageBean;
import com.leo.appmaster.quickgestures.model.QuickGestureContactTipInfo;
import com.leo.appmaster.quickgestures.model.QuickGsturebAppInfo;
import com.leo.appmaster.quickgestures.model.QuickSwitcherInfo;
import com.leo.appmaster.quickgestures.ui.QuickGestureFreeDisturbAppDialog;
import com.leo.appmaster.quickgestures.view.AppleWatchContainer;
import com.leo.appmaster.utils.LeoLog;

public class QuickGestureManager {
    public static final String TAG = "QuickGestureManager";

    private static Context mContext;
    private static QuickGestureManager mInstance;
    public TreeSet<AppLauncherRecorder> mAppLaunchRecorders;
    private static AppMasterPreference mSpSwitch;
    public List<MessageBean> mMessages;
    public List<ContactCallLog> mCallLogs;

    public List<BaseInfo> mDynamicList;
    public List<BaseInfo> mMostUsedList;
    private Drawable[] mEmptyIcon;

    private QuickGestureManager(Context ctx) {
        mContext = ctx.getApplicationContext();
        mSpSwitch = AppMasterPreference.getInstance(mContext);
    }

    public static synchronized QuickGestureManager getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new QuickGestureManager(ctx);
        }
        return mInstance;
    }

    public void init() {
        mDynamicList = new ArrayList<BaseInfo>();
        mMostUsedList = new ArrayList<BaseInfo>();
        loadAppLaunchReorder();
        preloadEmptyIcon();
        getSwitcherList();
    }

    public List<BaseInfo> getDynamicList() {
        Vector<BusinessItemInfo> businessDatas = AppBusinessManager.getInstance(mContext)
                .getBusinessData();
        List<BaseInfo> dynamicList = new ArrayList<BaseInfo>();
        BusinessItemInfo businessItem = null;
        if (businessDatas != null && businessDatas.size() > 0) {
            businessItem = businessDatas.get(0);
            dynamicList.add(businessItem);
        }
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RecentTaskInfo> recentTasks = am.getRecentTasks(50,
                ActivityManager.RECENT_WITH_EXCLUDED);
        String pkg;
        Drawable icon;
        AppItemInfo appInfo;
        AppLoadEngine engine = AppLoadEngine.getInstance(mContext);
        List<String> pkgs = new ArrayList<String>();
        for (RecentTaskInfo recentTaskInfo : recentTasks) {
            if (dynamicList.size() > 12)
                break;
            pkg = recentTaskInfo.baseIntent.getComponent().getPackageName();
            if (!pkgs.contains(pkg)) {
                pkgs.add(pkg);
                icon = engine.getAppIcon(pkg);
                if (icon != null) {
                    appInfo = new AppItemInfo();
                    appInfo.packageName = pkg;
                    appInfo.activityName = engine.getActivityName(pkg);
                    appInfo.icon = icon;
                    appInfo.label = engine.getAppName(pkg);
                    dynamicList.add(appInfo);
                }
            }
        }
        return dynamicList;
    }

    private void preloadEmptyIcon() {
        Resources res = mContext.getResources();
        mEmptyIcon = new Drawable[12];
        mEmptyIcon[0] = res.getDrawable(R.drawable.switch_orange);
        mEmptyIcon[1] = res.getDrawable(R.drawable.switch_green);
        mEmptyIcon[2] = res.getDrawable(R.drawable.seitch_purple);
        mEmptyIcon[3] = res.getDrawable(R.drawable.switch_red);
        mEmptyIcon[4] = res.getDrawable(R.drawable.switch_blue);
        mEmptyIcon[5] = res.getDrawable(R.drawable.switch_blue_2);
        mEmptyIcon[6] = res.getDrawable(R.drawable.switch_blue_3);
        mEmptyIcon[7] = res.getDrawable(R.drawable.switch_green_2);
        mEmptyIcon[8] = res.getDrawable(R.drawable.switch_orange_2);
        mEmptyIcon[9] = res.getDrawable(R.drawable.switch_purple_2);
        mEmptyIcon[10] = res.getDrawable(R.drawable.switch_red_2);
        mEmptyIcon[11] = res.getDrawable(R.drawable.switch_red_3);
    }

    public void stopFloatWindow() {
        LockManager.getInstatnce().stopFloatWindowService();
    }

    public void startFloatWindow() {
        LockManager.getInstatnce().startFloatWindowService();
    }

    public void loadAppLaunchReorder() {
        mAppLaunchRecorders = new TreeSet<QuickGestureManager.AppLauncherRecorder>();
        String recoders = AppMasterPreference.getInstance(mContext).getAppLaunchRecoder();
        AppLauncherRecorder temp = null;
        int sIndex = -1;
        if (!TextUtils.isEmpty(recoders)) {
            recoders = recoders.substring(0, recoders.length() - 1);
            String[] recoderList = recoders.split(";");
            for (String recoder : recoderList) {
                sIndex = recoder.indexOf(':');
                if (sIndex != -1) {
                    temp = new AppLauncherRecorder();
                    temp.pkg = recoder.substring(0, sIndex);
                    temp.launchCount = Integer.parseInt(recoder.substring(sIndex + 1));
                }
            }
        }
    }

    public void recordAppLaunch(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        boolean hit = false;
        for (AppLauncherRecorder recorder : mAppLaunchRecorders) {
            if (recorder.pkg.equals(pkg)) {
                recorder.launchCount++;
                hit = true;
                break;
            }
        }
        if (!hit) {
            AppLauncherRecorder recoder = new AppLauncherRecorder();
            recoder.pkg = pkg;
            recoder.launchCount = 1;
            mAppLaunchRecorders.add(recoder);
        }
        saveAppLaunchRecoder();
    }

    public void saveAppLaunchRecoder() {
        StringBuilder resault = new StringBuilder();
        for (AppLauncherRecorder recorder : mAppLaunchRecorders) {
            resault.append(recorder.pkg).append(':').append(recorder.launchCount).append(';');
        }
        AppMasterPreference.getInstance(mContext).setAppLaunchRecoder(resault.toString());
    }

    public void removeAppLaunchRecoder(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        AppLauncherRecorder hitRecoder = null;
        for (AppLauncherRecorder recorder : mAppLaunchRecorders) {
            if (recorder.pkg.equals(pkg)) {
                hitRecoder = recorder;
                break;
            }
        }
        if (hitRecoder != null) {
            mAppLaunchRecorders.remove(hitRecoder);
            StringBuilder resault = new StringBuilder();
            for (AppLauncherRecorder recorder : mAppLaunchRecorders) {
                resault.append(recorder.pkg).append(':').append(recorder.launchCount).append(';');
            }
            AppMasterPreference.getInstance(mContext).setAppLaunchRecoder(resault.toString());
        }
    }

    public List<BaseInfo> getMostUsedList() {
        if (mSpSwitch.getQuickGestureCommonAppDialogCheckboxValue()) {
            return loadRecorderAppInfo();
        } else {
            return loadCommonAppInfo();
        }
    }

    
    // Recorder App
    private List<BaseInfo> loadRecorderAppInfo() {
        List<BaseInfo> resault = new ArrayList<BaseInfo>();
        TreeSet<AppLauncherRecorder> recorderApp = QuickGestureManager
                .getInstance(mContext).mAppLaunchRecorders;
        AppLoadEngine engin = AppLoadEngine.getInstance(mContext);
        Iterator<AppLauncherRecorder> recorder = recorderApp.descendingIterator();
        int i = 0;
        AppItemInfo info;
        QuickGsturebAppInfo temp = null;
        while (recorder.hasNext()) {
            AppLauncherRecorder recorderAppInfo = recorder.next();
            info = engin.getAppInfo(recorderAppInfo.pkg);
            if (i >= 13) {
                break;
            } else {
                temp = new QuickGsturebAppInfo();
                temp.packageName = info.packageName;
                temp.activityName = info.activityName;
                temp.label = info.label;
                temp.icon = info.icon;
                temp.gesturePosition = i;
                resault.add(temp);
            }
        }

        return resault;
    }

    // Customize common app
    private List<BaseInfo> loadCommonAppInfo() {
        
        List<BaseInfo> resault = new ArrayList<BaseInfo>();
        List<QuickGsturebAppInfo> packageNames = new ArrayList<QuickGsturebAppInfo>();
        AppLoadEngine engin = AppLoadEngine.getInstance(mContext);
        String commonAppString = mSpSwitch.getCommonAppPackageName();
        if (!TextUtils.isEmpty(commonAppString)) {
            String[] names = commonAppString.split(";");
            QuickGsturebAppInfo temp = null;
            int sIndex = -1;
            for (String recoder : names) {
                sIndex = recoder.indexOf(':');
                if (sIndex != -1) {
                    temp = new QuickGsturebAppInfo();
                    temp.packageName = recoder.substring(0, sIndex);
                    temp.gesturePosition = Integer.parseInt(recoder.substring(sIndex + 1));
                    packageNames.add(temp);
                }
            }

            AppItemInfo info;
            for (QuickGsturebAppInfo appItemInfo : packageNames) {
                if (resault.size() >= 13) {
                    break;
                }
                info = engin.getAppInfo(appItemInfo.packageName);
                if (info != null) {
                    appItemInfo.packageName = info.packageName;
                    appItemInfo.activityName = info.activityName;
                    appItemInfo.label = info.label;
                    appItemInfo.icon = info.icon;
                    resault.add(appItemInfo);
                }
            }
        }

        return resault;
    }

    public List<BaseInfo> getSwitcherList() {
        QuickSwitchManager.getInstance(mContext).getAllList();
        return QuickSwitchManager.getInstance(mContext).getSwitchList(13);
    }

    public void updateSwitcherData(List<BaseInfo> infos) {
        String saveToSp = QuickSwitchManager.getInstance(mContext)
                .listToString(infos, infos.size());
        LeoLog.d("updateSwitcherData", "saveToSp:" + saveToSp);
        mSpSwitch.setSwitchList(saveToSp);
        mSpSwitch.setSwitchListSize(infos.size());
        QuickSwitchManager.getInstance(mContext).onDataChange(saveToSp);
    }

    public void onRunningPkgChanged(String pkg) {

    }

    public void checkEventItemRemoved(BaseInfo info) {
        if (info instanceof MessageBean) {
            MessageBean bean = (MessageBean) info;
            if (mMessages != null && mMessages.size() > 0) {
                mMessages.remove(bean);
            }
        } else if (info instanceof ContactCallLog) {
            ContactCallLog callLog = (ContactCallLog) info;
            if (mCallLogs != null && mCallLogs.size() > 0) {
                mCallLogs.remove(callLog);
            }
        } else if (info instanceof QuickGestureContactTipInfo) {
            if (LockManager.getInstatnce().isShowPrivacyCallLog) {
                LockManager.getInstatnce().isShowPrivacyCallLog = false;
            }
            if (LockManager.getInstatnce().isShowPrivacyMsm) {
                LockManager.getInstatnce().isShowPrivacyMsm = false;
            }
        }

    }

    public List<String> getFreeDisturbAppName() {
        List<String> packageNames = new ArrayList<String>();
        String packageName = AppMasterPreference.getInstance(mContext)
                .getFreeDisturbAppPackageName();
        if (!AppMasterPreference.PREF_QUICK_GESTURE_FREE_DISTURB_APP_PACKAGE_NAME
                .equals(packageName)) {
            String[] names = packageName.split(";");
            packageNames = Arrays.asList(names);
        }
        return packageNames;
    }

    // 获取未读短信数量
    public int getNoReadMsg() {
        int noReadMsgCount = 0;
        Cursor c = null;
        try {
            Uri uri = Uri.parse("content://sms");
            c = mContext.getContentResolver().query(uri, null,
                    "read=0 AND type=1", null, null);
            if (c != null) {
                noReadMsgCount = c.getCount();
                c.close();
                c = null;
            }
            uri = Uri.parse("content://mms");
            c = mContext.getContentResolver().query(uri, null,
                    "read=0 AND m_type=132", null, null);

            if (c != null) {
                noReadMsgCount += c.getCount();
                c.close();
                c = null;
            }
        } catch (Exception e) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return noReadMsgCount;
    }

    // 获取未读通话
    public int getMissedCallCount() {
        int missedCallCount = 0;
        Cursor c = null;
        try {
            String selection = Calls.TYPE + "=? and " + Calls.NEW + "=?";
            String[] selectionArgs = new String[] {
                    String.valueOf(Calls.MISSED_TYPE), String.valueOf(1)
            };

            c = mContext.getContentResolver().query(Calls.CONTENT_URI,
                    new String[] {
                            Calls.NUMBER, Calls.TYPE, Calls.NEW
                    },
                    selection, selectionArgs, Calls.DEFAULT_SORT_ORDER);

            if (c != null) {
                missedCallCount = c.getCount();
            }
        } catch (Exception e) {
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return missedCallCount;
    }

    public Drawable applyEmptyIcon() {
        Drawable icon = null;
        int index = (int) (Math.random() * 12);
        icon = mEmptyIcon[index];
        return icon;
    }

    /**
     * Common App Dialog
     * 
     * @param context
     */
    public void showCommontAppDialog(final Activity activity) {
        final QuickGestureFreeDisturbAppDialog commonApp = new QuickGestureFreeDisturbAppDialog(
                activity.getApplicationContext(), 3);
        final AppMasterPreference pref = AppMasterPreference.getInstance(activity);
        commonApp.setIsShowCheckBox(true);
        commonApp.setCheckBoxText(R.string.quick_gesture_change_common_app_dialog_checkbox_text);
        // 设置是否选择习惯
        commonApp.setCheckValue(pref.getQuickGestureCommonAppDialogCheckboxValue());
        commonApp.setTitle(R.string.quick_gesture_change_common_app_dialog_title);
        commonApp.setRightBt(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // 确认按钮
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 添加的应用包名
                        List<BaseInfo> addCommonApp = commonApp.getAddFreePackageName();
                        // 移除的应用包名
                        List<BaseInfo> removeCommonApp = commonApp.getRemoveFreePackageName();
                        // 是否选择使用习惯自动填充
                        boolean flag = commonApp.getCheckValue();
                        if (addCommonApp != null && addCommonApp.size() > 0) {
                            for (BaseInfo info : addCommonApp) {
                                QuickGsturebAppInfo string = (QuickGsturebAppInfo) info;
                                pref.setCommonAppPackageNameAdd(string.packageName + ":"
                                        + string.gesturePosition);
                            }
                        }
                        if (removeCommonApp != null && removeCommonApp.size() > 0) {
                            for (BaseInfo info : removeCommonApp) {
                                QuickGsturebAppInfo string = (QuickGsturebAppInfo) info;
                                pref.setCommonAppPackageNameRemove(string.packageName + ":"
                                        + string.gesturePosition);
                            }
                        }
                        if (pref.getQuickGestureCommonAppDialogCheckboxValue() != flag) {
                            pref.setQuickGestureCommonAppDialogCheckboxValue(flag);
                        }
                    }
                }).start();
                commonApp.dismiss();
            }
        });
        commonApp.setLeftBt(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // 取消按钮
                commonApp.dismiss();
            }
        });

        commonApp.getWindow().setType(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        commonApp.show();
        activity.finish();
    }

    /**
     * Quick Switch Dialog
     * 
     * @param mSwitchList
     * @param context
     * @param mContainer
     */
    public void showQuickSwitchDialog(final Context context,
            final AppleWatchContainer mContainer) {
        final QuickGestureFreeDisturbAppDialog quickSwitch = new QuickGestureFreeDisturbAppDialog(
                context.getApplicationContext(), 2);
        quickSwitch.setTitle(R.string.pg_appmanager_quick_switch_dialog_title);
        quickSwitch.setRightBt(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // 确认按钮
                AppMasterApplication.getInstance().postInAppThreadPool(new
                        Runnable() {
                            @Override
                            public void run() {
                                // 添加的应用包名
                                boolean addSwitch = false;
                                boolean removeSwitch = false;
                                List<BaseInfo> addQuickSwitch = quickSwitch.getAddFreePackageName();
                                // 移除的应用包名
                                List<BaseInfo> removeQuickSwitch = quickSwitch
                                        .getRemoveFreePackageName();
                                // 正确添加或删除的list
                                String mListString = mSpSwitch.getSwitchList();
                                List<BaseInfo> rightQuickSwitch =
                                        QuickSwitchManager.getInstance(mContext).StringToList(
                                                mListString);
                                List<BaseInfo> mDefineList = new ArrayList<BaseInfo>();
                                LeoLog.d("QuickGestureManager", "rightQuickSwitch.size is : "
                                        + rightQuickSwitch.size());
                                if (addQuickSwitch != null && addQuickSwitch.size() > 0) {
                                    String saveToSp = QuickSwitchManager.getInstance(context)
                                            .listToString(
                                                    addQuickSwitch, addQuickSwitch.size());
                                    LeoLog.d("QuickGestureManager", "saveToSp is : " + saveToSp);
                                    addSwitch = true;
                                }
                                if (removeQuickSwitch != null && removeQuickSwitch.size() > 0) {
                                    String removeToSp = QuickSwitchManager.getInstance(context)
                                            .listToString(removeQuickSwitch,
                                                    removeQuickSwitch.size());
                                    LeoLog.d("QuickGestureManager", "removeToSp is : " + removeToSp);
                                    for (int i = 0; i < rightQuickSwitch.size(); i++) {
                                        boolean isHasSameName = false;
                                        BaseInfo nInfo = rightQuickSwitch.get(i);
                                        String rightName = nInfo.label;
                                        for (int j = 0; j < removeQuickSwitch.size(); j++) {
                                            BaseInfo mInfo = removeQuickSwitch.get(j);
                                            String removeName = mInfo.label;
                                            if (rightName.equals(removeName)) {
                                                isHasSameName = true;
                                            }
                                        }
                                        if (!isHasSameName) {
                                            mDefineList.add(nInfo);
                                        }
                                    }
                                    removeSwitch = true;
                                }

                                if (addSwitch) {
                                    if (!removeSwitch) {
                                        mDefineList = rightQuickSwitch;
                                    }
                                    // 记录现有的Icon位置
                                    LeoLog.d("QuickGestureManager", "有货要加");
                                    List<BaseInfo> mfixPostionList = new ArrayList<BaseInfo>();
                                    List<Integer> sPosition = new ArrayList<Integer>();
                                    for (int i = 0; i < mDefineList.size(); i++) {
                                        sPosition.add(mDefineList.get(i).gesturePosition);
                                        LeoLog.d("QuickGestureManager",
                                                "货的位置 :" + mDefineList.get(i).gesturePosition);
                                    }

                                    int k = 0;
                                    for (int i = 0; i < 13; i++) {
                                        boolean isHasIcon = false;
                                        for (int j = 0; j < mDefineList.size(); j++) {
                                            if (i == mDefineList.get(j).gesturePosition) {
                                                isHasIcon = true;
                                            }
                                        }

                                        if (!isHasIcon && addQuickSwitch.size() > k) {
                                            LeoLog.d("QuickGestureManager", i + "号位没人坐，收藏起来");
                                            BaseInfo mInfo = addQuickSwitch.get(k);
                                            mInfo.gesturePosition = i;
                                            mfixPostionList.add(mInfo);
                                            k++;
                                        }
                                    }

                                    LeoLog.d("QuickGestureManager",
                                            "收藏完毕，霸占了" + mfixPostionList.size() + "个位置");
                                    for (int i = 0; i < mfixPostionList.size(); i++) {
                                        BaseInfo mInfo = mfixPostionList.get(i);
                                        mDefineList.add(mInfo);
                                        LeoLog.d("QuickGestureManager", "霸占了 "
                                                + mInfo.gesturePosition + "号位");
                                    }
                                }

                                if (!addSwitch && !removeSwitch) {
                                    mDefineList = rightQuickSwitch;
                                }

                                String mChangeList = QuickSwitchManager.getInstance(mContext)
                                        .listToString(mDefineList, mDefineList.size());
                                LeoLog.d("QuickGestureManager", "mChangeList ： " + mChangeList);
                                mSpSwitch.setSwitchList(mChangeList);
                            }
                        });
                quickSwitch.dismiss();
            }
        });
        quickSwitch.setLeftBt(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // 取消按钮
                quickSwitch.dismiss();
            }
        });
        quickSwitch.getWindow().setType(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        quickSwitch.show();
    }

    public class AppLauncherRecorder implements Comparable<AppLauncherRecorder> {
        public String pkg;
        public int launchCount;

        @Override
        public int compareTo(AppLauncherRecorder another) {
            if (launchCount > another.launchCount) {
                return 1;
            } else if (launchCount == another.launchCount) {
                return 0;
            } else {
                return -1;
            }
        }
    }

}
