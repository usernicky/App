package com.leo.appmaster.home;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.R;
import com.leo.appmaster.appmanage.BackUpActivity;
import com.leo.appmaster.appmanage.EleActivity;
import com.leo.appmaster.appmanage.FlowActivity;
import com.leo.appmaster.appmanage.UninstallActivity;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.engine.AppLoadEngine;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.PrivacyEditFloatEvent;
import com.leo.appmaster.imagehide.ImageHideMainActivity;
import com.leo.appmaster.mgr.LockManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.privacycontact.PrivacyContactActivity;
import com.leo.appmaster.privacycontact.PrivacyContactUtils;
import com.leo.appmaster.quickgestures.ISwipUpdateRequestManager;
import com.leo.appmaster.quickgestures.IswipUpdateTipDialog;
import com.leo.appmaster.activity.QuickHelperActivity;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.HomeUpArrow;
import com.leo.appmaster.ui.SlidingUpPanelLayout;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;
import com.leo.appmaster.videohide.VideoHideMainActivity;

/**
 * 首页上拉列表，各个模块入口列表
 *
 * @author Jasper
 */
public class HomeMoreFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final String TAG = "HomeMoreFragment";

    private static final int DEFAULT_FADE_COLOR = 0x99000000;

    private static final String PG_TO_ISWIPE = "pg_to_iswipe";
    private static final String ISWIPE_FIRST_TIP = "iswipe_first_tip";
    private static final String ISWIPE_NO_FIRST_TIP = "iswipe_no_firt_tip";

    private SlidingUpPanelLayout mSlidingLayout;
    private View mRootView;
    private View mTouchDelegate;
    private HomeUpArrow mUpArrow;
    private View mUpClickView;

    private HomeMoreAdapter mAdapter;
    private IswipUpdateTipDialog mAppManagerIswipDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_more, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mUpClickView = view.findViewById(R.id.more_up_click_view);
        mUpClickView.setOnClickListener(this);
        mTouchDelegate = view.findViewById(R.id.home_more_empty);
        mTouchDelegate.setOnClickListener(this);
        mRootView = view.findViewById(R.id.home_more_root);
        final ListView lv = (ListView) view.findViewById(R.id.list);
        mAdapter = new HomeMoreAdapter();
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(this);

        mUpArrow = (HomeUpArrow) view.findViewById(R.id.more_up_arrow);
        mSlidingLayout = (SlidingUpPanelLayout) view.findViewById(R.id.sliding_layout);
        mSlidingLayout.setTabRectFunction(mUpArrow);
        mSlidingLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                Log.i(TAG, "onPanelSlide, offset " + slideOffset);
                int apha = Color.alpha(DEFAULT_FADE_COLOR);
                int r = Color.red(DEFAULT_FADE_COLOR);
                int g = Color.green(DEFAULT_FADE_COLOR);
                int b = Color.blue(DEFAULT_FADE_COLOR);
                apha *= slideOffset;
                int color = Color.argb(apha, r, g, b);
                mRootView.setBackgroundColor(color);

