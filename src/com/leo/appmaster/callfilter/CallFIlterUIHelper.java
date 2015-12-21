package com.leo.appmaster.callfilter;

import java.util.ArrayList;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.R;
import com.leo.appmaster.intruderprotection.IntruderprotectionActivity;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOChoiceDialog;
import com.leo.appmaster.ui.dialog.LEOWithSIngleCheckboxDialog;
import com.leo.appmaster.ui.dialog.MultiChoicesWitchSummaryDialog;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class CallFIlterUIHelper {
//    private static LEOWithSIngleCheckboxDialog mConfirmRemoveFromBlacklistDialog;
    private static CallFIlterUIHelper mInstance = null;

    public  LEOWithSIngleCheckboxDialog getConfirmRemoveFromBlacklistDialog(Context context) {
        LEOWithSIngleCheckboxDialog dialog = new LEOWithSIngleCheckboxDialog(context);
        dialog.setCheckboxText(context.getResources().
                getString(R.string.call_filter_remove_from_blacklist_checkbox_text));
        dialog.setTitle(context.getResources().
                getString(R.string.call_filter_remove_from_blacklist_checkbox_title));
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }
    
    public LEOChoiceDialog getCallHandleDialog(String title,Context context) {
        LEOChoiceDialog dialog = new LEOChoiceDialog(context);
        ArrayList<String> list = new ArrayList<String>();
        list.add(context.getResources().getString(R.string.call_filter_delete_record));
        list.add(context.getResources().getString(R.string.call_filter_remove_from_blacklist));
        list.add(context.getResources().getString(R.string.call_filter_mark));
        dialog.setNeedCheckbox(false);
        dialog.setTitle(title);
        dialog.setItemsWithDefaultStyle(list, 0);
        return dialog;
    }

    public MultiChoicesWitchSummaryDialog getCallHandleDialogWithSummary(String title,Context context) {
        MultiChoicesWitchSummaryDialog dialog = new MultiChoicesWitchSummaryDialog(context);
        dialog.setTitle(title);
        dialog.setContent(context.getResources().getString(R.string.call_filter_ask_add_to_blacklist));
        String[] itemContent = {context.getResources().getString(R.string.call_filter_mark_as_sr), 
                                                 context.getResources().getString(R.string.call_filter_mark_as_tx), 
                                                 context.getResources().getString(R.string.call_filter_mark_as_zp)};
        dialog.fillData(itemContent, 0);
        return dialog;
    }
//    public static Dialog getConfirmRemoveFromBlacklistDialog() {
//        return null;
//    }
    public static synchronized CallFIlterUIHelper getInstance() {
        if (mInstance == null) {
            mInstance = new CallFIlterUIHelper();
        }
        return mInstance;
    }
    
    public LEOAlarmDialog getConfirmClearAllRecordDialog(Context mContext) {
        LEOAlarmDialog dialog = new LEOAlarmDialog(mContext);
        dialog.setContentVisiable(false);
        dialog.setTitle("确定清空所有记录吗");
        dialog.setDialogIconVisibility(false);
        return dialog;
    }

    private CallFIlterUIHelper() {
    }
    
//    public void showReceiveCallNotification() {
//        Intent intent = new Intent(AppMasterApplication.getInstance(), IntruderprotectionActivity.class);
//        PendingIntent pendingIntent= PendingIntent.getActivity(AppMasterApplication.getInstance(), 1, intent, Notification.FLAG_AUTO_CANCEL);
//        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);  
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);  
//        mBuilder.setContentTitle("拦截到以下来电")
//        .setContentText("13510261550")
//        .setContentIntent(pendingIntent) //设置通知栏点击意图  
//        .setTicker("拦截到以下来电")
//        .setWhen(System.currentTimeMillis())  
//        .setPriority(Notification.PRIORITY_DEFAULT)
//        .setAutoCancel(true)
//        .setSmallIcon(R.drawable.ic_launcher);
//        mNotificationManager.notify(1, mBuilder.build());
//    }
    
}
