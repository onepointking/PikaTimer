/* 
 * Copyright (C) 2017 John Garner
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


import com.pikatimer.race.Wave;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author jcgarner
 */
class ImportWizardData {
    private final StringProperty filename = new SimpleStringProperty();
    private final StringProperty sourceURL = new SimpleStringProperty();
    private final StringProperty webUsername = new SimpleStringProperty();
    private final StringProperty webPassword = new SimpleStringProperty();
    // The race-reg provider adapter chosen by the user (or auto-detected
    // from the URL). View1's provider ComboBox writes to this; fetchEvents()
    // and (eventually) View2/View3 delegate to it. Null until set.
    private final ObjectProperty<RaceRegProvider> selectedProvider = new SimpleObjectProperty<>();
    // Remote event picker state for the web import flow.
    // The provider's /events endpoint is queried once the user enters a base
    // URL + credentials; the resulting RemoteEvent list is exposed here so the
    // View1 ComboBox can bind to it. selectedRemoteEvent is what View2/View3
    // will eventually use to fetch the participants export.
    private final ObservableList<RemoteEvent> remoteEvents = FXCollections.observableArrayList();
    private final ObjectProperty<RemoteEvent> selectedRemoteEvent = new SimpleObjectProperty<>();
    private final BooleanProperty fetchingEvents = new SimpleBooleanProperty(false);
    private final StringProperty fetchEventsStatus = new SimpleStringProperty();
    private final BooleanProperty waveAssignByBib = new SimpleBooleanProperty();
    private final BooleanProperty waveAssignByAttribute = new SimpleBooleanProperty();
    private final BooleanProperty clearExistingAttribute = new SimpleBooleanProperty();
    private final BooleanProperty nextButtonEnabledAttribute = new SimpleBooleanProperty(true);
    private final StringProperty duplicateHandlingAttribute = new SimpleStringProperty();

    private Wave assignedWave; 
    private final Map<String, String> attributeMap = new HashMap<>();
    private ResultSet rs = null;
    private int numToAdd = 0; 
    
    public int getNumToAdd() {
        return numToAdd;
    }
    public void setNumToAdd(int i) {
        numToAdd = i; 
    }
    public String getFileName() {
        return filename.getValueSafe();
    }
    public void setFileName(String fName) {
        System.out.println("setFileName: from " + filename.getValueSafe() + " to " + fName);
        filename.setValue(fName);
    }
    public StringProperty fileNameProperty() {
        return filename; 
    }        
    
    public String getSourceURL() {
        return sourceURL.getValueSafe();
    }
    public void setSourceURL(String url) {
        sourceURL.setValue(url);
    }
    public StringProperty sourceURLProperty() {
        return sourceURL;
    }
    
    public String getWebUsername() {
        return webUsername.getValueSafe();
    }
    public void setWebUsername(String username) {
        webUsername.setValue(username);
    }
    public StringProperty webUsernameProperty() {
        return webUsername;
    }
    
    public String getWebPassword() {
        return webPassword.getValueSafe();
    }
    public void setWebPassword(String password) {
        webPassword.setValue(password);
    }
    public StringProperty webPasswordProperty() {
        return webPassword;
    }

    /**
     * The provider adapter currently in effect for the web import flow.
     * Set by the View1 provider ComboBox (either manually or via URL
     * auto-detect); read by fetchEvents() and, later, by View2/View3 to
     * fetch participants.
     */
    public RaceRegProvider getSelectedProvider() {
        return selectedProvider.get();
    }
    public void setSelectedProvider(RaceRegProvider p) {
        selectedProvider.set(p);
    }
    public ObjectProperty<RaceRegProvider> selectedProviderProperty() {
        return selectedProvider;
    }

    /**
     * The list of events returned by the remote provider's {@code /events}
     * endpoint. Bound to the View1 event ComboBox; cleared and repopulated
     * whenever a fetch completes.
     */
    public ObservableList<RemoteEvent> getRemoteEvents() {
        return remoteEvents;
    }

    /**
     * The event the user picked from the list, or null if none selected.
     * View2/View3 will use this (together with the base sourceURL) to fetch
     * the participants export.
     */
    public RemoteEvent getSelectedRemoteEvent() {
        return selectedRemoteEvent.get();
    }
    public void setSelectedRemoteEvent(RemoteEvent e) {
        selectedRemoteEvent.set(e);
    }
    public ObjectProperty<RemoteEvent> selectedRemoteEventProperty() {
        return selectedRemoteEvent;
    }

    /**
     * True while a background fetch of the event list is in flight. The View1
     * UI uses this to disable the event ComboBox and show a "Fetching..."
     * indicator, and to keep the Next button disabled until the fetch settles.
     */
    public Boolean isFetchingEvents() {
        return fetchingEvents.get();
    }
    public void setFetchingEvents(boolean b) {
        fetchingEvents.set(b);
    }
    public BooleanProperty fetchingEventsProperty() {
        return fetchingEvents;
    }

    /**
     * Human-readable status for the last event-list fetch: empty on success,
     * an error message on failure, or a transient hint ("Fetching...") while
     * in flight. Bound to the View1 status label under the event ComboBox.
     */
    public String getFetchEventsStatus() {
        return fetchEventsStatus.getValueSafe();
    }
    public void setFetchEventsStatus(String s) {
        fetchEventsStatus.set(s);
    }
    public StringProperty fetchEventsStatusProperty() {
        return fetchEventsStatus;
    }

    public StringProperty duplicateHandlingProperty(){
        return duplicateHandlingAttribute;
    }
    
    public void setResultsSet(ResultSet r) {
        rs = r;
    }
    public ResultSet getResultSet() {
        return rs; 
    }
    
    public BooleanProperty  waveAssignByAttributeProperty() {
        return waveAssignByAttribute; 
    }
    public Boolean getWaveAssignByAttribute() {
        return waveAssignByAttribute.getValue(); 
    }
    public BooleanProperty  waveAssignByBibProperty() {
        return waveAssignByBib; 
    }
    public Boolean getWaveAssignByBib() {
        return waveAssignByBib.getValue(); 
    }
    public void setAssignedWave(Wave w) {
        assignedWave=w; 
    }
    public Wave getAssignedWave(){
        return assignedWave; 
    }
    
    public BooleanProperty  clearExistingProperty() {
        return clearExistingAttribute; 
    }

    public BooleanProperty nextButtonDisabledProperty(){
        return nextButtonEnabledAttribute;
    }
    
    public void mapAttrib(String k, String v) {
        attributeMap.put(k, v); 
        System.out.println("ImportWizardData: Setting " + k + " to " + v);
    }
    public Map getAttributeMap() {
        return attributeMap;
    }
    
}
