
package com.example.trafficscotland;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trafficscotland.ui.SituationListFragment;
import com.example.trafficscotland.ui.TrafficMapFragment;
import com.google.android.material.tabs.TabLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;

    private static final String STATE_TAB = "selected_tab";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabLayout);

        tabLayout.addTab(
                tabLayout.newTab().setText("Incidents")
        );

        tabLayout.addTab(
                tabLayout.newTab().setText("Roadworks")
        );

        tabLayout.addTab(
                tabLayout.newTab().setText("Map")
        );

        tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        showFragment(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                    }
                }
        );

        if (savedInstanceState == null) {
            showFragment(0);
        } else {
            int selectedTab = savedInstanceState.getInt(
                    STATE_TAB, 0
            );

            TabLayout.Tab tab =
                    tabLayout.getTabAt(selectedTab);

            if (tab != null) {
                tab.select();
            }
        }
    }

    private void showFragment(int position) {

        Fragment fragment;

        switch (position) {
            case 1:
                fragment =
                        SituationListFragment.newInstance("Roadworks");
                break;

            case 2:
                fragment = new TrafficMapFragment();
                break;

            default:
                fragment =
                        SituationListFragment.newInstance("Incident");
                break;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(
                STATE_TAB,
                tabLayout.getSelectedTabPosition()
        );
    }
}
