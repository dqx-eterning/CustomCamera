package com.dangqx.customcamera.util;

import android.media.Image;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by dang on 2020-01-14.
 * Time will tell.
 * 保存图片的方法
 * @description
 */
public class ImageSaver implements Runnable {
    private Image mImage;
    private File mFile;
    public ImageSaver(Image image){
        this.mImage = image;
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
        //使用nio的ByteBuffer
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        //get(byte[] dst)：尝试读取 dst 目标数组长度的数据，拷贝至目标数组，
        buffer.get(bytes);
        FileOutputStream outputStream = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String fName = "IMG_" + sdf.format(new Date()) + ".jpg";
        mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(),fName);
        try{
            outputStream = new FileOutputStream(mFile);
            outputStream.write(bytes);
        }catch(IOException e){
            e.printStackTrace();
        }finally {
            if (outputStream != null){
                try{
                    outputStream.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
