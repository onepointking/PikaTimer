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

import com.pikatimer.PikaPreferences;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import java.io.File;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javax.annotation.PostConstruct;


@FXMLController("FXMLImportWizardView1.fxml")
public class ImportWizardView1Controller {
    
    @FXMLViewFlowContext private ViewFlowContext context;
    
    @FXML private Label fileStatusLabel;
    @FXML private TextField fileTextField;
    @FXML private Button fileChooserButton;
    @FXML private RadioButton fileSourceRadioButton;
    @FXML private RadioButton webSourceRadioButton;
    @FXML private VBox fileSourceVBox;
    @FXML private VBox webSourceVBox;
    @FXML private TextField urlTextField;
    @FXML private TextField usernameTextField;
    @FXML private PasswordField passwordTextField;
    @FXML private Label urlStatusLabel;
    @FXML private ComboBox<RaceRegProvider> providerComboBox;
    @FXML private Button fetchEventsButton;
    @FXML private ComboBox<RemoteEvent> remoteEventComboBox;
    @FXML private Label fetchEventsStatusLabel;
    @FXML private CheckBox clearExistingCheckBox; 
    @FXML private CheckBox cleanupCityCheckBox; 
    @FXML private CheckBox cleanupNamesCheckBox; 
    @FXML private VBox bibAssignmentVBox; 
    @FXML private CheckBox waveByBibCheckBox; 
    @FXML private CheckBox waveHardCodeCheckBox;
    @FXML private CheckBox waveByAttributeCheckBox; 
    @FXML private ComboBox<Wave> waveComboBox;
    @FXML private ComboBox<String> duplicateHandlingComboBox;
    @FXML private HBox duplicateHandlingHBox;
    
    
    ImportWizardData model;
    
