package net.kuratkoo.locusaddon.geogetdatabase.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingLog;
import locus.api.objects.geocaching.GeocachingWaypoint;
import android.content.SharedPreferences;

/**
 * Geoget
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 * @author Jakub Jerabek <jerabek.jakub@gmail.com> since 2014-02
 */
public class Geoget {

    @SuppressWarnings("unused")
	private static final String TAG = "LocusAddonGeogetDatabase|GeogetUtils";

    public static String decodeZlib(byte[] s, byte[] buff) throws DataFormatException, OutOfMemoryError, UnsupportedEncodingException {
    	if (s == null){
    		return "";
    	}
        Inflater decompresser = new Inflater();
        decompresser.setInput(s, 0, s.length);
        int resultLength = decompresser.inflate(buff);
        decompresser.end();
        return new String(buff, 0, resultLength, "UTF-8");
    }

    public static int convertLogType(String logType) {
        if (logType.equals("Announcement")) {
            return GeocachingLog.CACHE_LOG_TYPE_ANNOUNCEMENT;
        } else if (logType.equals("Attended")) {
            return GeocachingLog.CACHE_LOG_TYPE_ATTENDED;
        } else if (logType.equals("Didn't find it")) {
            return GeocachingLog.CACHE_LOG_TYPE_NOT_FOUNDED;
        } else if (logType.equals("Enable Listing")) {
            return GeocachingLog.CACHE_LOG_TYPE_ENABLE_LISTING;
        } else if (logType.equals("Found it")) {
            return GeocachingLog.CACHE_LOG_TYPE_FOUNDED;
        } else if (logType.equals("Needs Archived")) {
            return GeocachingLog.CACHE_LOG_TYPE_NEEDS_ARCHIVED;
        } else if (logType.equals("Needs Maintenance")) {
            return GeocachingLog.CACHE_LOG_TYPE_NEEDS_MAINTENANCE;
        } else if (logType.equals("Owner Maintenance")) {
            return GeocachingLog.CACHE_LOG_TYPE_OWNER_MAINTENANCE;
        } else if (logType.equals("Post Reviewer Note")) {
            return GeocachingLog.CACHE_LOG_TYPE_POST_REVIEWER_NOTE;
        } else if (logType.equals("Publish Listing")) {
            return GeocachingLog.CACHE_LOG_TYPE_PUBLISH_LISTING;
        } else if (logType.equals("Temporarily Disable Listing")) {
            return GeocachingLog.CACHE_LOG_TYPE_TEMPORARILY_DISABLE_LISTING;
        } else if (logType.equals("Update Coordinates")) {
            return GeocachingLog.CACHE_LOG_TYPE_UPDATE_COORDINATES;
        } else if (logType.equals("Webcam Photo Taken")) {
            return GeocachingLog.CACHE_LOG_TYPE_WEBCAM_PHOTO_TAKEN;
        } else if (logType.equals("Will Attend")) {
            return GeocachingLog.CACHE_LOG_TYPE_WILL_ATTEND;
        } else if (logType.toLowerCase(Locale.getDefault()).equals("write note")) {
            return GeocachingLog.CACHE_LOG_TYPE_WRITE_NOTE;
        } else {
            return GeocachingLog.CACHE_LOG_TYPE_UNKNOWN;
        }
    }

    public static int convertCacheSize(String size) {
        if (size.equals("Small")) {
            return GeocachingData.CACHE_SIZE_SMALL;
        } else if (size.equals("Large")) {
            return GeocachingData.CACHE_SIZE_LARGE;
        } else if (size.equals("Micro")) {
            return GeocachingData.CACHE_SIZE_MICRO;
        } else if (size.equals("Not chosen")) {
            return GeocachingData.CACHE_SIZE_NOT_CHOSEN;
        } else if (size.equals("Other")) {
            return GeocachingData.CACHE_SIZE_OTHER;
        } else if (size.equals("Regular")) {
            return GeocachingData.CACHE_SIZE_REGULAR;
        } else if (size.equals("Virtual")) {
            return GeocachingData.CACHE_SIZE_NOT_CHOSEN;
        } else {
            return GeocachingData.CACHE_SIZE_NOT_CHOSEN;
        }
    }

    
    public static int convertCacheType(String type) {
        if (type.equals("Cache In Trash Out Event")) {
            return GeocachingData.CACHE_TYPE_CACHE_IN_TRASH_OUT;
        } else if (type.equals("Earthcache")) {
            return GeocachingData.CACHE_TYPE_EARTH;
        } else if (type.equals("Event Cache")) {
            return GeocachingData.CACHE_TYPE_EVENT;
        } else if (type.equals("Letterbox Hybrid")) {
            return GeocachingData.CACHE_TYPE_LETTERBOX;
        } else if (type.equals("Mega-Event Cache")) {
            return GeocachingData.CACHE_TYPE_MEGA_EVENT;
        } else if (type.equals("Multi-cache")) {
            return GeocachingData.CACHE_TYPE_MULTI;
        } else if (type.equals("Traditional Cache")) {
            return GeocachingData.CACHE_TYPE_TRADITIONAL;
        } else if (type.equals("Unknown Cache")) {
            return GeocachingData.CACHE_TYPE_MYSTERY;
        } else if (type.equals("Virtual Cache")) {
            return GeocachingData.CACHE_TYPE_VIRTUAL;
        } else if (type.equals("Webcam Cache")) {
            return GeocachingData.CACHE_TYPE_WEBCAM;
        } else if (type.equals("Wherigo Cache")) {
            return GeocachingData.CACHE_TYPE_WHERIGO;
        } else {
            return GeocachingData.CACHE_TYPE_TRADITIONAL;
        }
    }
    
