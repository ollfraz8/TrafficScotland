package com.example.trafficscotland.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trafficscotland.R;
import com.example.trafficscotland.model.Situation;
import com.example.trafficscotland.utils.DistanceUtils;
import com.example.trafficscotland.viewmodel.TrafficViewModel;

import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SituationListFragment extends Fragment {

    private static final String ARG_TYPE = "type";

    private SituationAdapter adapter;
    private TrafficViewModel viewModel;

    private EditText editSearchRoad;
    private EditText editLatitude;
    private EditText editLongitude;
    private EditText editRadius;

    private TextView txtEmpty;
    private TextView txtDistanceStatus;
    private RecyclerView recyclerView;

    private TextView txtLastUpdated;
    private Button btnRefresh;


    private boolean isLoading = false;
    private String selectedType = "Incident";

    public static SituationListFragment newInstance(
            String type) {

        SituationListFragment fragment =
                new SituationListFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_situation_list,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            selectedType = getArguments().getString(
                    ARG_TYPE, "Incident"
            );
        }

        viewModel = new ViewModelProvider(requireActivity())
                .get(TrafficViewModel.class);

        editSearchRoad = view.findViewById(
                R.id.editSearchRoad);
        editLatitude = view.findViewById(
                R.id.editLatitude);
        editLongitude = view.findViewById(
                R.id.editLongitude);
        editRadius = view.findViewById(
                R.id.editRadius);

        txtEmpty = view.findViewById(R.id.txtEmpty);
        txtDistanceStatus = view.findViewById(
                R.id.txtDistanceStatus);

        txtLastUpdated = view.findViewById(
                R.id.txtLastUpdated);

        btnRefresh = view.findViewById(
                R.id.btnRefresh);

        btnRefresh.setOnClickListener(v -> {
            viewModel.refreshSituations();
        });


        recyclerView = view.findViewById(
                R.id.recyclerSituations);

        ProgressBar progressBar = view.findViewById(
                R.id.progressBar);

        adapter = new SituationAdapter();

        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        recyclerView.setAdapter(adapter);

        // Open existing detail screen
        adapter.setOnSituationClickListener(situation -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragmentContainer,
                            SituationDetailFragment.newInstance(
                                    situation.getId()
                            )
                    )
                    .addToBackStack(null)
                    .commit();
        });

        // Restore shared road search
        editSearchRoad.setText(
                viewModel.getCurrentRoadQuery()
        );

        // Restore shared distance search
        restoreDistanceFields();
        updateDistanceStatus();

        editSearchRoad.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        viewModel.setRoadQuery(s.toString());
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );

        view.findViewById(R.id.btnApplyDistance)
                .setOnClickListener(
                        v -> applyDistanceFilter()
                );

        view.findViewById(R.id.btnClearDistance)
                .setOnClickListener(
                        v -> clearDistanceFilter()
                );


        viewModel.getSituations().observe(
                getViewLifecycleOwner(),
                situations -> {
                    filterSituations();
                    updateLastUpdated();
                }
        );


        viewModel.getRoadQuery().observe(
                getViewLifecycleOwner(),
                query -> {
                    String currentText =
                            editSearchRoad.getText().toString();

                    if (!currentText.equals(query)) {
                        editSearchRoad.setText(query);
                    }

                    filterSituations();
                }
        );

        viewModel.getDistanceActive().observe(
                getViewLifecycleOwner(),
                active -> {
                    restoreDistanceFields();
                    updateDistanceStatus();
                    filterSituations();
                }
        );

        viewModel.isLoading().observe(
                getViewLifecycleOwner(),
                loading -> {

                    isLoading = Boolean.TRUE.equals(loading);

                    btnRefresh.setEnabled(!isLoading);

                    if (!isLoading) {
                        updateLastUpdated();
                    }


                    progressBar.setVisibility(
                            isLoading
                                    ? View.VISIBLE
                                    : View.GONE
                    );

                    filterSituations();
                }
        );

        viewModel.getError().observe(
                getViewLifecycleOwner(),
                message -> {
                    if (message != null &&
                            !message.isEmpty()) {

                        Toast.makeText(
                                requireContext(),
                                "Download failed: " + message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );

        viewModel.loadSituations();
    }

    private void applyDistanceFilter() {

        String latText = editLatitude.getText()
                .toString().trim();

        String lonText = editLongitude.getText()
                .toString().trim();

        String radiusText = editRadius.getText()
                .toString().trim();

        try {
            double latitude = Double.parseDouble(latText);
            double longitude = Double.parseDouble(lonText);
            double radius = Double.parseDouble(radiusText);

            if (!DistanceUtils.isValidCoordinate(
                    latitude, longitude)) {

                editLatitude.setError(
                        "Enter valid latitude and longitude"
                );
                return;
            }

            if (!Double.isFinite(radius) || radius <= 0) {
                editRadius.setError(
                        "Enter a radius greater than zero"
                );
                return;
            }

            viewModel.setDistanceFilter(
                    latitude, longitude, radius
            );

        } catch (NumberFormatException e) {
            Toast.makeText(
                    requireContext(),
                    "Enter valid latitude, longitude and miles",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void clearDistanceFilter() {
        viewModel.clearDistanceFilter();

        editLatitude.setText("");
        editLongitude.setText("");
        editRadius.setText("");
    }

    private void restoreDistanceFields() {

        if (viewModel.isDistanceActive()) {
            editLatitude.setText(String.valueOf(
                    viewModel.getSearchLatitude()));

            editLongitude.setText(String.valueOf(
                    viewModel.getSearchLongitude()));

            editRadius.setText(String.valueOf(
                    viewModel.getSearchRadius()));
        }
    }

    private void updateDistanceStatus() {

        if (viewModel.isDistanceActive()) {

            txtDistanceStatus.setText(
                    String.format(
                            Locale.UK,
                            "Within %.1f miles of %.5f, %.5f",
                            viewModel.getSearchRadius(),
                            viewModel.getSearchLatitude(),
                            viewModel.getSearchLongitude()
                    )
            );

            txtDistanceStatus.setVisibility(View.VISIBLE);

        } else {
            txtDistanceStatus.setVisibility(View.GONE);
        }
    }

    private void filterSituations() {

        if (adapter == null || viewModel == null) {
            return;
        }

        List<Situation> filtered =
                viewModel.getFilteredSituations(selectedType);

        adapter.updateSituations(filtered);

        if (filtered.isEmpty() && !isLoading) {

            String query = viewModel
                    .getCurrentRoadQuery().trim();

            if (viewModel.isDistanceActive()) {
                txtEmpty.setText(
                        "No traffic situations found within "
                                + viewModel.getSearchRadius()
                                + " miles"
                );

            } else if (query.isEmpty()) {
                txtEmpty.setText(
                        "No " + selectedType
                                + " situations found"
                );

            } else {
                txtEmpty.setText(
                        "No situations found for \""
                                + query + "\""
                );
            }

            txtEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

        } else {
            txtEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateLastUpdated() {

        long timestamp =
                viewModel.getLastUpdated(selectedType);

        if (timestamp <= 0) {
            txtLastUpdated.setText(
                    "Last updated: No cached data available"
            );
            return;
        }

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "dd MMM yyyy, HH:mm:ss",
                        Locale.UK
                );
        String formattedDate = dateFormat.format(
                new Date(timestamp)
        );

        txtLastUpdated.setText(
                "Last updated: " + formattedDate
        );
    }

}