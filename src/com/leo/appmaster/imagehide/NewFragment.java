package com.leo.appmaster.imagehide;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.leo.appmaster.R;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.MaterialRippleLayout;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.utils.LeoLog;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by Jasper on 2015/10/16.
 */
public abstract class NewFragment extends Fragment implements AbsListView.OnScrollListener,
        CompoundButton.OnCheckedChangeListener, View.OnClickListener, NewAdaper.SelectionChangeListener {

    private static final String TAG = NewFragment.class.getSimpleName();


    private Dictionary<Integer, Integer> listViewItemHeights = new Hashtable<Integer, Integer>();
    protected Activity mActivity;
    protected NewAdaper mAdapter;

    protected Button mHideBtn;
    protected Button mSelectBtn;

    private TextView mProcessTv;
    private MaterialRippleLayout mProcessBtn;
    private View mProcessClick;
    private MaterialRippleLayout mIgnoreBtn;
    private View mIgnoreClick;

    public View mStickView;
//    private CheckBox mStickyCb;
//    protected TextView mNewLabelTv;
//    protected TextView mNewLabelContent;

    private View mEmptyBg;

    private int mToolbarHeight;
    protected int mEmptyHeight;
    protected int mStickyHeight;

    private LEOAlarmDialog mIgnoreDlg;

    protected List mDataList;

    protected String mAppName;

    public void setData(List<? extends Object> list, String text) {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;

        mToolbarHeight = activity.getResources().getDimensionPixelSize(R.dimen.toolbar_height);
        mEmptyHeight = activity.getResources().getDimensionPixelSize(R.dimen.pri_pro_header);
    }

    protected void hideDone(){
        mHideBtn.setText(getString(R.string.app_hide_image));
        mHideBtn.setEnabled(false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mProcessBtn = (MaterialRippleLayout) view.findViewById(R.id.pp_process_rv);
        mProcessBtn.setRippleOverlay(true);
        mProcessClick = view.findViewById(R.id.pp_process_rv_click);

        mIgnoreBtn = (MaterialRippleLayout) view.findViewById(R.id.pp_process_ignore_rv);
        mIgnoreBtn.setRippleOverlay(true);
        mIgnoreClick = view.findViewById(R.id.pp_process_ignore_rv_click);
        mProcessTv = (TextView) view.findViewById(R.id.pp_process_tv);
        mHideBtn = (Button) view.findViewById(R.id.hide_image);
        mSelectBtn = (Button) view.findViewById(R.id.select_all);

        mProcessClick.setOnClickListener(this);
        mIgnoreClick.setOnClickListener(this);

        mStickView = view.findViewById(R.id.pri_pro_sticky_header);
//        mStickyCb = (CheckBox) view.findViewById(R.id.pri_pro_cb);
//        mNewLabelTv = (TextView) view.findViewById(R.id.pri_pro_new_label_tv);
//        mNewLabelContent = (TextView) view.findViewById(R.id.app_name);
//        mStickyCb.setOnClickListener(this);
        mHideBtn.setOnClickListener(this);
        mSelectBtn.setOnClickListener(this);
        mAdapter.setOnSelectionChangeListener(this);

        mEmptyBg = view.findViewById(R.id.empty_bg);

        mProcessBtn.setEnabled(false);
        mProcessClick.setEnabled(false);
        if (mAdapter != null) {
            mAdapter.setOnSelectionChangeListener(this);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        View c = view.getChildAt(0); //this is the first visible row
        if (c == null) return;

        int scrollY = -c.getTop();
        listViewItemHeights.put(view.getFirstVisiblePosition(), c.getHeight());
        for (int i = 0; i < view.getFirstVisiblePosition(); ++i) {
            if (listViewItemHeights.get(i) != null) // (this is a sanity check)
                scrollY += listViewItemHeights.get(i); //add all heights of the views that are gone
        }

        int stickyMaxScrollHeight = mEmptyHeight - mToolbarHeight;
        if (scrollY > stickyMaxScrollHeight) {
            mStickView.setTranslationY(-stickyMaxScrollHeight);
            mEmptyBg.setTranslationY(-stickyMaxScrollHeight);
        } else {
            mStickView.setTranslationY(-scrollY);
            mEmptyBg.setTranslationY(-scrollY);
        }
//        mActivity.onListScroll(scrollY);
    }

    protected void onProcessClick() {

    }

//    protected void onIgnoreClick(boolean direct) {
//        mActivity.onIgnoreClick(0, null);
//    }

    protected void setProcessContent(int stringId) {
        mProcessTv.setText(stringId);
    }

    protected void setProcessContent(String string) {
        mProcessTv.setText(string);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            mAdapter.selectAll();
        } else {
            mAdapter.deselectAll();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pp_process_rv_click:
                onProcessClick();
                break;
            case R.id.pp_process_ignore_rv_click:
                mIgnoreClick.setEnabled(false);
                mIgnoreClick.setClickable(false);
                break;
            case R.id.hide_image:
                List list = mAdapter.getSelectedList();
                LeoLog.v(TAG, "隐藏数量：" + (list != null ? list.size() : 0));
                onProcessClick();
                break;
            case R.id.select_all:
                if (mAdapter.getSelectedList() != null && mAdapter.getSelectedList().size() < mDataList.size()) {
                    LeoLog.v(TAG, "selectAllGroup");
                    mAdapter.selectAll();
                    mSelectBtn.setText(R.string.app_select_none);
                    mSelectBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.no_select_all_selector), null,
                            null);
                } else {
                    LeoLog.v(TAG, "cancelSelectAllGroup");
                    mAdapter.deselectAll();
                    mSelectBtn.setText(R.string.app_select_all);
                    mSelectBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.select_all_selector), null,
                            null);

                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mIgnoreDlg != null && mIgnoreDlg.isShowing()) {
            mIgnoreDlg.dismiss();
            mIgnoreDlg = null;
        }
    }

    @Override
    public void onSelectionChange(boolean selectAll, int selectedCount) {
        if (selectedCount > 0) {
            mHideBtn.setEnabled(true);
            mProcessBtn.setEnabled(true);
            mProcessClick.setEnabled(true);
            mProcessBtn.setBackgroundResource(R.drawable.green_radius_btn_shape);
            mHideBtn.setText(getString(R.string.new_hide_num, mAdapter.getSelectedList() == null ? 0 : mAdapter.getSelectedList().size()));
        } else {
            mProcessBtn.setEnabled(false);
            mProcessClick.setEnabled(false);
            mProcessBtn.setBackgroundResource(R.drawable.green_radius_shape_disable);
            mHideBtn.setText(getString(R.string.app_hide_image));
            mHideBtn.setEnabled(false);
        }
    }

    private void initIgnoreDlg() {
        if (mIgnoreDlg != null) return;

        mIgnoreDlg = new LEOAlarmDialog(getActivity());
        String content = getString(R.string.pri_pro_ignore_dialog);
        mIgnoreDlg.setContent(content);
        mIgnoreDlg.setCanceledOnTouchOutside(false);
        mIgnoreDlg.setLeftBtnListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "process", getSkipCancelDesc());
                mIgnoreDlg.dismiss();
            }
        });
        mIgnoreDlg.setRightBtnListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "process", getSkipConfirmDesc());
//                onIgnoreClick(false);
                mIgnoreDlg.dismiss();
            }
        });
    }

    protected abstract int getIgnoreStringId();

    protected abstract String getSkipConfirmDesc();

    protected abstract String getSkipCancelDesc();

    protected abstract String getFolderFullDesc();
}