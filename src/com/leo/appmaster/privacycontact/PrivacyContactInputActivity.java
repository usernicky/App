
package com.leo.appmaster.privacycontact;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mobstat.e;
import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.PrivacyEditFloatEvent;
import com.leo.appmaster.privacy.PrivacyHelper;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.CommonTitleBar;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog.OnDiaogClickListener;
import com.leo.appmaster.ui.dialog.LEOProgressDialog;

public class PrivacyContactInputActivity extends BaseActivity {
    private CommonTitleBar mTtileBar;
    private EditText mNameEt, mNumberEt;
    private int mPhoneState = 1;
    private CheckBox mRadioNormal, mRadioHangup;
    private String mPhoneName, mPhoneNumber;
    private List<MessageBean> mAddMessages;
    private List<ContactCallLog> mAddCallLogs;
    private boolean mIsOtherLogs = false;
    private LEOProgressDialog mProgressDialog;
    private LEOAlarmDialog mAddCallLogDialog;
    private TextView mPhoneNumberShow;
    private Handler mHandler;
    
    public static final String TO_CONTACT_LIST = "to_contact_list";
    private boolean mToContactList = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_contact_input);
        initUI();
        Intent intent = getIntent();
        mToContactList = intent.getBooleanExtra(TO_CONTACT_LIST, false);
        
        mRadioNormal.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                /* sdk */
                SDKWrapper.addEvent(PrivacyContactInputActivity.this, SDKWrapper.P1,
                        "contactsadd", "answer");
                mPhoneState = 1;
                mRadioNormal.setSelected(true);
                mRadioHangup.setSelected(false);
            }
        });
        mRadioHangup.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                mPhoneState = 0;
                mRadioNormal.setSelected(false);
                mRadioHangup.setSelected(true);
            }
        });
        mTtileBar.setOptionListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!"".equals(mPhoneNumber) && mPhoneNumber != null) {
                    PrivacyContactManager pcm = PrivacyContactManager
                            .getInstance(PrivacyContactInputActivity.this);
                    ArrayList<ContactBean> contacts = pcm.getPrivateContacts();
                    // 隐私联系人去重
                    String tempNumber =
                            PrivacyContactUtils.formatePhoneNumber(mPhoneNumber);
                    boolean flagContact = false;
                    if (contacts != null && contacts.size() != 0
                            && mPhoneNumber != null && !"".equals(mPhoneNumber)) {
                        for (ContactBean contactBean : contacts) {
                            flagContact =
                                    contactBean.getContactNumber().contains(tempNumber);
                            if (flagContact) {
                                break;
                            }
                        }
                    }
                    if (!flagContact) {
                        ContactBean contact = new ContactBean();
                        contact.setContactName(mPhoneName);
                        contact.setContactNumber(mPhoneNumber);
                        contact.setAnswerType(mPhoneState);
                        ContentValues values = new ContentValues();
                        values.put(Constants.COLUMN_PHONE_NUMBER, mPhoneNumber);
                        if (mPhoneName != null && !"".equals(mPhoneName)) {
                            values.put(Constants.COLUMN_CONTACT_NAME, mPhoneName);
                        } else {
                            values.put(Constants.COLUMN_CONTACT_NAME, mPhoneNumber);
                        }
                        values.put(Constants.COLUMN_PHONE_ANSWER_TYPE, mPhoneState);
                        Uri result = getContentResolver().insert(Constants.PRIVACY_CONTACT_URI,
                                values);
                        long id = ContentUris.parseId(result);
                        if (id != -1) {
                            if (contacts != null) {
                                pcm.addContact(contact);
                                PrivacyHelper.getInstance(getApplicationContext())
                                        .computePrivacyLevel(
                                                PrivacyHelper.VARABLE_PRIVACY_CONTACT);
                            }
                            // 查询是否存在短信和通话记录
                            if (mAddMessages == null) {
                                mAddMessages = PrivacyContactUtils.getSysMessage(
                                        PrivacyContactInputActivity.this,
                                        PrivacyContactInputActivity.this.getContentResolver(),
                                        "address LIKE ? ", new String[] {
                                            "%" + tempNumber
                                        }, true);
                            } else {
                                List<MessageBean> addMessages = PrivacyContactUtils.getSysMessage(
                                        PrivacyContactInputActivity.this,
                                        PrivacyContactInputActivity.this.getContentResolver(),
                                        "address LIKE ?", new String[] {
                                            "%" + tempNumber
                                        }, true);
                                mAddMessages.addAll(addMessages);
                            }
                            if (mAddCallLogs == null) {
                                mAddCallLogs = PrivacyContactUtils.getSysCallLog(
                                        PrivacyContactInputActivity.this,
                                        PrivacyContactInputActivity.this.getContentResolver(),
                                        "number LIKE ?", new String[] {
                                            "%" + tempNumber
                                        });
                            } else {
                                List<ContactCallLog> addCalllog = PrivacyContactUtils
                                        .getSysCallLog(PrivacyContactInputActivity.this,
                                                PrivacyContactInputActivity.this
                                                        .getContentResolver(),
                                                "number LIKE ?", new String[] {
                                                    "%" + tempNumber
                                                });
                                mAddCallLogs.addAll(addCalllog);
                            }
                            if (!mIsOtherLogs) {
                                if ((mAddMessages != null && mAddMessages.size() != 0)
                                        || (mAddCallLogs != null && mAddCallLogs.size() != 0)) {
                                    mIsOtherLogs = true;
                                }
                            }
                            if (mIsOtherLogs) {
                                String title = getResources().getString(
                                        R.string.privacy_contact_add_log_dialog_title);
                                String content = getResources().getString(
                                        R.string.privacy_contact_add_log_dialog_dialog_content);
                                showAddContactLogDialog(title, content);
                            } else {
                                toContactList();
                            }
                        }
                        // "添加成功！",
                        // 通知更新隐私联系人列表
                        LeoEventBus
                                .getDefaultBus()
                                .post(new PrivacyEditFloatEvent(
                                        PrivacyContactUtils.PRIVACY_ADD_CONTACT_UPDATE));
                    } else {
                        Toast.makeText(PrivacyContactInputActivity.this,
                                getResources().getString(R.string.privacy_add_contact_toast),
                                Toast.LENGTH_SHORT)
                                .show();
                    }

                } else {
                    Toast.makeText(PrivacyContactInputActivity.this,
                            getResources().getString(R.string.input_toast_no_number_tip),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initUI() {
        mTtileBar = (CommonTitleBar) findViewById(R.id.title_bar);
        mTtileBar.setTitle(getResources().getString(
                R.string.privacy_contact_popumenus_add_new_privacy_contact));
        mTtileBar.openBackView();
        mTtileBar.setOptionImage(R.drawable.mode_done);
        mNameEt = (EditText) findViewById(R.id.privacy_input_nameET);
        mNumberEt = (EditText) findViewById(R.id.privacy_input_numberEV);
        mRadioNormal = (CheckBox) findViewById(R.id.privacy_input_normalRB);
        mRadioNormal.setSelected(true);
        mRadioHangup = (CheckBox) findViewById(R.id.privacy_input_hangupRB);
        mPhoneNumberShow = (TextView) findViewById(R.id.privacy_input_numberTV);
        String numberTip = getResources().getString(
                R.string.privacy_contact_activity_input_edit_number);
        Spanned spannedText = Html.fromHtml(numberTip);
        mPhoneNumberShow.setText(spannedText);
        TextWatcher watcher = new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                checkEditTextAdd();
            }
        };
        mNumberEt.addTextChangedListener(watcher);
        mNameEt.addTextChangedListener(watcher);
    }

    private void showProgressDialog(int maxValue, int currentValue) {
        if (mProgressDialog == null) {
            mProgressDialog = new LEOProgressDialog(this);
        }
        String title = getResources().getString(R.string.privacy_contact_progress_dialog_title);
        String content = getResources().getString(R.string.privacy_contact_progress_dialog_content);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(content);
        mProgressDialog.setMax(maxValue);
        mProgressDialog.setProgress(currentValue);
        mProgressDialog.setButtonVisiable(false);
        mProgressDialog.show();
    }

    private void showAddContactLogDialog(String title, String content) {
        if (mAddCallLogDialog == null) {
            mAddCallLogDialog = new LEOAlarmDialog(this);
        }
        mAddCallLogDialog.setOnClickListener(new OnDiaogClickListener() {
            @Override
            public void onClick(int which) {
                if (which == 1) {
                    final int privacyTotal = mAddMessages.size() + mAddCallLogs.size();
                    if (mHandler == null) {
                        mHandler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                int currentValue = msg.what;
                                if (currentValue >= privacyTotal) {
                                    mProgressDialog.cancel();
                                    toContactList();
                                } else {
                                    mProgressDialog.setProgress(currentValue);
                                }
                                super.handleMessage(msg);
                            }
                        };
                    }
                    showProgressDialog(privacyTotal, 0);
                    QueryLogAsyncTask task = new QueryLogAsyncTask();
                    task.execute(true);
                } else if (which == 0) {
                    /* SDK */
                    SDKWrapper.addEvent(PrivacyContactInputActivity.this, SDKWrapper.P1,
                            "contactsadd", "unimport");
                    if (mAddCallLogDialog != null) {
                        mAddCallLogDialog.cancel();
                    }
                    toContactList();
                }
            }
        });
        mAddCallLogDialog.setCanceledOnTouchOutside(false);
        mAddCallLogDialog.setTitle(title);
        mAddCallLogDialog.setContent(content);
        mAddCallLogDialog.show();
    }

    private class QueryLogAsyncTask extends AsyncTask<Boolean, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Boolean... arg0) {
            boolean flag = arg0[0];
            int count = 0;
            ContentResolver cr = getContentResolver();
            if (flag) {
//                ArrayList<MessageBean> messages = pm.getSysMessage();
                String formateNumber = PrivacyContactUtils.formatePhoneNumber(mPhoneNumber);

                // 导入短信和通话记录
                if (mAddMessages != null && mAddMessages.size() != 0) {
                    for (MessageBean message : mAddMessages) {
                        String contactNumber = message.getPhoneNumber();
                        String number = PrivacyContactUtils.deleteOtherNumber(contactNumber);
                        // String name = message.getMessageName();
                        String name = null;
                        if (mPhoneName != null && !"".equals(mPhoneName)) {
                            name = mPhoneName;
                        } else {
                            name = mPhoneNumber;
                        }
                        String body = message.getMessageBody();
                        String time = message.getMessageTime();
                        String threadId = message.getMessageThreadId();
                        int isRead = 1;// 0未读，1已读
                        int type = message.getMessageType();// 短信类型1是接收到的，2是已发出
                        ContentValues values = new ContentValues();
                        values.put(Constants.COLUMN_MESSAGE_PHONE_NUMBER, number);
                        values.put(Constants.COLUMN_MESSAGE_CONTACT_NAME, name);
                        String bodyTrim = body.trim();
                        values.put(Constants.COLUMN_MESSAGE_BODY, bodyTrim);
                        values.put(Constants.COLUMN_MESSAGE_DATE, time);
                        int thread = PrivacyContactUtils.queryContactId(
                                PrivacyContactInputActivity.this, message.getPhoneNumber());
                        values.put(Constants.COLUMN_MESSAGE_THREAD_ID, thread);
                        values.put(Constants.COLUMN_MESSAGE_IS_READ, isRead);
                        values.put(Constants.COLUMN_MESSAGE_TYPE, type);
                        Uri messageFlag = cr.insert(Constants.PRIVACY_MESSAGE_URI, values);
                        PrivacyContactUtils.deleteMessageFromSystemSMS("address = ?",
                                new String[] {
                                    number
                                }, PrivacyContactInputActivity.this);
                        if (messageFlag != null) {
                            Message messge = new Message();
                            count = count + 1;
                            messge.what = count;
                            mHandler.sendMessage(messge);
                        }
                    }
                    //更新短信列表
//                    for (MessageBean messageBean : messages) {
//                        if (messageBean.getPhoneNumber().contains(formateNumber)) {
//                            pm.removeSysMessage(messageBean);
//                        }
//
//                    }

                }
                // 导入通话记录
                if (mAddCallLogs != null && mAddCallLogs.size() != 0) {
                    for (ContactCallLog calllog : mAddCallLogs) {
                        String number = calllog.getCallLogNumber();
                        // String name = calllog.getCallLogName();
                        String name = null;
                        if (mPhoneName != null && !"".equals(mPhoneName)) {
                            name = mPhoneName;
                        } else {
                            name = mPhoneNumber;
                        }
                        String date = calllog.getClallLogDate();
                        int type = calllog.getClallLogType();
                        ContentValues values = new ContentValues();
                        values.put(Constants.COLUMN_CALL_LOG_PHONE_NUMBER, number);
                        values.put(Constants.COLUMN_CALL_LOG_CONTACT_NAME, name);
                        values.put(Constants.COLUMN_CALL_LOG_DATE, date);
                        values.put(Constants.COLUMN_CALL_LOG_TYPE, type);
                        values.put(Constants.COLUMN_CALL_LOG_IS_READ, 1);
                        Uri callLogFlag = cr.insert(Constants.PRIVACY_CALL_LOG_URI, values);
                        PrivacyContactUtils.deleteCallLogFromSystem("number LIKE ?", number,
                                PrivacyContactInputActivity.this);
                        if (callLogFlag != null) {
                            Message messge = new Message();
                            count = count + 1;
                            messge.what = count;
                            mHandler.sendMessage(messge);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (mAddCallLogs != null && mAddCallLogs.size() != 0) {
                LeoEventBus.getDefaultBus().post(
                        new
                        PrivacyEditFloatEvent(PrivacyContactUtils.UPDATE_CALL_LOG_FRAGMENT));
            }
            if (mAddMessages != null && mAddMessages.size() != 0) {
                LeoEventBus.getDefaultBus().post(
                        new
                        PrivacyEditFloatEvent(PrivacyContactUtils.UPDATE_MESSAGE_FRAGMENT));
            }
        }
    }

    private void checkEditTextAdd() {
        mPhoneName = mNameEt.getText().toString();
        mPhoneNumber = mNumberEt.getText().toString().trim();
    }
    
    /**
     * 跳转联系人列表
     */
    private void toContactList(){
        if(mToContactList){
            Intent intent = new Intent(this, PrivacyContactActivity.class);
            intent.putExtra(PrivacyContactUtils.TO_PRIVACY_CONTACT, PrivacyContactUtils.TO_PRIVACY_CONTACT_FLAG);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }else {
            PrivacyContactInputActivity.this.finish();
        }
    }
}
