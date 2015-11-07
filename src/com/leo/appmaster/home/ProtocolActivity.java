
package com.leo.appmaster.home;

import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;

import com.leo.appmaster.R;
import com.leo.appmaster.quickgestures.ISwipUpdateRequestManager;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.ui.CommonTitleBar;
import com.leo.appmaster.ui.CommonToolbar;

public class ProtocolActivity extends BaseActivity implements OnClickListener {
    private CommonToolbar mTtileBar;
    private WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protocol);
        mTtileBar = (CommonToolbar) findViewById(R.id.appwallTB);
        mTtileBar.setToolbarTitle(R.string.protocolBar);
//        mTtileBar.setOptionImageVisibility(View.VISIBLE);
        webView = (WebView) findViewById(R.id.webView);

        String area = Locale.getDefault().getLanguage();
        if (area.equalsIgnoreCase("zh")) {
            webView.loadUrl("file:///android_asset/protocol_zh.html");
        } else {
            webView.loadUrl("file:///android_asset/protocol_en.html");
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

    }

}
