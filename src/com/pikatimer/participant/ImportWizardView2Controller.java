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

import com.pikatimer.race.RaceDAO;
import com.pikatimer.util.CharsetDetector;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javax.annotation.PostConstruct;
import org.h2.tools.Csv;


@FXMLController("FXMLImportWizardView2.fxml")
public class ImportWizardView2Controller {
    @FXMLViewFlowContext
    private ViewFlowContext context;
    
    @FXML
    GridPane mapGridPane;
    
    @FXML
    Label statusLabel;
    
    @PostConstruct
    public void init() throws FlowException {
        System.out.println("ImportWizardView2Controller.initialize()");
        ImportWizardData model = context.getRegisteredObject(ImportWizardData.class);
        //model.setFileName("Test2");

        // --- Source branching ---
        // If a remote event was selected in View1, we are importing from a URL
        // (race registration API). Otherwise we fall through to the local CSV
        // file pipeline.
        ArrayList<String> csvColumns = new ArrayList<>();
        if (model.getSelectedRemoteEvent() != null) {
            // --- URL import path ---
            // Fetch the participants export from the provider adapter on a
            // background thread (it performs real HTTP), then build the mapping
            // grid the same way the file path does below. The ResultSet is kept
            // in the model so View3 can import from it without a second fetch.
            fetchRemoteParticipants(model, csvColumns);
        } else {
            // --- File import path (fully implemented) ---
            // Detect the file encoding (UTF-8 BOM, strict UTF-8, or legacy fallback)
            Charset charset = StandardCharsets.UTF_8;
            try {
                charset = CharsetDetector.detect(model.getFileName());
                System.out.println("Detected charset: " + charset.name());
            } catch (IOException ex) {
                Logger.getLogger(ImportWizardView2Controller.class.getName())
                        .log(Level.WARNING, "Unable to detect charset for " + model.getFileName(), ex);
            }

            ResultSet rs;
            try {
                rs = new Csv().read(model.getFileName(), null, charset.name());
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 0; i < meta.getColumnCount(); i++) {
                    csvColumns.add(meta.getColumnLabel(i+1));
                    System.out.println(meta.getColumnLabel(i+1));
                }
                int numAdded = 0;
                while (rs.next()) { numAdded++; }
                model.setNumToAdd(numAdded);

            } catch (SQLException ex) {
                Logger.getLogger(ImportWizardView2Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
            // The URL path builds the grid from a background task once its
            // fetch completes; the file path can build it right away.
            buildMappingGrid(model, csvColumns);
        }
    }

    /**
     * URL import path: fetch the selected event's participants from the
     * provider adapter on a background thread (it performs real HTTP), keep
     * the ResultSet in the model for View3, and build the mapping grid when
     * the fetch completes. Next stays disabled while the fetch is in flight
     * (and after a failure) so the user can't import against an empty map.
     */
    private void fetchRemoteParticipants(ImportWizardData model, ArrayList<String> csvColumns) {
        RaceRegProvider provider = model.getSelectedProvider();
        RemoteEvent event = model.getSelectedRemoteEvent();
        if (provider == null || event == null) {
            statusLabel.setText("Select an event first");
            model.nextButtonDisabledProperty().set(true);
            return;
        }
        // Never fall back to a stale participant set from a previous event if
        // the fetch below fails.
        model.setResultsSet(null);
        model.nextButtonDisabledProperty().set(true);
        statusLabel.setText("Fetching participants from " + provider.displayName() + "...");

        Task<ParticipantExport> fetchTask = new Task<ParticipantExport>() {
            @Override
            protected ParticipantExport call() throws Exception {
                return provider.fetchParticipants(event,
                        model.getWebUsername(), model.getWebPassword());
            }
        };
        fetchTask.setOnSucceeded(e -> Platform.runLater(() -> {
            try {
                ParticipantExport export = fetchTask.getValue();
                ResultSet rs = export.getResultSet();
                model.setResultsSet(rs);
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 0; i < meta.getColumnCount(); i++) {
                    csvColumns.add(meta.getColumnLabel(i + 1));
                    System.out.println(meta.getColumnLabel(i + 1));
                }
                // Count the rows for the View3 progress bar, then rewind so
                // View3 can iterate the same ResultSet. H2's SimpleResultSet
                // supports beforeFirst(); if a future provider returns a
                // forward-only ResultSet this will throw and be logged.
                int numToAdd = 0;
                while (rs.next()) {
                    numToAdd++;
                }
                try {
                    rs.beforeFirst();
                } catch (SQLException ex) {
                    Logger.getLogger(ImportWizardView2Controller.class.getName())
                            .log(Level.WARNING, "Unable to rewind participants ResultSet", ex);
                }
                model.setNumToAdd(numToAdd);
                statusLabel.setText(numToAdd + " participant(s) loaded from " + provider.displayName());
                // Offer to register any custom-attribute definitions the
                // provider advertised, BEFORE the mapping grid is built so the
                // new columns show up as mappable targets.
                promptForCustomAttributes(export.getCustomAttributes());
                buildMappingGrid(model, csvColumns);
            } catch (Exception ex) {
                Logger.getLogger(ImportWizardView2Controller.class.getName())
                        .log(Level.SEVERE, null, ex);
                statusLabel.setText("Failed to load participants: " + ex.getMessage());
                model.nextButtonDisabledProperty().set(true);
            }
        }));
        fetchTask.setOnFailed(e -> Platform.runLater(() -> {
            Throwable ex = fetchTask.getException();
            String msg = (ex == null || ex.getMessage() == null) ? "unknown error" : ex.getMessage();
            statusLabel.setText("Failed to load participants: " + msg);
            model.nextButtonDisabledProperty().set(true);
        }));
        Thread t = new Thread(fetchTask);
        t.setDaemon(true);
        t.setName("ImportWizard Participant Fetch");
        t.start();
    }

