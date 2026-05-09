package com.zero.study.ui.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

import com.zero.base.activity.BaseActivity;
import com.zero.study.databinding.ActivityRxBinding;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Admin
 */
public class RxActivity extends BaseActivity<ActivityRxBinding> {
    public static final String DIR_NAME = "ZERO";
    protected final String TAG = RxActivity.class.getSimpleName();

    public RxActivity() {
        super(ActivityRxBinding::inflate);
    }

    @Override
    public void initView() {
        try {
            InputStream inputStream = getAssets().open("wide.jpg");
            binding.ivLargePic.setInputStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initData() {
    }

    @Override
    public void addListener() {
    }

    private File saveBitmap(Bitmap bitmap) throws IOException {
        File file = new File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            System.currentTimeMillis() + ".jpg"
        );
        Log.i(TAG, "save file to disk:" + file.getAbsolutePath());
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        bos.flush();
        bos.close();
        return file;
    }

    public Bitmap compressImage(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
        byte[] byteArray = stream.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    private Bitmap addTextWatermarkToBitmap(Bitmap originalBitmap, String waterMarkText) {
        Bitmap bitmapWithWatermark = originalBitmap.copy(originalBitmap.getConfig(), true);
        Canvas canvas = new Canvas(bitmapWithWatermark);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(80);
        paint.setAntiAlias(true);
        paint.setShadowLayer(1f, 0f, 1f, Color.BLACK);
        int xPos = originalBitmap.getWidth() / 2;
        int yPos = originalBitmap.getHeight() - 50;
        canvas.drawText(waterMarkText, xPos, yPos, paint);
        return bitmapWithWatermark;
    }
}
