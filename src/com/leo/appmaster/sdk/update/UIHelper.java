
package com.leo.appmaster.sdk.update;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.leo.analytics.update.IUIHelper;
import com.leo.analytics.update.UpdateManager;
import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.R;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.NotificationUtil;

public class UIHelper implements IUIHelper {

    private final static String TAG = UIHelper.class.getSimpleName();

    private static UIHelper sUIHelper = null;
    private Context mContext = null;
    private UpdateManager mManager = null;
    private OnStateChangeListener listener = null;

    private NotificationManager nm = null;
    // private RemoteViews updateRv = null;
//    private RemoteViews downloadRv = null;
    private Notification updateNotification = null;
    private Notification downloadNotification = null;

    /* modify these constants in different application */
    public final static String ACTION_NEED_UPDATE = "com.leo.appmaster.update";
    public final static String ACTION_CANCEL_UPDATE = "com.leo.appmaster.update.cancel";
    public final static String ACTION_DOWNLOADING = "com.leo.appmaster.download";
    public final static String ACTION_CANCEL_DOWNLOAD = "com.leo.appmaster.download.cancel";
    public final static String ACTION_DOWNLOAD_FAILED = "com.leo.appmaster.download.failed";
    public final static String ACTION_DOWNLOAD_FAILED_CANCEL = "com.leo.appmaster.download.failed.dismiss";

    private final static int DOWNLOAD_NOTIFICATION_ID = 1001;
    private final static int UPDATE_NOTIFICATION_ID = 1002;
    private final static int DOWNLOAD_FAILED_NOTIFICATION_ID = 1003;

    private int mUIType = IUIHelper.TYPE_CHECKING;
    private int mUIParam = 0;
    private int mProgress = 0;

