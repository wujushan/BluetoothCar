package com.juju.bluetoothcar;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.juju.tools.MyDraw;
import com.juju.tools.MySeekBar;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements AccelerometerFragment.PositonChangedListener,
        BluetoothFragment.DataChangeListener

{

    private SlidingMenu slidingmenu;
    private BluetoothFragment bluetoothFragment;
    private AccelerometerFragment controlFragment;
    private TextView X,Y,Z;
    private  MySeekBar bar = null;
    private MyDraw ball;
    private boolean isPower =false;
    private  RelativeLayout root;
    private Button ifEnterButton,cancelControl;
    private ToggleButton toggleButton;
    private boolean isAhead = true;
    private int gears = 2; //默认初始档位是2档
    public final static int SEND_COMMAND = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        root = (RelativeLayout)findViewById(R.id.root);

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            prepareGame();
            ball = new MyDraw(this);
            root.addView(ball);
        }
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            findButton();
        findID();
        initSlidingMenu(this);

        bluetoothFragment = new BluetoothFragment(mHandler);
        controlFragment = new AccelerometerFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.search_fragment, bluetoothFragment, "search_fragment");
        transaction.add(controlFragment, "control_fragment");
        transaction.commit();

    }

    private void findButton() {
        ifEnterButton = (Button)findViewById(R.id.ifEnter);
        ifEnterButton.setOnClickListener(onClickListener);
    }

    /**
     * 游戏模式初始化
     */
    private void prepareGame() {
        bar = (MySeekBar)findViewById(R.id.myseekbar);
        bar.setOnSeekBarChangeListener(onSeekBarChangedListener);
        toggleButton = (ToggleButton)findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(onCheckedChangeListener);
        cancelControl = (Button)findViewById(R.id.cancelControl);
        cancelControl.setOnClickListener(onClickListener);
        getActionBar().hide();
    }

    private void findID() {
        X =(TextView)findViewById(R.id.x);
        Y = (TextView)findViewById(R.id.y);
        Z = (TextView)findViewById(R.id.z);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * 初始化slidingmenu
     * @param mainActivity
     */
    private void initSlidingMenu(MainActivity mainActivity) {
        slidingmenu = new SlidingMenu(mainActivity);
        slidingmenu.setMode(SlidingMenu.LEFT);
        slidingmenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        slidingmenu.setBehindOffsetRes(R.dimen.SlidingMenu_margin);
        slidingmenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        slidingmenu.setMenu(R.layout.devicesearchlayout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public SlidingMenu getObject(){
        return  slidingmenu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //noinspection SimplifiableIfStatement
        switch (item.getItemId()){

            case R.id.openBluetooth:
                bluetoothFragment.openBt();
                break;

            case R.id.disconnectDevice:
                bluetoothFragment.disconnectDevice();
                break;

            case R.id.searchDevices:

                boolean isOpen = slidingmenu.isMenuShowing();
                bluetoothFragment.searchBt();
                BluetoothFragment fm = (BluetoothFragment)getFragmentManager().findFragmentByTag("search_fragment");
                if(fm.isOpenCheck()){
                    if (!isOpen)
                        slidingmenu.toggle(true);
                    bluetoothFragment.searchBt();
                }
                else
                    Toast.makeText(this,"请确认蓝牙是否开启",Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * seekbar的监听
     */
    private MySeekBar.OnSeekBarChangeListener onSeekBarChangedListener = new MySeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(MySeekBar VerticalSeekBar, final int progress, boolean fromUser) {
            X.setText(String.valueOf(progress));

            if (progress > 85){
//              如果加速器达到85或以上,判断小车是否已经启动
//              如果还没启动,执行启动动作
                if (!isPower){
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mHandler.obtainMessage(SEND_COMMAND, progress,-1).sendToTarget();
                        }
                    },0,2000);
                    isPower = true;
                }
                else {

                }
            }
        }

        @Override
        public void onStartTrackingTouch(MySeekBar VerticalSeekBar) {

        }

        @Override
        public void onStopTrackingTouch(MySeekBar VerticalSeekBar) {

        }
    };

    OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if(isChecked){
//                发送前进指令
                isAhead = true;
                ball.setDirection(0);// 小车向前
            }else {
//                发送后退指令
                isAhead = false;
                ball.setDirection(1);//小车向后
            }
        }
    };

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch(view.getId()){
                case R.id.ifEnter:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case R.id.cancelControl:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }
    };
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what){
//               间隔性向小车发送状态指令
                case SEND_COMMAND:
                    int accelerator = msg.arg1; //获取加速值
                    bluetoothFragment.sendCommand("zhiling");
                    X.setText("receive the message");
                    break;
            }
        }
    };


    @Override
    public void postionChanged(float x, float y, float z) {
        X.setText("X: " + String.valueOf(x));
        Y.setText("Y: " + String.valueOf(y));
        Z.setText("Z: " + String.valueOf(z));
        if (toggleButton != null){
            if (x < 5.5){
                toggleButton.setChecked(true); //x轴小于5.5,将方向置为向前
            }else if (x > 7.5){
                toggleButton.setChecked(false);//x轴大于7.5,将方向置为向后
            }
        }

        //如果x小于0,需要调整y的方向
        if (x < 0 ){
            y = -(int)y;
        }
        if (ball != null)
            ball.deliverXY(x, y);
    }

    @Override
    public void dataChange() {

    }
}
