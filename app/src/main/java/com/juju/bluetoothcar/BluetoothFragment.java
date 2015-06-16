package com.juju.bluetoothcar;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


/**
 * Created by juju on 2015/6/8.
 */
public class BluetoothFragment extends Fragment {

    public interface DataChangeListener{
        public void dataChange();
    }
    private DataChangeListener dataChangeListener;
    private  Handler mHandler;
    private View currentView;
    private static final String TAG = "BluetoothCar";
    BluetoothDevice btDevice;
    BluetoothSocket btSocket;
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//SPP服务号UUID
    private static final int RESQUEST_ENABLE = 0x1;
    private ArrayAdapter<String> mPariedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicsArrayAdapter;

    private  Context context;
    private boolean hasConnected= false;
    private ReceiverThread receiverThread;
//    private OutputStream os;
    private InputStream is;

    public final static int BL_SOCKET_FAILED = 4;
    public final static int RECEIVE_MESSAGE = 5;
    public BluetoothFragment(Handler mHandler){
        this.mHandler = mHandler;
    }
    public BluetoothFragment(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        currentView = inflater.inflate(R.layout.devicesearchlayout,container,false);
        mPariedDevicesArrayAdapter = new ArrayAdapter<String>(this.getActivity(), R.layout.devicename);
        mNewDevicsArrayAdapter = new ArrayAdapter<String>(this.getActivity(), R.layout.devicename);


        ListView pariedListView = (ListView)currentView.findViewById(R.id.paried_devices_list);
        pariedListView.setAdapter(mPariedDevicesArrayAdapter);
        pariedListView.setOnItemClickListener(mDevicClickListener);

        ListView newDevicesListView = (ListView) currentView.findViewById(R.id.new_devices_list);
        newDevicesListView.setAdapter(mNewDevicsArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDevicClickListener);


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.getActivity().registerReceiver(mReceiver,filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.getActivity().registerReceiver(mReceiver, filter);
        return currentView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{dataChangeListener = (DataChangeListener)activity;}
        catch (ClassCastException e){
            throw new ClassCastException(activity.toString()+"must implement PositionChangedListener");
        }
    }
    public void openBt(){
        Intent enableIn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIn,RESQUEST_ENABLE);
    }

    public void disconnectDevice(){
        try {
            btSocket.close();
            btSocket = null;
        }catch (Exception e){}
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    public void searchBt(){
        currentView.findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
        mNewDevicsArrayAdapter.clear();
        mPariedDevicesArrayAdapter.clear();
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }
    /**
     * 连接蓝牙设备
     * @param btAddress
     */
    private void connectBt(String btAddress) {


        //得到蓝牙设备句柄
        btDevice = bluetoothAdapter.getRemoteDevice(btAddress);
        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {}
        new Thread(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.cancelDiscovery();
                try {
                    btSocket.connect();
                } catch (IOException e) {
                    Log.e(TAG,"Connection Failed--2");
                    try {
                        btSocket.close();
                    } catch (IOException e1) {
                        Log.e(TAG,"unable to close() socket during connection failure");
                    }
                    return;
                }
                synchronized (BluetoothFragment.this){
                    hasConnected = true;
                }

                receiverThread = new ReceiverThread();
                receiverThread.start();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (hasConnected == true) {

                            ((MainActivity) getActivity()).getObject().toggle(false);//close the slidingmenu
//                            蓝牙连接成功,将游戏模式请求按钮可视化
                            getActivity().findViewById(R.id.enterLayout).setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 设备连接监听
     */
    private AdapterView.OnItemClickListener mDevicClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //准备连接设备，关闭服务查找
            bluetoothAdapter.cancelDiscovery();
            //得到mac地址
            String info = ((TextView) view).getText().toString();
            String btAddress = info.substring(info.length() - 17);
            connectBt(btAddress);
        }
    };
    /**
     *查找设备和搜索完成action监听器
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    mNewDevicsArrayAdapter.add(device.getName()+"\n"+device.getAddress());
                }else {
                    mPariedDevicesArrayAdapter.add(device.getName()+"\n"+device.getAddress());
                }
            }
        }
    };

    /**
     * 向远端蓝牙发送指令
     * @param command
     */
    public void sendCommand(String command){
        byte[]commandBuffer = command.getBytes();
        try {
            OutputStream os = btSocket.getOutputStream();
            os.write(commandBuffer);
        }catch (IOException e) {}
    }

    /**
     * 接收远端蓝牙传回的信息
     */
    private class ReceiverThread extends Thread{
        private InputStream btInputStream = null;

        public ReceiverThread() {
            InputStream tmpIn = null;

            try {
                tmpIn = btSocket.getInputStream();
            } catch (IOException e) { }

            btInputStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;
            while(true){
                try {
                    bytes = btInputStream.read(buffer);
                    byte[]flash = buffer ;
                    mHandler.obtainMessage(RECEIVE_MESSAGE,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
    }

    public BluetoothSocket getBtSocket(){
        return btSocket;
    }

    public boolean isOpenCheck() {

        return bluetoothAdapter.isEnabled();
    }
}
