package cn.edu.swufe.foodmap;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.percent.PercentRelativeLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements Runnable {
    private Spinner districtL, typeL;//下拉菜单
    private List<SpinnerData> data_list;
    private ArrayAdapter<SpinnerData> data_adapter;//spinner适配器
    Handler handler,random_handler;
    Message msg;

    @Bind(R.id.main_fl_container) PercentRelativeLayout mFlContainer;
    @Bind(R.id.main_fl_card_back) FrameLayout mFlCardBack;
    @Bind(R.id.main_fl_card_front) FrameLayout mFlCardFront;

    private AnimatorSet mRightOutSet; // 右出动画
    private AnimatorSet mLeftInSet; // 左入动画
    private boolean mIsShowBack;

    TextView name,contact,address;
    String d_id,t_id;
    String random;

    private SensorManager sensorManager;//感应器
    private Vibrator vibrator;
    private static final int SENSOR_SHAKE = 10;
    private static final int UPTATE_INTERVAL_TIME = 1000;
    private long lastUpdateTime;

    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        districtL = (Spinner) findViewById(R.id.districtList);
        typeL = (Spinner) findViewById(R.id.typeList);
        name = (TextView)findViewById(R.id.store_name);
        contact = (TextView)findViewById(R.id.store_contact);
        address = (TextView)findViewById(R.id.store_address);

        image = (ImageView)findViewById(R.id.storeimg);

        Thread t = new Thread(this);
        t.start();

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Log.i("handler","handleMessage...");
                if(msg.what == 1){
                    Bundle retBdl = (Bundle) msg.obj;
                    if(retBdl.getString("district")!= null){
                        String district = retBdl.getString("district");
                        try {
                            JSONObject disJson = new JSONObject(district);
                            JSONArray disArray = disJson.getJSONArray("district");
                            data_list = new ArrayList<SpinnerData>();
                            SpinnerData c1 = new SpinnerData("0", "全部");
                            data_list.add(c1);
                            for(int i=0;i<disArray.length();i++) {
                                JSONObject tempJson = disArray.optJSONObject(i);
                                SpinnerData c = new SpinnerData(tempJson.getString("id"), tempJson.getString("name"));
                                data_list.add(c);
                            }
                            data_adapter = new ArrayAdapter<SpinnerData>(MainActivity.this,android.R.layout.simple_spinner_item,data_list);
                            data_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            districtL.setAdapter(data_adapter);
                        } catch (JSONException e) {
                            System.out.println("Something wrong...");
                            e.printStackTrace();
                        }
                    }
                    if(retBdl.getString("type")!= null){
                        String type = retBdl.getString("type");
                        try {
                            JSONObject typeJson = new JSONObject(type);
                            JSONArray typeArray = typeJson.getJSONArray("type");
                            data_list = new ArrayList<SpinnerData>();
                            SpinnerData c2 = new SpinnerData("0", "全部");
                            data_list.add(c2);
                            for(int i=0;i<typeArray.length();i++) {
                                JSONObject tempJson = typeArray.optJSONObject(i);
                                SpinnerData c = new SpinnerData(tempJson.getString("id"), tempJson.getString("name"));
                                data_list.add(c);
                            }
                            data_adapter = new ArrayAdapter<SpinnerData>(MainActivity.this,android.R.layout.simple_spinner_item,data_list);
                            data_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            typeL.setAdapter(data_adapter);
                        } catch (JSONException e) {
                            System.out.println("Something wrong...");
                            e.printStackTrace();
                        }
                    }
                }
                super.handleMessage(msg);
            }
        };

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        ButterKnife.bind(this);
        setAnimators(); // 设置动画
        setCameraDistance(); // 设置镜头距离
    }

    public void run(){
        Bundle bdl = new Bundle();
        Log.i("run","run thread");

        try {
            String district = HttpService.getHtml("http://192.168.123.41:9090/FoodMap/district/1");//模拟器访问本机tomcat服务需要使用android内置的ip 10.0.2.2来访问
            bdl.putString("district",district);
//            Log.i("dis",district);
            String type = HttpService.getHtml("http://192.168.123.41:9090/FoodMap/type/1");
            bdl.putString("type",type);
        }catch (MalformedURLException e){
            Log.e("www", e.toString());
            e.printStackTrace();
        }catch (Exception e) {
            Log.e("www", e.toString());
            e.printStackTrace();
        }
        msg = handler.obtainMessage();
        msg.what= 1;
        msg.obj = bdl;
        handler.sendMessage(msg);
        Log.i("thread","sendMessage...");
    }

    /*spinnerID为R.id.xxx*/
    //取得value
    public String getSpinnerSelVal(Integer spinnerID){
        Spinner sp = (Spinner)findViewById(spinnerID);
        return ((SpinnerData)sp.getSelectedItem()).getValue();
    }
    //取得text
    public String getSpinnerSelName(Integer spinnerID){
        Spinner sp = (Spinner)findViewById(spinnerID);
        return ((SpinnerData)sp.getSelectedItem()).getText();
    }

    //初始化右出(RightOut)和左入(LeftIn)动画, 使用动画集合AnimatorSet.
    //当右出动画开始时, 点击事件无效, 当左入动画结束时, 点击事件恢复.
    private void setAnimators() {
        mRightOutSet = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.anim_out);
        mLeftInSet = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.anim_in);

        // 设置点击事件
        mRightOutSet.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mFlContainer.setClickable(false);
            }
        });
        mLeftInSet.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mFlContainer.setClickable(true);
            }
        });
    }

    private void setCameraDistance() {
        int distance = 16000;
        float scale = getResources().getDisplayMetrics().density * distance;
        mFlCardFront.setCameraDistance(scale);
        mFlCardBack.setCameraDistance(scale);
    }

    // 翻转卡片
    public void flipCard(View view) {
        // 正面朝上
        if (!mIsShowBack) {
            mRightOutSet.setTarget(mFlCardFront);
            mLeftInSet.setTarget(mFlCardBack);
            mRightOutSet.start();
            mLeftInSet.start();
            mIsShowBack = true;
            d_id = getSpinnerSelVal(R.id.districtList);
            t_id = getSpinnerSelVal(R.id.typeList);
            random = "";
            new Thread(networkTask).start();

            random_handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    Bundle data = msg.getData();
                    random = data.getString("random");
                    if(!random.equals("")){
                        try {
                            JSONObject randomJson = new JSONObject(random);
                            JSONObject storeJson = randomJson.getJSONObject("store");
                            name.setText(storeJson.getString("name"));
                            contact.setText("联系方式：" + storeJson.getString("contact"));
                            address.setText("地址：" + storeJson.getString("address"));
                            JSONArray pics = randomJson.getJSONArray("pics");
                            String[] picadd = new String[pics.length()];
                            for (int i=0;i<pics.length();i++){
                                picadd[i] = pics.optJSONObject(i).getString("pic");
                            }
//                            Bitmap bitmap = HttpService.getPic("http://192.168.123.41:9090/FoodMap/"+picadd[0]);
//                            image.setImageBitmap(bitmap);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        } else { // 背面朝上
            mRightOutSet.setTarget(mFlCardBack);
            mLeftInSet.setTarget(mFlCardFront);
            mRightOutSet.start();
            mLeftInSet.start();
            mIsShowBack = false;
        }
    }

    Runnable networkTask = new Runnable() {

        @Override
        public void run() {
            // TODO
            try {
                random = HttpService.getHtml("http://192.168.123.41:9090/FoodMap/random/" + d_id + "/" + t_id);//模拟器访问本机tomcat服务需要使用android内置的ip 10.0.2.2来访问
                Log.i("run",random);
            }catch (MalformedURLException e){
                Log.e("www", e.toString());
                e.printStackTrace();
            }catch (Exception e) {
                Log.e("www", e.toString());
                e.printStackTrace();
            }
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("random", random);
            msg.setData(data);
            random_handler.sendMessage(msg);
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    //交互时启动
    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            long currentUpdateTime = System.currentTimeMillis();
            long timeInterval = currentUpdateTime - lastUpdateTime;
            if (timeInterval < UPTATE_INTERVAL_TIME) {
                return;
            }
            lastUpdateTime = currentUpdateTime;
            float[] values = event.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];
            int medumValue = 19;
            if (Math.abs(x) > medumValue || Math.abs(y) > medumValue || Math.abs(z) > medumValue) {
                Log.i("isback","" + mIsShowBack);
                flipCard(mFlContainer);
                //vibrator.vibrate(200);
                Message msg = new Message();
                msg.what = SENSOR_SHAKE;
                shake_handler.sendMessage(msg);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    Handler shake_handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SENSOR_SHAKE:
                    Toast.makeText(MainActivity.this, "biubiubiu", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

    };
}
