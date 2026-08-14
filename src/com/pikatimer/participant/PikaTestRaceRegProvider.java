/*
 * Copyright (C) 2026
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pikatimer.participant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.h2.tools.SimpleResultSet;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A {@link RaceRegProvider} adapter for the static PikaTimer test API used
 * to exercise the Import Wizard's "Import from Web" flow.
 *
 * <p>The base URL is entered by the user in the wizard (typically their own
 * test server) rather than baked into the source, so no personal host name is
 * exposed in the code. The API itself is deliberately simple and
 * unauthenticated, and is used to exercise
 * the Import Wizard's "Import from Web" flow against realistic data. The API
 * shape is a minimal PikaTimer-style schema:
 *
 * <ul>
 *   <li>{@code GET {base}/events} returns a JSON <em>array</em> of events,
 *       each with {@code id}, {@code name}, {@code date} and a direct
 *       {@code participantURL}.</li>
 *   <li>{@code GET {participantURL}} returns a JSON <em>object</em> with an
 *       {@code eventId}, {@code participantCount} and a {@code participants}
 *       array of {@code {bib, firstName, lastName, gender, age, division,
 *       city, state, email, registered}} objects.</li>
 *   <li>When the event uses custom participant attributes, the same payload
 *       carries an optional {@code pikaCustomAttributes} object mapping
 *       attribute name to a {@link CustomAttributeType} display name, e.g.
 *       {@code {"ward": "String"}}. Each named attribute also becomes a
 *       column in the participants ResultSet, populated from the matching
 *       per-participant key (absent when the event defines none).</li>
 * </ul>
 *
 * <p>Because the endpoint is public, the {@code username}/{@code password}
 * arguments to {@link #fetchEvents} / {@link #fetchParticipants} are ignored;
 * they exist only to satisfy the {@link RaceRegProvider} contract.
 */
public class PikaTestRaceRegProvider implements RaceRegProvider {

    private static final String EVENTS_PATH = "/events";
    private static final String PARTICIPANTS_PATH = "/participants";

    @Override
    public String displayName() {
        return "PikaTimer Test API";
    }

    /**
     * Auto-detect this provider from the URL's <em>path</em> rather than its
     * host: any http(s) URL whose path contains {@code pika.php} is claimed
     * by the test API. Matching on the path keeps the provider recognisable
     * without embedding a personal host name in the source.
     */
    @Override
    public boolean matches(String baseURL) {
        if (baseURL == null) {
            return false;
        }
        String url = baseURL.trim();
        if (!url.toLowerCase().startsWith("http")) {
            return false;
        }
        try {
            String path = new URL(url).getPath();
            return path != null && path.contains("pika.php");
        } catch (MalformedURLException ex) {
            // Oddly formed input; fall back to a plain substring check.
            return url.contains("pika.php");
        }
    }

    @Override
    public List<RemoteEvent> fetchEvents(String baseURL, String username, String password) throws Exception {
        String endpoint = eventsEndpoint(baseURL);
        String body = httpGet(endpoint);
        JSONArray events = new JSONArray(body);
        List<RemoteEvent> result = new ArrayList<>();
        for (int i = 0; i < events.length(); i++) {
            result.add(new RemoteEvent(events.getJSONObject(i)));
        }
        return result;
    }

