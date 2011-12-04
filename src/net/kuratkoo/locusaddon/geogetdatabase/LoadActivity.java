package net.kuratkoo.locusaddon.geogetdatabase;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.LocusIntents;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataWaypoint;

public class LoadActivity extends Activity {

    private static final String TAG = "LocusAddonGeogetDatabase|LoadActivity";
    private ProgressDialog progress;
    private ArrayList<PointsData> data;
    private File externalDir;
    private Point point;

    private class LoadAsyncTask extends AsyncTask<Point, Integer, Exception> {

        @Override
        protected void onPreExecute() {
            progress.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progress.setMessage(getString(R.string.loading) + " " + values[0] + " " + getString(R.string.geocaches));
        }

        @Override
        protected void onPostExecute(Exception ex) {
            progress.dismiss();

            if (ex != null) {
                Toast.makeText(LoadActivity.this, getString(R.string.unable_to_load_geocaches) + " (" + ex.getLocalizedMessage() + ")", Toast.LENGTH_LONG).show();
                LoadActivity.this.finish();
                return;
            }

            String filePath = externalDir.getAbsolutePath();
            if (!filePath.endsWith("/")) {
                filePath += "/";
            }
            filePath += "/Android/data/net.kuratkoo.locusaddon.geogetdatabase/data.locus";

            try {
                DisplayData.sendDataFile(LoadActivity.this,
                        data,
                        filePath,
                        PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("import", true));
            } catch (OutOfMemoryError e) {
                AlertDialog.Builder ad = new AlertDialog.Builder(LoadActivity.this);
                ad.setIcon(android.R.drawable.ic_dialog_alert);
                ad.setTitle(R.string.error);
                ad.setMessage(R.string.out_of_memory);
                ad.setPositiveButton(android.R.string.ok, new OnClickListener() {

                    public void onClick(DialogInterface di, int arg1) {
                        di.dismiss();
                    }
                });
                ad.show();
            }
        }

        protected Exception doInBackground(Point... pointSet) {
            try {
                Point pp = pointSet[0];
                Location curr = pp.getLocation();
                PointsData pd = new PointsData("Geoget data");
                Float radius = Float.valueOf(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("radius", "1")) / 70;

                SQLiteDatabase database = SQLiteDatabase.openDatabase(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("db", ""), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                String[] cond = new String[]{
                    String.valueOf(curr.getLatitude() - radius),
                    String.valueOf(curr.getLatitude() + radius),
                    String.valueOf(curr.getLongitude() - radius),
                    String.valueOf(curr.getLongitude() + radius)
                };
                Cursor c;
                String sql = "SELECT x, y, id, author FROM geocache WHERE ";
                if (PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("disable", false)) {
                    sql = sql + "cachestatus != 2";
                } else {
                    sql = sql + "cachestatus = 0";
                }

                if (!PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("found", false)) {
                    sql = sql + " AND dtfound = 0";
                }

                if (!PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("own", false)) {
                    sql = sql + " AND author != \"" + PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("nick", "") + "\"";
                }
                sql += " AND x > ? AND x < ? AND y > ? AND y < ?";

                c = database.rawQuery(sql, cond);

                /** Load GC codes **/
                double max = 0;
                List<Pair> gcCodes = new ArrayList<Pair>();
                while (c.moveToNext()) {
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("x")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("y")));
                    if (loc.distanceTo(curr) < Float.valueOf(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("radius", "1")) * 1000) {
                        if (max < loc.getLongitude() - curr.getLongitude()) {
                            max = loc.getLongitude() - curr.getLongitude();
                        }
                        gcCodes.add(new Pair(loc.distanceTo(curr), c.getString(c.getColumnIndex("id"))));
                    }
                }
                c.close();

                int count = 0;
                int limit = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("limit", "0"));

                if (limit > 0) {
                    Collections.sort(gcCodes, new Comparator<Pair>() {

                        public int compare(Pair p1, Pair p2) {
                            return p1.distance.compareTo(p2.distance);
                        }
                    });
                }

                for (Pair pair : gcCodes) {
                    if (limit > 0) {
                        if (count >= limit) {
                            break;
                        }
                    }
                    String gcCode = pair.gcCode;
                    publishProgress(++count);
                    c = database.rawQuery("SELECT x, y, name, id, author, difficulty, terrain, country, state, comment, cachesize, cachetype, cachestatus, dtfound, dthidden, dtupdate FROM geocache WHERE id = ?", new String[]{gcCode});
                    c.moveToNext();
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("x")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("y")));
                    Point p = new Point(c.getString(c.getColumnIndex("name")), loc);

