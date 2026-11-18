package com.example.trafficscotland.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.trafficscotland.R;
import com.example.trafficscotland.model.Situation;
import com.example.trafficscotland.viewmodel.TrafficViewModel;

public class SituationDetailFragment extends Fragment {

    private static final String ARG_ID = "situation_id";

    public static SituationDetailFragment newInstance(String id) {
        SituationDetailFragment fragment = new SituationDetailFragment();

        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
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
                R.layout.fragment_situation_detail,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(
                v -> getParentFragmentManager().popBackStack()
        );

        TrafficViewModel viewModel =
                new ViewModelProvider(requireActivity())
                        .get(TrafficViewModel.class);

        String situationId = requireArguments().getString(ARG_ID);

        viewModel.getSituations().observe(
                getViewLifecycleOwner(),
                situations -> {
                    for (Situation situation : situations) {
                        if (situationId != null &&
                                situationId.equals(situation.getId())) {
                            displaySituation(view, situation);
                            break;
                        }
                    }
                }
        );
    }

    private void displaySituation(View view, Situation s) {

        setText(view, R.id.txtDetailRoad,
                valueOrFallback(s.getRoadName(), "Unknown road"));

        setText(view, R.id.txtDetailType,
                valueOrFallback(s.getType(), "Unknown type"));

        setText(view, R.id.txtDetailDescription,
                valueOrFallback(s.getDescription(),
                        "No description available"));

        setText(view, R.id.txtDetailDirection,
                "Direction: " + valueOrFallback(
                        s.getDirection(), "Not specified"));

        setText(view, R.id.txtDetailDelay,
                "Delay: " + valueOrFallback(
                        s.getDelayType(), "Not specified"));

        setText(view, R.id.txtDetailDates,
                "Start: " + valueOrFallback(
                        s.getStartDate(), "Not specified")
                        + "\nEnd: " + valueOrFallback(
                        s.getEndDate(), "Not specified"));

        setText(view, R.id.txtDetailLocation,
                "From: " + s.getFromLatitude()
                        + ", " + s.getFromLongitude()
                        + "\nTo: " + s.getToLatitude()
                        + ", " + s.getToLongitude());
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback : value;
    }

    private void setText(View view, int id, String text) {
        ((TextView) view.findViewById(id)).setText(text);
    }
}
