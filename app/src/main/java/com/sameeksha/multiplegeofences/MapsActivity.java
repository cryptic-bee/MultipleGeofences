package com.sameeksha.multiplegeofences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryEventListener {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentuser;
    private DatabaseReference myLocationRef;
    private GeoFire geoFire;
    private List<LatLng> dangerous_areas;
    private EditText address;
    private String location;
    private  DatabaseReference databaseReference;
   private FirebaseUser firebaseUser;
   private FirebaseAuth firebaseAuth;
    private  String contactnumber="";
    String contact="";
    String contact1="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        firebaseUser=firebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase database =FirebaseDatabase.getInstance();
        databaseReference= database.getReference("Users").child(firebaseUser.getUid()).child("Emergency contacts");
        ActivityCompat.requestPermissions(MapsActivity.this,new String[]{Manifest.permission.SEND_SMS,Manifest.permission.READ_SMS},PackageManager.PERMISSION_GRANTED);
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.


                        buildLocationRequest();
                        buildLocationCallback();
                        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(MapsActivity.this);


                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(MapsActivity.this);
//mMap.setMyLocationEnabled(true);

                        //initArea();

                       settingGeofire();

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this, "You must enable permission", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }
    public  void onClick(View view)
    {   address=(EditText)findViewById(R.id.search);
        location=address.getText().toString();
        //System.out.println(location);
        initArea();
       // settingGeofire();
    }
//String contactnumber="";
public  void sendsms() {


    databaseReference.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

            for(DataSnapshot childsnop : dataSnapshot.getChildren()) {
                contact="";
                contactnumber="";
                contact = childsnop.child("emergency").getValue(String.class);

             //   Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                System.out.println(contact);
               //contact.trim();
               contact1=contact.replaceAll("\\s","");
                System.out.println(contact1);

                int last = contact1.lastIndexOf(" ");
                if(contact1.charAt(last+1)=='+')
                {
                    for( int i=last+1;i<contact1.length();i++)
                    {
                        contactnumber=contactnumber+contact1.charAt(i);
                    }
                }
                else {
                    for (int i = last+1; i < contact1.length(); i++) {
                        contactnumber = contactnumber +contact1.charAt(i);

                    }
                }
                System.out.println(contactnumber);
                SmsManager manager=SmsManager.getDefault();
                manager.sendTextMessage(contactnumber,null,"hi",null,null);


            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }


    });


}
            private void initArea() {



                   // EditText address=(EditText)findViewById(R.id.search);
                  //  String location=address.getText().toString();
                   // System.out.println(location);
                List<Address> addressesList=null;

                if(!(TextUtils.isEmpty(location)))
                  {
                      Geocoder geocoder=new Geocoder(MapsActivity.this);
                      try {
                          addressesList=geocoder.getFromLocationName(location,8);//geocoder gets  the coordinates from the address
//System.out.println(addressesList);
                      }

                      catch (IOException e)
                      {
                          e.printStackTrace();
                      }

                      for(int i=0;i<addressesList.size();i++)
                      {
                          Address useraddress=addressesList.get(i);
                          dangerous_areas=new ArrayList<>();

                          dangerous_areas.add(new LatLng(useraddress.getLatitude(),useraddress.getLongitude()));
                       //   System.out.println(dangerous_areas);
                          if( geoFire!=null)
                              geoFire=new GeoFire(myLocationRef);
                          for (LatLng latLng : dangerous_areas) {

                              mMap.addCircle(new CircleOptions().center(latLng)
                                      .radius(500)
                                      .strokeColor(Color.BLUE)
                                      .fillColor(0x22000FF)
                                      .strokeWidth(5.05f)
                              );

                              //Create GeoQuery for user in danger location

                              GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude), 0.5f);//500m
                              geoQuery.addGeoQueryEventListener(MapsActivity.this);
                          }

                      }
                     // fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                  }



        }



      /*  {mMap.addCircle(new CircleOptions().center(latLng)
                .radius(500)
                .strokeColor(Color.BLUE)
                .fillColor(0x22000FF)
                .strokeWidth(5.05f)
        );
            GeoQuery geoQuery=geoFire.queryAtLocation(new GeoLocation(latLng.latitude,latLng.longitude),0.5f) ;//500m
            geoQuery.addGeoQueryEventListener(MapsActivity.this);
        }*/



    private void settingGeofire() {

        myLocationRef= FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid()).child("Mylocation");
        geoFire=new GeoFire(myLocationRef);
    }

    private void buildLocationCallback() {
        locationCallback=new LocationCallback(){
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                if(mMap!=null)

                  {



                geoFire.setLocation("You", new GeoLocation(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if(currentuser!=null)currentuser.remove();
                        currentuser= mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(locationResult.getLastLocation().getLatitude(),
                                        locationResult.getLastLocation().getLongitude()))
                                .title("You"));

                        mMap.animateCamera(CameraUpdateFactory
                                .newLatLngZoom(currentuser.getPosition(),12.0f));

                    }
                });


                 }
            }


        };

    }

    private void buildLocationRequest() {
        locationRequest= new  LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);


    }


    @Override
    public void onMapReady(GoogleMap googleMap){
            mMap = googleMap;

            mMap.getUiSettings().setZoomControlsEnabled(true);


            if (fusedLocationProviderClient != null)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
  //addcicle for danger
        System.out.println(dangerous_areas);
          /* for (LatLng latLng : dangerous_areas) {

               mMap.addCircle(new CircleOptions().center(latLng)
                       .radius(500)
                        .strokeColor(Color.BLUE)
                      .fillColor(0x22000FF)
                      .strokeWidth(5.05f)
                );

                //Create GeoQuery for user in danger location

                GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude), 0.5f);//500m
              geoQuery.addGeoQueryEventListener(MapsActivity.this);
            }*/
       }




    @Override
    protected void onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();

    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("You",String.format("%s entered into the dangerous area",key));
        sendsms();
    }

    @Override
    public void onKeyExited(String key) {
        sendNotification("You",String.format("%s left the dangerous area",key));
    }



    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("You",String.format("%s are moving within dangerous area",key));
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
Toast.makeText(this,""+error.getMessage(),Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String title, String content) {

        String Notification_id="you_multiple_location";
        NotificationManager notificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel= new NotificationChannel(Notification_id,"My Notification",
                    NotificationManager.IMPORTANCE_DEFAULT);

            //config

            notificationChannel.setDescription("Channer Description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder=new NotificationCompat.Builder(this,Notification_id);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        Notification notification=builder.build();
        notificationManager.notify(new Random().nextInt(),notification);
    }
}
