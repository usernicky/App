
package com.leo.appmaster.privacycontact;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;

import com.leo.appmaster.Constants;
import com.leo.appmaster.R;

public class PrivacyContactUtils {
    public static final Uri SMS_INBOXS = Uri.parse("content://sms/");
    public static final Uri SYS_SMS = Uri.parse("content://sms/inbox");
    public static final Uri CONTACT_INBOXS = Uri.parse("content://icc/adn");
    public static final Uri contactUri = Phone.CONTENT_URI;
    public static final Uri CALL_LOG_URI = android.provider.CallLog.Calls.CONTENT_URI;
    public static final String ADD_CONTACT_MODEL = "add_contact_model";
    public static final String ADD_CALL_LOG_AND_MESSAGE_MODEL = "add_call_log_and_message_model";
    public static final String ADD_CONTACT_FROM_CONTACT_NO_REPEAT_EVENT = "add_conact_from_contact_no_repeat_event";
    public final static String KEY_PLAY_ANIM = "play_anim";
    public static final String TO_PRIVACY_CONTACT = "to_privacy_contact";
    public static final String TO_PRIVACY_MESSAGE_FLAG = "from_privacy_message";
    public static final String TO_PRIVACY_CALL_FLAG = "from_privacy_call";
    public static final String TO_PRIVACY_CONTACT_FLAG = "from_privacy_contact";
    public static final String FROM_MESSAGE_EVENT = "from_message_event";
    public static final String FROM_CALL_LOG_EVENT = "from_call_log_event";
    public static final String FROM_CONTACT_EVENT = "from_contact_event";
    public static final String CANCEL_EDIT_MODEL = "cancel_edit_model";
    public static final String EDIT_MODEL_OPERATION_RESTORE = "edit_model_operatioin_restore";
    public static final String CALL_LOG_EDIT_MODEL_OPERATION_DELETE = "call_log_edit_model_operatioin_delete";
    public static final String MESSAGE_EDIT_MODEL_OPERATION_DELETE = "message_edit_model_operatioin_delete";
    public static final String CONTACT_EDIT_MODEL_OPERATION_DELETE = "contact_edit_model_operatioin_delete";
    public static final String EDIT_MODEL_RESTOR_TO_SMS_CANCEL = "edit_model_restore_to_sms_cancel";
    public static final String MESSAGE_RECEIVER_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    public static final String MESSAGE_RECEIVER_ACTION2 = "android.provider.Telephony.SMS_RECEIVED_2";
    public static final String MESSAGE_RECEIVER_ACTION3 = "android.provider.Telephony.GSM_SMS_RECEIVED";
    public static final String CALL_RECEIVER_ACTION = "android.intent.action.PHONE_STATE";
    public static final String PHONE_BOOT_COMPLETE = "android.intent.action.BOOT_COMPLETED";
    public static final String UPDATE_MESSAGE_FRAGMENT = "update_message_fragment";
    public static final String UPDATE_CALL_LOG_FRAGMENT = "update_call_log_fragment";
    public static final String RECEIVER_NOTIFICATION_OBSERVER_NO_INSTER_CALL_LOG = "receiver_notification_observer_no_inster_call_log";
    public static final String CONTACT_CALL_LOG = "contact_call_log";
    public static final String DELETE_MESSAGE = "delete_message";
    public static final String CONTACT_DETAIL_DELETE_LOG_UPDATE_CALL_LOG_LIST = "contact_detail_detele_log_update_call_log_list";
    public static final String RED_TIP = "red_tip";
    public static final String SENT_SMS_ACTION = "SENT_SMS_ACTION";
    public static final int RECEIVE_MESSAGE = 1;
    public static final int SEND_MESSAGE = 2;
    public static final String NORMAL_ANSWER_TYPE = "normal_answer";
    public static final String HANG_UP_ANSWER_TYPE = "hang_up_answer";
    public static final String CONTACT_DETAIL_DELETE_LOG = "contact_detail_delete_log";
    public static final String CONTACT_EDIT_MODEL_DELETE_CONTACT_UPDATE = "contact_edit_model_delete_contact_update";
    public static final String CONTACT_DETAIL_DELETE_LOG_UPDATE_MESSAGE_LIST = "contact_detail_detele_log_update_message_list";
    public static final String RED_TIP_FLAG_MESSAGE = "message_flag";
    public static final String RED_TIP_FLAG_CALL_LOG = "call_log_flag";
    public static final String MESSAGE_PRIVACY_RECEIVER_MESSAGE = "privacy_receiver_message";
    public static final String MESSAGE_PRIVACY_RECEIVER_CALL_LOG = "privacy_receiver_call_log";
    public static final String MESSAGE_PRIVACY_INTERCEPT_NOTIFICATION = "message_privacy_intercept_notification";

