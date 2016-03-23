package com.leo.appmaster.applocker.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.Constants;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.applocker.IntruderPhotoInfo;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.DeviceAdminEvent;
import com.leo.appmaster.eventbus.event.EventId;
import com.leo.appmaster.intruderprotection.CameraSurfacePreview;
import com.leo.appmaster.intruderprotection.IntruderCatchedActivity;
import com.leo.appmaster.intruderprotection.WaterMarkUtils;
import com.leo.appmaster.mgr.IntrudeSecurityManager;
import com.leo.appmaster.mgr.LockManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.PrivacyDataManager;
import com.leo.appmaster.utils.BitmapUtils;
import com.leo.appmaster.utils.FileOperationUtil;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceReceiverNewOne extends DeviceAdminReceiver {
	private PrivacyDataManager mPDManager;
	private IntrudeSecurityManager mISManager;
	private LockManager mLockManager;
	private FrameLayout mFlRoot = null;
	private WindowManager mWm = null;

	public final static long NORMAL_TIME = 1500; // 默认打开入侵者防护界面时间
	public final static long LONG_TIME = 3000; // 增长打开入侵者防护界面时间

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
	    String action = intent.getAction();
	    LeoLog.d("STONE_ADMIN", intent.getAction());

	    if(action.equals(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED)){
	        LeoLog.d("stone_admin", "ACTION_DEVICE_ADMIN_ENABLED");
	        LeoEventBus.getDefaultBus().post(
	                new DeviceAdminEvent(EventId.EVENT_DEVICE_ADMIN_ENABLE, "useless"));
	    }
	    if(action.equals(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_DISABLED)){
	        LeoLog.d("stone_admin", "ACTION_DEVICE_ADMIN_DISABLED");
	        LeoEventBus.getDefaultBus().post(
	                new DeviceAdminEvent(EventId.EVENT_DEVICE_ADMIN_DISABLE, "useless"));
	    }
	}

	@Override
	public void onEnabled(Context context, Intent intent) {
		super.onEnabled(context, intent);
		((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).removeActiveAdmin(DeviceReceiver.getComponentName(context));
		if (mISManager == null) {
			mISManager = (IntrudeSecurityManager) MgrContext.getManager(MgrContext.MGR_INTRUDE_SECURITY);
		}
		mISManager.setSystIntruderProtectionSwitch(true);
	}

	@Override
	public void onDisabled(Context context, Intent intent) {
		super.onDisabled(context, intent);
		if (mISManager == null) {
			mISManager = (IntrudeSecurityManager) MgrContext.getManager(MgrContext.MGR_INTRUDE_SECURITY);
		}
		mISManager.setSystIntruderProtectionSwitch(false);
	}


	@Override
	public void onPasswordFailed(final Context context, Intent intent) {
		IntrudeSecurityManager.sFailTimesAtSystLock ++;
		LeoLog.d("poha_admin", "sFailTimesAtSystLock = " + IntrudeSecurityManager.sFailTimesAtSystLock);
		if (mPDManager == null) {
			mPDManager = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
		}
		if (mISManager == null) {
			mISManager = (IntrudeSecurityManager) MgrContext.getManager(MgrContext.MGR_INTRUDE_SECURITY);
		}
		boolean isOpen = mISManager.getSystIntruderProtecionSwitch();

		int finalJudgeTimes = getFailTimesByApi(context);
		LeoLog.d("poha_admin", "getFailTimesByApi = " + finalJudgeTimes);
		if (getFailTimesByApi(context) == -1) {
			finalJudgeTimes = IntrudeSecurityManager.sFailTimesAtSystLock;
			LeoLog.d("poha_admin", "use value accumulated by myself = " + finalJudgeTimes);
		}


		LeoLog.d("poha_admin", "IntrudeSecurityManager.sHasTakenWhenUnlockSystemLock = " + IntrudeSecurityManager.sHasTakenWhenUnlockSystemLock);
		LeoLog.d("poha_admin", "isOpen = " + isOpen);
		LeoLog.d("poha_admin", "finalJudgeTimes = " + finalJudgeTimes);

		if (!IntrudeSecurityManager.sHasTakenWhenUnlockSystemLock && isOpen && finalJudgeTimes >= mISManager.getTimesForTakePhoto() && mISManager.getIsIntruderSecurityAvailable()) {
			try {
				LeoLog.d("poha_admin", "to take pic");
				final Context ctx = AppMasterApplication.getInstance();
				final CameraSurfacePreview cfp = new CameraSurfacePreview(ctx);
				final WindowManager mWM = (WindowManager) AppMasterApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
				if (mFlRoot == null) {
					mFlRoot = new FrameLayout(ctx);
				}
				WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
				localLayoutParams.height = 1;
				localLayoutParams.width = 1;
				localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
				localLayoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
				mFlRoot.addView(cfp);
				mWM.addView(mFlRoot, localLayoutParams);
				ThreadManager.executeOnAsyncThreadDelay(new Runnable() {
					@Override
					public void run() {
						if (cfp == null) {
							return;
						}
						cfp.takePicture(new Camera.PictureCallback() {
							@Override
							public void onPictureTaken(final byte[] data, Camera camera) {
								IntrudeSecurityManager.sHasPicShowedWhenUserPresent = false;
								IntrudeSecurityManager.sHasPicTakenAtSystemLockSaved = false;
								IntrudeSecurityManager.sHasTakenWhenUnlockSystemLock = true;
								ThreadManager.executeOnAsyncThread(new Runnable() {
									@Override
									public void run() {
										Bitmap bitmapt = null;
										try {
											bitmapt = BitmapUtils.bytes2BimapWithScale(data, ctx);
										} catch (Throwable e) {
										}
										Matrix m = new Matrix();
										int orientation = cfp.getCameraOrientation();
										mWM.removeView(mFlRoot);
										m.setRotate(180 - orientation, (float) bitmapt.getWidth() / 2, (float) bitmapt.getHeight() / 2);
										bitmapt = Bitmap.createBitmap(bitmapt, 0, 0, bitmapt.getWidth(), bitmapt.getHeight(), m, true);
										String timeStamp = new SimpleDateFormat(Constants.INTRUDER_PHOTO_TIMESTAMP_FORMAT).format(new Date());
										//添加水印
										bitmapt = WaterMarkUtils.createIntruderPhoto(bitmapt, timeStamp, IntrudeSecurityManager.ICON_SYSTEM, ctx);
										//将bitmap压缩并保存
										ByteArrayOutputStream baos = new ByteArrayOutputStream();
										bitmapt.compress(Bitmap.CompressFormat.JPEG, 80, baos);
										byte[] finalBytes = baos.toByteArray();
										File photoSavePath = getPhotoSavePath();
										if (photoSavePath == null) {
											return;
										}
										String finalPicPath = "";
										FileOutputStream fos = null;
										try {
											fos = new FileOutputStream(photoSavePath);
											fos.write(finalBytes);
											fos.close();
											// 隐藏图片
											finalPicPath = mPDManager.onHidePic(photoSavePath.getPath(), null);
											FileOperationUtil.saveFileMediaEntry(finalPicPath, ctx);
											FileOperationUtil.deleteImageMediaEntry(photoSavePath.getPath(), ctx);
											PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
											pdm.notifySecurityChange();
										} catch (Exception e) {
											return;
										}

										IntruderPhotoInfo info = new IntruderPhotoInfo(finalPicPath, IntrudeSecurityManager.ICON_SYSTEM, timeStamp);
										mISManager.insertInfo(info);
										mISManager.setCatchTimes(mISManager.getCatchTimes() + 1);
										PreferenceTable.getInstance().putLong(PrefConst.KEY_LATEAST_PATH, finalPicPath.hashCode());
										IntrudeSecurityManager.sHasPicTakenAtSystemLockSaved = true;

										//这里判断的第二个参数实际不用考虑其意思，因为这个参数只有在经过解开系统锁后才会重置为0,所以可以用来判断是否已经经过系统解锁阶段
										//只有已经经过系统解锁，此处的跳转抓拍界面的操作才有可能需要执行
										if (!IntrudeSecurityManager.sHasPicShowedWhenUserPresent && IntrudeSecurityManager.sFailTimesAtSystLock == 0) {
											final Intent intent = new Intent(AppMasterApplication.getInstance(), IntruderCatchedActivity.class);
											intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
											intent.putExtra("pkgname", "from_systemlock");
											if (mLockManager == null) {
												mLockManager = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);
											}
											long delayTime = NORMAL_TIME;
											if (IntrudeSecurityManager.sEnterBrowser) {
												delayTime = LONG_TIME;
											}
											ThreadManager.getUiThreadHandler().postDelayed(new Runnable() {
												@Override
												public void run() {
													mLockManager.filterPackage(context.getPackageName(), 1000);
													context.startActivity(intent);
													IntrudeSecurityManager.sEnterBrowser = false;
													LeoLog.d("poha_admin", "set sHasTakenWhenUnlockSystemLock false 1");
													IntrudeSecurityManager.sHasTakenWhenUnlockSystemLock = false;
												}
											}, delayTime);
											LeoLog.i("delayTime", " 00 " + delayTime);
//											context.startActivity(intent);
										} else {
											IntrudeSecurityManager.sHasPicShowedWhenUserPresent = false;
										}
									}
								});
							}
						});
					}
				}, 1000);

			} catch (Throwable e) {
			} finally {
				if (mFlRoot != null && mWm != null) {
					mWm.removeView(mFlRoot);
					mFlRoot = null;
				}
			}
		}
	}

	public static final ComponentName getComponentName(Context context) {
		return new ComponentName(context, DeviceReceiverNewOne.class);
	}

	@Override
	public void onPasswordSucceeded(Context context, Intent intent) {

	}

	public static boolean isActive(Context context) {
		return ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(getComponentName(context));
	}

//	public static boolean isActivePasswordSufficient(Context context) {
//		return ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).isActivePasswordSufficient();
//	}

	public static int getFailTimesByApi(Context context) {
		try{
			return ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).getCurrentFailedPasswordAttempts();
		} catch (Throwable e) {
			return -1;
		}
	}

	private File getPhotoSavePath() {
		File picDir = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath());
		String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH).format(new Date());
		File dir = new File(picDir.getPath() + File.separator + "IntruderP");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return new File(dir + File.separator + "IMAGE_" + timeStamp + ".jpg");
	}
}