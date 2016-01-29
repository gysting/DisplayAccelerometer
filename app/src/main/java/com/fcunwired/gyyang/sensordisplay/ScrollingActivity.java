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
    /* Current Accelerometer */
    float aX, aY, aZ;
    /* Average value without noise */
    float aXM = 0, aYM = 0, aZM = 0;
    /* Integrated Avlue */
    float aXI = 0, aYI = 0, aZI = 0;
    int movDirection = 0;
    float aXMPrev, aYMPrev, aZMPrev;
    float totalAcc = 0;
    long duration = 0;
    float totalMax = 0;
    float integratedMax = 0;



    /* magnetic field */
    float mx, my, mz;
    /* gyroscope */
    float gx, gy, gz;
    float accSum = 0;
    private long upDuration = 0, accCount= 0;
    private long durationMax = 0;
    private long takeOffCounter=0;
    boolean isAcceleration = true;
    boolean takeoffDetected = false;



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
        boolean currentAcceleration= false;
        long currentTimeMs = 0;
        String str;

        currentTimeMs = System.currentTimeMillis();

        final TextView scrollTextView = (TextView) findViewById(R.id.scrollTextView);

        if (isAccellerometer && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
            float x, y, z;
            float x2, y2, z2;
            float[] values = event.values;
            float accTotal, accTotalAbs;

            float aZI2, aXI2, aYI2;


            // Movement
            x = values[0];
            y = values[1];
            z = values[2];

            aXM = (float) (aXM * 0.8 + x * 0.2);
            aYM = (float) (aYM * 0.8 + y * 0.2);
            aZM = (float) (aZM * 0.8 + z * 0.2);

            scrollTextView.setText("<Accelerometer>\n");
            str = String.format("X: %.04f, Y: %.04f, Z: %.04f\n", aXM, aYM, aZM);
            scrollTextView.setText(str);

            x2 = x * x;
            y2 = y * y;
            z2 = z * z;

            // accTotal = (float)(sqrt(x2 + y2 + z2) - SensorManager.GRAVITY_EARTH);
            accTotal = (x2 + y2 + z2) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH) - 1;
            accTotalAbs = (float) (sqrt(accTotal * accTotal));

            //accelSquareRoot2 = (ax * ax + ay * ay + az * az) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
            //accelationSquareRoot = (float) (sqrt(ax * ax + ay * ay + az * az) - SensorManager.GRAVITY_EARTH);

            long actualTime = event.timestamp;

            if (accTotal > 0) {
                currentAcceleration = true;
            } else {
                currentAcceleration = false;
            }

            if ((accTotalAbs > 0.1) && (currentAcceleration == isAcceleration))
            {


                if (lastUpdate == 0) {
                    lastUpdate = System.currentTimeMillis();

                    /* Started move, Let mark current level */
                    aXMPrev = aXM;
                    aYMPrev = aYM;
                    aZMPrev = aZM;

                    isAcceleration = currentAcceleration;
                }

                /* Add all the diff values */
                aXI += (x - aXMPrev);
                aYI += (y - aYMPrev);
                aZI += (z - aZMPrev);


                accSum += accTotalAbs;
                accCount++;

                totalAcc = (accSum / accCount) * duration;

                if (totalAcc > integratedMax)
                {
                    integratedMax = totalAcc;
                }

                if (accTotalAbs > totalMax)
                {
                    totalMax = accTotalAbs;
                }

                duration = currentTimeMs - lastUpdate;

                /* Get Moved duration */
                    if (duration >= 2000) {
                        //scrollTextView.append("1 sec!!!\n");

                        if (takeoffDetected == false) {
                            takeoffDetected = true;

                        /* average value * time */

                            takeOffCounter++;
                        }
                    }



                if (duration > durationMax)
                {
                    durationMax = duration;
                }

            }
            else
            {

                if (lastUpdate != 0) {



                    /* Check moving direction */
                    aXI2 = aXI * aXI;
                    aYI2 = aYI * aYI;
                    aZI2 = aZI * aZI;

                    if (aXI2 > aYI2)
                    {
                        if (aXI2 > aZI2)
                        {
                        /* Moving X direction */
                            movDirection = 0;
                        }
                        else
                        {
                        /* Moving Z */
                            movDirection = 2;
                        }

                    }
                    else
                    {
                        if (aYI2 > aZI2)
                        {
                        /* movint Y */
                            movDirection = 1;
                        }
                        else
                        {
                        /* mvoing Z */
                            movDirection = 2;
                        }
                    }

                }

                lastUpdate = 0;
                accSum = 0;
                accCount = 0;

                aXI = 0;
                aYI = 0;
                aZI = 0;

                aXMPrev = 0;
                aYMPrev = 0;
                aZMPrev = 0;

                takeoffDetected = false;
                isAcceleration = currentAcceleration;
            }


            str = String.format("Total: %.4f, %.4f, %.4f\n", accTotal, accTotalAbs, totalMax);
            scrollTextView.append(str);


            str = String.format("Moving To: %d\n", movDirection);
            scrollTextView.append(str);


            str = String.format("Total Max: %.4f, %.4f\n", totalAcc, integratedMax);
            scrollTextView.append(str);


            str = String.format("Dur: %d , %d ms\n", duration, durationMax);
            scrollTextView.append(str);

            str = String.format("TakeOff: %d\n", takeOffCounter);
            scrollTextView.append(str);
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

        //String str = String.format("X: %.04f, Y: %.04f, Z: %.04f\n", ax, ay, az);


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
