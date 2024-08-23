package com.example.weather;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.location.Location;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private TextView textViewTemperature, textViewCondition, textViewDetails, textViewRain;
    private Handler handler;
    private Runnable weatherRunnable;
    private final int UPDATE_INTERVAL = 10 * 60 * 1000;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewCondition = findViewById(R.id.textViewCondition);
        textViewDetails = findViewById(R.id.textViewDetails);
        textViewRain = findViewById(R.id.textViewRain);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {

            startWeatherUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startWeatherUpdates();
            } else {
                Toast.makeText(this, "Permission is Necessary", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void startWeatherUpdates() {
        weatherRunnable = new Runnable() {
            @Override
            public void run() {
                getLastKnownLocation();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        handler = new Handler();
        handler.post(weatherRunnable); // Start immediately
    }

    private void getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        fetchWeatherData(latitude, longitude);
                    }
                }
            });
        }
    }


    private void fetchWeatherData(double latitude, double longitude) {
        OkHttpClient client = new OkHttpClient();

        String apiKey = getString(R.string.openweather_api_key);
        String units = "metric"; // Change to metric to get temperature in Celsius
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&units=" + units + "&appid=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseData = response.body().string();
                JSONObject json = new JSONObject(responseData);

                double temp = json.getJSONObject("main").getDouble("temp");
                String condition = json.getJSONArray("weather").getJSONObject(0).getString("description");
                double feelsLike = json.getJSONObject("main").getDouble("feels_like");
                double tempMin = json.getJSONObject("main").getDouble("temp_min");
                double tempMax = json.getJSONObject("main").getDouble("temp_max");

                runOnUiThread(() -> {
                    textViewTemperature.setText(String.format("%.0f°C", temp));
                    textViewCondition.setText(condition);
                    textViewDetails.setText(String.format("Feels like %.0f°C", feelsLike));
                    textViewRain.setText(String.format("Heavy rain is expected. The low will be %.0f°C.", tempMin));
                });
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(weatherRunnable);
    }
}