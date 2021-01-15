package com.dangqx.customcamera;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dangqx.customcamera.method.setPreviewAndCapture;
import com.dangqx.customcamera.util.ImageSaver;
import com.dangqx.customcamera.util.Utils;
import com.dangqx.customcamera.view.ResizeAbleSurfaceView;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private setPreviewAndCapture setPreviewAndCapture;

    private int currentCameraId = CameraCharacteristics.LENS_FACING_FRONT;//手机后面的摄像头

    private ResizeAbleSurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Size previewSize;//图片尺寸
    private Size mWinSize;//获取屏幕的尺寸
    private ImageReader imageReader;//接受图片数据
    private float mRate = 1;//缩放倍数，默认为1

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;

    private HandlerThread handlerThread;
    private Handler handler;

    //private VideoRecorderUtils videoRecorderUtils;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWinSize = Utils.loadWinSize(this);
        //动态获取权限
        List<String> permissionList = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {
            initView();
        }

        //为按钮绑定点击事件
        Button picture = findViewById(R.id.btnTakePhoto);
        Button change = findViewById(R.id.btnSwitch);
        Button record = findViewById(R.id.record);
        Button stopRecord = findViewById(R.id.stop);
        Button zoom = findViewById(R.id.btn_zoom);
        picture.setOnClickListener(this);
        change.setOnClickListener(this);
        record.setOnClickListener(this);
        stopRecord.setOnClickListener(this);
        zoom.setOnClickListener(this);
    }

    /**
     * 请求权限回调
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "拒绝权限无法使用", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    initView();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    /**
     * 加载布局，初始化组件
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initView(){
        surfaceView = findViewById(R.id.surfaceView);
        //surfaceView.resize(1080,1080);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                //打开相机同时开启预览
                setAndOpenCamera();
                int height = surfaceView.getHeight();
                int width = surfaceView.getWidth();
                if (height > width) {
                    float justH = width * 4.f / 3;
                    //设置View在水平方向的缩放比例,保证宽高比为3:4
                    surfaceView.setScaleX(height / justH);
                } else {
                    float justW = height * 4.f / 3;
                    surfaceView.setScaleY(width / justW);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                //关闭相机释放资源
                closeCamera();
            }
        });

        //获取相机管理
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        //开启子线程，处理某些耗时操作
        handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }
    private void setAndOpenCamera(){
        //获取摄像头属性描述
        CameraCharacteristics cameraCharacteristics = null;
        try{
            //根据摄像头id获取摄像头属性类
            cameraCharacteristics = cameraManager.getCameraCharacteristics(String.valueOf(currentCameraId));
            //获取该摄像头支持输出的图片尺寸
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //根据屏幕尺寸即摄像头输出尺寸计算图片尺寸
            previewSize = Utils.fitPhotoSize(map,mWinSize);
            //初始化imageReader
            imageReader = ImageReader.newInstance(previewSize.getWidth(),previewSize.getHeight(), ImageFormat.JPEG,2);
            //设置回调处理接受图片数据
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    //发送数据进子线程处理
                    handler.post(new ImageSaver(reader.acquireNextImage()));
                }
            },handler);
            //打开相机，先检查权限
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED){
                return;
            }
            //打开摄像头
            cameraManager.openCamera(String.valueOf(currentCameraId),stateCallback,null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * 打开相机后的状态回调，获取CameraDevice对象
     */
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            //打开相机后开启预览
            setPreviewAndCapture = new setPreviewAndCapture(cameraDevice,surfaceHolder,imageReader,handler);
            setPreviewAndCapture.startPreview();

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
            finish();
        }
    };


    /**
     * 关闭相机
     */
    private void closeCamera() {
        //关闭捕捉会话
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        //关闭相机
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        //关闭拍照处理器
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    /**
     * 点击不同按钮的事件
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnTakePhoto:

                break;
        }
    }
}