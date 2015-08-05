
package com.leo.appmaster.videohide;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.R;
import com.leo.appmaster.browser.aidl.mInterface;
import com.leo.appmaster.engine.AppLoadEngine;
import com.leo.appmaster.model.AppItemInfo;
import com.leo.appmaster.privacy.PrivacyHelper;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.ui.CommonTitleBar;
import com.leo.appmaster.ui.LeoPictureViewPager;
import com.leo.appmaster.ui.LeoPictureViewPager.OnPageChangeListener;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog.OnDiaogClickListener;
import com.leo.appmaster.utils.FileOperationUtil;
import com.leo.appmaster.utils.LeoLog;
import com.leo.imageloader.DisplayImageOptions;
import com.leo.imageloader.ImageLoader;
import com.leo.imageloader.ImageLoaderConfiguration;
import com.leo.imageloader.core.FadeInBitmapDisplayer;
import com.leo.imageloader.core.ImageScaleType;

public class VideoViewPager extends BaseActivity implements OnClickListener {
    private static final int SHOW_TOAST = 0;
    private static final int SHOW_DIALOG = 1;
    private CommonTitleBar mTtileBar;
    private Button mUnhideVideo;
    private Button mCancelVideo;
    private String mPath;
    private ArrayList<String> mAllPath;
    private LeoPictureViewPager viewPager;
    private int mPosition = 0;
    private LEOAlarmDialog mDialog;
    private static final int DIALOG_CANCLE_VIDEO = 0;
    private static final int DIALOG_DELECTE_VIDEO = 1;
    private VideoPagerAdapter mPagerAdapter;
    private ArrayList<String> mResultPath;
    public static final int REQUEST_CODE_LOCK = 1000;
    public static final int REQUEST_CODE_OPTION = 1001;
    public static final int JUMP_GP = 0;
    public static final int JUMP_URL = 1;
    private DisplayImageOptions mOptions;
    private ImageLoader mImageLoader;

