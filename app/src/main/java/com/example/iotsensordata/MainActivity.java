package com.example.iotsensordata;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TAG" ;
    //initialise all text view, button and edittext
    TextView view;
    EditText port, address;
    Button reset, connect;

    //assign variable and create variable to store data
    private SensorManager sensorManager;
    private Sensor thermo;
    private Sensor pulsemeter;
    private Sensor accelerometer;
    private float temperature, heartrate;
    private float x, y, z;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        address = (EditText) findViewById(R.id.address);
        port = (EditText) findViewById(R.id.port);
        reset = (Button) findViewById(R.id.reset);
        connect = (Button) findViewById(R.id.connect);
        view = (TextView) findViewById(R.id.view);

        //assign and configure sensor type to the variable
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        thermo = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        pulsemeter = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //check the existence of sensor in the device
        if (sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null){
            Log.e(TAG, "Successful! There is a temperature sensor");
        } else{ Log.e(TAG, "Failed! There is no temperature sensor");}

        if (sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null){
            Log.e(TAG, "Successful! There is a pulserate sensor");
        } else{ Log.e(TAG, "Failed! There is no pulserate sensor");}

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            Log.e(TAG, "Successful! There is a accelerometer");
        } else{ Log.e(TAG, "Failed! There is no accelerometer");}


        connect.setOnClickListener(connectOnClickListener);

        //clear text view when reset button is clicked
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.setText("");
            }
        });
    }

    View.OnClickListener connectOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            MyClientTask clientTask = new MyClientTask(
                    address.getText().toString(),
                    //The parseInt() function parses a string and returns an integer
                    Integer.parseInt(port.getText().toString()));
            clientTask.execute();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //sensor delay fastest to get sensor data as fast as possible
        sensorManager.registerListener(temp_listener, thermo, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(accelero_listener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(pulse_listener, pulsemeter, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop() {
        //Unregisters a listener for the sensors with which it is registered.
        sensorManager.unregisterListener(temp_listener);
        sensorManager.unregisterListener(accelero_listener);
        sensorManager.unregisterListener(pulse_listener);
        super.onStop();
    }

    //Events are returned in the usual way through the SensorEventListener
    private SensorEventListener temp_listener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            temperature = event.values[0];
        }
    };

    private SensorEventListener pulse_listener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            heartrate = event.values[0];
        }
    };

    private SensorEventListener accelero_listener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
        }
    };

    //Android AsyncTask is an abstract class provided by Android which gives us the liberty to perform heavy tasks in the background
    // and keep the UI thread light thus making the application more responsive
    public class MyClientTask extends AsyncTask<Void, Void, Void> {
        String dstAddress;
        int dstPort;
        String response = "";

        MyClientTask(String addr, int portnum) {
            dstAddress = addr;
            dstPort = portnum;
        }

        //stream the sensor data in background, send result multiple time to the UI thread
        @Override
        protected Void doInBackground(Void... arg0) {
            Socket socket = null;
            String sensorReading;
            int readData = 1;
            DataOutputStream dataOutputStream = null;
            try {
                //A socket is an endpoint for communication between the gateway and the mobile.
                socket = new Socket(dstAddress, dstPort);
                while (true) {
                    dataOutputStream = new DataOutputStream((socket.getOutputStream()));
                    sensorReading = Float.toString(temperature) + "," + Float.toString(heartrate) + "," + Float.toString(x)
                            + "," + Float.toString(y) + "," + Float.toString(z) + "," + Integer.toString(readData);

                    //label the index for the sensor data output
                    readData = readData + 1;
                    //Writes out a byte to the underlying output stream as a 1-byte value.
                    dataOutputStream.writeBytes(sensorReading);

                    response = "temperature : " + Float.toString(temperature) + "\nHeart_Rate : " + Float.toString(heartrate)  + "\nx = "
                            + Float.toString(x) + "\ny = " + Float.toString(y) + "\nz = " + Float.toString(z) + "\nData_id = " + Float.toString(readData);
                    //runOnUiThread do background operations on worker thread and update the result on main thread.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            view.setText(response);
                        }
                    });
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
        //After doInBackground processing method is completed, pass the processed sensor data to the show on the text view
        @Override
        protected void onPostExecute(Void result) {
            view.setText(response);
            super.onPostExecute(result);
        }
    }
}
