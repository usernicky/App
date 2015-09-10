
package com.leo.appmaster.privacycontact;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.EventId;
import com.leo.appmaster.eventbus.event.PrivacyEditFloatEvent;
import com.leo.appmaster.eventbus.event.PrivacyMessageEvent;
import com.leo.appmaster.fragment.BaseFragment;
import com.leo.appmaster.quickgestures.FloatWindowHelper;
import com.leo.appmaster.quickgestures.QuickGestureManager;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog.OnDiaogClickListener;
import com.leo.appmaster.ui.dialog.LEOProgressDialog;
import com.leo.appmaster.ui.dialog.LEORoundProgressDialog;

public class PrivacyCalllogFragment extends BaseFragment {

    private TextView mTextView;
    private LinearLayout mDefaultText;
    private ListView mContactCallLog;
    private CallLogAdapter mAdapter;
    private ArrayList<ContactCallLog> mContactCallLogs;
    private Context mContext;
    private boolean mIsEditModel = false;
    private LEOAlarmDialog mAddCallLogDialog;
    private List<ContactCallLog> mDeleteCallLog;
    private int mCallLogCount;
    private Handler mHandler;
    private LEORoundProgressDialog mProgressDialog;
    private SimpleDateFormat mSimpleDateFormate;

    @Override
    protected int layoutResourceId() {
        return R.layout.fragment_privacy_call_log;
    }

