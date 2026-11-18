
package com.example.trafficscotland.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trafficscotland.R;
import com.example.trafficscotland.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SituationAdapter
        extends RecyclerView.Adapter<SituationAdapter.ViewHolder> {

    private final List<Situation> situations = new ArrayList<>();

    private OnSituationClickListener listener;

    public interface OnSituationClickListener {
        void onSituationClick(Situation situation);
    }

    public void setOnSituationClickListener(
            OnSituationClickListener listener) {
        this.listener = listener;
    }

    public void updateSituations(List<Situation> newSituations) {
        situations.clear();
        situations.addAll(newSituations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_situation, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {

        Situation situation = situations.get(position);

        String roadName = situation.getRoadName();

        holder.txtRoadName.setText(
                roadName == null || roadName.isEmpty()
                        ? "Unknown road"
                        : roadName
        );

        holder.txtType.setText(situation.getType());

        holder.txtDescription.setText(
                situation.getDescription() == null
                        ? "No description available"
                        : situation.getDescription()
        );

        // Choose icon based on situation information
        int iconResource = getSituationIcon(situation);
        holder.imgSituationIcon.setImageResource(iconResource);

        // Colour-coded delay label
        displayDelay(
                holder.txtDelay,
                situation.getDelayType()
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSituationClick(situation);
            }
        });
    }

    private int getSituationIcon(Situation situation) {

        String recordType = situation.getRecordType();
        String description = situation.getDescription();

        String text = (
                (recordType == null ? "" : recordType)
                        + " "
                        + (description == null ? "" : description)
        ).toLowerCase(Locale.ROOT);

        // Road closures
        if (text.contains("closure") ||
                text.contains("closed") ||
                text.contains("blocked")) {
            return R.drawable.ic_road_closed;
        }

        // Accidents
        if (text.contains("accident") ||
                text.contains("collision") ||
                text.contains("crash")) {
            return R.drawable.ic_accident;
        }

        // Congestion
        if (text.contains("congestion") ||
                text.contains("heavy traffic") ||
                text.contains("queue")) {
            return R.drawable.ic_congestion;
        }

        // Weather
        if (text.contains("weather") ||
                text.contains("snow") ||
                text.contains("ice") ||
                text.contains("flood") ||
                text.contains("wind")) {
            return R.drawable.ic_weather;
        }

        // Roadworks
        if ("Roadworks".equalsIgnoreCase(situation.getType())) {
            return R.drawable.ic_roadworks;
        }

        // Other incidents
        return R.drawable.ic_incident;
    }

    @Override
    public int getItemCount() {
        return situations.size();
    }

    private void displayDelay(TextView textView, String delayType) {

        if (delayType == null || delayType.trim().isEmpty()) {
            textView.setVisibility(View.GONE);
            return;
        }

        String delay = delayType.trim()
                .toLowerCase(Locale.ROOT);

        int backgroundColor;
        int textColor;
        String label;

        switch (delay) {

            case "longdelays":
                backgroundColor = Color.rgb(254, 226, 226);
                textColor = Color.rgb(153, 27, 27);
                label = "Long delays";
                break;

            case "mediumdelays":
                backgroundColor = Color.rgb(255, 237, 213);
                textColor = Color.rgb(154, 52, 18);
                label = "Medium delays";
                break;

            case "shortdelays":
                backgroundColor = Color.rgb(220, 252, 231);
                textColor = Color.rgb(22, 101, 52);
                label = "Short delays";
                break;

            default:
                backgroundColor = Color.rgb(229, 231, 235);
                textColor = Color.rgb(55, 65, 81);
                label = delayType;
                break;
        }

        GradientDrawable background = new GradientDrawable();
        background.setColor(backgroundColor);
        background.setCornerRadius(
                12 * textView.getResources()
                        .getDisplayMetrics().density
        );

        textView.setBackground(background);
        textView.setTextColor(textColor);
        textView.setText(label);
        textView.setVisibility(View.VISIBLE);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtRoadName;
        TextView txtType;
        TextView txtDescription;
        TextView txtDelay;
        ImageView imgSituationIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtRoadName = itemView.findViewById(R.id.txtRoadName);
            txtType = itemView.findViewById(R.id.txtType);
            txtDescription = itemView.findViewById(
                    R.id.txtDescription);
            txtDelay = itemView.findViewById(R.id.txtDelay);
            imgSituationIcon = itemView.findViewById(
                    R.id.imgSituationIcon);
        }
    }
}