    /**
     * If the provider advertised custom-attribute definitions, offer to
     * register the ones this event file doesn't already define. Runs on the
     * FX thread (called from the fetch success handler) and must complete
     * before {@link #buildMappingGrid} so the new attributes appear in the
     * mapping ComboBoxes.
     *
     * <p>Matching is by name (case-insensitive); definitions that already
     * exist in the event file are skipped, so re-fetching an event doesn't
     * create duplicates or nag the user again. This is a simple list dialog
     * for now — a dedicated wizard page can come later.
     */
    private void promptForCustomAttributes(List<CustomAttribute> advertised) {
        if (advertised == null || advertised.isEmpty()) {
            return;
        }
        // Only the definitions this event file doesn't have yet.
        Set<String> existing = new HashSet<>();
        ParticipantDAO.getInstance().getCustomAttributes().forEach(ca ->
                existing.add(ca.getName().toLowerCase()));
        List<CustomAttribute> fresh = new ArrayList<>();
        for (CustomAttribute ca : advertised) {
            if (!existing.contains(ca.getName().toLowerCase())) {
                fresh.add(ca);
            }
        }
        if (fresh.isEmpty()) {
            return;
        }

        ListView<String> attrList = new ListView<>();
        attrList.setPrefHeight(180);
        fresh.forEach(ca -> attrList.getItems().add(
                ca.getName() + "  (" + ca.getAttributeType() + ")"));
        attrList.getSelectionModel().selectFirst();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Custom Attributes");
        dialog.setHeaderText("The event defines custom participant attributes. "
                + "Set them up to map and import their values:");
        dialog.getDialogPane().setContent(attrList);
        ButtonType setup = new ButtonType("Set Up", ButtonBar.ButtonData.OK_DONE);
        ButtonType skip = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(setup, skip);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == setup) {
            fresh.forEach(ca -> ParticipantDAO.getInstance().saveCustomAttribute(ca));
            statusLabel.setText("Added " + fresh.size() + " custom attribute definition(s)");
        }
    }

    /**
     * Build the CSV-column -> participant-attribute chooser grid from the
     * columns of the current source (local CSV file or remote export), and
     * populate the model's attribute map with the initial guesses. The Next
     * button stays disabled when there are no columns to map, so the wizard
     * can't proceed to the import step with an empty map.
     */
    private void buildMappingGrid(ImportWizardData model, List<String> csvColumns) {
        class AttributeMap {
            public SimpleStringProperty key = new SimpleStringProperty();
            public SimpleStringProperty value= new SimpleStringProperty();
            Integer customKey = -1;
            private AttributeMap(String k, String v) {
                key.setValue(k);
                value.setValue(v);            
            }
            private AttributeMap(Integer ck, String v) {
                key.setValue(v);
                value.setValue(v);  
                customKey = ck;
            }
            @Override
            public String toString(){
                return value.getValueSafe();
            }
            
        }

        
        ObservableList<AttributeMap> attList = FXCollections.observableArrayList();
        attList.add(new AttributeMap("Ignore","Ignore"));
        Participant.getAvailableAttributes().entrySet().stream().forEach((entry) -> {
            attList.add(new AttributeMap(entry.getKey(),entry.getValue()));
        });
        ParticipantDAO.getInstance().getCustomAttributes().forEach(ca -> {
            attList.add(new AttributeMap(ca.getID(),ca.getName()));
        });
        if (model.getWaveAssignByAttribute()) {
            if (RaceDAO.getInstance().listRaces().size() > 1)
                attList.add(new AttributeMap("RACE","Race"));
            if (RaceDAO.getInstance().listWaves().size() > RaceDAO.getInstance().listRaces().size() ) 
                attList.add(new AttributeMap("WAVE","Wave"));
        }
            
            
            
        ArrayList<ComboBox> comboBoxes = new ArrayList<>();
        // display the colum -> attribute chooser maps
        mapGridPane.setPadding(new Insets(10,10,10,10));
        mapGridPane.setHgap(20);
        mapGridPane.setVgap(2);
        for (int i = 0; i < csvColumns.size(); i++) {
            final String csvAttr = csvColumns.get(i); 
            final ComboBox<AttributeMap> comboBox = new ComboBox(); 
            mapGridPane.add(new Label(csvColumns.get(i)),0,i+1);
            comboBoxes.add(i,comboBox);
            mapGridPane.add(comboBoxes.get(i),1,i+1);
            comboBoxes.get(i).setItems(attList);
            comboBoxes.get(i).getSelectionModel().selectFirst(); 
            comboBoxes.get(i).setOnAction((event) -> {
                //TODO: If the new selected value is "Ignore" we should remove the map entry
                if (comboBox.getSelectionModel().getSelectedItem().customKey >= 0) 
                    model.mapAttrib(csvAttr,comboBox.getSelectionModel().getSelectedItem().customKey.toString());
                else model.mapAttrib(csvAttr, comboBox.getSelectionModel().getSelectedItem().key.getValue());
            });
            for(AttributeMap entry: attList) {
                //System.out.println("Does " + csvColumns.get(i).toLowerCase() + " contain " + entry.key.toString().toLowerCase());
                if (csvColumns.get(i).toLowerCase().contains(entry.key.getValue().toLowerCase()) || 
                        entry.key.getValue().toLowerCase().contains(csvColumns.get(i).toLowerCase())) {
                    comboBoxes.get(i).setValue(entry);
                    if (entry.customKey >= 0) model.mapAttrib(csvAttr, entry.customKey.toString());
                    else model.mapAttrib(csvAttr,entry.key.getValue());
                    //System.out.println("Import: " + csvColumns.get(i).toLowerCase() + " matches " + entry.key.getValue().toLowerCase() );
                }
            }
        }
        
        // If nothing could be mapped, don't let the user proceed to the import
        // step with an empty attribute map.
        if (csvColumns.isEmpty()) {
            model.nextButtonDisabledProperty().set(true);
        } else {
            model.nextButtonDisabledProperty().set(false);
        }
    }
    
    
    
}
