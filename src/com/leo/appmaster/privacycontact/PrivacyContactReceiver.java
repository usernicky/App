
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
import android.os.RemoteException;
import android.provider.CallLog;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.callfilter.CallFIlterUIHelper;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.PrivacyEditFloatEvent;
import com.leo.appmaster.phoneSecurity.PhoneSecurityManager;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.NotificationUtil;
import com.leo.appmaster.utils.Utilities;

public class PrivacyContactReceiver extends BroadcastReceiver {
    private static final String TAG = "PrivacyContactReceiver";
    private static final boolean DBG = false;
    private ITelephony mITelephony;
    private AudioManager mAudioManager;
    private int mAnswer = 1;
    private String mPhoneNumber, mMessgeBody;
    private long mSendDate;
    private Context mContext;
    private SimpleDateFormat mSimpleDateFormate;

    public PrivacyContactReceiver() {
    }

    public PrivacyContactReceiver(ITelephony itelephony, AudioManager audioManager) {
        this.mITelephony = itelephony;
        this.mAudioManager = audioManager;
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        /* 测试 */
        if (DBG) {
            printTestReceiverLog(intent);
        }
        String action = intent.getAction();
        mContext = context;
        if (mSimpleDateFormate == null) {
            mSimpleDateFormate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        }
        /*短信监听*/
        if (action.equals(PrivacyContactUtils.MESSAGE_RECEIVER_ACTION)
                || action.equals(PrivacyContactUtils.MESSAGE_RECEIVER_ACTION2)
                || action.equals(PrivacyContactUtils.MESSAGE_RECEIVER_ACTION3)) {
            PrivacyContactManager.getInstance(mContext).testValue = true;
//            if (PrivacyContactManager.getInstance(context).getPrivacyContactsCount() == 0) {
//                return;
//            }
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

                    /*手机防盗功能处理:防止手机防盗号码为隐私联系人时拦截掉放在最前面*/
                    boolean isSecurInstr = PhoneSecurityManager.getInstance(mContext).securityPhoneReceiverHandler(message);
                    if (isSecurInstr) {
                        break;
                    }

                    /*隐私联系人功能处理：*/
                    if (!Utilities.isEmpty(mPhoneNumber)) {
                        String formateNumber = PrivacyContactUtils.formatePhoneNumber(mPhoneNumber);
                        /*查询来的号码是否为隐私联系人，如果返回值为空则说明不是*/
                        ContactBean contact = PrivacyContactManager.getInstance(mContext).getPrivateMessage(formateNumber, mContext);
                        /*设置每次来的号码，方便在Observer的onChanage中去使用*/
                        PrivacyContactManager.getInstance(mContext).setLastMessageContact(contact);
                        if (contact != null) {

                            /*4.4以上不去做短信拦截操作*/
                            boolean isLessLeve19 = PrivacyContactUtils.isLessApiLeve19();
                            if (!isLessLeve19) {
                                return;
                            }

                            /* 拦截短信*/
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
//                            PrivacyContactManager.getInstance(mContext).deleteMsmDatebaseFlag = true;
                            ThreadManager.executeOnAsyncThread(new Runnable() {
                                @Override
                                public void run() {
                                    PrivacyContactManager.getInstance(mContext).synMessage(
                                            mSimpleDateFormate, messageBean, mContext,
                                            mSendDate);
                                }
                            });
                        }
                    }

                }
            } catch (Exception e) {

            } catch (Error error) {

            }
        } else if (PrivacyContactUtils.CALL_RECEIVER_ACTION.equals(action)) {
            PrivacyContactManager.getInstance(mContext).testValue = true;
            // 获取来电号码
            final String phoneNumber =
                    intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            // 没有隐私联系人时直接结束
            if (PrivacyContactManager.getInstance(context).getPrivacyContactsCount() == 0) {
                return;
            }
            // 获取当前时间
            if (mSimpleDateFormate == null) {
                mSimpleDateFormate = new SimpleDateFormat(Constants.PATTERN_DATE);
            }
            if (phoneNumber != null && !"".equals(phoneNumber)) {
                String formateNumber = PrivacyContactUtils.formatePhoneNumber(phoneNumber);
                // 查询该号码是否挂断拦截
                ContactBean cb = PrivacyContactManager.getInstance(mContext).getPrivateContact(formateNumber);
                // 查询该号码是否为隐私联系人
                ContactBean privacyConatact = PrivacyContactManager.getInstance(mContext).getPrivateMessage(formateNumber, mContext);
                PrivacyContactManager.getInstance(mContext).setLastCall(privacyConatact);
                if (cb != null) {
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
                        // 先静音处理
                        if (mAudioManager != null) {
                            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        }

                        if (mITelephony != null) {
                            String msg = PrivacyContactUtils.PRIVACY_INTERCEPT_CONTACT_EVENT;
                            PrivacyEditFloatEvent event = new PrivacyEditFloatEvent(msg);
                            LeoEventBus.getDefaultBus().post(event);
                            try {
                                /* 挂断电话*/
                                mITelephony.endCall();

                                /* 判断是否为5.0系统，特别处理*/
                                PrivacyContactUtils.saveCallLog(cb);
                            } catch (Exception e) {
                            }

                            /*恢复正常铃声*/
                            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        }
                    }
                }
            }
        } else if (PrivacyContactUtils.SENT_SMS_ACTION.equals(action)) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:

                    /*短信发送成功*/

                    break;
                /*短信发送失败*/
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                case SmsManager.RESULT_ERROR_NULL_PDU:
                default:
                    /*该标志用于限制同一次失败提示值提示一次*/
                    if (!PrivacyContactManager.getInstance(mContext).mSendMsmFail) {
                        PrivacyContactManager.getInstance(mContext).mSendMsmFail = true;
                        String failStr = mContext.getResources().getString(
                                R.string.privacy_message_item_send_message_fail);
                        Toast.makeText(mContext, failStr, Toast.LENGTH_SHORT).show();
                    }
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
            LeoLog.i(TAG, "接收到新短信广播");
        } else if (PrivacyContactUtils.CALL_RECEIVER_ACTION.equals(action)) {
            LeoLog.i(TAG, "接收到来电广播");
        }
    }

}