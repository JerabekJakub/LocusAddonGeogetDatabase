package net.kuratkoo.locusaddon.geogetdatabase;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import menion.android.locus.addon.publiclib.LocusConst;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingAttributes;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataLog;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataWaypoint;

public class DetailActivity extends Activity {

    private static final String TAG = "LocusAddonGeogetDatabase|DetailActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        File fd = new File(PreferenceManager.getDefaultSharedPreferences(this).getString("db", ""));
        if (!GeogetUtils.isGeogetDatabase(fd)) {
            Toast.makeText(this, R.string.no_db_file, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (intent.hasExtra("cacheId")) {
            String value = intent.getStringExtra("cacheId");
            try {

                byte[] buff = new byte[100000];

                SQLiteDatabase database = SQLiteDatabase.openDatabase(PreferenceManager.getDefaultSharedPreferences(this).getString("db", ""), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                Cursor c = database.rawQuery("SELECT * FROM geocache JOIN geolist USING (id) WHERE id = ?", new String[]{value});
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
                gcData.encodedHints = c.getString(c.getColumnIndex("hint"));
                gcData.longDescription = GeogetUtils.decodeZlib(c.getBlob(c.getColumnIndex("longdesc")), buff);

                // Try to get files
                String attachPath = PreferenceManager.getDefaultSharedPreferences(this).getString("attach", "");
                String filesDescription = "";
                if (attachPath.length() != 0) {
                    if (!attachPath.endsWith("/")) {
                        attachPath += "/";
                    }
                    File geocacheFilePath = new File(attachPath + gcData.cacheID.substring(gcData.cacheID.length() - 1) + "/" + gcData.cacheID);
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
                gcData.shortDescription = filesDescription + GeogetUtils.decodeZlib(c.getBlob(c.getColumnIndex("shortdesc")), buff);
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
                }
                wp.close();
                gcData.waypoints = pgdws;

                /** Add logs to Geocache **/
                String limit = PreferenceManager.getDefaultSharedPreferences(this).getString("logs_count", "20");
                Cursor logs = database.rawQuery("SELECT dt, finder, type, logtext FROM geolog WHERE id = ? ORDER BY dt DESC LIMIT ?", new String[]{gcData.cacheID, limit});
                ArrayList<PointGeocachingDataLog> pgdls = new ArrayList<PointGeocachingDataLog>();

                while (logs.moveToNext()) {
                    PointGeocachingDataLog pgdl = new PointGeocachingDataLog();
                    String found = logs.getString(logs.getColumnIndex("dt"));
                    pgdl.date = found.substring(0, 4) + "-" + found.substring(4, 6) + "-" + found.substring(6, 8) + "T00:00:00Z";
                    pgdl.finder = logs.getString(logs.getColumnIndex("finder"));
                    pgdl.logText = GeogetUtils.decodeZlib(logs.getBlob(logs.getColumnIndex("logtext")), buff);
                    pgdl.type = GeogetUtils.convertLogType(logs.getString(logs.getColumnIndex("type")));
                    pgdls.add(pgdl);
                }
                logs.close();
                gcData.logs = pgdls;

                /** Add attributes to Geocache **/
                Cursor at = database.rawQuery("SELECT gtv.value, gtc.value AS category FROM geotag gt JOIN geotagvalue gtv ON gt.ptrvalue = gtv.key JOIN geotagcategory gtc ON gtc.key = gt.ptrkat WHERE gt.id = ?", new String[]{gcData.cacheID});
                ArrayList<PointGeocachingAttributes> pgas = new ArrayList<PointGeocachingAttributes>();

                while (at.moveToNext()) {
                    if (at.getString(at.getColumnIndex("category")).equals("attribute")) { // is Attribute, no index in db, fuuuu
                        PointGeocachingAttributes pga = new PointGeocachingAttributes(GeogetUtils.convertAttribute(at.getString(at.getColumnIndex("value"))), GeogetUtils.isAttributePositive(at.getString(at.getColumnIndex("value"))));
                        pgas.add(pga);
                    }
                }
                at.close();
                gcData.attributes = pgas;

                p.setGeocachingData(gcData);
                database.close();

                Intent retIntent = new Intent();
                retIntent.putExtra(LocusConst.EXTRA_POINT, p);
                setResult(RESULT_OK, retIntent);

            } catch (Exception e) {
                Toast.makeText(this, R.string.unable_to_load_detail, Toast.LENGTH_LONG).show();

            } finally {
                finish();
            }
        }
    }
}