    public static final String PRIVACY_RECEIVER_MESSAGE_NOTIFICATION = "privacy_receiver_message_notification";
    public static final String PRIVACY_RECEIVER_CALL_LOG_NOTIFICATION = "privacy_receiver_call_log_notification";
    public static final String PRIVACY_MESSAGE_ITEM_RUNING_NOTIFICATION = "message_item_runing_notification";
    public static final String PRIVACY_ADD_CONTACT_UPDATE = "add_contact_update";
    public static final String PRIVACY_ALL_CALL_NOTIFICATION_HANG_UP = "all_call_notification_hang_up";
    public static final String PRIVACY_INTERCEPT_CONTACT_EVENT = "intercept_contact_event";
    public static final int ID = 0;

    public static final int DATE = 1;

    public static final int MESSAGE_COUNT = 2;

    public static final int RECIPIENT_IDS = 3;

    public static final int SNIPPET = 4;

    public static final int SNIPPET_CS = 5;

    public static final int READ = 6;

    public static final int TYPE = 7;

    public static final int ERROR = 8;

    public static final int HAS_ATTACHMENT = 9;
    String[] projecttion = new String[] {
            Phone.CONTACT_ID,
            Phone.DISPLAY_NAME,
            Phone.LOOKUP_KEY,
            Phone.NUMBER,
            Phone.SORT_KEY_PRIMARY,
            Phone.STARRED
    };
    String selection = Contacts.IN_VISIBLE_GROUP + "=1 and "
            + Phone.HAS_PHONE_NUMBER + "=1 and "
            + Phone.DISPLAY_NAME + " IS NOT NULL";

    /**
     * getSysMessage
     * 
     * @return
     */
    public static List<MessageBean> getSysMessage(Context context, ContentResolver cr, String
            selection,
            String[] selectionArgs, boolean isItemFlag) {
        List<MessageBean> messages = new ArrayList<MessageBean>();
        Map<String, MessageBean> messageList = new HashMap<String,
                MessageBean>();
        Cursor cur = null;
        try {
            cur = cr.query(SMS_INBOXS, null, selection, selectionArgs,
                    "_id desc");
            if (cur != null) {
                while (cur.moveToNext()) {
                    MessageBean mb = new MessageBean();
                    String number = cur.getString(cur.getColumnIndex("address"));
                    Bitmap icon = PrivacyContactUtils.getContactIconFromSystem(
                            context, number);
                    if (icon != null) {
                        mb.setContactIcon(icon);
                    } else {
                        mb.setContactIcon(((BitmapDrawable) context.getResources().getDrawable(
                                R.drawable.default_user_avatar)).getBitmap());
                    }
                    String threadId = cur.getString(cur.getColumnIndex("thread_id"));
                    /**
                     * getContactNameFromNumber
                     */
                    String name =
                            PrivacyContactUtils.getContactNameFromNumber(cr, number);
                    String body = cur.getString(cur.getColumnIndex("body"));
                    int type = cur.getInt(cur.getColumnIndex("type"));
                    int isRead = cur.getInt(cur.getColumnIndex("read"));
                    Date date = new
                            Date(cur.getLong(cur.getColumnIndex("date")));
                    SimpleDateFormat sfd = new
                            SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    String time = sfd.format(date);
                    mb.setMessageBody(body);
                    mb.setMessageName(name);
                    mb.setPhoneNumber(number);
                    mb.setMessageType(type);
                    mb.setMessageIsRead(1);
                    mb.setMessageThreadId(threadId);
                    mb.setMessageTime(time);
                    if (number != null) {
                        if (!isItemFlag) {
                            // isItemFlag:true--详细列表，false--列表
                            if (threadId != null && !threadId.equals("")) {
                                if (!messageList.containsKey(threadId)) {
                                    messageList.put(threadId, mb);
                                }
                            }
                        } else {
                            messages.add(mb);
                        }
                    }
                }
                if (!isItemFlag) {
                    Iterable<MessageBean> it = messageList.values();
                    for (MessageBean mb : it) {
                        messages.add(mb);
                    }
                }
            }
        } catch (Exception e) {

        } finally {
            if (cur != null) {
                cur.close();
            }
        }

        return messages;
    }

