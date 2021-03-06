package net.kuratkoo.locusaddon.geogetdatabase;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import locus.api.android.utils.LocusConst;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Waypoint;
import locus.api.objects.geocaching.GeocachingAttribute;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingLog;
import net.kuratkoo.locusaddon.geogetdatabase.util.Geoget;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * DetailActivity
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 * @author Jakub Jerabek <jerabek.jakub@gmail.com> since 2014-02
 */
public class DetailActivity extends Activity {
    private static final String TAG = "LocusAddonGeogetDatabase|DetailActivity";
    private Cursor c;
    private Cursor at;
    private Cursor logs;
    private Cursor tags;
    private SQLiteDatabase db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

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

                db = SQLiteDatabase.openDatabase(
                		URLDecoder.decode(database, "UTF-8"), 
                		null, 
                		SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                c = db.rawQuery(
                		"SELECT geocache.*, shortdesc, shortdescflag, longdesc, longdescflag, hint " +
                		" FROM geocache LEFT JOIN geolist ON geolist.id = geocache.id " +
                		" WHERE geocache.id = ?",
                		new String[]{cacheId});
                c.moveToNext();

                Location loc = new Location(TAG);
                loc.setLatitude(c.getDouble(c.getColumnIndex("x")));
                loc.setLongitude(c.getDouble(c.getColumnIndex("y")));
        		Waypoint wpt = new Waypoint(c.getString(c.getColumnIndex("name")), loc);

                GeocachingData gcData = new GeocachingData();
                gcData.setCacheID(c.getString(c.getColumnIndex("id")));
                gcData.setName(c.getString(c.getColumnIndex("name")));
                gcData.setDifficulty(c.getFloat(c.getColumnIndex("difficulty")));
                gcData.setTerrain(c.getFloat(c.getColumnIndex("terrain")));
                gcData.setContainer(Geoget.convertCacheSize(c.getString(c.getColumnIndex("cachesize"))));
                gcData.setType(Geoget.convertCacheType(c.getString(c.getColumnIndex("cachetype"))));                    
                gcData.setAvailable(Geoget.isAvailable(c.getInt(c.getColumnIndex("cachestatus"))));
                gcData.setArchived(Geoget.isArchived(c.getInt(c.getColumnIndex("cachestatus"))));
                gcData.setFound(Geoget.isFound(c.getInt(c.getColumnIndex("dtfound"))));
                gcData.setOwner(c.getString(c.getColumnIndex("author")));
                gcData.setPlacedBy(c.getString(c.getColumnIndex("author")));
                gcData.setCountry(c.getString(c.getColumnIndex("country")));
                gcData.setState(c.getString(c.getColumnIndex("state")));
                gcData.setNotes(c.getString(c.getColumnIndex("comment")));
                gcData.setLatOriginal(c.getDouble(c.getColumnIndex("x")));
                gcData.setLonOriginal(c.getDouble(c.getColumnIndex("y")));

                Double d = c.getDouble(c.getColumnIndex("dtupdate2"));
                gcData.setLastUpdated(Math.round((d-25569) * 86400000));
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMd", Locale.getDefault());
                try {
                    gcData.setDateCreated(dateFormat.parse(c.getString(c.getColumnIndex("dthidden"))).getTime());
                } catch(ParseException ex) {
                	ex.printStackTrace();
                	gcData.setDateCreated(0);
                }

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
                
                /** Add PMO tag, number of favorites and elevation **/
                String query = "SELECT geotagcategory.value AS key, geotagvalue.value FROM geotag " +
                		"INNER JOIN geotagcategory ON geotagcategory.key = geotag.ptrkat " +
                		"INNER JOIN geotagvalue ON geotagvalue.key = geotag.ptrvalue " +
                		"WHERE geotagcategory.value IN (\"favorites\", \"Elevation\", \"PMO\") AND geotag.id = ?";
                tags = db.rawQuery(query, new String[]{gcData.getCacheID()});

                while (tags.moveToNext()){
               		String key = tags.getString(tags.getColumnIndex("key"));
               		String value = tags.getString(tags.getColumnIndex("value"));
               		
               		if (key.equals("PMO") && value.equals("X")) {
               			gcData.setPremiumOnly(true);
               		} else if (key.equals("favorites")) {
               			gcData.setFavoritePoints(Integer.parseInt(value));
               		} else if (key.equals("Elevation")) {
                		wpt.getLocation().setAltitude(Double.parseDouble(value));
               		}
               	}

                /** Add logs to Geocache **/                    
                String logsLimit = PreferenceManager.getDefaultSharedPreferences(DetailActivity.this).getString("logs_count", "0");
                logs = db.rawQuery(
                		"SELECT dt, type, finder, logtext FROM geolog WHERE id = ? LIMIT ?",
                		new String[]{gcData.getCacheID(), logsLimit});

                while (logs.moveToNext()) {
                	GeocachingLog pgdl = new GeocachingLog();
                	
                    pgdl.setFinder(logs.getString(logs.getColumnIndex("finder")));
                    pgdl.setLogText(Geoget.decodeZlib(logs.getBlob(logs.getColumnIndex("logtext")), buff));
                    pgdl.setType(Geoget.convertLogType(logs.getString(logs.getColumnIndex("type"))));
                    try {
                        pgdl.setDate(dateFormat.parse(logs.getString(logs.getColumnIndex("dt"))).getTime());
                    } catch (ParseException ex) {
                    	pgdl.setDate(0);
                    }                    
					gcData.logs.add(pgdl);
                }

                /** Add attributes to Geocache **/
                at = db.rawQuery("SELECT gtv.value, gtc.value AS category FROM geotag gt JOIN geotagvalue gtv ON gt.ptrvalue = gtv.key JOIN geotagcategory gtc ON gtc.key = gt.ptrkat WHERE gt.id = ?", new String[]{gcData.getCacheID()});

                while (at.moveToNext()) {
                    if (at.getString(at.getColumnIndex("category")).equals("attribute")) { // is Attribute, no index in db, fuuuu
                        GeocachingAttribute pga = new GeocachingAttribute(Geoget.convertAttribute(at.getString(at.getColumnIndex("value"))), Geoget.isAttributePositive(at.getString(at.getColumnIndex("value"))));
                        gcData.attributes.add(pga);
                    }
                }

                wpt.gcData = gcData;

                Intent retIntent = new Intent();
                retIntent.putExtra(LocusConst.INTENT_EXTRA_POINT, wpt.getAsBytes());
                setResult(RESULT_OK, retIntent);

            } catch (Exception e) {
            	e.printStackTrace();
                Toast.makeText(this, getString(R.string.unable_to_load_detail) + " " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

            } finally {
            	if(tags != null){tags.close();}
            	if(at != null){at.close();}
            	if(logs != null){logs.close();}
            	if(c != null){c.close();}
            	if(db != null){db.close();} 
            	finish();
            }
        }
    }
}
