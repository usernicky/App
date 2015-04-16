
package com.leo.appmaster.applocker;

import com.leo.appmaster.R;
import com.leo.appmaster.utils.LeoLog;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

public class UnKnowCallActivity5 extends Activity implements OnTouchListener {
    private TextView tv_use_tips, tv_use_tips_content;
    private ImageView iv_dianhua_hold, iv_guaduan, iv_duanxin, iv_jieting, iv_guaduan_big,
            iv_duanxin_big, iv_jieting_big;
    private GestureRelative mViewContent;

    private int hold_left, hold_top, hold_right, hold_bottom;
    private float mYuanX, mYuanY, mZhiJing, mBanJing;
    private int startX, startY;
    private float lc_left, lc_top, lc_right, lc_bottom;
    private float hold_width, hold_height;
    private int gua_yuan_x, gua_yuan_y, gua_left, gua_top, gua_right, gua_bottom;
    private int duan_yuan_x, duan_yuan_y, duan_left, duan_top, duan_right, duan_bottom;
    private int jie_yuan_x, jie_yuan_y, jie_left, jie_top, jie_right, jie_bottom;
    private int gua_left_big, gua_top_big, gua_right_big, gua_bottom_big;
    private int duan_left_big, duan_top_big, duan_right_big, duan_bottom_big;
    private int jie_left_big, jie_top_big, jie_right_big, jie_bottom_big;

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    iv_dianhua_hold.layout(hold_left, hold_top, hold_right, hold_bottom);

                    iv_guaduan.layout(gua_left, gua_top, gua_right, gua_bottom);
                    iv_guaduan_big.layout(gua_left_big, gua_top_big, gua_right_big, gua_bottom_big);
                    
                    iv_duanxin.layout(duan_left, duan_top, duan_right, duan_bottom);
                    iv_duanxin_big.layout(duan_left_big, duan_top_big, duan_right_big, duan_bottom_big);
                    