    public static int countTypes = 11;
    public static int countSizes = 6;
    public static int countDiff = 9;
    public static int countTerr = 9;

    public static Boolean isAvailable(Integer cacheStatus) {
    	return (cacheStatus == 0 ? true : false);
    }

    public static Boolean isArchived(Integer cacheStatus) {
        return (cacheStatus == 2 ? true : false);
    }

    public static Boolean isFound(Integer dt) {
        return (dt > 0 ? true : false);
    }

    public static String convertWaypointType(String waypointType) {
        if (waypointType.equals("Final Location")) {
            return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_FINAL;
        } else if (waypointType.equals("Parking Area")) {
            return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PARKING;
        } else if (waypointType.equals("Question to Answer")) {
            return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_QUESTION;
        } else if (waypointType.equals("Reference Point")) {
            return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE;
        } else if (waypointType.equals("Stages of a Multicache")) {
            return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_STAGES;
        } else if (waypointType.equals("Trailhead")) {
            return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_TRAILHEAD;
        } else {
            return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE;
        }
    }

    public static Integer convertAttribute(String s) {
        if (s.contains("dogs")) {
            return 1;
        } else if (s.contains("fee")) {
            return 2;
        } else if (s.contains("rappelling")) {
            return 3;
        } else if (s.contains("boat")) {
            return 4;
        } else if (s.contains("scuba")) {
            return 5;
        } else if (s.contains("kids")) {
            return 6;
        } else if (s.contains("onehour")) {
            return 7;
        } else if (s.contains("scenic")) {
            return 8;
        } else if (s.contains("hiking")) {
            return 9;
        } else if (s.contains("climbing")) {
            return 10;
        } else if (s.contains("wading")) {
            return 11;
        } else if (s.contains("swimming")) {
            return 12;
        } else if (s.contains("available")) {
            return 13;
        } else if (s.contains("night")) {
            return 14;
        } else if (s.contains("winter")) {
            return 15;
        } else if (s.contains("camping")) {
            return 16;
        } else if (s.contains("poisonoak")) {
            return 17;
        } else if (s.contains("snakes")) {
            return 18;
        } else if (s.contains("ticks")) {
            return 19;
        } else if (s.contains("mine")) {
            return 20;
        } else if (s.contains("cliff")) {
            return 21;
        } else if (s.contains("hunting")) {
            return 22;
        } else if (s.contains("danger")) {
            return 23;
        } else if (s.contains("wheelchair")) {
            return 24;
        } else if (s.contains("parking")) {
            return 25;
        } else if (s.contains("public")) {
            return 26;
        } else if (s.contains("water")) {
            return 27;
        } else if (s.contains("restrooms")) {
            return 28;
        } else if (s.contains("phone")) {
            return 29;
        } else if (s.contains("picnic")) {
            return 30;
        } else if (s.contains("camping")) {
            return 31;
        } else if (s.contains("bicycles")) {
            return 32;
        } else if (s.contains("motorcycles")) {
            return 33;
        } else if (s.contains("quads")) {
            return 34;
        } else if (s.contains("jeeps")) {
            return 35;
        } else if (s.contains("snowmobiles")) {
            return 36;
        } else if (s.contains("horses")) {
            return 37;
        } else if (s.contains("campfires")) {
            return 38;
        } else if (s.contains("thorn")) {
            return 39;
        } else if (s.contains("stealth")) {
            return 40;
        } else if (s.contains("stroller")) {
            return 41;
        } else if (s.contains("firstaid")) {
            return 42;
        } else if (s.contains("cow")) {
            return 43;
        } else if (s.contains("flashlight")) {
            return 44;
        } else if (s.contains("landf")) {
            return 45;
        } else if (s.contains("rv")) {
            return 46;
        } else if (s.contains("field_puzzle")) {
            return 47;
        } else if (s.contains("UV")) {
            return 48;
        } else if (s.contains("snowshoes")) {
            return 49;
        } else if (s.contains("skiis")) {
            return 50;
        } else if (s.contains("s-tool")) {
            return 51;
        } else if (s.contains("nightcache")) {
            return 52;
        } else if (s.contains("parkngrab")) {
            return 53;
        } else if (s.contains("AbandonedBuilding")) {
            return 54;
        } else if (s.contains("hike_short")) {
            return 55;
        } else if (s.contains("hike_med")) {
            return 56;
        } else if (s.contains("hike_long")) {
            return 57;
        } else if (s.contains("fuel")) {
            return 58;
        } else if (s.contains("food")) {
            return 59;
        } else if (s.contains("wirelessbeacon")) {
            return 60;
        } else if (s.contains("partnership")) {
            return 61;
        } else if (s.contains("seasonal")) {
            return 62;
        } else if (s.contains("touristOK")) {
            return 63;
        } else if (s.contains("treeclimbing")) {
            return 64;
        } else if (s.contains("frontyard")) {
            return 65;
        } else if (s.contains("teamwork")) {
            return 66;
        } else {
            return 0;
        }
    }

