package net.kuratkoo.locusaddon.geogetdatabase;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import locus.api.android.ActionDisplay.ExtraAction;
import locus.api.android.ActionDisplayPoints;
import locus.api.android.objects.PackWaypoints;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Waypoint;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingLog;
import locus.api.objects.geocaching.GeocachingWaypoint;
import net.kuratkoo.locusaddon.geogetdatabase.util.Geoget;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * LoadActivity
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 * @author Jakub Jerabek <jerabek.jakub@gmail.com> since 2014-02
 */
public class LoadActivity extends Activity implements DialogInterface.OnDismissListener {

    private static final String TAG = "LocusAddonGeogetDatabase|LoadActivity";
    private boolean importCaches;
    private ProgressDialog progress;
    private PackWaypoints packWpt;
    private File externalDir;
    private Waypoint point;
    private LoadAsyncTask loadAsyncTask;

    public void onDismiss(DialogInterface arg0) {
        loadAsyncTask.cancel(true);
    }

    private class LoadAsyncTask extends AsyncTask<Waypoint, Integer, Exception> {

        private SQLiteDatabase db;

		@Override
        protected void onPreExecute() {
            progress.show();
            try {
				db = SQLiteDatabase.openDatabase(
						URLDecoder.decode(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("db", ""), "UTF-8"),
						null,
						SQLiteDatabase.NO_LOCALIZED_COLLATORS);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
            importCaches = PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("import", true);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progress.setMessage(getString(R.string.loading) + " " + values[0] + " " + getString(R.string.geocaches));
        }

        protected Exception doInBackground(Waypoint... pointSet) {
            try {
                if (this.isCancelled()) {
                    return null;
                }
                // center of display
                Waypoint pp = pointSet[0];

                Location curr =  new Location();
                curr.setLatitude(pp.getLocation().latitude);
                curr.setLongitude(pp.getLocation().longitude);

                packWpt = new PackWaypoints("Geoget data");
                Float radius = Float.valueOf(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("radius", "1")) / 70;

                String[] cond = new String[]{
                    String.valueOf(pp.getLocation().latitude - radius),
                    String.valueOf(pp.getLocation().latitude + radius),
                    String.valueOf(pp.getLocation().longitude - radius),
                    String.valueOf(pp.getLocation().longitude + radius)
                };
                String[] cond2 = new String[8];
                cond2[0] = cond[0];
                cond2[1] = cond[1];
                cond2[2] = cond[2];
                cond2[3] = cond[3];
                cond2[4] = cond[0];
                cond2[5] = cond[1];
                cond2[6] = cond[2];
                cond2[7] = cond[3];
                
                Cursor c;
                Cursor cWP;
                String sql = "SELECT x, y, id FROM geocache WHERE cachestatus IN (0";
                String sqlWP = "SELECT waypoint.x, waypoint.y, waypoint.id " +
                		"FROM waypoint LEFT JOIN geocache ON geocache.id = waypoint.id " +
                		"WHERE cachestatus IN (0";

                // Disabled geocaches
                if (PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("disable", false)) {
                    sql += ",1";
                    sqlWP += ",1";
                }

                // Archived geocaches
                if (PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("archive", false)) {
                    sql += ",2";
                    sqlWP += ",2";
                }
                
                sql += ") ";
                sqlWP += ") ";

                // Found by user
                if (!PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("found", false)) {
                    sql += " AND dtfound = 0";
                    sqlWP += " AND dtfound = 0";
                }

                // Owned by user
                if (!PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("own", false)) {
                	String author = PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("nick", "");
                    sql += " AND author != \"" + author + "\"";
                    sqlWP += " AND author != \"" + author + "\"";
                }

                // Filter cache type
                List<String> geocacheTypes = Geoget.geocacheTypesFromFilter(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this));
                
                if (!geocacheTypes.isEmpty() && geocacheTypes.size() != Geoget.countTypes){
                	String firstType = geocacheTypes.remove(0);
                	sql += " AND cachetype IN (\"" + firstType + "\"";
                	sqlWP += " AND cachetype IN (\"" + firstType + "\"";
                    
                	for (String geocacheType : geocacheTypes) {
                    	sql += ", \"" + geocacheType + "\"";
                    	sqlWP += ", \"" + geocacheType + "\"";
                    }
                	sql += ") ";
                	sqlWP += ") ";
                }

                // Filter cache size
                List<String> geocacheSizes = Geoget.geocacheSizesFromFilter(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this));
                if (!geocacheSizes.isEmpty() && geocacheSizes.size() != Geoget.countSizes){
                	String firstSize = geocacheSizes.remove(0);
                	sql += " AND cachesize IN (\"" + firstSize + "\"";
                	sqlWP += " AND cachesize IN (\"" + firstSize + "\"";
                    
                	for (String geocacheSize : geocacheSizes) {
                    	sql += ", \"" + geocacheSize + "\"";
                    	sqlWP += ", \"" + geocacheSize + "\"";
                    }
                	sql += ") ";
                	sqlWP += ") ";
                }

                // Filter terrain
                List<String> geocacheTerrains = Geoget.geocacheDiffTerrFromFilter(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this), "terr");
                if (!geocacheTerrains.isEmpty() && geocacheTerrains.size() != Geoget.countTerr){
                	String firstTerrain = geocacheTerrains.remove(0);
                	sql += " AND terrain IN (\"" + firstTerrain + "\"";
                	sqlWP += " AND terrain IN (\"" + firstTerrain + "\"";
                    
                	for (String geocacheTerrain : geocacheTerrains) {
                    	sql += ", \"" + geocacheTerrain + "\"";
                    	sqlWP += ", \"" + geocacheTerrain + "\"";
                    }
                	sql += ") ";
                	sqlWP += ") ";
                }

