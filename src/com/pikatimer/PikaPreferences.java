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
package com.pikatimer;

import java.io.File;
import java.util.prefs.Preferences;

/**
 *
 * @author John
 */
public class PikaPreferences {
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    
    private File recentFile;
    Preferences prefs = Preferences.userRoot().node("PikaTimer");
    
    Boolean dbLoaded = false;
    
    private static class SingletonHolder { 
            private static final PikaPreferences INSTANCE = new PikaPreferences();
    }

    public static PikaPreferences getInstance() {
            return SingletonHolder.INSTANCE;
    }
    
    public void setRecentFile(File f) {
        recentFile = f;
    }
    
    public File getRecentFile() {
        return recentFile;
    }
    
    public Preferences getGlobalPreferences(){
        return prefs;
    }
    
    public File getCWD(){
        File cwd = new File(prefs.get("PikaEventHome", System.getProperty("user.home")));
        if (!cwd.exists() ) {
            // we have a problem
            cwd= new File(System.getProperty("user.home"));
        } else if (cwd.exists() && cwd.isFile()){
            cwd = new File(cwd.getParent());
           
        }
        return cwd;
    }
    
    public void setDBLoaded(){
        dbLoaded = true;
    }

    public Boolean getDBLoaded(){
        return dbLoaded;
    }

    /**
     * Save the visible-width state of a set of JavaFX TableColumns to the
     * global preferences store so we can restore them on the next launch.
     *
     * @param tableName  logical name of the table (e.g. "participant")
     * @param columns    the columns to persist, in the order they currently
     *                   appear in the TableView
     */
    public void saveColumnWidths(String tableName, javafx.collections.ObservableList<? extends javafx.scene.control.TableColumn<?, ?>> columns) {
        if (tableName == null || columns == null) return;
        int i = 0;
        for (javafx.scene.control.TableColumn<?, ?> col : columns) {
            // Only persist columns that the user can actually see; hidden ones
            // would record 0 width and undo themselves on the next launch.
            if (col.isVisible()) {
                prefs.putDouble(tableName + ".col." + i + ".width", col.getWidth());
            }
            i++;
        }
    }

    /**
     * Read a previously-saved width for the column at the given index, or
     * {@code -1} if no value was stored.
     */
    public double getSavedColumnWidth(String tableName, int index) {
        return prefs.getDouble(tableName + ".col." + index + ".width", -1);
    }

}
