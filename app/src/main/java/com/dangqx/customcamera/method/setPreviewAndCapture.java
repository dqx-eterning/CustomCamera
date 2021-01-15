package com.dangqx.customcamera.method;

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
 * Created by dang on 2020-01-15.
 * Time will tell.
 * 此类时预览及拍照方法的提供类
 * @description
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class setPreviewAndCapture {
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewBuilder;
    private CaptureRequest.Builder captureBuilder;
    private SurfaceHolder surfaceHolder;
    private ImageReader imageReader;
    private Handler handler;
    public setPreviewAndCapture(CameraDevice cameraDevice,SurfaceHolder surfaceHolder,
                                ImageReader imageReader,Handler handler) {
        this.cameraDevice = cameraDevice;
        this.surfaceHolder = surfaceHolder;
        this.imageReader = imageReader;
        this.handler = handler;
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
}
