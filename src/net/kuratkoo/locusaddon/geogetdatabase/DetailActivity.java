package net.kuratkoo.locusaddon.geogetdatabase;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import locus.api.android.utils.LocusConst;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Waypoint;
import locus.api.objects.geocaching.GeocachingAttribute;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingLog;
import locus.api.objects.geocaching.GeocachingWaypoint;
import net.kuratkoo.locusaddon.geogetdatabase.util.Geoget;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * DetailActivity
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 * @author Jakub Jerabek <jerabek.jakub@gmail.com> since 2014-02
 */
public class DetailActivity extends Activity {

    private static final String TAG = "LocusAddonGeogetDatabase|DetailActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        Log.v(TAG, "Nacitam detaily z databaze...");
        // Check path to database.
        File fd;
        String database = PreferenceManager.getDefaultSharedPreferences(this).getString("db", "");
		try {
			fd = new File(URLDecoder.decode(database, "UTF-8"));
	        if (!Geoget.isGeogetDatabase(fd)) {
	            Toast.makeText(this, getString(R.string.no_db_file), Toast.LENGTH_LONG).show();
	            finish();
	            return;
	        }
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}


        if (intent.hasExtra("cacheId")) {
            String cacheId = intent.getStringExtra("cacheId");
            try {

                byte[] buff = new byte[100000];

                SQLiteDatabase db = SQLiteDatabase.openDatabase(
                		URLDecoder.decode(database, "UTF-8"), 
                		null, 
                		SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                Cursor c = db.rawQuery(
                		"SELECT * FROM geocache LEFT JOIN geolist ON geolist.id = geocache.id WHERE geocache.id = ?",
                		new String[]{cacheId});
                c.moveToNext();

                Location loc = new Location(TAG);
                loc.setLatitude(c.getDouble(c.getColumnIndex("x")));
                loc.setLongitude(c.getDouble(c.getColumnIndex("y")));
        		Waypoint wpt = new Waypoint(c.getString(c.getColumnIndex("name")), loc);

                GeocachingData gcData = new GeocachingData();
                gcData.setCacheID(c.getString(c.getColumnIndex("id")));
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
                gcData.setCountry(c.getString(c.getColumnIndex("country")));
                gcData.setState(c.getString(c.getColumnIndex("state")));
                gcData.setNotes(c.getString(c.getColumnIndex("comment")));
                gcData.computed = false;

                Date date = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMd", Locale.getDefault());
                gcData.dateCreated = date.getTime();
                gcData.lastUpdated = c.getLong(c.getColumnIndex("dtupdate2"));
                gcData.hidden = dateFormat.parse(c.getString(c.getColumnIndex("dthidden"))).getTime();

                // Try to get files
                String attachPath = PreferenceManager.getDefaultSharedPreferences(this).getString("attach", "");
                String filesDescription = "";
                if (attachPath.length() != 0) {
                    if (!attachPath.endsWith("/")) {
                        attachPath += "/";
                    }
                    File geocacheFilePath = new File(attachPath + gcData.getCacheID().substring(gcData.getCacheID().length() - 1) + "/" + gcData.getCacheID());
                    if (geocacheFilePath.exists() && geocacheFilePath.isDirectory() && geocacheFilePath.canRead()) {
                        filesDescription += "<b>" + getText(R.string.geoget_files) + ":</b>";
                        boolean first = true;
                        for (File f : geocacheFilePath.listFiles()) {
                            if (first) {
                                first = false;
                            } else {
                                filesDescription += " | ";
                            }
                            filesDescription += "<a href=\"file://" + f.getAbsolutePath() + "\">" + f.getName() + "</a>";
                        }
                        filesDescription += "<br /><hr /><br />";
                    }
                }


                gcData.setEncodedHints(c.getString(c.getColumnIndex("hint")));
                gcData.setShortDescription(filesDescription +
                		Geoget.decodeZlib(c.getBlob(c.getColumnIndex("shortdesc")), buff),
                		(c.getInt(c.getColumnIndex("shortdescflag")) == 1 ? true : false));
                gcData.setLongDescription(
                		Geoget.decodeZlib(c.getBlob(c.getColumnIndex("longdesc")), buff),
                		(c.getInt(c.getColumnIndex("longdescflag")) == 1 ? true : false));
                c.close();
                
                /** Add PMO tag, number of favorites and elevation **/
                String query = "SELECT geotagcategory.value AS key, geotagvalue.value FROM geotag " +
                		"INNER JOIN geotagcategory ON geotagcategory.key = geotag.ptrkat " +
                		"INNER JOIN geotagvalue ON geotagvalue.key = geotag.ptrvalue " +
                		"WHERE geotagcategory.value IN (\"favorites\", \"Elevation\", \"PMO\") AND geotag.id = ?";
                Cursor tags = db.rawQuery(query, new String[]{gcData.getCacheID()});

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
                Cursor wp = db.rawQuery("SELECT x, y, name, wpttype, cmt, prefixid, comment, flag FROM waypoint WHERE id = ?", new String[]{gcData.getCacheID()});

                while (wp.moveToNext()) {
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
                    	pgdw.desc += " <hr><b>" + getText(R.string.wp_personal_note) + "</b> " + comment;
                    }

                    gcData.waypoints.add(pgdw);
                }
                wp.close();

                /** Add logs to Geocache **/                    
                String logsLimit = PreferenceManager.getDefaultSharedPreferences(DetailActivity.this).getString("logs_count", "0");
                Cursor logs = db.rawQuery(
                		"SELECT dt, type, finder, logtext FROM geolog WHERE id = ? LIMIT ?",
                		new String[]{gcData.getCacheID(), logsLimit});

                while (logs.moveToNext()) {
                	GeocachingLog pgdl = new GeocachingLog();
                    pgdl.date = dateFormat.parse(logs.getString(logs.getColumnIndex("dt"))).getTime();
                    pgdl.finder = logs.getString(logs.getColumnIndex("finder"));
                    pgdl.logText = Geoget.decodeZlib(logs.getBlob(logs.getColumnIndex("logtext")), buff);
                    pgdl.type = Geoget.convertLogType(logs.getString(logs.getColumnIndex("type")));

					gcData.logs.add(pgdl);
                }
                logs.close();

                /** Add attributes to Geocache **/
                Cursor at = db.rawQuery("SELECT gtv.value, gtc.value AS category FROM geotag gt JOIN geotagvalue gtv ON gt.ptrvalue = gtv.key JOIN geotagcategory gtc ON gtc.key = gt.ptrkat WHERE gt.id = ?", new String[]{gcData.getCacheID()});

                while (at.moveToNext()) {
                    if (at.getString(at.getColumnIndex("category")).equals("attribute")) { // is Attribute, no index in db, fuuuu
                        GeocachingAttribute pga = new GeocachingAttribute(Geoget.convertAttribute(at.getString(at.getColumnIndex("value"))), Geoget.isAttributePositive(at.getString(at.getColumnIndex("value"))));
                        gcData.attributes.add(pga);
                    }
                }
                at.close();

                wpt.gcData = gcData;
                db.close();

                Intent retIntent = new Intent();
                retIntent.putExtra(LocusConst.INTENT_EXTRA_POINT, wpt.getAsBytes());
                setResult(RESULT_OK, retIntent);

            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.unable_to_load_detail) + " " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

            } finally {
                finish();
            }
        }
    }
}
