package net.kuratkoo.locusaddon.geogetdatabase;

import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;

public class Geoget {

    private static final String TAG = "LocusAddonGeogetDatabase|Geoget";

    public static String decodeZlib(byte[] s, byte[] buff) throws DataFormatException, UnsupportedEncodingException {
        Inflater decompresser = new Inflater();
        decompresser.setInput(s, 0, s.length);
        int resultLength = decompresser.inflate(buff);
        decompresser.end();
        return new String(buff, 0, resultLength, "UTF-8");
    }

    public static int convertLogType(String logType) {
        if (logType.equals("Announcement")) {
            return PointGeocachingData.CACHE_LOG_TYPE_ANNOUNCEMENT;
        } else if (logType.equals("Attended")) {
            return PointGeocachingData.CACHE_LOG_TYPE_ATTENDED;
        } else if (logType.equals("Didn't find it")) {
            return PointGeocachingData.CACHE_LOG_TYPE_NOT_FOUNDED;
        } else if (logType.equals("Enable Listing")) {
            return PointGeocachingData.CACHE_LOG_TYPE_ENABLE_LISTING;
        } else if (logType.equals("Found it")) {
            return PointGeocachingData.CACHE_LOG_TYPE_FOUNDED;
        } else if (logType.equals("Needs Archived")) {
            return PointGeocachingData.CACHE_LOG_TYPE_NEEDS_ARCHIVED;
        } else if (logType.equals("Needs Maintenance")) {
            return PointGeocachingData.CACHE_LOG_TYPE_NEEDS_MAINTENANCE;
        } else if (logType.equals("Owner Maintenance")) {
            return PointGeocachingData.CACHE_LOG_TYPE_OWNER_MAINTENANCE;
        } else if (logType.equals("Post Reviewer Note")) {
            return PointGeocachingData.CACHE_LOG_TYPE_POST_REVIEWER_NOTE;
        } else if (logType.equals("Publish Listing")) {
            return PointGeocachingData.CACHE_LOG_TYPE_PUBLISH_LISTING;
        } else if (logType.equals("Temporarily Disable Listing")) {
            return PointGeocachingData.CACHE_LOG_TYPE_TEMPORARILY_DISABLE_LISTING;
        } else if (logType.equals("Update Coordinates")) {
            return PointGeocachingData.CACHE_LOG_TYPE_UPDATE_COORDINATES;
        } else if (logType.equals("Webcam Photo Taken")) {
            return PointGeocachingData.CACHE_LOG_TYPE_WEBCAM_PHOTO_TAKEN;
        } else if (logType.equals("Will Attend")) {
            return PointGeocachingData.CACHE_LOG_TYPE_WILL_ATTEND;
        } else if (logType.toLowerCase().equals("write note")) {
            return PointGeocachingData.CACHE_LOG_TYPE_WRITE_NOTE;
        } else {
            return PointGeocachingData.CACHE_LOG_TYPE_UNKNOWN;
        }
    }

    public static int convertCacheSize(String size) {
        if (size.equals("Small")) {
            return PointGeocachingData.CACHE_SIZE_SMALL;
        } else if (size.equals("Large")) {
            return PointGeocachingData.CACHE_SIZE_LARGE;
        } else if (size.equals("Micro")) {
            return PointGeocachingData.CACHE_SIZE_MICRO;
        } else if (size.equals("Not chosen")) {
            return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
        } else if (size.equals("Other")) {
            return PointGeocachingData.CACHE_SIZE_OTHER;
        } else if (size.equals("Regular")) {
            return PointGeocachingData.CACHE_SIZE_REGULAR;
        } else if (size.equals("Virtual")) {
            return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
        } else {
            return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
        }
    }

    public static int convertCacheType(String type) {
        if (type.equals("Cache In Trash Out Event")) {
            return PointGeocachingData.CACHE_TYPE_CACHE_IN_TRASH_OUT;
        } else if (type.equals("Earthcache")) {
            return PointGeocachingData.CACHE_TYPE_EARTH;
        } else if (type.equals("Event Cache")) {
            return PointGeocachingData.CACHE_TYPE_EVENT;
        } else if (type.equals("Letterbox Hybrid")) {
            return PointGeocachingData.CACHE_TYPE_LETTERBOX;
        } else if (type.equals("Mega-Event Cache")) {
            return PointGeocachingData.CACHE_TYPE_MEGA_EVENT;
        } else if (type.equals("Multi-cache")) {
            return PointGeocachingData.CACHE_TYPE_MULTI;
        } else if (type.equals("Traditional Cache")) {
            return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
        } else if (type.equals("Unknown Cache")) {
            return PointGeocachingData.CACHE_TYPE_MYSTERY;
        } else if (type.equals("Virtual Cache")) {
            return PointGeocachingData.CACHE_TYPE_VIRTUAL;
        } else if (type.equals("Webcam Cache")) {
            return PointGeocachingData.CACHE_TYPE_WEBCAM;
        } else if (type.equals("Wherigo Cache")) {
            return PointGeocachingData.CACHE_TYPE_WHERIGO;
        } else {
            return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
        }
    }

    public static Boolean isAvailable(Integer cacheStatus) {
        if (cacheStatus == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static Boolean isArchived(Integer cacheStatus) {
        if (cacheStatus == 2) {
            return true;
        } else {
            return false;
        }
    }

    public static Boolean isFound(Integer dt) {
        if (dt > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static String convertWaypointType(String waypointType) {
        if (waypointType.equals("Final Location")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_FINAL;
        } else if (waypointType.equals("Parking Area")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_PARKING;
        } else if (waypointType.equals("Question to Answer")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_QUESTION;
        } else if (waypointType.equals("Reference Point")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_REFERENCE;
        } else if (waypointType.equals("Stages of a Multicache")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES;
        } else if (waypointType.equals("Trailhead")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_TRAILHEAD;
        } else {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_REFERENCE;
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
}