    /**
     * getSysContact
     * 
     * @return
     */
    public static List<ContactBean> getSysContact(Context context, ContentResolver cr,
            String selection,
            String[] selectionArgs) {
        selection = Contacts.IN_VISIBLE_GROUP + "=1 and "
                + Phone.HAS_PHONE_NUMBER + "=1 and "
                + Phone.DISPLAY_NAME + " IS NOT NULL";
        List<ContactBean> contacts = new ArrayList<ContactBean>();
        Cursor phoneCursor = null;
        try {
            phoneCursor = cr.query(contactUri,
                    null, selection, null, Phone.SORT_KEY_PRIMARY);
            if (phoneCursor != null) {
                while (phoneCursor.moveToNext()) {
                    // get phonenumber
                    String phoneNumber = phoneCursor
                            .getString(phoneCursor.getColumnIndex(Phone.NUMBER));
                    // IF IS NULL CONTINUE
                    if (TextUtils.isEmpty(phoneNumber)) {
                        continue;
                    }
                    // get name
                    String contactName = phoneCursor.getString(phoneCursor
                            .getColumnIndex(Phone.DISPLAY_NAME));
                    Long contactid =
                            phoneCursor.getLong(phoneCursor.getColumnIndex(Phone.CONTACT_ID));
                    Long photoid =
                            phoneCursor.getLong(phoneCursor.getColumnIndex("photo_id"));
                    Bitmap contactPhoto = null;
                    if (photoid > 0) {
                        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                                contactid);
                        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(
                                cr,
                                uri);
                        contactPhoto = BitmapFactory.decodeStream(input);
                    }
                    ContactBean cb = new ContactBean();
                    cb.setContactName(contactName);
                    cb.setContactNumber(phoneNumber);
                    cb.setContactIcon(contactPhoto);
//                    cb.setSortLetter(phoneCursor.getString(phoneCursor
//                            .getColumnIndex(Phone.SORT_KEY_PRIMARY)));
                    if (phoneNumber != null) {
                        contacts.add(cb);
                    } else {
                        contacts = null;
                    }
                }
            }
        } catch (Exception e) {

        } finally {
            if (phoneCursor != null) {
                phoneCursor.close();
            }
        }

