package com.leo.appmaster.ad;

import android.content.Context;
import android.view.View;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.applocker.manager.ADShowTypeRequestManager;
import com.leo.appmaster.applocker.manager.MobvistaEngine;
import com.leo.appmaster.db.PrefTableHelper;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.utils.LeoLog;
import com.mobvista.msdk.out.Campaign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by stone on 16/2/26.
 */
public class ADEngineWrapper {

    private static final String TAG = "ADEngineWrapper [AD_DEBUG]";
	
	public static final int AD_TYPE_NATIVE = 0;
	public static final int AD_TYPE_TEMPLATE = 1;
	
    /* 两个可选的广告来源 */
    public static final int SOURCE_MOB = AppMasterPreference.AD_SDK_SOURCE_USE_3TH;
    public static final int SOURCE_MAX = AppMasterPreference.AD_SDK_SOURCE_USE_MAX;

    private LEOAdEngine mMaxEngine;
    private MobvistaEngine mMobEngine;

    private Map<String, Iterator<Integer>> mRandomMap = new HashMap<String, Iterator<Integer>>();

	


	public static interface WrappedTemplateAdListener {
		/**
		 * 模板广告请求成功
		 * @param code 返回码，如ERR_PARAMS_NULL
		 * @param campaigns 请求成功的广告结构体集合，失败为null
		 * @param msg 请求失败sdk返回的描述，成功为null
		 */
		public void onWrappedAdLoadFinished(int code, List<Campaign> campaigns, String msg);

		/**
		 * 模板广告被点击
		 * @param campaign
		 * @param unitID
		 */
		public void onWrappedAdClick(Campaign campaign, String unitID);

		/**
		 * 请求模板广告失败
		 * @param message
		 */
		public void onAdLoadError(String message);
	}

    public static interface WrappedAdListener {
        /**
         * 广告请求回调
         * @param code 返回码，如ERR_PARAMS_NULL
         * @param campaigns 请求成功的广告结构体，失败为null
         * @param msg 请求失败sdk返回的描述，成功为null
         */
        public void onWrappedAdLoadFinished(int code, List<WrappedCampaign> campaigns, String msg);
        /**
         * 广告点击回调
         * @param campaign
         */
        public void onWrappedAdClick(WrappedCampaign campaign, String unitID);
    }

    private static ADEngineWrapper sInstance;
    private ADEngineWrapper (Context context) {
        mMaxEngine = LEOAdEngine.getInstance(context);
        mMobEngine = MobvistaEngine.getInstance(context);
    }

    public static ADEngineWrapper getInstance (Context context) {
        if (sInstance == null) {
            sInstance = new ADEngineWrapper(context);
        }
        return sInstance;
    }

    /**
     * 批量加载广告，目前仅仅用于Lock页三个大图, 一次概率控制三个广告展示
     * @param sources
     * @param unitIds
     * @param listeners
     * @param forceLoad 不管概率如何，强制加载
     */
    public void loadAdBatch(int[] sources, String[] unitIds, WrappedAdListener[] listeners, boolean forceLoad) {
        if (sources == null || sources.length <= 0 || unitIds == null || unitIds.length <=0 || sources.length != unitIds.length) {
            return;
        }
        if (!forceLoad && !isHitProbability(unitIds[0])) {
            // 未命中显示概率，不显示广告
            if (listeners != null) {
                for (int i = 0; i < listeners.length; i++) {
                    WrappedAdListener listener = listeners[i];
                    if (listener == null) {
                        continue;
                    }
                    listener.onWrappedAdLoadFinished(LEOAdEngine.ERR_MOBVISTA_RESULT_NULL, null, "probability is not hit.");
                }
            }
            return;
        }
        for (int i = 0; i < sources.length; i++) {
            WrappedAdListener listener = null;
            try {
                listener = listeners[i];
            } catch (Exception e) {
                // 可以不用传listener进来，catch住所有异常忽略掉
            }
            loadAdForce(sources[i], unitIds[i], AD_TYPE_NATIVE,  listener);
        }
    }
	
	
	private void loadMobTemplate(final String unitId, final WrappedAdListener listener) {
		if (unitId!= null && listener != null) {
			mMobEngine.loadMobvistaTemplate(unitId, new MobvistaEngine.MobvistaListener() {
				@Override
				public void onMobvistaFinished(int code, List<Campaign> campaigns, String msg) {
					if (listener != null) {

						if (code == LEOAdEngine.ERR_OK) {
							List<WrappedCampaign> wrappedCampaignList = WrappedCampaign.fromMabVistaSDK(campaigns);
							listener.onWrappedAdLoadFinished(code, wrappedCampaignList, msg);
						} else {

							listener.onWrappedAdLoadFinished(code, null, msg);
						}
					}
				}

				@Override
				public void onMobvistaClick(Campaign campaign, String unitID) {
					if (listener != null) {
						WrappedCampaign wrappedCampaign = WrappedCampaign.converCampaignFromMob(campaign);
						listener.onWrappedAdClick(wrappedCampaign, unitID);
					}
				}
			});
		}
	}