    @Override
    public ParticipantExport fetchParticipants(RemoteEvent event, String username, String password) throws Exception {
        // The events listing exposes a direct participantURL per event; prefer
        // the normalized field (populated from participantURL) and fall back
        // to the raw JSON key, then to deriving it from the event id.
        String url = event.getParticipantsURL();
        if (url.isEmpty()) {
            url = event.getRaw().optString("participantURL", "");
        }
        if (url.isEmpty()) {
            url = event.getRaw().optString("participant_url", "");
        }
        if (url.isEmpty() && !event.getId().isEmpty()) {
            url = eventsEndpointFromRaw(event.getRaw()) + "/" + event.getId() + PARTICIPANTS_PATH;
        }
        if (url.isEmpty()) {
            throw new IOException("No participants URL available for event \"" + event.getName() + "\"");
        }

        String body = httpGet(url);
        JSONObject payload = new JSONObject(body);
        JSONArray participants = payload.optJSONArray("participants");
        if (participants == null) {
            // Tolerate a bare array too, mirroring the events endpoint.
            participants = new JSONArray(body);
        }

        // Optional custom-attribute definitions advertised in the same
        // payload: "pikaCustomAttributes": {"ward": "String"} — an object
        // mapping attribute name to a CustomAttributeType display name. It is
        // absent when the event defines no custom attributes. Each named
        // attribute becomes both a CustomAttribute definition (for the View2
        // setup dialog) and a column in the ResultSet below.
        JSONObject customAttrSpec = payload.optJSONObject("pikaCustomAttributes");
        List<CustomAttribute> customAttributes = new ArrayList<>();
        List<String> customAttrColumns = new ArrayList<>();
        if (customAttrSpec != null) {
            for (Iterator<String> keys = customAttrSpec.keys(); keys.hasNext();) {
                String attrName = keys.next();
                CustomAttribute ca = new CustomAttribute();
                ca.setName(attrName);
                ca.setAttributeType(parseAttributeType(customAttrSpec.optString(attrName, "")));
                customAttributes.add(ca);
                customAttrColumns.add(attrName);
            }
        }

        SimpleResultSet rs = new SimpleResultSet();
        // SimpleResultSet defaults to autoClose=true, which makes it report
        // TYPE_FORWARD_ONLY and causes beforeFirst() to throw H2 error 90128
        // ("result set is not scrollable and can not be reset"). View2/View3
        // rewind the ResultSet (count rows, then import), so keep the full
        // row set in memory and make it scrollable.
        rs.setAutoClose(false);
        rs.addColumn("bib", Types.INTEGER, 10, 0);
        rs.addColumn("firstName", Types.VARCHAR, 100, 0);
        rs.addColumn("lastName", Types.VARCHAR, 100, 0);
        rs.addColumn("gender", Types.VARCHAR, 1, 0);
        rs.addColumn("age", Types.INTEGER, 3, 0);
        rs.addColumn("division", Types.VARCHAR, 50, 0);
        rs.addColumn("city", Types.VARCHAR, 100, 0);
        rs.addColumn("state", Types.VARCHAR, 2, 0);
        rs.addColumn("email", Types.VARCHAR, 200, 0);
        rs.addColumn("registered", Types.BOOLEAN, 1, 0);
        for (String col : customAttrColumns) {
            rs.addColumn(col, Types.VARCHAR, 100, 0);
        }

        for (int i = 0; i < participants.length(); i++) {
            JSONObject p = participants.getJSONObject(i);
            List<Object> row = new ArrayList<>();
            row.add(p.optInt("bib", 0));
            row.add(p.optString("firstName", ""));
            row.add(p.optString("lastName", ""));
            row.add(p.optString("gender", ""));
            row.add(p.optInt("age", 0));
            row.add(p.optString("division", ""));
            row.add(p.optString("city", ""));
            row.add(p.optString("state", ""));
            row.add(p.optString("email", ""));
            row.add(p.optBoolean("registered", false));
            for (String col : customAttrColumns) {
                row.add(p.optString(col, ""));
            }
            rs.addRow(row.toArray());
        }
        return new ParticipantExport(rs, customAttributes);
    }

    /**
     * Map a JSON type display name onto a {@link CustomAttributeType}, e.g.
     * {@code "String"} → {@code CustomAttributeType.STRING}. Accepts both
     * the display name ({@code toString()}) and the enum constant name, case
     * insensitively. Unknown or blank values default to {@code STRING} so a
     * provider schema change can't break the import.
     */
    private static CustomAttributeType parseAttributeType(String type) {
        if (type != null) {
            for (CustomAttributeType t : CustomAttributeType.values()) {
                if (t.toString().equalsIgnoreCase(type) || t.name().equalsIgnoreCase(type)) {
                    return t;
                }
            }
        }
        return CustomAttributeType.STRING;
    }

    /**
     * Normalise the user-entered base URL into the events endpoint. Accepts
     * both the bare base (e.g. {@code https://example.com/pika.php}) and the
     * full events URL ({@code .../pika.php/events}), with or without a
     * trailing slash, and never appends a duplicate {@code /events}.
     */
    private static String eventsEndpoint(String baseURL) {
        String base = baseURL.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.toLowerCase().endsWith(EVENTS_PATH)) {
            return base;
        }
        return base + EVENTS_PATH;
    }

    /**
     * Reconstruct a plausible base from the raw event JSON when the listing
     * omitted a participants URL. Uses the raw {@code participantURL} if one
     * is present, else the event id, else the empty string (the caller then
     * gives up).
     */
    private static String eventsEndpointFromRaw(JSONObject raw) {
        String base = raw.optString("participantURL",
                raw.optString("participant_url", ""));
        if (base.endsWith(PARTICIPANTS_PATH)) {
            base = base.substring(0, base.length() - PARTICIPANTS_PATH.length());
            int slash = base.lastIndexOf('/');
            if (slash >= 0) {
                base = base.substring(0, slash);
            }
        }
        return base;
    }

    /**
     * Simple blocking GET helper. The events and participants endpoints are
     * static and public, so no auth headers are attached; credentials are
     * intentionally ignored.
     */
    private static String httpGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        try {
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code + " while fetching " + url);
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    body.append(line);
                }
            }
            return body.toString();
        } finally {
            conn.disconnect();
        }
    }
}
