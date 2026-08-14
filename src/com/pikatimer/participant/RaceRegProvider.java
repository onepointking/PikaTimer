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
 * Adapter for a single race-registration provider's API.
 *
 * <p>Each provider (RunReg, UltraSignup, RaceRoster, a custom PikaTimer peer,
 * etc.) has its own event schema, participants-export schema, URL conventions,
 * and authentication scheme. Rather than encoding all of that variability in
 * the import wizard or in a declarative config file, every provider gets its
 * own implementation of this interface. The wizard stays provider-agnostic:
 * it calls {@link #fetchEvents} to populate the event picker and
 * {@link #fetchParticipants} to pull the selected event's registrations,
 * without knowing how either is actually accomplished.
 *
 * <p>The contract between the two methods is deliberately the only coupling:
 * the {@link RemoteEvent} instances returned by {@code fetchEvents} are opaque
 * tokens that {@code fetchParticipants} knows how to turn into a participants
 * export. The wizard never inspects event internals.
 *
 * <p>Implementations must be safe to call on a background thread (the wizard
 * runs them inside a {@link javafx.concurrent.Task}); they should not touch
 * the FX scene graph. They are expected to throw on failure rather than return
 * empty/null, so the wizard can surface the error message.
 */
public interface RaceRegProvider {

    /**
     * Human-readable name for this provider, shown in the View1 provider
     * ComboBox (e.g. "RunReg", "UltraSignup", "Generic HTTP").
     *
     * @return a short, stable display name; never null
     */
    String displayName();

    /**
     * Hint used to auto-select this provider from a pasted base URL. The
     * registry asks each provider in turn whether it recognises the URL;
     * the first match wins. Return {@code false} for a manual-only provider
     * (one the user must pick explicitly), or match on the host/path pattern
     * that uniquely identifies this provider's API.
     *
     * <p>This is a hint, not a security boundary: a {@code true} result just
     * pre-selects the ComboBox entry, and the user can still override it.
     *
     * @param baseURL the base URL the user entered, already trimmed; never null
     * @return true if this provider claims to handle the given base URL
     */
    boolean matches(String baseURL);

    /**
     * Fetch the list of importable events from the provider.
     *
     * <p>Implementations are responsible for whatever authentication the
     * provider requires (basic auth, bearer token, cookie exchange, etc.)
     * using the supplied credentials, and for building the correct events
     * endpoint URL from the base. The returned {@link RemoteEvent}s carry
     * enough state for {@link #fetchParticipants} to retrieve the
     * registrations for a chosen event.
     *
     * @param baseURL  the provider base URL (e.g. "https://api.runreg.com")
     * @param username the entered username; may be empty if the provider
     *                 does not use one
     * @param password the entered password/token; may be empty
     * @return the parsed list of events; never null (empty if the provider
     *         legitimately has no events)
     * @throws Exception if the fetch or parse fails; the message is shown
     *                   to the user via the fetch status label
     */
    List<RemoteEvent> fetchEvents(String baseURL, String username, String password) throws Exception;

    /**
     * Fetch the participants export for a single event.
     *
     * <p>The returned {@link ParticipantExport} carries the participant rows
     * as a JDBC {@link ResultSet} (so it can feed straight into the existing
     * View2/View3 CSV pipeline, which already iterates a {@code ResultSet}
     * from {@code org.h2.tools.Csv}) together with any custom-attribute
     * definitions the provider advertised alongside the rows. Providers whose
     * export carries no custom attributes return an empty definitions list.
     *
     * <p>The {@code event} argument is one of the {@link RemoteEvent}s
     * returned by {@link #fetchEvents}; implementations may pull the event
     * id, a direct participants URL, or provider-specific extras from
     * {@link RemoteEvent#getRaw()}.
     *
     * @param event    the event selected by the user; never null
     * @param username the entered username; may be empty
     * @param password the entered password/token; may be empty
     * @return the participants export (rows plus optional custom-attribute
     *         definitions); never null
     * @throws Exception if the fetch or parse fails
     */
    ParticipantExport fetchParticipants(RemoteEvent event, String username, String password) throws Exception;
}