package com.example.womensafetyapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class AlertActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private TextView mWelcomeUser;
    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager mLocationManager;

    private LocationRequest mLocationRequest;
    private com.google.android.gms.location.LocationListener listener;
    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 6000; /* 6 sec */
    static public final int REQUEST_LOCATION = 1;

    private LocationManager locationManager;
    private LatLng latLng;

    FirebaseFirestore db;
    FirebaseAuth fAuth;
    DocumentReference dRef;
    FirebaseUser fb_user;

    private GoogleMap mMap;

    private ListView listView;
    private Button emergency, add_number;

    private LayoutInflater li;
    private String itemValue;
    ContactListAdapter adapter;
    private Map<String, String> newContacts;
    private ArrayList<AllContacts> contactsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        Log.d("AlertActivity", "Inside Oncreate");

        db = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        fb_user = fAuth.getCurrentUser();
        dRef = db.collection("users").document(fb_user.getEmail());

        mWelcomeUser = findViewById(R.id.welcome_user);
        mLatitudeTextView = findViewById((R.id.latitude_textview));
        mLongitudeTextView = findViewById((R.id.longitude_textview));
        listView = findViewById(R.id.list);
        emergency = findViewById(R.id.btn_emergency);
        add_number = findViewById(R.id.add_number);
        emergency = findViewById(R.id.btn_emergency);

        // getting user name
        db.collection("users").document(fb_user.getEmail()).get().addOnCompleteListener(
                new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        Log.i("AlertActivity", "inside on complete of fetching user_name");
                        DocumentSnapshot document = task.getResult();
                        String user_name = document.getString("user_name");
                        mWelcomeUser.setText("Welcome " + user_name + ",");
                    }
                }
        );


        emergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("AlertActivity", "inside emergency click");

                if (newContacts == null) {
                    db.collection("users").document(fb_user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            Log.i("AlertActivity", "contacts: inside on complete");

                            DocumentSnapshot document = task.getResult();
                            newContacts = (Map<String, String>) document.get("new_contacts");

                        }
                    });
                }
                if (newContacts == null) {
                    Toast.makeText(AlertActivity.this, "No Emergency contact listed.", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (Map.Entry<String, String> entry : newContacts.entrySet()) {
                    String phn = entry.getValue();
                    Log.d("AlertActivity", "phone number is: " + phn);
                    //   http://maps.google.com/maps?q=<lat>,<lng>
                    String msg;
                    msg = "http://maps.google.com/maps?q=" + latLng.latitude + "," + latLng.longitude;
                    try {
                        Log.d("AlertActivity", "Sending message now. bol");
                        SmsManager smms = SmsManager.getDefault();
                        smms.sendTextMessage(phn, null, msg, null, null);
                        Toast.makeText(AlertActivity.this, "Emergency notification sent to the registered numbers", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.d("AlertActivity", e.getMessage());

                    }
                }
            }
        });
        add_number.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                startActivityForResult(intent, 1);


            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.d("AlertActivity", "Now checking location");

        checkLocation(); //check whether location service is enable or not in your  phone
        Log.d("AlertActivity", "Now updating contacts");

        updateContacts();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Uri contactData = data.getData();
                Cursor cursor = managedQuery(contactData, null, null, null, null);
                cursor.moveToFirst();

                String moNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                if (newContacts == null)
                    newContacts = new HashMap<>();

                newContacts.put(contactName, moNumber);

                Map<String, Object> contactUpdate = new HashMap<>();
                contactUpdate.put("new_contacts", newContacts);

                db.collection("users").document(fb_user.getEmail()).update(contactUpdate).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.i("AlertActivity", "saved new number to array");
                    }
                });

                db.collection("users").document(fb_user.getEmail()).update("contacts", FieldValue.arrayUnion(moNumber)).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("AlertActivity", "saved new number to array");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("AlertActivity", "could not save new number to array");
                    }
                });


            }

        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("AlertActivity", "inside on connected");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.i("AlertActivity", "inside ask permissions");

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS},
                    REQUEST_LOCATION);


            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Log.i("AlertActivity", "starting location updates 1");
        startLocationUpdates();

        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLocation == null) {
            Log.i("AlertActivity", "mLocation null");
            startLocationUpdates();
        }
        if (mLocation != null) {
            Log.i("AlertActivity", "mLocation not null. lat:" + String.valueOf(mLocation.getLatitude()) + " long:" + String.valueOf(mLocation.getLongitude()));

            // mLatitudeTextView.setText(String.valueOf(mLocation.getLatitude()));
            //mLongitudeTextView.setText(String.valueOf(mLocation.getLongitude()));
        } else {
            Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("AlertActivity", "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("AlertActivity", "onstart");

        if (mGoogleApiClient != null) {
            Log.i("AlertActivity", "onstart. apliclient not null");

            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("AlertActivity", "onstop");

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void startLocationUpdates() {
        Log.i("AlertActivity", "start location updates");

        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("AlertActivity", "onlocation changed");

        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        mLatitudeTextView.setText(String.valueOf(location.getLatitude()));
        mLongitudeTextView.setText(String.valueOf(location.getLongitude()));
        Log.i("AlertActivity", msg);
        // You can now create a LatLng Object for use with maps
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        Log.i("AlertActivity", "updating map marker now.");

        mMap.addMarker(new MarkerOptions().position(latLng).title("current position"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        // mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng , 16));

    }

    private boolean checkLocation() {
        Log.i("AlertActivity", "check location");
        if (!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private void showAlert() {
        Log.i("AlertActivity", "show alert");

        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        Log.i("AlertActivity", "location enabled");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("AlertActivity", "permission got. starting location updates");
                    startLocationUpdates();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_user_out:
                Log.i("AlertActivity", "signing out");

                FirebaseAuth.getInstance().signOut();
                finish();
                Intent i = new Intent(AlertActivity.this, MainActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

//        Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void updateContacts() {
        Log.i("AlertActivity", "inside update contacts");


        db.collection("users").document(fb_user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                Log.i("AlertActivity", "contacts: inside on complete");

                DocumentSnapshot document = task.getResult();

                newContacts = (Map<String, String>) document.get("new_contacts");
                ArrayList<AllContacts> contactsList = new ArrayList<AllContacts>();
                if (newContacts != null) {
                    for (Map.Entry<String, String> entry : newContacts.entrySet()) {
                        contactsList.add(new AllContacts(entry.getKey(), entry.getValue().toString()));
                    }
                    adapter = new ContactListAdapter(AlertActivity.this, contactsList);
                    listView.setAdapter(adapter);
                }
            }
        });

        db.collection("users").document(fb_user.getEmail()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.i("AlertActivity", "exception inside on snapshot listener: " + e.getMessage());
                    //  apiProgressBar.setVisibility(View.INVISIBLE);
                    return;
                }
                // resetting the list view
                Log.i("AlertActivity", "resetting the list view after change");
                newContacts = (Map<String, String>) documentSnapshot.get("new_contacts");
                contactsList = new ArrayList<AllContacts>();

                if (newContacts != null) {
                    for (Map.Entry<String, String> entry : newContacts.entrySet()) {

                        contactsList.add(new AllContacts(entry.getKey(), entry.getValue().toString()));
                    }
                    adapter = new ContactListAdapter(AlertActivity.this, contactsList);
                    listView.setAdapter(adapter);
                }
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Log.i("AlertActivity", "item to delete number clicked");

                int itemPosition = position;
                AllContacts contactToDel = contactsList.get(itemPosition);

                if (newContacts != null)
                    newContacts.remove(contactToDel.getName());

                li = LayoutInflater.from(AlertActivity.this);
                View promptsDelete = li.inflate(R.layout.delete_number_dialog, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        AlertActivity.this);
                alertDialogBuilder.setView(promptsDelete);

                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Log.i("AlertActivity", "now attempting number delete");
                                        Map<String, Object> contactUpdate = new HashMap<>();
                                        contactUpdate.put("new_contacts", newContacts);
                                        db.collection("users").document(fb_user.getEmail()).update(contactUpdate).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Log.i("AlertActivity", "saved new number to array");
                                            }
                                        });
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Log.i("AlertActivity", "user pressed cancel in delete number dialog");
                                        dialog.cancel();
                                    }
                                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

            }
        });

    }

}
