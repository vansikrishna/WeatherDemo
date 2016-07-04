package com.xinthe.weatherdemo.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.xinthe.weatherdemo.R;
import com.xinthe.weatherdemo.constants.Constants;
import com.xinthe.weatherdemo.entity.WeatherEntity;
import com.xinthe.weatherdemo.service.WeatherService;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    EditText cityEditText;
    ImageView weatherStatusImageView;
    TextView weatherStatusTextView;
    TextView minTempTextView;
    TextView maxTempTextView;
    TextView updateTimeTextView;
    WeatherService weatherService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initListeners();
        LocalBroadcastManager.getInstance(this).registerReceiver(weatherDataReceiver,
                new IntentFilter(Constants.INTENT_ACTION));
        updateViews(new WeatherEntity());
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(weatherDataReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(weatherService != null) {
            if(weatherService.getWeatherData() != null)
                updateViews(weatherService.getWeatherData());
            else
                updateViews(new WeatherEntity());
        }
    }

    private void initViews(){
        cityEditText = (EditText) findViewById(R.id.cityEditText);
        weatherStatusImageView = (ImageView) findViewById(R.id.weatherStatusImageView);
        weatherStatusTextView = (TextView) findViewById(R.id.weatherStatusTextView);
        minTempTextView = (TextView) findViewById(R.id.minTempTextView);
        maxTempTextView = (TextView) findViewById(R.id.maxTempTextView);
        updateTimeTextView = (TextView) findViewById(R.id.updateTimeTextView);
    }

    private void initListeners() {
        cityEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    hideKeyboard();
                    startWeatherService();
                    return true;
                }
                return false;
            }
        });
    }

    private void hideKeyboard() {
        cityEditText.clearFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(cityEditText.getWindowToken(), 0);
    }

    private void startWeatherService() {
        if(cityEditText.getText().toString().length() > 0){
            // Save the city name in Preferences, for later access.
            SharedPreferences preferences = getSharedPreferences(Constants.PREF_FILE_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.KEY_CITY, cityEditText.getText().toString().trim());
            editor.commit();

            // Start the service with the mentioned city.
            Intent intent = new Intent(this, WeatherService.class);
            intent.putExtra(Constants.KEY_CITY, cityEditText.getText().toString().trim());
            bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        }
        else{
            Toast.makeText(this, "Please enter a city name.", Toast.LENGTH_LONG).show();
        }
    }

    // a broadcast receiver created to listen to the broadcasted events sent using ACTION.
    private BroadcastReceiver weatherDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equalsIgnoreCase(Constants.INTENT_ACTION)){
                Toast.makeText(context, "Gathering forecast info...", Toast.LENGTH_LONG).show();
                if(intent.hasExtra(Constants.WEATHER_DATA)){
                    updateViews((WeatherEntity) intent.getSerializableExtra(Constants.WEATHER_DATA));
                }
            }
        }
    };

    private void updateViews(WeatherEntity weatherEntity) {
        if(weatherEntity.weatherTitle != null && weatherEntity.mainTemp != null){
            weatherStatusTextView.setText(String.format("%s - %s", weatherEntity.weatherTitle, weatherEntity.mainTemp));
        }
        else{
            weatherStatusTextView.setText("");
        }
        if(weatherEntity.minTemp != null)
            minTempTextView.setText(weatherEntity.minTemp);
        else
            minTempTextView.setText("0 ℃");

        if(weatherEntity.maxTemp != null)
            maxTempTextView.setText(weatherEntity.maxTemp);
        else
            maxTempTextView.setText("0 ℃");

        if(weatherEntity.updateTime != 0)
            updateTimeTextView.setText(new SimpleDateFormat("EEE, dd MMM yyyy, h:mm a").format(new Date(weatherEntity.updateTime)));
        else
            updateTimeTextView.setText("");

    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WeatherService.MyBinder myBinder = (WeatherService.MyBinder) service;
            weatherService = myBinder.getService();
        }
    };


}
