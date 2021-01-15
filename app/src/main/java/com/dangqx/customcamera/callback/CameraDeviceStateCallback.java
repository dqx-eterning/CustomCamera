package com.dangqx.customcamera.callback;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Arrays;

/**
 * Created by dang on 2020-01-14.
 * Time will tell.
 * 打开摄像头的回调
 * @description
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraDeviceStateCallback extends CameraDevice.StateCallback {

    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private Activity mActivity;

    private SurfaceHolder surfaceHolder;
    private ImageReader imageReader;
    private Handler handler;

    //构造方法，接受参数
    public CameraDeviceStateCallback(Activity activity, SurfaceHolder surfaceHolder, ImageReader imageReader,Handler handler) {
        this.mActivity = activity;
        this.surfaceHolder = surfaceHolder;
        this.imageReader = imageReader;
        this.handler = handler;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        //获得CameraDevice对象
        cameraDevice = camera;
        //开始预览
        startPreview();
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
        camera.close();
        cameraDevice = null;
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
        camera.close();
        cameraDevice = null;
        mActivity.finish();
    }
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void startPreview(){
        try{
            //构建预览请求
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //设置预览输出的界面
            previewBuilder.addTarget(surfaceHolder.getSurface());
            //创建相机的会话Session
            cameraDevice.createCaptureSession(Arrays.asList(surfaceHolder.getSurface(),imageReader.getSurface()),stateCallback,handler);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }

    }

    /**
     * CameraCaptureSession的状态回调
     */
    private CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            //会话已经建立，可以发送请求了
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
            //关闭会话
            session.close();
            cameraCaptureSession = null;
            cameraDevice.close();
            cameraDevice = null;
        }
    };
}