    public static Boolean isAttributePositive(String s) {
        if (s.endsWith("yes")) {
            return true;
        } else {
            return false;
        }
    }

    public static Boolean isGeogetDatabase(File f) {
        return (f.exists() && f.canRead() && f.isFile() && f.getName().endsWith("db3") ? true : false);
    }
    
    public static List<String> geocacheSizesFromFilter(SharedPreferences sharedPref){
        List<String> geocacheSizes = new ArrayList<String>();

        if (sharedPref.getBoolean("gc_size_micro", false)) {
        	geocacheSizes.add("Micro");
        }
        if (sharedPref.getBoolean("gc_size_small", false)) {
        	geocacheSizes.add("Small");
        }
        if (sharedPref.getBoolean("gc_size_regular", false)) {
        	geocacheSizes.add("Regular");
        }
        if (sharedPref.getBoolean("gc_size_large", false)) {
        	geocacheSizes.add("Large");
        }
        if (sharedPref.getBoolean("gc_size_other", false)) {
        	geocacheSizes.add("Other");
        }
        if (sharedPref.getBoolean("gc_size_not_chosen", false)) {
        	geocacheSizes.add("Not Chosen");
        }

        return geocacheSizes;
    }
    
    public static List<String> geocacheDiffTerrFromFilter(SharedPreferences sharedPref, String attribute){
        List<String> geocacheDiffTerr = new ArrayList<String>();

        if (sharedPref.getBoolean("gc_"+attribute+"_1", false)) {
        	geocacheDiffTerr.add("1");
        }
        if (sharedPref.getBoolean("gc_"+attribute+"_15", false)) {
        	geocacheDiffTerr.add("1.5");
        }
        if (sharedPref.getBoolean("gc_"+attribute+"_2", false)) {
        	geocacheDiffTerr.add("2");
        }
        if (sharedPref.getBoolean("gc_"+attribute+"_25", false)) {
        	geocacheDiffTerr.add("2.5");
        }
        if (sharedPref.getBoolean("gc_"+attribute+"_3", false)) {
        	geocacheDiffTerr.add("3");
        }
        if (sharedPref.getBoolean("gc_"+attribute+"_35", false)) {
        	geocacheDiffTerr.add("3.5");
        }
        if (sharedPref.getBoolean("gc_"+attribute+"_4", false)) {
        	geocacheDiffTerr.add("4");
        }
        if (sharedPref.getBoolean("gc_"+attribute+"_45", false)) {
        	geocacheDiffTerr.add("4.5");
        }
        if (sharedPref.getBoolean("gc_"+attribute+"_5", false)) {
        	geocacheDiffTerr.add("5");
        }

        return geocacheDiffTerr;
    }
    
    public static List<String> geocacheTypesFromFilter(SharedPreferences sharedPref) {
        List<String> geocacheTypes = new ArrayList<String>();

        if (sharedPref.getBoolean("gc_type_tradi", false)) {
            geocacheTypes.add("Traditional Cache");
        }
        if (sharedPref.getBoolean("gc_type_multi", false)) {
            geocacheTypes.add("Multi-cache");
        }
        if (sharedPref.getBoolean("gc_type_mystery", false)) {
            geocacheTypes.add("Unknown Cache");
        }
        if (sharedPref.getBoolean("gc_type_earth", false)) {
            geocacheTypes.add("Earthcache");
        }
        if (sharedPref.getBoolean("gc_type_letter", false)) {
            geocacheTypes.add("Letterbox Hybrid");
        }
        if (sharedPref.getBoolean("gc_type_event", false)) {
            geocacheTypes.add("Event Cache");
        }
        if (sharedPref.getBoolean("gc_type_cito", false)) {
            geocacheTypes.add("Cache In Trash Out Event");
        }
        if (sharedPref.getBoolean("gc_type_mega", false)) {
            geocacheTypes.add("Mega-Event Cache");
        }
        if (sharedPref.getBoolean("gc_type_wig", false)) {
            geocacheTypes.add("Wherigo Cache");
        }
        if (sharedPref.getBoolean("gc_type_virtual", false)) {
            geocacheTypes.add("Virtual Cache");
        }
        if (sharedPref.getBoolean("gc_type_webcam", false)) {
            geocacheTypes.add("Webcam Cache");
        }

        return geocacheTypes;
    }
}