    /***
     * 请求广告数据
     */
    public void loadAd (final int source, final String unitId, final int adType, final WrappedAdListener listener) {
        if (!isHitProbability(unitId)) {
            // 未命中显示概率，不显示广告
            if (listener != null) {
                listener.onWrappedAdLoadFinished(LEOAdEngine.ERR_MOBVISTA_RESULT_NULL, null, "probability is not hit.");
            }
            return;
        }

		if (source == AppMasterPreference.AD_SDK_SOURCE_USE_3TH && adType == AD_TYPE_TEMPLATE) {
			if (listener == null) {
				listener.onWrappedAdLoadFinished(LEOAdEngine.ERR_MOBVISTA_RESULT_NULL, null, "call back is null.");
				return;
			}

			loadMobTemplate(unitId, listener);
			return;
		}

		loadAdForce(source, unitId, adType, listener);
    }

    private void loadAdForce(final int source, final String unitId, final int adType,  final WrappedAdListener listener) {
		LeoLog.e(TAG, "AD TYPE :" + source + " AD ID: " + unitId);
		
		String sdk = (source == 2) ? "Max" : "Mobvista";
		final TreeMap<String, String> map = new TreeMap<String, String>();
		map.put("engineType", sdk);
		map.put("unitId", unitId);
		SDKWrapper.addEvent(AppMasterApplication.getInstance(), "max_ad", SDKWrapper.P1, "start_to_loadad", sdk, source, map);
        
		if (source == SOURCE_MAX) {
            mMaxEngine.loadMobvista(unitId, new LEOAdEngine.LeoListener() {
                @Override
                public void onLeoAdLoadFinished(int code, LEONativeAdData campaign, String msg) {
                    LeoLog.d(TAG, "[" + unitId + "] source = " + source + "; code = " + code);
                    List<WrappedCampaign> wrappedCampaignList = null;
                    if (code == LEOAdEngine.ERR_OK) {
						wrappedCampaignList = WrappedCampaign.fromMaxSDK(campaign);

						if (wrappedCampaignList != null) {
							SDKWrapper.addEvent(AppMasterApplication.getInstance(), "max_ad", SDKWrapper.P1, "ad_onLoadFinished", "ad pos: " + unitId + " state is suc and campaign is ready", source, null);
						} else {
							SDKWrapper.addEvent(AppMasterApplication.getInstance(), "max_ad", SDKWrapper.P1, "ad_onLoadFinished", "ad pos: " + unitId + " state is suc and campaign is null", source, null);
						}
                    } else {

						SDKWrapper.addEvent(AppMasterApplication.getInstance(), "max_ad", SDKWrapper.P1, "ad_onLoadFinished", "ad pos: " + unitId + " state is failed code: " + code, source, null);
					}
                    listener.onWrappedAdLoadFinished(code, wrappedCampaignList, msg);
                }

                @Override
                public void onLeoAdClick(LEONativeAdData campaign, String unitID) {
                    listener.onWrappedAdClick(WrappedCampaign.converCampaignFromMax(campaign), unitID);
                }
            });
        } else {
			
			mMobEngine.loadMobvista(unitId, adType, new MobvistaEngine.MobvistaListener() {
				@Override
				public void onMobvistaFinished(int code, List<Campaign> campaigns, String msg) {
					LeoLog.d(TAG, "[" + unitId + "] source = " + source + "; code = " + code);
					List<WrappedCampaign> wrappedCampaignList = null;
					if (code == MobvistaEngine.ERR_OK) {
						wrappedCampaignList = WrappedCampaign.fromMabVistaSDK(campaigns);
					}
					listener.onWrappedAdLoadFinished(code, wrappedCampaignList, msg);
				}

				@Override
				public void onMobvistaClick(com.mobvista.msdk.out.Campaign campaign, String unitID) {
					
					listener.onWrappedAdClick(WrappedCampaign.converCampaignFromMob(campaign), unitID);
				}
			});

        }
    }

