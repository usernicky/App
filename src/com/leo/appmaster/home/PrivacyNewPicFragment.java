package com.leo.appmaster.home;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.SecurityNotifyChangeEvent;
import com.leo.appmaster.imagehide.PhotoItem;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.PrivacyDataManager;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.HeaderGridView;
import com.leo.appmaster.utils.DataUtils;
import com.leo.appmaster.utils.PrefConst;

/**
 * Created by Jasper on 2015/10/16.
 */
public class PrivacyNewPicFragment extends PrivacyNewFragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "PrivacyNewPicFragment";

    private HeaderGridView mPicList;
    private PrivacyDataManager mLockMgr;

    private List<PhotoItem> mDataList = new ArrayList<PhotoItem>();;

    private boolean mHidingTimeout;
    private boolean mHidingFinish;


    public static Fragment getFragment(List<PhotoItem> list) {
        Fragment fragment = null;
        if (list.size() > 60) {
            if (DataUtils.differentDirPic(list)) {
                fragment = PrivacyPicFolderFragment.newInstance();
            } else {
                fragment = PrivacyNewPicFragment.newInstance();
            }
        } else {
            fragment = PrivacyNewPicFragment.newInstance();
        }
        if (fragment instanceof PrivacyPicFolderFragment) {
            ((PrivacyPicFolderFragment) fragment).setData(list);
        } else if (fragment instanceof PrivacyNewPicFragment) {
            ((PrivacyNewPicFragment) fragment).setData(list);
        }

        return fragment;
    }

    public static PrivacyNewPicFragment newInstance() {
        PrivacyNewPicFragment fragment = new PrivacyNewPicFragment();
        return fragment;
    }

    @Override
    public void setData(List<? extends Object> list) {
        if (list == null) return;

        for (Object o : list) {
            mDataList.add((PhotoItem) o);
        }

        if (mAdaper != null) {
            mAdaper.setList(list);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockMgr = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);

        mAdaper = new PrivacyNewPicAdapter();
        mAdaper.setList(mDataList);

        LeoEventBus.getDefaultBus().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LeoEventBus.getDefaultBus().unregister(this);
    }

    @Override
    protected int getIgnoreStringId() {
        return R.string.pri_pro_ignore_dialog;
    }

    @Override
    protected String getSkipConfirmDesc() {
        return "pic_skip_confirm";
    }

    @Override
    protected String getSkipCancelDesc() {
        return "pic_skip_cancel";
    }

    @Override
    protected String getFolderFullDesc() {
        return "pic_full_cnts";
    }

    public void onEventMainThread(SecurityNotifyChangeEvent event) {
        if (!MgrContext.MGR_PRIVACY_DATA.equals(event.mgr)) return;

        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
                List<PhotoItem> list = pdm.getAddPic();
                if (list == null) return;

                if (list.size() != mDataList.size()) {
                    mDataList.clear();
                    mDataList.addAll(list);
                    mAdaper.setList(list);
                    ThreadManager.getUiThreadHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            setLabelCount(mDataList.size());
                        }
                    });
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_pic, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPicList = (HeaderGridView) view.findViewById(R.id.pic_gv);
        mPicList.setOnScrollListener(this);

        mPicList.addHeaderView(getEmptyHeader());
        mPicList.setAdapter(mAdaper);
        mPicList.setOnItemClickListener(this);

        setLabelCount(mDataList.size());
        setProcessContent(R.string.pri_pro_hide_pic);


    }

    private void setLabelCount(int count) {
        if (isDetached() || isRemoving() || getActivity() == null) return;

        boolean processed = PreferenceTable.getInstance().getBoolean(PrefConst.KEY_SCANNED_PIC, false);
        int stringId = R.string.pri_pro_new_pic;
        if (!processed) {
            stringId = R.string.pri_pro_scan_pic;
        }
        String content = AppMasterApplication.getInstance().getString(stringId, count);
        mNewLabelTv.setText(Html.fromHtml(content));
    }

    @Override
    protected void onProcessClick() {
        SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "process", "pic_hide_cnts");
        mActivity.onProcessClick(this);
        PreferenceTable.getInstance().putBoolean(PrefConst.KEY_SCANNED_PIC, true);
        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                List<PhotoItem> list = mAdaper.getSelectedList();

                PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);

                List<String> photos = new ArrayList<String>(list.size());
                for (PhotoItem photoItem : list) {
                    photos.add(photoItem.getPath());
                }
//                int incScore = pdm.onHideAllPic(photos);
//                pdm.haveCheckedPic();
//                onProcessFinish(incScore);
                final int incScore = pdm.haveCheckedPic();
                hideAllPicBackground(photos, incScore);
                ThreadManager.getUiThreadHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mHidingTimeout = true;
                        if (!mHidingFinish) {
                            onProcessFinish(incScore);
                        }
                    }
                }, 8000);
            }
        });
    }

    private void hideAllPicBackground(final List<String> photoItems, final int incScore) {
        mHidingTimeout = false;
        mHidingFinish = false;
        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
                pdm.onHideAllPic(photoItems);
                mHidingFinish = true;
                if (!mHidingTimeout) {
                    onProcessFinish(incScore);
                }
            }
        });
    }

    @Override
    protected void onIgnoreClick() {
//        PreferenceTable.getInstance().putBoolean(PrefConst.KEY_SCANNED_PIC, true);
//        PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
//        int incScore = pdm.haveCheckedPic();
//        mActivity.jumpToNextFragment(incScore, MgrContext.MGR_PRIVACY_DATA);
        mActivity.onIgnoreClick(0, MgrContext.MGR_PRIVACY_DATA);
        SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "process", "pic_skip_cnts");
    }

    private void onProcessFinish(final int incScore) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.onProcessFinish(incScore, MgrContext.MGR_PRIVACY_DATA);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PhotoItem item = (PhotoItem) mAdaper.getItem(position);
        mAdaper.toggle(item);

    }
}
