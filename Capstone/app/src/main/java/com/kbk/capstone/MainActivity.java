package com.kbk.capstone;

import static android.content.ContentValues.TAG;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //WebView
    private final String VIDEO_PATH = "http://203.250.133.153:8080/stream";
    private WebView webView;
    private WebView webView_background;

    private ClassifierWithSupport cls;

    //수동자동 버튼, 객체인식시작버튼
    private SwitchMaterial switchMaterial;
    private Button btn_MLstart;

    //컨트롤 버튼
    private Button btn_fwd; //전진
    private Button btn_reverse; //후진
    private Button btn_left; //좌회전
    private Button btn_right; //우회전
    private Button btn_stop; //정지

    private ImageView imageView;
    private TextView textView;
    private TextView textView2;

    private Bitmap bitmap = null;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IdSetting();
        WebViewSetting();
        switchMaterial.setChecked(false);


        //RC카 자율주행 및 객체인식 시작버튼
        btn_MLstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackgroundThread thread = new BackgroundThread();
                thread.start();
            }
        });

        //정지버튼
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView_background.loadUrl("http://203.250.133.153:15000/stop");
            }
        });
        //전진버튼
        btn_fwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        webView_background.loadUrl("http://203.250.133.153:15000/forward");
                        btn_fwd.setBackground(getDrawable(R.drawable.baseline_north_white_24dp));
                        return true;
                    case MotionEvent.ACTION_UP:
                        webView_background.loadUrl("http://203.250.133.153:15000/stop");
                        btn_fwd.setBackground(getDrawable(R.drawable.baseline_north_black_24dpp));
                        return true;
                }
                return false;
            }
        });
        //후진버튼
        btn_reverse.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        webView_background.loadUrl("http://203.250.133.153:15000/reverse");
                        btn_reverse.setBackground(getDrawable(R.drawable.baseline_south_white_24dp));
                        return true;
                    case MotionEvent.ACTION_UP:
                        webView_background.loadUrl("http://203.250.133.153:15000/stop");
                        btn_reverse.setBackground(getDrawable(R.drawable.baseline_south_black_24dpp));
                        return true;
                }
                return false;
            }
        });
        //좌회전버튼
        btn_left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        webView_background.loadUrl("http://203.250.133.153:15000/left");
                        btn_left.setBackground(getDrawable(R.drawable.baseline_west_white_24dp));
                        return true;
                    case MotionEvent.ACTION_UP:
                        webView_background.loadUrl("http://203.250.133.153:15000/stop");
                        btn_left.setBackground(getDrawable(R.drawable.baseline_west_black_24dpp));
                        return true;
                }
                return false;
            }
        });
        //우회전버튼
        btn_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        webView_background.loadUrl("http://203.250.133.153:15000/right");
                        btn_right.setBackground(getDrawable(R.drawable.baseline_east_white_24dp));
                        return true;
                    case MotionEvent.ACTION_UP:
                        webView_background.loadUrl("http://203.250.133.153:15000/stop");
                        btn_right.setBackground(getDrawable(R.drawable.baseline_east_black_24dpp));
                        return true;
                }
                return false;
            }
        });

//---------------------------BUTTONS_END--------------------------BUTTONS_END------------------------BUTTONS_END----------------------//

        //자동,수동 전환 기능
        switchMaterial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //수동상태
                if(isChecked) {
                    webView_background.loadUrl("http://203.250.133.153:15000/auto");
                    switchMaterial.setText("현재(자동)");

                    btn_fwd.setEnabled(false);
                    btn_reverse.setEnabled(false);
                    btn_left.setEnabled(false);
                    btn_right.setEnabled(false);
                }
                //자동상태
                else{
                    webView_background.loadUrl("http://203.250.133.153:15000/autostop");
                    switchMaterial.setText("현재(수동)");
                    btn_fwd.setEnabled(true);
                    btn_reverse.setEnabled(true);
                    btn_left.setEnabled(true);
                    btn_right.setEnabled(true);
                }
            }
        });

        cls = new ClassifierWithSupport(this);
        try{
            cls.init();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


//-------------------------------------------------------Method Field----------------------------------------------------------------//

    //Ids
    private void IdSetting() {
        webView_background = (WebView) findViewById(R.id.web_background);
        webView = (WebView) findViewById(R.id.webView);

        switchMaterial = (SwitchMaterial) findViewById(R.id.switch_Manual);
        btn_fwd = (Button) findViewById(R.id.btn_fwd);
        btn_reverse = (Button) findViewById(R.id.btn_back);
        btn_left = (Button) findViewById(R.id.btn_left);
        btn_right = (Button) findViewById(R.id.btn_right);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_MLstart = (Button) findViewById(R.id.btn_MLstart);

        imageView = (ImageView) findViewById(R.id.imageView3);
        textView = (TextView) findViewById(R.id.textView4);
        textView2 = (TextView) findViewById(R.id.textView5);

    }

    //WebView
    private void WebViewSetting() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setInitialScale(1);

        webView.loadUrl(VIDEO_PATH);
    }

    //webView capture function
    public static Bitmap screenshot2(WebView webView) {
        webView.measure(View.MeasureSpec.makeMeasureSpec(
                View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        webView.layout(0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight());
        webView.setDrawingCacheEnabled(true);
        webView.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(webView.getMeasuredWidth(),
                webView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        int iHeight = bitmap.getHeight();
        canvas.drawBitmap(bitmap, 0, iHeight, paint);
        webView.draw(canvas);
        return bitmap;
    }

    //뒤로가기 시 해당 프로그램 종료
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        ActivityCompat.finishAffinity(MainActivity.this);
        System.exit(0);
    }

    class BackgroundThread extends Thread {
        int value = 0;

        public void run() {
            for (int i = 0; i < 6000; i++) {
                try {
                    Thread.sleep(100);
                } catch(Exception e) {}

                value += 1;
                Log.d("Thread", "value : " + value);

                handler.post(new Runnable() {
                    public void run() {
                        bitmap = screenshot2(webView);
                        imageView.setImageBitmap(screenshot2(webView));

                        if(bitmap != null){
                            Pair<String, Float> output = cls.classify(bitmap);
                            textView.setText("");
                            String str = output.first; //머신러닝 거쳐서 나온 출력값 텍스트
                            imageView.setImageBitmap(bitmap);
                            if (str.equals("pop bottle") || str.equals("beer bottle") || str.equals("pill bottle")
                                    || str.equals("water bottle") || str.equals("wine bottle")) {

                                //탐지 되었을때
                                webView_background.loadUrl("http://203.250.133.153:15000/autostop");
                                String resultStr =
                                        String.format(Locale.ENGLISH, "종류 : %s, 인식률 : %.2f%%", output.first, output.second * 100);
                                textView.setText(resultStr);
                            }else {

                                textView.setText("객체가 탐지 되지 않았습니다");

                            }
                        }
                        textView2.setText("value 값 : " + value);
                    }
                });
            }
        }
    }
}