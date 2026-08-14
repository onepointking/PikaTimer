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

import java.sql.ResultSet;
import java.util.List;

/**
 * A placeholder {@link RaceRegProvider} that claims no URLs and refuses to
 * fetch. It exists so the import wizard's provider ComboBox is never empty
 * and so the failure path is exercised end-to-end before any real adapter
 * is written.
 *
 * <p>This is the bottom entry in {@link RaceRegProviders}'s list: it never
 * auto-detects ({@link #matches} returns false for every URL), so it only
 * takes effect when the user manually selects "Generic HTTP" from the
 * ComboBox. Real provider adapters should be registered above this one.
 *
 * <p>When you implement a real adapter, copy this class's shape: a
 * provider-specific {@code matches()} (usually a host check), a
 * {@code fetchEvents()} that does the auth dance and parses the provider's
 * event schema into {@link RemoteEvent}s, and a {@code fetchParticipants()}
 * that turns a selected event into a {@link ParticipantExport}. Keep each
 * provider's quirks inside its own class.
 */
public class GenericRaceRegProvider implements RaceRegProvider {

    @Override
    public String displayName() {
        return "Generic HTTP";
    }

    /**
     * Never auto-detect. The generic provider is a manual fallback only;
     * real providers should return true for their own hosts.
     */
    @Override
    public boolean matches(String baseURL) {
        return false;
    }

    @Override
    public List<RemoteEvent> fetchEvents(String baseURL, String username, String password) throws Exception {
        throw new UnsupportedOperationException(
                "No race-reg provider is configured for this URL. "
                + "Implement a RaceRegProvider adapter for your provider and register it "
                + "in RaceRegProviders.");
    }

    @Override
    public ParticipantExport fetchParticipants(RemoteEvent event, String username, String password) throws Exception {
        throw new UnsupportedOperationException(
                "No race-reg provider is configured to fetch participants. "
                + "Implement fetchParticipants() in the matching RaceRegProvider adapter.");
    }
}