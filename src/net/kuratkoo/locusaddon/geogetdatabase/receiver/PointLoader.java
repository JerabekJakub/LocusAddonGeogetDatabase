package net.kuratkoo.locusaddon.geogetdatabase.receiver;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import locus.api.android.ActionDisplayPoints;
import locus.api.android.PeriodicUpdate;
import locus.api.android.UpdateContainer;
import locus.api.android.objects.PackWaypoints;
import locus.api.android.utils.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Waypoint;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingWaypoint;
import net.kuratkoo.locusaddon.geogetdatabase.R;
import net.kuratkoo.locusaddon.geogetdatabase.util.Geoget;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * This class is responsible for loading caches to Live Map
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 * @author Jakub Jerabek <jerabek.jakub@gmail.com> since 2014-02
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
    	
    	final boolean liveMap = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("livemap", false);
    	final String database = PreferenceManager.getDefaultSharedPreferences(context).getString("db", "");
    	
        File fd;
		try {
			fd = new File(URLDecoder.decode(database, "UTF-8"));
			if (!Geoget.isGeogetDatabase(fd) && liveMap) {
				// database does not exists - switch live map off
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
	            SharedPreferences.Editor editor = sharedPref.edit();
	            editor.putBoolean("livemap", false);
	            editor.commit();
	            Toast.makeText(context, context.getString(R.string.no_db_file_live), Toast.LENGTH_LONG).show();
	            return;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	
        PeriodicUpdate pu = PeriodicUpdate.getInstance();
        pu.setLocNotificationLimit(50.0);        
		pu.onReceive(context, intent, new PeriodicUpdate.OnUpdate() {

            public void onUpdate(UpdateContainer update) {
                if (liveMap && !database.equals("")) {
                    if ((update.isNewMapCenter() || update.isNewZoomLevel()) && update.isMapVisible()) {
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

        private PackWaypoints pd;
        private SQLiteDatabase db;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
				db = SQLiteDatabase.openDatabase(
						URLDecoder.decode(PreferenceManager.getDefaultSharedPreferences(context).getString("db", ""), "UTF-8"),
						null,
						SQLiteDatabase.NO_LOCALIZED_COLLATORS);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
        }

        @Override
        protected Exception doInBackground(UpdateContainer... updateSet) {
            try {
                if (this.isCancelled()) {
                    return null;
                }

                UpdateContainer update = updateSet[0];
                pd = new PackWaypoints("GeoGet live data");

                String[] cond = new String[]{
                    String.valueOf(update.getMapBottomRight().getLatitude()),
                    String.valueOf(update.getMapTopLeft().getLatitude()),
                    String.valueOf(update.getMapTopLeft().getLongitude()),
                    String.valueOf(update.getMapBottomRight().getLongitude()),
                    String.valueOf(update.getMapBottomRight().getLatitude()),
                    String.valueOf(update.getMapTopLeft().getLatitude()),
                    String.valueOf(update.getMapTopLeft().getLongitude()),
                    String.valueOf(update.getMapBottomRight().getLongitude())
                };

                String sql = "SELECT geocache.id, geocache.x, geocache.y, geocache.name, difficulty, terrain, cachesize, cachetype, cachestatus, dtfound, author " +
                		"FROM geocache " +
                		"LEFT JOIN waypoint ON geocache.id = waypoint.id " +
                		"WHERE cachestatus IN (0";

                // Disabled geocaches
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("disable", false)) {
                    sql += ",1";
                }

                // Archived geocaches
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("archive", false)) {
                    sql += ",2";
                }

                sql += ") ";

                if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("found", false)) {
                    sql += " AND dtfound = 0 ";
                }

                if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("own", false)) {
                    sql += " AND author != \"" + PreferenceManager.getDefaultSharedPreferences(context).getString("nick", "") + "\"";
                }

                // Filter cache type
                List<String> geocacheTypes = Geoget.geocacheTypesFromFilter(PreferenceManager.getDefaultSharedPreferences(context));
                if (!geocacheTypes.isEmpty() && geocacheTypes.size() != Geoget.countTypes){
                	sql += " AND cachetype IN (\"" + geocacheTypes.remove(0) + "\"";
                    
                	for (String geocacheType : geocacheTypes) {
                    	sql += ", \"" + geocacheType + "\"";
                    }
                	sql += ") ";
                }

                // Filter cache size
                List<String> geocacheSizes = Geoget.geocacheSizesFromFilter(PreferenceManager.getDefaultSharedPreferences(context));
                if (!geocacheSizes.isEmpty() && geocacheSizes.size() != Geoget.countSizes){
                	sql += " AND cachesize IN (\"" + geocacheSizes.remove(0) + "\"";
                    
                	for (String geocacheSize : geocacheSizes) {
                    	sql += ", \"" + geocacheSize + "\"";
                    }
                	sql += ") ";
                }

                // Filter terrain
                List<String> geocacheTerrains = Geoget.geocacheDiffTerrFromFilter(PreferenceManager.getDefaultSharedPreferences(context), "terr");
                if (!geocacheTerrains.isEmpty() && geocacheTerrains.size() != Geoget.countTerr){
                	sql += " AND terrain IN (\"" + geocacheTerrains.remove(0) + "\"";
                    
                	for (String geocacheTerrain : geocacheTerrains) {
                    	sql += ", \"" + geocacheTerrain + "\"";
                    }
                	sql += ") ";
                }

                // Filter difficulty
                List<String> geocacheDifficulties = Geoget.geocacheDiffTerrFromFilter(PreferenceManager.getDefaultSharedPreferences(context), "diff");
                if (!geocacheDifficulties.isEmpty() && geocacheDifficulties.size() != Geoget.countDiff){
                	sql += " AND difficulty IN (\"" + geocacheDifficulties.remove(0) + "\"";
                    
                	for (String geocacheDiff : geocacheDifficulties) {
                    	sql += ", \"" + geocacheDiff + "\"";
                    }
                	sql += ") ";
                }

                sql += " AND (" +
                		"(CAST(geocache.x AS REAL) > ? AND CAST(geocache.x AS REAL) < ? AND CAST(geocache.y AS REAL) > ? AND CAST(geocache.y AS REAL) < ?) " +
                		"OR (CAST(waypoint.x AS REAL) > ? AND CAST(waypoint.x AS REAL) < ? AND CAST(waypoint.y AS REAL) > ? AND CAST(waypoint.y AS REAL) < ?)" +
                		")";
                sql += " GROUP BY geocache.id";
                Cursor c = db.rawQuery(sql, cond);
                
                if (this.isCancelled()) {
                    c.close();
                    return null;
                }

                while (c.moveToNext()) {
                    if (this.isCancelled()) {
                        c.close();
                        return null;
                    }
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("x")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("y")));
                    Waypoint p = new Waypoint(c.getString(c.getColumnIndex("name")), loc);

                    GeocachingData gcData = new GeocachingData();
                    gcData.setCacheID(c.getString(c.getColumnIndex("id")));
                    gcData.setName(c.getString(c.getColumnIndex("name")));
                    gcData.difficulty = c.getFloat(c.getColumnIndex("difficulty"));
                    gcData.terrain = c.getFloat(c.getColumnIndex("terrain"));
                    gcData.setContainer(Geoget.convertCacheSize(c.getString(c.getColumnIndex("cachesize"))));
                    gcData.type = Geoget.convertCacheType(c.getString(c.getColumnIndex("cachetype")));
                    gcData.setOwner(c.getString(c.getColumnIndex("author")));
                    gcData.setPlacedBy(c.getString(c.getColumnIndex("author")));

                    gcData.available = Geoget.isAvailable(c.getInt(c.getColumnIndex("cachestatus")));
                    gcData.archived = Geoget.isArchived(c.getInt(c.getColumnIndex("cachestatus")));
                    gcData.found = Geoget.isFound(c.getInt(c.getColumnIndex("dtfound")));
                    gcData.computed = false;
  
                    /** Add PMO tag **/
                    String query = "SELECT geotagcategory.value AS key, geotagvalue.value FROM geotag " +
                    		"INNER JOIN geotagcategory ON geotagcategory.key = geotag.ptrkat " +
                    		"INNER JOIN geotagvalue ON geotagvalue.key = geotag.ptrvalue " +
                    		"WHERE geotagcategory.value = \"PMO\" AND geotag.id = ?";
                    Cursor tags = db.rawQuery(query, new String[]{gcData.getCacheID()});

                    while (tags.moveToNext()){
                   		if (tags.getString(tags.getColumnIndex("value")).equals("X")) {
                   			gcData.premiumOnly = true;
                   		}
                   	}
                    tags.close();
                    
                    /** Add waypoints to Geocache **/
                    Cursor wp = db.rawQuery("SELECT x, y, name, wpttype, cmt, prefixid, comment FROM waypoint WHERE id = ?", new String[]{gcData.getCacheID()});

                    while (wp.moveToNext()) {
                        if (this.isCancelled()) {
                            wp.close();
                            return null;
                        }

                        GeocachingWaypoint pgdw = new GeocachingWaypoint();
                        pgdw.lat = wp.getDouble(wp.getColumnIndex("x"));
                        pgdw.lon = wp.getDouble(wp.getColumnIndex("y"));
                        pgdw.name = wp.getString(wp.getColumnIndex("name"));
                        pgdw.type = Geoget.convertWaypointType(wp.getString(wp.getColumnIndex("wpttype")));
                        pgdw.desc = wp.getString(wp.getColumnIndex("cmt"));
                        pgdw.code = wp.getString(wp.getColumnIndex("prefixid"));

                        String comment = wp.getString(wp.getColumnIndex("comment"));
                        if (comment != null && !comment.equals("")){
                        	pgdw.desc += " <hr><b>" + context.getString(R.string.wp_personal_note) + "</b> " + comment;
                        }
                        gcData.waypoints.add(pgdw);
                    }
                    wp.close();

                    p.gcData = gcData;
                    // where to obtain more data
                    p.setExtraOnDisplay("net.kuratkoo.locusaddon.geogetdatabase", "net.kuratkoo.locusaddon.geogetdatabase.DetailActivity", "cacheId", gcData.getCacheID());
                    pd.addWaypoint(p);
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
		        Toast.makeText(context, "Error: "+exception.getClass()+ " - " + exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }

            try {
                File externalDir = Environment.getExternalStorageDirectory();
                String filePath = externalDir.getAbsolutePath();
                if (!filePath.endsWith("/")) {
                    filePath += "/";
                }
                filePath += "/Android/data/net.kuratkoo.locusaddon.geogetdatabase/livemap.locus";

                ArrayList<PackWaypoints> data = new ArrayList<PackWaypoints>();
                data.add(pd);
                ActionDisplayPoints.sendPacksFileSilent(context, data, filePath, true);
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
