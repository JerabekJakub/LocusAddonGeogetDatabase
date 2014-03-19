package net.kuratkoo.locusaddon.geogetdatabase.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * PreferencesBackupAgent
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class PreferencesBackupAgent extends BackupAgentHelper {

    @SuppressWarnings("unused")
	private static final String TAG = "LocusAddonGeogetDatabase|PreferencesBackupAgent";
    private static final String PREFS_BACKUP_KEY = "PREFERENCES";

    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, getPackageName() + "_preferences");
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}
