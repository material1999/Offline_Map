package com.example.osm;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
import java.util.Arrays;
import java.util.List;

import static org.osmdroid.tileprovider.util.StreamUtils.copy;


public class MainActivity extends AppCompatActivity {

    static final int PERMISSION_REQUEST = 1;
    public static final String SHARED_PREFERENCES = "sharedPreferences";
    public static final String LANGUAGE = "language";
    int language; // 0-HUN, 1-ENG, 2-SRB
    Bundle mySavedInstanceState;
    DatabaseHelper databaseHelper;
    Cursor placesCursor, categoriesCursor, subcategoriesCursor, subcategoriesPlacesCursor;
    int chosenId = 0;
    ArrayList<Integer> showPlaces = new ArrayList<>();
    XYTileSource tileSource;
    int search_results;
    MapView map;
    Marker marker;
    TextView test_text;
    Button center_button, all_button, none_button, search_button;
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
        map.getOverlays().clear();
        placesCursor.moveToPosition(-1);
        while (placesCursor.moveToNext()) {
            StringBuilder stringBuilder = new StringBuilder();
            subcategoriesPlacesCursor.moveToPosition(-1);
            while (subcategoriesPlacesCursor.moveToNext()) {
                subcategoriesCursor.moveToPosition(-1);
                while (subcategoriesCursor.moveToNext()) {
                    if (placesCursor.getInt(0) == subcategoriesPlacesCursor.getInt(1) &&
                            subcategoriesPlacesCursor.getInt(2) == subcategoriesCursor.getInt(0)) {
                        stringBuilder.append(subcategoriesCursor.getString(1 + language) + ", ");
                    }
                }
            }
            if (!stringBuilder.toString().equals("")) {
                stringBuilder.setLength(stringBuilder.length() - 2);
            }
            final String type = stringBuilder.toString();
            subcategoriesPlacesCursor.moveToPosition(-1);
            while (subcategoriesPlacesCursor.moveToNext()) {
                if (placesCursor.getInt(0) == subcategoriesPlacesCursor.getInt(1) &&
                        showPlaces.contains(subcategoriesPlacesCursor.getInt(2))) {
                    Integer id = placesCursor.getInt(0);
                    Double coordinatesX = placesCursor.getDouble(1);
                    Double coordinatesY = placesCursor.getDouble(2);
                    final String name = placesCursor.getString(4 + language);
                    final String description = placesCursor.getString(7 + language);
                    marker = new Marker(map) {
                        @Override
                        public boolean onLongPress(MotionEvent event, MapView mapView) {
                            boolean touched = hitTest(event, map);
                            if (touched) {
                                test_text.setText(this.getId() + " long");
                                chosenId = Integer.parseInt(this.getId());
                                mapController.animateTo(this.getPosition());
                                class MyInfoWindow extends DialogFragment {
                                    @Override
                                    public Dialog onCreateDialog(Bundle savedInstanceState) {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                        View view = View.inflate(context, R.layout.infowindow, null);
                                        builder.setView(view);
                                        builder.setNegativeButton((language == 0) ? "Bezárás" : (language == 1) ? "Close" : "[szerb]",
                                                new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {}
                                        });
                                        TextView descriptionText = view.findViewById(R.id.description);
                                        descriptionText.setText(description);
                                        TextView titleText = view.findViewById(R.id.title);
                                        titleText.setText(name);
                                        TextView typeText = view.findViewById(R.id.type);
                                        typeText.setText(type);
                                        builder.show();
                                        return builder.create();
                                    }
                                }
                                MyInfoWindow myInfoWindow = new MyInfoWindow();
                                myInfoWindow.onCreateDialog(mySavedInstanceState);
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
                    marker.setSnippet(type);
                    switch (language) {
                        case 0:
                            marker.setSubDescription("További információkért nyomjon hosszan a megfelelő pin-re!");
                            break;
                        case 1:
                            marker.setSubDescription("For more information, please long press the desired pin!");
                            break;
                        case 2:
                            marker.setSubDescription("[szerb]");
                            break;
                    }
                    added++;
                    map.getOverlays().add(marker);
                    break;
                }
            }
        }
        return added;
    }

    public void setLanguage(int language) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(LANGUAGE, language);
        editor.apply();
    }