                // Filter difficulty
                List<String> geocacheDifficulties = Geoget.geocacheDiffTerrFromFilter(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this), "diff");
                if (!geocacheDifficulties.isEmpty() && geocacheDifficulties.size() != Geoget.countDiff){
                	String firstDiff = geocacheDifficulties.remove(0);
                	sql += " AND difficulty IN (\"" + firstDiff + "\"";
                	sqlWP += " AND difficulty IN (\"" + firstDiff + "\"";
                    
                	for (String geocacheDiff : geocacheDifficulties) {
                    	sql += ", \"" + geocacheDiff + "\"";
                    	sqlWP += ", \"" + geocacheDiff + "\"";
                    }
                	sql += ") ";
                	sqlWP += ") ";
                }

                sql += " AND CAST(x AS REAL) > ? AND CAST(x AS REAL) < ? AND CAST(y AS REAL) > ? AND CAST(y AS REAL) < ?";
                sqlWP += " AND (CAST(waypoint.x AS REAL) > ? AND CAST(waypoint.x AS REAL) < ? AND CAST(waypoint.y AS REAL) > ? AND CAST(waypoint.y AS REAL) < ?) " +
                		"AND (CAST(geocache.x AS REAL) <= ? OR CAST(geocache.x AS REAL) >= ? OR CAST(geocache.y AS REAL) <= ? OR CAST(geocache.y AS REAL) >= ?)";
                c = db.rawQuery(sql, cond);
                cWP = db.rawQuery(sqlWP, cond2);

