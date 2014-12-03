package com.leo.appmaster.ui;

import java.util.ArrayList;
import java.util.List;

import com.leo.appmaster.R;
import com.leo.appmaster.engine.AppLoadEngine;
import com.leo.appmaster.model.BaseAppInfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.PageTransformer;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PagedGridView extends LinearLayout {

	private int mCellX, mCellY;
	private LeoAppViewPager mViewPager;
	private CirclePageIndicator mIndicator;
	private LayoutInflater mInflater;

	private PagerAdapter mAdapter;
	private int mPageItemCount;
	private ArrayList<GridView> mGridViewList;
	private ArrayList<List<BaseAppInfo>> mPageDatas;

	private OnItemClickListener mClickListener;
	private OnTouchListener mTouchListener;
	private int mPageCount;

	public PagedGridView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mInflater = LayoutInflater.from(context);
	}

	public void setDatas(List<BaseAppInfo> data, int cellX, int cellY) {
		mCellX = cellX;
		mCellY = cellY;
		mPageItemCount = mCellX * mCellY;
		updateUI(data);
	}

	private void updateUI(List<BaseAppInfo> data) {
		mPageCount = (int) Math
				.ceil(((double) data.size()) / (mCellX * mCellY));
		int itemCounts[] = new int[mPageCount];
		int i;
		for (i = 0; i < itemCounts.length; i++) {
			if (i == itemCounts.length - 1) {
				itemCounts[i] = data.size() / mPageItemCount;
			} else {
				itemCounts[i] = mPageItemCount;
			}
		}

		mGridViewList = new ArrayList<GridView>();
		mPageDatas = new ArrayList<List<BaseAppInfo>>();

		for (i = 0; i < mPageCount; i++) {
			GridviewAdapter adapter = null;
			List<BaseAppInfo> pageData = null;
			GridView gridView = (GridView) mInflater.inflate(
					R.layout.grid_page_item, mViewPager, false);
			if (i == mPageCount - 1) {
				pageData = copyFrom(data.subList(i * mPageItemCount,
						data.size()));
				adapter = new GridviewAdapter(pageData, i);
				gridView.setAdapter(adapter);

			} else {
				pageData = copyFrom(data.subList(i * mPageItemCount, (i + 1)
						* mPageItemCount));
				adapter = new GridviewAdapter(pageData, i);
				gridView.setAdapter(adapter);
			}
			if (mClickListener != null) {
				gridView.setOnItemClickListener(mClickListener);
			}

			gridView.setOnTouchListener(mTouchListener);
			mGridViewList.add(gridView);
			mPageDatas.add(pageData);
		}

		mAdapter = new DataPagerAdapter();
		mViewPager.setAdapter(mAdapter);
		mIndicator.setViewPager(mViewPager);
	}

	public void notifyChange(List<BaseAppInfo> list) {
		updateUI(list);
	}

	private List<BaseAppInfo> copyFrom(List<BaseAppInfo> source) {
		ArrayList<BaseAppInfo> list = null;
		if (source != null) {
			list = new ArrayList<BaseAppInfo>();
			BaseAppInfo item;
			for (BaseAppInfo info : source) {
				item = new BaseAppInfo();
				item.packageName = info.packageName;
				item.icon = info.icon;
				item.label = info.label;
				item.isLocked = info.isLocked;
				list.add(item);
			}
		}
		return list;
	}

	public void setItemClickListener(OnItemClickListener listener) {
		mClickListener = listener;
		if (mGridViewList != null) {
			for (GridView gridView : mGridViewList) {
				gridView.setOnItemClickListener(mClickListener);
			}
		}
	}

	public void setItemTouchListener(OnTouchListener listener) {
		mTouchListener = listener;
	}

	@Override
	protected void onFinishInflate() {
		mInflater.inflate(R.layout.paged_gridview, this, true);
		mViewPager = (LeoAppViewPager) findViewById(R.id.pager);
		// mViewPager.setPageTransformer(true, new DepthPageTransformer());
		mIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
		super.onFinishInflate();
	}

	private class DataPagerAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return mGridViewList.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			if (position < mGridViewList.size()) {
				container.removeView(mGridViewList.get(position));
			}
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			container.addView(mGridViewList.get(position), 0);
			return mGridViewList.get(position);
		}
	}

	private class GridviewAdapter extends BaseAdapter {
		List<BaseAppInfo> mList;

		private int mPageIndex;

		public GridviewAdapter(List<BaseAppInfo> list, int page) {
			super();
			mPageIndex = page;
			initData(list);
		}

		private void initData(List<BaseAppInfo> list) {
			mList = list;
		}

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.app_item, null);
			}

			LockImageView imageView = (LockImageView) convertView
					.findViewById(R.id.iv_app_icon);
			TextView textView = (TextView) convertView
					.findViewById(R.id.tv_app_name);
			BaseAppInfo info = mList.get(position);

			if (AppLoadEngine.getInstance(getContext()).getRecommendLockList()
					.contains(info.packageName)) {
				imageView.setRecommend(true);
			}

			imageView.setLocked(info.isLocked);
			imageView.setImageDrawable(info.icon);
			textView.setText(info.label);
			convertView.setTag(info);
			return convertView;
		}
	}

	public class DepthPageTransformer implements PageTransformer {
		private static final float MIN_SCALE = 0.75f;

		@SuppressLint("NewApi")
		@Override
		public void transformPage(View view, float position) {
			int pageWidth = view.getWidth();
			if (position < -1) { // [-Infinity,-1)
									// This page is way off-screen to the left.
				view.setAlpha(0);
			} else if (position <= 0) { // [-1,0]
										// Use the default slide transition when
										// moving to the left page
				view.setAlpha(1);
				view.setTranslationX(0);
				view.setScaleX(1);
				view.setScaleY(1);
			} else if (position <= 1) { // (0,1]
										// Fade the page out.
				view.setAlpha(1 - position);
				// Counteract the default slide transition
				view.setTranslationX(pageWidth * -position);
				// Scale the page down (between MIN_SCALE and 1)
				float scaleFactor = MIN_SCALE + (1 - MIN_SCALE)
						* (1 - Math.abs(position));
				view.setScaleX(scaleFactor);
				view.setScaleY(scaleFactor);
			} else { // (1,+Infinity]
						// This page is way off-screen to the right.
				view.setAlpha(0);

			}
		}

	}
}
