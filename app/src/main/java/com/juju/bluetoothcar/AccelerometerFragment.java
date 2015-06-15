package com.juju.bluetoothcar;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by juju on 2015/6/8.
 */
public class AccelerometerFragment extends Fragment{



    public interface PositonChangedListener{
        public void postionChanged(float x,float y,float z);
    }
    private SensorManager sensorManager;
    private PositonChangedListener positonChangedListener;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);

    }

    @Override
    public void onResume() {
        super.onResume();

    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{positonChangedListener = (PositonChangedListener)activity;}
        catch (ClassCastException e){
            throw new ClassCastException(activity.toString()+"must implement PositionChangedListener");
        }
    }

    private  final SensorEventListener sensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {

            float x =event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            positonChangedListener.postionChanged(x,y,z);
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
}
