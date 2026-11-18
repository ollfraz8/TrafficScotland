package com.example.trafficscotland.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.trafficscotland.model.Situation;
import com.example.trafficscotland.repository.TrafficRepository;
import com.example.trafficscotland.utils.DistanceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrafficViewModel extends AndroidViewModel {

    private final TrafficRepository repository;

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
    }
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Situation>> situations =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    // Shared filter state
    private final MutableLiveData<String> roadQuery =
            new MutableLiveData<>("");

    private final MutableLiveData<Boolean> distanceActive =
            new MutableLiveData<>(false);

    private double searchLatitude = 0;
    private double searchLongitude = 0;
    private double searchRadius = 0;

    private boolean loaded = false;

    public LiveData<List<Situation>> getSituations() {
        return situations;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getRoadQuery() {
        return roadQuery;
    }

    public LiveData<Boolean> getDistanceActive() {
        return distanceActive;
    }

    public String getCurrentRoadQuery() {
        String value = roadQuery.getValue();
        return value == null ? "" : value;
    }

    public boolean isDistanceActive() {
        return Boolean.TRUE.equals(distanceActive.getValue());
    }

    public double getSearchLatitude() {
        return searchLatitude;
    }

    public double getSearchLongitude() {
        return searchLongitude;
    }

    public double getSearchRadius() {
        return searchRadius;
    }

    public void setRoadQuery(String query) {
        String newQuery = query == null ? "" : query;

        if (!newQuery.equals(getCurrentRoadQuery())) {
            roadQuery.setValue(newQuery);
        }
    }

    public void setDistanceFilter(
            double latitude,
            double longitude,
            double radiusMiles) {

        if (!DistanceUtils.isValidCoordinate(
                latitude, longitude)
                || !Double.isFinite(radiusMiles)
                || radiusMiles <= 0) {
            return;
        }

        searchLatitude = latitude;
        searchLongitude = longitude;
        searchRadius = radiusMiles;

        distanceActive.setValue(true);
    }

    public void clearDistanceFilter() {
        searchLatitude = 0;
        searchLongitude = 0;
        searchRadius = 0;

        distanceActive.setValue(false);
    }

    // One filtering method used by both lists and map
    public List<Situation> getFilteredSituations(
            String selectedType) {

        List<Situation> filtered = new ArrayList<>();

        List<Situation> all = situations.getValue();

        if (all == null) {
            return filtered;
        }

        String query = getCurrentRoadQuery()
                .trim()
                .toLowerCase(Locale.ROOT);

        for (Situation situation : all) {

            // Optional type filter
            if (selectedType != null &&
                    (situation.getType() == null ||
                            !selectedType.equalsIgnoreCase(
                                    situation.getType()))) {
                continue;
            }

            // Road name filter
            String roadName = situation.getRoadName();

            if (roadName == null) {
                roadName = "";
            }

            if (!roadName.toLowerCase(Locale.ROOT)
                    .contains(query)) {
                continue;
            }

            // Distance filter
            if (isDistanceActive() &&
                    !DistanceUtils.isWithinRadius(
                            situation,
                            searchLatitude,
                            searchLongitude,
                            searchRadius)) {
                continue;
            }

            filtered.add(situation);
        }

        return filtered;
    }


    public void loadSituations() {

        if (loaded ||
                Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        downloadSituations();
    }

    public void refreshSituations() {

        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        downloadSituations();
    }

    private void downloadSituations() {

        loading.setValue(true);
        error.setValue(null);

        executor.execute(() -> {
            try {
                ArrayList<Situation> results =
                        repository.getSituations();

                loaded = true;
                situations.postValue(results);

            } catch (Exception e) {
                error.postValue(e.getMessage());

            } finally {
                loading.postValue(false);
            }
        });
    }
    public long getLastUpdated(String situationType) {
        return repository.getLastUpdated(situationType);
    }


    @Override
    protected void onCleared() {
        executor.shutdown();
        super.onCleared();
    }
}