                    PointGeocachingData gcData = new PointGeocachingData();
                    gcData.cacheID = c.getString(c.getColumnIndex("id"));
                    gcData.name = c.getString(c.getColumnIndex("name"));
                    gcData.owner = c.getString(c.getColumnIndex("author"));
                    gcData.placedBy = c.getString(c.getColumnIndex("author"));
                    gcData.difficulty = c.getFloat(c.getColumnIndex("difficulty"));
                    gcData.terrain = c.getFloat(c.getColumnIndex("terrain"));
                    gcData.country = c.getString(c.getColumnIndex("country"));
                    gcData.state = c.getString(c.getColumnIndex("state"));
                    gcData.notes = c.getString(c.getColumnIndex("comment"));
                    gcData.container = GeogetUtils.convertCacheSize(c.getString(c.getColumnIndex("cachesize")));
                    gcData.type = GeogetUtils.convertCacheType(c.getString(c.getColumnIndex("cachetype")));
                    gcData.available = GeogetUtils.isAvailable(c.getInt(c.getColumnIndex("cachestatus")));
                    gcData.archived = GeogetUtils.isArchived(c.getInt(c.getColumnIndex("cachestatus")));
                    gcData.found = GeogetUtils.isFound(c.getInt(c.getColumnIndex("dtfound")));
                    gcData.computed = false;

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    Date date = new Date();
                    gcData.exported = dateFormat.format(date);

                    String lastUpdated = c.getString(c.getColumnIndex("dtupdate"));
                    gcData.lastUpdated = lastUpdated.substring(0, 4) + "-" + lastUpdated.substring(4, 6) + "-" + lastUpdated.substring(6, 8) + "T";

                    String hidden = c.getString(c.getColumnIndex("dthidden"));
                    gcData.hidden = hidden.substring(0, 4) + "-" + hidden.substring(4, 6) + "-" + hidden.substring(6, 8) + "T";


                    c.close();

                    /** Add waypoints to Geocache **/
                    Cursor wp = database.rawQuery("SELECT x, y, name, wpttype, cmt, prefixid FROM waypoint WHERE id = ?", new String[]{gcData.cacheID});
                    ArrayList<PointGeocachingDataWaypoint> pgdws = new ArrayList<PointGeocachingDataWaypoint>();

                    while (wp.moveToNext()) {
                        PointGeocachingDataWaypoint pgdw = new PointGeocachingDataWaypoint();
                        pgdw.lat = wp.getDouble(wp.getColumnIndex("x"));
                        pgdw.lon = wp.getDouble(wp.getColumnIndex("y"));
                        pgdw.name = wp.getString(wp.getColumnIndex("name"));
                        pgdw.type = GeogetUtils.convertWaypointType(wp.getString(wp.getColumnIndex("wpttype")));
                        pgdw.description = wp.getString(wp.getColumnIndex("cmt"));
                        pgdw.code = wp.getString(wp.getColumnIndex("prefixid"));
                        pgdws.add(pgdw);
                        if (!(pgdw.lat == 0 && pgdw.lon == 0) && GeogetUtils.convertWaypointType(wp.getString(wp.getColumnIndex("wpttype"))).equals(PointGeocachingData.CACHE_WAYPOINT_TYPE_FINAL)) {
                            gcData.computed = true;
                        }
                    }
                    wp.close();
                    gcData.waypoints = pgdws;

                    p.setGeocachingData(gcData);
                    p.setExtraOnDisplay("net.kuratkoo.locusaddon.geogetdatabase", "net.kuratkoo.locusaddon.geogetdatabase.DetailActivity", "cacheId", gcData.cacheID);
                    pd.addPoint(p);

                }

                database.close();

                data = new ArrayList<PointsData>();
                data.add(pd);

                return null;
            } catch (Exception e) {
                return e;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.loading_dots));
        progress.setIcon(android.R.drawable.ic_dialog_info);
        progress.setTitle(getString(R.string.loading));

        externalDir = Environment.getExternalStorageDirectory();
        if (externalDir == null || !(externalDir.exists())) {
            Toast.makeText(LoadActivity.this, R.string.no_external_storage, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        File fd = new File(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("db", ""));
        if (!GeogetUtils.isGeogetDatabase(fd)) {
            Toast.makeText(LoadActivity.this, R.string.no_db_file, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent fromIntent = getIntent();
        if (LocusIntents.isIntentOnPointAction(fromIntent)) {
            point = LocusIntents.handleIntentOnPointAction(fromIntent);
        } else if (LocusIntents.isIntentMainFunction(fromIntent)) {
            LocusIntents.handleIntentMainFunction(fromIntent, new LocusIntents.OnIntentMainFunction() {

                public void onLocationReceived(boolean gpsEnabled, Location locGps, Location locMapCenter) {
                    point = new Point("Map center", locMapCenter);
                }

                public void onFailed() {
                }
            });
        }
        new LoadAsyncTask().execute(point);
    }

    private class Pair {

        private String gcCode;
        private Float distance;

        public Pair(Float f, String s) {
            this.distance = f;
            this.gcCode = s;
        }
    }
}
