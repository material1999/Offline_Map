package com.example.osm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

import static org.osmdroid.tileprovider.util.StreamUtils.copy;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 1;

    Database myDatabase;
    MapView map;
    TextView test_text;
    Button test_button;
    Context c;
    MyLocationNewOverlay myLocationOverlay;
    LocationManager myLocationManager;
    IMapController mapController;
    GpsMyLocationProvider myLocationProvider;
    ScaleBarOverlay myScaleBarOverlay;
    LocationListener myLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {}

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {}

        @Override
        public void onProviderEnabled(String s) {
            myLocationOverlay.enableFollowLocation();
            myLocationOverlay.enableMyLocation(myLocationProvider);
            map.getOverlays().add(1, myLocationOverlay);
        }

        @Override
        public void onProviderDisabled(String s) {
            myLocationOverlay.disableFollowLocation();
            myLocationOverlay.disableMyLocation();
            map.getOverlays().remove(myLocationOverlay);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        myLocationOverlay.enableMyLocation(myLocationProvider);
        map.getOverlays().add(1, myLocationOverlay);
    }

    @Override
    public void onPause() {
        super.onPause();
        myLocationOverlay.disableFollowLocation();
        myLocationOverlay.disableMyLocation();
        map.getOverlays().remove(myLocationOverlay);
    }

    public void initializeMyMap () {
        map.setUseDataConnection(false);
        File tileDir = Configuration.getInstance().getOsmdroidBasePath();
        File tileZipFile = new File(tileDir, "test_map.zip");
        if (!tileZipFile.exists()){
            try{
                InputStream in = getAssets().open("test_map.zip");
                OutputStream out = new FileOutputStream(tileZipFile);
                copy(in,out);
                in.close();
                //in = null;
                out.flush();
                out.close();
                //out = null;
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        XYTileSource tileSource = new XYTileSource(
                "4uMaps",
                10,
                15,
                256,
                ".png",
                new String[]{});
        map.setTileSource(tileSource);
        map.setMaxZoomLevel(15.0);
        map.setMinZoomLevel(10.0);
        map.setMultiTouchControls(true);
        map.setClickable(true);
        mapController = map.getController();
        mapController.setZoom(12.0);
    }

    @SuppressLint("MissingPermission")
    public void initializeMyGPS() {
        myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
        myLocationProvider = new GpsMyLocationProvider(c);
        myLocationOverlay = new MyLocationNewOverlay(myLocationProvider, map);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        c = getApplicationContext();
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(c, PreferenceManager.getDefaultSharedPreferences(c));

        test_text = (TextView) findViewById(R.id.test_text);
        String fileName = "data.csv";
        myDatabase = new Database(this);
        SQLiteDatabase db = null;
        try {
            InputStreamReader is = new InputStreamReader(getAssets().open(fileName));
            db = myDatabase.addData(is, c);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Cursor cursor = myDatabase.getCursor();

        String[] permissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String p: permissions) {
                if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST);
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            map = (MapView) findViewById(R.id.mapview);
            test_button = (Button) findViewById(R.id.test_button);
            test_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (myLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && myLocationOverlay.getMyLocation() != null) {
                        myLocationOverlay.enableFollowLocation();
                    }
                }
            });

            myScaleBarOverlay = new ScaleBarOverlay(map);
            myScaleBarOverlay.setAlignBottom(true);
            myScaleBarOverlay.setAlignRight(true);
            myScaleBarOverlay.setMaxLength(2);
            myScaleBarOverlay.setScaleBarOffset(50,50);
            myScaleBarOverlay.setTextSize(60);
            myScaleBarOverlay.setLineWidth(7);
            map.getOverlays().add(myScaleBarOverlay);

            initializeMyMap();
            initializeMyGPS();

            map.addMapListener(new MapListener() {
                @Override
                public boolean onScroll(ScrollEvent event) {
                    myLocationOverlay.disableFollowLocation();
                    return true;
                }

                @Override
                public boolean onZoom(ZoomEvent event) {
                    myLocationOverlay.disableFollowLocation();
                    return false;
                }
            });

            GeoPoint startPoint = new GeoPoint(46.253, 20.1414);
            mapController.setCenter(startPoint);

            while (cursor.moveToNext()) {
                String name = cursor.getString(1);
                Double coordinatesX = cursor.getDouble(2);
                Double coordinatesY = cursor.getDouble(3);
                String type = cursor.getString(4);
                String description = cursor.getString(5);
                Marker marker = new Marker(map);
                marker.setPosition(new GeoPoint(coordinatesX, coordinatesY));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);











                map.getOverlays().add(marker);
            }
            map.getOverlays().add(myLocationOverlay);
        } else {
            System.exit(1);
        }
    }
}

