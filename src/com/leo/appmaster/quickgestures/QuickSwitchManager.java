
package com.leo.appmaster.quickgestures;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.R;
import com.leo.appmaster.quickgestures.model.QuickSwitcherInfo;
import com.leo.appmaster.quickgestures.ui.QuickGestureActivity;
import com.leo.appmaster.quickgestures.view.AppleWatchLayout;
import com.leo.appmaster.quickgestures.view.GestureItemView;
import com.leo.appmaster.quickgestures.view.SectorQuickGestureContainer;
import com.leo.appmaster.quickgestures.view.AppleWatchLayout;
import com.leo.appmaster.quickgestures.view.AppleWatchLayout.LayoutParams;
import com.leo.appmaster.utils.LeoLog;

import android.R.integer;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class QuickSwitchManager {

    private static QuickSwitchManager mInstance;
    public final static String BLUETOOTH = "bluetooth";
    public final static String FLASHLIGHT = "flashlight";
    public final static String WLAN = "wlan";
    public final static String CRAME = "carme";
    public final static String SOUND = "sound";
    public final static String LIGHT = "light";
    public final static String MSM_PACKAGE_NAME = "com.android.contacts";
    public final static String SPEEDUP = "speedup";
    public final static String CHANGEMODE = "changemode";
    public final static String SWITCHSET = "switchset";
    public final static String SETTING = "setting";
    public final static String GPS = "gps";
    public final static String FLYMODE = "flymode";
    public final static String ROTATION = "rotation";
    public final static String MOBILEDATA = "mobiledata";
    public final static String HOME = "home";
    public final static String XUKUANG = "xukuang";
    public final static String SYS_NO_READ_MESSAGE_TIP = "sys_no_read_message_tip";
    public final static String SYS_NO_READ_CALL_LOG_TIP = "sys_no_read_call_log_tip";
    public final static String PRIVACY_NO_READ_CONTACT_TIP = "privacy_no_read_contact_tip";
    private Context mContext;
    private static BluetoothAdapter mBluetoothAdapter;
    private WifiManager mWifimanager;
    public Camera mCamera;
    private AudioManager mSoundManager;
    private PowerManager mPowerManager;
    public static boolean isBlueToothOpen = false;
    public static boolean isFlashLightOpen = false;
    public static boolean isWlantOpen = false;
    public static boolean isGpsOpen = false;
    public static boolean isFlyModeOpen = false;
    public static boolean isRotationOpen = false;
    public static boolean isMobileDataOpen = false;
    private static int mSoundStatus;
    public final static int mSound = 0;
    public final static int mQuite = 1;
    public final static int mVibrate = 2;
    private static int mLightStatus;
    public static final int LIGHT_ERR = -1;
    public static final int LIGHT_AUTO = 0;
    public static final int LIGHT_NORMAL = 64;
    public static final int LIGHT_50_PERCENT = 127;
    public static final int LIGHT_100_PERCENT = 255;
    private RotationObserver mRotationObserver;
    private BrightObserver mBrightObserver;
    private GpsObserver mGpsObserver;
    private Vibrator vib;
    private LocationManager locationManager;
    private int isAirplaneMode;
    private int mRotateState;
    private ConnectivityManager mConnectivityManager;
    private Handler mHandler;
    private AppMasterPreference switchPreference;

    public static synchronized QuickSwitchManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new QuickSwitchManager(context);
        }
        return mInstance;
    }

    private QuickSwitchManager(Context context) {
        mContext = context.getApplicationContext();
        vib = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
        switchPreference = AppMasterPreference.getInstance(mContext);

        // 打开前判断每一个的状态
        blueTooth();
        wlan();
        sound();
        lightPower();
        gps();
        flyMode();
        rotation();
        mobileData();

        mHandler = new Handler();
        // 屏幕旋转观察者
        mRotationObserver = new RotationObserver(mHandler);
        mRotationObserver.startObserver();
        // 屏幕亮度观察者
        mBrightObserver = new BrightObserver(mHandler);
        mBrightObserver.startObserver();
        // GPS观察者
        mGpsObserver = new GpsObserver(mHandler);
        mGpsObserver.startObserver();
    }

    public void mobileData() {
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        try {
            boolean isMobileDataEnable = invokeMethod("getMobileDataEnabled",
                    null);
            if (isMobileDataEnable) {
                isMobileDataOpen = true;
            } else {
                isMobileDataOpen = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rotation() {
        try {
            mRotateState = android.provider.Settings.System.getInt(mContext.getContentResolver(),
                    android.provider.Settings.System.ACCELEROMETER_ROTATION);
            // 观察屏幕旋转开关
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mRotateState == 1) {
            isRotationOpen = true;
        } else {
            isRotationOpen = false;
        }
    }

    public void flyMode() {
        isAirplaneMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0);
        if (isAirplaneMode == 1) {
            isFlyModeOpen = true;
        } else {
            isFlyModeOpen = false;
        }
    }

    private void gps() {
        if (locationManager == null) {
            locationManager = (LocationManager) mContext
                    .getSystemService(Context.LOCATION_SERVICE);
        }
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            isGpsOpen = true;
        } else {
            isGpsOpen = false;
        }
    }

    public void sound() {
        if (mSoundManager == null) {
            mSoundManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        if (mSoundManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            mSoundStatus = 0;
        } else if (mSoundManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            mSoundStatus = 1;
        } else if (mSoundManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
            mSoundStatus = 2;
        }
    }

    public void wlan() {
        if (mWifimanager == null) {
            mWifimanager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        }
        if (mWifimanager.isWifiEnabled()) {
            isWlantOpen = true;
        } else {
            isWlantOpen = false;
        }
    }

    public void blueTooth() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter
                    .getDefaultAdapter();
        }
        if (mBluetoothAdapter.isEnabled()) {
            isBlueToothOpen = true;
        } else {
            isBlueToothOpen = false;
        }
    }

    private void lightPower() {
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        }
        refreshLightButton();
    }

    // 更新按钮
    private void refreshLightButton()
    {
        switch (getBrightStatus())
        {
            case LIGHT_AUTO:
                mLightStatus = LIGHT_AUTO;
                break;
            case LIGHT_NORMAL:
                mLightStatus = LIGHT_NORMAL;
                break;
            case LIGHT_50_PERCENT:
                mLightStatus = LIGHT_50_PERCENT;
                break;
            case LIGHT_100_PERCENT:
                mLightStatus = LIGHT_100_PERCENT;
                break;
            case LIGHT_ERR:
                mLightStatus = LIGHT_ERR;
                break;
        }
    }

    // 得到当前亮度值状态
    private int getBrightStatus() {
        int light = 0;
        boolean auto = false;
        ContentResolver cr = mContext.getContentResolver();
        try {
            auto = Settings.System.getInt(cr,
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            if (!auto) {
                light = android.provider.Settings.System.getInt(cr,
                        Settings.System.SCREEN_BRIGHTNESS, -1);
                if (light > 0 && light <= LIGHT_NORMAL) {
                    return LIGHT_NORMAL;
                } else if (light > LIGHT_NORMAL && light <= LIGHT_50_PERCENT) {
                    return LIGHT_50_PERCENT;
                } else if (light > LIGHT_50_PERCENT && light <= LIGHT_100_PERCENT) {
                    return LIGHT_100_PERCENT;
                }
            } else {
                return LIGHT_AUTO;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return LIGHT_ERR;
    }

    public Drawable[] getIconFromName(String IdentiName) {
        Drawable[] iCons = null;
        if (IdentiName.equals(BLUETOOTH)) {
            iCons = new Drawable[2];
            iCons[0] = mContext.getResources().getDrawable(
                    R.drawable.switch_bluetooth_pre);
            iCons[1] = mContext.getResources().getDrawable(R.drawable.switch_bluetooth);
        } else if (IdentiName.equals(FLASHLIGHT)) {
            iCons = new Drawable[2];
            iCons[0] = mContext.getResources().getDrawable(
                    R.drawable.switch_flashlight_pre);
            iCons[1] = mContext.getResources().getDrawable(
                    R.drawable.switch_flashlight);
        } else if (IdentiName.equals(WLAN)) {
            iCons = new Drawable[2];
            iCons[0] = mContext.getResources().getDrawable(R.drawable.switch_wifi_pre);
            iCons[1] = mContext.getResources().getDrawable(R.drawable.switch_wifi);
        } else if (IdentiName.equals(CRAME)) {
            iCons = new Drawable[1];
            iCons[0] = mContext.getResources().getDrawable(R.drawable.switch_camera);
        } else if (IdentiName.equals(SOUND)) {
            iCons = new Drawable[3];
            iCons[0] = mContext.getResources().getDrawable(R.drawable.switch_volume_min);
            iCons[1] = mContext.getResources()
                    .getDrawable(R.drawable.switch_volume_mute);
            iCons[2] = mContext.getResources().getDrawable(
                    R.drawable.switch_volume_vibration);
        } else if (IdentiName.equals(LIGHT)) {
            iCons = new Drawable[4];
            iCons[0] = mContext.getResources().getDrawable(
                    R.drawable.switch_brightness_automatic);
            iCons[1] = mContext.getResources()
                    .getDrawable(R.drawable.switch_brightness_min);
            iCons[2] = mContext.getResources().getDrawable(
                    R.drawable.switch_brightness_half);
            iCons[3] = mContext.getResources().getDrawable(
                    R.drawable.switch_brightness_max);
        } else if (IdentiName.equals(SPEEDUP)) {
            iCons = new Drawable[1];
            iCons[0] = mContext.getResources().getDrawable(R.drawable.switch_speed_up);
        } else if (IdentiName.equals(SWITCHSET)) {
            iCons = new Drawable[1];
            iCons[0] = mContext.getResources().getDrawable(
                    R.drawable.switch_gestureset_pre);
        } else if (IdentiName.equals(CHANGEMODE)) {
            iCons = new Drawable[1];
            iCons[0] = mContext.getResources().getDrawable(R.drawable.switch_mode);
        } else if (IdentiName.equals(MOBILEDATA)) {
            iCons = new Drawable[2];
            iCons[0] = mContext.getResources().getDrawable(
                    R.drawable.switch_data_pre);
            iCons[1] = mContext.getResources().getDrawable(
                    R.drawable.switch_data);
        } else if (IdentiName.equals(SETTING)) {
            iCons = new Drawable[1];
            iCons[0] = mContext.getResources().getDrawable(
                    R.drawable.switch_gestureset_pre);
        } else if (IdentiName.equals(GPS)) {
            iCons = new Drawable[2];
            iCons[0] = mContext.getResources().getDrawable(R.drawable.switch_gps_pre);
            iCons[1] = mContext.getResources().getDrawable(R.drawable.switch_gps);
        } else if (IdentiName.equals(ROTATION)) {
            iCons = new Drawable[2];
            iCons[0] = mContext.getResources().getDrawable(
                    R.drawable.switch_rotation_pre);
            iCons[1] = mContext.getResources().getDrawable(
                    R.drawable.switch_rotation);
        } else if (IdentiName.equals(HOME)) {
            iCons = new Drawable[1];
            iCons[0] = mContext.getResources().getDrawable(
                    R.drawable.switch_home);
        } else if (IdentiName.equals(FLYMODE)) {
            iCons = new Drawable[2];
            iCons[0] = mContext.getResources().getDrawable(
                    R.drawable.switch_flightmode_pre);
            iCons[1] = mContext.getResources().getDrawable(
                    R.drawable.switch_flightmode);
        } else if (IdentiName.equals(XUKUANG)) {
            iCons = new Drawable[1];
            iCons[0] = mContext.getResources().getDrawable(R.drawable.switch_add);
        }
        return iCons;
    }

    public String getLabelFromName(String IdentiName) {
        String mLabel = "";
        if (IdentiName.equals(BLUETOOTH)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_bluetooth);
        } else if (IdentiName.equals(FLASHLIGHT)) {
            mLabel = mContext.getResources()
                    .getString(R.string.quick_guesture_flashlight);
        } else if (IdentiName.equals(WLAN)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_wlan);
        } else if (IdentiName.equals(CRAME)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_carme);
        } else if (IdentiName.equals(SOUND)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_sound);
        } else if (IdentiName.equals(LIGHT)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_light);
        } else if (IdentiName.equals(SPEEDUP)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_speedup);
        } else if (IdentiName.equals(SWITCHSET)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_switchset);
        } else if (IdentiName.equals(CHANGEMODE)) {
            mLabel = mContext.getResources()
                    .getString(R.string.quick_guesture_changemode);
        } else if (IdentiName.equals(MOBILEDATA)) {
            mLabel = mContext.getResources()
                    .getString(R.string.quick_guesture_mobliedata);
        } else if (IdentiName.equals(SETTING)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_setting);
        } else if (IdentiName.equals(GPS)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_gps);
        } else if (IdentiName.equals(ROTATION)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_rotation);
        } else if (IdentiName.equals(HOME)) {
            mLabel = mContext.getResources()
                    .getString(R.string.quick_guesture_home);
        } else if (IdentiName.equals(FLYMODE)) {
            mLabel = mContext.getResources().getString(R.string.quick_guesture_flymode);
        }
        return mLabel;
    }

    public List<QuickSwitcherInfo> getSwitchList(int switchNum) {
        List<QuickSwitcherInfo> mSwitchList = new ArrayList<QuickSwitcherInfo>();
        // 蓝牙开关
        QuickSwitcherInfo lanyaInfo = new QuickSwitcherInfo();
        lanyaInfo.iDentiName = BLUETOOTH;
        lanyaInfo.switchIcon = getIconFromName(BLUETOOTH);
        lanyaInfo.label = getLabelFromName(BLUETOOTH);
        lanyaInfo.position = 0;
        mSwitchList.add(lanyaInfo);
        // 手电筒
        QuickSwitcherInfo flashlightInfo = new QuickSwitcherInfo();
        flashlightInfo.iDentiName = FLASHLIGHT;
        flashlightInfo.switchIcon = getIconFromName(FLASHLIGHT);
        flashlightInfo.label = getLabelFromName(FLASHLIGHT);
        flashlightInfo.position = 1;
        mSwitchList.add(flashlightInfo);
        // WLAN
        QuickSwitcherInfo wlanInfo = new QuickSwitcherInfo();
        wlanInfo.iDentiName = WLAN;
        wlanInfo.switchIcon = getIconFromName(WLAN);
        wlanInfo.label = getLabelFromName(WLAN);
        wlanInfo.position = 2;
        mSwitchList.add(wlanInfo);
        // 相机
        QuickSwitcherInfo carmeInfo = new QuickSwitcherInfo();
        carmeInfo.iDentiName = CRAME;
        carmeInfo.switchIcon = getIconFromName(CRAME);
        carmeInfo.label = getLabelFromName(CRAME);
        carmeInfo.position = 3;
        mSwitchList.add(carmeInfo);
        // 声音
        QuickSwitcherInfo soundInfo = new QuickSwitcherInfo();
        soundInfo.iDentiName = SOUND;
        soundInfo.switchIcon = getIconFromName(SOUND);
        soundInfo.label = getLabelFromName(SOUND);
        soundInfo.position = 4;
        mSwitchList.add(soundInfo);
        // 亮度
        QuickSwitcherInfo lightInfo = new QuickSwitcherInfo();
        lightInfo.iDentiName = LIGHT;
        lightInfo.switchIcon = getIconFromName(LIGHT);
        lightInfo.label = getLabelFromName(LIGHT);
        lightInfo.position = 5;
        mSwitchList.add(lightInfo);
        // 加速
        QuickSwitcherInfo speedUpInfo = new QuickSwitcherInfo();
        speedUpInfo.iDentiName = SPEEDUP;
        speedUpInfo.switchIcon = getIconFromName(SPEEDUP);
        speedUpInfo.label = getLabelFromName(SPEEDUP);
        speedUpInfo.position = 6;
        mSwitchList.add(speedUpInfo);
        // 手势设置
        QuickSwitcherInfo switchSetInfo = new QuickSwitcherInfo();
        switchSetInfo.iDentiName = SWITCHSET;
        switchSetInfo.switchIcon = getIconFromName(SWITCHSET);
        switchSetInfo.label = getLabelFromName(SWITCHSET);
        switchSetInfo.position = 7;
        mSwitchList.add(switchSetInfo);
        // 情景模式切换
        QuickSwitcherInfo changeModeInfo = new QuickSwitcherInfo();
        changeModeInfo.iDentiName = CHANGEMODE;
        changeModeInfo.switchIcon = getIconFromName(CHANGEMODE);
        changeModeInfo.label = getLabelFromName(CHANGEMODE);
        changeModeInfo.position = 8;
        mSwitchList.add(changeModeInfo);
        // 移动数据
        QuickSwitcherInfo mobileDataInfo = new QuickSwitcherInfo();
        mobileDataInfo.iDentiName = MOBILEDATA;
        mobileDataInfo.switchIcon = getIconFromName(MOBILEDATA);
        mobileDataInfo.label = getLabelFromName(MOBILEDATA);
        mobileDataInfo.position = 9;
        mSwitchList.add(mobileDataInfo);
        // 系统设置
        QuickSwitcherInfo settingInfo = new QuickSwitcherInfo();
        settingInfo.iDentiName = SETTING;
        settingInfo.switchIcon = getIconFromName(SETTING);
        settingInfo.label = getLabelFromName(SETTING);
        settingInfo.position = 10;
        mSwitchList.add(settingInfo);
        // GPS
        QuickSwitcherInfo gpsInfo = new QuickSwitcherInfo();
        gpsInfo.iDentiName = GPS;
        gpsInfo.switchIcon = getIconFromName(GPS);
        gpsInfo.label = getLabelFromName(GPS);
        gpsInfo.position = 11;
        mSwitchList.add(gpsInfo);
        // 屏幕旋转
        QuickSwitcherInfo rotationInfo = new QuickSwitcherInfo();
        rotationInfo.iDentiName = ROTATION;
        rotationInfo.switchIcon = getIconFromName(ROTATION);
        rotationInfo.label = getLabelFromName(ROTATION);
        rotationInfo.position = 12;
        mSwitchList.add(rotationInfo);
        // Home
        QuickSwitcherInfo homeInfo = new QuickSwitcherInfo();
        homeInfo.iDentiName = HOME;
        homeInfo.switchIcon = getIconFromName(HOME);
        homeInfo.label = getLabelFromName(HOME);
        homeInfo.position = 13;
        mSwitchList.add(homeInfo);
        // 飞行模式
        QuickSwitcherInfo flyModeInfo = new QuickSwitcherInfo();
        flyModeInfo.iDentiName = FLYMODE;
        flyModeInfo.switchIcon = getIconFromName(FLYMODE);
        flyModeInfo.label = getLabelFromName(FLYMODE);
        flyModeInfo.position = 14;
        mSwitchList.add(flyModeInfo);
        return mSwitchList;
    }

    public void toggleWlan(SectorQuickGestureContainer mContainer, List<QuickSwitcherInfo> list,
            AppleWatchLayout quickGestureLayout) {
        if (!mWifimanager.isWifiEnabled()) {
            mWifimanager.setWifiEnabled(true);
            isWlantOpen = true;
        } else {
            mWifimanager.setWifiEnabled(false);
            isWlantOpen = false;
        }
        mContainer.fillSwitchItem(quickGestureLayout, list);
    }

    public void toggleBluetooth(SectorQuickGestureContainer mContainer, List<QuickSwitcherInfo> list,
            AppleWatchLayout quickGestureLayout) {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter
                    .getDefaultAdapter();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            isBlueToothOpen = true;
        } else {
            mBluetoothAdapter.disable();
            isBlueToothOpen = false;
        }
        mContainer.fillSwitchItem(quickGestureLayout, list);
    }

    public void toggleSound(SectorQuickGestureContainer mContainer, List<QuickSwitcherInfo> switchList,
            AppleWatchLayout quickGestureLayout) {
        if (mSoundManager == null) {
            mSoundManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        if (mSoundManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            mSoundManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            mSoundStatus = mQuite;
        } else if (mSoundManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            mSoundManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            mSoundStatus = mVibrate;
            if (vib == null) {
                vib = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
            }
            vib.vibrate(150);
        } else if (mSoundManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
            mSoundManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            mSoundStatus = mSound;
        }
        mContainer.fillSwitchItem(quickGestureLayout, switchList);
    }

    public void toggleFlashLight(SectorQuickGestureContainer mContainer, List<QuickSwitcherInfo> list,
            AppleWatchLayout quickGestureLayout) {
        if (!isFlashLightOpen) {
            isFlashLightOpen = true;
            try {
                mCamera = Camera.open();
            } catch (Exception e) {
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
                isFlashLightOpen = false;
                return;
            }
            try {
                Parameters params = mCamera.getParameters();
                params.setFlashMode(Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(params);
                mCamera.startPreview();
            } catch (Exception ee) {
                return;
            }
        } else {
            try {
                isFlashLightOpen = false;
                Parameters params = mCamera.getParameters();
                params.setFlashMode(Parameters.FLASH_MODE_OFF);
                mCamera.stopPreview();
                mCamera.release();
            } catch (Exception e) {
                isFlashLightOpen = false;
                return;
            }
        }
        mContainer.fillSwitchItem(quickGestureLayout, list);
    }

    public static boolean checkBlueTooth() {
        if (isBlueToothOpen) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkFlashLight() {
        if (isFlashLightOpen) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkWlan() {
        if (isWlantOpen) {
            return true;
        } else {
            return false;
        }
    }

    public static int checkSound() {
        if (mSoundStatus == mSound) {
            return mSound;
        } else if (mSoundStatus == mQuite) {
            return mQuite;
        } else {
            return mVibrate;
        }
    }

    public static int checkLight() {
        if (mLightStatus == LIGHT_AUTO) {
            return LIGHT_AUTO;
        } else if (mLightStatus == LIGHT_NORMAL) {
            return LIGHT_NORMAL;
        } else if (mLightStatus == LIGHT_50_PERCENT) {
            return LIGHT_50_PERCENT;
        } else if (mLightStatus == LIGHT_100_PERCENT) {
            return LIGHT_100_PERCENT;
        } else {
            return LIGHT_ERR;
        }
    }

    public void openCrame() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    public void toggleLight(SectorQuickGestureContainer mContainer, List<QuickSwitcherInfo> switchList,
            AppleWatchLayout quickGestureLayout) {
        int light = 0;
        switch (getBrightStatus()) {
            case LIGHT_AUTO:
                light = LIGHT_NORMAL - 1;
                stopAutoBrightness(mContext.getContentResolver());
                mLightStatus = LIGHT_NORMAL;
                break;
            case LIGHT_NORMAL:
                light = LIGHT_50_PERCENT - 1;
                mLightStatus = LIGHT_50_PERCENT;
                break;
            case LIGHT_50_PERCENT:
                light = LIGHT_100_PERCENT - 1;
                mLightStatus = LIGHT_100_PERCENT;
                break;
            case LIGHT_100_PERCENT:
                startAutoBrightness(mContext.getContentResolver());
                mLightStatus = LIGHT_AUTO;
                break;
            case LIGHT_ERR:
                light = LIGHT_NORMAL - 1;
                break;
        }
        setLight(light);
        setScreenLightValue(mContext.getContentResolver(), light);
        mContainer.fillSwitchItem(quickGestureLayout, switchList);
    }

    /*
     * 因为PowerManager提供的函数setBacklightBrightness接口是隐藏的，
     * 所以在基于第三方开发调用该函数时，只能通过反射实现在运行时调用
     */
    private void setLight(int light) {
        try {
            // 得到PowerManager类对应的Class对象
            Class<?> pmClass = Class.forName(mPowerManager.getClass().getName());
            // 得到PowerManager类中的成员mService（mService为PowerManagerService类型）
            Field field = pmClass.getDeclaredField("mService");
            field.setAccessible(true);
            // 实例化mService
            Object iPM = field.get(mPowerManager);
            // 得到PowerManagerService对应的Class对象
            Class<?> iPMClass = Class.forName(iPM.getClass().getName());
            /*
             * 得到PowerManagerService的函数setBacklightBrightness对应的Method对象，
             * PowerManager的函数setBacklightBrightness实现在PowerManagerService中
             */
            Method method = iPMClass.getDeclaredMethod("setBacklightBrightness", int.class);
            method.setAccessible(true);
            // 调用实现PowerManagerService的setBacklightBrightness
            method.invoke(iPM, light);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 启动自动调节亮度
    public void startAutoBrightness(ContentResolver cr)
    {
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    // 关闭自动调节亮度
    public void stopAutoBrightness(ContentResolver cr)
    {
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    // 设置改变亮度值
    public void setScreenLightValue(ContentResolver resolver, int value)
    {
        android.provider.Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS,
                value);
    }

    public void speedUp() {
        Toast.makeText(mContext, "加你的头～方案还没出！", 0).show();
    }

    public void toggleMode() {
        Toast.makeText(mContext, "切你的头～方案还没出！", 0).show();
    }

    public void switchSet() {
        Intent intent = new Intent(mContext, QuickGestureActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    public void goSetting() {
        Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    public static boolean checkGps() {
        if (isGpsOpen) {
            return true;
        } else {
            return false;
        }
    }

    public void toggleGPS() {
        try {
            Intent callGPSSettingIntent = new Intent(
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            callGPSSettingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(callGPSSettingIntent);
        } catch (Exception e) {
            Toast.makeText(mContext, "GPS is not available!", 0).show();
        }
    }

    public static boolean checkFlyMode() {
        if (isFlyModeOpen) {
            return true;
        } else {
            return false;
        }
    }

    public void toggleFlyMode() {
        try {
            Intent callFLYSettingIntent = new Intent(
                    android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            callFLYSettingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(callFLYSettingIntent);
        } catch (Exception e) {
            Toast.makeText(mContext, "Flight mode is not available!", 0).show();
        }
    }

    public static boolean checkRotation() {
        if (isRotationOpen) {
            return true;
        } else {
            return false;
        }
    }

    public void toggleRotation(SectorQuickGestureContainer mContainer, List<QuickSwitcherInfo> list,
            AppleWatchLayout quickGestureLayout) {
        try {
            mRotateState = android.provider.Settings.System.getInt(mContext.getContentResolver(),
                    android.provider.Settings.System.ACCELEROMETER_ROTATION);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mRotateState == 1) {
            // 得到uri
            Uri uri = android.provider.Settings.System.getUriFor("accelerometer_rotation");
            // 沟通设置status的值改变屏幕旋转设置
            android.provider.Settings.System.putInt(mContext.getContentResolver(),
                    "accelerometer_rotation",
                    0);
            // 通知改变
            mContext.getContentResolver().notifyChange(uri, null);
            isRotationOpen = false;
        } else {
            // 得到uri
            Uri uri = android.provider.Settings.System.getUriFor("accelerometer_rotation");
            // 沟通设置status的值改变屏幕旋转设置
            android.provider.Settings.System.putInt(mContext.getContentResolver(),
                    "accelerometer_rotation",
                    1);
            // 通知改变
            mContext.getContentResolver().notifyChange(uri, null);
            isRotationOpen = true;
        }
        mContainer.fillSwitchItem(quickGestureLayout, list);
    }

    // 观察屏幕旋转设置变化
    public class RotationObserver extends ContentObserver {
        ContentResolver mRotationResolver;

        public RotationObserver(Handler handler) {
            super(handler);
            mRotationResolver = mContext.getContentResolver();
        }

        // 屏幕旋转设置改变时调用
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            // 更新按钮状态
            LeoLog.d("QuickSwitchManager", " 屏幕旋转设置改变!!");
            rotation();
        }

        public void startObserver() {
            mRotationResolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.ACCELEROMETER_ROTATION), false,
                    this);
        }

        public void stopObserver() {
            mRotationResolver.unregisterContentObserver(this);
        }
    }

    // 观察亮度设置变化
    private class BrightObserver extends ContentObserver {
        ContentResolver mBrightResolver;

        public BrightObserver(Handler handler) {
            super(handler);
            mBrightResolver = mContext.getContentResolver();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            lightPower();
        }

        // 注册观察
        public void startObserver() {
            mBrightResolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.SCREEN_BRIGHTNESS), false,
                    this);
            mBrightResolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false,
                    this);
        }

        // 解除观察
        public void stopObserver() {
            mBrightResolver.unregisterContentObserver(this);
        }
    }

    // 观察GPS设置变化
    private class GpsObserver extends ContentObserver {
        ContentResolver mGpsResolver;

        public GpsObserver(Handler handler) {
            super(handler);
            mGpsResolver = mContext.getContentResolver();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            gps();
        }

        // 注册观察
        public void startObserver() {
            mGpsResolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.LOCATION_PROVIDERS_ALLOWED),
                    false, this);
        }

        // 解除观察
        public void stopObserver() {
            mGpsResolver.unregisterContentObserver(this);
        }
    }

    public static boolean checkMoblieData() {
        if (isMobileDataOpen) {
            return true;
        } else {
            return false;
        }
    }

    public void toggleMobileData(SectorQuickGestureContainer mContainer,
            List<QuickSwitcherInfo> switchList, AppleWatchLayout quickGestureLayout) {
        Object[] arg = null;
        try {
            boolean isMobileDataEnable = invokeMethod("getMobileDataEnabled",
                    arg);
            if (isMobileDataEnable) {
                invokeBooleanArgMethod("setMobileDataEnabled", false);
                isMobileDataOpen = false;
            } else {
                invokeBooleanArgMethod("setMobileDataEnabled", true);
                isMobileDataOpen = true;
            }
            mContainer.fillSwitchItem(quickGestureLayout, switchList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean invokeMethod(String methodName, Object[] arg)
            throws Exception {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        Class ownerClass = mConnectivityManager.getClass();
        Class[] argsClass = null;
        if (arg != null) {
            argsClass = new Class[1];
            argsClass[0] = arg.getClass();
        }
        Method method = ownerClass.getMethod(methodName, argsClass);
        Boolean isOpen = (Boolean) method.invoke(mConnectivityManager, arg);
        return isOpen;
    }

    public Object invokeBooleanArgMethod(String methodName, boolean value)
            throws Exception {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        Class ownerClass = mConnectivityManager.getClass();
        Class[] argsClass = new Class[1];
        argsClass[0] = boolean.class;
        Method method = ownerClass.getMethod(methodName, argsClass);
        return method.invoke(mConnectivityManager, value);
    }

    public void goHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        mContext.startActivity(intent);
    }

    public String ListToString(List<QuickSwitcherInfo> mSwitchList, int mNum) {
        String ListString = "";
        for (int i = 0; i < mNum; i++) {
            QuickSwitcherInfo switchInfo = mSwitchList.get(i);
            String name = switchInfo.iDentiName;
            int position = switchInfo.position;

            LeoLog.d("QuickSwitchManager", "name : " + name + "--position : " + position);
            if (i == 0) {
                ListString = name + ":" + position;
            } else {
                ListString = ListString + "," + name + ":" + position;
            }
        }
        return ListString;
    }

    public List<QuickSwitcherInfo> StringToList(String mSwitchListFromSp) {
        List<QuickSwitcherInfo> mSwitcherList = new ArrayList<QuickSwitcherInfo>();
        String[] mSwitchAllInfo = mSwitchListFromSp.split(",");
        for (int i = 0; i < mSwitchAllInfo.length; i++) {
            QuickSwitcherInfo mInfo = new QuickSwitcherInfo();
            String[] mEachOneInfo = mSwitchAllInfo[i].split(":");
            LeoLog.d(
                    "QuickSwitchManager",
                    "name : " + mEachOneInfo[0] + "--position : "
                            + Integer.parseInt(mEachOneInfo[1]));
            mInfo.iDentiName = mEachOneInfo[0];
            mInfo.position = Integer.parseInt(mEachOneInfo[1]);
            mInfo.label = getLabelFromName(mEachOneInfo[0]);
            mInfo.switchIcon = getIconFromName(mEachOneInfo[0]);
            mSwitcherList.add(mInfo);
        }
        LeoLog.d("QuickSwitchManager",
                "List size is : " + mSwitcherList.size());
        return mSwitcherList;
    }

    public String getListStringFromSp() {
        return switchPreference.getSwitchList();
    }

    // public QuickSwitcherInfo getXuKuangInfo() {
    // QuickSwitcherInfo mXuKuang = new QuickSwitcherInfo();
    // mXuKuang.label = "";
    // mXuKuang.iDentiName = XUKUANG;
    // mXuKuang.switchIcon = getIconFromName(XUKUANG);
    // return mXuKuang;
    // }

//    public GestureItemView getXuKuang(View hitView) {
//        LayoutParams params = (LayoutParams) hitView.getLayoutParams();
//        GestureItemView mIvXuKuang = new GestureItemView(mContext);
//        mIvXuKuang.setLayoutParams(params);
//        mIvXuKuang.setBackground(mContext.getResources().getDrawable(R.drawable.switch_add));
//        return mIvXuKuang;
//    }
}
