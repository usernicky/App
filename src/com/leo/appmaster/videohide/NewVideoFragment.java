package com.leo.appmaster.videohide;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.db.LeoPreference;
import com.leo.appmaster.home.FolderVidFragment;
import com.leo.appmaster.imagehide.NewFragment;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.PrivacyDataManager;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.HeaderGridView;
import com.leo.appmaster.ui.XHeaderView;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOCircleProgressDialog;
import com.leo.appmaster.utils.DataUtils;
import com.leo.appmaster.utils.PrefConst;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jasper on 2015/10/16.
 */
public class NewVideoFragment extends NewFragment implements AdapterView.OnItemClickListener {
    private static final int FOLDER_VIDEO_COUNT = 35;

    private HeaderGridView mVideoList;

    private TextView mNewImageNum;
    private LEOAlarmDialog mDialog;
    private LEOCircleProgressDialog mProgressDialog;

    public static Fragment getFragment(List<VideoItemBean> list) {
        Fragment fragment = null;
        if (list.size() < FOLDER_VIDEO_COUNT) {
            fragment = NewVideoFragment.newInstance();
            ((NewVideoFragment) fragment).setData(list, "");
        } else {
            if (DataUtils.differentDirVid(list)) {
                fragment = FolderVidFragment.newInstance();
                ((FolderVidFragment) fragment).setData(list);
            } else {
                fragment = NewVideoFragment.newInstance();
                ((NewVideoFragment) fragment).setData(list, "");
            }
        }
        return fragment;
    }

    public static Fragment getNewVidFragment(List<VideoItemBean> list) {
        Fragment fragment = null;
        if (list.size() < FOLDER_VIDEO_COUNT) {
            fragment = NewVideoFragment.newInstance();
            ((NewVideoFragment) fragment).setData(list, "");
        } else {
            if (DataUtils.differentDirVid(list)) {
                fragment = FolderNewVideoFragment.newInstance();
                ((FolderNewVideoFragment) fragment).setData(list);
            } else {
                fragment = NewVideoFragment.newInstance();
                ((NewVideoFragment) fragment).setData(list, "");
            }
        }
        return fragment;
    }

    public static NewVideoFragment newInstance() {
        NewVideoFragment fragment = new NewVideoFragment();
        return fragment;
    }

    @Override
    public void setData(List<? extends Object> list, String text) {
        if (list == null) return;
        mDataList = new ArrayList<VideoItemBean>();
        for (Object o : list) {
            mDataList.add((VideoItemBean) o);
        }

        if (mAdapter != null) {
            mAdapter.setList(list);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new NewVidAdapter();
        mAdapter.setList(mDataList);

    }

    @Override
    protected void onSelectAllClick() {
        SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "hide_Video", "vid_hide_all");
    }


    @Override
    protected void onProcessClick() {
        showAlarmDialog();
    }

    @Override
    public void onDestroy() {
        PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
        pdm.haveCheckedVid();
        super.onDestroy();
    }