    /**
     * 此次随机是否命中展示概率
     * @return
     */
    private boolean isHitProbability(String unitId) {
        int local = nextRandomInt(unitId);
        int server = PrefTableHelper.getAdProbability();

        boolean hit = local < server;
        LeoLog.d(TAG, "[" + unitId + "] probability hit: " + hit + "; localRandom = " + local + "; server = " + server);
        return hit;
    }

    /**
     * 精准的随机，10次，0 ~ 9 每个数字都会被随机到
     * @param unitId
     * @return
     */
    private int nextRandomInt(String unitId) {
        synchronized (mRandomMap) {
            int max = ADShowTypeRequestManager.AD_PROBABILITY_MAX;
            Iterator<Integer> randomList = mRandomMap.get(unitId);
            if (randomList == null || !randomList.hasNext()) {
                List<Integer> list = new ArrayList<Integer>(max);
                for (int i = 0; i < max; i++) {
                    list.add(i);
                }
                // 随机打乱
                Collections.shuffle(list);
                StringBuilder stringBuilder = new StringBuilder();
                for (Integer integer : list) {
                    stringBuilder.append(integer).append(",");
                }
                LeoLog.d(TAG, "gen random list: " + stringBuilder.toString());
                randomList = list.iterator();
                mRandomMap.put(unitId, randomList);
            }

            int result = randomList.next();
            randomList.remove();
            return result;
        }
    }
	
	public void registerTemplateView(View view, int index,  String unitId) {
		mMobEngine.registerTemplateView(view, index, unitId);
	}

    public void registerView (int source, View view, String unitId) {
        LeoLog.d(TAG, "registerView called");
        if (source == SOURCE_MAX) {
            mMaxEngine.registerView(unitId, view);
        } else {
            mMobEngine.registerView(unitId, view);
        }
    }

    public void releaseAd (int source, String unitId, View view) {
        if (source == SOURCE_MAX) {
            mMaxEngine.release(unitId);
        } else {
            mMobEngine.release(unitId, view);
        }
    }
	
	public void removeMobAdData(int source, String unitId, View view) {
		if (source == SOURCE_MAX) {
			mMaxEngine.removeMobAdData(unitId);
		} else {
			mMobEngine.removeMobAdData(unitId, view);
		}
	}
	
	public boolean isADCacheEmpty(int source) {
		if (source == SOURCE_MAX) {
			return mMaxEngine.isADCacheEmpty();
		} else {
			return mMobEngine.isADCacheEmpty();
		}
	}
	public void proloadTemplate(String unitId) {
		mMobEngine.proloadTemplate(unitId);
	}
	
	public void releaseTemplateAd(String unitId) {
		mMobEngine.releaseTemplate(unitId);
	}
}
