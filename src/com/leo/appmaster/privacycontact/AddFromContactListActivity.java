
package com.leo.appmaster.privacycontact;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.EventId;
import com.leo.appmaster.eventbus.event.PrivacyEditFloatEvent;
import com.leo.appmaster.eventbus.event.PrivacyMessageEvent;
import com.leo.appmaster.privacy.PrivacyHelper;
import com.leo.appmaster.privacycontact.ContactSideBar.OnTouchingLetterChangedListener;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.CommonTitleBar;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog.OnDiaogClickListener;
import com.leo.appmaster.ui.dialog.LEORoundProgressDialog;

public class AddFromContactListActivity extends BaseActivity implements OnItemClickListener {
    private ListView mListContact;
    private ContactAdapter mContactAdapter;
    private List<ContactBean> mPhoneContact;
    private ContactSideBar mContactSideBar;
    // private PinyinComparator mPinyinComparator;
    private CommonTitleBar mTtileBar;
    private List<ContactBean> mAddPrivacyContact;
    private Handler mHandler;
    private LEORoundProgressDialog mProgressDialog;
    private LEOAlarmDialog mAddContactDialog;
    private ProgressBar mProgressBar;
    private TextView mDialog;
    private List<MessageBean> mAddMessages;
    private List<ContactCallLog> mAddCallLogs;
    private int mAnswerType = 1;// 接听类型1-正常接听，0-立即挂断;
    private boolean mLogFlag = false;
    private LinearLayout mDefaultText;
    private Button mAutoDddBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_privacy_contact);
        mDefaultText = (LinearLayout) findViewById(R.id.add_contact_default_tv);
        mAutoDddBtn = (Button)mDefaultText.findViewById(R.id.moto_add_btn);
        mAutoDddBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                /* SDK */
                SDKWrapper.addEvent(AddFromContactListActivity.this, SDKWrapper.P1, "contactsadd",
                        "contactsemptyadd");
                Intent intent = new Intent(AddFromContactListActivity.this,
                        PrivacyContactInputActivity.class);
                intent.putExtra(PrivacyContactInputActivity.TO_CONTACT_LIST, true);
                startActivity(intent);
            }
        });
        mTtileBar = (CommonTitleBar) findViewById(R.id.add_privacy_contact_title_bar);
        mTtileBar.openBackView();
        mTtileBar.setOptionImageVisibility(View.VISIBLE);
        mTtileBar.setOptionImage(R.drawable.mode_done);
        mTtileBar.findViewById(R.id.tv_option_image).setBackgroundResource(
                R.drawable.privacy_title_bt_selecter);
        mTtileBar.setTitle(getResources()
                .getString(R.string.privacy_contact_popumenus_from_contact));
        mPhoneContact = new ArrayList<ContactBean>();
        mAddPrivacyContact = new ArrayList<ContactBean>();
        mListContact = (ListView) findViewById(R.id.add_contactLV);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar_loading);
        /**
         * 初始化sidebar
         */
        mContactSideBar = (ContactSideBar) findViewById(R.id.contact_sidrbar);
        mDialog = (TextView) findViewById(R.id.contact_dialog);
        mContactSideBar.setTextView(mDialog);
        mContactAdapter = new ContactAdapter();
        mListContact.setAdapter(mContactAdapter);
        mListContact.setOnItemClickListener(this);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int currentValue = msg.what;
                if (currentValue >= mAddPrivacyContact.size()) {
                    if (!mLogFlag) {
                        if (mProgressDialog != null) {
                            mProgressDialog.cancel();
                        }
                        AddFromContactListActivity.this.finish();
                    } else {
                        if (mProgressDialog != null) {
                            mProgressDialog.cancel();
                        }
                        mLogFlag = false;
                    }
                } else {
                    mProgressDialog.setProgress(currentValue);
                }
                super.handleMessage(msg);
            }
        };
        mTtileBar.setOptionListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mPhoneContact != null && mPhoneContact.size() > 0) {
                    if (mAddPrivacyContact != null && mAddPrivacyContact.size() > 0) {
                        // 添加隐私联系人
                        showProgressDialog(mAddPrivacyContact.size(), 0);
                        PrivacyContactTask task = new PrivacyContactTask();
                        task.execute(PrivacyContactUtils.ADD_CONTACT_MODEL);
                    } else {
                        Toast.makeText(AddFromContactListActivity.this,
                                getResources().getString(R.string.privacy_contact_toast_no_choose),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        mContactSideBar.setOnTouchingLetterChangedListener(new OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                int position = mContactAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mListContact.setSelection(position);
                }
            }
        });
        AddContactAsyncTask addContacctTask = new AddContactAsyncTask();
        addContacctTask.execute(true);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LeoEventBus.getDefaultBus().unregister(this);
        mHandler = null;
        mAddPrivacyContact.clear();
        mListContact.post(new Runnable() {
            @Override
            public void run() {
                for (ContactBean contact : mPhoneContact) {
                    if (contact.isCheck()) {
                        contact.setCheck(false);
                    }
                }
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ContactBean contact = mPhoneContact.get(position);
        ImageView image = (ImageView) view.findViewById(R.id.contact_item_check_typeIV);
        if (!contact.isCheck()) {
            mAddPrivacyContact.add(contact);
            image.setImageDrawable(getResources().getDrawable(R.drawable.select));
            contact.setCheck(true);
        } else {
            mAddPrivacyContact.remove(contact);
            image.setImageDrawable(getResources().getDrawable(R.drawable.unselect));
            contact.setCheck(false);
        }

    }

    @SuppressLint("CutPasteId")
    private class ContactAdapter extends BaseAdapter implements SectionIndexer {
        LayoutInflater relativelayout;

        public ContactAdapter() {
            relativelayout = LayoutInflater.from(AddFromContactListActivity.this);
        }

        @Override
        public int getCount() {

            return (mPhoneContact != null) ? mPhoneContact.size() : 0;
        }

        @Override
        public Object getItem(int position) {

            return mPhoneContact.get(position);
        }

        @Override
        public long getItemId(int position) {

            return position;
        }

        class ViewHolder {
            TextView name, number, sortLetter;
            ImageView checkImage;
            CircleImageView contactIcon;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if (convertView == null) {
                vh = new ViewHolder();
                convertView = relativelayout.inflate(R.layout.activity_add_privacy_contact_item,
                        null);
                vh.name = (TextView) convertView.findViewById(R.id.contact_item_nameTV);
                vh.number = (TextView) convertView.findViewById(R.id.contact_item_numberTV);
                vh.checkImage = (ImageView) convertView
                        .findViewById(R.id.contact_item_check_typeIV);
                vh.sortLetter = (TextView) convertView
                        .findViewById(R.id.add_from_contact_sort_letter);
                vh.contactIcon = (CircleImageView) convertView.findViewById(R.id.contactIV);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            ContactBean mb = mPhoneContact.get(position);
            // mContactIcon.setVisibility(View.VISIBLE);
            // 通过position获取分类的首字母
            // int section = getSectionForPosition(position);
            // 通过section来获取第一次出现字母的位置
            // int sectionPosition = getPositionForSection(section);
            // if (position == sectionPosition) {
            // vh.sortLetter.setVisibility(View.VISIBLE);
            // vh.sortLetter.setText(mb.getSortLetter());
            // } else {
            // vh.sortLetter.setVisibility(View.GONE);
            // }
            vh.name.setText(mb.getContactName());
            vh.number.setText(mb.getContactNumber());
            // vh.imageView.setImageBitmap(mb.getContactIcon());
            if (mb.isCheck()) {
                vh.checkImage.setImageResource(R.drawable.select);
            } else {
                vh.checkImage.setImageResource(R.drawable.unselect);
            }
            if (mb.getContactIcon() != null) {
                vh.contactIcon.setImageBitmap(mb.getContactIcon());
            } else {
                vh.contactIcon.setImageResource(R.drawable.default_user_avatar);
            }
            return convertView;
        }

        @Override
        public Object[] getSections() {
            return null;
        }

        /**
         * 根据分类的首字母的其第一次出现该首字母的位置
         */
        @SuppressLint("DefaultLocale")
        @Override
        public int getPositionForSection(int sectionIndex) {
            for (int i = 0; i < getCount(); i++) {
                String sortStr = mPhoneContact.get(i).getSortLetter();
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == sectionIndex) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * 根据ListView的当前位置获取分类的首字母
         */
        @Override
        public int getSectionForPosition(int position) {
            return mPhoneContact.get(position).getSortLetter().charAt(0);
        }
    }

    private class PrivacyContactTask extends AsyncTask<String, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            String flag = arg0[0];
            int count = 0;
            boolean isOtherLogs = false;
            ContentResolver cr = getContentResolver();
            if (PrivacyContactUtils.ADD_CONTACT_MODEL.equals(flag)) {
            Log.e("#############", "导入记录");
                boolean added = false;
                PrivacyContactManager pcm = PrivacyContactManager
                        .getInstance(getApplicationContext());
                for (ContactBean contact : mAddPrivacyContact) {
                    String name = contact.getContactName();
                    String number = PrivacyContactUtils.deleteOtherNumber(contact
                            .getContactNumber());
                    Bitmap contactIcon = contact.getContactIcon();
                    // 隐私联系人去重
                    String tempNumber =
                            PrivacyContactUtils.formatePhoneNumber(number);
                    boolean flagContact = false;
                    ArrayList<ContactBean> contacts = pcm.getPrivateContacts();
                    if (contacts != null && contacts.size() != 0
                            && number != null && !"".equals(number)) {
                        for (ContactBean contactBean : contacts) {
                            flagContact =
                                    contactBean.getContactNumber().contains(tempNumber);
                            if (flagContact) {
                                break;
                            }
                        }
                    }
                    if (!flagContact) {
                        ContentValues contactValues = new ContentValues();
                        contactValues.put(Constants.COLUMN_PHONE_NUMBER, number);
                        contactValues.put(Constants.COLUMN_CONTACT_NAME, name);
                        contactValues.put(Constants.COLUMN_PHONE_ANSWER_TYPE, mAnswerType);
                        byte[] icon = PrivacyContactUtils.formateImg(contactIcon);
                        contactValues.put(Constants.COLUMN_ICON, icon);
                        cr.insert(Constants.PRIVACY_CONTACT_URI, contactValues);
                        pcm.addContact(new ContactBean(0, name, number, null, contactIcon, null,
                                false, mAnswerType, null));
                        added = true;
                    }
                    if (mAddMessages == null) {
                        mAddMessages = PrivacyContactUtils.getSysMessage(
                                AddFromContactListActivity.this, cr,
                                "address LIKE ? ", new String[] {
                                    "%" + tempNumber
                                }, true);
                    } else {
                        List<MessageBean> addMessages = PrivacyContactUtils.getSysMessage(
                                AddFromContactListActivity.this, cr,
                                "address LIKE ?", new String[] {
                                    "%" + tempNumber
                                }, true);
                        mAddMessages.addAll(addMessages);
                    }
                    if (mAddCallLogs == null) {
                        mAddCallLogs = PrivacyContactUtils.getSysCallLog(
                                AddFromContactListActivity.this, cr,
                                "number LIKE ?", new String[] {
                                    "%" + tempNumber
                                });
                    } else {
                        List<ContactCallLog> addCalllog = PrivacyContactUtils.getSysCallLog(
                                AddFromContactListActivity.this,
                                cr,
                                "number LIKE ?", new String[] {
                                    "%" + tempNumber
                                });
                        mAddCallLogs.addAll(addCalllog);
                    }
                    if (!isOtherLogs) {
                        if ((mAddMessages != null && mAddMessages.size() != 0)
                                || (mAddCallLogs != null && mAddCallLogs.size() != 0)) {
                            isOtherLogs = true;
                            mLogFlag = isOtherLogs;
                        }
                    }
                    if (flagContact) {
                        if (mAddPrivacyContact.size() == 1 && mAddPrivacyContact != null) {
                            LeoEventBus
                                    .getDefaultBus()
                                    .post(
                                            new PrivacyMessageEvent(
                                                    EventId.EVENT_PRIVACY_EDIT_MODEL,
                                                    PrivacyContactUtils.ADD_CONTACT_FROM_CONTACT_NO_REPEAT_EVENT));

                            isOtherLogs = false;
                        }
                    }
                    Message messge = new Message();
                    count = count + 1;
                    messge.what = count;
                    if (messge != null && mHandler!=null) {
                        mHandler.sendMessage(messge);
                    }
                    flagContact = false;
                    if (added) {
                        // 通知更新隐私联系人列表
                        LeoEventBus
                                .getDefaultBus()
                                .post(new PrivacyEditFloatEvent(
                                        PrivacyContactUtils.PRIVACY_ADD_CONTACT_UPDATE));
                        PrivacyHelper.getInstance(getApplicationContext()).computePrivacyLevel(
                                PrivacyHelper.VARABLE_PRIVACY_CONTACT);
                        SDKWrapper.addEvent(getApplicationContext(), SDKWrapper.P1, "contactsadd",
                                "contactsadd");
                    }
                }
            } else if (PrivacyContactUtils.ADD_CALL_LOG_AND_MESSAGE_MODEL.equals(flag)) {
                List<String> addNumber = new ArrayList<String>();
                for (ContactBean contact : mAddPrivacyContact) {
                    addNumber.add(contact.getContactNumber());
                }
                // 导入短信和通话记录
                if (mAddMessages != null && mAddMessages.size() != 0) {
                    for (MessageBean message : mAddMessages) {
                        String contactNumber = message.getPhoneNumber();
                        String number = PrivacyContactUtils.deleteOtherNumber(contactNumber);
                        String name = message.getMessageName();
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
                                AddFromContactListActivity.this, message.getPhoneNumber());
                        values.put(Constants.COLUMN_MESSAGE_THREAD_ID, thread);
                        values.put(Constants.COLUMN_MESSAGE_IS_READ, isRead);
                        values.put(Constants.COLUMN_MESSAGE_TYPE, type);
                        Uri messageFlag = cr.insert(Constants.PRIVACY_MESSAGE_URI, values);
                        PrivacyContactUtils.deleteMessageFromSystemSMS("address = ?",
                                new String[] {
                                    number
                                }, AddFromContactListActivity.this);
                        if (messageFlag != null && mHandler!=null) {
                            Message messge = new Message();
                            count = count + 1;
                            messge.what = count;
                            mHandler.sendMessage(messge);
                        }
                    }
                    // 更新隐私短信
                    // for (MessageBean messageBean : messages) {
                    // String formateNumber =
                    // PrivacyContactUtils.formatePhoneNumber(messageBean
                    // .getPhoneNumber());
                    // for (String string : addNumber) {
                    // if (string.contains(formateNumber)) {
                    // pm.removeSysMessage(messageBean);
                    // }
                    // }
                    // }
                }
                // 导入通话记录
                if (mAddCallLogs != null && mAddCallLogs.size() != 0) {
                    for (ContactCallLog calllog : mAddCallLogs) {
                        String number = calllog.getCallLogNumber();
                        String name = calllog.getCallLogName();
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
                                AddFromContactListActivity.this);
                        if (callLogFlag != null && mHandler!=null) {
                            Message messge = new Message();
                            count = count + 1;
                            messge.what = count;
                            mHandler.sendMessage(messge);
                        }
                    }

                }
                if (mAddCallLogs != null && mAddCallLogs.size() != 0) {
                    LeoEventBus.getDefaultBus().post(
                            new PrivacyEditFloatEvent(
                                    PrivacyContactUtils.UPDATE_CALL_LOG_FRAGMENT));
                }
                if (mAddMessages != null && mAddMessages.size() != 0) {

                    LeoEventBus.getDefaultBus().post(
                            new PrivacyEditFloatEvent(
                                    PrivacyContactUtils.UPDATE_MESSAGE_FRAGMENT));
                }
                isOtherLogs = false;
            }
            return isOtherLogs;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                String title = getResources().getString(
                        R.string.privacy_contact_add_log_dialog_title);
                String content = getResources().getString(
                        R.string.privacy_contact_add_log_dialog_dialog_content);
                showAddContactDialog(title, content);
                mHandler = null;
            } else {
                // 通知更新隐私联系人列表
                notificationUpdatePrivacyContactList();
            }

            if (mProgressDialog != null) {
                mProgressDialog.cancel();
            }
            super.onPostExecute(result);
        }
    }

    private void showProgressDialog(int maxValue, int currentValue) {
        if (mProgressDialog == null) {
            mProgressDialog = new LEORoundProgressDialog(this);
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

    private void showAddContactDialog(String title, String content) {
        if (mAddContactDialog == null) {
            mAddContactDialog = new LEOAlarmDialog(this);
        }
        mAddContactDialog.setOnClickListener(new OnDiaogClickListener() {
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
                                    if (mProgressDialog != null) {
                                        mProgressDialog.cancel();
                                    }
                                    // 通知更新隐私联系人列表
                                    notificationUpdatePrivacyContactList();
                                    AddFromContactListActivity.this.finish();
                                } else {
                                    mProgressDialog.setProgress(currentValue);
                                }
                                super.handleMessage(msg);
                            }
                        };
                    }
                    showProgressDialog(privacyTotal, 0);
                    PrivacyContactTask task = new PrivacyContactTask();
                    task.execute(PrivacyContactUtils.ADD_CALL_LOG_AND_MESSAGE_MODEL);
                    SDKWrapper.addEvent(getApplicationContext(), SDKWrapper.P1, "contactsadd",
                            "import");
                } else if (which == 0) {
                    SDKWrapper.addEvent(getApplicationContext(), SDKWrapper.P1, "contactsadd",
                            "unimport");
                    if (mAddContactDialog != null) {
                        mAddContactDialog.cancel();
                    }
                    AddFromContactListActivity.this.finish();
                }

            }
        });
        mAddContactDialog.setCanceledOnTouchOutside(false);
        mAddContactDialog.setTitle(title);
        mAddContactDialog.setContent(content);
        mAddContactDialog.show();
    }

    private class AddContactAsyncTask extends AsyncTask<Boolean, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            mProgressBar.setVisibility(View.VISIBLE);
            mContactSideBar.setVisibility(View.GONE);
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Boolean... arg0) {
            boolean flag = arg0[0];
            if (flag) {
                mPhoneContact =
                        PrivacyContactUtils.getSysContact(AddFromContactListActivity.this,
                                getContentResolver(), null, null);
                // mPhoneContact =
                // PrivacyContactManager.getInstance(AddFromContactListActivity.this)
                // .getSysContacts();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mPhoneContact != null && mPhoneContact.size() > 0) {
                mDefaultText.setVisibility(View.GONE);
                mContactSideBar.setVisibility(View.VISIBLE);
            } else {
                mDefaultText.setVisibility(View.VISIBLE);

            }
            mProgressBar.setVisibility(View.GONE);
            mContactAdapter.notifyDataSetChanged();
            super.onPostExecute(result);
        }
    }

    public static void notificationUpdatePrivacyContactList() {
        // 通知更新隐私联系人列表
        LeoEventBus
                .getDefaultBus()
                .post(new PrivacyEditFloatEvent(
                        PrivacyContactUtils.CONTACT_EDIT_MODEL_DELETE_CONTACT_UPDATE));
    }

}
