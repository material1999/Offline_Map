package com.example.osm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static org.osmdroid.tileprovider.util.StreamUtils.copy;


public class MainActivity extends AppCompatActivity {

    DatabaseHelper databaseHelper;
    Cursor placesCursor;
    Cursor categoriesCursor;
    Cursor subcategoriesCursor;
    Cursor subcategoriesPlacesCursor;
    Cursor settingsCursor;
    int language; // 0-HUN, 1-ENG, 2-SRB
    int chosenId = 0;
    ArrayList<Integer> show = new ArrayList<>();
    static final int PERMISSION_REQUEST = 1;
    XYTileSource tileSource;
    int search_results;
    MapView map;
    Marker marker;
    TextView test_text;
    Button center_button, all_button, none_button, kikoto, strand;
    Context context;
    MyLocationNewOverlay locationOverlay;
    LocationManager locationManager;
    IMapController mapController;
    GpsMyLocationProvider locationProvider;
    ScaleBarOverlay scaleBarOverlay;
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {}

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {}

        @Override
        public void onProviderEnabled(String s) {
            locationOverlay.enableFollowLocation();
            locationOverlay.enableMyLocation(locationProvider);
            map.getOverlays().add(locationOverlay);
            map.getOverlays().remove(scaleBarOverlay);
            map.getOverlays().add(scaleBarOverlay);
        }

        @Override
        public void onProviderDisabled(String s) {
            locationOverlay.disableFollowLocation();
            locationOverlay.disableMyLocation();
            map.getOverlays().remove(locationOverlay);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        locationOverlay.enableMyLocation(locationProvider);
        map.getOverlays().add(locationOverlay);
        map.getOverlays().remove(scaleBarOverlay);
        map.getOverlays().add(scaleBarOverlay);
    }

