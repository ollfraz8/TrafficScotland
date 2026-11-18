
package com.example.trafficscotland.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.trafficscotland.R;
import com.example.trafficscotland.model.Situation;
import com.example.trafficscotland.viewmodel.TrafficViewModel;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class TrafficMapFragment extends Fragment
        implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private TrafficViewModel viewModel;

    private static final LatLng SCOTLAND =
            new LatLng(56.4907, -4.2026);

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_traffic_map,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity())
                .get(TrafficViewModel.class);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager()
                        .findFragmentById(R.id.googleMapContainer);

        if (mapFragment == null) {

            mapFragment = SupportMapFragment.newInstance();

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.googleMapContainer,
                            mapFragment
                    )
                    .commitNow();
        }

        mapFragment.getMapAsync(this);

        // Update markers when traffic data changes.
        viewModel.getSituations().observe(
                getViewLifecycleOwner(),
                situations -> updateMarkers()
        );

        // Update markers when road-name search changes.
        viewModel.getRoadQuery().observe(
                getViewLifecycleOwner(),
                query -> updateMarkers()
        );

        // Update markers when distance search changes.
        viewModel.getDistanceActive().observe(
                getViewLifecycleOwner(),
                active -> updateMarkers()
        );

        viewModel.loadSituations();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        googleMap = map;

        // Default map position: Scotland.
        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        SCOTLAND,
                        7f
                )
        );

        googleMap.getUiSettings()
                .setZoomControlsEnabled(true);

        // Open situation details when the user
        // taps a marker's information window.
        googleMap.setOnInfoWindowClickListener(marker -> {

            Object tag = marker.getTag();

            if (tag instanceof String) {

                getParentFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragmentContainer,
                                SituationDetailFragment.newInstance(
                                        (String) tag
                                )
                        )
                        .addToBackStack(null)
                        .commit();
            }
        });

        updateMarkers();
    }

    private void updateMarkers() {

        if (googleMap == null || viewModel == null) {
            return;
        }

        // Remove previous markers before updating.
        googleMap.clear();

        // Retrieve situations matching shared filters.
        // Null includes both incidents and roadworks.
        List<Situation> filtered =
                viewModel.getFilteredSituations(null);

        LatLngBounds.Builder boundsBuilder =
                new LatLngBounds.Builder();

        int markerCount = 0;
        LatLng singlePosition = null;

        for (Situation situation : filtered) {

            LatLng position =
                    getSituationPosition(situation);

            // Skip situations without valid coordinates.
            if (position == null) {
                continue;
            }

            // Determine marker colour.
            boolean isIncident =
                    "Incident".equalsIgnoreCase(
                            situation.getType()
                    );

            float colour = isIncident
                    ? BitmapDescriptorFactory.HUE_RED
                    : BitmapDescriptorFactory.HUE_AZURE;

            // Display road name.
            String roadName = situation.getRoadName();

            if (roadName == null ||
                    roadName.trim().isEmpty()) {

                roadName = "Unknown road";
            }

            // Display situation description.
            String description = situation.getDescription();

            if (description == null ||
                    description.trim().isEmpty()) {

                description = "Tap for more information";
            }

            // NEW: Limit long descriptions so that
            // marker information windows remain readable.
            if (description.length() > 100) {

                description =
                        description.substring(0, 97) + "...";
            }

            // Add marker to Google Maps.
            Marker marker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(position)
                            .title(roadName)
                            .snippet(description)
                            .icon(
                                    BitmapDescriptorFactory
                                            .defaultMarker(colour)
                            )
            );

            if (marker != null) {

                // Store situation ID for detail navigation.
                marker.setTag(situation.getId());

                boundsBuilder.include(position);

                markerCount++;
                singlePosition = position;
            }
        }

        // Automatically position map to show
        // all matching traffic situations.
        if (markerCount == 1) {

            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            singlePosition,
                            11f
                    )
            );

        } else if (markerCount > 1) {

            LatLngBounds bounds = boundsBuilder.build();

            View mapView = getView();

            if (mapView != null) {

                mapView.post(() -> {

                    if (googleMap != null && isAdded()) {

                        googleMap.animateCamera(
                                CameraUpdateFactory
                                        .newLatLngBounds(
                                                bounds,
                                                100
                                        )
                        );
                    }
                });
            }

        } else {

            // Return to Scotland if no matching
            // situations have valid coordinates.
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            SCOTLAND,
                            7f
                    )
            );
        }
    }

    private LatLng getSituationPosition(
            Situation situation) {

        // First choice: starting coordinates.
        double fromLat = situation.getFromLatitude();
        double fromLon = situation.getFromLongitude();

        if (isUsablePosition(fromLat, fromLon)) {

            return new LatLng(fromLat, fromLon);
        }

        // Second choice: ending coordinates.
        double toLat = situation.getToLatitude();
        double toLon = situation.getToLongitude();

        if (isUsablePosition(toLat, toLon)) {

            return new LatLng(toLat, toLon);
        }

        // NEW: Final fallback to general coordinates.
        double latitude = situation.getLatitude();
        double longitude = situation.getLongitude();

        if (isUsablePosition(latitude, longitude)) {

            return new LatLng(latitude, longitude);
        }

        // No valid location available.
        return null;
    }

    private boolean isUsablePosition(
            double latitude,
            double longitude) {

        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180
                && !(latitude == 0 && longitude == 0);
    }

    @Override
    public void onDestroyView() {

        googleMap = null;

        super.onDestroyView();
    }
}
