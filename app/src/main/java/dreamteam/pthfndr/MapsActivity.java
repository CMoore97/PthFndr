package dreamteam.pthfndr;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import dreamteam.pthfndr.models.FirebaseAccessor;
import dreamteam.pthfndr.models.Path;
import dreamteam.pthfndr.models.Trip;
import dreamteam.pthfndr.models.User;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static MapsActivity thisRef;
    boolean isActive = false;
    long time = 0;
    public User currentUser;
    public GoogleMap mMap;
    private boolean mLocationPermissionGranted;
    private DrawerLayout mDrawerLayout;
    private LocServices ls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String[] keys = getIntent().getExtras().keySet().toArray(new String[1]);
        currentUser = getIntent().getExtras().getParcelable(keys[0]);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        thisRef = this;
        currentUser = FirebaseAccessor.getUserModel();
        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                (MenuItem menuItem) -> {
                    menuItem.setChecked(true);

                    mDrawerLayout.closeDrawers();

                    if (menuItem.getItemId() == R.id.nav_Profile) {
                        Intent newIntent = new Intent(this, ProfileActivity.class);
                        newIntent.putExtra("user", currentUser);
                        startActivity(newIntent);
                    } else if (menuItem.getItemId() == R.id.nav_SignOut) {
                        FirebaseAccessor.logout();
                        Intent intent = new Intent(this, SigninActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("user", currentUser);
                        startActivity(intent);
                    } else if (menuItem.getItemId() == R.id.nav_Mpgs) {
                        Intent newIntent = new Intent(this, MpgCalcActivity.class);
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        newIntent.putExtra("user", currentUser);
                        startActivity(newIntent);
                    } else if (menuItem.getItemId() == R.id.nav_History) {
                        Intent newIntent = new Intent(this, TripHistoryActivity.class);
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        newIntent.putExtra("user", currentUser);
                        startActivity(newIntent);
                    } //else if (menuItem.getItemId() == R.id.nav_Map) {
//                        Intent newIntent = new Intent(this, MapsActivity.class);
//                        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        startActivity(newIntent);
//                    }
                    return true;
                });
    }//keep here

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        ls = new LocServices(currentUser, mMap);
        if (currentUser != null) {
            for (Trip t : currentUser.getTrips()) {
                for (Path p : t.getPaths()) {
                    Polyline l = mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(p.getEndLocation().getLatitude(), p.getEndLocation().getLongitude()), new LatLng(p.getStartLocation().getLatitude(), p.getStartLocation().getLongitude()))
                            .color(p.getColor())
                    );
                    if (p.getPl() != null) {
                        l.setWidth(p.getPl().getWidth());
                    } else {
                        l.setWidth(5);
                    }
                }
            }
        } else {
            // if the map is ready but we still don't have the user model
            // manually get it
            if (getIntent().getExtras() != null) {
                currentUser = getIntent().getExtras().getParcelable("user");
            }
        }

        getLocationPermission();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setBuildingsEnabled(true);

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        ls.cLoc = location;
        ls.latitude = location.getLatitude();
        ls.longitude = location.getLongitude();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, ls.getLocationListener());

        time = System.currentTimeMillis();
    }//keep here

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }//keep here

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                }
            }
        }
    }//keep here

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void signOut(View view) {
        Intent i = new Intent(this, ProfileActivity.class);
        User user = getIntent().getExtras().getParcelable("user");
        i.putExtra("user", user);
        startActivity(i);
    }//keep here

    public void manageTrip(View view) {
        isActive = !isActive;
        ls.isActive = isActive;

        FloatingActionButton b = findViewById(R.id.TripButton);
        if (isActive) {
            if (ls.trip == null) {
                ls.NewTrip();
            }
            b.setImageResource(R.drawable.stopsign);
        } else {
            mMap.clear();
            for (Trip t : currentUser.getTrips()) {
                for (Path p : t.getPaths()) {
                    if (p.getStartLocation() != null && p.getEndLocation() != null) {
                        Polyline l = mMap.addPolyline(new PolylineOptions()
                                .add(new LatLng(p.getEndLocation().getLatitude(), p.getEndLocation().getLongitude()), new LatLng(p.getStartLocation().getLatitude(), p.getStartLocation().getLongitude()))
                        );
                        if (p.getPl() != null) {
                            l.setWidth(p.getPl().getWidth());
                            l.setColor(p.getPl().getColor());
                        } else {
                            l.setWidth(5);
                            l.setColor(Color.BLACK);
                        }
                    }
                }
            }
            ls.trip.endTrip();
            currentUser.addTrip(ls.trip);
            ls.NewTrip();
            b.setImageResource(R.drawable.map);
            updateUser();
        }
    }

    private void updateUser() {
        if (FirebaseAccessor.updateUserModel(currentUser)) {
            Toast.makeText(thisRef, "Trip Saved!", Toast.LENGTH_LONG).show();
        }
        mMap.clear();
        for (Trip t : currentUser.getTrips()) {
            for (Path p : t.getPaths()) {
                if (p.getStartLocation() != null && p.getEndLocation() != null) {
                    Polyline l = mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(p.getEndLocation().getLatitude(), p.getEndLocation().getLongitude()), new LatLng(p.getStartLocation().getLatitude(), p.getStartLocation().getLongitude()))
                            .color(p.getPl().getColor())
                            .width(p.getPl().getWidth())
                    );
                    if (p.getPl() != null) {
                        l.setWidth(p.getPl().getWidth());
                    } else {
                        l.setWidth(5);
                    }
                }
            }
        }
    }
}