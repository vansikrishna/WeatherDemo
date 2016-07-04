package com.xinthe.weatherdemo.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.xinthe.weatherdemo.R;
import com.xinthe.weatherdemo.activity.MainActivity;
import com.xinthe.weatherdemo.constants.Constants;
import com.xinthe.weatherdemo.entity.WeatherEntity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by BALBI on 6/29/2016.
 */
public class WeatherService extends Service{

    private final String TAG = "WeatherDemo";
    private static final String OPEN_WEATHER_MAP_API =
            "http://api.openweathermap.org/data/2.5/weather?q=%s&units=metric";
    private IBinder mBinder = new MyBinder();
    WeatherEntity weatherEntity;

    public WeatherEntity getWeatherData() {
        return weatherEntity;
    }

    public class MyBinder extends Binder {
        public WeatherService getService() {
            return WeatherService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");
    }

    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        broadcastData();
        initWeatherForecast(intent);
        return START_STICKY;
    }*/

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        broadcastData();
        initWeatherForecast(intent);
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
    }

    private void broadcastData(){
        Intent intent = new Intent();
        intent.setAction(Constants.INTENT_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private void initWeatherForecast(Intent intent) {
        if(intent != null && intent.hasExtra(Constants.KEY_CITY)){
            gatherForecastData(intent.getStringExtra(Constants.KEY_CITY));
        }
        else{
            gatherForecastData(getCityFromPreferences());
        }

    }

    private String getCityFromPreferences(){
        SharedPreferences preferences = getSharedPreferences(Constants.PREF_FILE_NAME, MODE_PRIVATE);
        return preferences.getString(Constants.KEY_CITY, "Visakhapatnam");
    }

    private void gatherForecastData(String city){
        new CustomAsyncTask(city).execute();
    }

    private class CustomAsyncTask extends AsyncTask<Void, String, WeatherEntity>{

        String city;

        public CustomAsyncTask(String city){
            this.city = city;
        }


        @Override
        protected WeatherEntity doInBackground(Void... params) {
            try {
                URL url = new URL(String.format(OPEN_WEATHER_MAP_API, city));
                HttpURLConnection connection =
                        (HttpURLConnection)url.openConnection();

                connection.addRequestProperty("x-api-key",
                        getString(R.string.open_weather_api_key));

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));

                StringBuffer stringBuffer = new StringBuffer(1024);
                String tmp="";
                while((tmp=reader.readLine())!=null)
                    stringBuffer.append(tmp).append("\n");
                reader.close();

                JSONObject jsonObject = new JSONObject(stringBuffer.toString());

                // This value will be 404 if the request was not
                // successful
                if(jsonObject.getInt("cod") != 200){
                    return null;
                }

                return parseJson(jsonObject);
            }catch(Exception e){
                return null;
            }
        }

        private WeatherEntity parseJson(JSONObject jsonObject) throws Exception{
            WeatherEntity weatherEntity = new WeatherEntity();
            JSONObject details = jsonObject.getJSONArray("weather").getJSONObject(0);
            JSONObject main = jsonObject.getJSONObject("main");
            weatherEntity.weatherTitle = details.getString("main");
            weatherEntity.weatherDescription = details.getString("description");
            weatherEntity.weatherIcon = details.getString("icon");
            weatherEntity.mainTemp = String.format("%.2f", main.getDouble("temp"))+ " ℃";
            weatherEntity.minTemp = String.format("%.2f", main.getDouble("temp_min"))+ " ℃";
            weatherEntity.maxTemp = String.format("%.2f", main.getDouble("temp_max"))+ " ℃";
            weatherEntity.updateTime = System.currentTimeMillis();
            return weatherEntity;
        }

        @Override
        protected void onPostExecute(WeatherEntity weatherEntity) {
            super.onPostExecute(weatherEntity);
            updateWeatherData(weatherEntity);
            updateNotification(city, weatherEntity);
            startTimer();
        }
    }

    private void startTimer() {
        CountDownTimer timer = new CountDownTimer(10*60*1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                gatherForecastData(getCityFromPreferences());
            }
        };
        timer.start();
    }

    private void updateWeatherData(WeatherEntity weatherEntity) {
        this.weatherEntity = weatherEntity;
        Intent intent = new Intent();
        intent.setAction(Constants.INTENT_ACTION);
        intent.putExtra(Constants.WEATHER_DATA, weatherEntity);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateNotification(String city, WeatherEntity weatherEntity) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_adb_white)
                .setContentTitle(String.format("%s in %s", weatherEntity.mainTemp, city))
                .setContentText(String.format("%s %s",
                        weatherEntity.weatherTitle, weatherEntity.weatherDescription))
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(1, notificationBuilder.build());
    }


}