                /** Load GC codes **/
                List<Pair> gcCodes = new ArrayList<Pair>();
                radius = radius * 70 * 1000;
                // Caches
                while (c.moveToNext()){
                    if (this.isCancelled()) {
                        c.close();
                        return null;
                    }
                    
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("x")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("y")));
                    if (loc.distanceTo(curr) < radius){
                    	gcCodes.add(new Pair(loc.distanceTo(curr), c.getString(c.getColumnIndex("id"))));
                    }
                	
                }
                // Waypoints
                while (cWP.moveToNext()){
                    if (this.isCancelled()) {
                        cWP.close();
                        return null;
                    }
                    
                    Location loc = new Location(TAG);
                    loc.setLatitude(cWP.getDouble(cWP.getColumnIndex("x")));
                    loc.setLongitude(cWP.getDouble(cWP.getColumnIndex("y")));
                    if (loc.distanceTo(curr) < radius){
                    	gcCodes.add(new Pair(loc.distanceTo(curr), cWP.getString(cWP.getColumnIndex("id"))));
                    }                	
                }
                c.close();
                cWP.close();

                int count = 0;
                int limit = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("limit", "0"));

                
                if (limit > 0) {
                	// remove duplicity
                    gcCodes = new ArrayList<Pair>(new HashSet<Pair>(gcCodes));
                    // sort 
                    Collections.sort(gcCodes, new Comparator<Pair>() {
                        public int compare(Pair p1, Pair p2) {
                            return p1.distance.compareTo(p2.distance);
                        }
                    });
                }

                /** Load geocaches from DB **/
                for (Pair pair : gcCodes) {
                    if (this.isCancelled()) {
                        return null;
                    }

                    if (limit > 0 && count >= limit) {
                    	break;
                    }

                    byte[] buff = new byte[100000];
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMd", Locale.getDefault());
                    String gcCode = pair.gcCode;
                    
                    publishProgress(++count);
                    
                    String columns = " x, y, name, difficulty, terrain, cachesize, cachetype, cachestatus, dtfound, author ";
                    if (importCaches){
                    	columns += ", author, country, state, comment, hint, shortdesc, longdesc, geocache.dtupdate2, dthidden, shortdescflag, longdescflag";
                    }

                    c = db.rawQuery(
                    		"SELECT " + columns + " FROM geocache LEFT JOIN geolist ON geolist.id = geocache.id WHERE geocache.id = ?",
                    		new String[]{gcCode});
                    c.moveToNext();
                    
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("x")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("y")));
                    Waypoint wpt = new Waypoint(c.getString(c.getColumnIndex("name")), loc);
                    
                    GeocachingData gcData = new GeocachingData();

                    gcData.setCacheID(gcCode);
                    gcData.setName(c.getString(c.getColumnIndex("name")));
                    gcData.difficulty = c.getFloat(c.getColumnIndex("difficulty"));
                    gcData.terrain = c.getFloat(c.getColumnIndex("terrain"));
                    gcData.setContainer(Geoget.convertCacheSize(c.getString(c.getColumnIndex("cachesize"))));
                    gcData.type = Geoget.convertCacheType(c.getString(c.getColumnIndex("cachetype")));                    
                    gcData.available = Geoget.isAvailable(c.getInt(c.getColumnIndex("cachestatus")));
                    gcData.archived = Geoget.isArchived(c.getInt(c.getColumnIndex("cachestatus")));
                    gcData.found = Geoget.isFound(c.getInt(c.getColumnIndex("dtfound")));
                    gcData.setOwner(c.getString(c.getColumnIndex("author")));
                    gcData.setPlacedBy(c.getString(c.getColumnIndex("author")));
                    gcData.computed = false;
                    
                    if (importCaches) {
	                    gcData.setCountry(c.getString(c.getColumnIndex("country")));
	                    gcData.setState(c.getString(c.getColumnIndex("state")));
	                    gcData.setNotes(c.getString(c.getColumnIndex("comment")));
	
	                    gcData.setEncodedHints(c.getString(c.getColumnIndex("hint")));
	
	                    gcData.setShortDescription(
	                    		Geoget.decodeZlib(c.getBlob(c.getColumnIndex("shortdesc")), buff),
	                    		(c.getInt(c.getColumnIndex("shortdescflag")) == 1 ? true : false));
	
	                    gcData.setLongDescription(
	                    		Geoget.decodeZlib(c.getBlob(c.getColumnIndex("longdesc")), buff),
	                    		(c.getInt(c.getColumnIndex("longdescflag")) == 1 ? true : false));

	                    Date date = new Date();
	                    gcData.dateCreated = date.getTime();
	                    gcData.lastUpdated = c.getLong(c.getColumnIndex("dtupdate2"));
	                    gcData.hidden = dateFormat.parse(c.getString(c.getColumnIndex("dthidden"))).getTime();
                    }
                    c.close();
                    
                    /** Add PMO tag, favorite points and elevation **/
                    String query = "SELECT geotag.id, geotagcategory.value AS key, geotagvalue.value FROM geotag " +
                    		"INNER JOIN geotagcategory ON geotagcategory.key = geotag.ptrkat " +
                    		"INNER JOIN geotagvalue ON geotagvalue.key = geotag.ptrvalue " +
                    		"WHERE geotagcategory.value IN (\"favorites\", \"Elevation\", \"PMO\") AND geotag.id = ?";
                    Cursor tags = db.rawQuery(query, new String[]{gcCode});

                    while (tags.moveToNext()){
                   		String key = tags.getString(tags.getColumnIndex("key"));
                   		String value = tags.getString(tags.getColumnIndex("value"));

                   		if (key.equals("PMO") && value.equals("X")) {
                   			gcData.premiumOnly = true;
                   		} else if (key.equals("favorites")) {
                   			gcData.favoritePoints = Integer.parseInt(value);
                   		} else if (key.equals("Elevation")) {
                    		wpt.getLocation().setAltitude(Double.parseDouble(value));
                   		}
                   	}
                    tags.close();

                    /** Add waypoints to Geocache **/
                    Cursor wp = db.rawQuery(
                    		"SELECT x, y, name, wpttype, cmt, prefixid, comment, flag FROM waypoint WHERE id = ?",
                    		new String[]{gcData.getCacheID()});

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

                        // Personal note from Geoget
                        String comment = wp.getString(wp.getColumnIndex("comment"));
                        if (comment != null && !comment.equals("")){
                        	pgdw.desc += " <br><b>" + getString(R.string.wp_personal_note) + "</b> " + comment;
                        }

                        gcData.waypoints.add(pgdw);
                    }
                    wp.close();

                    if (importCaches){
                        /** Add logs to Geocache - only if import is enabled **/
                    	String logsLimit = PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("logs_count", "0");
	                    Cursor logs = db.rawQuery(
	                    		"SELECT dt, type, finder, logtext FROM geolog WHERE id = ? LIMIT ?",
	                    		new String[]{gcData.getCacheID(), logsLimit});
	
	                    while (logs.moveToNext()) {
	                        if (this.isCancelled()) {
	                            logs.close();
	                            return null;
	                        }
	
	                        GeocachingLog log = new GeocachingLog();
	                        log.finder = logs.getString(logs.getColumnIndex("finder"));
	                        log.logText = Geoget.decodeZlib(logs.getBlob(logs.getColumnIndex("logtext")), buff);
	                        log.type = Geoget.convertLogType(logs.getString(logs.getColumnIndex("type")));
	                       	log.date = dateFormat.parse(logs.getString(logs.getColumnIndex("dt"))).getTime();
	                        
	                        gcData.logs.add(log);
	                    }
	                    logs.close();
                    }
                    
                    wpt.gcData = gcData;
                    if (!importCaches){
                    	wpt.setExtraOnDisplay(
                    		"net.kuratkoo.locusaddon.geogetdatabase",
                    		"net.kuratkoo.locusaddon.geogetdatabase.DetailActivity",
                    		"cacheId",
                    		gcData.getCacheID());
                    }
                    packWpt.addWaypoint(wpt);
                }
                
                if (this.isCancelled()) {
                    return null;
                }
                return null;
            } catch (ParseException e){
            	Toast.makeText(LoadActivity.this, "Chyba pri zpracovani data... ", Toast.LENGTH_LONG).show();
                return e;
            } catch (Exception e) {
            	e.printStackTrace();
            	Log.e("VOLDIK", e.getLocalizedMessage()+", "+e.getMessage()+", "+e.getStackTrace());
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception ex) {
            super.onPostExecute(ex);
            progress.dismiss();
            db.close();
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

			ArrayList<PackWaypoints> data = new ArrayList<PackWaypoints>();
			data.add(packWpt);

            try {
            	ActionDisplayPoints.sendPacksFile(
            			LoadActivity.this,
            			data,
            			filePath,
            			(importCaches ? ExtraAction.IMPORT : ExtraAction.CENTER));
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
            } catch (RequiredVersionMissingException rvme) {
                Toast.makeText(LoadActivity.this, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception ex1){
                Toast.makeText(LoadActivity.this, "Error: " + ex1.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            db.close();
            progress.dismiss();
            Toast.makeText(LoadActivity.this, getString(R.string.canceled), Toast.LENGTH_LONG).show();
            LoadActivity.this.finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.loading_dots));
        progress.setIcon(android.R.drawable.ic_dialog_info);
        progress.setTitle(getString(R.string.loading));
        progress.setOnDismissListener(this);

        externalDir = Environment.getExternalStorageDirectory();
        if (externalDir == null || !(externalDir.exists())) {
            Toast.makeText(LoadActivity.this, getString(R.string.no_external_storage), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        File fd;
		try {
			fd = new File(URLDecoder.decode(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("db", ""), "UTF-8"));
			if (!Geoget.isGeogetDatabase(fd)) {
	            Toast.makeText(LoadActivity.this, getString(R.string.no_db_file), Toast.LENGTH_LONG).show();
	            finish();
	            return;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        
        Intent intent = getIntent();

        if (LocusUtils.isIntentMainFunction(intent)) {
        	LocusUtils.handleIntentMainFunction(intent,
        			new LocusUtils.OnIntentMainFunction() {
				@Override
				public void onReceived(Location locGps, Location locMapCenter) {
					// get map center
					point = new Waypoint("Map center", locMapCenter);
				}
				
				@Override
				public void onFailed() {
					Toast.makeText(LoadActivity.this, "Wrong INTENT!", Toast.LENGTH_LONG).show();
				}
			});
        }
        
        
        loadAsyncTask = new LoadAsyncTask();
        loadAsyncTask.execute(new Waypoint[]{point});
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