        return contacts;
    }

    /**
     * getSysCallLog
     * 
     * @return
     */
    public static List<ContactCallLog> getSysCallLog(Context context, ContentResolver cr,
            String selection,
            String[] selectionArgs) {
        List<ContactCallLog> calllogs = new ArrayList<ContactCallLog>();
        Map<String, ContactCallLog> calllog = new HashMap<String, ContactCallLog>();
        try {
            Cursor cursor = cr.query(CALL_LOG_URI, null, selection, selectionArgs,
                    CallLog.Calls.DEFAULT_SORT_ORDER);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    ContactCallLog callLog = new ContactCallLog();
                    int count = cursor.getCount();
                    String number = cursor.getString(cursor.getColumnIndex("number"));
                    Bitmap icon = PrivacyContactUtils.getContactIconFromSystem(
                            context, number);
                    if (icon != null) {
                        callLog.setContactIcon(icon);
                    } else {
                        callLog.setContactIcon(((BitmapDrawable) context.getResources()
                                .getDrawable(
                                        R.drawable.default_user_avatar)).getBitmap());
                    }
                    String name = cursor.getString(cursor.getColumnIndex("name"));
                    Date date = new Date(Long.parseLong(cursor.getString(cursor
                            .getColumnIndex(CallLog.Calls.DATE))));
                    SimpleDateFormat sfd = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    String time = sfd.format(date);
                    int type = (cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE)));
                    callLog.setCallLogCount(count);
                    // callLog.setCallLogDuraction();
                    callLog.setCallLogName(name);
                    callLog.setCallLogNumber(number);
                    callLog.setClallLogDate(time);
                    callLog.setClallLogType(type);
                    if (number != null) {
                        if (callLog != null) {
                            if (!calllog.containsKey(number)) {
                                calllog.put(number, callLog);
                            }
                        }
                    }
                }
                Iterable<ContactCallLog> it = calllog.values();
                for (ContactCallLog contactCallLog : it) {
                    calllogs.add(contactCallLog);
                }
                cursor.close();
            }
        } catch (Exception e) {

        }

        return calllogs;
    }

    /**
     * 得到手机SIM卡联系人信息 private static final String[] PHONES_PROJECTION = new
     * String[] { Phone.DISPLAY_NAME, Phone.NUMBER,
     * Photo.PHOTO_ID,Phone.CONTACT_ID };
     */
    @SuppressWarnings("unused")
    private List<ContactBean> getSIMContacts(ContentResolver cr, String selection,
            String[] selectionArgs) {
        List<ContactBean> mSIMSContact = new ArrayList<ContactBean>();
        Cursor phoneCursor = null;
        try {
            phoneCursor = cr.query(CONTACT_INBOXS, null, selection, selectionArgs,
                    null);
            if (phoneCursor != null) {
                while (phoneCursor.moveToNext()) {
                    ContactBean cb = new ContactBean();
                    String phoneNumber = phoneCursor
                            .getString(phoneCursor.getColumnIndex(Phone.NUMBER));
                    if (TextUtils.isEmpty(phoneNumber))
                        continue;
                    String contactName = phoneCursor
                            .getString(phoneCursor.getColumnIndex(Phone.DISPLAY_NAME));
                    cb.setContactName(contactName);
                    cb.setContactNumber(phoneNumber);
                    mSIMSContact.add(cb);
                }
            } else {
                mSIMSContact = null;
            }
        } catch (Exception e) {

        } finally {
            if (phoneCursor != null) {
                phoneCursor.close();
            }
        }
        return mSIMSContact;
    }

    public static final String PRIVACY_MESSAGE_CONTACT_NUMBER = "privacy_message_contact_number";

    public static String getPrivacyMessageContact(List<String> numbers) {
        StringBuilder sb = new StringBuilder();
        for (String string : numbers) {
            sb.append(string + "^");
        }
        return sb.toString();
    }

    public static List<String> splitPrivacyMessageContact(String numberString) {
        List<String> numbers = new ArrayList<String>();
        String[] numberArray = numberString.split("^");
        for (String string : numberArray) {
            numbers.add(string);
        }
        return numbers;
    }

    public static boolean saveNumberPreference(String number, Context context) {
        boolean flag = false;
        SharedPreferences preferences = context.getSharedPreferences("privacy_message_contact_leo",
                Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        try {
            editor.putString(PRIVACY_MESSAGE_CONTACT_NUMBER, number);
            editor.commit();
            flag = true;
        } catch (Exception e) {

        }
        return flag;
    }

    public static boolean getNumberPreference(String number, Context context) {
        boolean flag = false;
        SharedPreferences preferences = context.getSharedPreferences("privacy_message_contact_leo",
                Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        try {
            editor.putString(PRIVACY_MESSAGE_CONTACT_NUMBER, number);
            editor.commit();
            flag = true;
        } catch (Exception e) {

        }
        return flag;
    }

    public static String getPrivacyMessageNumber(List<String> numbers) {
        StringBuilder sb = new StringBuilder();
        for (String string : numbers) {
            sb.append("," + string);
        }
        String str = sb.toString().substring(1);
        return str;
    }

    /**
     * String reverse
     */
    public static String stringReverse(String str) {
        StringBuffer sbf = new StringBuffer(str);
        return sbf.reverse().toString();
    }

    // 格式化电话号码
    public static String deleteOtherNumber(String number) {
        String deleteOtherNumber = null;
        if (number != null) {
            String deleteSpaceNumber = number.replace(" ", "");
            deleteOtherNumber = deleteSpaceNumber.replace("-", "");
        }
        return deleteOtherNumber;
    }

    public static String formatePhoneNumber(String number) {
        String temp = deleteOtherNumber(number);
        String reverseStr = null;
        if (temp != null) {
            int strLength = temp.length();
            if (strLength >= 7) {
                String tempStr = stringReverse(temp);
                String subNumber = tempStr.substring(0, 7);
                reverseStr = stringReverse(subNumber);
            } else {
                reverseStr = temp;
            }
        }
        return reverseStr;
    }

    /**
     * getContactNameFromNumber
     * 
     * @author run
     */
    public static String getContactNameFromNumber(ContentResolver contentResolver, String number) {
        // String reverseStr = formatePhoneNumber(number);
        String phoneName = null;

        // Cursor cursor = contentResolver.query(Uri.withAppendedPath(
        // PhoneLookup.CONTENT_FILTER_URI, number), new String[] {
        // PhoneLookup._ID,
        // PhoneLookup.NUMBER,
        // PhoneLookup.DISPLAY_NAME,
        // PhoneLookup.TYPE, PhoneLookup.LABEL
        // }, null, null, null);
        Cursor cursor = contentResolver.query(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, number), new String[] {
                PhoneLookup.DISPLAY_NAME,
        }, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                phoneName = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
            }
            cursor.close();
        }
        if (phoneName == null || "".equals(phoneName)) {
            phoneName = number;
        }
        return phoneName;
    }

    /**
     * getContactNameFromId
     * 
     * @author run
     */
    public static String getContactNameFromId(ContentResolver contentResolver, String id) {

        String phoneName = null;
        if (id != null && contentResolver != null) {
            Cursor cursor = null;
            try {

                cursor = contentResolver.query(Phone.CONTENT_URI, new String[] {
                        Phone.DISPLAY_NAME
                }, Phone.CONTACT_ID + " = ?  ", new String[] {
                        id
                }, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        phoneName = cursor.getString(cursor.getColumnIndex("display_name"));
                    } else {
                        phoneName = "未知号码";
                    }
                }
            } catch (Exception e) {
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return phoneName;
    }

    // insert message to system
    public static void insertMessageToSystemSMS(ContentValues values, Context context) {
        try {
            context.getContentResolver().insert(SYS_SMS, values);
        } catch (Exception e) {

        }
    }

    // delete message from system
    public static void deleteMessageFromSystemSMS(String selection, String[] selectionArgs,
            Context context) {
        try {
            context.getContentResolver().delete(SMS_INBOXS, selection, selectionArgs);
        } catch (Exception e) {

        }
    }

    // delete call_log from system
    public static int deleteCallLogFromSystem(String selection, String selectionArgs,
            Context context) {
        int flag = 0;
        try {
            String string = formatePhoneNumber(selectionArgs);
            flag = context.getContentResolver().delete(CALL_LOG_URI, selection, new String[] {
                    "%" + string
            });
        } catch (Exception e) {

        }
        return flag;
    }

    // query myself contact_table
    public static List<ContactBean> queryContactFromMySelfTable(ContentResolver mdb, Uri uri,
            String selection, String[] selectionArgs) {
        List<ContactBean> privacyContacts = new ArrayList<ContactBean>();
        Cursor cur = mdb.query(uri, null, selection, selectionArgs, null);
        if (cur != null) {
            // count = cur.getCount();
            while (cur.moveToNext()) {
                String number = cur.getString(cur
                        .getColumnIndex(Constants.COLUMN_PHONE_NUMBER));
                int contactId = cur.getInt(cur.getColumnIndex(Constants.COLUMN_CONTACT_ID));
                ContactBean contact = new ContactBean();
                contact.setContactNumber(number);
                contact.setContactId(contactId);
                privacyContacts.add(contact);
            }
            cur.close();
        } else {
            privacyContacts = null;
        }
        return privacyContacts;
    }

    // query myself message_table
    public static List<MessageBean> queryMySelfMessageTable(ContentResolver mdb,
            String selection, String[] selectionArgs) {
        List<MessageBean> privacyMessages = new ArrayList<MessageBean>();
        Cursor cur = mdb.query(Constants.PRIVACY_MESSAGE_URI, null, selection, selectionArgs, null);
        if (cur != null) {
            while (cur.moveToNext()) {
                MessageBean mb = new MessageBean();
                String number = cur.getString(cur
                        .getColumnIndex(Constants.COLUMN_MESSAGE_PHONE_NUMBER));
                String threadId = cur.getString(cur
                        .getColumnIndex(Constants.COLUMN_MESSAGE_THREAD_ID));
                String name = cur.getString(cur
                        .getColumnIndex(Constants.COLUMN_MESSAGE_CONTACT_NAME));
                String body = cur.getString(cur.getColumnIndex(Constants.COLUMN_MESSAGE_BODY));
                int type = cur.getInt(cur.getColumnIndex(Constants.COLUMN_MESSAGE_TYPE));
                int isRead = cur.getInt(cur.getColumnIndex(Constants.COLUMN_MESSAGE_IS_READ));
                String time = cur.getString(cur.getColumnIndex(Constants.COLUMN_MESSAGE_DATE));
                mb.setMessageBody(body);
                mb.setMessageName(name);
                mb.setPhoneNumber(number);
                mb.setMessageType(type);
                mb.setMessageIsRead(isRead);
                mb.setMessageThreadId(threadId);
                mb.setMessageTime(time);
                privacyMessages.add(mb);
            }
            cur.close();
        } else {
            privacyMessages = null;
        }
        return privacyMessages;
    }

    // query myself call_log_table
    public static List<ContactCallLog> queryMySelfCallLogTable(ContentResolver mdb,
            String selection, String[] selectionArgs) {
        List<ContactCallLog> privacyCallLogs = new ArrayList<ContactCallLog>();
        Cursor cursor = mdb.query(Constants.PRIVACY_CALL_LOG_URI, null, selection, selectionArgs,
                null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ContactCallLog callLog = new ContactCallLog();
                String number = cursor.getString(cursor
                        .getColumnIndex(Constants.COLUMN_CALL_LOG_PHONE_NUMBER));
                String name = cursor.getString(cursor
                        .getColumnIndex(Constants.COLUMN_CALL_LOG_CONTACT_NAME));
                String date = cursor
                        .getString(cursor.getColumnIndex(Constants.COLUMN_CALL_LOG_DATE));
                callLog.setClallLogDate(date);
                cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_CALL_LOG_TYPE));
                callLog.setCallLogName(name);
                callLog.setCallLogNumber(number);
                privacyCallLogs.add(callLog);
            }
            cursor.close();
        } else {
            privacyCallLogs = null;
        }
        return privacyCallLogs;
    }

    // query myself count
    public static int queryLogFromMySelg(ContentResolver cr, Uri uri, String selection,
            String[] selectionArgs) {
        int count = 0;
        Cursor cur = cr.query(uri, null, selection, selectionArgs, null);
        if (cur != null) {
            count = cur.getCount();
        }
        return count;

    }

    // delete myself
    public static int deleteMessageFromMySelf(ContentResolver mdb, Uri uri,
            String selection, String[] selectionArgs) {
        int number = mdb.delete(uri, selection, selectionArgs);
        return number;

    }

    // delete contact from myself
    public static int deleteContactFromMySelf(String selection, String selectionArgs,
            Context context) {
        int number = -1;
        try {
            number = context.getContentResolver().delete(Constants.PRIVACY_CONTACT_URI, selection,
                    new String[] {
                        selectionArgs
                    });
        } catch (Exception e) {

        }
        return number;
    }

    // delete call_log from myself
    public static int deleteCallLogFromMySelf(String selection, String selectionArgs,
            Context context) {
        int number = -1;
        try {
            number = context.getContentResolver().delete(Constants.PRIVACY_CALL_LOG_URI, selection,
                    new String[] {
                        selectionArgs
                    });
        } catch (Exception e) {

        }

        return number;
    }

    // inset call_log_table
    public static void insertCallLogToSystem(ContentResolver cr, ContentValues values)
    {
        try {
            cr.insert(CALL_LOG_URI, values);
        } catch (Exception e) {
        }

    }

    // 将图片转换成字节数组
    public static byte[] formateImg(Bitmap bmp)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (bmp != null) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        }
        return baos.toByteArray();
    }

    // 从数据库中读取图片转换成Bitmap类型
    public static Bitmap getBmp(byte[] in)
    {
        Bitmap bmpout = BitmapFactory.decodeByteArray(in, 0, in.length);
        return bmpout;
    }

    // 通话记录时间排序
    public static Comparator<ContactCallLog> mCallLogCamparator = new Comparator<ContactCallLog>() {

        public final int compare(ContactCallLog a, ContactCallLog b) {
            if (new Date(a.getClallLogDate()).before(new Date(b.getClallLogDate())))
                return 1;
            if (new Date(a.getClallLogDate()).after(new Date(b.getClallLogDate())))
                return -1;
            return 0;
        }
    };
    // 短信时间排序
    public static Comparator<MessageBean> mMessageCamparator = new Comparator<MessageBean>() {

        public final int compare(MessageBean a, MessageBean b) {
            if (new Date(a.getMessageTime()).before(new Date(b.getMessageTime())))
                return 1;
            if (new Date(a.getMessageTime()).after(new Date(b.getMessageTime())))
                return -1;
            return 0;
        }
    };
    // 短信时间升序排序
    public static Comparator<MessageBean> mMessageAscCamparator = new Comparator<MessageBean>() {

        public final int compare(MessageBean a, MessageBean b) {
            if (new Date(a.getMessageTime()).before(new Date(b.getMessageTime())))
                return -1;
            if (new Date(a.getMessageTime()).after(new Date(b.getMessageTime())))
                return 1;
            return 0;
        }
    };

    // 从隐私联系人中获取联系人头像
    public static Bitmap getContactIcon(Context context, String number) {
        Bitmap contactIcon = null;
        String formateNumber = PrivacyContactUtils.formatePhoneNumber(number);
        Cursor cur = null;
        try {
            cur = context.getContentResolver().query(Constants.PRIVACY_CONTACT_URI, null,
                    "contact_phone_number LIKE ? ",
                    new String[] {
                        "%" + formateNumber
                    }, null);
            if (cur != null) {
                while (cur.moveToNext()) {
                    byte[] icon = cur.getBlob(cur.getColumnIndex(Constants.COLUMN_ICON));
                    if (icon != null) {
                        contactIcon = PrivacyContactUtils.getBmp(icon);
                    } else {
                        BitmapDrawable drawable = (BitmapDrawable) context.getResources()
                                .getDrawable(
                                        R.drawable.default_user_avatar);
                        contactIcon = drawable.getBitmap();
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return contactIcon;
    }

    // 从系统中获取联系人头像
    public static Bitmap getContactIconFromSystem(Context context, String number) {
        Bitmap contactIcon = null;
        String formateNumber = PrivacyContactUtils.formatePhoneNumber(number);
        Cursor cur = context.getContentResolver().query(contactUri, null,
                Phone.NUMBER + " LIKE ? ",
                new String[] {
                    "%" + formateNumber
                }, null);
        if (cur != null) {
            while (cur.moveToNext()) {
                Long contactid =
                        cur.getLong(cur.getColumnIndex(Phone.CONTACT_ID));
                Long photoid =
                        cur.getLong(cur.getColumnIndex("photo_id"));
                if (photoid > 0) {
                    Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                            contactid);
                    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(
                            context.getContentResolver(),
                            uri);
                    contactIcon = BitmapFactory.decodeStream(input);
                }
            }
            cur.close();
        }
        return contactIcon;
    }

    // 查询短信会话列表
    public static List<MessageBean> queryMessageList(Context context) {
        List<MessageBean> messageList = new ArrayList<MessageBean>();
        SimpleDateFormat sfd = new
                SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        // 查询canonical_address表
        Cursor cur = context.getContentResolver().query(
                Uri.parse("content://mms-sms/canonical-addresses"), null, null, null, "_id desc");
        if (cur != null && cur.getCount() > 0) {
            cur.moveToFirst();
            while (cur.moveToNext()) {
                MessageBean message = new MessageBean();
                String id = String.valueOf(cur.getInt(cur.getColumnIndex("_id")));
                // 通过_id查询thread表中对应的会话
                Cursor cr = null;
                try {
                    cr = context.getContentResolver().query(PrivacyContactUtils.SMS_INBOXS,
                            new String[] {
                                " * from threads--"
                            }, null, null, null);
                } catch (Exception e) {

                }
                String number = cur.getString(cur.getColumnIndex("address"));
                /**
                 * getContactNameFromNumber
                 */

                String name =
                        PrivacyContactUtils.getContactNameFromNumber(context.getContentResolver(),
                                number);
                if (cr != null && cr.getCount() > 0) {
                    while (cr.moveToNext()) {
                        long date = cr.getLong(DATE);
                        String snippet = cr.getString(SNIPPET);
                        String recipIDs = cr.getString(RECIPIENT_IDS);
                        if (recipIDs != null && recipIDs.equals(id)) {
                            message.setMessageBody(snippet);
                            message.setMessageTime(sfd.format(date));
                            break;
                        }
                        long messageId = cr.getLong(ID);
                        long msgCount = cr.getLong(MESSAGE_COUNT);
                        long snippetCS = cr.getLong(SNIPPET_CS);
                        long read = cr.getLong(READ);
                        long type = cr.getLong(TYPE);
                        long hasAttach = cr.getLong(HAS_ATTACHMENT);
                    }
                }
                message.setMessageName(name);
                if (name == null || "".equals(name)) {
                    message.setMessageName(number);
                } else {
                    message.setMessageName(name);
                }
                Bitmap icon = getContactIconFromSystem(context, number);
                if (icon != null) {
                    message.setContactIcon(icon);
                } else {
                    message.setContactIcon(((BitmapDrawable) context.getResources().getDrawable(
                            R.drawable.default_user_avatar)).getBitmap());
                }
                message.setPhoneNumber(number);
                messageList.add(message);
                cr.close();
            }
        }
        cur.close();
        return messageList;
    }

    /**
     * 返回联系人头像。
     * 
     * @param contactId 联系人的Id。
     * @return 返回小型头像;如果未能查询到，则返回的是一个null值。
     */
    public static Bitmap getContactPhoto(Cursor cr, Context context) {
        Long contactid =
                cr.getLong(cr.getColumnIndex(Phone.CONTACT_ID));
        Long photoid =
                cr.getLong(cr.getColumnIndex("photo_id"));
        Bitmap contactPhoto = null;
        if (photoid > 0) {
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                    contactid);
            InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(
                    context.getContentResolver(), uri);
            contactPhoto = BitmapFactory.decodeStream(input);
        }

        return contactPhoto;
    }

    public static int queryMessageFromNumber(ContentResolver cr, String number) {
        int count = 0;
        if (number != null && !"".equals(number)) {
            Cursor cursor = cr.query(Constants.PRIVACY_MESSAGE_URI, null,
                    "contact_phone_number LIKE ? ", new String[] {
                        "%" + number
                    }, null);
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
            }
        }
        return count;
    }

    // 标记为已读
    public static void updateMessageMyselfIsRead(int read, String selection,
            String[] selectionArgs, Context context) {
        ContentValues values = new ContentValues();
        values.put("message_is_read", read);
        context.getContentResolver().update(Constants.PRIVACY_MESSAGE_URI,
                values, selection,
                selectionArgs);
    }

    // 查询自定义短信列表thead_id
    public static int queryMySelfMessageThreadId(Context context, String selection,
            String selectionArgs) {
        int threadId = -1;
        Cursor cur = context.getContentResolver().query(Constants.PRIVACY_MESSAGE_URI, null,
                selection, new String[] {
                    selectionArgs
                }, null);
        if (cur != null && cur.getCount() > 0) {
            while (cur.moveToNext()) {
                threadId = cur.getInt(cur.getColumnIndex(Constants.COLUMN_MESSAGE_THREAD_ID));
                break;
            }
        }
        return threadId;
    }

    public static List<ContactBean> loadPrivateContacts(Context mContext) {
        List<ContactBean> mContacts = new ArrayList<ContactBean>();
        Cursor cur = mContext.getContentResolver().query(Constants.PRIVACY_CONTACT_URI, null,
                null, null, "_id desc");
        if (cur != null) {
            while (cur.moveToNext()) {
                ContactBean mb = new ContactBean();
                String number = cur.getString(cur
                        .getColumnIndex(Constants.COLUMN_PHONE_NUMBER));
                String name = cur.getString(cur
                        .getColumnIndex(Constants.COLUMN_CONTACT_NAME));
                int answerType = cur.getInt(cur
                        .getColumnIndex(Constants.COLUMN_PHONE_ANSWER_TYPE));
                byte[] icon = cur.getBlob(cur.getColumnIndex(Constants.COLUMN_ICON));
                switch (answerType) {
                    case 0:
                        mb.setAnswerStatus(mContext
                                .getString(R.string.privacy_contact_activity_input_checkbox_hangup));
                        break;
                    case 1:
                        mb.setAnswerStatus(mContext
                                .getString(R.string.privacy_contact_activity_input_checkbox_normal));
                        break;
                    default:
                        break;
                }
                mb.setContactName(name);
                mb.setContactNumber(number);
                if (icon != null) {
                    Bitmap contactIcon = PrivacyContactUtils.getBmp(icon);
                    mb.setContactIcon(contactIcon);
                } else {
                    BitmapDrawable drawable = (BitmapDrawable) mContext.getResources()
                            .getDrawable(
                                    R.drawable.default_user_avatar);
                    mb.setContactIcon(drawable.getBitmap());
                }
                mb.setAnswerType(answerType);
                mContacts.add(mb);
            }
            cur.close();
        }
        return mContacts;
    }

    public static int queryContactId(Context context, String number) {
        int threadId = 0;
        if (number != null && !"".equals(number)) {
            String formateNumber = PrivacyContactUtils.formatePhoneNumber(number);
            List<ContactBean> contacts = PrivacyContactUtils.queryContactFromMySelfTable(
                    context.getContentResolver(),
                    Constants.PRIVACY_CONTACT_URI,
                    Constants.COLUMN_PHONE_NUMBER + " LIKE ? ", new String[] {
                        "%" + formateNumber
                    });
            if (contacts != null && contacts.size() > 0) {
                for (ContactBean contactBean : contacts) {
                    threadId = contactBean.getContactId();
                    break;
                }
            }
        }
        return threadId;
    }

    /**
     * 分页查询系统联系人信息
     * 
     * @param pageSize 每页最大的数目
     * @param currentOffset 当前的偏移量
     * @return
     */
    public static List<ContactBean> getContactsByPage(int pageSize, int currentOffset,
            Context context) {

        List<ContactBean> contacts = new ArrayList<ContactBean>();
        Cursor phoneCursor = null;
        try {
            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            phoneCursor = context.getContentResolver().query(
                    uri,
                    null,
                    null,
                    null,
                    " sort_key COLLATE LOCALIZED asc limit  " + pageSize + " offset "
                            + currentOffset);
            if (phoneCursor != null) {
                while (phoneCursor.moveToNext()) {
                    // get phonenumber
                    String phoneNumber = phoneCursor
                            .getString(phoneCursor.getColumnIndex(Phone.NUMBER));
                    // IF IS NULL CONTINUE
                    if (TextUtils.isEmpty(phoneNumber)) {
                        continue;
                    }
                    // get name
                    String contactName = phoneCursor.getString(phoneCursor
                            .getColumnIndex(Phone.DISPLAY_NAME));
                    Long contactid =
                            phoneCursor.getLong(phoneCursor.getColumnIndex(Phone.CONTACT_ID));
                    Long photoid =
                            phoneCursor.getLong(phoneCursor.getColumnIndex("photo_id"));
                    Bitmap contactPhoto = null;
                    if (photoid > 0) {
                        Uri uriContact = ContentUris.withAppendedId(
                                ContactsContract.Contacts.CONTENT_URI,
                                contactid);
                        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(
                                context.getContentResolver(),
                                uriContact);
                        contactPhoto = BitmapFactory.decodeStream(input);
                    }
                    ContactBean cb = new ContactBean();
                    cb.setContactName(contactName);
                    cb.setContactNumber(phoneNumber);
                    cb.setContactIcon(contactPhoto);
                    if (phoneNumber != null) {
                        contacts.add(cb);
                    }
                }

            }
        } catch (Exception e) {
        } finally {
            if (phoneCursor != null) {
                phoneCursor.close();
            }
        }

        return contacts;
    }
}