//                int alpha = Color.alpha(HomeUpArrow.FULL_COLOR);
//                alpha *= slideOffset;
                mUpArrow.setBgAlpha(255);
            }

            @Override
            public void onPanelExpanded(View panel) {
                // 打开
                Log.i(TAG, "onPanelExpanded");
                mTouchDelegate.setVisibility(View.VISIBLE);
                mUpArrow.reverse();
                mUpArrow.cancelUpAnimation();

                PreferenceTable preferenceTable = PreferenceTable.getInstance();
                boolean pulledEver = preferenceTable.getBoolean(PrefConst.KEY_MORE_PULLED, false);
                if (!pulledEver) {
                    preferenceTable.putBoolean(PrefConst.KEY_MORE_PULLED, true);
                }
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "home_listup");
                mSlidingLayout.setDragClickEnable(true);
                mUpClickView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPanelCollapsed(View panel) {
                // 关闭
                Log.i(TAG, "onPanelCollapsed");
                mRootView.setBackgroundColor(Color.TRANSPARENT);
                mTouchDelegate.setVisibility(View.INVISIBLE);

                mUpArrow.reset();
                mUpArrow.setBgAlpha(0);
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "home_listdown");
                mSlidingLayout.setDragClickEnable(false);
                mUpClickView.setVisibility(View.GONE);
            }

            @Override
            public void onPanelAnchored(View panel) {
                Log.i(TAG, "onPanelAnchored");
            }

            @Override
            public void onPanelHidden(View panel) {
                Log.i(TAG, "onPanelHidden");
            }
        });

    }

    public void setEnable(boolean enable) {
        if (enable && mRootView.getVisibility() == View.GONE) {
            mRootView.setVisibility(View.VISIBLE);
        } else {
            mRootView.setVisibility(View.GONE);
        }
    }

    public boolean isEnable() {
        return mRootView.getVisibility() == View.VISIBLE;
    }

    public void onEventMainThread(PrivacyEditFloatEvent event) {
        AppMasterPreference preference = AppMasterPreference.getInstance(getActivity());
        int msgCount = preference.getMessageNoReadCount();
        int callCount = preference.getCallLogNoReadCount();
        boolean pulledEver = PreferenceTable.getInstance().getBoolean(PrefConst.KEY_MORE_PULLED, false);
        LeoLog.i(TAG+1,"隐私通话："+callCount);
        if (msgCount > 0 || callCount > 0) {
            mAdapter.notifyDataSetInvalidated();
            mUpArrow.showRedTip(true);
        } else if (pulledEver) {
            mAdapter.notifyDataSetInvalidated();
            mUpArrow.showRedTip(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean pulledEver = PreferenceTable.getInstance().getBoolean(PrefConst.KEY_MORE_PULLED, false);

        AppMasterPreference preference = AppMasterPreference.getInstance(getActivity());
        int msgCount = preference.getMessageNoReadCount();
        int callCount = preference.getCallLogNoReadCount();
        LeoLog.i(TAG,"msgCount = "+msgCount+",callCount = "+callCount);
        if (msgCount > 0 || callCount > 0) {
            mUpArrow.showRedTip(true);
            mAdapter.notifyDataSetInvalidated();
        } else {
            mUpArrow.showRedTip(false);
            mAdapter.notifyDataSetInvalidated();
        }
        if (!pulledEver) {
            mUpArrow.startUpAnimation();
        } else {
            mUpArrow.cancelUpAnimation();
        }

        if (isPanelOpen()) {
            mUpArrow.setBgAlpha(255);
        } else {
            mUpArrow.setBgAlpha(0);
        }
        LeoEventBus.getDefaultBus().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        LeoEventBus.getDefaultBus().unregister(this);
    }

    public boolean isPanelOpen() {
        return mSlidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED;
    }

    public void closePanel() {
        mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.more_up_click_view:
            case R.id.home_more_empty:
                closePanel();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int itemId = (int) mAdapter.getItemId(position);

        Intent intent = null;
        switch (itemId) {
            case R.string.hp_hide_img:
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "hidpic");
                intent = new Intent(getActivity(), ImageHideMainActivity.class);
                getActivity().startActivity(intent);
                // 隐藏图片
                break;
            case R.string.hp_hide_video:
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "hidvideo");
                intent = new Intent(getActivity(), VideoHideMainActivity.class);
                getActivity().startActivity(intent);
                // 隐藏视频
                break;
            case R.string.hp_contact_call:
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "pricall");
                intent = new Intent(getActivity(), PrivacyContactActivity.class);
                intent.putExtra(PrivacyContactUtils.TO_PRIVACY_CONTACT,
                        PrivacyContactUtils.TO_PRIVACY_CALL_FLAG);
                getActivity().startActivity(intent);
                // 隐私通话
                break;
            case R.string.hp_contact_sms:
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "primesg");
                intent = new Intent(getActivity(), PrivacyContactActivity.class);
                intent.putExtra(PrivacyContactUtils.TO_PRIVACY_CONTACT,
                        PrivacyContactUtils.TO_PRIVACY_MESSAGE_FLAG);
                LeoLog.i(TAG, "scan uri: " + intent.toURI());
                getActivity().startActivity(intent);
                // 隐私短信
                break;
            case R.string.hp_app_manage_del:
                // 应用卸载
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "newuninstall");
                intent = new Intent(getActivity(), UninstallActivity.class);
                startActivity(intent);
                break;
            case R.string.hp_app_manage_back:
                // 应用备份
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "backup");
                intent = new Intent(getActivity(), BackUpActivity.class);
                startActivity(intent);
                break;
            case R.string.hp_device_gprs:
                // 流量监控
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "home", "data");
                intent = new Intent(getActivity(), FlowActivity.class);
                startActivity(intent);
                break;
            case R.string.hp_device_power:
                // 电量管理
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "boost", "battery");
                Intent dlIntent = new Intent(getActivity(), EleActivity.class);
                startActivity(dlIntent);
                break;
            case R.string.hp_helper_shot:
                // 快捷小助手
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "boost", "home_shortcutsAssistant");
                Intent qhintent = new Intent(getActivity(), QuickHelperActivity.class);
                startActivity(qhintent);
                break;
            case R.string.hp_helper_iswipe:
                // iswipe
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "boost", "home_swifty");
                boolean installISwipe = ISwipUpdateRequestManager.isInstallIsiwpe(getActivity());
                // Log.e(Constants.RUN_TAG, "是否安装ISwipe：" + installISwipe);
                startISwipHandlerForInstallIS(installISwipe);
                break;
        }
    }

    private void startISwipHandlerForInstallIS(boolean flag) {
        if (!flag) {
            /* 下载ISwip对话框 */
            showDownLoadISwipDialog(getActivity());
            // ISwipUpdateRequestManager.getInstance(getActivity()).iSwipDownLoadHandler();

        } else {
            /* 启动ISwipe主页 */
            startISwipIntent();
        }
    }

    private void showDownLoadISwipDialog(Context context) {
        LeoLog.i("HomeAppManagerFragment", "HomeAppManagerFragment中的Dialog");
        if (mAppManagerIswipDialog == null) {
            mAppManagerIswipDialog = new IswipUpdateTipDialog(context);
        }
        mAppManagerIswipDialog.setVisiblilyTitle(false);
        String contentButtonText = context.getResources().getString(
                R.string.first_open_quick_gesture_dialog_tip_cotent);
        mAppManagerIswipDialog.setContentText(contentButtonText);
        String leftButtonText = context.getResources().getString(
                R.string.quick_first_tip_dialog_left_bt);
        mAppManagerIswipDialog.setLeftButtonText(leftButtonText);
        String rightButtonText = context.getResources().getString(
                R.string.quick_first_tip_dialog_right_bt);
        mAppManagerIswipDialog.setRightButtonText(rightButtonText);
        mAppManagerIswipDialog.setLeftListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                /* 稍后再说 */
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "qs_iSwipe", "new_dia_n");
                if (mAppManagerIswipDialog != null) {
                    mAppManagerIswipDialog.dismiss();
                }
            }
        });
        mAppManagerIswipDialog.setRightListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                /* 立即下载 */
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "qs_iSwipe", "new_dia_y");
                ISwipUpdateRequestManager.getInstance(getActivity()).iSwipDownLoadHandler();
                if (mAppManagerIswipDialog != null) {
                    mAppManagerIswipDialog.dismiss();
                }
            }
        });
        mAppManagerIswipDialog.show();
    }

    private void startISwipIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        ComponentName cn = new ComponentName(AppLoadEngine.ISWIPE_PACKAGENAME,
                "com.leo.iswipe.activity.QuickGestureActivity");
        intent.setComponent(cn);
        boolean iswipeFirstTip = AppMasterPreference.getInstance(getActivity())
                .getFristSlidingTip();
        if (iswipeFirstTip) {
            intent.putExtra(PG_TO_ISWIPE, ISWIPE_FIRST_TIP);
        } else {
            intent.putExtra(PG_TO_ISWIPE, ISWIPE_NO_FIRST_TIP);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        LockManager lockManager = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);
        try {
            lockManager.filterSelfOneMinites();
            lockManager.filterPackage(AppLoadEngine.ISWIPE_PACKAGENAME, false);
            startActivity(intent);
        } catch (Exception e) {
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUpArrow.release();
    }
}
