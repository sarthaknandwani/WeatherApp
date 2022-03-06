package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV;
    private TextInputEditText cityEdt;
    private ImageView backIV;
    private ImageView iconIV;
    private ArrayList<WeatherRVModal> weatherRVModalArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private final int PERMISSION_CODE=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);


        setContentView(R.layout.activity_main);

        homeRL = findViewById(R.id.idRLHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        RecyclerView weatherRV = findViewById(R.id.idRVWeather);
        cityEdt = findViewById(R.id.idEdtCity);
        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idTVIcon);
        ImageView searchIV = findViewById(R.id.idIVSearch);

        weatherRVModalArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModalArrayList);

        weatherRV.setAdapter(weatherRVAdapter);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        String cityName = getCityName(location.getLongitude(), location.getLatitude());

        getWeatherInfo(cityName);

        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String city = cityEdt.getText().toString();

                if(city.isEmpty()){
                    Toast.makeText(MainActivity.this, "Please enter a city name", Toast.LENGTH_SHORT).show();
                }
                else{
                    cityNameTV.setText(city);
                    getWeatherInfo(city);
                }

            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==PERMISSION_CODE){
            if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "Please provide the permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double longitude, double latitude){
        String cityName="Not Found";

        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try{
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);

            for(Address adr: addresses){
                if(adr!=null){
                    String city = adr.getLocality();
                    if(city!=null && !city.equals("")){
                        cityName=city;
                        Toast.makeText(this, "Your City is: " + city, Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Log.d(TAG, "getCityName: CITY NOT FOUND");
                    }
                }
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return cityName;
    }

    private void getWeatherInfo(String cityName){
//        String url= "http://api.weatherapi.com/v1/forecast.json?key=c946ef1074f54ea7a0b125955220203&q=" + cityName + "&days=1&aqi=no&alerts=no";

//        String url= "http://api.weatherapi.com/v1/forecast.json?key=c946ef1074f54ea7a0b125955220203&q="+cityName+"&days=1&aqi=no&alerts=no";

        String url = "http://api.weatherapi.com/v1/forecast.json?key=c946ef1074f54ea7a0b125955220203 &q="+cityName+"&days=1&aqi=yes&alerts=yes";
        cityNameTV.setText(cityName);

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        String r=null;
        try {

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    loadingPB.setVisibility(View.GONE);
                    homeRL.setVisibility(View.VISIBLE);

                    weatherRVModalArrayList.clear();

                    try {
                        String temperature = response.getJSONObject("current").getString("temp_c");
                        temperatureTV.setText(temperature + "ºc");

                        int isDay = response.getJSONObject("current").getInt("is_day");
                        String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                        String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                        Picasso.get().load("http:".concat(conditionIcon)).into(iconIV);
                        conditionTV.setText(condition);

                        if (isDay == 1) {
                            Picasso.get().load("https://images.unsplash.com/photo-1503891617560-5b8c2e28cbf6?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=1374&q=80").into(backIV);
                        } else {
                            Picasso.get().load("https://images.unsplash.com/photo-1568233823082-873ab0156ad7?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=387&q=80").into(backIV);
                        }

                        JSONObject forecastObject = response.getJSONObject("forecast");
                        JSONObject forecast0 = forecastObject.getJSONArray("forecastday").getJSONObject(0);
                        JSONArray hourArray = forecast0.getJSONArray("hour");

                        for (int i = 0; i < hourArray.length(); i++) {
                            JSONObject hourObject = hourArray.getJSONObject(i);
                            String time = hourObject.getString("time");
                            String temper = hourObject.getString("temp_c");
                            String img = hourObject.getJSONObject("condition").getString("icon");
                            String wind = hourObject.getString("wind_kph");
                            weatherRVModalArrayList.add(new WeatherRVModal(time, temper, img, wind));
                        }
                        weatherRVAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
//                Toast.makeText(MainActivity.this, "Please Enter A valid city Name", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onErrorResponse: "+ error.getMessage());
                }
            });

            requestQueue.add(jsonObjectRequest);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}