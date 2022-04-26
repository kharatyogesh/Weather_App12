package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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

    private ProgressBar loadingPB;
    private RelativeLayout homeRL;
    private TextView cityNameTV,temperatureTV,conditionTV;
    private ImageView backIV,searchIV,iconIV;
    private TextInputEditText cityEdt;
    private RecyclerView weatherRV;
    private ArrayList<WeatherRVModal> weatherRVModalArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private LocationManager locationManager;
    private int  PERMISSION_CODE=1;
    private String cityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);

        loadingPB =findViewById(R.id.idPBLoading);
        homeRL =findViewById(R.id.idRLHome);
        cityNameTV =findViewById(R.id.idTVCityName);
        temperatureTV =findViewById(R.id.idTVTemperature);
        conditionTV =findViewById(R.id.idTVCondition);
        backIV =findViewById(R.id.idIVBack);
        searchIV =findViewById(R.id.idIVSearch);
        iconIV =findViewById(R.id.idIVIcon);
        cityEdt =findViewById(R.id.idEdtCity);
        weatherRV =findViewById(R.id.idRvWeather);
        weatherRVModalArrayList=new ArrayList<>();
        weatherRVAdapter=new WeatherRVAdapter(this,weatherRVModalArrayList);
        weatherRV.setAdapter(weatherRVAdapter);
        cityEdt.onEditorAction(EditorInfo.IME_ACTION_DONE);
// Then just use the following:
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(homeRL.getWindowToken(), 0);
        locationManager=(LocationManager) getSystemService(Context.LOCATION_SERVICE);


        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_CODE);
        }
        
        

        if (isNetworkConnected()){
            Location location=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            //   cityName = getCityName(location.getLongitude(),location.getLatitude());

            if (location != null){
                cityName = getCityName(location.getLongitude(),location.getLatitude());
                getWeatherInfo(cityName);
            } else {
                cityName = "Chikhli";
                getWeatherInfo(cityName);
            }

            getWeatherInfo(cityName);
        }else {
            Toast.makeText(this, "Please Enable Your internet connection", Toast.LENGTH_LONG).show();
        }
        locationEnabled();
        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissKeyboard(MainActivity.this);
                String city=cityEdt.getText().toString();
                if(city.isEmpty()){
                    Toast.makeText(MainActivity.this, " Please Enter city Name", Toast.LENGTH_SHORT).show();
                }else{
                    cityNameTV.setText(cityName);
                    getWeatherInfo(city);
                }
            }
        });
    }

    public void dismissKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != activity.getCurrentFocus())
            imm.hideSoftInputFromWindow(activity.getCurrentFocus()
                    .getApplicationWindowToken(), 0);
    }


    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
    public void locationEnabled(){
        LocationManager lm = (LocationManager)getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {
            Log.v("XYZXYZ0",ex.toString());
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {
            Log.v("XYZXYZ1",ex.toString());
        }

        if(!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(this)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.open_location_settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            MainActivity.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.Cancel,null)
                    .show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==PERMISSION_CODE){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, " Permissions Granted..", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, " Please provide the permissions ", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double longitude, double latitude){
        String cityName="City nhi sapdli";
        Geocoder gcd=new Geocoder(MainActivity.this, Locale.getDefault());
        try{
            List<Address> addresses=gcd.getFromLocation(latitude,longitude,5);

            for(Address adr: addresses){
                if(adr!=null){
                    String city=adr.getLocality();
                   // String city=adr.getAddressLine(0);
                    if(city!=null && !city.equals("")){
                        Log.v("ABCABC11",city);
                        cityName=city;
                    }else{
                        Log.d("TAG"," CITY NOT FOUND");
                        Toast.makeText(this, " User City Not Found...", Toast.LENGTH_SHORT).show();

                    }
                }
            }

        }catch (IOException e){
            e.printStackTrace();
        }
        return  cityName;

    }

    private void getWeatherInfo(String cityName){

        Log.v("ABCABCABC",cityName);
        String url="https://api.weatherapi.com/v1/forecast.json?key=90c3abc253fc49dcb1c173643222004&q=" + cityName +"&days=1&aqi=yes&alerts=yes";
        Log.v("ABCABCABC",url);
        cityNameTV.setText(cityName);
        RequestQueue requestQueue= Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(JSONObject response) {
                Log.v("ERRORRESPONSE",response.toString());
                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModalArrayList.clear();

                try {
                    String temperature= response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature +" °c");
                    int isDay=response.getJSONObject("current").getInt("is_day");
                    String condition= response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon= response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("https:".concat(conditionIcon)).into(iconIV);
                    conditionTV.setText(condition);

                    if(isDay==1){
                        //morning
                        Picasso.get().load("https://images.unsplash.com/photo-1505533321630-975218a5f66f?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=1374&q=80").into(backIV);
                    }else{
                        Picasso.get().load("https://images.unsplash.com/photo-1505322022379-7c3353ee6291?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=387&q=80").into(backIV);
                    }

                    JSONObject forecastObj=response.getJSONObject("forecast");
                    JSONObject forecastO=forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray=forecastO.getJSONArray("hour");

                    for(int i=0; i< hourArray.length(); i++){
                        JSONObject hourObj=hourArray.getJSONObject(i);

                        String time=hourObj.getString("time");
                        String temper=hourObj.getString("temp_c");
                        String img=hourObj.getJSONObject("condition").getString("icon");
                        String wind=hourObj.getString("wind_kph");
                        weatherRVModalArrayList.add(new WeatherRVModal(time,temper,img,wind));
                    }
                    weatherRVAdapter.notifyDataSetChanged();


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("ERRORRESPONSE",error.toString());
                Toast.makeText(MainActivity.this,"please enter a valid city name", Toast.LENGTH_SHORT).show();
//              Toast.makeText(MainActivity.this,error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }
}