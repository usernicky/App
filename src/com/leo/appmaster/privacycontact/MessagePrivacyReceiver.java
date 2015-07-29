
package com.leo.appmaster.privacycontact;

import java.text.SimpleDateFormat;
import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.PrivacyEditFloatEvent;
import com.leo.appmaster.quickgestures.FloatWindowHelper;
import com.leo.appmaster.quickgestures.QuickGestureManager;
import com.leo.appmaster.quickgestures.ui.QuickGestureActivity;
import com.leo.appmaster.utils.BuildProperties;
import com.leo.appmaster.utils.NotificationUtil;
import com.leo.appmaster.utils.Utilities;

public class MessagePrivacyReceiver extends BroadcastReceiver {
    private ITelephony mITelephony;
    private AudioManager mAudioManager;
    private int mAnswer = 1;
    private String mPhoneNumber, mMessgeBody;
    private long mSendDate;
    private Context mContext;
    private SimpleDateFormat mSimpleDateFormate;

    public MessagePrivacyReceiver() {
    }

    public MessagePrivacyReceiver(ITelephony itelephony, AudioManager audioManager) {
        this.mITelephony = itelephony;
        this.mAudioManager = audioManager;
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        // 测试
        printTestReceiverLog(intent);
        String action = intent.getAction();
        mContext = context;
        if (mSimpleDateFormate == null) {
            mSimpleDateFormate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        }
        // 拦截信息
        if (action.equals(PrivacyContactUtils.MESSAGE_RECEIVER_ACTION)
                || action.equals(PrivacyContactUtils.MESSAGE_RECEIVER_ACTION2)
                || action.equals(PrivacyContactUtils.MESSAGE_RECEIVER_ACTION3)) {
            PrivacyContactManager.getInstance(mContext).testValue = true;
            /*
             * 有新短信来时恢复该短信是否经过红点提示标记的默认值false
             */
            if (QuickGestureManager.getInstance(mContext).isMessageReadRedTip) {
                QuickGestureManager.getInstance(mContext).isMessageReadRedTip = false;
                AppMasterPreference.getInstance(mContext).setMessageIsRedTip(false);
            }
            if (PrivacyContactManager.getInstance(context).getPrivacyContactsCount() == 0) {
                return;
            }
            // Crash from feedback
            try {
                Bundle bundle = intent.getExtras();
                Object[] pdus = (Object[]) bundle.get("pdus");
                for (Object object : pdus) {
                    byte[] data = (byte[]) object;
                    SmsMessage message = SmsMessage.createFromPdu(data);
                    mPhoneNumber = message.getOriginatingAddress();// 电话号
                    mMessgeBody = message.getMessageBody();// 短信内容
                    mSendDate = message.getTimestampMillis();
                    if (!Utilities.isEmpty(mPhoneNumber)) {
                        String formateNumber = PrivacyContactUtils.formatePhoneNumber(mPhoneNumber);
                        // 查询来的号码是否为隐私联系人，如果返回值为空则说明不是
                        ContactBean contact = getPrivateMessage(formateNumber, mContext);
                        // 设置每次来的号码，方便在Observer的onChanage中去使用
                        PrivacyContactManager.getInstance(mContext).setLastMessageContact(contact);
                        if (contact != null) {
                            // 拦截短信
                            abortBroadcast();
                            String sendTime = mSimpleDateFormate.format(System.currentTimeMillis());
                            final MessageBean messageBean = new MessageBean();
                            messageBean.setMessageName(contact.getContactName());
                            messageBean.setPhoneNumber(mPhoneNumber);
                            messageBean.setMessageBody(mMessgeBody);
                            messageBean.setMessageIsRead(0);
                            messageBean.setMessageTime(sendTime);
                            messageBean.setMessageType(mAnswer);
                            // 过滤监控短信记录数据库，隐私联系人删除未读短信记录时引发数据库变化而做的操作（要在执行删除操作之前去赋值）
                            PrivacyContactManager.getInstance(mContext).deleteMsmDatebaseFlag = true;
                            AppMasterApplication.getInstance().postInAppThreadPool(new Runnable() {
                                @Override
                                public void run() {
                                    PrivacyContactManager.getInstance(mContext).synMessage(
                                            mSimpleDateFormate, messageBean, mContext,
                                            mSendDate);
                                    if (Build.VERSION.SDK_INT < 19 && !BuildProperties.isMIUI()) {
                                        // 对于4.4的系统由于可以直接拦截，拦截后不会触发数据库变化，所以再此处通知快捷手势有新消息
                                        noReadPrivacyMsmTipForQuickGesture(AppMasterPreference
                                                .getInstance(mContext));
                                    }
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {

            } catch (Error error) {

            }
        } else if (PrivacyContactUtils.CALL_RECEIVER_ACTION.equals(action)) {
            // 获取来电号码
            final String phoneNumber =
                    intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            // 通话判断来电号码是否存在来判断是，拨出还是呼入，进行isCallLogRead值的初始化
            if (!Utilities.isEmpty(phoneNumber)) {
                if (QuickGestureManager.getInstance(mContext).isCallLogRead) {
                    QuickGestureManager.getInstance(mContext).isCallLogRead = false;
                    AppMasterPreference.getInstance(mContext)
                            .setCallLogIsRedTip(false);
                }
            }
            // 没有隐私联系人时直接结束
            if (PrivacyContactManager.getInstance(context).getPrivacyContactsCount() == 0) {
                return;
            }
            // 获取当前时间
            if (mSimpleDateFormate == null) {
                mSimpleDateFormate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            }
            if (phoneNumber != null && !"".equals(phoneNumber)) {
                String formateNumber = PrivacyContactUtils.formatePhoneNumber(phoneNumber);
                // 查询该号码是否挂断拦截
                ContactBean cb = getPrivateContact(formateNumber);
                // 查询该号码是否为隐私联系人
                ContactBean privacyConatact = getPrivateMessage(formateNumber, mContext);
                PrivacyContactManager.getInstance(mContext).setLastCall(privacyConatact);
                if (cb != null) {
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
                        // 先静音处理
                        if (mAudioManager != null) {
                            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        }

                        if (mITelephony != null) {
                            LeoEventBus
                                    .getDefaultBus()
                                    .post(
                                            new PrivacyEditFloatEvent(
                                                    PrivacyContactUtils.PRIVACY_INTERCEPT_CONTACT_EVENT));
                            try {
                                // 挂断电话
                                mITelephony.endCall();
                                // 判断是否为5.0系统，特别处理
                                saveCallLog(cb);
                                Log.d("MessagePrivacyReceiver",
                                        "Call intercept successful!");
                            } catch (Exception e) {

                            }
                            // 恢复正常铃声
                            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        }
                    }
                }
            }
        } else if (PrivacyContactUtils.SENT_SMS_ACTION.equals(action)) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    // 短信发送成功
                    break;
                // 短信发送失败
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                case SmsManager.RESULT_ERROR_NULL_PDU:
                default:
                    Toast.makeText(
                            mContext,
                            mContext.getResources().getString(
                                    R.string.privacy_message_item_send_message_fail),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    // 测试来新短信或者来电能否接收到广播
    private void printTestReceiverLog(Intent intent) {
        String action = intent.getAction();
        if (action.equals(PrivacyContactUtils.MESSAGE_RECEIVER_ACTION)
                || action.equals(PrivacyContactUtils.MESSAGE_RECEIVER_ACTION2)
                || action.equals(PrivacyContactUtils.MESSAGE_RECEIVER_ACTION3)) {
            Log.e(Constants.RUN_TAG, "接收到新短信广播");
        } else if (PrivacyContactUtils.CALL_RECEIVER_ACTION.equals(action)) {
            Log.e(Constants.RUN_TAG, "接收到来电广播");
        }
    }

    private ContactBean getPrivateContact(String number) {
        ContactBean flagContact = null;
        if (!Utilities.isEmpty(number)) {
            List<ContactBean> contacts = PrivacyContactManager.getInstance(mContext)
                    .getPrivateContacts();
            for (ContactBean contactBean : contacts) {
                if (contactBean.getContactNumber().contains(number)
                        && contactBean.getAnswerType() == 0) {
                    flagContact = contactBean;
                    break;
                }
            }
        }
        return flagContact;
    }

    public static ContactBean getPrivateMessage(String number, Context context) {
        ContactBean flagContact = null;
        String formateNumber = null;
        if (!Utilities.isEmpty(number)) {
            List<ContactBean> contacts = PrivacyContactManager.getInstance(context)
                    .getPrivateContacts();
            formateNumber = PrivacyContactUtils.formatePhoneNumber(number);
            for (ContactBean contactBean : contacts) {
                if (contactBean.getContactNumber().contains(formateNumber)) {
                    flagContact = contactBean;
                    break;
                }
            }
        }
        return flagContact;
    }

    public void callLogNotification(Context context) {
        boolean callLogRuningStatus = AppMasterPreference.getInstance(
                context)
                .getCallLogItemRuning();
        if (callLogRuningStatus) {
            NotificationManager notificationManager = (NotificationManager)
                    context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification();
            Intent intentPending = new Intent(context,
                    PrivacyContactActivity.class);
            intentPending.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intentPending.putExtra(
                    PrivacyContactUtils.TO_PRIVACY_CONTACT,
                    PrivacyContactUtils.TO_PRIVACY_CALL_FLAG);
            intentPending.putExtra("message_call_notifi", true);
            PendingIntent contentIntent = PendingIntent
                    .getActivity(
                            context,
                            0,
                            intentPending,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            notification.icon = R.drawable.ic_launcher_notification;
            notification.tickerText = context
                    .getString(R.string.privacy_contact_notification_title_big);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            notification
                    .setLatestEventInfo(
                            context,
                            context.getString(R.string.privacy_contact_notification_title_big),
                            context.getString(R.string.privacy_contact_notification_title_small),
                            contentIntent);
            NotificationUtil.setBigIcon(notification,
                    R.drawable.ic_launcher_notification_big);
            notification.when = System.currentTimeMillis();
            notificationManager.notify(20140902, notification);
        }
        /*
         * 记录最后隐私短信和隐私通话哪个最后记录(解决：在快捷手势中有隐私联系人时，点击跳入最后记录的Tab页面)
         */
        QuickGestureManager.getInstance(mContext).privacyLastRecord = QuickGestureManager.RECORD_CALL;
    }

    private void saveCallLog(ContactBean contact) {
        // 判断是否为5.0系统，特别处理
        if (Build.VERSION.SDK_INT >= 21) {
            ContentValues values = new ContentValues();
            values.put(Constants.COLUMN_CALL_LOG_PHONE_NUMBER,
                    contact.getContactNumber());
            if (!"".equals(contact.getContactName())
                    && contact.getContactName() != null) {
                values.put(Constants.COLUMN_CALL_LOG_CONTACT_NAME,
                        contact.getContactName());
            } else {
                values.put(Constants.COLUMN_CALL_LOG_CONTACT_NAME,
                        contact.getContactNumber());
            }
            values.put(Constants.COLUMN_CALL_LOG_DATE,
                    mSimpleDateFormate.format(System.currentTimeMillis()));
            values.put(Constants.COLUMN_CALL_LOG_TYPE,
                    CallLog.Calls.INCOMING_TYPE);
            values.put(Constants.COLUMN_CALL_LOG_IS_READ, 0);
            // 保存记录
            Uri uri = mContext.getContentResolver().insert(
                    Constants.PRIVACY_CALL_LOG_URI, values);
            AppMasterPreference pre = AppMasterPreference
                    .getInstance(mContext);
            int count = pre.getCallLogNoReadCount();
            if (count > 0) {
                pre.setCallLogNoReadCount(count + 1);
            } else {
                pre.setCallLogNoReadCount(1);
            }
            if (uri != null) {
                // 通知更新通话记录
                LeoEventBus
                        .getDefaultBus()
                        .post(
                                new PrivacyEditFloatEvent(
                                        PrivacyContactUtils.PRIVACY_ALL_CALL_NOTIFICATION_HANG_UP));

            }
            LeoEventBus
                    .getDefaultBus()
                    .post(
                            new PrivacyEditFloatEvent(
                                    PrivacyContactUtils.PRIVACY_RECEIVER_CALL_LOG_NOTIFICATION));
            // 发送通知
            callLogNotification(mContext);
        }
    }

    private void noReadPrivacyMsmTipForQuickGesture(AppMasterPreference pref) {
        Log.e(Constants.RUN_TAG,
                "pref.getSwitchOpenPrivacyContactMessageTip()="
                        + pref.getSwitchOpenPrivacyContactMessageTip()
                        + ";pref.getQuickGestureMsmTip()=" + pref.getQuickGestureMsmTip());
        if (pref.getSwitchOpenPrivacyContactMessageTip() && pref.getQuickGestureMsmTip()) {
            QuickGestureManager.getInstance(mContext).isShowPrivacyMsm = true;
            QuickGestureManager.getInstance(mContext).isShowSysNoReadMessage = true;
            FloatWindowHelper.removeShowReadTipWindow(mContext);
        }
    }
}
