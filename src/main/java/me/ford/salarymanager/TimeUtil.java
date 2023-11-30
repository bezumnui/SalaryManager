package me.ford.salarymanager;

import org.bukkit.configuration.ConfigurationSection;

public final class TimeUtil {
    private TimeUtil() {
    }

    static String s = "s";
    static String m = "m";
    static String h = "h";
    static String d = "d";
    static String mo = "mo";
    static String y = "y";

    public static void init() {
        ConfigurationSection section = Messages.getConfig().getConfigurationSection("time");
        if (section != null) {
            if (section.contains("seconds"))
                s = section.getString("seconds");
            if (section.contains("minutes"))
                m = section.getString("minutes");
            if (section.contains("hours"))
                h = section.getString("hours");
            if (section.contains("days"))
                d = section.getString("days");
            if (section.contains("months"))
                mo = section.getString("months");
            if (section.contains("years"))
                y = section.getString("years");
        }
    }

    public static String timeParser(long timeleft) {
        String time = "";
        if (timeleft > 60 * 60 * 24 * 365) {
            time += "" + timeleft / (60 * 60 * 24 * 365) + TimeUtil.y;
            timeleft = timeleft % (60 * 60 * 24 * 365);
        }
        if (timeleft > 60 * 60 * 24 * 30) {
            time += "" + timeleft / (60 * 60 * 24 * 30) + TimeUtil.mo;
            timeleft = timeleft % (60 * 60 * 24 * 30);
        }
        if (timeleft > 60 * 60 * 24) {
            time += "" + timeleft / (60 * 60 * 24) + TimeUtil.d;
            timeleft = timeleft % (60 * 60 * 24);
        }
        if (timeleft > 3600) {
            time += "" + timeleft / 3600 + TimeUtil.h;
            timeleft = timeleft % 3600;
        }
        if (timeleft > 60) {
            time += timeleft / 60 + TimeUtil.m;
            timeleft = timeleft % 60;
        }
        time += timeleft + TimeUtil.s;
        return time;
    }

}