    public int getLanguage() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        return sharedPreferences.getInt(LANGUAGE, 1);
    }

    public void renameUI() {
        switch (language) {
            case 0:
                center_button.setText("Középre");
                all_button.setText("Minden");
                none_button.setText("Semmi");
                search_button.setText("Keresés");
                break;
            case 1:
                center_button.setText("Center");
                all_button.setText("All");
                none_button.setText("None");
                search_button.setText("Search");
                break;
            case 2:
                center_button.setText("[szerb]");
                all_button.setText("[szerb]");
                none_button.setText("[szerb]");
                search_button.setText("[szerb]");
                break;
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        mySavedInstanceState = savedInstanceState;

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

            language = getLanguage();

            tileSource = initializeMyMap();
            setContentView(R.layout.activity_main);
            map = findViewById(R.id.mapview);

            loadMyMap(tileSource);
            initializeMyGPS();

            test_text = findViewById(R.id.test_text);

            ////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////
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
            all_button = findViewById(R.id.all);
            all_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    chosenId = 0;
                    subcategoriesCursor.moveToPosition(-1);
                    showPlaces.clear();
                    while (subcategoriesCursor.moveToNext()) {
                        showPlaces.add(subcategoriesCursor.getInt(0));
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
                    showPlaces.clear();
                    search_results = 0;
                    map.getOverlays().clear();
                    map.getOverlays().add(locationOverlay);
                    map.getOverlays().add(scaleBarOverlay);
                    map.postInvalidate();
                    test_text.setText(Integer.toString(search_results));
                }
            });
            search_button = findViewById(R.id.search_button);
            search_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    class MySearchDialog extends DialogFragment {
                        @Override
                        public Dialog onCreateDialog(Bundle savedInstanceState) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            View view = View.inflate(context, R.layout.search, null);
                            builder.setView(view);
                            builder.setTitle((language == 0) ? "Keresés" : (language == 1) ? "Search" : "[szerb]");
                            TextView searchText = view.findViewById(R.id.search_text);
                            searchText.setText((language == 0) ? "Kérem válassza ki a megjeleníteni kívánt kategóriákat:" :
                                (language == 1) ? "Please choose the categories you want to be displayed:" :
                                "[szerb]");
                            subcategoriesCursor.moveToPosition(-1);
                            int counter = 0;
                            while (subcategoriesCursor.moveToNext()) {
                                counter++;
                            }
                            final int subcategoryCounter = counter;
                            final String[] allSubcategories = new String[subcategoryCounter];
                            subcategoriesCursor.moveToPosition(-1);
                            for (int i = 0; i < subcategoryCounter; i++) {
                                subcategoriesCursor.moveToNext();
                                allSubcategories[i] = subcategoriesCursor.getString(1 + language);
                            }
                            final ListView listView = view.findViewById(R.id.list);
                            final List<String> list = new ArrayList<>(Arrays.asList(allSubcategories));
                            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context,
                                    android.R.layout.simple_list_item_multiple_choice, list);
                            listView.setAdapter(arrayAdapter);
                            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                            builder.setNegativeButton((language == 0) ? "Bezárás" : (language == 1) ? "Close" : "[szerb]",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {}
                                    });
                            builder.setPositiveButton((language == 0) ? "Keresés" : (language == 1) ? "Search" : "[szerb]",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            showPlaces.clear();
                                            SparseBooleanArray sparseBooleanArray = listView.getCheckedItemPositions();
                                            for (int i = 0; i < subcategoryCounter; i++) {
                                                if (sparseBooleanArray.get(i)) {
                                                    showPlaces.add(i + 1);
                                                }
                                            }
                                            chosenId = 0;
                                            search_results = addMarkers(placesCursor, subcategoriesPlacesCursor, subcategoriesCursor);
                                            map.getOverlays().add(locationOverlay);
                                            map.getOverlays().add(scaleBarOverlay);
                                            map.postInvalidate();
                                            test_text.setText(Integer.toString(search_results));
                                        }
                                    });
                            builder.setCancelable(false);
                            builder.show();
                            return builder.create();
                        }
                    }
                    MySearchDialog mySearchDialog = new MySearchDialog();
                    mySearchDialog.onCreateDialog(mySavedInstanceState);
                }
            });
            Button hun = findViewById(R.id.hun);
            hun.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setLanguage(0);
                    language = getLanguage();
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
                    language = getLanguage();
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
            showPlaces.clear();
            while (subcategoriesCursor.moveToNext()) {
                showPlaces.add(subcategoriesCursor.getInt(0));
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