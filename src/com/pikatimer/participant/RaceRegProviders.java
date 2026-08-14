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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry of available {@link RaceRegProvider} implementations.
 *
 * <p>The import wizard asks this registry for the list of providers to show
 * in the View1 provider ComboBox, and for an auto-detected match when the
 * user pastes a base URL. New providers are added by appending an instance
 * to {@link #PROVIDERS} below; the wizard picks them up with no further
 * wiring.
 *
 * <p>This is a small, explicit, hand-maintained list rather than a
 * {@link java.util.ServiceLoader} discovery: PikaTimer currently bundles a
 * single generic stub, and the set of real providers is expected to stay
 * small enough that a ServiceLoader's classpath/manifest machinery would be
 * more cost than benefit. If that changes, swapping the body of
 * {@link #all()} to use {@code ServiceLoader.load(RaceRegProvider.class)}
 * is a localised change.
 */
public final class RaceRegProviders {

    /**
     * The ordered list of registered providers. The first entry is the
     * default selection in the wizard's ComboBox; auto-detect
     * ({@link #match(String)}) walks the list in order and returns the first
     * match, so put more-specific providers before more-generic ones.
     */
    private static final List<RaceRegProvider> PROVIDERS = buildList();

    private static List<RaceRegProvider> buildList() {
        List<RaceRegProvider> list = new ArrayList<>();
        // The static PikaTimer test API. Sits above the generic fallback so
        // its matches() is consulted first; as the first entry it is also the
        // default ComboBox selection. (The test host is entered by the user,
        // not hard-coded, so no personal URL is baked into the source.)
        list.add(new PikaTestRaceRegProvider());
        // Generic fallback: claims no URL, so it never auto-detects but is
        // always available for manual selection. Real providers should be
        // inserted ABOVE this one so their matches() is consulted first.
        list.add(new GenericRaceRegProvider());
        return Collections.unmodifiableList(list);
    }

    private RaceRegProviders() {
        // no instances
    }

    /**
     * All registered providers, in priority order. The returned list is
     * unmodifiable.
     *
     * @return the provider list; never null, never empty
     */
    public static List<RaceRegProvider> all() {
        return PROVIDERS;
    }

    /**
     * The default provider (the first in the list), used as the initial
     * ComboBox selection before the user has entered a URL.
     *
     * @return the default provider; never null
     */
    public static RaceRegProvider defaultProvider() {
        return PROVIDERS.get(0);
    }

    /**
     * Auto-detect a provider from a base URL. Walks the registered providers
     * in order and returns the first whose {@link RaceRegProvider#matches}
     * returns true. Returns {@code null} if none claim the URL (in which case
     * the wizard leaves the ComboBox on its current/manual selection).
     *
     * @param baseURL the base URL the user entered; may be null or empty
     * @return the matching provider, or null if none match
     */
    public static RaceRegProvider match(String baseURL) {
        if (baseURL == null || baseURL.trim().isEmpty()) {
            return null;
        }
        String url = baseURL.trim();
        for (RaceRegProvider p : PROVIDERS) {
            if (p.matches(url)) {
                return p;
            }
        }
        return null;
    }
}