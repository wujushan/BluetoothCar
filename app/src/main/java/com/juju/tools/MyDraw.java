package com.juju.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.AttributeSet;
import android.view.View;


/**
 * Created by juju on 2015/6/8.
 */
public class MyDraw extends View{

    public float currentX = 640;
    public float currentY = 360;//小球初始位置,屏幕中间
    private int direction;
    Paint paint = new Paint();


    public MyDraw(Context context) {
        super(context);
        paint.setTextSize(150);
    }
    public MyDraw(Context context, AttributeSet set) {
        super(context, set);
    }
    public void deliverXY(float x,float y){
        if (x >=0)
            currentX = 500+(float) (y * 38.33);
        else
            currentX = 500-(float) (y * 38.33);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.GRAY);
//        canvas.drawCircle(currentX, currentY, 90, paint);
        canvas.drawText(direction == 0?"前进":"后退",currentX,currentY,paint);
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

}

