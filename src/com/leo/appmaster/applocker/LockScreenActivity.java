package com.leo.appmaster.applocker;

import java.util.ArrayList;
import java.util.List;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.R;
import com.leo.appmaster.SDKWrapper;
import com.leo.appmaster.applocker.logic.LockHandler;
import com.leo.appmaster.applocker.service.LockService;
import com.leo.appmaster.fragment.GestureLockFragment;
import com.leo.appmaster.fragment.LockFragment;
import com.leo.appmaster.fragment.PasswdLockFragment;
import com.leo.appmaster.home.HomeActivity;
import com.leo.appmaster.theme.ThemeUtils;
import com.leo.appmaster.lockertheme.LockerTheme;
import com.leo.appmaster.ui.CommonTitleBar;
import com.leo.appmaster.ui.LeoPopMenu;
import com.leo.appmaster.ui.dialog.LeoDoubleLinesInputDialog;
import com.leo.appmaster.ui.dialog.LeoDoubleLinesInputDialog.OnDiaogClickListener;
import com.leo.appmaster.utils.AppUtil;
import com.leo.appmaster.utils.DipPixelUtil;
import com.leo.appmaster.utils.FastBlur;
import com.leo.appmaster.utils.LeoLog;
import com.leoers.leoanalytics.LeoStat;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class LockScreenActivity extends FragmentActivity implements
		OnClickListener, OnDiaogClickListener {

	public static String EXTRA_UNLOCK_FROM = "extra_unlock_from";
	public static String EXTRA_UKLOCK_TYPE = "extra_unlock_type";
	public static String EXTRA_TO_ACTIVITY = "extra_to_activity";
	public static String EXTRA_LOCK_TITLE = "extra_lock_title";

	int mFromType;
	private String mToPackage;
	private String mToActivity;
	private CommonTitleBar mTtileBar;
	private LockFragment mFragment;
	private Bitmap mAppBaseInfoLayoutbg;
	private LeoPopMenu mLeoPopMenu;
	private LeoDoubleLinesInputDialog mDialog;
	private EditText mEtQuestion, mEtAnwser;
	private String mLockTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lock_setting);
		handleIntent();
		initUI();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	private void handleIntent() {
		Intent intent = getIntent();
		int type = intent.getIntExtra(EXTRA_UKLOCK_TYPE,
				LockFragment.LOCK_TYPE_PASSWD);
	      mFromType = intent.getIntExtra(EXTRA_UNLOCK_FROM,
	                LockFragment.FROM_SELF);
	      
		if (type == LockFragment.LOCK_TYPE_PASSWD) {
			mFragment = new PasswdLockFragment();
		} else {
			mFragment = new GestureLockFragment();
		}

		if (!ThemeUtils.checkThemeNeed(this) &&  (mFromType == LockFragment.FROM_OTHER
				|| mFromType == LockFragment.FROM_SCREEN_ON)) {
			BitmapDrawable bd = (BitmapDrawable) AppUtil.getDrawable(
					getPackageManager(),
					intent.getStringExtra(LockHandler.EXTRA_LOCKED_APP_PKG));
			// createChoiceDialog();
			setAppInfoBackground(bd);
		}
		mLockTitle = intent.getStringExtra(EXTRA_LOCK_TITLE);
		mFragment.setFrom(mFromType);
		mToPackage = intent.getStringExtra(LockHandler.EXTRA_LOCKED_APP_PKG);
		mToActivity = intent.getStringExtra(EXTRA_TO_ACTIVITY);
		mFragment.setPackage(mToPackage);
		mFragment.setActivity(mToActivity);
	}

	// private void createChoiceDialog() {
	// final String[] valueString = getResources().getStringArray(
	// R.array.det_lock_time_items);
	//
	// AlertDialog scaleIconListDlg = new AlertDialog.Builder(this)
	// .setTitle(R.string.change_lock_time)
	// .setSingleChoiceItems(R.array.lock_time_entrys, -1,
	// new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog,
	// int whichButton) {
	// AppMasterPreference.getInstance(
	// LockScreenActivity.this)
	// .setRelockTimeout(
	// valueString[whichButton]);
	// SDKWrapper.addEvent(LockScreenActivity.this,
	// LeoStat.P1, "lock_setting",
	// valueString[whichButton]);
	// dialog.dismiss();
	// }
	// })
	// .setNegativeButton(R.string.cancel,
	// new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog,
	// int whichButton) {
	// /* User clicked No so do some stuff */
	// }
	// }).create();
	// TextView title = new TextView(this);
	// title.setText(getString(R.string.change_lock_time));
	// title.setTextColor(Color.WHITE);
	// title.setTextSize(20);
	// title.setPadding(DipPixelUtil.dip2px(this, 20),
	// DipPixelUtil.dip2px(this, 10), 0, DipPixelUtil.dip2px(this, 10));
	// title.setBackgroundColor(getResources().getColor(
	// R.color.dialog_title_area_bg));
	// scaleIconListDlg.setCustomTitle(title);
	// scaleIconListDlg.show();
	// }

	private void setAppInfoBackground(Drawable drawable) {
		int h = drawable.getIntrinsicHeight() * 9 / 10;
		int w = h * 3 / 5;
		mAppBaseInfoLayoutbg = Bitmap.createBitmap(w, h,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(mAppBaseInfoLayoutbg);
		canvas.drawColor(Color.WHITE);
		drawable.setBounds(-(drawable.getIntrinsicWidth() - w) / 2,
				-(drawable.getIntrinsicHeight() - h) / 2,
				(drawable.getIntrinsicWidth() - w) / 2 + w,
				(drawable.getIntrinsicHeight() - h) / 2 + h);
		drawable.draw(canvas);
		canvas.drawColor(Color.argb(70, 0, 0, 0));
		mAppBaseInfoLayoutbg = FastBlur.doBlur(mAppBaseInfoLayoutbg, 25, true);

		RelativeLayout layout = (RelativeLayout) findViewById(R.id.activity_lock_layout);

		layout.setBackgroundDrawable(new BitmapDrawable(mAppBaseInfoLayoutbg));

	}

	@Override
	protected void onDestroy() {
		LeoLog.e("LockScreenActivity", "onDestroy");
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mAppBaseInfoLayoutbg != null) {
			mAppBaseInfoLayoutbg.recycle();
			mAppBaseInfoLayoutbg = null;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		LeoLog.d("LockScreenActivity", "onStop" + "      mFromType = "
				+ mFromType);
		if (mFromType == LockFragment.FROM_OTHER) {
			if (!AppMasterPreference.getInstance(this).isAutoLock()) {
				return;
			}
			finish();
		}
	}

	private void initUI() {
		mTtileBar = (CommonTitleBar) findViewById(R.id.layout_title_bar);

		if (AppMasterPreference.getInstance(this).hasPswdProtect()) {
			mTtileBar.setOptionImage(R.drawable.setting_selector);
			mTtileBar.setOptionImageVisibility(View.VISIBLE);
			mTtileBar.setOptionListener(this);
		}

		if (mFromType == LockFragment.FROM_SELF_HOME
				|| mFromType == LockFragment.FROM_SELF) {
			mTtileBar.setBackViewListener(this);
			if (TextUtils.isEmpty(mLockTitle)) {
				mTtileBar.setTitle(R.string.app_lock);
			} else {
				mTtileBar.setTitle(mLockTitle);
			}
		} else {
			mTtileBar.setBackArrowVisibility(View.GONE);
			mTtileBar.setTitle(R.string.app_name);
		}
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction tans = fm.beginTransaction();
		tans.replace(R.id.fragment_contain, mFragment);
		tans.commit();
	}

	public void onUnlockSucceed() {
		if (mFromType == LockFragment.FROM_SELF) {
			Intent intent = null;
			intent = new Intent(this, LockService.class);
			this.startService(intent);
			setResult(11);
		} else if (mFromType == LockFragment.FROM_OTHER
				|| mFromType == LockFragment.FROM_SCREEN_ON) {
			// input right gesture, just finish self
			Intent intent = new Intent(LockHandler.ACTION_APP_UNLOCKED);
			intent.putExtra(LockHandler.EXTRA_LOCKED_APP_PKG, mToPackage);
			sendBroadcast(intent);
		} else if (mFromType == LockFragment.FROM_SELF_HOME) {
			Intent intent = null;
			// try start lock service
			intent = new Intent(this, LockService.class);
			this.startService(intent);
			intent = new Intent();
			intent.setClassName(this, mToActivity);
			this.startActivity(intent);
		}

		AppMasterPreference pref = AppMasterPreference.getInstance(this);
		pref.setUnlockCount(pref.getUnlockCount() + 1);
		finish();
	}

	public void onUolockOutcount() {
		Intent intent = new Intent(this, WaitActivity.class);
		intent.putExtra(LockHandler.EXTRA_LOCKED_APP_PKG, mToPackage);
		startActivity(intent);
	}

	private void findPasswd() {
		mDialog = new LeoDoubleLinesInputDialog(this);
		mDialog.setTitle(R.string.pleas_input_anwser);
		mDialog.setFirstHead(R.string.passwd_question);
		mDialog.setSecondHead(R.string.passwd_anwser);
		mDialog.setOnClickListener(this);
		mEtQuestion = mDialog.getFirstEditText();
		mEtAnwser = mDialog.getSecondEditText();
		mEtQuestion.setFocusable(false);
		mEtQuestion.setText(AppMasterPreference.getInstance(this)
				.getPpQuestion());
		mDialog.show();
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		if (mFromType == LockFragment.FROM_OTHER
				|| mFromType == LockFragment.FROM_SCREEN_ON) {

			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
		} else {

			intent.setClassName(getApplicationContext(),
					HomeActivity.class.getName());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		startActivity(intent);
		finish();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tv_option_image:
			if (mLeoPopMenu == null) {
				mLeoPopMenu = new LeoPopMenu();
				mLeoPopMenu.setPopItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						if (position == 0) {
							findPasswd();
						} else if (position == 1) {
							Intent intent = new Intent(LockScreenActivity.this,
									LockerTheme.class);
							startActivityForResult(intent, 0);
						}
						mLeoPopMenu.dismissSnapshotList();
					}
				});
			}
			mLeoPopMenu.setPopMenuItems(getPopMenuItems());
			mLeoPopMenu.showPopMenu(this,
					mTtileBar.findViewById(R.id.tv_option_image), null, null);
			break;
		case R.id.layout_title_back:
			onBack();
			break;
		default:
			break;
		}

	}

	private void onBack() {
		Intent intent = new Intent();
		if (mFromType == LockFragment.FROM_OTHER
				|| mFromType == LockFragment.FROM_SCREEN_ON) {

			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
		} else {

			intent.setClassName(getApplicationContext(),
					HomeActivity.class.getName());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		startActivity(intent);
		finish();
	}

	private List<String> getPopMenuItems() {
		List<String> listItems = new ArrayList<String>();
		Resources resources = AppMasterApplication.getInstance().getResources();
		if (AppMasterPreference.getInstance(this).getLockType() == AppMasterPreference.LOCK_TYPE_GESTURE) {
			listItems.add(resources.getString(R.string.find_gesture));
		} else if (AppMasterPreference.getInstance(this).getLockType() == AppMasterPreference.LOCK_TYPE_PASSWD) {
			listItems.add(resources.getString(R.string.find_passwd));
		}
		listItems.add(resources.getString(R.string.lockerTheme));
		return listItems;
	}

	@Override
	public void onClick(int which) {
		if (which == 1) {// make sure
			String anwser = AppMasterPreference.getInstance(this).getPpAnwser();
			if (anwser.equals(mEtAnwser.getText().toString())) {
				// goto reset passwd
				Intent intent = new Intent(this, LockSettingActivity.class);
				intent.putExtra(LockSettingActivity.RESET_PASSWD_FLAG, true);
				this.startActivity(intent);
				finish();
			} else {
				Toast.makeText(this, R.string.reinput_anwser, 0).show();
				mEtAnwser.setText("");
			}
		} else if (which == 0) { // cancel
			mDialog.dismiss();
		}
	}
	
	public int getFromType() {
	    return mFromType;
	}
}
