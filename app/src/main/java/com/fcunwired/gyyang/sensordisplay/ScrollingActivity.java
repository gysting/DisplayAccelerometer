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
    /* time ms at the start of motion */
    private long accXMotionStartedMs = 0, accYMotionStartedMs = 0, accZMotionStartedMs = 0;
    private boolean isMagnetosensor;
    private boolean isAccellerometer;
    private boolean isGiroScope;

    /* accelerometer */
    /* Current Accelerometer */
    private Sensor mAccelerometer;
    //float aX, aY, aZ;

    /* Average value without noise */
    float accXMean = 0, accYMean = 0, accZMean = 0;
    /* previous average mean value */
    float accXMeanPrev = 0, accYMeanPrev = 0, accZMeanPrev = 0;

    /* Integrated Avlue */
    //float aXI = 0, aYI = 0, aZI = 0;
    //int movDirection = 0;
    //float accXMeanPrev, accYMeanPrev, accZMeanPrev;

    float totalAccIntegrated = 0;
    float integratedMax = 0;

    /* total acceleration - G */
    float accTotalPrev = 0;

    long durationY = 0, durationX = 0, durationZ = 0;

    /* max absolute total sqrt(x^2 + y^2 + Z^2) */
    float totalMaxAbs = 0;


    float accSum = 0;
    private long accCount= 0;
    private long durationMaxX = 0, durationMaxY = 0;

    private long takeOffCounter=0;

    //boolean isAcceleration = true;
    boolean takeoffDetected = false;

    /* ms to detect take off, 7 seconds*/
    long checkDuration = 7000;

    /* minimum absolute acceleration to detect movement */
    double checkAcc=0.1;

    /* compensation value after calibration */
    float compensation  = 0;

    /* deviation from previous position */
    float devXPrev, devYPrev, devZPrev;

    float[] xBuf = new float[100];
    float[] yBuf = new float[100];
    float[] zBuf = new float[100];
    int bufIndex = 0;

    /* Number of X-axis values */
    int xDataLength = 0;
    int accCalSample = 40;
    int accEventCounter = 0;

    /* Peroid of accelerometer event to get */
    private long accPeriod = 0;
    private long accFreq = 20;

    /* Time stapm in ms */
    private long accMsPrev = 0;

    //long count;

    LineGraphSeries<DataPoint> seriesAccX = new LineGraphSeries<DataPoint>();
    LineGraphSeries<DataPoint> seriesAccY = new LineGraphSeries<DataPoint>();
    LineGraphSeries<DataPoint> seriesAccZ = new LineGraphSeries<DataPoint>();



    /* magnetic field */
    float magSquareRoot;
    float mX, mY, mZ;

    /* gyroscope */
    float gyroSquareRoot;
    float gX, gY, gZ;

    long startTimeMs = System.currentTimeMillis();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        //final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        //final TextView checkTextView = (TextView)findViewById(R.id.checkTextView);
        //checkTextView.setText("current: " + checkDuration + ",  " + checkAcc);
        final EditText accDurationTextBox = (EditText)findViewById(R.id.durationTextBox);
        final EditText accTextBox = (EditText)findViewById(R.id.accTextBox);

        String str;

        str = String.format("%.1f", checkAcc);
        accTextBox.setText(str);
        str = String.format("%d", checkDuration);
        accDurationTextBox.setText(str);

        /*

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

*/

        TextView scrollTextView = (TextView) findViewById(R.id.scrollTextView);
        scrollTextView.setMovementMethod(new ScrollingMovementMethod());
        scrollTextView.setVerticalScrollBarEnabled(true);
        scrollTextView.setMaxLines(30);
        scrollTextView.setText("");


        GraphView graph = (GraphView) findViewById(R.id.graph);
        seriesAccX = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(0,0)});
        seriesAccY = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(0,0)});
        seriesAccZ = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(0,0)});

        seriesAccX.setColor(Color.CYAN);
        seriesAccY.setColor(Color.RED);
        seriesAccZ.setColor(Color.BLACK);



        graph.addSeries(seriesAccX);
        graph.addSeries(seriesAccY);
        graph.addSeries(seriesAccZ);


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



        accXMotionStartedMs = 0;
        accYMotionStartedMs = 0;
        accZMotionStartedMs = 0;

        final Button checkButton = (Button)findViewById(R.id.CheckButton);
        Button button = (Button)findViewById(R.id.button);

        button.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        totalAccIntegrated=0;
                        totalMaxAbs=0;
                        integratedMax = 0;
                        accSum = 0;
                        accCount= 0;
                        durationMaxX = 0;
                        durationMaxY = 0;
                        takeOffCounter=0;
                        accTotalPrev = 0;
                        accXMean = 0;
                        accYMean = 0;
                        accZMean = 0;
                        accXMeanPrev = 0;
                        accYMeanPrev = 0;
                        accZMeanPrev = 0;
                        durationY = 0;
                        durationX = 0;
                        durationZ = 0;
                        takeoffDetected = false;
                        compensation  = 0;
                        accXMotionStartedMs = 0;
                        accYMotionStartedMs = 0;
                        accZMotionStartedMs = 0;
                        accEventCounter = 0;
                    }
                }
        );

        checkButton.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        if ((accTextBox.getText().length() != 0) && (accDurationTextBox.getText().length() != 0))
                        {
                            checkAcc = Double.valueOf(accTextBox.getText().toString());
                            checkDuration = Long.valueOf(accDurationTextBox.getText().toString());
                            //durationTextBox.setText("");
                            //accTextBox.setText("");

                        }
                        //checkTextView.setText("current: " + checkDuration + ",   " + checkAcc);
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
       // boolean currentAcceleration = false;
        long currentTimeMs = 0;
        // long freq= 0;

        String str;

        currentTimeMs = System.currentTimeMillis();

        final TextView scrollTextView = (TextView) findViewById(R.id.scrollTextView);

        if (isAccellerometer && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
            float x, y, z;
            float x2, y2, z2;
            float[] values = event.values;
            float accTotal, accTotalAbs;
            float devX, devY, devZ;
            final int bufLenth = 20;


            // Movement
            x = values[0];
            y = values[1];
            z = values[2];


            if (accMsPrev == 0) {
                accMsPrev = currentTimeMs;
            }
            else
            {
                /* time stamp */
                accPeriod = (currentTimeMs - accMsPrev);

                if ((accPeriod) < (1000/accFreq))
                {
                    /* 20 Hz */
                    return;
                }

                accMsPrev = currentTimeMs;

            }

            /* number of event */
            accEventCounter++;


            /*
            accXMean = (float) (accXMean + x)/2;
            accYMean = (float) (accYMean + y)/2;
            accZMean = (float) (accZMean + z)/2;
            */

            /* remove small high frequency noise. */
            xBuf[bufIndex] = x;
            yBuf[bufIndex] = y;
            zBuf[bufIndex] = z;
            bufIndex++;
            if (bufIndex >= bufLenth)
            {
                bufIndex = 0;
            }

            accXMean = 0;
            accYMean = 0;
            accZMean = 0;

            for (int i = 0; i < bufLenth; i++)
            {
                accXMean += xBuf[i];
                accYMean += yBuf[i];
                accZMean += zBuf[i];
            }

            /* Get Average value */
            accXMean = accXMean / bufLenth;
            accYMean = accYMean / bufLenth;
            accZMean = accZMean / bufLenth;

            /* deviation from previous mean value */
            if ((accXMeanPrev == 0) && (accYMeanPrev == 0) && (accZMeanPrev == 0)){
                devX = 0;
                devY = 0;
                devZ = 0;
            }
            else
            {
                devX = accXMeanPrev - accXMean;
                devY = accYMeanPrev - accYMean;
                devZ = accZMeanPrev - accZMean;

            }


            str = String.format("Accel: %d Hz, No. %d\n", accFreq, accEventCounter );
            scrollTextView.setText(str);
            str = String.format("X: %.04f, Y: %.04f, Z: %.04f\n", accXMean, accYMean, accZMean);
            scrollTextView.append(str);

            /* square values */
            x2 = accXMean * accXMean;
            y2 = accYMean * accYMean;
            z2 = accZMean * accZMean;

            /* possible motion values */
            accTotal = (float) (sqrt(x2 + y2 + z2) - SensorManager.GRAVITY_EARTH  + compensation);


            //accTotal = (x2 + y2 + z2) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH) - 1;

            /* sqrt of motion values to remove negative values */
            accTotalAbs = (float) (sqrt(accTotal * accTotal));


            //GraphView graph = (GraphView) findViewById(R.id.graph);


            if ((seriesAccX.getHighestValueX() - seriesAccX.getLowestValueX()) < 4000) {
                xDataLength++;
                seriesAccX.appendData(new DataPoint((currentTimeMs - startTimeMs), accTotal), false, 100);
                seriesAccY.appendData(new DataPoint((currentTimeMs - startTimeMs), accYMean), false, 100);
                seriesAccZ.appendData(new DataPoint((currentTimeMs - startTimeMs), devY), false, 100);


            }
            else
            {
                seriesAccX.appendData(new DataPoint((currentTimeMs - startTimeMs), accTotal), true, xDataLength);
                seriesAccY.appendData(new DataPoint((currentTimeMs - startTimeMs), accYMean), true, xDataLength);
                seriesAccZ.appendData(new DataPoint((currentTimeMs - startTimeMs), devY), true, xDataLength);
            }

            if (accEventCounter == accCalSample)
            {
                /* calibrated compensation data */
                compensation = (accTotal * -1);
            }

            //graph.getViewport().setMinX(seriesAccX.getLowestValueX());
            //graph.getViewport().setMaxX(seriesAccX.getLowestValueX() + 4000);
            //accelSquareRoot2 = (ax * ax + ay * ay + az * az) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
            //accelationSquareRoot = (float) (sqrt(ax * ax + ay * ay + az * az) - SensorManager.GRAVITY_EARTH);

            /* Update max motion value */
            if (accTotalAbs > totalMaxAbs)
            {
                totalMaxAbs = accTotalAbs;
            }

            if ((accTotalAbs > checkAcc) && ((accTotalPrev * accTotal) > 0) && (accEventCounter > accCalSample))
            {

                if (accXMotionStartedMs == 0) {
                    accXMotionStartedMs = System.currentTimeMillis();
                    durationX = 0;
                    devXPrev = devX;

                }
                if (accYMotionStartedMs == 0) {
                    accYMotionStartedMs = System.currentTimeMillis();
                    devYPrev = devY;
                    durationY = 0;
                }

                if (accZMotionStartedMs == 0) {
                    accZMotionStartedMs = System.currentTimeMillis();
                    devZPrev = devZ;
                    durationZ = 0;
                }

                if ((devXPrev * devX) > 0) {
                    durationX = currentTimeMs - accXMotionStartedMs;
                    /*
                    if (sqrt(devX * devX) > 0.5) {
                        durationX = currentTimeMs - accXMotionStartedMs;
                    }
                    else
                    {
                        accXMotionStartedMs = currentTimeMs;
                        durationX = 0;
                    }
                    */
                }
                else
                {
                    durationX = 0;
                    accXMotionStartedMs = 0;
                }

                if (((devYPrev * devY) > 0) /* && ( sqrt(devY * devY) > 0.5) */)
                {
                    durationY = currentTimeMs - accYMotionStartedMs;
                    /*
                    if ( sqrt(devY * devY) > 0.5) {
                        durationY = currentTimeMs - accYMotionStartedMs;
                    }
                    else
                    {
                        accYMotionStartedMs = currentTimeMs;
                        durationY = 0;
                    }
                    */
                }
                else
                {
                    durationY = 0;
                    accYMotionStartedMs = currentTimeMs;
                }

                if (((devZPrev * devZ) > 0))
                {
                    durationZ = currentTimeMs - accYMotionStartedMs;
                }
                else
                {
                    accZMotionStartedMs = 0;
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

                if (durationY > durationMaxY)
                {
                    durationMaxY = durationY;
                }
                if (durationX > durationMaxX)
                {
                    durationMaxX = durationX;
                }

            }
            else
            {
                totalAccIntegrated = 0;

                accXMotionStartedMs = 0;
                accYMotionStartedMs = 0;
                accZMotionStartedMs = 0;
                accSum = 0;
                accCount = 0;

                takeoffDetected = false;
                //isAcceleration = currentAcceleration;
                durationY = 0;

                accXMeanPrev = accXMean;
                accZMeanPrev = accZMean;
                accYMeanPrev = accYMean;

                devXPrev = 0;
                devYPrev = 0;
                devZPrev = 0;
                accTotalPrev = accTotal;
            }



            str = String.format("Total: %.4f, %.4f, %.4f\n", accTotal, accTotalAbs, totalMaxAbs);
            scrollTextView.append(str);


            //str = String.format("Moving To: %d\n", movDirection);
            //scrollTextView.append(str);


            str = String.format("Total Max: %.4f, %.4f\n", totalAccIntegrated, integratedMax);
            scrollTextView.append(str);


            str = String.format("Dur: %d , %d ms\n", durationY, durationMaxY);
            scrollTextView.append(str);

            str = String.format("TakeOff: %d\n", takeOffCounter);
            scrollTextView.append(str);
        }
        if (isMagnetosensor && (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD))
        {
            float[] values = event.values;
            mX = values[0];
            mY = values[1];
            mZ = values[2];

            magSquareRoot = (float) (sqrt(mX *mX + mX * mX + mZ * mZ));

        }
        if (isGiroScope && (event.sensor.getType() == Sensor.TYPE_GYROSCOPE))
        {
            float[] values = event.values;
            gX = values[0];
            gY = values[1];
            gZ = values[2];

            gyroSquareRoot = (float) (sqrt(gX * gX + gY * gY + gY * gZ));
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
