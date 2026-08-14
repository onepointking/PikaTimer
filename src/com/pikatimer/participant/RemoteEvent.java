/*
 * Copyright (C) 2017
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

import java.util.Objects;
import org.json.JSONObject;

/**
 * A lightweight, immutable descriptor for a single event exposed by a remote
 * race-registration provider's {@code /events} endpoint.
 *
 * <p>The fields are intentionally permissive: providers differ wildly in what
 * metadata they expose, so only {@code id} and {@code name} are required. The
 * raw JSON is retained so downstream code (View2/View3) can pull provider-
 * specific extras (date, distance, cap, etc.) without round-tripping back to
 * the server.
 *
 * <p>{@code toString()} returns the human-readable name so instances render
 * nicely inside a JavaFX {@code ComboBox<RemoteEvent>}.
 */
public final class RemoteEvent {

    private final String id;
    private final String name;
    private final String date;
    private final String participantsURL;
    private final JSONObject raw;

    /**
     * Build a RemoteEvent from a single JSON object returned by the
     * provider's {@code /events} listing.
     *
     * <p>Recognised keys (all optional except {@code id} and {@code name}):
     * <ul>
     *   <li>{@code id}            - provider-scoped event identifier</li>
     *   <li>{@code name}          - human-readable event name</li>
     *   <li>{@code date}          - event date (free-form string)</li>
     *   <li>{@code participantsURL} / {@code participants_url} /
     *       {@code participantURL} - direct URL to the participants export;
     *       if absent, the provider derives it from the base URL + id</li>
     * </ul>
     * Unknown keys are preserved in {@link #getRaw()}.
     *
     * @param json a single event object from the {@code /events} response
     */
    public RemoteEvent(JSONObject json) {
        this.raw = json;
        this.id = json.optString("id", "");
        this.name = json.optString("name", json.optString("title", id));
        this.date = json.optString("date", json.optString("eventDate", ""));
        this.participantsURL = json.optString("participantsURL",
                json.optString("participants_url",
                        json.optString("participantURL", "")));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    /**
     * Direct URL to the participants export for this event, if the provider
     * exposed one. May be empty; in that case the caller should derive the
     * URL from the base {@code sourceURL} and the event {@code id}.
     *
     * @return the participants URL or an empty string if not provided
     */
    public String getParticipantsURL() {
        return participantsURL;
    }

    /**
     * The original JSON object for this event, so downstream code can read
     * provider-specific fields without re-fetching.
     *
     * @return the raw JSON; never null
     */
    public JSONObject getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (!date.isEmpty()) {
            sb.append(" (").append(date).append(")");
        }
        if (sb.length() == 0) {
            return id;
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteEvent)) return false;
        RemoteEvent that = (RemoteEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}