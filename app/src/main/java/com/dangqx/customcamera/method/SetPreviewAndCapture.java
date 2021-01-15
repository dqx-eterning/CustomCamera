package com.dangqx.customcamera.method;

import android.app.Activity;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Arrays;

/**
 * Created by dang on 2020-01-15.
 * Time will tell.
 * 此类时预览及拍照方法的提供类
 * @description
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SetPreviewAndCapture {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();//旋转方向集合
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private float mRate = 1;//缩放倍数，默认为1
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewBuilder;
    private CaptureRequest.Builder captureBuilder;
    private SurfaceHolder surfaceHolder;
    private ImageReader imageReader;
    private Activity activity;
    private Handler handler;
    private Size previewSize;
    public SetPreviewAndCapture(CameraDevice cameraDevice, SurfaceHolder surfaceHolder,
                                ImageReader imageReader, Handler handler, Activity activity,
                                Size size) {
        this.cameraDevice = cameraDevice;
        this.surfaceHolder = surfaceHolder;
        this.imageReader = imageReader;
        this.handler = handler;
        this.activity = activity;
        this.previewSize = size;
    }

    /**
     * 开启预览的方法
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startPreview(){
        try{
            //首先需要构建预览请求
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //设置预览输出界面
            previewBuilder.addTarget(surfaceHolder.getSurface());
            //创建相机的会话Session
            cameraDevice.createCaptureSession(Arrays.asList(surfaceHolder.getSurface(),imageReader.getSurface()),stateCallback,handler);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * 创建Session的状态回调
     */
    private CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            //会话已经建立，可以开启预览了
            cameraCaptureSession = session;
            //设置自动对焦
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //发送预览请求
            try{
                cameraCaptureSession.setRepeatingRequest(previewBuilder.build(),null,handler);
            }catch(CameraAccessException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            //开启失败时，关闭会话
            session.close();
            cameraCaptureSession = null;
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    /**
     * 拍摄照片的方法
     */
    public void takePhoto(){
        try{
            //创建拍照请求，设置属性
            captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //设置拍照的画面
            captureBuilder.addTarget(imageReader.getSurface());
            //设置自动对焦
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //设置自动曝光
            //captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //设置每次拍照都打开闪光灯
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
            captureBuilder.set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_TORCH);
            //获取手机方向
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            //根据设备方向计算照片的方向
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));
            //拍照前需要停止预览
            cameraCaptureSession.stopRepeating();
            //发送拍照请求
            cameraCaptureSession.capture(captureBuilder.build(),captureCallback,handler);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * 拍照请求的回调，返回拍照的结果消息
     */
    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            try{
                //停止自动聚焦
                captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
                //重新打开预览
                cameraCaptureSession.setRepeatingRequest(previewBuilder.build(),null,null);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            cameraCaptureSession.close();
            cameraCaptureSession = null;
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    /**
     * 设置缩放的方法
     */
    public void setZoom(){
        //限制可以缩放8次。。。
        if ((mRate - 1) > 1.2) {
            mRate = 1;
        }
        try {
            CaptureRequest.Builder reqBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            reqBuilder.addTarget(surfaceHolder.getSurface());
            //这里的Rect表示一个矩形区域，由四条边的坐标组成
            reqBuilder.set(
                    CaptureRequest.SCALER_CROP_REGION,
                    new Rect(0, 0, (int) (previewSize.getWidth() / mRate), (int) (previewSize.getHeight() / mRate)));
            cameraCaptureSession.setRepeatingRequest(reqBuilder.build(), null, handler);
            mRate += 0.15;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
