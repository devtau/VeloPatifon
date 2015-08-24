package tt.velopatifon;

import android.app.Activity;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.TimeUnit;
import android.os.AsyncTask;


public class MainActivity extends Activity {
    String LOG_TAG = "MainActivity";
    private TextView tvSpeed1, tvSpeed2, tvSpeed3, tvSpeed4, tvSpeed5, tvSpeedDiff1, tvSpeedDiff2, tvSpeedDiff3, tvSpeedDiff4, tvSpeedDiff5, tvTargetSpeed, tvDiffNormal;
    private EditText etTargetSpeed, etDiffNormal;
    private Button btnStartTracking, btnPlayer, btnStopTracking;
    private AsyncSpeedometer speedometer;
    private GPSTracker       mGPS;
    private final static int STEP_IN_SECONDS = 10;
    private double currentPointLat, currentPointLong;
    private double targetSpeed, diffNormal, currentSpeed, speedDiff;

    boolean isPlaying = false;
    SoundPool soundPool;
    static int streamId = 0;
    int soundId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(LOG_TAG, "new instance created");
        tvSpeed1            = (TextView) findViewById(R.id.tvSpeed1);
        tvSpeed2            = (TextView) findViewById(R.id.tvSpeed2);
        tvSpeed3            = (TextView) findViewById(R.id.tvSpeed3);
        tvSpeed4            = (TextView) findViewById(R.id.tvSpeed4);
        tvSpeed5            = (TextView) findViewById(R.id.tvSpeed5);
        tvSpeedDiff1        = (TextView) findViewById(R.id.tvSpeedDiff1);
        tvSpeedDiff2        = (TextView) findViewById(R.id.tvSpeedDiff2);
        tvSpeedDiff3        = (TextView) findViewById(R.id.tvSpeedDiff3);
        tvSpeedDiff4        = (TextView) findViewById(R.id.tvSpeedDiff4);
        tvSpeedDiff5        = (TextView) findViewById(R.id.tvSpeedDiff5);
        tvTargetSpeed       = (TextView) findViewById(R.id.tvTargetSpeed);
        etTargetSpeed       = (EditText) findViewById(R.id.etTargetSpeed);
        tvDiffNormal        = (TextView) findViewById(R.id.tvDiffNormal);
        etDiffNormal        = (EditText) findViewById(R.id.etDiffNormal);
        btnStartTracking    = (Button)   findViewById(R.id.btnStartTracking);
        btnStopTracking     = (Button)   findViewById(R.id.btnStopTracking);
        btnStopTracking.setEnabled(false);
        btnPlayer           = (Button)   findViewById(R.id.btnPlayer);
        mGPS = new GPSTracker(this);

        if (Build.VERSION.SDK_INT >= 21)
            soundPool = new SoundPool.Builder().build();
        else soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);

