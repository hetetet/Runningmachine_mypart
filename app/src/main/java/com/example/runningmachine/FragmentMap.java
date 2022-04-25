package com.example.runningmachine;

import static android.content.Context.LOCATION_SERVICE;
import static java.lang.Math.max;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class FragmentMap extends Fragment{

    private GoogleMap gmap;
    private LocationListener locationListener;
    private Location location;
    private LocationManager locationManager;
    private FusedLocationProviderClient client;
    private FusedLocationProviderClient parks;
    private Polyline polyline;
    private PolylineOptions ploption=null;
    private ArrayList<LatLng> previous_loc;

    private int willpass=0;
    private ArrayList<LatLng> passed_loc;
    private Polyline passed;
    private PolylineOptions passedOptions;

    private final long MIN_TIME=1000; //1 second
    private final long MIN_DIST=2; //1미터?

    private MarkerOptions myLocationMarker;
    private Marker myMarker;

    private Button btnSearchPark;
    private Button btnSetRoute;
    private Button btnStartWalk;

    private boolean isTrackRecorded;
    private boolean isWalking;
    private boolean showPark;

    private int light_purple;
    private int purple;

    private double recordedDist;
    private double actualWalked;
    private double percentage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_map,
                container, false);
        btnSearchPark = (Button) view.findViewById(R.id.neigh_park);
        btnSetRoute = (Button) view.findViewById(R.id.setRoute);
        btnStartWalk = (Button) view.findViewById(R.id.startWalk);

        light_purple=ContextCompat.getColor(getActivity(), R.color.purple_200);
        purple=ContextCompat.getColor(getActivity(), R.color.purple_500);
        recordedDist=(double)0;
        actualWalked=(double)0;

        btnSearchPark.setBackgroundColor(light_purple);
        btnSetRoute.setBackgroundColor(light_purple);
        btnStartWalk.setBackgroundColor(light_purple);

        locationManager = (LocationManager)getContext().getSystemService(LOCATION_SERVICE);
        previous_loc=new ArrayList<LatLng>();
        passed_loc=new ArrayList<LatLng>();
        isTrackRecorded=false;
        isWalking=false;
        showPark=false;
        parks=LocationServices.getFusedLocationProviderClient(getActivity());
        percentage=0.0;

        SupportMapFragment supportMapFragment=(SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {

            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                //when map is loaded
                gmap=googleMap;
                client= LocationServices.getFusedLocationProviderClient(getActivity());
                if(ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED&&
                        ContextCompat.checkSelfPermission(getActivity(),
                                Manifest.permission.ACCESS_COARSE_LOCATION) ==PackageManager.PERMISSION_GRANTED)
                {

                    locationListener=new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            getLocation(location);
                        }
                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {

                        }
                        @Override
                        public void onProviderEnabled(String provider) {

                        }
                        @Override
                        public void onProviderDisabled(String provider) {

                        }
                    };
                    startLocationService();

                    btnSearchPark.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //get park lists marker
                            if(!showPark)
                            {
                                btnSearchPark.setBackgroundColor(purple);
                                showPark=true;
                                readparkdata();
                            }
                            else
                            {
                                btnSearchPark.setBackgroundColor(light_purple);
                                showPark=false;
                                gmap.clear();
                                getLocation(location);
                            }
                        }

                    });

                    btnSetRoute.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(isWalking)
                                AlartDialog("산책 중에는 이용하실 수 없습니다.");
                            else if(isTrackRecorded){
                                if(polyline!=null){
                                isTrackRecorded=false;
                                btnSetRoute.setBackgroundColor(light_purple);
                                if(polyline!=null) {

                                    gmap.clear();
                                    myLocationMarker.position(previous_loc.get(max(0,previous_loc.size() - 1)));
                                    myMarker = gmap.addMarker(myLocationMarker);

                                    for (int i = 0; i < previous_loc.size() - 1; i++) {
                                        recordedDist += latlnToLoc(previous_loc.get(i+1))
                                                .distanceTo(latlnToLoc(previous_loc.get(i)));
                                    }
                                    Log.d("경로 저장 완료, 총 거리: ", "거리: " + recordedDist);
                                    }
                                }
                            }else{
                                if(polyline!=null)
                                    openDialog();
                                else
                                {
                                    isTrackRecorded = true;
                                    btnSetRoute.setBackgroundColor(purple);
                                }
                            }
                        }
                    });

                    btnStartWalk.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            if(isTrackRecorded)
                                AlartDialog("산책 경로 기록 중에는 이용하실 수 없습니다.");
                            else if(previous_loc.isEmpty())
                                AlartDialog("산책 경로를 먼저 설정해주세요.");
                            else
                            {
                                if (isWalking) {
                                    isWalking = false;
                                    btnStartWalk.setBackgroundColor(light_purple);

                                    gmap.clear();
                                    myLocationMarker.position(passed_loc.get(max(0,passed_loc.size() - 1)));
                                    myMarker = gmap.addMarker(myLocationMarker);

                                    for (int i = 0; i < passed_loc.size() - 1; i++) {
                                        actualWalked += latlnToLoc(passed_loc.get(i+1))
                                                .distanceTo(latlnToLoc(passed_loc.get(i)));
                                    }
                                    percentage=(actualWalked/recordedDist)*100;
                                    //전체 경로와 걸은 경로의 비율 달력에 넘겨줌
                                } else {
                                    isWalking = true;
                                    actualWalked=0;
                                    passed_loc.clear();
                                    btnStartWalk.setBackgroundColor(purple);
                                    ploption=new PolylineOptions().addAll(previous_loc).clickable(true);
                                    polyline= gmap.addPolyline(ploption);
                                    polyline.setColor(Color.rgb( 0,100,200));
                                    //polyline 그리는 코드는 location 관련 함수에 있음
                                }
                            }
                        }
                    });

                    locationManager=(LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
                    try{
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME,MIN_DIST,locationListener);
                    }catch(SecurityException e){
                        e.printStackTrace();
                    }
                }
                else{ //위치 권한 요청하기
                    Toast.makeText(getActivity(),
                            "기기에서 'RunningMachine에 대한 위치 엑세스를 허용한 후 지도를 다시 실행해 주세요.", Toast.LENGTH_SHORT).show();
                }
            }

        });

        return view;
    }
    public void startLocationService()
    {
        location=null;
        try {
            location = null;

            long minTime = 0;        // 0초마다 갱신 - 바로바로갱신
            float minDistance = 0;

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    getLocation(location);
                }

                //위치 요청하기
                locationManager.requestLocationUpdates
                        (LocationManager.GPS_PROVIDER, minTime, minDistance, locationListener);
                //locationManager.removeUpdates(gpsListener);

            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    getLocation(location);
                }
                //위치 요청하기
                locationManager.requestLocationUpdates
                        (LocationManager.NETWORK_PROVIDER, minTime, minDistance, locationListener);
            }

        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    public void getLocation(Location location)
    {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng curPoint = new LatLng(latitude, longitude);
        gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 16));
        if (myLocationMarker == null) {
            myLocationMarker = new MarkerOptions(); // 마커 객체 생성
            myLocationMarker.position(curPoint);
            myMarker = gmap.addMarker(myLocationMarker);
        } else {
            myMarker.remove(); // 마커삭제
            myLocationMarker.position(curPoint);
            myMarker = gmap.addMarker(myLocationMarker);
        }

        if(isTrackRecorded)
        {
            //location의 위치 정보를 marker에 저장

            if (location != null)
            {
                previous_loc.add(curPoint);
                if(previous_loc.size()>=0)
                {
                    ploption=new PolylineOptions().addAll(previous_loc).clickable(true);
                    polyline = gmap.addPolyline(ploption);
                    polyline.setColor(Color.rgb( 0,100,200));
                }
            }
            else
            {
                Toast.makeText(getActivity(), "location==null", Toast.LENGTH_SHORT).show();
            }
        }
        if(isWalking)
        {

            double dist=location.distanceTo(latlnToLoc(previous_loc.get(willpass)));

            Log.d("location",location.toString());
            Log.d("willpass",previous_loc.get(willpass).latitude+","+previous_loc.get(willpass).longitude);
            Log.d("거리","dist:"+dist);

            if(dist<=10){
                Toast.makeText(getActivity(), "이대로 쭉쭉 가면 된다~~", Toast.LENGTH_SHORT).show();
                passed_loc.add(previous_loc.get(willpass));
                passedOptions=new PolylineOptions().addAll(passed_loc).clickable(true);
                passed = gmap.addPolyline(passedOptions);
                passed.setColor(Color.rgb( 200,0,0));
                willpass++;
            }
        }
    }
    private ArrayList<parkSample> parksamples=new ArrayList<>();
    public void readparkdata()
    {
       InputStream is= getResources().openRawResource(R.raw.parksloc);
        BufferedReader reader=new BufferedReader(
                new InputStreamReader(is, Charset.forName("x-windows-949"))
        );

        String line="";

            try {
                reader.readLine();
                while(((line=reader.readLine())!=null)) {

                    int i=0;
                    String[] tokens=line.split(String.valueOf(','));
                    parkSample sample=new parkSample();
                    sample.setParkname(tokens[i]);

                    i++;

                    if(tokens.length>=3&&tokens[i].length()>0)
                    {
                        try {
                            sample.setLatitude(Double.parseDouble(tokens[i]));
                        }catch(NumberFormatException e)
                        {
                            tokens[i-1]=tokens[i-1]+tokens[i];
                        }
                    }
                    else
                        continue;

                    i++;

                    if(tokens[i].length()>0)
                        sample.setLongitude(Double.parseDouble(tokens[i]));
                    else
                        continue;
                    Location parkloc=new Location("");
                    parkloc.setLatitude(sample.getLatitude());
                    parkloc.setLongitude(sample.getLongitude());
                    if(location!=null&&location.distanceTo(parkloc)<2000)
                    {
                        parksamples.add(sample);
                        Log.d("MyActivity","Just created: "+sample);
                        MarkerOptions parkMarker=new MarkerOptions().position(
                                new LatLng(sample.getLatitude(),sample.getLongitude())).
                                title(sample.getParkname()).
                                icon(BitmapDescriptorFactory.
                                        defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                        gmap.addMarker(parkMarker);
                    }
                }
            } catch (IOException e) {
                Log.wtf("Myactivity","Error reading data file on line"+line,e);
                e.printStackTrace();
        }
   }

    private void openDialog() {
        AlertDialog.Builder oDialog = new AlertDialog.Builder(getActivity(),
                android.R.style.Theme_DeviceDefault_Light_Dialog);

        oDialog.setMessage("이미 지정된 경로가 있습니다.\n경로를 새로 저장하시겠습니까?")
                .setTitle("경고")
                .setPositiveButton("예", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        previous_loc.clear();
                        recordedDist=0;
                        isTrackRecorded=true;
                        btnSetRoute.setBackgroundColor(purple);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("아니오.", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        isTrackRecorded=false;
                        dialog.dismiss();
                    }
                })

                .show();
    }

    private void AlartDialog(String string) {
        AlertDialog.Builder oDialog = new AlertDialog.Builder(getActivity(),
                android.R.style.Theme_DeviceDefault_Light_Dialog);

        oDialog.setMessage(string)
                .setTitle("알림")
                .setPositiveButton("확인", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private Location latlnToLoc(LatLng latLng)
    {
        double lat=latLng.latitude;
        double lon=latLng.longitude;
        Location newloc=new Location("");
        newloc.setLatitude(lat);
        newloc.setLongitude(lon);
        return newloc;
    }

    public double getPercentage() {
        return percentage;
    }

}

