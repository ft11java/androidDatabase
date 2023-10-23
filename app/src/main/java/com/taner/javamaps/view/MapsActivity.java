package com.taner.javamaps.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.taner.javamaps.R;
import com.taner.javamaps.databinding.ActivityMapsBinding;
import com.taner.javamaps.model.Place;
import com.taner.javamaps.roomdb.PlaceDao;
import com.taner.javamaps.roomdb.PlaceDatabase;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ActivityResultLauncher<String> permissionLauncher;

    LocationManager locationManager;
    LocationListener locationListener;

    SharedPreferences sharedPreferences;
    boolean info;

    PlaceDatabase db;
    PlaceDao placeDao;

    Double selectedLatitude;
    Double selectedLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        registerLauncher();
        sharedPreferences= MapsActivity.this.getSharedPreferences("package com.taner.javamaps",MODE_PRIVATE);
        info=false;

        db= Room.databaseBuilder(getApplicationContext(),PlaceDatabase.class,"Places")
               // .allowMainThreadQueries()
                .build();
        placeDao=db.placeDao();
        selectedLatitude=0.0;
        selectedLongitude=0.0;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        binding.saveButton.setEnabled(false);

         locationManager=(LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
         locationListener=new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                info=sharedPreferences.getBoolean("info",false);
                if (!info){
                    LatLng userLocation=new LatLng(location.getLatitude(),location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));
                    sharedPreferences.edit().putBoolean("info",true).apply();
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                LocationListener.super.onStatusChanged(provider, status, extras);
            }
        };

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            //request permission
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.getRoot(),"Permission need for maps",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    //request permission
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                }).show();
            }else {
                //request permission
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);

            }
        }else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
            Location lastLocation= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(lastLocation!=null){
                LatLng lastUserLocation=new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));
            }
        }

        mMap.setMyLocationEnabled(true);

    }

    private void registerLauncher(){
        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
            if (result) {
                //permission granted
                if(ContextCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);

                    Location lastLocation= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if(lastLocation!=null){
                        LatLng lastUserLocation=new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));
                    }
                }

            }else {
                Toast.makeText(MapsActivity.this,"Permission needed",Toast.LENGTH_LONG).show();
            }
            }
        });
    }


    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));
        selectedLongitude=latLng.longitude;
        selectedLatitude=latLng.latitude;
        binding.saveButton.setEnabled(true);
    }

    public void save(View view){

        Place place=new Place(binding.placeNameText.getText().toString(),selectedLatitude,selectedLongitude);
        placeDao.insert(place);
    }

    public void delete(View view){

    }
}