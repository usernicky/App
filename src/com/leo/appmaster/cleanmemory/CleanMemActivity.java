package com.leo.appmaster.cleanmemory;

import com.leo.appmaster.R;
import com.leo.appmaster.ui.CommonTitleBar;
import com.leo.appmaster.ui.RocketDock;
import com.leo.appmaster.ui.ShadeView;
import com.leo.appmaster.utils.ProcessUtils;
import com.leo.appmaster.utils.TextFormater;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class CleanMemActivity extends Activity implements OnClickListener,
		OnTouchListener {

	public static final int MSG_UPDATE_MEM = 0;
	private CommonTitleBar mTtileBar;
	private ImageButton mRocket;
	private ImageView mIvLoad, mIvOk;;
	private View mRocketHolder;
	private RocketDock mRocketDock;
	private TextView mTvUsedMemory, mTvTotalMemory, mTvCleanResult,
			mTvAccelerate;
	private ShadeView mShadeView;

	private long mLastUsedMem;
	private long mTotalMem;
	private long mCleanMem;
	private Vibrator mVibrator;
	public boolean mVibrating;
	private boolean mTranslating;
	private boolean mUpdating;
	private ProcessCleaner mCleaner;
	private Animation mRocketAnima;
	private float mTouchDownX, mTouchDownY;
	private float mThreshold = 0;
	private Animation mShakeAnim;

	private boolean mAllowClean;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_clean_mem);
		createTranslateAnima();
		initUI();
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	}

	private void initUI() {
		mTtileBar = (CommonTitleBar) findViewById(R.id.layout_title_bar);
		mTtileBar.openBackView();
		mRocket = (ImageButton) findViewById(R.id.rocket_icon);
		mRocketHolder = findViewById(R.id.layout_rocket_holder);
		mRocketDock = (RocketDock) findViewById(R.id.rocket_dock);
		mTvUsedMemory = (TextView) findViewById(R.id.tv_memory);
		mTvTotalMemory = (TextView) findViewById(R.id.tv_total);
		mTvCleanResult = (TextView) findViewById(R.id.tv_clean_result);
		mTvAccelerate = (TextView) findViewById(R.id.tv_accelerate);
		mShadeView = (ShadeView) findViewById(R.id.shade_view);
		mIvLoad = (ImageView) findViewById(R.id.iv_load);
		mIvOk = (ImageView) findViewById(R.id.clean_ok);
		mCleaner = ProcessCleaner.getInstance(this);

		mTotalMem = mCleaner.getTotalMem();
		mLastUsedMem = mCleaner.getUsedMem();
		updateMem();

		mAllowClean = mCleaner.allowClean();
		if (mAllowClean) {
			mRocket.setOnTouchListener(this);
			mRocket.setVisibility(View.VISIBLE);
			mIvLoad.setVisibility(View.VISIBLE);
			startLoad();
			startTranslate();
		} else {
			String s = "为您清理"
					+ TextFormater.dataSizeFormat(mCleaner.getLastCleanMem())
					+ "内存";
			mTvCleanResult.setText(s);
			mRocket.setVisibility(View.INVISIBLE);
			mIvLoad.setVisibility(View.INVISIBLE);
			mIvOk.setVisibility(View.VISIBLE);
			mTvAccelerate.setVisibility(View.INVISIBLE);
			mRocketHolder.setBackgroundDrawable(null);
			mTvAccelerate.setText(R.string.compeletely);
			// todo change UI
		}

	}

	private void startLoad() {
		rotateLoadView(2000, 360 * 3);
		final int target = (int) mLastUsedMem;
		final ValueAnimator up = ValueAnimator.ofInt(0, target);
		up.setDuration(2000);
		up.setInterpolator(new AccelerateDecelerateInterpolator());
		up.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator va) {
				mLastUsedMem = (Integer) va.getAnimatedValue();
				updateMem();
			}
		});
		up.start();

		mShadeView.updateColor(0xff, 0x3b, 0x00, 2000);
	}

	private void rotateLoadView(int duration, int degrees) {
		Animation ra = new RotateAnimation(0, degrees,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		ra.setDuration(duration);
		ra.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				AlphaAnimation aa = new AlphaAnimation(1f, 0.0f);
				aa.setDuration(1000);
				aa.setFillEnabled(true);
				aa.setFillAfter(true);
				mIvLoad.startAnimation(aa);
			}
		});
		mIvLoad.startAnimation(ra);
	}

	private void stopLoad() {
		mIvLoad.clearAnimation();
	}

	private void updateMem() {
		mTvUsedMemory.setText(TextFormater.dataSizeFormat(mLastUsedMem));
		mTvTotalMemory.setText("/" + TextFormater.dataSizeFormat(mTotalMem));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		default:
			break;
		}

	}

	private void startTranslate() {
		mTranslating = true;
		mRocket.startAnimation(mRocketAnima);
		mRocket.setImageResource(R.drawable.rocket_stop);
	}

	private void stopTranslate() {
		if (mTranslating) {
			mTranslating = false;
			mRocketAnima.cancel();
			mRocket.setImageResource(R.drawable.rocket_fly);
		}
	}

	private void createTranslateAnima() {
		AnimationSet as = new AnimationSet(true);
		TranslateAnimation ta = new TranslateAnimation(0, 0, 0, 50);
		ScaleAnimation sa = new ScaleAnimation(1f, 1f, 1f, 0.9f);
		as.addAnimation(sa);
		as.addAnimation(ta);
		as.setDuration(1000);

		ta.setRepeatCount(Animation.INFINITE);
		ta.setRepeatMode(Animation.REVERSE);
		sa.setRepeatCount(Animation.INFINITE);
		sa.setRepeatMode(Animation.REVERSE);

		mRocketAnima = as;
	}

	private void cleanMemory() {
		launchRocket();
		showOK();
		mCleaner.tryClean();
		long curUsedMem = mCleaner.getUsedMem();
		mCleanMem = Math.abs(mLastUsedMem - curUsedMem);
		startUpdataMemTip(curUsedMem);
		mShadeView.updateColor(0x28, 0x93, 0xfe, 1200);
		mAllowClean = false;
	}

	private void showOK() {
		AnimationSet as = new AnimationSet(true);
		AlphaAnimation aa = new AlphaAnimation(1.0f, 0.0f);
		ScaleAnimation sa = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f,
				ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
				ScaleAnimation.RELATIVE_TO_SELF, 0.5f);

		aa.setDuration(800);
		sa.setDuration(1000);

		as.addAnimation(sa);
		as.addAnimation(aa);

		as.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mRocketHolder.setBackgroundDrawable(null);
				mTvAccelerate.setVisibility(View.INVISIBLE);
				mIvOk.setVisibility(View.VISIBLE);

				ScaleAnimation show = new ScaleAnimation(0.0f, 1.0f, 0.0f,
						1.0f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
						ScaleAnimation.RELATIVE_TO_SELF, 0.5f);

				show.setDuration(500);

				mRocketHolder.startAnimation(show);
			}
		});

		mRocketHolder.startAnimation(as);

	}

	private void shakeRocket() {
		mShakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake);

		mRocket.startAnimation(mShakeAnim);
	}

	private void stopShakeRocket() {
		mShakeAnim.cancel();
	}

	public void launchRocket() {
		Animation ta = createRocketFly();
		mRocket.setImageResource(R.drawable.rocket_fly);
		mRocket.startAnimation(ta);
	}

	private Animation createRocketFly() {
		TranslateAnimation ta = new TranslateAnimation(0, 0, 0, -2000);
		ta.setDuration(1000);
		ta.setFillEnabled(true);
		ta.setFillAfter(true);
		ta.setInterpolator(new AccelerateInterpolator());
		ta.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// startTranslate();
				mRocket.setVisibility(View.INVISIBLE);
				mRocket.setOnTouchListener(null);
			}
		});
		return ta;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();

		if (mThreshold == 0) {
			mThreshold = Math.min(mRocketHolder.getWidth(),
					mRocketHolder.getHeight()) / 2;
			Log.e("xxxx", "mThreshold = " + mThreshold);
		}

		if (action == MotionEvent.ACTION_DOWN) {
			mTouchDownX = event.getX();
			mTouchDownY = event.getY();
			stopTranslate();
			prepareLaunch();
		} else if (action == MotionEvent.ACTION_UP) {

			float x = event.getX();
			float y = event.getY();

			if (Math.abs(mTouchDownX - x) > mThreshold
					|| Math.abs(mTouchDownY - y) > mThreshold) {
				cancelLaunch();
			} else {
				stopVibrate();
				cleanMemory();
			}
		} else if (action == MotionEvent.ACTION_MOVE) {
			float x = event.getX();
			float y = event.getY();

			if (Math.abs(mTouchDownX - x) > mThreshold
					|| Math.abs(mTouchDownY - y) > mThreshold) {
				cancelLaunch();
			} else {
				prepareLaunch();
			}
		}
		return false;
	}

	private void prepareLaunch() {
		if (!mVibrating) {
			mVibrating = true;
			new Thread(new VibrateTask()).start();
			// todo
			startSmoking();
			shakeRocket();
		}
		stopTranslate();
	}

	private void startSmoking() {

	}

	private void cancelLaunch() {
		stopVibrate();
		stopShakeRocket();
		if (!mTranslating) {
			mTranslating = true;
			startTranslate();
		}
	}

	private void stopVibrate() {
		mVibrating = false;
		mVibrator.cancel();
	}

	private class VibrateTask implements Runnable {

		@Override
		public void run() {
			while (mVibrating) {
				mVibrator.vibrate(200);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private void startUpdataMemTip(long targetMem) {
		if (!mUpdating) {
			mUpdating = true;
			ValueAnimator down = ValueAnimator.ofInt((int) mLastUsedMem, 0);
			down.setDuration(500);

			final ValueAnimator up = ValueAnimator.ofInt(0, (int) targetMem);
			up.setDuration(500);

			down.addUpdateListener(new AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator va) {
					mLastUsedMem = (Integer) va.getAnimatedValue();
					updateMem();
				}
			});

			down.addListener(new AnimatorListener() {
				@Override
				public void onAnimationStart(Animator arg0) {
				}

				@Override
				public void onAnimationRepeat(Animator arg0) {
				}

				@Override
				public void onAnimationCancel(Animator arg0) {
				}

				@Override
				public void onAnimationEnd(Animator arg0) {
					up.start();
				}
			});

			up.addUpdateListener(new AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator va) {
					mLastUsedMem = (Integer) va.getAnimatedValue();
					updateMem();
				}
			});
			up.addListener(new AnimatorListener() {
				@Override
				public void onAnimationStart(Animator arg0) {
				}

				@Override
				public void onAnimationRepeat(Animator arg0) {
				}

				@Override
				public void onAnimationCancel(Animator arg0) {
				}

				@Override
				public void onAnimationEnd(Animator arg0) {
					mUpdating = false;

					String s = "为您清理" + TextFormater.dataSizeFormat(mCleanMem)
							+ "内存";
					Toast.makeText(CleanMemActivity.this, s, Toast.LENGTH_SHORT)
							.show();

					mTvCleanResult.setText(s);
				}
			});

			down.start();
		}
	}

	private static class EventHandler extends Handler {

		CleanMemActivity mActivity;

		public EventHandler(CleanMemActivity activity) {
			super();
			this.mActivity = activity;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_MEM:
				mActivity.updateMem();
				break;

			default:
				break;
			}
		}
	}

}