                    iv_jieting.layout(jie_left, jie_top, jie_right, jie_bottom);
                    iv_jieting_big.layout(jie_left_big, jie_top_big, jie_right_big, jie_bottom_big);
                    break;
                default:
                    break;
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unknowcall_five);
        init();
    }

    private void init() {
        tv_use_tips = (TextView) findViewById(R.id.tv_use_tips);
        tv_use_tips.setVisibility(View.VISIBLE);
        tv_use_tips_content = (TextView) findViewById(R.id.tv_use_tips_content);
        tv_use_tips_content.setVisibility(View.VISIBLE);
        iv_guaduan = (ImageView) findViewById(R.id.iv_guaduan);

        mViewContent = (GestureRelative) findViewById(R.id.mid_view);
        iv_dianhua_hold = (ImageView) findViewById(R.id.iv_dianhua_hold);
        iv_dianhua_hold.setOnTouchListener(this);

        iv_guaduan = (ImageView) findViewById(R.id.iv_guaduan);
        iv_duanxin = (ImageView) findViewById(R.id.iv_duanxin);
        iv_jieting = (ImageView) findViewById(R.id.iv_jieting);

        iv_guaduan_big = (ImageView) findViewById(R.id.iv_guaduan_big);
        iv_duanxin_big = (ImageView) findViewById(R.id.iv_duanxin_big);
        iv_jieting_big = (ImageView) findViewById(R.id.iv_jieting_big);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {

            hold_width = iv_dianhua_hold.getWidth();
            hold_height = iv_dianhua_hold.getHeight();

            // 圆心在？
            mYuanX = mViewContent.getPointX();
            mYuanY = mViewContent.getPointY();
            LeoLog.d("testnewcall", "mYuanX : " + mYuanX + "mYuanY : " + mYuanY);

            // 直径是？
            mZhiJing = mViewContent.getZhiJing();
            mBanJing = mZhiJing / 2;
            LeoLog.d("testnewcall", "mZhiJing : " + mZhiJing + "mBanJing " + mBanJing);

            hold_left = (int) (mYuanX - (hold_width / 2));
            hold_top = (int) (mYuanY - (hold_height / 2));
            hold_right = (int) (mYuanX + (hold_width / 2));
            hold_bottom = (int) (mYuanY + (hold_height / 2));
            LeoLog.d("testnewcall", "hold_left is : " + hold_left
                    + "--hold_top is : " + hold_top + "--hold_right is : "
                    + hold_right + "hold_bottom is : " + hold_bottom);

            // 挂断，短信，接听icon位置
            setPosition();
            // 电话柄位于？
            setHold();
        }
        super.onWindowFocusChanged(hasFocus);
    }

    private void setPosition() {
        // 挂断
        gua_yuan_x = (int) (mYuanX - mBanJing);
        gua_yuan_y = (int) mYuanY;
        gua_left = gua_yuan_x - (iv_guaduan.getWidth() / 2);
        gua_top = gua_yuan_y - (iv_guaduan.getHeight() / 2);
        gua_right = gua_yuan_x + (iv_guaduan.getWidth() / 2);
        gua_bottom = gua_yuan_y + (iv_guaduan.getHeight() / 2);
        // big
        gua_left_big = gua_yuan_x - (iv_guaduan_big.getWidth() / 2);
        gua_top_big = gua_yuan_y - (iv_guaduan_big.getHeight() / 2);
        gua_right_big = gua_yuan_x + (iv_guaduan_big.getWidth() / 2);
        gua_bottom_big = gua_yuan_y + (iv_guaduan_big.getHeight() / 2);

        // 短信
        duan_yuan_x = (int) mYuanX;
        duan_yuan_y = (int) (mYuanY - mBanJing);
        duan_left = duan_yuan_x - (iv_duanxin.getWidth() / 2);
        duan_top = duan_yuan_y - (iv_duanxin.getHeight() / 2);
        duan_right = duan_yuan_x + (iv_duanxin.getWidth() / 2);
        duan_bottom = duan_yuan_y + (iv_duanxin.getHeight() / 2);
        // big
        duan_left_big = duan_yuan_x - (iv_duanxin_big.getWidth() / 2);
        duan_top_big = duan_yuan_y - (iv_duanxin_big.getHeight() / 2);
        duan_right_big = duan_yuan_x + (iv_duanxin_big.getWidth() / 2);
        duan_bottom_big = duan_yuan_y + (iv_duanxin_big.getHeight() / 2);

        // 接听
        jie_yuan_x = (int) (mYuanX + mBanJing);
        jie_yuan_y = (int) mYuanY;
        jie_left = jie_yuan_x - (iv_guaduan.getWidth() / 2);
        jie_top = jie_yuan_y - (iv_guaduan.getHeight() / 2);
        jie_right = jie_yuan_x + (iv_guaduan.getWidth() / 2);
        jie_bottom = jie_yuan_y + (iv_guaduan.getHeight() / 2);
        // big
        jie_left_big = jie_yuan_x - (iv_guaduan_big.getWidth() / 2);
        jie_top_big = jie_yuan_y - (iv_guaduan_big.getHeight() / 2);
        jie_right_big = jie_yuan_x + (iv_guaduan_big.getWidth() / 2);
        jie_bottom_big = jie_yuan_y + (iv_guaduan_big.getHeight() / 2);
    }

    private void setHold() {
        new Thread() {
            public void run() {
                try {
                    sleep(70);
                    handler.sendEmptyMessage(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        }.start();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.iv_dianhua_hold) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:// 手指按下屏幕
                    startX = (int) event.getRawX();
                    startY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int newX = (int) event.getRawX() - startX;
                    int newY = (int) event.getRawY() - startY;

                    lc_left = iv_dianhua_hold.getLeft();
                    lc_top = iv_dianhua_hold.getTop();
                    lc_right = iv_dianhua_hold.getRight();
                    lc_bottom = iv_dianhua_hold.getBottom();

                    int top = (int) (lc_top + newY);
                    int left = (int) (lc_left + newX);
                    int bottom = (int) (lc_bottom + newY);
                    int right = (int) (lc_right + newX);

                    if (left < gua_right + 10 && top > gua_top - gua_top / 2
                            && bottom < gua_bottom + gua_bottom / 2) {
                        iv_guaduan_big.setVisibility(View.VISIBLE);
                    }
                    
                    v.layout(left, top, right, bottom);
                    startX = (int) event.getRawX();
                    startY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    v.layout(hold_left, hold_top, hold_right, hold_bottom);
                    break;
                default:
                    break;
            }
        }
        return false;
    }
}
