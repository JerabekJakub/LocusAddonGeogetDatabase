package net.kuratkoo.locusaddon.geogetdatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import net.kuratkoo.locusaddon.geogetdatabase.util.NoCachesFoundException;
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
            progress.setMessage(getString(R.string.loading) + " " + values[0] + "/" + values[1] + " "+ getString(R.string.geocaches));
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
                
                long t1 = System.nanoTime();
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
                    sql += " AND dtfound = 0 ";
                    sqlWP += " AND dtfound = 0 ";
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

                Cursor c = db.rawQuery(sql, cond);
                Cursor cWP = db.rawQuery(sqlWP, cond2);

                /** Load GC codes **/
                List<Pair> gcCodes = new ArrayList<Pair>();
                radius = radius * 70 * 1000;
                // Caches
                while (c.moveToNext()) {
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(0));
                    loc.setLongitude(c.getDouble(1));
                    if (loc.distanceTo(curr) < radius) {
                    	gcCodes.add(new Pair(loc.distanceTo(curr), c.getString(2)));
                    }
                }
                c.close();
                
                if (this.isCancelled()) {
                    cWP.close();
                    return null;
                }

                // Waypoints
                while (cWP.moveToNext()) {
                    Location loc = new Location(TAG);
                    loc.setLatitude(cWP.getDouble(0));
                    loc.setLongitude(cWP.getDouble(1));
                    if (loc.distanceTo(curr) < radius){
                    	gcCodes.add(new Pair(loc.distanceTo(curr), cWP.getString(2)));
                    }
                }
                cWP.close();

                if (this.isCancelled()) {
                    return null;
                }

                // no caches found
                if (gcCodes.size() == 0) {
                	return new NoCachesFoundException();
                }

                int limit = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("limit", "0"));

                // Limit
                if (limit > 0 && gcCodes.size() > limit) {
                    // sort caches
                    Collections.sort(gcCodes, new Comparator<Pair>() {
                        public int compare(Pair p1, Pair p2) {
                            return p1.distance.compareTo(p2.distance);
                        }
                    });
                    
                    // cut off the rest
                    gcCodes = gcCodes.subList(0, limit);
                }

                // columns
                String columns = " geocache.id, x, y, name, difficulty, terrain, cachesize, cachetype, cachestatus, dtfound, author ";
                if (importCaches) {
                	columns += " , country, state, comment, hint, shortdesc, longdesc, geocache.dtupdate2, dthidden, shortdescflag, longdescflag ";
                }

                // GC codes
                int total = gcCodes.size();
                String codesCond = "(\""+gcCodes.remove(0).gcCode+"\"";
                for (Pair pair : gcCodes) {
                	codesCond += ",\""+pair.gcCode+"\"";
                }
                codesCond += ")";

                String query = "SELECT " + columns + " FROM geocache ";
                if (importCaches) {
                	query += " LEFT JOIN geolist ON geolist.id = geocache.id ";
                }
                query += " WHERE geocache.id IN " + codesCond;
                Cursor caches = db.rawQuery(query, null);

                int count = 0;
                byte[] buff = new byte[100000];
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMd", Locale.getDefault());
                String queryTags = "SELECT geotagcategory.value AS key, geotagvalue.value FROM geotag " +
                		"INNER JOIN geotagcategory ON geotagcategory.key = geotag.ptrkat " +
                		"INNER JOIN geotagvalue ON geotagvalue.key = geotag.ptrvalue " +
                		"WHERE geotagcategory.value IN (\"favorites\", \"Elevation\", \"PMO\") AND geotag.id = ?";
                String queryWPs = "SELECT x, y, name, wpttype, cmt, prefixid, comment, flag FROM waypoint WHERE id = ?";
                String queryLogs = "SELECT type, finder, logtext, dt FROM geolog WHERE id = ? LIMIT ?";
                String logsLimit = PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("logs_count", "0");
                long t2 = System.nanoTime();
                while (caches.moveToNext()) {
                    if (this.isCancelled()) {
                    	caches.close();
                        return null;
                    }

                    publishProgress(new Integer[]{++count, total});
                    
                    buff = new byte[100000];

                    Location loc = new Location(TAG);
                    // geocache.id, x, y, name, difficulty, terrain, cachesize, cachetype, cachestatus, dtfound, author
                    // import -> country, state, comment, hint, shortdesc, longdesc, geocache.dtupdate2, dthidden, shortdescflag, longdescflag
                    loc.setLatitude(caches.getDouble(1));
                    loc.setLongitude(caches.getDouble(2));
                    Waypoint wpt = new Waypoint(caches.getString(3), loc);
                    
                    GeocachingData gcData = new GeocachingData();
                    gcData.setCacheID(caches.getString(0));
                    gcData.setName(caches.getString(3));
                    gcData.difficulty = caches.getFloat(4);
                    gcData.terrain = caches.getFloat(5);
                    gcData.setContainer(Geoget.convertCacheSize(caches.getString(6)));
                    gcData.type = Geoget.convertCacheType(caches.getString(7));                    
                    gcData.available = Geoget.isAvailable(caches.getInt(8));
                    gcData.archived = Geoget.isArchived(caches.getInt(8));
                    gcData.found = Geoget.isFound(caches.getInt(9));
                    gcData.setOwner(caches.getString(10));
                    gcData.setPlacedBy(caches.getString(10));
                    gcData.computed = false;

                    if (importCaches) {
	                    gcData.setCountry(caches.getString(11));
	                    gcData.setState(caches.getString(12));
	                    gcData.setNotes(caches.getString(13));
	                    gcData.setEncodedHints(caches.getString(14));
	
	                    gcData.setShortDescription(
	                    		Geoget.decodeZlib(caches.getBlob(15), buff),
	                    		(caches.getInt(19) == 1 ? true : false));
	
	                    gcData.setLongDescription(
	                    		Geoget.decodeZlib(caches.getBlob(16), buff),
	                    		(caches.getInt(20) == 1 ? true : false));

	                    gcData.lastUpdated = caches.getLong(17);
	                    try {
	                    	gcData.dateCreated = dateFormat.parse(caches.getString(18)).getTime();
	                    } catch (ParseException ex) {
	                    	gcData.dateCreated = 0;
	                    }
                    }

                    /** Add PMO tag, favorite points and elevation **/
                    Cursor tags = db.rawQuery(queryTags, new String[]{gcData.getCacheID()});
                    while (tags.moveToNext()) {
                   		String key = tags.getString(0);
                   		String value = tags.getString(1);

                   		if (key.equals("PMO") && value.equals("X")) {
                   			gcData.premiumOnly = true;
                   		} else if (key.equals("favorites")) {
                   			try {
                   				gcData.favoritePoints = Integer.parseInt(value);
                   			} catch (NumberFormatException e) {
                   				// do nothing
                   			}
                   		} else if (key.equals("Elevation")) {
                   			try {
                   				wpt.getLocation().setAltitude(Double.parseDouble(value));
                   			} catch (NumberFormatException e){
                   				// do nothing
                   			}
                   		}
                   	}
                    tags.close();

                    /** Add waypoints to Geocache **/
                    // SELECT x, y, name, wpttype, cmt, prefixid, comment FROM waypoint WHERE id = ?
                    Cursor wp = db.rawQuery(queryWPs, new String[]{gcData.getCacheID()});
                    while (wp.moveToNext()) {
                        GeocachingWaypoint pgdw = new GeocachingWaypoint();
                        pgdw.setLat(wp.getDouble(0));
                        pgdw.setLon(wp.getDouble(1));
                        pgdw.setName(wp.getString(2));
                        pgdw.setType(Geoget.convertWaypointType(wp.getString(3)));
                        pgdw.setCode(wp.getString(5));
                        String desc = wp.getString(4);

                        String comment = wp.getString(6);
                        if (comment != null && !comment.equals("")){
                        	desc += " <hr><b>" + getString(R.string.wp_personal_note) + "</b> " + comment;
                        }
                        pgdw.setDesc(desc);

                        gcData.waypoints.add(pgdw);
                    }
                    wp.close();

                    /** Add logs to Geocache - only if import is enabled **/
                    // SELECT type, finder, logtext, dt FROM geolog WHERE id = ? LIMIT ?
                    if (importCaches) {
	                    Cursor logs = db.rawQuery(queryLogs, new String[]{gcData.getCacheID(), logsLimit});
	
	                    while (logs.moveToNext()) {	
	                        GeocachingLog log = new GeocachingLog();
	                        log.type = Geoget.convertLogType(logs.getString(0));
	                        log.finder = logs.getString(1);
	                        log.logText = Geoget.decodeZlib(logs.getBlob(2), buff);
	                        try {
	                        	log.date = dateFormat.parse(logs.getString(3)).getTime();
	                        } catch (ParseException ex){
	                        	log.date = 0;
	                        }

	                        gcData.logs.add(log);
	                    }
	                    logs.close();
                    }
                    
                    /** Add geocaching data to waypoint **/
                    wpt.gcData = gcData;
                    
                    /** Where to obtain more data **/
                    if (!importCaches){
                    	wpt.setExtraOnDisplay(
                    		"net.kuratkoo.locusaddon.geogetdatabase",
                    		"net.kuratkoo.locusaddon.geogetdatabase.DetailActivity",
                    		"cacheId",
                    		gcData.getCacheID());
                    }
                    
                    /** Add waypoint to list **/
                    packWpt.addWaypoint(wpt);
                }
                caches.close();
                
                long t3 = System.nanoTime();
                Log.d("VOLDIK", "Pøíprava: " + (t2-t1)/1000000);
                Log.d("VOLDIK", "Cyklus: " + (t3-t2)/1000000);
                Log.d("VOLDIK", "Celkem: " + (t3-t1)/1000000);
                
                if (this.isCancelled()) {
                    return null;
                }

    			File down = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	        	File file = new File(down.getAbsolutePath() + File.separator + "addonVystup.txt");
	        	file.getParentFile().mkdirs();
	        	FileOutputStream fos = new FileOutputStream(file, true);
	        	fos.write("\n############".getBytes());
	        	fos.write(("\nPriprava: "+(t2-t1)/1000000+" ms").getBytes());
	        	fos.write(("\nCyklus: "+(t3-t2)/1000000+" ms").getBytes());
	        	fos.write(("\nCelkem: "+(t3-t1)/1000000+" ms").getBytes());
	        	fos.write(("\nNacitano kesi: "+total).getBytes());
	        	fos.write(("\nKesi za sekundu: "+((float) total/((float) (t3-t1)/1000000000))).getBytes());
	        	fos.close();
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception ex) {
            super.onPostExecute(ex);
            progress.dismiss();
            db.close();
            if (ex != null) {
            	if (ex.getClass().getName().contains("NoCachesFoundException")) {
                	Toast.makeText(LoadActivity.this, getString(R.string.no_caches_found), Toast.LENGTH_LONG).show();
            	} else {
            		try {
	        			File down = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	    	        	File file = new File(down.getAbsolutePath() + File.separator + "addonERRVystup.txt");
	    	        	file.getParentFile().mkdirs();
	    	        	FileOutputStream fos = new FileOutputStream(file, true);
	    	        	fos.write("\n############\n".getBytes());
	    	        	PrintWriter pw = new PrintWriter(fos);     
	    	            ex.printStackTrace(pw);
	    	            pw.close();
	    	        	fos.close();
	            		ex.printStackTrace();
            		} catch (Exception e2) {}

            		Toast.makeText(LoadActivity.this, getString(R.string.unable_to_load_geocaches) + " (" + ex.getClass() + ")", Toast.LENGTH_LONG).show();
                }
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
        	
        	LocusUtils.handleIntentMainFunction(intent, new LocusUtils.OnIntentMainFunction() {
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
        } else if (LocusUtils.isIntentPointTools(intent)) {
    		try {
				point = LocusUtils.handleIntentPointTools(this, intent);
			} catch (RequiredVersionMissingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
