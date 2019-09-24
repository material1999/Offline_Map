package com.example.osm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint;

import java.io.File;


public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int PERMISSION_REQUEST_STORAGE = 1000;
    private static final int PERMISSION_REQUEST_LOCATION = 1000;
    private static final int PERMISSION_REQUEST_INTERNET = 1000;

    MapView map;
    TextView test_text;
    MyLocationNewOverlay myLocationOverlay;
    IMapController mapController;
    GpsMyLocationProvider provider;
    ScaleBarOverlay myScaleBarOverlay;


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.enableMyLocation(provider);
        myLocationOverlay.setDrawAccuracyEnabled(true);
        map.getOverlays().add(myLocationOverlay);
    }

    @Override
    public void onPause() {
        super.onPause();
        myLocationOverlay.disableFollowLocation();
        myLocationOverlay.disableMyLocation();
        myLocationOverlay.setDrawAccuracyEnabled(false);
        map.getOverlays().remove(myLocationOverlay);
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onLocationChanged(Location location) {
        GeoPoint center = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapController.animateTo(center);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context ctx = getApplicationContext();
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        }

        //Might be unnecessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_INTERNET);
        }


        map = (MapView) findViewById(R.id.mapview);
        test_text = (TextView) findViewById(R.id.test_text);


        //Configuration.getInstance().setOsmdroidBasePath((new File( "storage/emulated/0/osmdroid")));
        //Configuration.getInstance().setOsmdroidTileCache((new File("storage/emulated/0/osmdroid/tiles")));
        test_text.setText("For debugging: ");


        map.setUseDataConnection(false);
        XYTileSource tileSource = new XYTileSource(
                "4uMaps",
                10,
                15,
                256,
                ".png",
                new String[] {});
        map.setTileSource(tileSource);
        //map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMaxZoomLevel(15.0);
        map.setMinZoomLevel(10.0);
        map.setMultiTouchControls(true);
        map.setClickable(true);
        mapController = map.getController();
        mapController.setZoom(12.0);

        provider = new GpsMyLocationProvider(ctx);
        provider.addLocationSource(LocationManager.NETWORK_PROVIDER);
        myLocationOverlay = new MyLocationNewOverlay(provider, map);
        myLocationOverlay = new MyLocationNewOverlay(map);
        map.getOverlays().add(myLocationOverlay);

        myScaleBarOverlay = new ScaleBarOverlay(map);
        myScaleBarOverlay.setAlignBottom(true);
        myScaleBarOverlay.setAlignRight(true);
        myScaleBarOverlay.setTextSize(50);
        myScaleBarOverlay.setLineWidth(6);
        map.getOverlays().add(myScaleBarOverlay);

        GeoPoint startPoint = new GeoPoint(46.253, 20.1414);
        mapController.setCenter(startPoint);

        /*
        myLocationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
                mapController.animateTo(myLocationOverlay.getMyLocation());
            }
        });
        */

        GeoPoint testPoint = new GeoPoint(46.246708, 20.151135);
        Marker startMarker = new Marker(map);
        startMarker.setPosition(testPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
    }

}

