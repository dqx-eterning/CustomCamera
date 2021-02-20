package com.dangqx.customcamera.util;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    private Context context;
    public ImageSaver(Image image,Context context){
        this.mImage = image;
        this.context = context;
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
        //使用nio的ByteBuffer
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        //get(byte[] dst)：尝试读取 dst 目标数组长度的数据，拷贝至目标数组，
        buffer.get(bytes);
        /*FileOutputStream outputStream = null;
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
        }*/
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String fName = "IMG_" + sdf.format(new Date()) + ".jpg";
        saveImage2Public(context,fName,bytes,"Pictures");
    }

    public static void saveImage2Public(Context context, String fileName, byte[] image, String subDir) {
        String subDirection;
        if (!TextUtils.isEmpty(subDir)) {
            if (subDir.endsWith("/")) {
                subDirection = subDir.substring(0, subDir.length() - 1);
            } else {
                subDirection = subDir;
            }
        } else {
            subDirection = "DCIM";
        }

        Cursor cursor = searchImageFromPublic(context, subDir, fileName);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));  // uri的id，用于获取图片
                Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                if (uri != null) {
                    OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        outputStream.write(image);
                        outputStream.flush();
                        outputStream.close();
                    }
                }
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            //设置保存参数到ContentValues中
            ContentValues contentValues = new ContentValues();
            //设置文件名
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            //兼容Android Q和以下版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //android Q中不再使用DATA字段，而用RELATIVE_PATH代替
                //RELATIVE_PATH是相对路径不是绝对路径
                //关于系统文件夹可以到系统自带的文件管理器中查看，不可以写没存在的名字
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, subDirection);
                //contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Music/sample");
            } else {
                contentValues.put(MediaStore.Images.Media.DATA, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath());
            }
            //设置文件类型
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/JPEG");
            //执行insert操作，向系统文件夹中添加文件
            //EXTERNAL_CONTENT_URI代表外部存储器，该值不变
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                //若生成了uri，则表示该文件添加成功
                //使用流将内容写入该uri中即可
                OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(image);
                    outputStream.flush();
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static Cursor searchImageFromPublic(Context context, String filePath, String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            Log.e("1111", "searchImageFromPublic: fileName is null");
            return null;
        }
        if (TextUtils.isEmpty(filePath)) {
            filePath = "DCIM/";
        } else {
            if (!filePath.endsWith("/")) {
                filePath = filePath + "/";
            }
        }

        //兼容androidQ和以下版本
        String queryPathKey = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q ? MediaStore.Images.Media.RELATIVE_PATH : MediaStore.Images.Media.DATA;
        String selection = queryPathKey + "=? and " + MediaStore.Images.Media.DISPLAY_NAME + "=?";
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID, queryPathKey, MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.DISPLAY_NAME},
                selection,
                new String[]{filePath, fileName},
                null);

        return cursor;
    }
}