//        LinearLayout rateButtons = (LinearLayout) findViewById(R.id.rateButtons);
//        for (View view : rateButtons.getTouchables()) view.setEnabled(true);
    }

    public void onStartBTNClick(View v){
        tvSpeed1.setText(""); tvSpeed2.setText(""); tvSpeed3.setText(""); tvSpeed4.setText(""); tvSpeed5.setText("");
        tvSpeedDiff1.setText(""); tvSpeedDiff2.setText(""); tvSpeedDiff3.setText(""); tvSpeedDiff4.setText(""); tvSpeedDiff5.setText("");
        if ("".equals(etTargetSpeed.getText().toString()) || "".equals(etDiffNormal.getText().toString()))
            Toast.makeText(getApplicationContext(), "заполните нужную скорость и отклонение", Toast.LENGTH_LONG).show();
        else {
            targetSpeed = Double.parseDouble(etTargetSpeed.getText().toString());
            diffNormal  = Double.parseDouble(etDiffNormal.getText().toString());
            if (mGPS.isGPSEnabled) {
                speedometer = new AsyncSpeedometer();
                speedometer.execute();
            } else {
                Toast.makeText(getApplicationContext(), "GPS выключен!\nВключите его перед использованием!\n" +
                        "Если Вы только что его включили\n" +
                        "нажмите еще раз", Toast.LENGTH_LONG).show();
                mGPS = new GPSTracker(this);
            }
        }
    }

    public void onPlayerBTNClick(View v) {
        if (!isPlaying) {
            if (streamId == 0) {
                soundId = soundPool.load(this, R.raw.redshoes, 1);
                soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                    @Override
                    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                        streamId = soundPool.play(soundId, 1, 1, 1, -1, 1f);
                        soundPool.setLoop(streamId, -1);
                        isPlaying = true;
                        btnPlayer.setText("Pause");
                    }
                });

            }
            soundPool.resume(streamId);
            Log.d(LOG_TAG, "resume stream: " + streamId);
            isPlaying = true;
            btnPlayer.setText("Pause");
        } else {
            soundPool.pause(streamId);
            Log.d(LOG_TAG, "Pause stream: " + streamId);
            isPlaying = false;
            btnPlayer.setText("Play");
        }
    }

    public void onStopBTNClick(View v){
        speedometer.cancel(true);
    }

    public void onRate090BTNClick(View v){ soundPool.setRate(streamId, 0.9f); }
    public void onRate100BTNClick(View v){ soundPool.setRate(streamId, 1.0f); }
    public void onRate110BTNClick(View v){ soundPool.setRate(streamId, 1.1f); }
    public void onRate200BTNClick(View v){ soundPool.setRate(streamId, 2.0f); }


    @Override
    public void onBackPressed(){
        super.onBackPressed();
        onDestroy();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundPool.release();
        streamId = 0;
        if (speedometer != null) speedometer.cancel(true);
    }


    //AsyncSpeedometer------------------------------------------------------------------------------
    class AsyncSpeedometer extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tvDiffNormal.setEnabled(false);
            etDiffNormal.setEnabled(false);
            tvTargetSpeed.setEnabled(false);
            etTargetSpeed.setEnabled(false);
            btnStartTracking.setEnabled(false);
            btnStopTracking.setEnabled(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (int i = 0; ; i++) {
                if (mGPS.isGPSEnabled) {
                    mGPS.getLocation();
                    double previousPointLat = currentPointLat;
                    double previousPointLong = currentPointLong;
                    currentPointLat = mGPS.getLatitude();
                    currentPointLong = mGPS.getLongitude();
                    if (i > 0) {
                        double distanceValue = CalculateDistanceByLatLng(previousPointLat, previousPointLong, currentPointLat, currentPointLong);
                        currentSpeed = (distanceValue / STEP_IN_SECONDS) * 3.6;
                        currentSpeed = RoundResult(currentSpeed, 2);
                        speedDiff = currentSpeed - targetSpeed;
                        speedDiff = RoundResult(speedDiff, 2);
                    }
                    if (i >= 5) publishProgress(5);
                    else publishProgress(i);

                    try{TimeUnit.SECONDS.sleep(STEP_IN_SECONDS);}catch (InterruptedException e){/*NOP*/}
                    if (isCancelled()) return null;
                } else {
                    publishProgress(-1);
                    try{TimeUnit.SECONDS.sleep(STEP_IN_SECONDS);}catch (InterruptedException e){/*NOP*/}
                    if (isCancelled()) return null;
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            switch (values[0]) {
                case -1:
                    Toast.makeText(getApplicationContext(), "у Вас выключен GPS", Toast.LENGTH_LONG).show();
                    break;
                case 0:
                    break;
                case 1:
                    tvSpeed1.setText(String.valueOf(currentSpeed));
                    tvSpeedDiff1.setText(String.valueOf(speedDiff));
                    break;
                case 2:
                    tvSpeed2.setText(tvSpeed1.getText());
                    tvSpeed1.setText(String.valueOf(currentSpeed));
                    tvSpeedDiff2.setText(tvSpeedDiff1.getText());
                    tvSpeedDiff1.setText(String.valueOf(speedDiff));
                    break;
                case 3:
                    tvSpeed3.setText(tvSpeed2.getText());
                    tvSpeed2.setText(tvSpeed1.getText());
                    tvSpeed1.setText(String.valueOf(currentSpeed));
                    tvSpeedDiff3.setText(tvSpeedDiff2.getText());
                    tvSpeedDiff2.setText(tvSpeedDiff1.getText());
                    tvSpeedDiff1.setText(String.valueOf(speedDiff));
                    break;
                case 4:
                    tvSpeed4.setText(tvSpeed3.getText());
                    tvSpeed3.setText(tvSpeed2.getText());
                    tvSpeed2.setText(tvSpeed1.getText());
                    tvSpeed1.setText(String.valueOf(currentSpeed));
                    tvSpeedDiff4.setText(tvSpeedDiff3.getText());
                    tvSpeedDiff3.setText(tvSpeedDiff2.getText());
                    tvSpeedDiff2.setText(tvSpeedDiff1.getText());
                    tvSpeedDiff1.setText(String.valueOf(speedDiff));
                    break;
                case 5:
                    tvSpeed5.setText(tvSpeed4.getText());
                    tvSpeed4.setText(tvSpeed3.getText());
                    tvSpeed3.setText(tvSpeed2.getText());
                    tvSpeed2.setText(tvSpeed1.getText());
                    tvSpeed1.setText(String.valueOf(currentSpeed));
                    tvSpeedDiff5.setText(tvSpeedDiff4.getText());
                    tvSpeedDiff4.setText(tvSpeedDiff3.getText());
                    tvSpeedDiff3.setText(tvSpeedDiff2.getText());
                    tvSpeedDiff2.setText(tvSpeedDiff1.getText());
                    tvSpeedDiff1.setText(String.valueOf(speedDiff));
                    break;
            }

            String msg;
            if (values[0] == 0) {
                msg = "Поехали!";
            }
            else if (currentSpeed < 5.0) {
                msg = "вы остановились";
                soundPool.setRate(streamId, 1.0f);
            }
            else if (speedDiff < -diffNormal)  {
                msg = "очень медленно";
                soundPool.setRate(streamId, 0.9f);
            }
            else if (speedDiff > diffNormal) {
                msg = "очень быстро";
                soundPool.setRate(streamId, 1.1f);
            }
            else {
                msg = "едем нормально";
                soundPool.setRate(streamId, 1.0f);
            }
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Toast.makeText(getApplicationContext(), "вы не увидите это сообщение", Toast.LENGTH_SHORT).show();

            tvDiffNormal.setEnabled(true);
            etDiffNormal.setEnabled(true);
            tvTargetSpeed.setEnabled(true);
            etTargetSpeed.setEnabled(true);
            btnStartTracking.setEnabled(true);
            btnStopTracking.setEnabled(false);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Toast.makeText(getApplicationContext(), "программа остановлена", Toast.LENGTH_SHORT).show();

            tvDiffNormal.setEnabled(true);
            etDiffNormal.setEnabled(true);
            tvTargetSpeed.setEnabled(true);
            etTargetSpeed.setEnabled(true);
            btnStartTracking.setEnabled(true);
            btnStopTracking.setEnabled(false);
        }

        //Computes the approximate distance between two locations in meters
        private double CalculateDistanceByLatLng(double latitudeStartP, double longitudeStartP, double latitudeEndP, double longitudeEndP) {
            float[] results = new float[1];
            Location.distanceBetween(latitudeStartP, longitudeStartP, latitudeEndP, longitudeEndP, results);
            return results[0];
        }

        private double RoundResult(double value, int decimalSigns) {
            int multiplier = (int) Math.pow(10.0, (double) decimalSigns);
            int numerator  = (int) Math.round(value * multiplier);
            return (double) numerator / multiplier;
        }
    }
}
