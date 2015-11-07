package com.leo.appmaster.applocker.manager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.Constants;
import com.leo.appmaster.utils.LeoLog;
import com.mobvista.sdk.m.core.AdListener;
import com.mobvista.sdk.m.core.AdTrackingListener;
import com.mobvista.sdk.m.core.MobvistaAd;
import com.mobvista.sdk.m.core.MobvistaAdNative;
import com.mobvista.sdk.m.core.MobvistaAdWall;
import com.mobvista.sdk.m.core.entity.Campaign;

/**
 * 广告相关引擎
 *  广告加载、回调
 *  注册广告点击
 *  创建appwall类型广告接口
 * @author Jasper
 *
 */
public class MobvistaEngine {
    private static final String TAG = "MobvistaEngine";
    
    private Context mAppContext;
    
    /**
     * 请求参数为null
     */
    public static final int ERR_PARAMS_NULL = -1000;
    /**
     * mobvista请求失败，详细失败原因见返回的msg
     */
    public static final int ERR_MOBVISTA_FAIL = -1001;
    /**
     * 请求成功，但返回的结构体为null
     */
    public static final int ERR_MOBVISTA_RESULT_NULL = -1002;

    /**
     * unid id 为空
     */
    public static final int ERR_UNITID_NULL = -1003;
    /**
     * 找不到对应的placement id
     */
    public static final int ERR_NOT_FOUND_PLACEMENTID = -1004;
    /**
     * 请求成功
     */
    public static final int ERR_OK = 0;
    
    /**
     * 广告过期时间, 1小时
     */
    private static final int AD_TIMEOUT = 60 * 60 * 1000;
    
    /**
     * 一分钟内，不要重复拉取广告
     * */
    private static final int AD_LOAD_INTERVAL_THRES = 60 * 1000;

    private static MobvistaEngine sInstance;
    
    private Map<String, MobvistaAdData> mMobvistaMap;
    private Map<String, MobvistaListener> mMobvistaListeners;
    private Map<String, MobvistaAdNative> mMobvistaNative;

    private Map<String, String> mUnitIdToPlacementIdMap;
    
    static {
        Context context = AppMasterApplication.getInstance();
        MobvistaAd.init(context, Constants.MOBVISTA_APPID,
                Constants.MOBVISTA_APPKEY);
        MobvistaEngine.getInstance(context).preloadMobvistaAds();
        LeoLog.i(TAG, "static block run done");
    }
    
    public static interface MobvistaListener {
        /**
         * 广告请求回调
         * @param code 返回码，如ERR_PARAMS_NULL
         * @param campaign 请求成功的广告结构体，失败为null
         * @param msg 请求失败sdk返回的描述，成功为null
         */
        public void onMobvistaFinished(int code, Campaign campaign, String msg);
        /**
         * 广告点击回调
         * @param campaign
         */
        public void onMobvistaClick(Campaign campaign);
    }
    
