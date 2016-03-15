package com.leo.appmaster.intruderprotection;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.Constants;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.applocker.IntruderPhotoInfo;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.mgr.IntrudeSecurityManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.PrivacyDataManager;
import com.leo.appmaster.utils.BitmapUtils;
import com.leo.appmaster.utils.FileOperationUtil;
import com.leo.appmaster.utils.PrefConst;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by chenfs on 16-3-14.
 */
public class SystLockAdminReceiver extends DeviceAdminReceiver {
//    public static final String PASSWORD_FAILED_TIMES_OUT_OF_LIMITS = "com.leo.appmaster.action.FAILED_PASSWORD_ATTEMPTS_OUT_OF_LIMITS";
    private PrivacyDataManager mPDManager;
    private IntrudeSecurityManager mISManager;
    private FrameLayout mFlRoot = null;
    private WindowManager mWm = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        super.onPasswordChanged(context, intent);
    }

    @Override
    public void onPasswordExpiring(Context context, Intent intent) {
        super.onPasswordExpiring(context, intent);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return super.onDisableRequested(context, intent);
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);

    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        if (mPDManager == null) {
            mPDManager = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
        }
        if (mISManager == null) {
            mISManager = (IntrudeSecurityManager) MgrContext.getManager(MgrContext.MGR_INTRUDE_SECURITY);
        }
        boolean isOpen = mISManager.getSystIntruderProtecionSwitch();
//        PreferenceTable pt = PreferenceTable.getInstance();
        int failedPswAttempts = 9999;
        try {
            DevicePolicyManager mgr = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            failedPswAttempts = mgr.getCurrentFailedPasswordAttempts();
//            mgr.isActivePasswordSufficient();
        } catch (Exception e) {
        }

        if (isOpen && failedPswAttempts >= mISManager.getTimesForTakePhoto()) {
            try {
                final Context ctx = AppMasterApplication.getInstance();
                Toast.makeText(ctx, "to add view 2", Toast.LENGTH_SHORT).show();
                final CameraSurfacePreview cfp = new CameraSurfacePreview(ctx);
                final WindowManager mWM = (WindowManager) AppMasterApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
                if (mFlRoot == null) {
                    mFlRoot = new FrameLayout(ctx);
                }
                WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
                localLayoutParams.height = 100;
                localLayoutParams.width = 100;
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
                                        bitmapt = WaterMarkUtils.createIntruderPhoto(bitmapt, timeStamp, "com.leo.appmaster", ctx);
                                        //将bitmap压缩并保存
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        bitmapt.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                                        byte[] finalBytes = baos.toByteArray();
                                        File photoSavePath = getPhotoSavePath();
                                        if (photoSavePath == null) {
                                            return;
                                        }
                                        String finalPicPath = "";
                                        try {
                                            FileOutputStream fos = new FileOutputStream(photoSavePath);
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

                                        IntruderPhotoInfo info = new IntruderPhotoInfo(finalPicPath, "com.leo.appmaster", timeStamp);
                                        mISManager.insertInfo(info);
                                    }
                                });
                            }
                        });
                    }
                }, 500);

            } catch (Throwable e) {
                Toast.makeText(AppMasterApplication.getInstance(), e.toString(), Toast.LENGTH_SHORT).show();
            } finally {
                if (mFlRoot != null && mWm != null) {
                    mWm.removeView(mFlRoot);
                }
            }
        }
    }

    public static final ComponentName getComponentName(Context context) {
        return new ComponentName(context, SystLockAdminReceiver.class);
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {

    }

    public static final boolean isActive(Context context) {
        return ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(getComponentName(context));
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
