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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of a {@link RaceRegProvider#fetchParticipants} call: the
 * participant rows as a JDBC {@link ResultSet}, plus any custom-attribute
 * definitions the provider advertised alongside those rows.
 *
 * <p>Providers are not required to advertise definitions; when they don't
 * (or when they do but the event uses no custom attributes), the list is
 * empty. The import wizard offers to set up the advertised definitions in the
 * event file (creating {@link CustomAttribute} rows) before the mapping step,
 * so the extra columns are mappable targets rather than dead columns.
 */
public final class ParticipantExport {

    private final ResultSet resultSet;
    private final List<CustomAttribute> customAttributes;

    /**
     * @param resultSet the participant rows; never null
     */
    public ParticipantExport(ResultSet resultSet) {
        this(resultSet, new ArrayList<CustomAttribute>());
    }

    /**
     * @param resultSet         the participant rows; never null
     * @param customAttributes  the advertised definitions, or null/empty
     *                          when the export carries none
     */
    public ParticipantExport(ResultSet resultSet, List<CustomAttribute> customAttributes) {
        this.resultSet = resultSet;
        this.customAttributes = customAttributes == null
                ? new ArrayList<CustomAttribute>()
                : new ArrayList<>(customAttributes);
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    /**
     * The custom-attribute definitions carried by the export. Never null;
     * empty when the provider advertised none.
     *
     * @return an unmodifiable list of definitions
     */
    public List<CustomAttribute> getCustomAttributes() {
        return Collections.unmodifiableList(customAttributes);
    }
}