    @SuppressLint("WrongViewCast")
    @Override
    protected void onInitUI() {
        mContext = getActivity();
        mSimpleDateFormate = new SimpleDateFormat("yy/MM/dd");
        mTextView = (TextView) findViewById(R.id.content);
        mContactCallLog = (ListView) findViewById(R.id.contactLV);
        mDefaultText = (LinearLayout) findViewById(R.id.call_log_default_tv);
        mDeleteCallLog = new ArrayList<ContactCallLog>();
        mContactCallLogs = new ArrayList<ContactCallLog>();
        LeoEventBus.getDefaultBus().register(this);
        mAdapter = new CallLogAdapter(mContactCallLogs);
        mContactCallLog.setAdapter(mAdapter);
        mContactCallLog.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
                ContactCallLog calllog = mContactCallLogs.get(position);
                if (!mIsEditModel) {
                    String name = calllog.getCallLogName();
                    String number = calllog.getCallLogNumber();
                    String[] bundleData = new String[] {
                            name, number
                    };
                    Bundle bundle = new Bundle();
                    bundle.putStringArray(PrivacyContactUtils.CONTACT_CALL_LOG, bundleData);
                    Intent intent = new Intent(mContext, PrivacyCallLogListActivity.class);
                    intent.putExtras(bundle);
                    try {
                        startActivity(intent);
                        // 标记为已读
                        String readNumberFlag = PrivacyContactUtils.formatePhoneNumber(calllog
                                .getCallLogNumber());
                        updateCallLogMyselfIsRead(1,
                                "call_log_phone_number LIKE ? and call_log_is_read = 0",
                                new String[] {
                                    "%" + readNumberFlag
                                }, mContext);
                        mAdapter.notifyDataSetChanged();
                    } catch (Exception e) {
                    }
                } else {
                    ImageView image = (ImageView) view.findViewById(R.id.call_log_itemCB);
                    if (!calllog.isCheck()) {
                        image.setImageDrawable(getResources().getDrawable(
                                R.drawable.select));
                        calllog.setCheck(true);
                        mDeleteCallLog.add(calllog);
                        mCallLogCount = mCallLogCount + 1;
                    } else {
                        image.setImageDrawable(getResources().getDrawable(
                                R.drawable.unselect));
                        calllog.setCheck(false);
                        mDeleteCallLog.remove(calllog);
                        if (mCallLogCount > 0) {
                            mCallLogCount = mCallLogCount - 1;
                        }
                    }
                    updateTitleBarSelectStatus();
                }
            }
        });

        mContactCallLog.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                LeoEventBus.getDefaultBus().post(
                        new PrivacyMessageEvent(EventId.EVENT_PRIVACY_EDIT_MODEL,
                                PrivacyContactUtils.FROM_CONTACT_NO_SELECT_EVENT));
                mIsEditModel = true;
                mAdapter.notifyDataSetChanged();
                return true;
            }
        });

        PrivacyContactCallLogTask task = new PrivacyContactCallLogTask();
        task.execute("");
    }

    // 更新TitleBar
    private void updateTitleBarSelectStatus() {
        if (mDeleteCallLog != null && mDeleteCallLog.size() > 0) {
            LeoEventBus.getDefaultBus().post(
                    new PrivacyMessageEvent(EventId.EVENT_PRIVACY_EDIT_MODEL,
                            PrivacyContactUtils.FROM_CALL_LOG_EVENT));
        } else {
            LeoEventBus.getDefaultBus().post(
                    new PrivacyMessageEvent(EventId.EVENT_PRIVACY_EDIT_MODEL,
                            PrivacyContactUtils.FROM_CONTACT_NO_SELECT_EVENT));
        }
    }

    public void onEventMainThread(PrivacyEditFloatEvent event) {
        if (PrivacyContactUtils.CANCEL_EDIT_MODEL.equals(event.editModel)) {
            restoreParameter();
            if (mContactCallLogs == null || mContactCallLogs.size() == 0) {
                mDefaultText.setVisibility(View.VISIBLE);
            } else {
                mDefaultText.setVisibility(View.GONE);
            }
            mAdapter.notifyDataSetChanged();
        } else if (PrivacyContactUtils.CALL_LOG_EDIT_MODEL_OPERATION_DELETE
                .equals(event.editModel)) {
            if (!mDeleteCallLog.isEmpty()) {
                showDeleteMoreDialog(
                        getResources().getString(R.string.privacy_call_delete_call_log),
                        PrivacyContactUtils.CALL_LOG_EDIT_MODEL_OPERATION_DELETE);
            }
        } else if (PrivacyContactUtils.UPDATE_CALL_LOG_FRAGMENT.equals(event.editModel)
                || PrivacyContactUtils.CONTACT_DETAIL_DELETE_LOG_UPDATE_CALL_LOG_LIST
                        .equals(event.editModel)) {
            PrivacyContactCallLogTask task = new PrivacyContactCallLogTask();
            task.execute("");
        } else if (PrivacyContactUtils.PRIVACY_INTERCEPT_CONTACT_EVENT.equals(event.editModel)) {
            PrivacyContactCallLogTask task = new PrivacyContactCallLogTask();
            task.execute("");
        } else if (PrivacyContactUtils.PRIVACY_ALL_CALL_NOTIFICATION_HANG_UP
                .equals(event.editModel)) {
            PrivacyContactCallLogTask task = new PrivacyContactCallLogTask();
            task.execute("");
        } else if (PrivacyContactUtils.PRIVACY_EDIT_NAME_UPDATE_CALL_LOG_EVENT
                .equals(event.editModel)) {
            PrivacyContactCallLogTask task = new PrivacyContactCallLogTask();
            task.execute("");
        }
    }

    // 恢复编辑状态之前的参数状态
    public void restoreParameter() {
        mIsEditModel = false;
        mDeleteCallLog.clear();
        setCallLOgCheck(false);
        mCallLogCount = 0;
    }

    // 设置选中状态
    public void setCallLOgCheck(boolean flag) {
        for (ContactCallLog calllog : mContactCallLogs) {
            calllog.setCheck(flag);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        LeoEventBus.getDefaultBus().unregister(this);
        super.onDestroyView();
    }

    public void setContent(String content) {
        if (mTextView != null) {
            mTextView.setText(content);
        }
    }

    @SuppressLint("CutPasteId")
    private class CallLogAdapter extends BaseAdapter {
        LayoutInflater relativelayout;

        public CallLogAdapter(ArrayList<ContactCallLog> contacts) {
            relativelayout = LayoutInflater.from(mContext);
        }

        @Override
        public int getCount() {

            return (mContactCallLogs != null) ? mContactCallLogs.size() : 0;
        }

        @Override
        public Object getItem(int position) {

            return mContactCallLogs.get(position);
        }

        @Override
        public long getItemId(int position) {

            return position;
        }

        class ViewHolder {
            ImageView checkImage, typeImage, bottomLine;
            CircleImageView contactIcon;
            TextView name, number, content, count;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            ContactCallLog mb = mContactCallLogs.get(position);
            if (convertView == null) {
                vh = new ViewHolder();
                convertView = relativelayout.inflate(R.layout.fragmetn_privacy_call_log_list, null);
                vh.name = (TextView) convertView.findViewById(R.id.call_log_item_nameTV);
                vh.number = (TextView) convertView.findViewById(R.id.message_item_numberTV);
                vh.content = (TextView) convertView.findViewById(R.id.call_log_list_dateTV);
                vh.typeImage = (ImageView) convertView.findViewById(R.id.call_log_type);
                vh.checkImage = (ImageView) convertView.findViewById(R.id.call_log_itemCB);
                vh.contactIcon = (CircleImageView) convertView.findViewById(R.id.contactIV);
                vh.bottomLine = (ImageView) convertView.findViewById(R.id.bottom_line);
                vh.checkImage.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        if (!mIsEditModel) {
                            ContactCallLog calllog = (ContactCallLog) arg0.getTag();
                            // 查询该号码是否为隐私联系人
                            String formateNumber = PrivacyContactUtils
                                    .formatePhoneNumber(calllog.getCallLogNumber());
                            ContactBean privacyConatact = MessagePrivacyReceiver.getPrivateMessage(
                                    formateNumber, mContext);
                            PrivacyContactManager.getInstance(mContext)
                                    .setLastCall(privacyConatact);
                            Uri uri = Uri.parse("tel:" + calllog.getCallLogNumber());
                            // 跳到拨号界面
                            Intent intent = new Intent(Intent.ACTION_DIAL,
                                    uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            try {
                                startActivity(intent);
                            } catch (Exception e) {
                            }
                        } else {
                            return;
                        }
                    }
                });
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            if (mb.getCallLogName() != null && !"".equals(mb.getCallLogName())) {
                vh.name.setText(mb.getCallLogName());
            } else {
                vh.name.setText(mb.getCallLogNumber());
            }
            vh.number.setText(mb.getCallLogNumber());
            Date tempDate = null;
            try {
                tempDate = mSimpleDateFormate.parse(mb.getClallLogDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String date = mSimpleDateFormate.format(tempDate);
            vh.content.setText(date);
            if (mb != null) {
                vh.typeImage.setVisibility(View.VISIBLE);
                if (mb.getClallLogType() == CallLog.Calls.INCOMING_TYPE) {
                    vh.typeImage.setImageResource(R.drawable.into_icon);
                } else if (mb.getClallLogType() == CallLog.Calls.OUTGOING_TYPE) {
                    vh.typeImage.setImageResource(R.drawable.exhale_icon);
                } else if (mb.getClallLogType() == CallLog.Calls.MISSED_TYPE) {
                    vh.typeImage.setImageResource(R.drawable.into_icon);
                }
            }
            if (mIsEditModel) {
                // vh.checkImage.setOnClickListener(null);
                if (mb.isCheck()) {
                    vh.checkImage.setImageResource(R.drawable.select);
                } else {
                    vh.checkImage.setImageResource(R.drawable.unselect);
                }
            } else {
                vh.checkImage.setImageResource(R.drawable.privacy_call_log_item_call_bt_selecter);
            }
            // 设置未读数量
            if (mb.getCallLogCount() > 0) {
                vh.contactIcon.setAnswerStatus(PrivacyContactUtils.RED_TIP);
            } else {
                vh.contactIcon.setAnswerStatus("");
            }
            Bitmap icon = mb.getContactIcon();
            vh.contactIcon.setImageBitmap(icon);
            if (mContactCallLogs != null && mContactCallLogs.size() > 0) {
                if (position == mContactCallLogs.size() - 1) {
                    vh.bottomLine.setVisibility(View.GONE);
                } else {
                    vh.bottomLine.setVisibility(View.VISIBLE);
                }
            }
            vh.checkImage.setTag(mb);
            return convertView;
        }
    }

    /**
     * getCallLog
     * 
     * @param phoneNumber
     * @return
     */
    private ArrayList<ContactCallLog> getCallLog() {
        ArrayList<ContactCallLog> contactCalls = new ArrayList<ContactCallLog>();
        Map<String, ContactCallLog> callLogList = new ConcurrentHashMap<String, ContactCallLog>();
        Cursor cursor = mContext.getContentResolver()
                .query(Constants.PRIVACY_CALL_LOG_URI, null, null, null, "call_log_date desc");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ContactCallLog callLog = new ContactCallLog();
                int count = cursor.getCount();
                String number = cursor.getString(cursor
                        .getColumnIndex(Constants.COLUMN_CALL_LOG_PHONE_NUMBER));
                String name = cursor.getString(cursor
                        .getColumnIndex(Constants.COLUMN_CALL_LOG_CONTACT_NAME));
                String date = cursor
                        .getString(cursor.getColumnIndex(Constants.COLUMN_CALL_LOG_DATE));
                callLog.setClallLogDate(date);
                int type = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_CALL_LOG_TYPE));
                callLog.setCallLogCount(count);
                callLog.setCallLogName(name);
                callLog.setCallLogNumber(number);
                callLog.setClallLogType(type);
                Bitmap icon = PrivacyContactUtils.getContactIcon(mContext, number);
                if (icon != null) {
                    callLog.setContactIcon(icon);
                } else {
                    callLog.setContactIcon(((BitmapDrawable) mContext.getResources().getDrawable(
                            R.drawable.default_user_avatar)).getBitmap());
                }
                if (callLogList != null && callLogList.size() > 0) {
                    Iterator<Entry<String, ContactCallLog>> iterator =
                            callLogList.entrySet()
                                    .iterator();
                    String formateNumber =
                            PrivacyContactUtils.formatePhoneNumber(number);
                    while (iterator.hasNext()) {
                        Entry<String, ContactCallLog> entry = iterator.next();
                        String contactCallLog = entry.getKey();
                        if (!contactCallLog.contains(formateNumber)) {
                            callLog.setCallLogCount(noReadCallLogCount(number));
                            callLogList.put(number, callLog);
                        }
                    }
                } else {
                    callLog.setCallLogCount(noReadCallLogCount(number));
                    callLogList.put(number, callLog);
                }
            }
            Iterable<ContactCallLog> iterable = callLogList.values();
            for (ContactCallLog contactCallLog : iterable) {
                contactCalls.add(contactCallLog);
            }
            Collections.sort(contactCalls, PrivacyContactUtils.mCallLogCamparator);
            cursor.close();
        }
        return contactCalls;
    }

    private void showProgressDialog(int maxValue, int currentValue) {
        if (mProgressDialog == null) {
            mProgressDialog = new LEORoundProgressDialog(mContext);
        }
        String title = getResources().getString(R.string.privacy_contact_progress_dialog_title);
        String content = getResources().getString(R.string.privacy_contact_progress_dialog_content);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(content);
        mProgressDialog.setMax(maxValue);
        mProgressDialog.setProgress(currentValue);
        mProgressDialog.setCustomProgressTextVisiable(true);
        mProgressDialog.setButtonVisiable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    private void showDeleteMoreDialog(String content, final String flag) {
        if (mAddCallLogDialog == null) {
            mAddCallLogDialog = new LEOAlarmDialog(mContext);
        }
        mAddCallLogDialog.setOnClickListener(new OnDiaogClickListener() {
            @Override
            public void onClick(int which) {
                if (which == 1) {
                    if (PrivacyContactUtils.CALL_LOG_EDIT_MODEL_OPERATION_DELETE.equals(flag)) {
                        /* sdk */
                        SDKWrapper.addEvent(mContext, SDKWrapper.P1, "privacyedit", "deletecall");
                        mHandler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                int currentValue = msg.what;
                                if (currentValue >= mCallLogCount) {
                                    if (mProgressDialog != null) {
                                        if (mContactCallLogs == null
                                                || mContactCallLogs.size() == 0) {
                                            mDefaultText.setVisibility(View.VISIBLE);
                                        } else {
                                            mDefaultText.setVisibility(View.GONE);
                                        }
                                        // mAdapter.notifyDataSetChanged();
                                        mProgressDialog.cancel();
                                    }
                                } else {
                                    mProgressDialog.setProgress(currentValue);
                                }
                                super.handleMessage(msg);
                            }
                        };
                        showProgressDialog(mCallLogCount, 0);
                        PrivacyCallLogTask task = new PrivacyCallLogTask();
                        task.execute(PrivacyContactUtils.CALL_LOG_EDIT_MODEL_OPERATION_DELETE);

                    }
                } else if (which == 0) {
                    mAddCallLogDialog.cancel();
                    restoreParameter();
                    mAdapter.notifyDataSetChanged();
                    LeoEventBus.getDefaultBus().post(
                            new PrivacyMessageEvent(EventId.EVENT_PRIVACY_EDIT_MODEL,
                                    PrivacyContactUtils.EDIT_MODEL_RESTOR_TO_SMS_CANCEL));
                }

            }
        });
        mAddCallLogDialog.setCanceledOnTouchOutside(false);
        mAddCallLogDialog.setContent(content);
        mAddCallLogDialog.show();
    }

    // 删除隐私通话记录
    private class PrivacyCallLogTask extends AsyncTask<String, Boolean, List<ContactCallLog>>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<ContactCallLog> doInBackground(String... arg0) {
            int count = 0;
            List<ContactCallLog> deleteCallLog = new ArrayList<ContactCallLog>();
            AppMasterPreference pre = AppMasterPreference.getInstance(mContext);
            int temp = pre.getCallLogNoReadCount();
            for (ContactCallLog calllog : mDeleteCallLog) {
                // check no read messag
                int noReadCount = noReadCallLogCount(calllog.getCallLogNumber());
                if (temp > 0) {
                    if (noReadCount > 0) {
                        for (int i = 0; i < noReadCount; i++) {
                            if (temp > 0) {
                                temp = temp - 1;
                                pre.setCallLogNoReadCount(temp);
                            }
                            if (temp <= 0) {
                                /* ISwipe处理：通知没有未读 */
                                QuickGestureManager.getInstance(mContext)
                                        .cancelPrivacyTipFromPrivacyCall();
                                // 没有未读去除隐私通知
                                if (pre.getMessageNoReadCount() <= 0) {
                                    NotificationManager notificationManager = (NotificationManager) getActivity()
                                            .getSystemService(
                                                    Context.NOTIFICATION_SERVICE);
                                    notificationManager.cancel(20140902);
                                }
                                // 隐私通话没有未读
                                /**
                                 * 对快捷手势隐私联系人,消费隐私通话时，红点去除操作
                                 */
                                PrivacyContactManager
                                        .getInstance(mContext)
                                        .deletePrivacyCallCancelRedTip(mContext);
                                LeoEventBus
                                        .getDefaultBus()
                                        .post(
                                                new PrivacyEditFloatEvent(
                                                        PrivacyContactUtils.PRIVACY_CONTACT_ACTIVITY_CALL_LOG_CANCEL_RED_TIP_EVENT));
                            }
                        }
                    }
                }
                int flagNumber = PrivacyContactUtils.deleteCallLogFromMySelf(
                        Constants.COLUMN_CALL_LOG_PHONE_NUMBER + " = ? ",
                        calllog.getCallLogNumber(),
                        mContext);
                if (flagNumber != -1 && mHandler != null) {
                    // mContactCallLogs.remove(calllog);
                    deleteCallLog.add(calllog);
                    Message messge = new Message();
                    count = count + 1;
                    messge.what = count;
                    mHandler.sendMessage(messge);
                }
            }
            return deleteCallLog;
        }

        @Override
        protected void onPostExecute(List<ContactCallLog> result) {
            for (ContactCallLog calllog : result) {
                mContactCallLogs.remove(calllog);
            }
            mIsEditModel = false;
            mCallLogCount = 0;
            mAdapter.notifyDataSetChanged();
            mDeleteCallLog.clear();
            LeoEventBus.getDefaultBus().post(
                    new PrivacyMessageEvent(EventId.EVENT_PRIVACY_EDIT_MODEL,
                            PrivacyContactUtils.EDIT_MODEL_RESTOR_TO_SMS_CANCEL));
            super.onPostExecute(result);
        }
    }

    // 获取未读通话数量
    private int noReadCallLogCount(String number) {
        return PrivacyContactUtils.getNoReadCallLogCount(mContext, number);
    }

    // 通话记录标记为已读
    public static void updateCallLogMyselfIsRead(int read, String selection,
            String[] selectionArgs, Context context) {
        ContentValues values = new ContentValues();
        values.put("call_log_is_read", read);
        int count = context.getContentResolver().update(Constants.PRIVACY_CALL_LOG_URI,
                values, selection,
                selectionArgs);
        if (count > 0) {
            AppMasterPreference pre = AppMasterPreference.getInstance(context);
            for (int i = 0; i < count; i++) {
                int temp = pre.getCallLogNoReadCount();
                if (temp > 0) {
                    pre.setCallLogNoReadCount(temp - 1);
                    if (temp - 1 <= 0) {
                        /* ISwipe处理：通知没有未读 */
                        QuickGestureManager.getInstance(context).cancelPrivacyTipFromPrivacyCall();
                        // 没有未读去除隐私通知
                        if (pre.getMessageNoReadCount() <= 0) {
                            NotificationManager notificationManager = (NotificationManager)
                                    context
                                            .getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.cancel(20140902);
                        }
                        // 隐私通话没有未读
                        /**
                         * 对快捷手势隐私联系人，红点去除操作
                         */
                        // 隐私通话

                        if (QuickGestureManager.getInstance(context).isShowPrivacyCallLog) {
                            QuickGestureManager.getInstance(context).isShowPrivacyCallLog = false;
                            AppMasterPreference.getInstance(context).setQuickGestureCallLogTip(
                                    false);
                            if ((QuickGestureManager.getInstance(context).getQuickNoReadCall() == null || QuickGestureManager
                                    .getInstance(context).getQuickNoReadCall().size() <= 0)/* 未读通话 */
                                    && (QuickGestureManager.getInstance(context)
                                            .getQuiQuickNoReadMessage() == null || QuickGestureManager
                                            .getInstance(context).getQuiQuickNoReadMessage().size() <= 0)/* 未读短信 */
                                    && AppMasterPreference.getInstance(context)
                                            .getMessageNoReadCount() <= 0/* 隐私短信 */
                                    && AppMasterPreference.getInstance(context)
                                            .getLastBusinessRedTipShow()/* 运营 */) {
                                QuickGestureManager.getInstance(context).isShowSysNoReadMessage = false;
                            }
                        }
                        FloatWindowHelper.removeShowReadTipWindow(context);
                        LeoEventBus
                                .getDefaultBus()
                                .post(
                                        new PrivacyEditFloatEvent(
                                                PrivacyContactUtils.PRIVACY_CONTACT_ACTIVITY_CALL_LOG_CANCEL_RED_TIP_EVENT));
                    }
                }
            }
        }
    }

    private class PrivacyContactCallLogTask extends
            AsyncTask<String, Boolean, ArrayList<ContactCallLog>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ArrayList<ContactCallLog> doInBackground(String... arg0) {
            ArrayList<ContactCallLog> calllogs = new ArrayList<ContactCallLog>();
            calllogs = getCallLog();
            return calllogs;
        }

        @Override
        protected void onPostExecute(ArrayList<ContactCallLog> result) {
            super.onPostExecute(result);
            if (mContactCallLogs != null) {
                mContactCallLogs.clear();
                mContactCallLogs = result;
            }
            if (mContactCallLogs == null || mContactCallLogs.size() == 0) {
                mDefaultText.setVisibility(View.VISIBLE);
            } else {
                mDefaultText.setVisibility(View.GONE);
            }
            mAdapter.notifyDataSetChanged();
        }
    }
}
