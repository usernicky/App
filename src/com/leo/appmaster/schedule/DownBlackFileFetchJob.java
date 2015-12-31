package com.leo.appmaster.schedule;

import android.content.Context;
import android.telecom.Call;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.HttpRequestAgent;
import com.leo.appmaster.callfilter.CallFilterConstants;
import com.leo.appmaster.callfilter.CallFilterUtils;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.impl.CallFilterContextManagerImpl;
import com.leo.appmaster.utils.NetWorkUtil;
import com.leo.appmaster.utils.Utilities;

/**
 * Created by runlee on 15-12-24.
 */
public class DownBlackFileFetchJob extends FetchScheduleJob {
    /**
     * 拉取间隔，24小时
     */
    private static final int FETCH_PERIOD = 24 * 60 * 60 * 1000;

    public static void startImmediately() {
        startWork();
    }

    @Override
    protected void work() {
        startWork();
    }

    @Override
    protected void onFetchSuccess(Object response, boolean noMidify) {
        super.onFetchSuccess(response, noMidify);
        StringBuilder sbName = new StringBuilder();
        String countryId = Utilities.getCountryID(AppMasterApplication.getInstance());
        sbName.append(countryId);
        sbName.append(CallFilterConstants.GZIP);

        StringBuilder sb = new StringBuilder();
        sb.append(CallFilterUtils.getBlackPath());
        sb.append(sbName.toString());
        String filePath = sb.toString();
        CallFilterUtils.parseBlactList(filePath);
    }

    @Override
    protected void onFetchFail(VolleyError error) {
        super.onFetchFail(error);
    }

    private static void startWork() {
         /*存在wifi网络再去拉取*/
        if (NetWorkUtil.isWifiConnected(AppMasterApplication.getInstance())) {
            DownBlackFileFetchJob job = new DownBlackFileFetchJob();
            FetchScheduleListener listener = job.newJsonObjListener();
            Context context = AppMasterApplication.getInstance();
            String filePath = getBlackFilePath();
            CallFilterContextManagerImpl pm = (CallFilterContextManagerImpl) MgrContext.getManager(MgrContext.MGR_CALL_FILTER);
            String uri = pm.getSerBlackFilePath();
            if (TextUtils.isEmpty(uri)) {
                return;
            }
            HttpRequestAgent.getInstance(context).downloadBlackList(filePath, listener, listener);
        }
    }

    @Override
    protected int getPeriod() {
        return FETCH_PERIOD;
    }

    public static String getBlackFilePath() {
        StringBuilder sbName = new StringBuilder();
        String countryId = Utilities.getCountryID(AppMasterApplication.getInstance());
        sbName.append(countryId);
        sbName.append(CallFilterConstants.GZIP);

        StringBuilder sb = new StringBuilder();
        sb.append(CallFilterUtils.getBlackPath());
        sb.append(sbName.toString());
        return sb.toString();
    }
}
