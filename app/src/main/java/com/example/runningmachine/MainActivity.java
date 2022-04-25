package com.example.runningmachine;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    FragmentToday fragmentToday;
    FragmentCalendar fragmentCalendar;
    FragmentCalorie fragmentCalorie;
    FragmentMap fragmentMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentToday = new FragmentToday();
        fragmentCalendar = new FragmentCalendar();
        fragmentCalorie = new FragmentCalorie();
        fragmentMap = new FragmentMap();

        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragmentToday).commit();
        BottomNavigationView bottom_menu = findViewById(R.id.bottom_menu);
        bottom_menu.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.menuToday:
                        getSupportFragmentManager().beginTransaction().replace(R.id.container,fragmentToday).commit();
                        break;
                    case R.id.menuCalendar:
                        getSupportFragmentManager().beginTransaction().replace(R.id.container,fragmentCalendar).commit();
                        break;
                    case R.id.menuCalorie:
                        getSupportFragmentManager().beginTransaction().replace(R.id.container,fragmentCalorie).commit();
                        break;
                    case R.id.menuMap:
                        getSupportFragmentManager().beginTransaction().replace(R.id.container,fragmentMap).commit();
                        break;
                }
                return false;
            }
        });
    }
}