    public static synchronized MobvistaEngine getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new MobvistaEngine(ctx);
        }
        
        return sInstance;
    }
    
    private MobvistaEngine(Context ctx) {
        mAppContext = ctx;
        mMobvistaMap = new HashMap<String, MobvistaAdData>();
        mMobvistaListeners = new HashMap<String, MobvistaListener>();
        mMobvistaNative = new HashMap<String, MobvistaAdNative>();

        mUnitIdToPlacementIdMap = new HashMap<String, String>();
        mUnitIdToPlacementIdMap.put(Constants.UNIT_ID_58, Constants.PLACEMENT_ID_58);
        mUnitIdToPlacementIdMap.put(Constants.UNIT_ID_59, Constants.PLACEMENT_ID_59);
        mUnitIdToPlacementIdMap.put(Constants.UNIT_ID_60, Constants.PLACEMENT_ID_60);
        mUnitIdToPlacementIdMap.put(Constants.UNIT_ID_61, Constants.PLACEMENT_ID_61);
        mUnitIdToPlacementIdMap.put(Constants.UNIT_ID_62, Constants.PLACEMENT_ID_62);
        mUnitIdToPlacementIdMap.put(Constants.UNIT_ID_63, Constants.PLACEMENT_ID_63);
        mUnitIdToPlacementIdMap.put(Constants.UNIT_ID_67, Constants.PLACEMENT_ID_67);
        
        LeoLog.i(TAG, "MobvistaEngine() called done");
    }
    
    /**
     * 程序启动，获取所有广告位的数据作为缓存
     * @param context 
     * @param 
     * */
    private void preloadMobvistaAds(){
        LeoLog.i(TAG, "loadMobvistaAds() called done");
        for(String unitId: mUnitIdToPlacementIdMap.keySet()){
            loadSingleMobAd(unitId);
        }
    }
    
    private void loadSingleMobAd(String unitId){
        // 对应的ad正在loading，不重复load
        if(mMobvistaNative.get(unitId) != null){
            return;
        }
        String placementId = mUnitIdToPlacementIdMap.get(unitId);
        
        // check placement first
        if (TextUtils.isEmpty(placementId)) {
            LeoLog.i(TAG, "cannot find place mentid of this unitid.");
            MobvistaListener listener = mMobvistaListeners.remove(unitId);
            if(listener != null){
                listener.onMobvistaFinished(ERR_NOT_FOUND_PLACEMENTID, null, "Mobvista execute throwable.");
            }
            return;
        }
        
        MobvistaAdNative nativeAd = MobvistaAd.newNativeController(mAppContext, unitId, placementId);
        try {
            // 这个地方执行导致crash，直接catch住
            nativeAd.loadAd(new AdListenerImpl(unitId));
            LeoLog.i(TAG, "loadSingleMobAd -> ad["+unitId+"]");
            mMobvistaNative.put(unitId, nativeAd);
        } catch (Throwable thr) {
            MobvistaListener listener = mMobvistaListeners.get(unitId);
            if(listener != null){
                listener.onMobvistaFinished(ERR_MOBVISTA_FAIL, null, "Mobvista execute throwable.");
            }
            doReleaseInner(unitId);
            return;
        }
    }
    
    /**
     * 获取广告内容
     * @param activity
     * @param listener
     */
    public void loadMobvista(String unitId, MobvistaListener listener) {
        LeoLog.i(TAG, "Attach to Native Ad");
        if (listener == null) return;

        if (TextUtils.isEmpty(unitId)) {
            LeoLog.i(TAG, "unit id is null.");
            listener.onMobvistaFinished(ERR_UNITID_NULL, null, null);
            return;
        }
        
        // 记录下listener
        mMobvistaListeners.put(unitId, listener);

        // 广告过时则需要重新拉取
        MobvistaAdData mobvista = mMobvistaMap.get(unitId);
        if (isOutOfDate(mobvista)) {
            loadSingleMobAd(unitId);
            LeoLog.i(TAG, "data out ofdate: reload new one.");
            return;
        }

        boolean loading = mMobvistaNative.get(unitId) != null;
        if (loading){
            LeoLog.i(TAG, "MobvistaNative is loading");
            return;
        }
        
        MobvistaAdData adData = mMobvistaMap.get(unitId);
        if(adData!= null && adData.campaign!=null && adData.nativeAd!=null){
            listener.onMobvistaFinished(ERR_OK, adData.campaign, null);
        }
    }
    
    /**
     * @deprecated
     * 创建appwall广告接口
     * @param activity
     * @return
     */
    public MobvistaAdWall createAdWallController(Activity activity) {
        return createAdWallController(activity, null);
    }

    public MobvistaAdWall createAdWallController(Activity activity, String unitId) {
        if (TextUtils.isEmpty(unitId)) {
            LeoLog.i(TAG, "unit id is null.");
            return null;
        }
        String placementId = mUnitIdToPlacementIdMap.get(unitId);
        if (TextUtils.isEmpty(placementId)) {
            LeoLog.i(TAG, "cannot find place mentid of this unitid.");
            return null;
        }
        return MobvistaAd.newAdWallController(activity, unitId, placementId);
    }
    
    /**
     * 注册广告点击事件
     * @param view
     */
    public void registerView(String unitId, View view) {
        MobvistaAdData adObject = mMobvistaMap.get(unitId);
        if(adObject == null){
            return;
        }
        MobvistaAdNative adNative = adObject.nativeAd;
        if (adNative == null) {
            LeoLog.i(TAG, "havnt register activity before.");
            return;
        }
        LeoLog.i(TAG, "registerView");
        adNative.registerView(view, new AdTrackingListener() {

            @Override
            public void onStartRedirection(Campaign arg0, String arg1) {
                LeoLog.i(TAG, "-->onStartRedirection arg0: " + arg0 + " | string: " + arg1);
            }

            @Override
            public void onRedirectionFailed(Campaign arg0, String arg1) {
                LeoLog.i(TAG, "-->onRedirectionFailed arg0: " + arg0 + " | string: " + arg1);

            }

            @Override
            public void onFinishRedirection(Campaign arg0, String arg1) {
                LeoLog.i(TAG, "-->onFinishRedirection arg0: " + arg0 + " | string: " + arg1);
                AppMasterApplication context = AppMasterApplication.getInstance();
                AppMasterPreference preference = AppMasterPreference.getInstance(context);

                // 记录广告已经被点击过
                preference.setMobvistaClicked();
            }

            @Override
            public void onDownloadStart(Campaign arg0) {
                LeoLog.i(TAG, "-->onDownloadStart arg0: " + arg0);
            }

            @Override
            public void onDownloadProgress(Campaign arg0, int arg1) {
                LeoLog.i(TAG, "-->onDownloadProgress arg0: " + arg0 + " | progress: " + arg1);
            }

            @Override
            public void onDownloadFinish(Campaign arg0) {
                LeoLog.i(TAG, "-->onDownloadFinish arg0: " + arg0);
            }

            @Override
            public void onDownloadError(String arg0) {
                LeoLog.i(TAG, "-->onDownloadError arg0: " + arg0);
            }

            @Override
            public void onDismissLoading(Campaign arg0) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public boolean onInterceptDefaultLoadingDialog() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void onShowLoading(Campaign arg0) {
                // TODO Auto-generated method stub

            }
        });
    }
    
    /**
     * 释放广告资源
     */
    public void release(String unitId) {
        LeoLog.i(TAG, "release ["+unitId+"]");
        doReleaseInner(unitId);

        // 重新拉取广告
        if(shouldReloadAd(unitId)){
            LeoLog.d(TAG, "reload ad[" +unitId+ "] when release");
            removeMobAdData(unitId);
            loadSingleMobAd(unitId);
        }
    }
    
    private boolean shouldReloadAd(String unitId){
       MobvistaAdData adData = mMobvistaMap.get(unitId);

        if(adData == null) return true;
        if(System.currentTimeMillis()-adData.requestTimeMs > AD_LOAD_INTERVAL_THRES){
            return true;
        }
        
        return false;
    }

    private void doReleaseInner(String unitId) {
        // 因为目前loading与UI已无关系，release的时候无需清除loading的广告
//        MobvistaAdNative adNative = mMobvistaNative.remove(unitId);
//        if (adNative != null) {
//            try {
//                adNative.release();
//            } catch (Throwable e) {
//            }
//        }
        
        mMobvistaListeners.remove(unitId);
//        removeMobAdData(unitId);
    }
    
    private void removeMobAdData(String unitId){
        MobvistaAdData adData = mMobvistaMap.remove(unitId);
        if(adData != null){
            MobvistaAdNative nativeAd = adData.nativeAd;
            if(nativeAd != null){
                nativeAd.release();
            }
        }
    }

    private static boolean isOutOfDate(MobvistaAdData mobvista) {
        if (mobvista == null) return true;
        
        long current = System.currentTimeMillis();
        return current - mobvista.requestTimeMs > AD_TIMEOUT; 
    }

    private class AdListenerImpl implements AdListener {
        private String mUnitId;
        public AdListenerImpl(String unitId) {
            this.mUnitId = unitId;
        }

        @Override
        public void onAdLoaded(Campaign campaign) {
            LeoLog.i(TAG, "onAdLoaded ["+mUnitId+"]: " + campaign.getAppName());
            MobvistaAdData mobvista = new MobvistaAdData();
            // 将load成功的 MobvistaAdNative 对象移动到 MobvistaAdData 中
            mobvista.nativeAd = mMobvistaNative.remove(mUnitId);
            mobvista.campaign = campaign;
            mobvista.requestTimeMs = System.currentTimeMillis();
            mMobvistaMap.put(mUnitId, mobvista);
            
            MobvistaListener listener = mMobvistaListeners.get(mUnitId);

            if (listener != null) {
                listener.onMobvistaFinished((campaign==null)?ERR_MOBVISTA_RESULT_NULL:ERR_OK, campaign, null);
            }
        }

        @Override
        public void onAdLoadError(String s) {
            LeoLog.i(TAG, "onAdLoadError["+ mUnitId +"], msg: " + s);
            MobvistaListener listener = mMobvistaListeners.get(mUnitId);

            if (listener != null) {
                listener.onMobvistaFinished(ERR_MOBVISTA_FAIL, null, s);
            }
            
            mMobvistaNative.remove(mUnitId);
        }

        @Override
        public void onAdClick(Campaign campaign) {
            Campaign data = null;
            MobvistaAdData m = mMobvistaMap.get(mUnitId);
            if (m != null) {
                data = m.campaign;
            }

            // 响应之后，干掉listener
            MobvistaListener listener = mMobvistaListeners.get(mUnitId);
            if (listener != null) {
                listener.onMobvistaClick(campaign == null ? data : campaign);
            }
            // 点击之后，重新load此位置的广告
            LeoLog.i(TAG, "reload the clicked Ad");
            loadSingleMobAd(mUnitId);
        }
    }
    
    private static class MobvistaAdData {
        public Campaign campaign;
        public MobvistaAdNative nativeAd;
        public long requestTimeMs;
    }

}
