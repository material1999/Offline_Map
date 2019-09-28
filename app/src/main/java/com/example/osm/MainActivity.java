package com.example.osm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 1;

    MapView map;
    TextView test_text;
    Button test_button;
    Context myContext;
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
            test_text.setText("enabled");
            myLocationOverlay.enableFollowLocation();
            myLocationOverlay.enableMyLocation(myLocationProvider);
            map.getOverlays().add(1, myLocationOverlay);
        }

        @Override
        public void onProviderDisabled(String s) {
            test_text.setText("disabled");
            myLocationOverlay.disableFollowLocation();
            myLocationOverlay.disableMyLocation();
            map.getOverlays().remove(myLocationOverlay);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        test_text.setText("resume");
        //myLocationOverlay.enableFollowLocation();
        myLocationOverlay.enableMyLocation(myLocationProvider);
        map.getOverlays().add(1, myLocationOverlay);
    }

    @Override
    public void onPause() {
        super.onPause();
        test_text.setText("pause");
        myLocationOverlay.disableFollowLocation();
        myLocationOverlay.disableMyLocation();
        map.getOverlays().remove(myLocationOverlay);
    }

    public void initializeMyMap () {
        map.setUseDataConnection(false);
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

    public void initializeMyGPS() {
        myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
        myLocationProvider = new GpsMyLocationProvider(myContext);
        myLocationOverlay = new MyLocationNewOverlay(myLocationProvider, map);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myContext = getApplicationContext();
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(myContext, PreferenceManager.getDefaultSharedPreferences(myContext));

        Configuration.getInstance().setOsmdroidBasePath((new File( "storage/emulated/0/osmdroid")));
        Configuration.getInstance().setOsmdroidTileCache((new File("storage/emulated/0/osmdroid/tiles")));

        String[] permissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String p: permissions) {
                if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST);
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            test_text = (TextView) findViewById(R.id.test_text);
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

            GeoPoint testPoint = new GeoPoint(46.246708, 20.151135);
            Marker startMarker = new Marker(map);
            startMarker.setPosition(testPoint);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(0, startMarker);

        } else {
            System.exit(1);
        }
    }
}

