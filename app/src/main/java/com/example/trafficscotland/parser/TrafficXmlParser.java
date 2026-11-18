package com.example.trafficscotland.parser;

import android.util.Xml;

import com.example.trafficscotland.model.Situation;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;

public class TrafficXmlParser {

    public ArrayList<Situation> parse(String xmlData, String feedType)
            throws Exception {

        ArrayList<Situation> situations = new ArrayList<>();

        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(
                XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(xmlData));

        Situation current = null;
        ArrayList<String> path = new ArrayList<>();

        int event = parser.getEventType();

        while (event != XmlPullParser.END_DOCUMENT) {

            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                path.add(tag);

                if (tag.equals("situation")) {
                    current = new Situation();
                    current.setId(
                            parser.getAttributeValue(null, "id"));
                    current.setType(feedType);
                }

                if (current != null &&
                        tag.equals("situationRecord")) {

                    String recordType = parser.getAttributeValue(
                            "http://www.w3.org/2001/XMLSchema-instance",
                            "type");

                    current.setRecordType(recordType);
                }

                if (current != null && isTextField(tag)) {

                    String value = parser.nextText().trim();

                    if (!value.isEmpty()) {
                        assignValue(current, path, value);
                    }

                    // nextText() consumes the closing tag.
                    path.remove(path.size() - 1);
                }

            } else if (event == XmlPullParser.END_TAG) {

                if (parser.getName().equals("situation")
                        && current != null) {
                    situations.add(current);
                    current = null;
                }

                if (!path.isEmpty()) {
                    path.remove(path.size() - 1);
                }
            }

            event = parser.next();
        }

        return situations;
    }

    private boolean isTextField(String tag) {
        return tag.equals("value")
                || tag.equals("overallStartTime")
                || tag.equals("overallEndTime")
                || tag.equals("delaysType")
                || tag.equals("tpegDirection")
                || tag.equals("latitude")
                || tag.equals("longitude")
                || tag.equals("numberOfLanesRestricted")
                || tag.equals("numberOfOperationalLanes");
    }

    private boolean contains(ArrayList<String> path,
                             String tag) {
        return path.contains(tag);
    }

    private void assignValue(Situation s,
                             ArrayList<String> path,
                             String value) {

        String tag = path.get(path.size() - 1);

        switch (tag) {

            case "overallStartTime":
                s.setStartDate(value);
                break;

            case "overallEndTime":
                s.setEndDate(value);
                break;

            case "delaysType":
                s.setDelayType(value);
                s.setImpact(value);
                break;

            case "tpegDirection":
                s.setDirection(value);
                break;

            case "numberOfLanesRestricted":
                s.setImpact(value + " lanes restricted");
                break;

            case "numberOfOperationalLanes":
                s.setImpact(value + " operational lanes");
                break;

            case "latitude":
                if (contains(path, "from")) {
                    s.setFromLatitude(Double.parseDouble(value));
                } else if (contains(path, "to")) {
                    s.setToLatitude(Double.parseDouble(value));
                } else {
                    s.setLatitude(Double.parseDouble(value));
                }
                break;

            case "longitude":
                if (contains(path, "from")) {
                    s.setFromLongitude(Double.parseDouble(value));
                } else if (contains(path, "to")) {
                    s.setToLongitude(Double.parseDouble(value));
                } else {
                    s.setLongitude(Double.parseDouble(value));
                }
                break;

            case "value":

                if (contains(path, "generalPublicComment")) {

                    s.setDescription(value);

                } else if (contains(path, "groupOfLocations")) {

                    String road = extractRoadName(value);

                    if (!road.isEmpty() &&
                            (s.getRoadName() == null ||
                                    s.getRoadName().isEmpty())) {

                        s.setRoadName(road);
                    }

                    if (contains(path, "from")) {
                        s.setLocationFrom(value);
                    } else if (contains(path, "to")) {
                        s.setLocationTo(value);
                    }
                }

                break;
        }
    }

    private String extractRoadName(String text) {

        if (text == null || text.isEmpty()) {
            return "";
        }

        String[] words = text.trim().split("\\s+");

        for (String word : words) {

            if (word.matches("(?i)^(M|A|B)\\d+(\\(M\\))?$")) {
                return word.toUpperCase();
            }
        }

        return "";
    }
}