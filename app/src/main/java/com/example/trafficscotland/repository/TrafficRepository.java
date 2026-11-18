package com.example.trafficscotland.repository;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.example.trafficscotland.BuildConfig;
import com.example.trafficscotland.model.Situation;
import com.example.trafficscotland.parser.TrafficXmlParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TrafficRepository {

    private static final String TAG = "TrafficRepository";

    private static final String BASE_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/";

    private static final String INCIDENTS_URL =
            BASE_URL + "UnplannedEvents/Content.xml";

    private static final String ROADWORKS_URL =
            BASE_URL + "CurrentRoadworks/Content.xml";

    private static final String INCIDENTS_CACHE =
            "cached_incidents.xml";

    private static final String ROADWORKS_CACHE =
            "cached_roadworks.xml";

    private final Context context;
    private final TrafficXmlParser parser = new TrafficXmlParser();

    public TrafficRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public ArrayList<Situation> getSituations() throws Exception {

        ArrayList<Situation> allSituations = new ArrayList<>();

        // Download incidents or use cached incidents.
        allSituations.addAll(
                loadFeed(
                        INCIDENTS_URL,
                        INCIDENTS_CACHE,
                        "Incident"
                )
        );

        // Download roadworks or use cached roadworks.
        allSituations.addAll(
                loadFeed(
                        ROADWORKS_URL,
                        ROADWORKS_CACHE,
                        "Roadworks"
                )
        );

        return allSituations;
    }

    private ArrayList<Situation> loadFeed(
            String url,
            String cacheFile,
            String situationType) throws Exception {

        Exception networkError;

        try {
            // Attempt to download the latest XML.
            String xml = downloadXml(url);

            // Parse before saving to avoid caching invalid XML.
            ArrayList<Situation> situations =
                    parser.parse(xml, situationType);

            // Save the successfully parsed XML.
            try {
                saveCache(cacheFile, xml);
            } catch (Exception cacheError) {
                Log.w(TAG,
                        "Unable to save " + situationType + " cache",
                        cacheError);
            }

            Log.d(TAG, situationType + ": loaded live data");

            return situations;

        } catch (Exception error) {
            networkError = error;

            Log.w(TAG,
                    situationType + ": live download failed",
                    error);
        }

        // Fall back to the most recently cached XML.
        try {
            String cachedXml = readCache(cacheFile);

            ArrayList<Situation> situations =
                    parser.parse(cachedXml, situationType);

            Log.d(TAG, situationType + ": loaded cached data");

            return situations;

        } catch (Exception cacheError) {

            Exception combinedError = new Exception(
                    "Unable to load " + situationType +
                            ": live data unavailable and no usable cache found.",
                    networkError
            );

            combinedError.addSuppressed(cacheError);
            throw combinedError;
        }
    }

    private void saveCache(
            String fileName,
            String xml) throws Exception {

        File target = new File(context.getFilesDir(), fileName);
        File temporary = new File(
                context.getFilesDir(),
                fileName + ".tmp"
        );

        try (FileOutputStream output =
                     new FileOutputStream(temporary)) {

            output.write(xml.getBytes(StandardCharsets.UTF_8));
            output.flush();
            output.getFD().sync();
        }

        // Replace the previous cache only after writing succeeds.
        if (!temporary.renameTo(target)) {
            throw new Exception(
                    "Could not replace cache: " + fileName
            );
        }
    }

    private String readCache(String fileName) throws Exception {

        File file = new File(context.getFilesDir(), fileName);

        if (!file.exists()) {
            throw new Exception(
                    "No cached data available: " + fileName
            );
        }

        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            return output.toString("UTF-8");
        }
    }

    // Timestamp of the last successful cache update.
    public long getLastUpdated(String situationType) {

        String fileName =
                "Roadworks".equalsIgnoreCase(situationType)
                        ? ROADWORKS_CACHE
                        : INCIDENTS_CACHE;

        File file = new File(context.getFilesDir(), fileName);

        return file.exists() ? file.lastModified() : 0L;
    }

    private String downloadXml(String urlString) throws Exception {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            String credentials =
                    BuildConfig.TRAFFIC_CLIENT_ID + ":" +
                            BuildConfig.TRAFFIC_CLIENT_KEY;

            String encoded = Base64.encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
            );

            // Preserve the authentication format
            // already working with your API.
            connection.setRequestProperty(
                    "Authorization",
                    encoded
            );

            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception(
                        "Traffic Scotland HTTP error: " + responseCode
                );
            }

            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output =
                         new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                return output.toString("UTF-8");
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}