    private mInterface mService;
    private ServiceConnection mConnection;
    private int mCbVersionCode = -1;
    private boolean isCbHere = false;
    private String mLastName;
    private String mSecondName;
    private boolean isServiceDo = false;
    private boolean isBindServiceOK = false;
    private boolean isHaveServiceToBind = false;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case SHOW_TOAST:
                    Toast.makeText(VideoViewPager.this,
                            getString(R.string.video_delete_fail), 0)
                            .show();
                    break;
                case SHOW_DIALOG:
                    String mContentString = (String) msg.obj;
                    showDownLoadNewCbDialog(mContentString);
                    break;
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_video);
        mAllPath = new ArrayList<String>();
        new ArrayList<View>();
        mResultPath = new ArrayList<String>();
        mTtileBar = (CommonTitleBar) findViewById(R.id.layout_title_bar_video);
        mTtileBar.setTitle("");
        mTtileBar.openBackView();
        mUnhideVideo = (Button) findViewById(R.id.unhide_video);
        mCancelVideo = (Button) findViewById(R.id.delete_video);
        initImageLoder();
        mCancelVideo.setOnClickListener(this);
        mUnhideVideo.setOnClickListener(this);
        /* get Path */
        getIntentPath();
        viewPager = (LeoPictureViewPager) findViewById(R.id.picture_view_pager);
        viewPager.setOffscreenPageLimit(2);
        mPagerAdapter = new VideoPagerAdapter(this);
        viewPager.setAdapter(mPagerAdapter);
        viewPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                mPosition = position;
                mTtileBar.setTitle(FileOperationUtil.getNoExtNameFromHideFilepath(mAllPath
                        .get(mPosition)));
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        if (mPath != null && !mPath.equals("")) {
            String videoName = FileOperationUtil.getNoExtNameFromHideFilepath(mPath);
            mTtileBar.setTitle(videoName);
            viewPager.setCurrentItem(mPosition, true);
        }
        getResultValue();

        // coolbrowser aidl
        gotoBindService();

    }

    private void gotoBindService() {
        mConnection = new AdditionServiceConnection();
        Intent intent = new Intent("com.appmater.aidl.service");
        LeoLog.d("testBindService", "bindService");
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    class AdditionServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder boundService) {
            mService = mInterface.Stub.asInterface((IBinder) boundService);
            isHaveServiceToBind = true;
            isBindServiceOK = true;
            LeoLog.d("testBindService", "connect service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            isHaveServiceToBind = true;
            isBindServiceOK = false;
            LeoLog.d("testBindService", "disconnect service");
        }
    }

    @Override
    protected void onResume() {
        mLastName = FileOperationUtil.getDirNameFromFilepath(mPath);
        mSecondName = FileOperationUtil
                .getSecondDirNameFromFilepath(mPath);
        checkCbAndVersion();
        super.onResume();
    }

    private void checkCbAndVersion() {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> list = packageManager
                .getInstalledPackages(PackageManager.GET_PERMISSIONS);

        for (PackageInfo packageInfo : list) {
            String packNameString = packageInfo.packageName;
            if (packNameString.equals(VideoHideMainActivity.CB_PACKAGENAME)) {
                isCbHere = true;
                mCbVersionCode = packageInfo.versionCode;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mImageLoader != null) {
            mImageLoader.clearMemoryCache();
        }
    }

    private void getResultValue() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("path", mResultPath);
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
    }

    /**
     * getIntent
     */
    private void getIntentPath() {
        Intent intent = getIntent();
        mPath = intent.getStringExtra("path");
        mAllPath = intent.getStringArrayListExtra("mAllPath");
        mPosition = intent.getIntExtra("position", 0);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    private void initImageLoder() {
        mOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.video_loading)
                .showImageForEmptyUri(R.drawable.video_loading)
                .showImageOnFail(R.drawable.video_loading)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .displayer(new FadeInBitmapDisplayer(500))
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
                .build();
        mImageLoader = ImageLoader.getInstance();
        mImageLoader.init(ImageLoaderConfiguration.createDefault(this));
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.unhide_video:

                if (mLastName.equals(VideoHideMainActivity.LAST_CATALOG)
                        && mSecondName.equals(VideoHideMainActivity.SECOND_CATALOG) && isCbHere
                        && isHaveServiceToBind && isBindServiceOK) {
                    isServiceDo = true;
                }

                String cancleHideVideoText = getString(R.string.app_unhide_dialog_content_video);
                showAlarmDialog(cancleHideVideoText, DIALOG_CANCLE_VIDEO);

                // new
                // if (mLastName.equals(VideoHideMainActivity.LAST_CATALOG)
                // && mSecondName.equals(VideoHideMainActivity.SECOND_CATALOG))
                // {
                // if (isCbHere) {
                // if (isHaveServiceToBind) {
                // if (isBindServiceOK) {
                // isServiceDo = true;
                // }
                // String cancleHideVideoText =
                // getString(R.string.app_unhide_dialog_content_video);
                // showAlarmDialog(cancleHideVideoText, DIALOG_CANCLE_VIDEO);
                // } else {
                // // need new cb
                // String mContentString =
                // getString(R.string.video_hide_need_new_cb);
                // showDownLoadNewCbDialog(mContentString);
                // }
                // } else {
                // // no cb , goto download
                // String mContentString =
                // getString(R.string.video_hide_need_cb);
                // showDownLoadNewCbDialog(mContentString);
                // }
                // } else {
                // String cancleHideVideoText =
                // getString(R.string.app_unhide_dialog_content_video);
                // showAlarmDialog(cancleHideVideoText, DIALOG_CANCLE_VIDEO);
                // }

                break;
            case R.id.delete_video:

                if (mLastName.equals(VideoHideMainActivity.LAST_CATALOG)
                        && mSecondName.equals(VideoHideMainActivity.SECOND_CATALOG) && isCbHere
                        && isHaveServiceToBind && isBindServiceOK) {
                    isServiceDo = true;
                }

                String deleteHideVideoText = getString(R.string.app_delete_dialog_content_video);
                showAlarmDialog(deleteHideVideoText, DIALOG_DELECTE_VIDEO);

                // if (mLastName.equals(VideoHideMainActivity.LAST_CATALOG)
                // && mSecondName.equals(VideoHideMainActivity.SECOND_CATALOG))
                // {
                // if (isCbHere) {
                // if (isHaveServiceToBind) {
                // if (isBindServiceOK) {
                // isServiceDo = true;
                // }
                // String deleteHideVideoText =
                // getString(R.string.app_delete_dialog_content_video);
                // showAlarmDialog(deleteHideVideoText, DIALOG_DELECTE_VIDEO);
                // } else {
                // // need new cb
                // String mContentString =
                // getString(R.string.video_delete_need_new_cb);
                // showDownLoadNewCbDialog(mContentString);
                // }
                // } else {
                // // no cb , goto download
                // String mContentString =
                // getString(R.string.video_hide_need_cb);
                // showDownLoadNewCbDialog(mContentString);
                // }
                // } else {
                // String deleteHideVideoText =
                // getString(R.string.app_delete_dialog_content_video);
                // showAlarmDialog(deleteHideVideoText, DIALOG_DELECTE_VIDEO);
                // }

                break;
            default:
                break;
        }
    }

    /**
     * There are this App
     */
    private boolean isVideo(String packageName) {
        boolean flag = false;
        ArrayList<AppItemInfo> appInfo = AppLoadEngine.getInstance(this).getAllPkgInfo();
        for (AppItemInfo appDetailInfo : appInfo) {
            String pn = appDetailInfo.packageName;
            if (pn.equals(packageName)) {
                return flag = true;
            }
        }
        return flag;
    }

    /**
     * ViewPagerAdapter PagerAdapter
     */
    private class VideoPagerAdapter extends PagerAdapter {

        public VideoPagerAdapter(Context context) {

        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getCount() {
            return mAllPath.size();
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            View view = (View) object;
            ((ViewGroup) container).removeView(view);
        }

        @Override
        public Object instantiateItem(View container, int position) {

            String path = mAllPath.get(position);
            View view = LayoutInflater.from(VideoViewPager.this).inflate(R.layout.item_pager_video,
                    null);
            ImageView imageView = (ImageView) view.findViewById(R.id.zoom_image_view);
            imageView.setTag(path);
            ImageView imageViewT = imageView;
            String pathT = path;
            // imageViewT.setTag(path);
            imageView.setImageDrawable(VideoViewPager.this.getResources()
                    .getDrawable(R.drawable.video_loading));
            // AsyncLoadImage asyncLoadImage = new AsyncLoadImage();
            // Drawable drawableCache = asyncLoadImage.loadImage(imageView,
            // pathT,
            // new ImageCallback() {
            // @Override
            // public void imageLoader(Drawable drawable) {
            // if (imageViewT != null && imageViewT.getTag().equals(pathT)
            // && drawable != null) {
            // imageViewT.setImageDrawable(drawable);
            // }
            // }
            // });
            // if (drawableCache != null) {
            // imageView.setImageDrawable(drawableCache);
            // }
            String filePath = "voidefile://" + path;
            mImageLoader.displayImage(filePath, imageView, mOptions);
            imageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    try {
                        // if (isVideoFlag) {
                        String path = mAllPath.get(mPosition);
                        // ComponentName componentName = new
                        // ComponentName(VIDEO_PLUS_PACKAGE_NAME,
                        // VIDEO_PLAYER_ACTIVITY);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse("file://" + path), "video/*");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        // intent.setComponent(componentName);

                        // startActivityForResult(intent, 1001);
                        startActivity(intent);
                    } catch (Exception e) {

                    }

                    // } else {
                    // showAlarmDialogPlayer();
                    // }
                }

            });

            ((LeoPictureViewPager) container).addView(view);
            return view;
        }
    }

    /**
     * showAlarmDialog
     * 
     * @param string
     * @param flag
     */
    private void showAlarmDialog(final String string, final int flag) {
        if (mDialog == null) {
            mDialog = new LEOAlarmDialog(this);
        }
        mDialog.setOnClickListener(new OnDiaogClickListener() {
            @Override
            public void onClick(int which) {
                if (which == 1) {
                    if (flag == DIALOG_CANCLE_VIDEO) {
                        BackgoundTask backgoundTask = new BackgoundTask(VideoViewPager.this);
                        backgoundTask.execute(true);
                    } else if (flag == DIALOG_DELECTE_VIDEO) {
                        AppMasterApplication.getInstance().postInAppThreadPool(new Runnable() {
                            @Override
                            public void run() {
                                deleteVideo();
                            }
                        });
                    }
                }
            }
        });
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setTitle(R.string.app_cancel_hide_image);
        mDialog.setSureButtonText(getString(R.string.makesure));
        mDialog.setContent(string);
        mDialog.show();
    }

    public void showDownLoadNewCbDialog(String mContentString) {
        if (mDialog == null) {
            mDialog = new LEOAlarmDialog(this);
        }
        mDialog.setOnClickListener(new OnDiaogClickListener() {
            @Override
            public void onClick(int which) {
                if (which == 1) {
                    // getURL and go browser
                    requestUrl();
                }
            }
        });
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setContent(mContentString);
        mDialog.setSureButtonText(getString(R.string.button_install));
        mDialog.show();
    }

    private void requestUrl() {
        String CB_FINAL_URL = VideoHideMainActivity.URL_CB + "?id="
                + this.getString(R.string.channel_code);
        Uri uri = Uri.parse(CB_FINAL_URL);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        this.startActivity(intent);
    }

    // /**
    // * showAlarmDialogPlayer , Download Video Plus
    // */
    // private void showAlarmDialogPlayer() {
    // if (mDialog == null) {
    // mDialog = new LEOAlarmDialog(this);
    // }
    // mDialog.setOnClickListener(new OnDiaogClickListener() {
    // @Override
    // public void onClick(int which) {
    // if (which == 1) {
    // boolean isGpFlag = isVideo(Constants.GP_PACKAGE);
    // if (isGpFlag) {
    // if (true) {
    // Intent intent = new Intent(Intent.ACTION_VIEW);
    // Uri uri = Uri
    // .parse(Constants.VIDEO_PLUS_GP);
    // intent.setData(uri);
    // ComponentName cn = new ComponentName(
    // "com.android.vending",
    // "com.google.android.finsky.activities.MainActivity");
    // intent.setComponent(cn);
    // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    // startActivity(intent);
    // }
    // } else {
    // if (true) {
    // Uri uri = Uri
    // .parse(Constants.VIDEO_PLUS_GP_URL);
    // Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    // startActivity(intent);
    // }
    // }
    // }
    // }
    // });
    // mDialog.setTitle(getString(R.string.hide_video_dialog_title));
    // mDialog.setContent(getString(R.string.hide_video_dialog_content));
    // mDialog.setLeftBtnStr(getString(R.string.cancel));
    // mDialog.setRightBtnStr(getString(R.string.button_install));
    // mDialog.show();
    // }

    /**
     * delete Video
     */
    private void deleteVideo() {
        boolean flag = false;
        String filePath = mAllPath.get(mPosition);
        // if (!FileOperationUtil.deleteFile(filePath)) {
        // return;
        // }

        if (isServiceDo) {
            int mProcessType = -1;
            try {
                mProcessType =
                        mService.deleteVideo(filePath);
                if (mProcessType == 0) {
                    mAllPath.remove(mPosition);
                    flag = true;
                }
            } catch (RemoteException e) {
            }

            // if cb can not do this , pg do this
            if (!flag) {
                try {
                    flag = FileOperationUtil.deleteFile(filePath);
                    FileOperationUtil.deleteFileMediaEntry(filePath, this);
                    mAllPath.remove(mPosition);
                    // flag = true;
                } catch (Exception e) {
                    flag = false;
                }
            }

        } else {
            try {
                flag = FileOperationUtil.deleteFile(filePath);
                FileOperationUtil.deleteFileMediaEntry(filePath, this);
                mAllPath.remove(mPosition);
                // flag = true;
            } catch (Exception e) {
                flag = false;
            }
        }

        if (flag) {
            int number = mAllPath.size();
            if (number == 0) {
                Intent intent = new Intent();
                intent.setClass(VideoViewPager.this, VideoHideMainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else if (mPosition == 0) {
                mTtileBar.setTitle(FileOperationUtil.getNoExtNameFromHideFilepath(mAllPath
                        .get(mPosition)));
            } else {
                if (mPosition == number) {
                    mPosition = 0;
                    mTtileBar.setTitle(FileOperationUtil.getNoExtNameFromHideFilepath(mAllPath
                            .get(mPosition)));
                }
            }
            mResultPath.add(filePath);
            mPagerAdapter = new VideoPagerAdapter(VideoViewPager.this);
            viewPager.setAdapter(mPagerAdapter);
        } else {
            if (isServiceDo) {
                Message msg = Message.obtain();
                msg.what = SHOW_TOAST;
                mHandler.sendMessage(msg);

            } else {
                String mContentString;
                if (!isCbHere) {// no cb
                    mContentString = getString(R.string.video_hide_need_cb);
                } else if (!isHaveServiceToBind) {
                    mContentString = getString(R.string.video_hide_need_new_cb);
                } else {
                    mContentString = getString(R.string.video_hide_need_new_cb);
                }

                Message msg = Message.obtain();
                msg.what = SHOW_DIALOG;
                msg.obj = mContentString;
                mHandler.sendMessage(msg);
            }
        }
        isServiceDo = false;
        PrivacyHelper.getInstance(this).computePrivacyLevel(PrivacyHelper.VARABLE_HIDE_VIDEO);
    }

    /**
     * unHideVideo BackgoundTask
     * 
     * @author run
     */
    private class BackgoundTask extends AsyncTask<Boolean, Integer, Boolean> {
        private Context context;

        BackgoundTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            String newFileName = null;
            boolean isSuccess = true;
            Boolean flag = params[0];
            if (flag && mPosition < mAllPath.size()) {
                String path = mAllPath.get(mPosition);
                if (isServiceDo) {
                    int mProcessType = -1;
                    try {
                        mProcessType =
                                mService.cancelHide(path);
                        if (mProcessType == 0) {
                            mResultPath.add(path);
                            mAllPath.remove(mPosition);
                        } else if (mProcessType == -1) {
                            isSuccess = false;
                        }
                    } catch (RemoteException e) {
                        isSuccess = false;
                    }

                    // if cb can not do this , pg do this
                    if (!isSuccess) {
                        newFileName = FileOperationUtil.getNameFromFilepath(path);
                        try {
                            newFileName = newFileName.substring(1,
                                    newFileName.indexOf(".leotmv"));
                            if (!FileOperationUtil.renameFile(path, newFileName)) {
                                return isSuccess = false;
                            } else {
                                mResultPath.add(path);
                                FileOperationUtil.saveFileMediaEntry(
                                        FileOperationUtil.makePath(
                                                FileOperationUtil.getDirPathFromFilepath(path),
                                                newFileName),
                                        context);
                                FileOperationUtil.deleteFileMediaEntry(path, context);
                                mAllPath.remove(mPosition);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    newFileName = FileOperationUtil.getNameFromFilepath(path);
                    try {
                        newFileName = newFileName.substring(1,
                                newFileName.indexOf(".leotmv"));
                        if (!FileOperationUtil.renameFile(path, newFileName)) {
                            return isSuccess = false;
                        } else {
                            mResultPath.add(path);
                            FileOperationUtil.saveFileMediaEntry(
                                    FileOperationUtil.makePath(
                                            FileOperationUtil.getDirPathFromFilepath(path),
                                            newFileName),
                                    context);
                            FileOperationUtil.deleteFileMediaEntry(path, context);
                            mAllPath.remove(mPosition);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return isSuccess;
        }

        @Override
        protected void onPostExecute(final Boolean isSuccess) {
            if (isSuccess) {
                int number = mAllPath.size();
                if (number == 0) {
                    Intent intent = new Intent();
                    intent.setClass(VideoViewPager.this, VideoHideMainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else if (mPosition == 0) {
                    mTtileBar.setTitle(FileOperationUtil.getNoExtNameFromHideFilepath(mAllPath
                            .get(mPosition)));
                } else {
                    if (mPosition == number) {
                        mPosition = 0;
                        mTtileBar.setTitle(FileOperationUtil.getNoExtNameFromHideFilepath(mAllPath
                                .get(mPosition)));
                    }
                }
                mPagerAdapter = new VideoPagerAdapter(VideoViewPager.this);
                viewPager.setAdapter(mPagerAdapter);

            } else {
                if (isServiceDo) {
                    Toast.makeText(VideoViewPager.this, getString(R.string.video_cencel_hide_fail),
                            0)
                            .show();
                } else {
                    String mContentString;
                    if (!isCbHere) {// no cb
                        mContentString = getString(R.string.video_hide_need_cb);
                    } else if (!isHaveServiceToBind) {
                        mContentString = getString(R.string.video_hide_need_new_cb);
                    } else {
                        mContentString = getString(R.string.video_hide_need_new_cb);
                    }
                    showDownLoadNewCbDialog(mContentString);
                }
            }
            isServiceDo = false;
            // video change, recompute privacy level
            PrivacyHelper.getInstance(VideoViewPager.this).computePrivacyLevel(
                    PrivacyHelper.VARABLE_HIDE_VIDEO);
        }
    }

}