    @Override
    public void onPause() {
        super.onPause();
        locationOverlay.disableFollowLocation();
        locationOverlay.disableMyLocation();
        map.getOverlays().remove(locationOverlay);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);
        map.getOverlays().add(locationOverlay);
        map.getOverlays().add(scaleBarOverlay);
        InfoWindow.closeAllInfoWindowsOn(map);
        map.postInvalidate();
        return super.dispatchTouchEvent(event);
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
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationProvider = new GpsMyLocationProvider(context);
        locationOverlay = new MyLocationNewOverlay(locationProvider, map);
    }

    public int addMarkers(final Cursor placesCursor, final Cursor subcategoriesPlacesCursor, final Cursor subcategoriesCursor) {
        int added = 0;
        String type;
        map.getOverlays().clear();
        placesCursor.moveToPosition(-1);
        while (placesCursor.moveToNext()) {
            type = "";
            subcategoriesPlacesCursor.moveToPosition(-1);
            while (subcategoriesPlacesCursor.moveToNext()) {
                subcategoriesCursor.moveToPosition(-1);
                while (subcategoriesCursor.moveToNext()) {
                    if (placesCursor.getInt(0) == subcategoriesPlacesCursor.getInt(1) &&
                            subcategoriesPlacesCursor.getInt(2) == subcategoriesCursor.getInt(0)) {
                        type += subcategoriesCursor.getString(1 + language) + ", ";
                    }
                }
            }
            if (!type.equals("")) type = type.substring(0, type.length() - 2);
            subcategoriesPlacesCursor.moveToPosition(-1);
            while (subcategoriesPlacesCursor.moveToNext()) {
                if (placesCursor.getInt(0) == subcategoriesPlacesCursor.getInt(1) &&
                        show.contains(subcategoriesPlacesCursor.getInt(2))) {
                    Integer id = placesCursor.getInt(0);
                    Double coordinatesX = placesCursor.getDouble(1);
                    Double coordinatesY = placesCursor.getDouble(2);
                    String name = placesCursor.getString(4 + language);
                    String description = placesCursor.getString(7 + language);
                    marker = new Marker(map) {
                        @Override
                        public boolean onLongPress(MotionEvent event, MapView mapView) {
                            boolean touched = hitTest(event, map);
                            if (touched) {
                                test_text.setText(this.getId() + " long");
                                chosenId = Integer.parseInt(this.getId());
                                mapController.animateTo(this.getPosition());
                                return super.onLongPress(event, map);
                            } else {
                                return false;
                            }
                        }
                    };
                    marker.setPosition(new GeoPoint(coordinatesX, coordinatesY));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    if (chosenId == id) {
                        marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marker_chosen, null));
                    } else {
                        marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marker, null));
                    }
                    marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker, MapView mapView) {
                            if (Integer.parseInt(marker.getId()) == chosenId) {
                                chosenId = 0;
                                marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marker, null));
                            } else {
                                marker.showInfoWindow();
                                chosenId = Integer.parseInt(marker.getId());
                                marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marker_chosen, null));
                                mapController.animateTo(marker.getPosition());
                            }
                            map.getOverlays().clear();
                            addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);
                            map.getOverlays().add(locationOverlay);
                            map.getOverlays().add(scaleBarOverlay);
                            map.postInvalidate();
                            test_text.setText(marker.getId());
                            return false;
                        }
                    });
                    marker.setId(id.toString());
                    marker.setTitle(name);
                    marker.setSnippet(description);
                    marker.setSubDescription(type);
                    added++;
                    map.getOverlays().add(marker);
                    break;
                }
            }
        }
        return added;
    }

    public void setLanguage(int language) {
        databaseHelper.setLanguage(language);
    }

    public int getLanguage(Cursor settingsCursor) {
        settingsCursor.moveToPosition(0);
        return settingsCursor.getInt(1);
    }

    public void renameUI() {
        switch (language) {
            case 0:
                center_button.setText("Középre");
                all_button.setText("Minden");
                none_button.setText("Semmi");
                break;
            case 1:
                center_button.setText("Center");
                all_button.setText("All");
                none_button.setText("None");
                break;
            case 2:
                center_button.setText("[szerb]");
                all_button.setText("[szerb]");
                none_button.setText("[szerb]");
                break;
        }
        subcategoriesCursor.moveToPosition(0);
        kikoto.setText(subcategoriesCursor.getString(1 + language));
        subcategoriesCursor.moveToNext();
        strand.setText(subcategoriesCursor.getString(1 + language));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        //Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        String[] permissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission: permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST);
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            databaseHelper = new DatabaseHelper(this);
            try {
                databaseHelper.updateDataBase();
            } catch (IOException mIOException) {
                throw new Error("Unable to update database!");
            }

            placesCursor = databaseHelper.getCursor("places");
            categoriesCursor = databaseHelper.getCursor("categories");
            subcategoriesCursor = databaseHelper.getCursor("subcategories");
            subcategoriesPlacesCursor = databaseHelper.getCursor("subcategoriesPlaces");
            settingsCursor = databaseHelper.getCursor("settings");

            language = getLanguage(settingsCursor);

            tileSource = initializeMyMap();
            setContentView(R.layout.activity_main);
            map = findViewById(R.id.mapview);

            loadMyMap(tileSource);
            initializeMyGPS();

            test_text = findViewById(R.id.test_text);

            center_button = findViewById(R.id.center_button);
            center_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationOverlay.getMyLocation() != null) {
                        locationOverlay.enableFollowLocation();
                    }
                    map.getOverlays().remove(scaleBarOverlay);
                    map.getOverlays().add(scaleBarOverlay);
                }
            });
            ////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////
            all_button = findViewById(R.id.all);
            all_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    chosenId = 0;
                    subcategoriesCursor.moveToPosition(-1);
                    show.clear();
                    while (subcategoriesCursor.moveToNext()) {
                        show.add(subcategoriesCursor.getInt(0));
                    }
                    search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);
                    map.getOverlays().add(locationOverlay);
                    map.getOverlays().add(scaleBarOverlay);
                    map.postInvalidate();
                    test_text.setText(Integer.toString(search_results));
                }
            });
            none_button = findViewById(R.id.none);
            none_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    chosenId = 0;
                    show.clear();
                    search_results = 0;
                    map.getOverlays().clear();
                    map.getOverlays().add(locationOverlay);
                    map.getOverlays().add(scaleBarOverlay);
                    map.postInvalidate();
                    test_text.setText(Integer.toString(search_results));
                }
            });
            kikoto = findViewById(R.id.kikoto);
            kikoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    chosenId = 0;
                    show.clear();
                    show.add(1);
                    search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);
                    map.getOverlays().add(locationOverlay);
                    map.getOverlays().add(scaleBarOverlay);
                    map.postInvalidate();
                    test_text.setText(Integer.toString(search_results));
                }
            });
            strand = findViewById(R.id.strand);
            strand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    chosenId = 0;
                    show.clear();
                    show.add(2);
                    search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);
                    map.getOverlays().add(locationOverlay);
                    map.getOverlays().add(scaleBarOverlay);
                    map.postInvalidate();
                    test_text.setText(Integer.toString(search_results));
                }
            });
            Button hun = findViewById(R.id.hun);
            hun.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setLanguage(0);
                    settingsCursor = databaseHelper.getCursor("settings");
                    language = getLanguage(settingsCursor);
                    search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);
                    map.getOverlays().add(locationOverlay);
                    map.getOverlays().add(scaleBarOverlay);
                    map.postInvalidate();
                    renameUI();
                    test_text.setText(Integer.toString(language));
                }
            });
            Button eng = findViewById(R.id.eng);
            eng.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setLanguage(1);
                    settingsCursor = databaseHelper.getCursor("settings");
                    language = getLanguage(settingsCursor);
                    search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);
                    map.getOverlays().add(locationOverlay);
                    map.getOverlays().add(scaleBarOverlay);
                    map.postInvalidate();
                    renameUI();
                    test_text.setText(Integer.toString(language));
                }
            });
            ////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////

            scaleBarOverlay = new ScaleBarOverlay(map);
            scaleBarOverlay.setAlignBottom(true);
            scaleBarOverlay.setAlignRight(true);
            scaleBarOverlay.setMaxLength(2);
            scaleBarOverlay.setScaleBarOffset(50,50);
            scaleBarOverlay.setTextSize(60);
            scaleBarOverlay.setLineWidth(7);

            map.addMapListener(new MapListener() {
                @Override
                public boolean onScroll(ScrollEvent event) {
                    locationOverlay.disableFollowLocation();
                    return true;
                }

                @Override
                public boolean onZoom(ZoomEvent event) {
                    locationOverlay.disableFollowLocation();
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

            renameUI();

            subcategoriesCursor.moveToPosition(-1);
            show.clear();
            while (subcategoriesCursor.moveToNext()) {
                show.add(subcategoriesCursor.getInt(0));
            }
            search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);

            test_text.setText(Integer.toString(search_results));

            map.getOverlays().add(locationOverlay);
            map.getOverlays().add(scaleBarOverlay);

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