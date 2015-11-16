
package com.leo.appmaster.intruderprotection;

import java.util.List;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;

public class CameraSurfacePreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
//    private boolean mIsActive = false;
    private boolean mIsinited = false;
    private Camera mCamera;
    private boolean mCanTake = true;
    private PreferenceTable mPt = PreferenceTable.getInstance();
    private PictureCallback mPendingCallback;

    public CameraSurfacePreview(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //创建时初始化，camera的初始化容易异常，使用try catch
        try {
            init();
        } catch (Exception e) {
        }
    }

    @SuppressWarnings("deprecation")
    public void init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            return;
        }
        int checkCameraFacing = CameraUtils.checkCameraFacing();
        // 打开摄像头，默认优先级是：前置，后置 都没有 直接返回
        if ((checkCameraFacing == CameraUtils.FRONT_AND_BACK)
                || (checkCameraFacing == CameraUtils.FRONT_FACING_ONLY)) {
            mCamera = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
        } 
//        else if (checkCameraFacing == CameraUtils.BACK_FACING_ONLY) {
//            mCamera = Camera.open(CameraInfo.CAMERA_FACING_BACK);
//        }
        else {
            return;
        }
        try {
            //设置照相机的参数
            mCamera.setDisplayOrientation(90);          //预览时的角度
            Parameters parameters = mCamera.getParameters();
            List<Size> Sizes = parameters.getSupportedPictureSizes();
            int normalQualityLevel = Sizes.size() /2;
            parameters.setPictureSize(Sizes.get(normalQualityLevel).width,Sizes.get(normalQualityLevel).height);  // 使用中等档次的照相品质
            mCamera.setParameters(parameters);
            Size pictureSize = parameters.getPictureSize();
            LeoLog.i("poha", "照相机实际的分辨率： " + "height :" + pictureSize.height + "  width : "+ pictureSize.width);
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(CameraInfo.CAMERA_FACING_FRONT, info);
            mPt.putInt(PrefConst.KEY_ORIENTATION_OF_CAMERA_FACING_FRONT, info.orientation);
            LeoLog.i("poha", "前置照相机  orientation = "+info.orientation);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            mIsinited = true;
            if(mPendingCallback != null) {
                ThreadManager.executeOnAsyncThreadDelay(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mCamera.takePicture(null, null, mPendingCallback);
                        } catch (Throwable e) {
                            LeoLog.i("poha", "Fail to takePic  :"+e.getMessage());
                        }
                        mPendingCallback = null;
                    }
                }, 1000);
            }
        } catch (Exception e) {
        }
//        mCamera.stopPreview();
//        mCamera.startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (!mIsinited)
            return;
        try {
            mCamera.startPreview();
        } catch (Exception e) {
        }
    }
    

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        release();
    }

    public void takePicture(PictureCallback imageCallback) {
        LeoLog.i("poha", "mCanTake : " + mCanTake);
        // Not init, pending
        if(!mIsinited) {
            LeoLog.i("poha", "!mIsinited!!!!!") ;
            mPendingCallback = imageCallback;
            return;
        }
//        try {
//            LeoLog.i("poha", "real take pic!!!!!") ;
            mCamera.takePicture(null, null, imageCallback);
//        } 
//    catch (Throwable e) {
//            LeoLog.i("poha", "Fail to takePic  :"+e.getMessage());
//        }
    }

    public void release() {
        if (mCamera != null) {
            mIsinited = false;
            try {
                mCamera.stopPreview();
                mCamera.release();
                LeoLog.i("poha", "Camera release");
            } catch (Exception e) {
                
            }
            mCamera = null;
        }
        mPendingCallback = null;
    }
}