    private void hideAllVidBackground(final List<String> photoItems, final int incScore) {
        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
                pdm.onHideAllVid(photoItems);
                for (int i = 0; i < photoItems.size(); i++) {
                    for (int j = 0; j < mDataList.size(); j++) {
                        if (((VideoItemBean) mDataList.get(j)).getPath().equals(photoItems.get(i))) {
                            mDataList.remove(j);
                        }
                    }
                }
                onProcessFinish(incScore);
            }
        });
    }

    private void onProcessFinish(final int incScore) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    if (mDataList.size() > 0) {
                        mAdapter.setList(mDataList);
                        setLabelCount();
                        mAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(mActivity, R.string.hide_complete_new_vid, Toast.LENGTH_LONG).show();
                        mActivity.finish();
                    }
                }
            }
        });
    }

    private void showAlarmDialog() {
        if (mDialog == null) {
            mDialog = new LEOAlarmDialog(mActivity);
        }
        mDialog.setOnClickListener(new LEOAlarmDialog.OnDiaogClickListener() {
            @Override
            public void onClick(int which) {
                if (which == 1) {
                    final List<VideoItemBean> list = mAdapter.getSelectedList();

                    showProgressDialog(getString(R.string.tips),
                            getString(R.string.app_hide_image) + "...",
                            true, true);
                    SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "process", "pic_hide_cnts");
                    SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "handled", "pic_prc_cnts_$"
                            + mAdapter.getSelectedList().size());
                    LeoPreference.getInstance().putBoolean(PrefConst.KEY_SCANNED_PIC, true);
                    ThreadManager.executeOnAsyncThread(new Runnable() {
                        @Override
                        public void run() {
                            List<String> photos = new ArrayList<String>(list.size());
                            for (VideoItemBean videoItemBean : list) {
                                photos.add(videoItemBean.getPath());
                            }
                            hideAllVidBackground(photos, 0);
                        }
                    });

                    SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "hide_Video", "vid_hide_cnts");
                    SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "hide_vid_operation", "vid_add_cnts");
                    SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "hide_vid_operation", "vid_add_cnts_$" + list.size());
                    SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "hide_vid_operation", "vid_new_$" + list.size());
                }
            }
        });
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setTitle(R.string.app_hide_image);
        mDialog.setContent(getString(R.string.app_hide_video_dialog_content));
        mDialog.show();
    }

    private void showProgressDialog(String title, String message,
                                    boolean indeterminate, boolean cancelable) {
        if (mProgressDialog == null) {
            mProgressDialog = new LEOCircleProgressDialog(mActivity);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mSelectBtn.setText(R.string.app_select_all);
                    mSelectBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.select_all_selector), null,
                            null);
                }
            });
        }
        mProgressDialog.setCancelable(cancelable);
        mProgressDialog.setButtonVisiable(cancelable);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setIndeterminate(indeterminate);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_vid_hide, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mVideoList = (HeaderGridView) view.findViewById(R.id.video_lv);
        mVideoList.setOnScrollListener(this);
        mVideoList.setOnItemClickListener(this);
        mAppName = "";

        setProcessContent(R.string.pri_pro_hide_vid);
        XHeaderView headerView = (XHeaderView) mStickView;
        headerView.setOnHeaderLayoutListener(new XHeaderView.OnHeaderLayoutListener() {
            @Override
            public void onHeaderLayout(int height) {
                if (mVideoList.getAdapter() == null) {
                    mStickyHeight = height;
                    mVideoList.addHeaderView(getEmptyHeader());
                    mVideoList.setAdapter(mAdapter);
                    if (mAdapter != null) {
                        mAdapter.setList(mDataList);
                    }
                }
            }
        });
    }

    protected View getEmptyHeader() {
        final View view = mActivity.getLayoutInflater().inflate(R.layout.image_folder_header_view, null);
        mNewImageNum = (TextView) view.findViewById(R.id.tv_image_hide_header);
        setLabelCount();
        return view;
    }

    private void setLabelCount() {
        String content = AppMasterApplication.getInstance().getString(R.string.new_vid_num, mDataList == null ? 0 : mDataList.size());
        mNewImageNum.setText(content);
    }

    @Override
    public void onSelectionChange(boolean selectAll, int selectedCount) {
        super.onSelectionChange(selectAll, selectedCount);
        if (selectedCount > 0) {
            mHideBtn.setEnabled(true);
        } else {
            mHideBtn.setEnabled(false);
        }

        List selectList = mAdapter.getSelectedList();
        if (selectList != null && selectList.size() < mDataList.size()) {
            mSelectBtn.setText(R.string.app_select_all);
            mSelectBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    getResources().getDrawable(R.drawable.select_all_selector), null,
                    null);
        } else {
            mSelectBtn.setText(R.string.app_select_none);
            mSelectBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    getResources().getDrawable(R.drawable.no_select_all_selector), null,
                    null);
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mAdapter.toggle(position);
    }
}