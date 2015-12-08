package com.leo.appmaster.applocker.manager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import com.leo.appmaster.R;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

public class ChangeThemeManager {
    public static final int VERSION_CODE_NEED_CHANGE_TO_CHRISMAS_THEME = 62;
    public static final int BG_LOCKSCREEN_PASSWORD_NUM = 1;
    public static final int BG_LOCKSCREEN_GESTURE_DOT = 2;
    public static final int COUNT_LOCKSCREEN_DOT_THEME_BG = 10;
    public static final int BG_LOCKSCREEN_WHOLE = 6;
    public static final int BG_HOME_TAB = 3;
    public static final int BG_HOME_UPARROW = 4;
    public static final int BG_HOME_ASIDE_FRAGMENT = 5;
    public static final String DATE_FORMAT = "yyyy-MM-dd-HH:mm:ss";
    public static final String DATE_LOCK_BEFORE = "2015-12-24-00:00:00";
    public static final String DATE_LOCK_AFTER = "2015-12-26-00:00:00";
    public static final String DATE_HOME_BEFORE = "2015-12-14-00:00:00";
    public static final String DATE_HOME_AFTER = "2015-12-27-00:00:00";
    private static PreferenceTable mPt = PreferenceTable.getInstance();
    private static final int[] LOCKSCREEN_DOT_DRAWABLE_ID = 
        {R.drawable.gesture_chrismas_dot1,R.drawable.gesture_chrismas_dot2,R.drawable.gesture_chrismas_dot3,
        R.drawable.gesture_chrismas_dot4,R.drawable.gesture_chrismas_dot5,R.drawable.gesture_chrismas_dot6,
        R.drawable.gesture_chrismas_dot7,R.drawable.gesture_chrismas_dot8,R.drawable.gesture_chrismas_dot9,
        R.drawable.gesture_chrismas_dot10};
    
    public static Drawable getChrismasThemeDrawbleBySlotId (int slotId, Context context) {
        //获取versionCode
        if (!mPt.getBoolean(PrefConst.KEY_HOME_NEED_CHANGE_TO_CHRISMAS_THEME, true) && !mPt.getBoolean(PrefConst.KEY_LOCK_NEED_CHANGE_TO_CHRISMAS_THEME, true)) {
            return null;
        }
        PackageInfo info = null;
        try {
            info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        int versionCode = info.versionCode;
        if(versionCode != VERSION_CODE_NEED_CHANGE_TO_CHRISMAS_THEME ) {
            return null;
        } else {
            //对比时间
            Date now = new Date();
            SimpleDateFormat sdf=new SimpleDateFormat(DATE_FORMAT);
            Date homeChrismasThemeBefore = null;
            Date homeChrismasThemeAfter = null;
            Date lockScreenChrismasThemeBefore = null;
            Date lockScreenChrismasThemeAfter = null;
            try {
                homeChrismasThemeBefore=sdf.parse(DATE_HOME_BEFORE);
                homeChrismasThemeAfter=sdf.parse(DATE_HOME_AFTER);
                lockScreenChrismasThemeBefore=sdf.parse(DATE_LOCK_BEFORE);
                lockScreenChrismasThemeAfter=sdf.parse(DATE_LOCK_AFTER);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
            LeoLog.i("ChangeThemeManager", "homeChrismasThemeBefore = "+homeChrismasThemeBefore);
            LeoLog.i("ChangeThemeManager", "homeChrismasThemeAfter = "+homeChrismasThemeAfter);
            LeoLog.i("ChangeThemeManager", "lockScreenChrismasThemeBefore = "+lockScreenChrismasThemeBefore);
            LeoLog.i("ChangeThemeManager", "lockScreenChrismasThemeAfter = "+lockScreenChrismasThemeAfter);
            LeoLog.i("ChangeThemeManager", "now = "+now);
            switch (slotId) {
                case BG_LOCKSCREEN_PASSWORD_NUM:
                case BG_LOCKSCREEN_GESTURE_DOT:
                case BG_LOCKSCREEN_WHOLE:
                    if (now.after(lockScreenChrismasThemeAfter)) {
                        //TODO 标记置为false
                        mPt.putBoolean(PrefConst.KEY_LOCK_NEED_CHANGE_TO_CHRISMAS_THEME, false);
                        return null;
                    }
                    break;
                case BG_HOME_TAB:
                case BG_HOME_UPARROW:
                case BG_HOME_ASIDE_FRAGMENT:
                    if (now.after(homeChrismasThemeAfter)) {
                        mPt.putBoolean(PrefConst.KEY_HOME_NEED_CHANGE_TO_CHRISMAS_THEME, false);
                        return null;
                    }
                    break;
                default:
                    break;
            }
            //所有条件满足，选择并返回drawable
            Drawable drawableForReturn = null;
            switch (slotId) {
                case BG_LOCKSCREEN_PASSWORD_NUM:
                case BG_LOCKSCREEN_GESTURE_DOT:
                    drawableForReturn = randomADrawable(context);
                    break;
                case BG_HOME_TAB:
                    drawableForReturn = context.getResources().getDrawable(R.drawable.gesture_chrismas_dot1);
                    break;
                case BG_HOME_ASIDE_FRAGMENT:
                    drawableForReturn = context.getResources().getDrawable(R.drawable.gesture_chrismas_dot1);
                    break;
                case BG_LOCKSCREEN_WHOLE:
                    drawableForReturn = context.getResources().getDrawable(R.drawable.lockscreen_chrismas_bg);
                    break;
                default:
                    break;
            }
            return drawableForReturn;
        }
    }

    private static Drawable randomADrawable(Context context) {
        Random ran = new Random();
        int nextInt = ran.nextInt(10);
        return context.getResources().getDrawable(LOCKSCREEN_DOT_DRAWABLE_ID[nextInt]);
    }
}
