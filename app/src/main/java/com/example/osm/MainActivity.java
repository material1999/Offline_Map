package com.example.osm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.MotionEvent;
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
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;

import static org.osmdroid.tileprovider.util.StreamUtils.copy;


public class MainActivity extends AppCompatActivity {

    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDb;

    private static final int PERMISSION_REQUEST = 1;
    XYTileSource myTileSource;
    int search_results;
    MapView map;
    TextView test_text;
    Button center_button;
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        InfoWindow.closeAllInfoWindowsOn(map);
        return super.dispatchTouchEvent(ev);
    }

    public XYTileSource initializeMyMap() {
        File tileDir = Configuration.getInstance().getOsmdroidBasePath();
        File tileFile = new File(tileDir, "map.sqlite");
        if (!tileFile.exists()) {
            try {
                InputStream in = getAssets().open("map.sqlite");
                OutputStream out = new FileOutputStream(tileFile);
                copy(in, out);
                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
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
        return tileSource;
    }

    public void loadMyMap(XYTileSource tileSource) {
        map.setUseDataConnection(false);
        map.setTileSource(tileSource);
        map.setMaxZoomLevel(15.0);
        map.setMinZoomLevel(12.0);
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

    public int addMarkers(Cursor placesCursor, Cursor subcategoriesPlacesCursor, Cursor subcategoriesCursor) {
        int added = 0;
        String type;
        map.getOverlays().clear();
        placesCursor.moveToPosition(-1);
        while (placesCursor.moveToNext()) {
            Marker marker = new Marker(map);
            Double coordinatesX = placesCursor.getDouble(1);
            Double coordinatesY = placesCursor.getDouble(2);
            String name = placesCursor.getString(4);
            String description = placesCursor.getString(7);
            marker.setPosition(new GeoPoint(coordinatesX, coordinatesY));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(name);
            marker.setSnippet(description);
            subcategoriesPlacesCursor.moveToPosition(-1);
            type = "";
            while (subcategoriesPlacesCursor.moveToNext()) {
                if (placesCursor.getInt(0) == subcategoriesPlacesCursor.getInt(1)) {
                    subcategoriesCursor.moveToPosition(-1);
                    while (subcategoriesCursor.moveToNext()) {
                        if (subcategoriesCursor.getInt(4) == subcategoriesPlacesCursor.getInt(2)) {
                            type += subcategoriesCursor.getString(1) + ", ";
                        }
                    }
                }
            }
            type = type.substring(0, type.length() - 2);
            marker.setSubDescription(type);
            added++;
            map.getOverlays().add(marker);
        }
        return added;
    }

    public int addMarkers(Cursor placesCursor, Cursor subcategoriesPlacesCursor, Cursor subcategoriesCursor, int subcategory_id) {
        int added = 0;
        String type;
        map.getOverlays().clear();
        placesCursor.moveToPosition(-1);
        while (placesCursor.moveToNext()) {
            Marker marker = new Marker(map);
            subcategoriesPlacesCursor.moveToPosition(-1);
            type = "";
            while (subcategoriesPlacesCursor.moveToNext()) {
                if (placesCursor.getInt(0) == subcategoriesPlacesCursor.getInt(1) &&
                        subcategoriesPlacesCursor.getInt(2) == subcategory_id) {
                    Double coordinatesX = placesCursor.getDouble(1);
                    Double coordinatesY = placesCursor.getDouble(2);
                    String name = placesCursor.getString(4);
                    String description = placesCursor.getString(7);
                    subcategoriesCursor.moveToPosition(-1);
                    while (subcategoriesCursor.moveToNext()) {
                        if (subcategoriesCursor.getInt(4) == subcategoriesPlacesCursor.getInt(2)) {
                            type += subcategoriesCursor.getString(1) + ", ";
                        }
                    }
                    type = type.substring(0, type.length() - 2);
                    marker.setPosition(new GeoPoint(coordinatesX, coordinatesY));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setTitle(name);
                    marker.setSnippet(description);
                    marker.setSubDescription(type);
                }
            }
            map.getOverlays().add(marker);
            added++;
        }
        return added;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        c = getApplicationContext();
        Configuration.getInstance().load(c, PreferenceManager.getDefaultSharedPreferences(c));
        //Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        String[] permissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String p: permissions) {
                if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST);
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mDBHelper = new DatabaseHelper(this);
            try {
                mDBHelper.updateDataBase();
            } catch (IOException mIOException) {
                throw new Error("UnableToUpdateDatabase");
            }
            try {
                mDb = mDBHelper.getWritableDatabase();
            } catch (SQLException mSQLException) {
                throw mSQLException;
            }

            final Cursor placesCursor = mDBHelper.getCursor("places");
            final Cursor categoriesCursor = mDBHelper.getCursor("categories");
            final Cursor subcategoriesCursor = mDBHelper.getCursor("subcategories");
            final Cursor subcategoriesPlacesCursor = mDBHelper.getCursor("subcategoriesPlaces");

            myTileSource = initializeMyMap();
            setContentView(R.layout.activity_main);
            map = (MapView) findViewById(R.id.mapview);
            loadMyMap(myTileSource);
            initializeMyGPS();

            test_text = (TextView) findViewById(R.id.test_text);

            center_button = (Button) findViewById(R.id.center_button);
            center_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (myLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && myLocationOverlay.getMyLocation() != null) {
                        myLocationOverlay.enableFollowLocation();
                    }
                }
            });

            ////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////
            Button all = (Button) findViewById(R.id.all);
            all.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);
                    map.getOverlays().add(myScaleBarOverlay);
                    map.getOverlays().add(myLocationOverlay);
                    map.postInvalidate();
                    test_text.setText(Integer.toString(search_results));
                }
            });
            Button none = (Button) findViewById(R.id.none);
            none.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    search_results = 0;
                    map.getOverlays().clear();
                    map.getOverlays().add(myScaleBarOverlay);
                    map.getOverlays().add(myLocationOverlay);
                    map.postInvalidate();
                    test_text.setText(Integer.toString(search_results));
                }
            });
            Button kikoto = (Button) findViewById(R.id.kikoto);
            kikoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor, 1);
                    map.getOverlays().add(myScaleBarOverlay);
                    map.getOverlays().add(myLocationOverlay);
                    map.postInvalidate();
                    test_text.setText(Integer.toString(search_results));
                }
            });
            Button strand = (Button) findViewById(R.id.strand);
            strand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor, 2);
                    map.getOverlays().add(myScaleBarOverlay);
                    map.getOverlays().add(myLocationOverlay);
                    map.postInvalidate();
                    test_text.setText(Integer.toString(search_results));
                }
            });
            /*
            Button bolt = (Button) findViewById(R.id.bolt);
            bolt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    search_results = addMarkers(cursor, "bolt");
                    map.getOverlays().add(myScaleBarOverlay);
                    map.getOverlays().add(myLocationOverlay);
                    map.postInvalidate();
                    test_text.setText(Integer.toString(search_results));
                }
            });
            */
            ////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////

            myScaleBarOverlay = new ScaleBarOverlay(map);
            myScaleBarOverlay.setAlignBottom(true);
            myScaleBarOverlay.setAlignRight(true);
            myScaleBarOverlay.setMaxLength(2);
            myScaleBarOverlay.setScaleBarOffset(50,50);
            myScaleBarOverlay.setTextSize(60);
            myScaleBarOverlay.setLineWidth(7);

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

            //map.setScrollableAreaLimitDouble(map.getBoundingBox());
            //map.computeScroll();

            //test_text.setText(Double.toString(map.getBoundingBox().getLonEast()));
            map.setScrollableAreaLimitLatitude(46.55, 46.08, 0);
            map.setScrollableAreaLimitLongitude(19.78, 20.38, 0);

            GeoPoint startPoint = new GeoPoint(46.253, 20.1414);
            mapController.setCenter(startPoint);

            search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);
            test_text.setText(Integer.toString(search_results));

            /*
            File f = new File(c.getApplicationInfo().dataDir + "/databases/");
            File[] paths = f.listFiles();
            for (File path : paths) {
                System.out.println(path);
            }
            */

            map.getOverlays().add(myScaleBarOverlay);
            map.getOverlays().add(myLocationOverlay);

            /*
            RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(map);
            rotationGestureOverlay.setEnabled(true);
            map.getOverlays().add(rotationGestureOverlay);
            */


        } else {
            System.exit(1);
        }
    }
}