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
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.util.List;

import static java.lang.Math.sqrt;



public class ScrollingActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private long lastUpdateX, lastUpdateY, lastUpdateZ;
    private boolean isMagnetosensor;
    private boolean isAccellerometer;
    private boolean isGiroScope;

    /* accelerometer */
    /* Current Accelerometer */
    private Sensor mAccelerometer;
    float aX, aY, aZ;
    /* Average value without noise */
    float aXM = 0, aYM = 0, aZM = 0;
    /* Integrated Avlue */
    float aXI = 0, aYI = 0, aZI = 0;
    int movDirection = 0;
    float aXMPrev, aYMPrev, aZMPrev;
    float totalAccIntegrated = 0;
    float accTotalPrev = 0;
    long durationY = 0, durationX = 0, durationZ;
    float totalMax = 0;
    float integratedMax = 0;
    float accSum = 0;
    private long accCount= 0;
    private long durationMax = 0;
    private long takeOffCounter=0;
    boolean isAcceleration = true;
    boolean takeoffDetected = false;
    float checkDuration=7000;
    double checkAcc=0.1;
    float compensation  = 0;
    float devXPrev, devYPrev, devZPrev;
    float[] xBuf = new float[100];
    float[] yBuf = new float[100];
    float[] zBuf = new float[100];
    int bufIndex = 0;
    int xDataLength = 0;
    private long period = 0;
    private long preMs = 0, preSec;
    long count;




    /* magnetic field */
    float magSquareRoot;
    float mx, my, mz;

    /* gyroscope */
    float gyroSquareRoot;
    float gx, gy, gz;

    long startTimeMs = System.currentTimeMillis();

    LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();
    LineGraphSeries<DataPoint> seriesY = new LineGraphSeries<DataPoint>();
    LineGraphSeries<DataPoint> seriesX = new LineGraphSeries<DataPoint>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final TextView checkTextView = (TextView)findViewById(R.id.checkTextView);
        checkTextView.setText("current: " + checkDuration + ",  " + checkAcc);
        final EditText durationTextBox = (EditText)findViewById(R.id.durationTextBox);
        final EditText accTextBox = (EditText)findViewById(R.id.accTextBox);



        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
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

        GraphView graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(0,0)});
        seriesY = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(0,0)});
        seriesX = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(0,0)});
        seriesY.setColor(Color.RED);
        seriesX.setColor(Color.BLACK);

        graph.addSeries(series);
        graph.addSeries(seriesY);
        graph.addSeries(seriesX);


        graph.setKeepScreenOn(true);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(4000);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-2);
        graph.getViewport().setMaxY(2);


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



        lastUpdateX = 0;
        lastUpdateY = 0;
        lastUpdateZ = 0;

        final Button checkButton = (Button)findViewById(R.id.CheckButton);
        Button button = (Button)findViewById(R.id.button);

        button.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        totalAccIntegrated=0;
                        totalMax=0;
                        integratedMax = 0;
                        accSum = 0;
                        accCount= 0;
                        durationMax = 0;
                        takeOffCounter=0;
                        accTotalPrev = 0;
                    }
                }
        );

        checkButton.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        if ((accTextBox.getText().length() != 0) && (durationTextBox.getText().length() != 0))
                        {
                            checkAcc = Double.valueOf(accTextBox.getText().toString());
                            checkDuration = Float.valueOf(durationTextBox.getText().toString());
                            durationTextBox.setText("");
                            accTextBox.setText("");

                        }
                        checkTextView.setText("current: " + checkDuration + ",   " + checkAcc);
                    }
                }
        );
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
        boolean currentAcceleration = false;
        long currentTimeMs = 0;
        long freq= 0;

        String str;

        currentTimeMs = System.currentTimeMillis();

        final TextView scrollTextView = (TextView) findViewById(R.id.scrollTextView);

        if (isAccellerometer && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
            float x, y, z;
            float x2, y2, z2;
            float[] values = event.values;
            float accTotal, accTotalAbs;
            float aZI2, aXI2, aYI2;
            float devX, devY, devZ;
            final int bufLenth = 20;
            final boolean enableCompensation = false;


            // Movement
            x = values[0];
            y = values[1];
            z = values[2];

            if (preMs == 0) {
                preMs = currentTimeMs;
                count = 0;
            }
            else
            {
                period =(currentTimeMs - preMs);

                if ((period) < 50)
                {
                    /* 20 Hz */
                    return;
                }

                preMs = currentTimeMs;

            }


            /*
            aXM = (float) (aXM + x)/2;
            aYM = (float) (aYM + y)/2;
            aZM = (float) (aZM + z)/2;
            */

            xBuf[bufIndex] = x;
            yBuf[bufIndex] = y;
            zBuf[bufIndex] = z;
            bufIndex++;
            if (bufIndex >= bufLenth)
            {
                bufIndex = 0;
            }
            aXM = 0;
            aYM = 0;
            aZM = 0;

            for (int i = 0; i < bufLenth; i++)
            {
                aXM += xBuf[i];
                aYM += yBuf[i];
                aZM += zBuf[i];
            }

            aXM = aXM / bufLenth;
            aYM = aYM / bufLenth;
            aZM = aZM / bufLenth;



            devX = aXMPrev - aXM;
            devY = aYMPrev - aYM;
            devZ = aZMPrev - aZM;


            str = String.format("Accel, %d Hz\n", count);
            scrollTextView.setText(str);
            str = String.format("X: %.04f, Y: %.04f, Z: %.04f\n", aXM, aYM, aZM);
            scrollTextView.append(str);

            x2 = aXM * aXM;
            y2 = aYM * aYM;
            z2 = aZM * aZM;

            accTotal = (float) (sqrt(x2 + y2 + z2) - SensorManager.GRAVITY_EARTH  + compensation);
            //accTotal = (x2 + y2 + z2) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH) - 1;

            accTotalAbs = (float) (sqrt(accTotal * accTotal));


            GraphView graph = (GraphView) findViewById(R.id.graph);

            {
                if ((series.getHighestValueX() - series.getLowestValueX()) < 4000) {
                    xDataLength++;
                    series.appendData(new DataPoint((currentTimeMs - startTimeMs), accTotal), false, 100);
                    seriesY.appendData(new DataPoint((currentTimeMs - startTimeMs), aYM), false, 100);
                    seriesX.appendData(new DataPoint((currentTimeMs - startTimeMs), devY), false, 100);

                    if (xDataLength == 40)
                    {
                        compensation = (accTotal * -1);
                    }

                }
                else
                {
                    series.appendData(new DataPoint((currentTimeMs - startTimeMs), accTotal), true, xDataLength);
                    seriesY.appendData(new DataPoint((currentTimeMs - startTimeMs), aYM), true, xDataLength);
                    seriesX.appendData(new DataPoint((currentTimeMs - startTimeMs), devY), true, xDataLength);
                }
            }

            //graph.getViewport().setMinX(series.getLowestValueX());
            //graph.getViewport().setMaxX(series.getLowestValueX() + 4000);

            //accelSquareRoot2 = (ax * ax + ay * ay + az * az) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
            //accelationSquareRoot = (float) (sqrt(ax * ax + ay * ay + az * az) - SensorManager.GRAVITY_EARTH);

            long actualTime = event.timestamp;

            if (accTotalAbs > totalMax)
            {
                totalMax = accTotalAbs;
            }
            /*
            if ((accTotalAbs > checkAcc) && ((accTotalPrev * accTotal) > 0) &&
                    ((aXM * aXMPrev) > 0) && ((aYM * aYMPrev) > 0) )
                    */
            if ((accTotalAbs > checkAcc) && ((accTotalPrev * accTotal) > 0))
            {

                if (lastUpdateX == 0) {
                    lastUpdateX = System.currentTimeMillis();
                    durationX = 0;
                    devXPrev = devX;

                }
                if (lastUpdateY == 0) {
                    lastUpdateY = System.currentTimeMillis();
                    devYPrev = devY;
                    durationY = 0;

                }

                if (lastUpdateZ == 0) {
                    lastUpdateZ = System.currentTimeMillis();
                    devZPrev = devZ;
                    durationZ = 0;
                }

                if ((devXPrev * devX) > 0) {

                    durationX = currentTimeMs - lastUpdateX;
                    /*
                    if (sqrt(devX * devX) > 0.5) {
                        durationX = currentTimeMs - lastUpdateX;
                    }
                    else
                    {
                        lastUpdateX = currentTimeMs;
                        durationX = 0;
                    }
                    */

                }
                else
                {
                    durationX = 0;
                    lastUpdateX = 0;

                }

                if (((devYPrev * devY) > 0) /* && ( sqrt(devY * devY) > 0.5) */)
                {
                    durationY = currentTimeMs - lastUpdateY;
                    /*
                    if ( sqrt(devY * devY) > 0.5) {
                        durationY = currentTimeMs - lastUpdateY;
                    }
                    else
                    {
                        lastUpdateY = currentTimeMs;
                        durationY = 0;
                    }
                    */
                }
                else
                {
                    durationY = 0;
                    lastUpdateY = currentTimeMs;
                }

                if (((devZPrev * devZ) > 0))
                {
                    durationZ = currentTimeMs - lastUpdateY;
                }
                else
                {
                    lastUpdateZ = 0;
                    durationZ = 0;
                }

                //devXPrev = devX;
                //devYPrev = devY;
                //devZPrev = devZ;


                /* Add all the diff v */
                accSum += accTotalAbs;
                accCount++;

                totalAccIntegrated = (accSum / accCount) * durationY;

                if (totalAccIntegrated > integratedMax)
                {
                    integratedMax = totalAccIntegrated;
                }

                /* Get Moved durationY */
                    if ((durationY >= checkDuration) || (durationX >= checkDuration)) {
                        //scrollTextView.append("1 sec!!!\n");

                        if (takeoffDetected == false) {
                            takeoffDetected = true;
                            takeOffCounter++;
                        }
                    }

                if (durationY > durationMax)
                {
                    durationMax = durationY;
                }
                if (durationX > durationMax)
                {
                    durationMax = durationX;
                }

            }
            else
            {

                totalAccIntegrated = 0;

                lastUpdateX = 0;
                lastUpdateY = 0;
                lastUpdateZ = 0;
                accSum = 0;
                accCount = 0;

                aXI = 0;
                aYI = 0;
                aZI = 0;

                takeoffDetected = false;
                isAcceleration = currentAcceleration;
                durationY = 0;



                aXMPrev = aXM;
                aZMPrev = aZM;
                aYMPrev = aYM;

                devXPrev = 0;
                devYPrev = 0;
                devZPrev = 0;

            }


            accTotalPrev = accTotal;




            str = String.format("Total: %.4f, %.4f, %.4f\n", accTotal, accTotalAbs, totalMax);
            scrollTextView.append(str);


            str = String.format("Moving To: %d\n", movDirection);
            scrollTextView.append(str);


            str = String.format("Total Max: %.4f, %.4f\n", totalAccIntegrated, integratedMax);
            scrollTextView.append(str);


            str = String.format("Dur: %d , %d ms\n", durationY, durationMax);
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
