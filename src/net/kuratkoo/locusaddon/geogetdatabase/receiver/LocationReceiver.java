package net.kuratkoo.locusaddon.geogetdatabase.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.PeriodicUpdate;
import menion.android.locus.addon.publiclib.PeriodicUpdate.UpdateContainer;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.utils.RequiredVersionMissingException;
import net.kuratkoo.locusaddon.geogetdatabase.util.Geoget;

/**
 * LocationReceiver
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class LocationReceiver extends BroadcastReceiver {

    private static final String TAG = "LocusAddonGeogetDatabase|LocationReceiver";
    private Context context;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        this.context = context;

        PeriodicUpdate pu = PeriodicUpdate.getInstance();
        pu.setLocNotificationLimit(10.0);
        pu.onReceive(context, intent, new PeriodicUpdate.OnUpdate() {

            public void onUpdate(UpdateContainer update) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("livemap", false)) {
                    if ((update.newMapCenter || update.newZoomLevel) && update.mapVisible) {
                        new MapLoadAsyncTask().execute(update);
                    }
                }
            }

            public void onIncorrectData() {
            }
        });
    }

    private class MapLoadAsyncTask extends AsyncTask<UpdateContainer, Integer, Exception> {

        private PointsData pd;
        private SQLiteDatabase db;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            db = SQLiteDatabase.openDatabase(PreferenceManager.getDefaultSharedPreferences(context).getString("db", ""), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        }

        @Override
        protected Exception doInBackground(UpdateContainer... updateSet) {
            try {
                UpdateContainer update = updateSet[0];
                pd = new PointsData("Livemap data");

                String[] cond = new String[]{
                    String.valueOf(update.mapBottomRight.getLatitude()),
                    String.valueOf(update.mapTopLeft.getLatitude()),
                    String.valueOf(update.mapTopLeft.getLongitude()),
                    String.valueOf(update.mapBottomRight.getLongitude())
                };

                String sql = "SELECT x, y, id, author, name, cachetype, cachestatus, dtfound FROM geocache WHERE (cachestatus = 0";

                // Disable geocaches
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("disable", false)) {
                    sql = sql + " OR cachestatus = 1";
                }

                // Archived geocaches
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("archive", false)) {
                    sql = sql + " OR cachestatus = 2";
                }

                sql = sql + ") ";

                if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("found", false)) {
                    sql = sql + " AND dtfound = 0";
                }

                if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("own", false)) {
                    sql = sql + " AND author != \"" + PreferenceManager.getDefaultSharedPreferences(context).getString("nick", "") + "\"";
                }
                sql += " AND CAST(x AS REAL) > ? AND CAST(x AS REAL) < ? AND CAST(y AS REAL) > ? AND CAST(y AS REAL) < ?";

                Cursor c = db.rawQuery(sql, cond);

                while (c.moveToNext()) {
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("x")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("y")));
                    Point p = new Point(c.getString(c.getColumnIndex("name")), loc);

                    PointGeocachingData gcData = new PointGeocachingData();
                    gcData.cacheID = c.getString(c.getColumnIndex("id"));
                    gcData.name = c.getString(c.getColumnIndex("name"));
                    gcData.owner = c.getString(c.getColumnIndex("author"));
                    gcData.type = Geoget.convertCacheType(c.getString(c.getColumnIndex("cachetype")));
                    gcData.available = Geoget.isAvailable(c.getInt(c.getColumnIndex("cachestatus")));
                    gcData.archived = Geoget.isArchived(c.getInt(c.getColumnIndex("cachestatus")));
                    gcData.found = Geoget.isFound(c.getInt(c.getColumnIndex("dtfound")));

                    p.setGeocachingData(gcData);
                    p.setExtraOnDisplay("net.kuratkoo.locusaddon.geogetdatabase", "net.kuratkoo.locusaddon.geogetdatabase.DetailActivity", "cacheId", gcData.cacheID);
                    pd.addPoint(p);
                }
                c.close();

            } catch (Exception ex) {
                return ex;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception exception) {
            super.onPostExecute(exception);

            if (exception != null) {
                Log.w(TAG, exception);
            }

            try {
                db.close();

                File externalDir = Environment.getExternalStorageDirectory();
                String filePath = externalDir.getAbsolutePath();
                if (!filePath.endsWith("/")) {
                    filePath += "/";
                }
                filePath += "/Android/data/net.kuratkoo.locusaddon.geogetdatabase/livemap.locus";
                
                ArrayList<PointsData> data = new ArrayList<PointsData>();
                data.add(pd);
                DisplayData.sendDataFileSilent(context, data, filePath, true);
            } catch (RequiredVersionMissingException ex) {
            }
        }
    }
}
