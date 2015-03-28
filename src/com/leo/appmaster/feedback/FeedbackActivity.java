
package com.leo.appmaster.feedback;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.appmaster.R;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.CommonTitleBar;
import com.leo.appmaster.ui.LeoPopMenu;
import com.leo.appmaster.ui.LeoPopMenu.LayoutStyles;
import com.leo.appmaster.ui.dialog.LEOMessageDialog;


public class FeedbackActivity extends BaseActivity implements OnClickListener, OnFocusChangeListener {

    private static final String EMAIL_EXPRESSION = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
    
    private View mBtnCommit;
    private EditText mEditContent;
    private EditText mEditEmail;
    private ImageView mEmailImg;
    private View mEmailLayout;
    private TextView mCategory;
    private ImageView mCategoryImg;
    private View mCategoryLayout;

    private LeoPopMenu mLeoPopMenu;

    private LEOMessageDialog mMessageDialog;

    private final static int[] sCategoryIds = {
        R.string.category_lock, R.string.pravicy_protect,
        R.string.app_manager, R.string.category_other
    };

    private final ArrayList<String> mCategories = new ArrayList<String>();
    private final ArrayList<String> mEmails = new ArrayList<String>();
    
    private int mCategoryPos = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        initEmails();
        
        initUi();

        // check if any data not submitted
        checkPendingData();
    }

    private void initEmails() {
        AccountManager am = AccountManager.get(getApplicationContext());
        Account[] accounts = am.getAccounts();
        for(Account a : accounts) {
            if(a.name != null && a.name.matches(EMAIL_EXPRESSION) && !mEmails.contains(a.name)) {
                mEmails.add(a.name);
            }
        }
    }

    private void initUi() {
        CommonTitleBar titleBar = (CommonTitleBar) findViewById(R.id.layout_title_bar);
        titleBar.setTitle(R.string.feedback);
        titleBar.openBackView();
        mEditContent = (EditText) findViewById(R.id.feedback_content);
        mEmailLayout = findViewById(R.id.feedback_email_layout);
        mEditEmail = (EditText) findViewById(R.id.feedback_email);
        mEditEmail.setOnFocusChangeListener(this);
        mEmailImg =  (ImageView) findViewById(R.id.feedback_email_arrow);
        mEmailImg.setOnClickListener(this);
        mEmailImg.setVisibility(mEmails.size() > 1 ? View.VISIBLE : View.GONE);
        mCategoryLayout = findViewById(R.id.feedback_category_layout);
        mCategoryLayout.setOnClickListener(this);
        mCategory = (TextView) findViewById(R.id.feedback_category_title);
        mCategoryImg = (ImageView) findViewById(R.id.feedback_category_arrow);
        mBtnCommit = findViewById(R.id.feedback_commit);
        mBtnCommit.setOnClickListener(this);
        for (int i = 0; i < sCategoryIds.length; i++) {
            mCategories.add(getString(sCategoryIds[i]));
        }
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                checkCommitable();
            }
        };
        mEditEmail.addTextChangedListener(textWatcher);
        mEditContent.addTextChangedListener(textWatcher);
        mEditContent.requestFocus();
    }

    private void checkPendingData() {
        SharedPreferences perference = PreferenceManager.getDefaultSharedPreferences(this);
        mEditContent.setText(perference.getString(FeedbackHelper.KEY_CONTENT, ""));
        String email = "";
        if(mEmails.size() > 0) {
            email = mEmails.get(0);
        }
        String currentEmail = perference.getString(FeedbackHelper.KEY_EMAIL, email);
        mEditEmail.setText(currentEmail.isEmpty() ? email : currentEmail);
        try {
            mCategoryPos = perference.getInt(FeedbackHelper.KEY_CATEGORY, -1);
        } catch(Exception e) {
           
        }
        if (mCategoryPos >= 0 && mCategoryPos < mCategories.size()) {
            mCategory.setText(mCategories.get(mCategoryPos));
            mCategory.setTag(1);
        }
    }

    private void checkCommitable() {
        boolean commitable = true;
        if (mCategory.getTag() == null) {
            commitable = false;
        } else {
            String email = mEditEmail.getText().toString().trim();
            if (email.isEmpty()) {
                commitable = false;
            } else {
                String content = mEditContent.getText().toString().trim();
                if (content.isEmpty()) {
                    commitable = false;
                }
            }
        }
        mBtnCommit.setEnabled(commitable);
        mBtnCommit.setBackgroundResource(commitable ? R.drawable.check_update_button
                : R.drawable.update_btn_disable_bg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMessageDialog != null) {
            mMessageDialog.dismiss();
            mMessageDialog = null;
        }
        SharedPreferences perference = PreferenceManager.getDefaultSharedPreferences(this);
        String email = mEditEmail.getText().toString().trim();
        if(email.matches(EMAIL_EXPRESSION)) {
            perference.edit().putString(FeedbackHelper.KEY_CONTENT, mEditContent.getText().toString())
            .putString(FeedbackHelper.KEY_EMAIL, email)
            .putInt(FeedbackHelper.KEY_CATEGORY, mCategoryPos).commit();
        } else {
            perference.edit().putString(FeedbackHelper.KEY_CONTENT, mEditContent.getText().toString())
            .putInt(FeedbackHelper.KEY_CATEGORY, mCategoryPos).commit();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        checkCommitable();
        SDKWrapper.addEvent(this, SDKWrapper.P1, "setting", "fb_enter");
    }

    @Override
    public void onClick(View v) {
        hideIME();
        if (v == mCategoryLayout) {
            mLeoPopMenu = new LeoPopMenu();
            LayoutStyles styles = new LayoutStyles();
            styles.width = LayoutParams.MATCH_PARENT;
            styles.height = LayoutParams.WRAP_CONTENT;
            styles.animation = R.style.PopupListAnimUpDown;
            mLeoPopMenu.setPopMenuItems(mCategories);
            mLeoPopMenu.setPopItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    mCategory.setText(mCategories.get(position));
                    mCategoryPos = position;
                    mCategory.setTag(1);
                    mLeoPopMenu.dismissSnapshotList();
                    checkCommitable();
                }
            });
            mLeoPopMenu.showPopMenu(this, mCategory, styles, new OnDismissListener() {
                @Override
                public void onDismiss() {
                    mCategoryImg.setImageResource(R.drawable.choose_normal);
                }
            });
            mCategoryImg.setImageResource(R.drawable.choose_active);
        } else if (v == mBtnCommit) {
            if (mEditEmail.getText().toString().trim().matches(EMAIL_EXPRESSION)) {
                FeedbackHelper.getInstance().tryCommit(mCategory.getText().toString(),
                        mEditEmail.getText().toString().trim(),
                        mEditContent.getText().toString().trim());
                showMessageDialog(getString(R.string.feedback_success_title),
                        getString(R.string.feedback_success_content));
                mEditEmail.setText(mEditEmail.getText().toString());
                mEditContent.setText(mEditContent.getText().toString());
                mCategory.setText(mCategory.getText().toString());
                mCategory.setTag(1);
            } else {
                Toast.makeText(FeedbackActivity.this,
                        this.getResources().getText(R.string.feedback_error), Toast.LENGTH_SHORT)
                        .show();
            }
            SDKWrapper.addEvent(this, SDKWrapper.P1, "setting", "fb_submit");
        } else if( v == mEmailImg) {
            mLeoPopMenu = new LeoPopMenu();
            LayoutStyles styles = new LayoutStyles();
            styles.width = LayoutParams.MATCH_PARENT;
            styles.height = LayoutParams.WRAP_CONTENT;
            styles.animation = R.style.PopupListAnimUpDown;
            mLeoPopMenu.setPopMenuItems(mEmails);
            mLeoPopMenu.setPopItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    mEditEmail.setText(mEmails.get(position));
                    mLeoPopMenu.dismissSnapshotList();
                    checkCommitable();
                }
            });
            mLeoPopMenu.showPopMenu(this, mEditEmail, styles, new OnDismissListener() {
                @Override
                public void onDismiss() {
                    mEmailImg.setImageResource(R.drawable.choose_normal);
                }
            });
            mEmailImg.setImageResource(R.drawable.choose_active);
        }
    }

    private void hideIME() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditEmail.getWindowToken(), 0);
    }

    private void showMessageDialog(String title, String message) {
        if (mMessageDialog == null) {
            mMessageDialog = new LEOMessageDialog(this);
            mMessageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mCategoryPos = -1;
                    mEditContent.setText("");
                    mCategory.setText(R.string.feedback_category_tip);
                    mCategory.setTag(null);
                    finish();
                }
            });
        }
        mMessageDialog.setTitle(title);
        mMessageDialog.setContent(message);
        mMessageDialog.show();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(v == mEditEmail) {
            mEmailLayout.setBackgroundResource(hasFocus ? R.drawable.text_bg_acitve : R.drawable.text_bg_normal);
        }
    }
}
