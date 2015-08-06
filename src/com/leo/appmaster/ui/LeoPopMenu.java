
package com.leo.appmaster.ui;

import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.leo.appmaster.utils.DipPixelUtil;
import com.leo.appmaster.utils.LeoLog;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.R;

public class LeoPopMenu {

    public final static int DIRECTION_DOWN = 1;
    // public final static float SMALLWidth = 160.0f;
    public final static float SMALLWidth = 160.0f;
    public final static float LongWidth = 230.0f;

    public final static float OVERPX = 220.0f;
    public static boolean isOverWidth = false;

    protected static float newSmallWidth;
    protected static float newLongWidth;
    protected  int mIconOffest = 0;
    protected Context mContext;

    public static class LayoutStyles {
        public int width;
        public int height;
        public int animation;
        public int direction;
    }

    protected PopupWindow mLeoPopMenu;

    protected List<String> mItems;
    protected List<Integer> mIcons;

    protected OnItemClickListener mPopItemClickListener;

    protected ListView mListView;
    protected BaseAdapter mAdapter;
    protected LayoutStyles mStyles = new LayoutStyles();
    protected boolean mIsItemHTMLFormatted = false;

    protected int mAnimaStyle = -1;

    /**
     * @param aContext
     * @param anchorView 显示tab列表
     */
    public void showPopMenu(Activity activity, View anchorView, LayoutStyles styles,
            OnDismissListener dimissListener) {
        if (mLeoPopMenu != null) {
            if (mLeoPopMenu.isShowing()) {
                return;
            }
            mLeoPopMenu = null;
        }

        setWindowStyle(styles);

        View convertView = buildTabListLayout();
        mLeoPopMenu = new PopupWindow(convertView, mStyles.width, mStyles.height, true);
        mLeoPopMenu.setFocusable(true);
        mLeoPopMenu.setOutsideTouchable(true);
        mLeoPopMenu.setOnDismissListener(dimissListener);
        mLeoPopMenu.setBackgroundDrawable(AppMasterApplication.getInstance()
                .getResources().getDrawable(R.drawable.popup_menu_bg));
        mLeoPopMenu.setAnimationStyle(mAnimaStyle);
        mLeoPopMenu.update();

        mLeoPopMenu.setAnimationStyle(mStyles.animation);
        // mLeoPopMenu.showAsDropDown(anchorView, 0, 0);
        if (mStyles != null && mStyles.direction == DIRECTION_DOWN) {
            mLeoPopMenu.showAtLocation(anchorView, Gravity.NO_GRAVITY, 0, 0);
        } else {
            mLeoPopMenu.showAsDropDown(anchorView, 50, 0);
        }
        mLeoPopMenu.update(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        
    }

    private void setWindowStyle(LayoutStyles styles) {
        if (styles == null) {
            float popWidth = 0;
            if (!isOverWidth) {
                LeoLog.d("LeoPopMenu", "newSmallWidth is : " + newSmallWidth);
                newSmallWidth += DipPixelUtil.dip2px(mContext, mIconOffest);
                popWidth = newSmallWidth;
            } else {
                LeoLog.d("LeoPopMenu", "newLongWidth is : " + newLongWidth);
                newLongWidth += DipPixelUtil.dip2px(mContext, mIconOffest);
                popWidth = newLongWidth;
            }
            LeoLog.d("LeoPopMenu", "popWidth is : " + popWidth+" mIconOffest = "+mIconOffest);
            mStyles.width = DipPixelUtil.dip2px((Context) mContext, popWidth);
            // LeoLog.d("LeoPopMenu", "dip2px popWidth is : " + mStyles.width);
            mStyles.height = LayoutParams.WRAP_CONTENT;
            if (mAnimaStyle != -1) {
                mStyles.animation = mAnimaStyle;
            } else {
                mStyles.animation = R.style.PopupListAnimUpDown;
            }
        } else {
            mStyles.width = styles.width;
            mStyles.height = styles.height;
            mStyles.animation = styles.animation;
            mStyles.direction = styles.direction;
        }
    }

    public void setAnimation(int animaStyle) {
        mAnimaStyle = animaStyle;
    }

    /**
     * call this to set mIsSpanedItem true when your item is HTML style format
     * string
     */
    public void setItemSpaned(boolean flag) {
        mIsItemHTMLFormatted = flag;
    }

    public void setOnDismiss(OnDismissListener l) {
        if (mLeoPopMenu != null) {
            mLeoPopMenu.setOnDismissListener(l);
        }
    }

    public void dismissSnapshotList() {
        if (mLeoPopMenu != null) {
            mLeoPopMenu.dismiss();
            mLeoPopMenu = null;
        }
    }

    private View buildTabListLayout() {
        LayoutInflater inflater = LayoutInflater.from(AppMasterApplication
                .getInstance());
        LinearLayout convertView = (LinearLayout) inflater.inflate(
                R.layout.popmenu_window_list_layout, null);
        mListView = (ListView) convertView.findViewById(R.id.menu_list);

        mListView.setOnItemClickListener(mPopItemClickListener);
        if (null == mAdapter) {
            mAdapter = new MenuListAdapter();
        }
        mListView.setAdapter(mAdapter);
        return convertView;
    }

    public void setPopMenuItems(Context context, List<String> items) {
        mItems = items;
        mContext = context;

        Display mDisplay = ((Activity) mContext).getWindowManager().getDefaultDisplay();
        int W = mDisplay.getWidth();
        int H = mDisplay.getHeight();
        LeoLog.d("LeoPopMenu", "Width = " + W);
        LeoLog.d("LeoPopMenu", "Height = " + H);

        float mMaxLength = 0;
        TextView testTextView = new TextView(mContext);
        for (int i = 0; i < mItems.size(); i++) {
            testTextView.setText(mItems.get(i));
            float mOne = getTextViewLength(testTextView, mItems.get(i));
            LeoLog.d("LeoPopMenu", "字符：" + mItems.get(i) + "...长度：" + mOne);
            if (mOne > mMaxLength) {
                mMaxLength = mOne;
            }
        }
        
        Log.e("hehe", "最长字符占的宽度px="+mMaxLength);
        
        

        if (W >= 1080) {
            if (mMaxLength > OVERPX) {
                isOverWidth = true;
                if (mMaxLength > 260) {
                    newLongWidth = mMaxLength - 130;
                } else {
                    newLongWidth = mMaxLength - 110;
                }
                if (newLongWidth > 210) {
                    newLongWidth = 210;
                }
                Log.i("tag","OVERPX mMaxLength = "+mMaxLength);
                Log.i("tag"," OVERPX newLongWidth = "+newLongWidth);
            } else {
                isOverWidth = false;
                if (mMaxLength < SMALLWidth) {
                    newSmallWidth = mMaxLength - 20;
                } else if (mMaxLength < 180) {
                    newSmallWidth = mMaxLength - 40;
                } else {
                    newSmallWidth = mMaxLength - 60;
                }
                Log.i("tag","SMALLWidth mMaxLength = "+mMaxLength);
                Log.i("tag"," SMALLWidth newSmallWidth = "+newSmallWidth);
            }
        } else if (W >= 720) {
            if (mMaxLength > OVERPX) {
                isOverWidth = true;
                newLongWidth = LongWidth - 30;
                if (newLongWidth > 210) {
                    newLongWidth = 210;
                }
            } else {
                isOverWidth = false;
                if (mMaxLength < SMALLWidth) {
                    newSmallWidth = mMaxLength;
                } else {
                    newSmallWidth = mMaxLength - 20;
                }
            }
        } else if (W >= 480) {
            if (mMaxLength > OVERPX) {
                isOverWidth = true;
                newLongWidth = LongWidth + 30;
                if (newLongWidth > 210) {
                    newLongWidth = 210;
                }
            } else {
                isOverWidth = false;
                newSmallWidth = mMaxLength + 40;
            }
        } else {
            if (mMaxLength > OVERPX) {
                isOverWidth = true;
                newLongWidth = LongWidth + 50;
                if (newLongWidth > 210) {
                    newLongWidth = 210;
                }
            } else {
                isOverWidth = false;
                newSmallWidth = mMaxLength + 60;
            }
        }
        
        
        Log.e("hehe", "是否超maxW="+isOverWidth);
        Log.e("hehe", "最终的minW="+newSmallWidth);
        Log.e("hehe", "最终的maxW="+newLongWidth);
        
        
        
        
        // 不改动上面的代码，对最终结果再做适配
        Locale locale = mContext.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        // Log.e("poha", language);
        
    }

    public List<String> getPopMenuItems() {
        return mItems;
    }

    public void setOnDismissListener() {

    }

    public void setPopItemClickListener(OnItemClickListener listener) {
        mPopItemClickListener = listener;
    }

    static class Holder {
        public TextView mItemName;
        public ImageView mItemIcon;
    }

    private class MenuListAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private Holder mHolder;

        private MenuListAdapter() {
            inflater = LayoutInflater.from(AppMasterApplication.getInstance());
        }

        @Override
        public int getCount() {
            if (mItems != null) {
                return mItems.size();
            } else {
                return 0;
            }

        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(final int position, View convertView,
                ViewGroup parent) {
            if (convertView != null) {
                mHolder = (Holder) convertView.getTag();
            } else {
                mHolder = new Holder();
                convertView = inflater.inflate(R.layout.popmenu_window_list_item, null);
                mHolder.mItemName = (TextView) convertView.findViewById(R.id.menu_text);
                convertView.setTag(mHolder);
            }

            if (mIsItemHTMLFormatted) {
                Spanned itemText = Html.fromHtml(mItems.get(position));
                mHolder.mItemName.setText(itemText);
            } else {
                mHolder.mItemName.setText(mItems.get(position));
            }
            return convertView;
        }
    }

    public static float getTextViewLength(TextView textView, String text) {
        TextPaint paint = textView.getPaint();
        // 得到使用该paint写上text的时候,像素为多少
        float textLength = paint.measureText(text);
        return textLength;
    }

    public void setListViewDivider(Drawable divider) {
        if (null != mListView) {
            mListView.setDivider(divider);
        }
    }
}
