
package com.leo.appmaster.applocker;

import android.app.Service;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.R;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog.OnDiaogClickListener;

public class ZhiWenActivity extends BaseActivity implements OnClickListener {
    private final static int ZHIWENWEIZHUANG = 3;
    private TextView tv_zhiwen_title, tv_zhiwen_jieshao;
    private ImageView iv_zhiwen_tips, zhiwen_bang, show_slowly_iv, iv_zhiwen_click;
    private AppMasterPreference sp_zhiwen_weizhuang;
    private float iv_zhiwen_click_height;
    private Vibrator vib;
    // three click
    long[] mHits = new long[3];
    private LEOAlarmDialog mAlarmDialog;

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    showDongHuaAlpha(1f, 0f);
                case 2:
                    showDongHuaTrans(iv_zhiwen_click_height - 30, 0);
                    break;
                default:
                    break;
            }

        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weizhuang_zhiwen);
        init();
    }

    private void init() {
        sp_zhiwen_weizhuang = AppMasterPreference.getInstance(this);
        tv_zhiwen_title = (TextView) findViewById(R.id.tv_zhiwen_title);
        tv_zhiwen_title.setVisibility(View.VISIBLE);
        tv_zhiwen_jieshao = (TextView) findViewById(R.id.tv_zhiwen_jieshao);
        tv_zhiwen_jieshao.setVisibility(View.VISIBLE);
        iv_zhiwen_tips = (ImageView) findViewById(R.id.iv_zhiwen_tips);
        iv_zhiwen_tips.setVisibility(View.VISIBLE);
        iv_zhiwen_click = (ImageView) findViewById(R.id.iv_zhiwen_click);
        iv_zhiwen_click.setOnClickListener(this);
        show_slowly_iv = (ImageView) findViewById(R.id.show_slowly_iv);
        zhiwen_bang = (ImageView) findViewById(R.id.zhiwen_bang);
        vib = (Vibrator) this.getSystemService(Service.VIBRATOR_SERVICE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            iv_zhiwen_click_height = iv_zhiwen_click.getHeight();
            showDongHuaTrans(0, iv_zhiwen_click_height - 30);
            showDongHuaAlpha(0f, 1f);
        }
        super.onWindowFocusChanged(hasFocus);
    }

    private void showDongHuaAlpha(final float i, final float j) {
        // zhiwen
        AlphaAnimation alpha = new AlphaAnimation(i, j);
        alpha.setDuration(3000);
        alpha.setFillAfter(true);
        alpha.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (i < j) {
                    handler.sendEmptyMessage(1);
                } else {
                    showDongHuaAlpha(0f, 1f);
                }
            }
        });
        show_slowly_iv.setAnimation(alpha);
    }

    private void showDongHuaTrans(final float k, final float z) {
        // zhiwen bang
        TranslateAnimation trans = new TranslateAnimation(0,
                0, k, z);
        trans.setDuration(3000);
        trans.setFillAfter(true);
        trans.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (k < z) {
                    handler.sendEmptyMessage(2);
                } else {
                    showDongHuaTrans(0, iv_zhiwen_click_height - 30);
                }
            }
        });
        zhiwen_bang.setAnimation(trans);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_zhiwen_click:
                System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                if (mHits[0] >= (SystemClock.uptimeMillis() - 800)) {
                    showAlarmDialog(getString(R.string.open_weizhuang_dialog_title),
                            getString(R.string.open_weizhuang_dialog_content),
                            getString(R.string.open_weizhuang_dialog_sure));
                }
                break;
            default:
                break;
        }
    }

    private void showAlarmDialog(String title, String content, String sureText) {
        if (mAlarmDialog == null) {
            mAlarmDialog = new LEOAlarmDialog(this);
            mAlarmDialog.setOnClickListener(new OnDiaogClickListener() {
                @Override
                public void onClick(int which) {
                    // ok
                    if (which == 1) {
                        makeText();
                        sp_zhiwen_weizhuang.setPretendLock(ZHIWENWEIZHUANG);
                        vib.vibrate(150);
                        ZhiWenActivity.this.finish();
                    }

                }
            });
        }
        mAlarmDialog.setSureButtonText(sureText);
        mAlarmDialog.setTitle(title);
        mAlarmDialog.setContent(content);
        mAlarmDialog.show();
    }

    protected void makeText() {
        Toast.makeText(this, getString(R.string.zhiwen_mode_ok), 0).show();
    }

    @Override
    protected void onDestroy() {
        if (mAlarmDialog != null) {
            mAlarmDialog.dismiss();
            mAlarmDialog = null;
        }
        super.onDestroy();
    }

}
