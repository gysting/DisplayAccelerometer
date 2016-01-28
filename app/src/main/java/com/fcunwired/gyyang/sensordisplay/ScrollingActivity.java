package com.fcunwired.gyyang.sensordisplay;

//import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import static java.lang.Math.sqrt;


public class ScrollingActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private boolean color = false;
    private View view;
    private long lastUpdate;
    private boolean isMagnetosensor;
    private boolean isAccellerometer;
    private boolean isGiroScope;
    private Sensor mAccelerometer;
    float accelationSquareRoot = 0, magSquareRoot = 0, gyroSquareRoot = 0;
    float accelSquareRoot2 = 0;
    /* accelerometer */
    float ax, ay, az;
    /* magnetic field */
    float mx, my, mz;
    /* gyroscope */
    float gx, gy, gz;
    float accSum = 0;
    private long upDuration = 0, accCount= 0;
    private long durationMax = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        TextView scrollTextView = (TextView) findViewById(R.id.scrollTextView);
        scrollTextView.setMovementMethod(new ScrollingMovementMethod());
        scrollTextView.setVerticalScrollBarEnabled(true);
        scrollTextView.setMaxLines(30);
        scrollTextView.setText("");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            isMagnetosensor = true;
        }

        if ((mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) != null) {
            isAccellerometer = true;
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            isGiroScope = true;
        }

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_UI);


        lastUpdate = 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void onSensorChanged(SensorEvent event) {

        getAccelerometer(event);

    }

    private void getAccelerometer(SensorEvent event) {
        boolean isMagnetometerCalibrated = false;
        boolean isGyroscopeCalibrated = false;


        final TextView scrollTextView = (TextView) findViewById(R.id.scrollTextView);

        if (isAccellerometer && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
            float[] values = event.values;
            // Movement
            ax = values[0];
            ay = values[1];
            az = values[2];


            accelSquareRoot2 = (ax * ax + ay * ay + az * az) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);


            accelationSquareRoot = (float) (sqrt(ax * ax + ay * ay + az * az) - SensorManager.GRAVITY_EARTH);

            long actualTime = event.timestamp;

        }
        if (isMagnetosensor && (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD))
        {
            float[] values = event.values;
            mx = values[0];
            my = values[1];
            mz = values[2];

            magSquareRoot = (float) (sqrt(mx *mx + my * my + mz * mz));

        }
        if (isGiroScope && (event.sensor.getType() == Sensor.TYPE_GYROSCOPE))
        {
            float[] values = event.values;
            gx = values[0];
            gy = values[1];
            gz = values[2];

            gyroSquareRoot = (float) (sqrt(gx * gx + gy * gy + gz * gz));
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)
        {
            isMagnetometerCalibrated = false;
        }
        if ((event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED))
        {
            isGyroscopeCalibrated = false;
        }

        String str = String.format("X: %.04f, Y: %.04f, Z: %.04f\n", ax, ay, az);

        if (accelationSquareRoot > 0.1)
        {
            scrollTextView.setText("<Accelerometer>\n");
            scrollTextView.append(str);
            str = String.format("SQRT: %04f, %04f\n", accelationSquareRoot, accelSquareRoot2);
            scrollTextView.append(str);
            if (lastUpdate == 0) {
                lastUpdate = System.currentTimeMillis();
            }
            accSum += accelationSquareRoot;
            accCount++;
        }
        else
        {
            if (lastUpdate != 0) {
                long currentTimeMs = System.currentTimeMillis();
                long duration;

                duration = currentTimeMs -lastUpdate;

                if (duration >= 1000)
                {
                    scrollTextView.append("1 sec!!!\n");
                    /* average value * time */
                    float totalAcc = (accSum / accCount) * duration;

                    str = String.format("TOTAL: %.04f\n", totalAcc / 1000);
                    scrollTextView.append(str);
                }
                else
                {
                    if (duration > durationMax)
                    {
                        durationMax = duration;
                    }
                    str = String.format("Dur: %d , %d ms\n", duration, durationMax);
                    scrollTextView.append(str);
                }

                lastUpdate = 0;
                accSum = 0;
                accCount = 0;


            }

        }

        /*
        scrollTextView.append("<Magnetic  Sensor>\n");
        str = String.format("X: %04f, Y: %04f, Z: %04f\n", mx, my, mz);
        scrollTextView.append(str);
        str = String.format("SQRT: %04f\n", magSquareRoot);
        scrollTextView.append(str);

        scrollTextView.append("<Gyro Sensor>\n");
        str = String.format("X: %04f, Y: %04f, Z: %04f\n", gx, gy, gz);
        scrollTextView.append(str);
        str = String.format("SQRT: %04f\n", gyroSquareRoot);
        scrollTextView.append(str);
*/
        int scrollCount = scrollTextView.getLineCount();
        int nHeight = scrollTextView.getLineHeight();
        if (scrollCount > 20)
        {
            scrollTextView.scrollBy(0, scrollTextView.getLineHeight());
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
