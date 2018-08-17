package com.justcode.clanugragedemo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.andsync.xpermission.XPermissionUtils;
import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    private Bitmap bitmap;
    private Bitmap resultImg;

    public native String stringFromJNI();

    public static native int[] addText2Picture(int[] pixels_, int width, int height, String content);

    private Button btn_open;
    private Button btn_dispose;
    private Button btn_save;
    private ImageView iv;
    private static final int ALBUM_RESULT_CODE = 1;// 从相册中选择

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_open = (Button) findViewById(R.id.btn_open);
        btn_dispose = (Button) findViewById(R.id.btn_dispose);
        btn_save = (Button) findViewById(R.id.btn_save);
        iv = (ImageView) findViewById(R.id.iv);
        //tv.setText(stringFromJNI());
        btn_open.setOnClickListener(this);
        btn_dispose.setOnClickListener(this);
        btn_save.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        XPermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_open:

                XPermissionUtils.requestPermissions(this, RequestCode.EXTERNAL, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        new XPermissionUtils.OnPermissionListener() {
                            @Override
                            public void onPermissionGranted() {
                                // 激活系统图库，选择一张图片
                                Intent albumIntent = new Intent(Intent.ACTION_PICK);
                                albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                                startActivityForResult(albumIntent, ALBUM_RESULT_CODE);
                            }

                            @Override
                            public void onPermissionDenied(final String[] deniedPermissions, boolean alwaysDenied) {
                                Toast.makeText(MainActivity.this, "获取权限失败", Toast.LENGTH_SHORT).show();
                                if (alwaysDenied) { // 拒绝后不再询问 -> 提示跳转到设置
                                    DialogUtil.showPermissionManagerDialog(MainActivity.this, "存储");
                                } else {    // 拒绝 -> 提示此公告的意义，并可再次尝试获取权限
                                    new AlertDialog.Builder(MainActivity.this).setTitle("温馨提示")
                                            .setMessage("我们需要存储权限才能正常使用该功能")
                                            .setNegativeButton("取消", null)
                                            .setPositiveButton("验证权限", new DialogInterface.OnClickListener() {
                                                @RequiresApi(api = Build.VERSION_CODES.M)
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    XPermissionUtils.requestPermissionsAgain(MainActivity.this, deniedPermissions,
                                                            RequestCode.EXTERNAL);
                                                }
                                            })
                                            .show();
                                }
                            }
                        });

                break;
            case R.id.btn_dispose:

                if (bitmap != null && !bitmap.isRecycled()) {
                    final int w = bitmap.getWidth();
                    final int h = bitmap.getHeight();
                    final int[] pixels = new int[w * h];
                    bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
                    bitmap.recycle();
                    final String content = "heheh";

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int[] resultInt = addText2Picture(pixels, w, h, content);
                                if (resultInt != null) {
                                    resultImg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                                    resultImg.setPixels(resultInt, 0, w, 0, 0, w, h);
                                }
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                resultImg.compress(Bitmap.CompressFormat.PNG, 100, baos);
                                final byte[] bytes = baos.toByteArray();

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Glide.with(MainActivity.this).load(bytes).into(iv);
                                    }
                                });
                            } catch (Exception e) {
                            }
                        }
                    }).start();
                }
                break;
            case R.id.btn_save:
                if (resultImg != null && !resultImg.isRecycled()) {
                    String urlpath = FileUtilcll.saveFile(this, "new.jpg", resultImg);
                    if (urlpath != null && !urlpath.isEmpty() && urlpath != "") {
                        Toast.makeText(this, "图片保存在" + urlpath, Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(this, "保存失败", Toast.LENGTH_LONG).show();
                    }
                    resultImg.recycle();
                    Log.e("路径", "处理后的图片地址->" + urlpath);
                }
                break;
            default:
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ALBUM_RESULT_CODE) {
            // 从相册返回的数据
            if (data != null) {
                // 得到图片的全路径
                Uri uri = data.getData();
                try {
                    // 读取uri所在的图片
                    bitmap = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), uri);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            final byte[] bytes = baos.toByteArray();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Glide.with(MainActivity.this).load(bytes).into(iv);
                                }
                            });
                        }
                    }).start();
                } catch (Exception e) {
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