    private UIHelper(Context ctx) {
        mContext = ctx;
        /* new version found */
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEED_UPDATE);
        mContext.registerReceiver(receive, filter);
        /* for cancel update */
        filter = new IntentFilter();
        filter.addAction(ACTION_CANCEL_UPDATE);
        mContext.registerReceiver(receive, filter);
        /* show downloading diaLeoLog */
        filter = new IntentFilter();
        filter.addAction(ACTION_DOWNLOADING);
        mContext.registerReceiver(receive, filter);
        /* for cancel download */
        filter = new IntentFilter();
        filter.addAction(ACTION_CANCEL_DOWNLOAD);
        mContext.registerReceiver(receive, filter);
        /* for cancel download */
        filter = new IntentFilter();
        filter.addAction(ACTION_DOWNLOAD_FAILED);
        mContext.registerReceiver(receive, filter);
        /* for cancel download */
        filter = new IntentFilter();
        filter.addAction(ACTION_DOWNLOAD_FAILED_CANCEL);
        mContext.registerReceiver(receive, filter);
    }

    public static UIHelper getInstance(Context ctx) {
        if (sUIHelper == null) {
            sUIHelper = new UIHelper(ctx);
        }
        return sUIHelper;
    }

    /* all function needs UpdateManager have to invoke after this call */
    public void setManager(UpdateManager manager) {
        mManager = manager;
        nm = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // buildUpdatedNotification();
        buildDownloadNotification();
    }

    public void setOnProgressListener(OnStateChangeListener l) {
        listener = l;
    }

    @SuppressWarnings("deprecation")
    private void buildDownloadNotification() {
        String appName = mContext.getString(R.string.app_name);
        String downloadTip = mContext.getString(R.string.downloading, appName);
        CharSequence from = appName;
        CharSequence message = downloadTip;
//        downloadRv = new RemoteViews(mContext.getPackageName(),
//                R.layout.sdk_notification_download);
//        downloadRv.setTextViewText(R.id.tv_content, downloadTip);
        Intent intent = new Intent(ACTION_DOWNLOADING);
        PendingIntent contentIntent = PendingIntent.getBroadcast(mContext, 0,
                intent, 0);
        // go back to app - begin
        // Intent intent = new Intent(mContext, SDKUpdateActivity.class);
        // ComponentName componentName = new ComponentName(
        // mContext.getPackageName(), SDKUpdateActivity.class.getName());
        // intent.setComponent(componentName);
        // intent.setAction("android.intent.action.MAIN");
        // intent.addCategory("android.intent.category.LAUNCHER");
        // intent.addFlags(Notification.FLAG_ONGOING_EVENT);
        // PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
        // intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // go back to app - end
        downloadNotification = new Notification(
                R.drawable.ic_launcher_notification, downloadTip,
                System.currentTimeMillis());
        downloadNotification.setLatestEventInfo(mContext, from, message,
                contentIntent);
        NotificationUtil.setBigIcon(downloadNotification, R.drawable.ic_launcher_notification_big);

        downloadNotification.flags = Notification.FLAG_AUTO_CANCEL
                | Notification.FLAG_ONGOING_EVENT;
    }

    public void sendDownloadNotification(int progress) {
        LeoLog.d(TAG, "sendDownloadNotification called ");
        String appName = mContext.getString(R.string.app_name);
//        downloadRv.setProgressBar(R.id.pb_download, 100, progress, false);
//        downloadRv.setTextViewText(
//                R.id.tv_content,
//                mContext.getString(R.string.downloading_notification, appName,
//                        progress) + "%");
        // downloadRv.setTextViewText(R.id.tv_progress, progress + "%");
//        downloadNotification.contentView = downloadRv;
        String title = mContext.getString(R.string.downloading, appName);
        String content = progress +  "%";
        downloadNotification.setLatestEventInfo(mContext, title, content,  downloadNotification.contentIntent);
        NotificationUtil.setBigIcon(downloadNotification, R.drawable.ic_launcher_notification_big);

        nm.notify(DOWNLOAD_NOTIFICATION_ID, downloadNotification);
    }

    public void cancelDownloadNotification() {
        nm.cancel(DOWNLOAD_NOTIFICATION_ID);
    }

    @SuppressWarnings("deprecation")
    private void sendUpdateNotification() {
        String appName = mContext.getString(R.string.app_name);
        String updateTip = mContext.getString(R.string.update_available,
                appName);
        Intent intent = new Intent(ACTION_NEED_UPDATE);
        PendingIntent contentIntent = PendingIntent.getBroadcast(mContext, 0,
                intent, 0);
        // go back to app - begin
        // Intent intent = new Intent(mContext, SDKUpdateActivity.class);
        // ComponentName componentName = new ComponentName(
        // mContext.getPackageName(), SDKUpdateActivity.class.getName());
        // intent.setComponent(componentName);
        // intent.setAction("android.intent.action.MAIN");
        // intent.addCategory("android.intent.category.LAUNCHER");
        // intent.addFlags(Notification.FLAG_ONGOING_EVENT);
        // PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
        // intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // go back to app - end

        updateNotification = new Notification(
                R.drawable.ic_launcher_notification, updateTip,
                System.currentTimeMillis());
        Intent dIntent = new Intent(UIHelper.ACTION_CANCEL_UPDATE);
        PendingIntent delIntent = PendingIntent.getBroadcast(mContext, 0,
                dIntent, 0);
        updateNotification.deleteIntent = delIntent;
        String contentText = mContext.getString(R.string.version_found,
                mManager.getVersion());
        updateNotification.setLatestEventInfo(mContext, updateTip, contentText,
                contentIntent);
        NotificationUtil.setBigIcon(updateNotification, R.drawable.ic_launcher_notification_big);

        updateNotification.flags = Notification.FLAG_AUTO_CANCEL
                | Notification.FLAG_ONGOING_EVENT;
        nm.notify(UPDATE_NOTIFICATION_ID, updateNotification);
    }

    public void cancelUpdateNotification() {
        nm.cancel(UPDATE_NOTIFICATION_ID);
    }

    private void sendDownloadFailedNotification() {
        String appName = mContext.getString(R.string.app_name);
        String failedTip = mContext.getString(R.string.download_error);
        Intent intent = new Intent(ACTION_DOWNLOAD_FAILED);
        PendingIntent contentIntent = PendingIntent.getBroadcast(mContext, 0,
                intent, 0);
        updateNotification = new Notification(
                R.drawable.ic_launcher_notification, failedTip,
                System.currentTimeMillis());
        Intent dIntent = new Intent(ACTION_DOWNLOAD_FAILED_CANCEL);
        PendingIntent delIntent = PendingIntent.getBroadcast(mContext, 0,
                dIntent, 0);
        updateNotification.deleteIntent = delIntent;
        String contentText = mContext.getString(R.string.version_found,
                mManager.getVersion());
        updateNotification.setLatestEventInfo(mContext, appName, failedTip,
                contentIntent);
        NotificationUtil.setBigIcon(updateNotification, R.drawable.ic_launcher_notification_big);

        updateNotification.flags = Notification.FLAG_AUTO_CANCEL
                | Notification.FLAG_ONGOING_EVENT;
        nm.cancel(DOWNLOAD_NOTIFICATION_ID);
        nm.notify(DOWNLOAD_FAILED_NOTIFICATION_ID, updateNotification);
    }

    public void cancelDownloadFailedNotification() {
        nm.cancel(DOWNLOAD_FAILED_NOTIFICATION_ID);
    }

    @Override
    public void onNewState(int ui_type, int param) {
        mUIType = ui_type;
        mUIParam = param;
        if (ui_type == IUIHelper.TYPE_DOWNLOAD_DONE && param == UpdateManager.FORCE_UPDATE) {
            AppMasterApplication.getInstance().exitApplication();
        }
        if (ui_type == IUIHelper.TYPE_CHECK_NEED_UPDATE
                && !isRunningForeground(mContext)) {
            sendUpdateNotification();
        } else {
            showUI(ui_type, param);
        }
    }

    private boolean isActivityOnTop(Context context) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        if (cn.getClassName().equals(UpdateActivity.class.getName())) {
            return true;
        }
        return false;
    }

    private boolean isAppOnTop(Context context) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if (!TextUtils.isEmpty(currentPackageName)
                && currentPackageName.equals(context.getPackageName())) {
            return true;
        }

        return false;
    }

    private boolean isRunningForeground(Context context) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if (!TextUtils.isEmpty(currentPackageName)
                && currentPackageName.equals(context.getPackageName())) {
            return true;
        }

        return false;
    }

    public int getProgress() {
        return mProgress;
    }

    public int getComplete() {
        return mManager.getCurrentCompleteSzie();
    }

    public int getTotal() {
        return mManager.getTotalSize();
    }

    @Override
    public void onProgress(int complete, int total) {
        long c = complete;
        long t = total;
        mProgress = (total == 0) ? 0 : (int) (c * 100 / t);
        if (mProgress == 100) {
            cancelDownloadNotification();
            if (listener != null) {
                listener.onChangeState(TYPE_DISMISS, 0);
            }
            return;
        }/* download done */
        if (!isActivityOnTop(mContext)) {
            LeoLog.d(TAG, "sendDownloadNotification in onProgress of UIHelper");
            sendDownloadNotification(mProgress);
        } else {
            cancelDownloadNotification();
        }
        if (listener != null) {
            listener.onProgress(complete, total);
        }
    }

    private void showUI(int type, int param) {
        LeoLog.d(TAG, "type=" + type + "; param=" + param);
        if (isActivityOnTop(mContext) && listener != null) {
            LeoLog.d(TAG, "activity on top");
            listener.onChangeState(type, param);
        } else if (isAppOnTop(mContext)) {
            relaunchActivity(type, param);
        } else {
            showNotification(type);
        }
    }

    private void showNotification(int type) {
        switch (type) {
            case IUIHelper.TYPE_CHECK_NEED_UPDATE:
                sendUpdateNotification();
                break;
            case IUIHelper.TYPE_DOWNLOAD_FAILED:
                sendDownloadFailedNotification();
                break;
        }
    }

    private void relaunchActivity(int type, int param) {
        Intent i = new Intent();
        i.setClass(mContext, UpdateActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra(LAYOUT_TYPE, type);
        i.putExtra(LAYOUT_PARAM, param);
        mContext.startActivity(i);
    }

    BroadcastReceiver receive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LeoLog.d(TAG, "onReceive action =" + action);
            try {
                if (action.equals(ACTION_NEED_UPDATE)) {
                    nm.cancel(UPDATE_NOTIFICATION_ID);
                    LeoLog.d(TAG, "recevie UPDATE_NOTIFICATION_ID");
                    relaunchActivity(IUIHelper.TYPE_UPDATE,
                            mManager.getReleaseType());
                } else if (action.equals(ACTION_CANCEL_UPDATE)) {
                    mManager.onCancelUpdate();
                    if (listener != null) {
                        listener.onChangeState(TYPE_DISMISS, 0);
                    }
                } else if (action.equals(ACTION_DOWNLOADING)) {
                    LeoLog.d(TAG, "recevie UPDATE_NOTIFICATION_ID");
                    relaunchActivity(IUIHelper.TYPE_DOWNLOADING,
                            mManager.getReleaseType());
                } else if (action.equals(ACTION_CANCEL_DOWNLOAD)) {
                    mManager.onCancelDownload();
                    if (listener != null) {
                        listener.onChangeState(TYPE_DISMISS, 0);
                    }
                } else if (action.equals(ACTION_DOWNLOAD_FAILED)) {
                    relaunchActivity(IUIHelper.TYPE_DOWNLOAD_FAILED, 0);
                } else if (action.equals(ACTION_DOWNLOAD_FAILED_CANCEL)) {
                    mManager.onCancelDownload();
                    if (listener != null) {
                        listener.onChangeState(TYPE_DISMISS, 0);
                    }
                }
            } catch (NullPointerException e) {
                // there's a situation that the application is killed by
                // notification still alive.
                // Nullpointer exception will happen in this case ,do nothing
                // when this happened
                e.printStackTrace();
            }
        }
    };

    @Override
    public int getLayoutType() {
        return mUIType;
    }

    @Override
    public int getLayoutParam() {
        return mUIParam;
    }

    @Override
    public void onBusy() {
        // String appname = mContext.getString(R.string.app_name);
        // Toast.makeText(mContext,
        // mContext.getString(R.string.downloading, appname),
        // Toast.LENGTH_SHORT).show();
        /* show UI corresponding to current state of download manager */
        showUI(mUIType, mUIParam);
    }

    @Override
    public void onUpdateChannel(int channel) {
        if (listener != null) {
            listener.onNotifyUpdateChannel(channel);
        }
    }

}
