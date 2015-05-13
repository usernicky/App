
package com.leo.appmaster.privacycontact;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.util.Log;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.Constants;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.PrivacyDeletEditEvent;
import com.leo.appmaster.quickgestures.FloatWindowHelper;
import com.leo.appmaster.quickgestures.QuickGestureManager;

@SuppressLint("NewApi")
public class PrivacyMessageContentObserver extends ContentObserver {
    private Context mContext;
    public static String CALL_LOG_MODEL = "call_log_model";
    public static String MESSAGE_MODEL = "message_model";
    public static String CONTACT_MODEL = "contact_model";
    public static final String spaceString = "\u00A0";
    private String mFlag;

    public PrivacyMessageContentObserver(Context context, Handler handler, String flag) {
        super(handler);
        mContext = context;
        this.mFlag = flag;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        ContentResolver cr = mContext.getContentResolver();
        PrivacyContactManager pcm = PrivacyContactManager.getInstance(mContext);
        if (MESSAGE_MODEL.equals(mFlag)) {

            /*
             * 快捷手势未读短信提醒
             */
            List<MessageBean> messages = PrivacyContactUtils
                    .getSysMessage(mContext, cr,
                            "read=0 AND type=1", null, false);
            if (messages != null && messages.size() > 0) {
                QuickGestureManager.getInstance(mContext).mMessages = messages;
                FloatWindowHelper.isShowSysNoReadMessage = true;
                FloatWindowHelper.removeSwipWindow(mContext, 1);
                FloatWindowHelper.removeSwipWindow(mContext, -1);
            }
            /*
             * _________________________________________________
             */
            MessageBean mb = pcm.getLastMessage();
            if (mb != null) {
                String number = mb.getPhoneNumber();
                try {
                    cr.delete(
                            PrivacyContactUtils.SMS_INBOXS,
                            "address =  " + "\"" + number + "\" and " + "body = \""
                                    + mb.getMessageBody() + "\"", null);
                } catch (Exception e) {
                }
            }
            // 更新短信列表
            // else {
            // PrivacyContactManager.getInstance(mContext).updateSysMessage();
            // }
        } else if (CALL_LOG_MODEL.equals(mFlag)) {
            /*
             * 快捷手势未读通话记录提醒
             */
            String selection = Calls.TYPE + "=? and " + Calls.NEW + "=?";
            String[] selectionArgs = new String[] {
                    String.valueOf(Calls.MISSED_TYPE), String.valueOf(1)
            };
            List<ContactCallLog> callLogs = PrivacyContactUtils
                    .getSysCallLog(mContext,
                            mContext.getContentResolver(), selection,
                            selectionArgs);
            if (callLogs != null && callLogs.size() > 0) {
                QuickGestureManager.getInstance(mContext).mCallLogs = callLogs;
                FloatWindowHelper.isShowSysNoReadMessage = true;
                FloatWindowHelper.removeSwipWindow(mContext, 1);
                FloatWindowHelper.removeSwipWindow(mContext, -1);
            }
            /*
             * ------------------------------------------------------------------
             */
            ContactBean call = PrivacyContactManager.getInstance(mContext).getLastCall();
            if (call != null) {
                Cursor cursor = null;
                try {
                    String formatNumber = PrivacyContactUtils.formatePhoneNumber(call
                            .getContactNumber());
                    cursor = cr.query(PrivacyContactUtils.CALL_LOG_URI, null,
                            "number LIKE  ? ", new String[] {
                                "%" + formatNumber
                            }, null, null);
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            String number = cursor.getString(cursor.getColumnIndex("number"));
                            // String name =
                            // cursor.getString(cursor.getColumnIndex("name"));
                            String name = call.getContactName();
                            cursor.getString(cursor.getColumnIndex("duration"));
                            Date date = new Date(Long.parseLong(cursor.getString(cursor
                                    .getColumnIndex(CallLog.Calls.DATE))));
                            SimpleDateFormat sfd = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                            String time = sfd.format(date);
                            int type = (cursor
                                    .getInt(cursor.getColumnIndex(CallLog.Calls.TYPE)));
                            ContentValues values = new ContentValues();
                            values.put(Constants.COLUMN_CALL_LOG_PHONE_NUMBER, number);
                            if (!"".equals(name) && name != null) {
                                values.put(Constants.COLUMN_CALL_LOG_CONTACT_NAME, name);
                            } else {
                                if (call.getContactName() != null
                                        && !"".equals(call.getContactName())) {
                                    values.put(Constants.COLUMN_CALL_LOG_CONTACT_NAME,
                                            call.getContactName());
                                } else {
                                    values.put(Constants.COLUMN_CALL_LOG_CONTACT_NAME, number);
                                }
                            }
                            values.put(Constants.COLUMN_CALL_LOG_DATE, time);
                            values.put(Constants.COLUMN_CALL_LOG_TYPE, type);
                            if (call.getAnswerType() == 1) {
                                if (CallLog.Calls.OUTGOING_TYPE == type
                                        || CallLog.Calls.MISSED_TYPE != type) {
                                    values.put(Constants.COLUMN_CALL_LOG_IS_READ, 1);
                                } else {
                                    values.put(Constants.COLUMN_CALL_LOG_IS_READ, 0);
                                }
                            } else if (call.getAnswerType() == 0) {
                                if (CallLog.Calls.OUTGOING_TYPE == type) {
                                    values.put(Constants.COLUMN_CALL_LOG_IS_READ, 1);
                                } else {
                                    values.put(Constants.COLUMN_CALL_LOG_IS_READ, 0);
                                }
                            }
                            // 保存记录
                            cr.insert(Constants.PRIVACY_CALL_LOG_URI, values);
                            if (call.getAnswerType() == 1) {
                                if (CallLog.Calls.OUTGOING_TYPE != type) {
                                    if (CallLog.Calls.MISSED_TYPE == type) {
                                        AppMasterPreference pre = AppMasterPreference
                                                .getInstance(mContext);
                                        int count = pre.getMessageNoReadCount();
                                        if (count > 0) {
                                            pre.setCallLogNoReadCount(count + 1);
                                        } else {
                                            pre.setCallLogNoReadCount(1);
                                        }
                                        LeoEventBus
                                                .getDefaultBus()
                                                .post(
                                                        new PrivacyDeletEditEvent(
                                                                PrivacyContactUtils.PRIVACY_RECEIVER_CALL_LOG_NOTIFICATION));
                                    }
                                }
                            } else if (call.getAnswerType() == 0) {
                                if (CallLog.Calls.OUTGOING_TYPE != type) {
                                    AppMasterPreference pre = AppMasterPreference
                                            .getInstance(mContext);
                                    int count = pre.getMessageNoReadCount();
                                    if (count > 0) {
                                        pre.setCallLogNoReadCount(count + 1);
                                    } else {
                                        pre.setCallLogNoReadCount(1);
                                    }
                                    LeoEventBus
                                            .getDefaultBus()
                                            .post(
                                                    new PrivacyDeletEditEvent(
                                                            PrivacyContactUtils.PRIVACY_RECEIVER_CALL_LOG_NOTIFICATION));
                                }
                            }
                            // 通知更新通话记录
                            LeoEventBus
                                    .getDefaultBus()
                                    .post(
                                            new PrivacyDeletEditEvent(
                                                    PrivacyContactUtils.PRIVACY_ALL_CALL_NOTIFICATION_HANG_UP));
                            /**
                             * 删除系统记录
                             */
                            PrivacyContactUtils.deleteCallLogFromSystem("number LIKE ?",
                                    number,
                                    mContext);
                            // -------------------------------------------------------发送通知-------------------------------------------------
                            if (call.getAnswerType() == 1) {
                                if (CallLog.Calls.OUTGOING_TYPE != type) {
                                    if (CallLog.Calls.MISSED_TYPE == type) {
                                        new MessagePrivacyReceiver().callLogNotification(mContext);
                                    }
                                }
                            } else if (call.getAnswerType() == 0) {
                                if (CallLog.Calls.OUTGOING_TYPE != type) {
                                    new MessagePrivacyReceiver().callLogNotification(mContext);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } else {
                PrivacyContactManager.getInstance(mContext).updateSysCallLog();
            }
        } else if (CONTACT_MODEL.equals(mFlag)) {
            // PrivacyContactManager.getInstance(mContext).updateSysContact();
        }
    }
}
