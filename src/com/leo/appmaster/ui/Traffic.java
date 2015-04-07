
package com.leo.appmaster.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.TrafficStats;
import android.text.format.Time;
import android.util.Log;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.Constants;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.ManagerFlowUtils;

public class Traffic {
    private static final String STATE_WIFI = "wifi";
    private static final String STATE_NO_NETWORK = "nonet";
    private static final String Tag = "TrafficAll";
    private Context mContext;
    private static Traffic mTraffic;
    private static AppMasterPreference s_preferences;
    private float[] gprs = {
            0, 0, 0
    };

    private Traffic(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static Traffic getInstance(Context context) {
        if (s_preferences == null) {
            s_preferences = AppMasterPreference.getInstance(context);
        }
        if (mTraffic == null) {
            mTraffic = new Traffic(context);
        }
        return mTraffic;
    }

    public float[] getAllgprs(int mVersion, String network_state) {

        long mBaseSend = s_preferences.getBaseSend();
        long mBaseRev = s_preferences.getBaseRev();
        long mGprsSend = s_preferences.getGprsSend();
        long mGprsRev = s_preferences.getGprsRev();

        // 5.0系统连接，断开wifi问题解决
        if (mVersion > 19) {
            if (network_state.equals(STATE_WIFI) || network_state.equals(STATE_NO_NETWORK)) {
                gprs[0] = mGprsSend;
                gprs[1] = mGprsRev;
                gprs[2] = gprs[0] + gprs[1];
                return gprs;
            }
        }

        // Log.d(Tag, "----頭炮！一開始檢測-----");
        // Log.d(Tag, "mBaseSend : " + mBaseSend / 1024 / 1024);
        // Log.d(Tag, "mGprsSend : " + mGprsSend / 1024 / 1024);
        // Log.d(Tag, "------------------------------------");

        int nowYear = getCurrentTime()[0];
        int nowMonth = getCurrentTime()[1];
        int nowDay = getCurrentTime()[2];
        String nowDayTime = ManagerFlowUtils.getNowTime();
        int lastSaveYear = 0;
        int lastSaveMonth = 0;
        int lastSaveDay = 0;
        String lastSaveDayTime = "";

        Cursor Testcursor = mContext.getContentResolver().query(Constants.MONTH_TRAFFIC_URI, null,
                null, null, null);
        if (Testcursor != null) {
            if (Testcursor.moveToLast()) {
                lastSaveDayTime = Testcursor.getString(1);
                lastSaveYear = Testcursor.getInt(4);
                lastSaveMonth = Testcursor.getInt(5);
                lastSaveDay = Testcursor.getInt(6);
            }
            Testcursor.close();
        }

        // 同一天内如果没进行数据上传或下载，不进行IO操作
        if (mBaseSend == TrafficStats.getMobileTxBytes()
                && mBaseRev == TrafficStats.getMobileRxBytes()
                && nowDayTime.equals(lastSaveDayTime)) {
            gprs[0] = mGprsSend;
            gprs[1] = mGprsRev;
            gprs[2] = gprs[0] + gprs[1];
            // Log.d(Tag, "----没上传下载----存储-----");
            // Log.d(Tag, "Same day , return gprs[2] : " + gprs[2] / 1024 /
            // 1024);
            // Log.d(Tag, "------------------------------------");
            return gprs;
        }

        if (TrafficStats.getMobileTxBytes() >= mBaseSend
                || TrafficStats.getMobileRxBytes() >= mBaseRev)
        {
            // Log.d(Tag, "--------正常状态接受-------！");
            // Log.d(Tag, "TrafficStats.getMobileTxBytes()  : " +
            // (TrafficStats.getMobileTxBytes())
            // / 1024 / 1024);
            // Log.d(Tag, "mBaseSend : " + (mBaseSend) / 1024 / 1024);
            // Log.d(Tag, "mGprsSend : " + (mGprsSend) / 1024 / 1024);
            // Log.d(Tag, "---------------------------------！");
            s_preferences.setGprsSend(TrafficStats.getMobileTxBytes() - mBaseSend + mGprsSend);
            s_preferences.setGprsRev(TrafficStats.getMobileRxBytes() - mBaseRev + mGprsRev);
        }
        else {
            s_preferences.setGprsSend(TrafficStats.getMobileTxBytes() + mGprsSend);
            s_preferences.setGprsRev(TrafficStats.getMobileRxBytes() + mGprsRev);
        }
        mBaseSend = TrafficStats.getMobileTxBytes();
        mBaseRev = TrafficStats.getMobileRxBytes();

        // Log.d(Tag, "-------!!基值!!-------");
        // LeoLog.d(Tag, "mBaseSend : " + mBaseSend / 1024 / 1024);
        // LeoLog.d(Tag, "mBaseRev: " + mBaseRev / 1024 / 1024);
        // Log.d(Tag, "-------------------------");

        s_preferences.setBaseSend(mBaseSend);
        s_preferences.setBaseRev(mBaseRev);

        gprs[0] = s_preferences.getGprsSend();
        gprs[1] = s_preferences.getGprsRev();
        gprs[2] = gprs[0] + gprs[1];
        // Log.d(Tag, "-------最终获取gprs[2]-------");
        // LeoLog.d(Tag, "gprs[2] : " + gprs[2] / 1024 / 1024);
        // Log.d(Tag, "--------------------------------------");
        // 每个月的天数
        int MonthOfDay = ManagerFlowUtils.getCurrentMonthDay();
        // 月结日
        int renewDay = s_preferences.getRenewDay();

        // 比较日期
        Cursor mCursor = mContext.getContentResolver().query(Constants.MONTH_TRAFFIC_URI, null,
                "daytime=?", new String[] {
                    nowDayTime
                }, null);
        if (mCursor != null) {
            if (!mCursor.moveToNext()) {
//                Log.d("testfuckflow", "新一天or月到来");
                ContentValues values = new ContentValues();
                values.put("daytime", nowDayTime);
                values.put("daymemory", 0);
                values.put("year", nowYear);
                values.put("month", nowMonth);
                values.put("day", nowDay);
                mContext.getContentResolver().insert(Constants.MONTH_TRAFFIC_URI, values);

//                Log.d("testfuckflow", "renewDay : " + renewDay);
//                Log.d("testfuckflow", "MonthOfDay : " + MonthOfDay);
//                Log.d("testfuckflow", "nowDay : " + nowDay);
//                Log.d("testfuckflow", "lastSaveDay : " + lastSaveDay);

                // 同年换月换日操作
                if (nowYear == lastSaveYear) {
                    // 分析，月结日坑，如果月结日在31号，但2月只有28天的情况。
                    // 月结日大于这个月天数
                    if (renewDay > MonthOfDay) {
//                        Log.d("testfuckflow", "renewDay > MonthOfDay");
                        if (nowMonth > lastSaveMonth) {
                            if (lastSaveDay < renewDay || nowDay > renewDay || nowDay == MonthOfDay) {
//                                LeoLog.d("testfuckflow", "1");
                                ReSetMonthTraffic();
                            } else {
                                s_preferences.setMonthGprsBase((long) (gprs[2] + s_preferences
                                        .getMonthGprsBase()));
                            }

                        } else {
                            if (nowDay == MonthOfDay) {
                                ReSetMonthTraffic();
//                                LeoLog.d("testfuckflow", "2");
                            } else {
                                s_preferences.setMonthGprsBase((long) (gprs[2] + s_preferences
                                        .getMonthGprsBase()));
                            }
                        }
                    } else {
//                        Log.d("testfuckflow", "renewDay <= MonthOfDay");
                        // 月结日 重置月流量计算
                        if (nowMonth > lastSaveMonth) {
                            if (lastSaveDay < renewDay || nowDay >= renewDay
                                    || nowDay == MonthOfDay) {
                                ReSetMonthTraffic();
//                                LeoLog.d("testfuckflow", "3");
                            } else {
                                s_preferences.setMonthGprsBase((long) (gprs[2] + s_preferences
                                        .getMonthGprsBase()));
                            }
                        } else {
                            if (nowDay >= renewDay && lastSaveDay < renewDay) {
                                ReSetMonthTraffic();
//                                LeoLog.d("testfuckflow", "4");
                            } else {
                                s_preferences.setMonthGprsBase((long) (gprs[2] + s_preferences
                                        .getMonthGprsBase()));
                            }
                        }
                    }
                    s_preferences.setGprsSend(0);
                    s_preferences.setGprsRev(0);
                    gprs[2] = 0;
                    // s_preferences.setItSelfTodayBase(0);
                } else if (nowYear > lastSaveYear) {
//                    LeoLog.d("testfuckflow", "5");
//                    Log.d(Tag, "换年咯,重置everything ! ");
                    s_preferences.setGprsSend(0);
                    s_preferences.setGprsRev(0);
                    ReSetMonthTraffic();
                    gprs[2] = 0;
                }
            } else {
                s_preferences
                        .setMonthGprsAll((long) (s_preferences.getMonthGprsBase() + gprs[2]));
                ContentValues values = new ContentValues();
                values.put("daymemory", gprs[2]);
                mContext.getContentResolver().update(Constants.MONTH_TRAFFIC_URI, values,
                        "daytime=?",
                        new String[] {
                            nowDayTime
                        });
            }
            mCursor.close();
        }

        long ItSelfBase = s_preferences.getItSelfTodayBase();
        // 如果设置了已用流量，那么会一直叠加，除非换月清零。
        if (s_preferences.getItselfMonthTraffic() > 0) {

            // 设置今日已用base
            if (ItSelfBase < 1 || ItSelfBase > gprs[2]) {
                s_preferences.setItSelfTodayBase((long) gprs[2]);
                ItSelfBase = s_preferences.getItSelfTodayBase();
            }

            long gprsKb = (long) (gprs[2] / 1024);
            ItSelfBase = ItSelfBase / 1024;

            s_preferences
                    .setItselfMonthTraffic((long) (gprsKb - ItSelfBase + s_preferences
                            .getItselfMonthTraffic()));
            LeoLog.d("testTraffic", "叠加是： " + (gprsKb - ItSelfBase) + s_preferences
                    .getItselfMonthTraffic());
            s_preferences.setItSelfTodayBase((long) gprs[2]);
        }

        return gprs;
    }

    private void ReSetMonthTraffic() {
//        Log.d(Tag, "月结日到了，重置月流量咯！！！！！！");
        // 换月，流量超额开关
        s_preferences.setAlotNotice(false);
        s_preferences.setFinishNotice(false);
        // 换月，已使用流量设置为0
        s_preferences.setItselfMonthTraffic(0);
        s_preferences.setMonthGprsBase(0);
        s_preferences.setMonthGprsAll(0);

        // if (s_preferences.getItselfMonthTraffic() > 0) {
        // s_preferences
        // .setItselfMonthTraffic(0);
        // }

    }

    // 获取系统时间。返回数组
    private int[] getCurrentTime() {
        int[] is = {
                0, 0, 0, 0, 0, 0
        };
        Time time = new Time();
        time.setToNow();
        is[0] = time.year;
        is[1] = time.month + 1;
        is[2] = time.monthDay;
        is[3] = time.hour;
        is[4] = time.minute;
        is[5] = time.second;
        return is;
    }

}