    @PostConstruct
    public void init() throws FlowException {
        System.out.println("ImportWizardView1Controller.initialize()");
        
        // TODO: 
        cleanupCityCheckBox.disableProperty().set(true);
        cleanupCityCheckBox.visibleProperty().set(false);
        cleanupCityCheckBox.managedProperty().set(false);
        cleanupNamesCheckBox.disableProperty().set(true);
        cleanupNamesCheckBox.visibleProperty().set(false);
        cleanupNamesCheckBox.managedProperty().set(false);

        model = context.getRegisteredObject(ImportWizardData.class);

        // --- Provider selection (web import flow) ---
        // The ComboBox lists every registered RaceRegProvider; the user can
        // pick one manually, or it auto-selects when the entered URL matches
        // a provider's matches() (see RaceRegProviders.match). The selection
        // is pushed into the model so fetchEvents() can delegate to it.
        providerComboBox.getItems().setAll(RaceRegProviders.all());
        providerComboBox.getSelectionModel().selectFirst();
        model.setSelectedProvider(providerComboBox.getSelectionModel().getSelectedItem());
        providerComboBox.getSelectionModel().selectedItemProperty().addListener((ov, oldP, newP) -> {
            model.setSelectedProvider(newP);
            // A provider change invalidates any previously fetched event list
            // (the old events came from a different schema/endpoint), and any
            // previously fetched participants export.
            model.getRemoteEvents().clear();
            model.setSelectedRemoteEvent(null);
            model.setResultsSet(null);
            remoteEventComboBox.getSelectionModel().clearSelection();
            validateUrl();
        });

        // Source selection: either/or between file and web import.
        // The web path is a stub for now (race reg API); only the file path
        // actually feeds the downstream CSV pipeline.
        fileSourceRadioButton.selectedProperty().addListener((ov, wasFile, isFile) -> {
            boolean file = isFile;
            fileSourceVBox.setVisible(file);
            fileSourceVBox.setManaged(file);
            webSourceVBox.setVisible(!file);
            webSourceVBox.setManaged(!file);
            // Re-validate the Next button against the now-active source.
            if (file) {
                validateFile();
            } else {
                validateUrl();
            }
        });

        urlTextField.textProperty().addListener((ob, oldT, newT) -> {
            // Auto-detect the provider from the URL. We only override the
            // ComboBox if a provider actually claims this URL; if none match,
            // we leave the user's manual selection alone. This keeps the
            // "Both" behaviour: auto-select when possible, overridable always.
            RaceRegProvider detected = RaceRegProviders.match(newT);
            if (detected != null
                    && providerComboBox.getSelectionModel().getSelectedItem() != detected) {
                providerComboBox.getSelectionModel().select(detected);
                // select() fires the listener above, which clears events and
                // re-validates, so we can return here.
                return;
            }
            validateUrl();
        });
        usernameTextField.textProperty().addListener((ob, oldT, newT) -> {
            model.setWebUsername(newT);
            validateUrl();
        });
        passwordTextField.textProperty().addListener((ob, oldT, newT) -> {
            model.setWebPassword(newT);
            validateUrl();
        });

        // --- Remote event picker (web import flow) ---
        // The provider exposes a /events endpoint; the user enters the base URL
        // + creds, hits "Fetch Events", and then picks an event from the list.
        // Next stays disabled until an event is selected.
        remoteEventComboBox.setItems(model.getRemoteEvents());
        remoteEventComboBox.getSelectionModel().selectedItemProperty().addListener((ov, oldE, newE) -> {
            model.setSelectedRemoteEvent(newE);
            // A different event has a different participants export; drop any
            // previously fetched one so View2/View3 can't import stale data.
            model.setResultsSet(null);
            validateUrl();
        });
        fetchEventsButton.setOnAction(this::fetchEvents);
        fetchEventsStatusLabel.textProperty().bind(model.fetchEventsStatusProperty());
        // Disable the fetch button while a fetch is in flight or when the URL
        // is not yet a valid http(s) endpoint.
        fetchEventsButton.disableProperty().bind(
                model.fetchingEventsProperty()
                        .or(urlTextField.textProperty().isEmpty())
        );
        remoteEventComboBox.disableProperty().bind(model.fetchingEventsProperty());
        //fileNameLabel.textProperty().bind(model.fileNameProperty());
        
        fileChooserButton.setOnAction(this::chooseFile);
        
        model.clearExistingProperty().bind(clearExistingCheckBox.selectedProperty());
        
        if (ParticipantDAO.getInstance().listParticipants().isEmpty()) {
            clearExistingCheckBox.visibleProperty().set(false);
            clearExistingCheckBox.managedProperty().set(false);
            
            duplicateHandlingHBox.visibleProperty().set(false);
            duplicateHandlingHBox.managedProperty().set(false);
            clearExistingCheckBox.selectedProperty().set(true);
            
        } else {
            duplicateHandlingHBox.disableProperty().bind(clearExistingCheckBox.selectedProperty());
            duplicateHandlingComboBox.setItems(FXCollections.observableArrayList("Ignore","Merge","Import"));
            duplicateHandlingComboBox.getSelectionModel().selectFirst();
            model.duplicateHandlingProperty().bind(duplicateHandlingComboBox.getSelectionModel().selectedItemProperty());
        }
        
        // Wave assignment options:
        // if only one race, hide it all and just do a straight assignment
        ObservableList<Wave> waves = RaceDAO.getInstance().listWaves(); 
        if (waves.size() == 1) {
            bibAssignmentVBox.setVisible(false);
            bibAssignmentVBox.setManaged(false);
            model.waveAssignByBibProperty().setValue(false);
            model.waveAssignByAttributeProperty().setValue(false); 
            model.setAssignedWave(waves.get(0));
        } else {
            model.waveAssignByAttributeProperty().bind(waveByAttributeCheckBox.selectedProperty());
            waveByAttributeCheckBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                if (new_val) { // checked
                    waveHardCodeCheckBox.setSelected(false); 
                    waveByBibCheckBox.setSelected(false);
                }
            });
            model.waveAssignByBibProperty().bind(waveByBibCheckBox.selectedProperty());
            waveByBibCheckBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                if (new_val) { // checked
                    waveHardCodeCheckBox.setSelected(false); 
                    waveByAttributeCheckBox.setSelected(false);
                }
            });
            waveHardCodeCheckBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                if (new_val) { // checked
                    waveByBibCheckBox.setSelected(false);
                    waveByAttributeCheckBox.setSelected(false);
                }
            });
            waveComboBox.getSelectionModel().selectedItemProperty().addListener((ov, old_val, new_val) -> {
                model.setAssignedWave(new_val);
            });
            waveComboBox.getItems().addAll(waves); 
            waveComboBox.setValue(waves.get(0));
            waveByBibCheckBox.setSelected(true);

            
            
        }
        // TODO:
        // Cleanup Names
        // Cleanup City / State (by zip?)

        fileTextField.textProperty().addListener((ob, oldT, newT) -> validateFile());

        // Start from the file source; Next is disabled until a valid source is set.
        model.nextButtonDisabledProperty().set(true);
    }

    /**
     * Validate the file-source path and enable/disable the Next button accordingly.
     * Only active when the file radio is selected.
     */
    private void validateFile() {
        if (!fileSourceRadioButton.isSelected()) return;
        File file = new File(fileTextField.getText());
        if (file.exists() && file.isFile() && file.canRead()) {
            System.out.println("  The file is good...");
            fileStatusLabel.setText("");
            model.nextButtonDisabledProperty().set(false);
            model.setFileName(file.getAbsolutePath());
        } else {
            System.out.println("  Unable to use this file");
            if (!file.exists()) fileStatusLabel.setText("File does not exist");
            else if (!file.isFile()) fileStatusLabel.setText("The path entered is not a regular file");
            else if (!file.canRead()) fileStatusLabel.setText("Unable to read the file");
            model.setFileName(fileTextField.getText());
            model.nextButtonDisabledProperty().set(true);
        }
    }

    /**
     * Validate the web-source URL and enable/disable the Next button accordingly.
     * Only active when the web radio is selected. The actual fetch is a TODO stub
     * pending the race reg API integration; for now we just require a non-empty
     * http(s) URL so the wizard can be navigated.
     */
    private void validateUrl() {
        if (!webSourceRadioButton.isSelected()) return;
        String url = urlTextField.getText().trim();
        if (url.isEmpty()) {
            urlStatusLabel.setText("Enter a URL");
            model.nextButtonDisabledProperty().set(true);
            return;
        }
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
            urlStatusLabel.setText("URL must start with http:// or https://");
            model.nextButtonDisabledProperty().set(true);
            return;
        }
        urlStatusLabel.setText("");
        model.setSourceURL(url);
        model.setWebUsername(usernameTextField.getText().trim());
        model.setWebPassword(passwordTextField.getText()); // password stored as-is (no trim)
        // Next is only enabled once an event has been picked from the fetched
        // list (and no fetch is in flight). If the user edits the URL/creds
        // after a previous selection, we clear the stale selection so they
        // have to re-fetch/re-pick against the new endpoint.
        if (model.isFetchingEvents()) {
            model.nextButtonDisabledProperty().set(true);
            return;
        }
        RemoteEvent selected = model.getSelectedRemoteEvent();
        if (selected == null) {
            model.nextButtonDisabledProperty().set(true);
        } else {
            // TODO: branch View2/View3 to fetch from the race reg API instead of
            // reading a local CSV file. For now the file pipeline is left
            // untouched, but we let the user proceed once an event is chosen.
            model.nextButtonDisabledProperty().set(false);
        }
    }

    /**
     * Kick off a background fetch of the selected provider's event list.
     * Delegates the actual HTTP + schema parsing to the
     * {@link RaceRegProvider} currently held in
     * {@link ImportWizardData#selectedProviderProperty()}, so this controller
     * stays provider-agnostic. On success the results populate
     * {@link ImportWizardData#getRemoteEvents()} (and thus the View1
     * ComboBox); on failure the status label shows the error.
     */
    @FXML
    protected void fetchEvents(ActionEvent fxevent) {
        RaceRegProvider provider = model.getSelectedProvider();
        if (provider == null) {
            model.setFetchEventsStatus("Select a provider first");
            return;
        }
        String base = urlTextField.getText().trim();
        String user = usernameTextField.getText().trim();
        String pass = passwordTextField.getText();

        model.setFetchingEvents(true);
        model.setFetchEventsStatus("Fetching events from " + provider.displayName() + "...");
        model.getRemoteEvents().clear();
        model.setSelectedRemoteEvent(null);
        model.setResultsSet(null);
        remoteEventComboBox.getSelectionModel().clearSelection();
        validateUrl();

        Task<List<RemoteEvent>> fetchTask = new Task<List<RemoteEvent>>() {
            @Override
            protected List<RemoteEvent> call() throws Exception {
                return provider.fetchEvents(base, user, pass);
            }
        };
        fetchTask.setOnSucceeded(e -> Platform.runLater(() -> {
            List<RemoteEvent> events = fetchTask.getValue();
            model.getRemoteEvents().setAll(events);
            model.setFetchingEvents(false);
            if (events.isEmpty()) {
                model.setFetchEventsStatus("No events returned by " + provider.displayName());
            } else {
                model.setFetchEventsStatus(events.size() + " event(s) available from " + provider.displayName());
                remoteEventComboBox.getSelectionModel().selectFirst();
            }
            validateUrl();
        }));
        fetchTask.setOnFailed(e -> Platform.runLater(() -> {
            model.setFetchingEvents(false);
            Throwable ex = fetchTask.getException();
            String msg = ex == null ? "Fetch failed" : ex.getMessage();
            model.setFetchEventsStatus("Fetch failed: " + msg);
            validateUrl();
        }));
        Thread t = new Thread(fetchTask);
        t.setDaemon(true);
        t.setName("ImportWizard Event Fetch");
        t.start();
    }

    @FXML
    protected void chooseFile(ActionEvent fxevent){
        
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open CSV File");
        
        if (model.getFileName().equals("")) {
            fileChooser.setInitialDirectory(PikaPreferences.getInstance().getCWD());
        } else {
            fileChooser.setInitialFileName(model.getFileName()); 
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV/TXT Files", "*.csv", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        File file = fileChooser.showOpenDialog(fileChooserButton.getScene().getWindow());
        if (file != null && file.exists() && file.isFile() && file.canRead()) {
           // model.setFileName(file.getAbsolutePath());
            fileTextField.setText(file.getAbsolutePath());
            //model.nextButtonDisabledProperty().set(false);
        }        
    }
}
