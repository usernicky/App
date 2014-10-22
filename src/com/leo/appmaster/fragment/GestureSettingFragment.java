package com.leo.appmaster.fragment;

import java.util.List;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.appmaster.R;
import com.leo.appmaster.applocker.AppLockListActivity;
import com.leo.appmaster.applocker.AppLockerPreference;
import com.leo.appmaster.applocker.LockOptionActivity;
import com.leo.appmaster.applocker.LockSettingActivity;
import com.leo.appmaster.applocker.PasswdProtectActivity;
import com.leo.appmaster.applocker.gesture.LockPatternView;
import com.leo.appmaster.applocker.gesture.LockPatternView.Cell;
import com.leo.appmaster.applocker.gesture.LockPatternView.OnPatternListener;
import com.leo.appmaster.applocker.service.LockService;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog.OnDiaogClickListener;
import com.leo.appmaster.ui.dialog.LEOMessageDialog;
import com.leo.appmaster.utils.LockPatternUtils;

public class GestureSettingFragment extends BaseFragment implements
		OnClickListener, OnPatternListener, OnDismissListener,
		OnDiaogClickListener {

	private TextView mTvGestureTip;
	private LockPatternView mLockPatternView;
	private int mInputCount = 1;
	private String mTempGesture1, mTempGesture2;
	private boolean mGotoPasswdProtect;
	private String mActivityName;

	@Override
	protected int layoutResourceId() {
		return R.layout.fragment_gesture_setting;
	}

	@Override
	protected void onInitUI() {
		mLockPatternView = (LockPatternView) findViewById(R.id.gesture_lockview);
		mLockPatternView.setOnPatternListener(this);
		mTvGestureTip = (TextView) findViewById(R.id.tv_gesture_tip);

		if (AppLockerPreference.getInstance(mActivity).getGesture() == null
				&& AppLockerPreference.getInstance(mActivity).getPassword() == null) {
			mTvGestureTip.setText(R.string.passwd_set_gesture_tip);
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tv_bottom:
			resetGesture();
			break;

		default:
			break;
		}
	}

	public void setActivityName(String name) {
		mActivityName = name;
	}

	private void resetGesture() {
		mInputCount = 1;
		mTempGesture1 = mTempGesture2 = "";
		mTvGestureTip.setText(R.string.gesture_hint);
	}

	@Override
	public void onPatternStart() {

	}

	@Override
	public void onPatternCleared() {

	}

	@Override
	public void onPatternCellAdded(List<Cell> pattern) {

	}

	@Override
	public void onPatternDetected(final List<Cell> pattern) {
		mLockPatternView.postDelayed(new Runnable() {
			@Override
			public void run() {
				checkGesture(pattern);
			}
		}, 300);

	}

	private void checkGesture(List<Cell> pattern) {
		int patternSize = pattern.size();
		if (mInputCount == 1) {
			if (patternSize < 4) {
				// shakeGestureTip();
				mLockPatternView.clearPattern();
				return;
			}
			mTempGesture1 = LockPatternUtils.patternToString(pattern);
			mTvGestureTip.setText(R.string.please_input_gesture_again);
			mLockPatternView.clearPattern();
			mInputCount++;
		} else {
			mTempGesture2 = LockPatternUtils.patternToString(pattern);
			if (mTempGesture2.equals(mTempGesture1)) {
				Intent intent = null;
				// now we can start lock service
				intent = new Intent(mActivity, LockService.class);
				mActivity.startService(intent);
				AppLockerPreference.getInstance(mActivity).saveGesture(
						mTempGesture2);
				if (((LockSettingActivity) mActivity).isResetPasswd()) {
					showResetSuc();
					return;
				}
				Toast.makeText(mActivity, R.string.set_gesture_suc, 1).show();
				if (!AppLockerPreference.getInstance(mActivity)
						.hasPswdProtect()) {
					showGestureProtectDialog();
				} else {

					if (LockOptionActivity.class.getName()
							.equals(mActivityName)) {
						intent = new Intent(mActivity, LockOptionActivity.class);
					} else {
						intent = new Intent(mActivity,
								AppLockListActivity.class);
					}
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					mActivity.startActivity(intent);
				}
			} else {
				resetGesture();
				Toast.makeText(mActivity, R.string.tip_no_the_same_pswd, 1)
						.show();
				mLockPatternView.clearPattern();
				if (AppLockerPreference.getInstance(mActivity).getGesture() == null
						&& AppLockerPreference.getInstance(mActivity)
								.getPassword() == null) {
					mTvGestureTip.setText(R.string.passwd_set_gesture_tip);
				}
			}
		}
	}

	private void showResetSuc() {
		LEOMessageDialog d = new LEOMessageDialog(mActivity);
		d.setTitle(mActivity.getString(R.string.reset_gesture));
		d.setContent(mActivity.getString(R.string.reset_passwd_successful));
		d.setOnDismissListener(this);
		d.setCanceledOnTouchOutside(false);
		d.show();
	}

	private void showGestureProtectDialog() {
		LEOAlarmDialog d = new LEOAlarmDialog(mActivity);
		d.setTitle(mActivity.getString(R.string.set_protect_or_not));
		d.setContent(mActivity.getString(R.string.set_protect_message));
		d.setLeftBtnStr(mActivity.getString(R.string.cancel));
		d.setRightBtnStr(mActivity.getString(R.string.makesure));
		d.setOnClickListener(this);
		d.setOnDismissListener(this);
		d.show();
	}

	private void shakeGestureTip() {
		Animation shake = AnimationUtils.loadAnimation(mActivity, R.anim.shake);
		mTvGestureTip.startAnimation(shake);
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		Intent intent;
		if (mGotoPasswdProtect) {
			intent = new Intent(mActivity, AppLockListActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			mActivity.startActivity(intent);
			intent = new Intent(mActivity, PasswdProtectActivity.class);
			mActivity.startActivity(intent);
		} else if (((LockSettingActivity) mActivity).isResetPasswd()) {
			// mActivity.finish();
		} else {
			intent = new Intent(mActivity, AppLockListActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			mActivity.startActivity(intent);
		}
		mActivity.finish();
	}

	@Override
	public void onClick(int which) {
		if (which == 0) {
		} else if (which == 1) {
			mGotoPasswdProtect = true;
		}
	}
}
