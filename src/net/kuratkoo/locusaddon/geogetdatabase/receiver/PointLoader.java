package net.kuratkoo.locusaddon.geogetdatabase.receiver;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.PeriodicUpdate;
import menion.android.locus.addon.publiclib.PeriodicUpdate.UpdateContainer;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataWaypoint;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.utils.RequiredVersionMissingException;
import net.kuratkoo.locusaddon.geogetdatabase.util.Geoget;

/**
 * PointLoader
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class PointLoader {

    private static final String TAG = "LocusAddonGeogetDatabase|PointLoader";
    private static PointLoader mInstance;
    private Context context;
    private Intent intent;
    private MapLoadAsyncTask mapLoadAsyncTask;

    public static PointLoader getInstance() {
        if (mInstance == null) {
            mInstance = new PointLoader();
        }
        return mInstance;
    }

    private PointLoader() {
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public void run() {
        PeriodicUpdate pu = PeriodicUpdate.getInstance();
        pu.setLocNotificationLimit(50.0);
        pu.onReceive(context, intent, new PeriodicUpdate.OnUpdate() {

            public void onUpdate(UpdateContainer update) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("livemap", false)
                        && !PreferenceManager.getDefaultSharedPreferences(context).getString("db", "").equals("")) {
                    if ((update.newMapCenter || update.newZoomLevel) && update.mapVisible) {
                        if (mapLoadAsyncTask instanceof AsyncTask) {
                        }
                        if (mapLoadAsyncTask == null || mapLoadAsyncTask.getStatus() == AsyncTask.Status.FINISHED) {
                            mapLoadAsyncTask = new MapLoadAsyncTask();
                            mapLoadAsyncTask.execute(update);
                        } else {
                            mapLoadAsyncTask.cancel(true);
                            mapLoadAsyncTask = new MapLoadAsyncTask();
                            mapLoadAsyncTask.execute(update);
                        }
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
                if (this.isCancelled()) {
                    return null;
                }

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

                List<String> geocacheTypes = Geoget.geocacheTypesFromFilter(PreferenceManager.getDefaultSharedPreferences(context));
                boolean first = true;
                String sqlType = "";
                for (String geocacheType : geocacheTypes) {
                    if (first) {
                        sqlType += "cachetype = \"" + geocacheType + "\"";
                        first = false;
                    } else {
                        sqlType += " OR cachetype = \"" + geocacheType + "\"";
                    }
                }
                if (!sqlType.equals("")) {
                    sql += " AND (" + sqlType + ")";
                }

                sql += " AND CAST(x AS REAL) > ? AND CAST(x AS REAL) < ? AND CAST(y AS REAL) > ? AND CAST(y AS REAL) < ?";

                Cursor c = db.rawQuery(sql, cond);

                if (this.isCancelled()) {
                    return null;
                }

                while (c.moveToNext()) {
                    if (this.isCancelled()) {
                        return null;
                    }
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

                    /** Add waypoints to Geocache **/
                    Cursor wp = db.rawQuery("SELECT x, y, name, wpttype, cmt, prefixid FROM waypoint WHERE id = ?", new String[]{gcData.cacheID});
                    ArrayList<PointGeocachingDataWaypoint> pgdws = new ArrayList<PointGeocachingDataWaypoint>();

                    while (wp.moveToNext()) {
                        if (this.isCancelled()) {
                            return null;
                        }
                        PointGeocachingDataWaypoint pgdw = new PointGeocachingDataWaypoint();
                        pgdw.lat = wp.getDouble(wp.getColumnIndex("x"));
                        pgdw.lon = wp.getDouble(wp.getColumnIndex("y"));
                        pgdw.name = wp.getString(wp.getColumnIndex("name"));
                        pgdw.type = Geoget.convertWaypointType(wp.getString(wp.getColumnIndex("wpttype")));
                        pgdw.code = wp.getString(wp.getColumnIndex("prefixid"));
                        pgdws.add(pgdw);
                    }
                    wp.close();
                    gcData.waypoints = pgdws;

                    p.setGeocachingData(gcData);
                    p.setExtraOnDisplay("net.kuratkoo.locusaddon.geogetdatabase", "net.kuratkoo.locusaddon.geogetdatabase.DetailActivity", "cacheId", gcData.cacheID);
                    pd.addPoint(p);
                }
                c.close();

                if (this.isCancelled()) {
                    return null;
                }

            } catch (Exception ex) {
                return ex;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception exception) {
            super.onPostExecute(exception);
            Log.d(TAG, "onPostExecute");
            
            db.close();
            if (exception != null) {
                Log.w(TAG, exception);
                Toast.makeText(context, "Error: " + exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }

            try {
                File externalDir = Environment.getExternalStorageDirectory();
                String filePath = externalDir.getAbsolutePath();
                if (!filePath.endsWith("/")) {
                    filePath += "/";
                }
                filePath += "/Android/data/net.kuratkoo.locusaddon.geogetdatabase/livemap.locus";

                ArrayList<PointsData> data = new ArrayList<PointsData>();
                data.add(pd);
                DisplayData.sendDataFileSilent(context, data, filePath, true);
            } catch (RequiredVersionMissingException rvme) {
                Toast.makeText(context, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d(TAG, "onCancelled");
            db.close();
        }
    }
}
