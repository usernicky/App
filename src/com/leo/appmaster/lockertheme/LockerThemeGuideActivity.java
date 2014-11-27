
package com.leo.appmaster.lockertheme;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.leo.appmaster.R;

public class LockerThemeGuideActivity extends Activity {
    private TextView mMakeSure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locker_theme_guide);
        mMakeSure = (TextView) findViewById(R.id.tv_save);
        mMakeSure.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                LockerThemeGuideActivity.this.finish();
            }
        });
    }
}
