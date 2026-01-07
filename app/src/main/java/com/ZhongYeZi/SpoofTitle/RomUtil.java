package com.ZhongYeZi.SpoofTitle;

import android.os.Build;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RomUtil {
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_EMUI_VERSION_CODE = "ro.build.version.emui";
    private static final String KEY_VIVO_OS_VERSION = "ro.vivo.os.version";
    private static final String KEY_OPPO_VERSION = "ro.build.version.opporom";
    private static final String KEY_SMARTISAN_VERSION = "ro.smartisan.version";
    private static final String KEY_DISPLAY_ID = "ro.build.display.id";
    private static final String KEY_ONEPLUS_VERSION = "ro.oxygen.version";

    public static boolean isMiui() {
        return !TextUtils.isEmpty(getSystemProperty(KEY_MIUI_VERSION_NAME));
    }

    public static boolean isOnePlus() {
        return !TextUtils.isEmpty(getSystemProperty(KEY_ONEPLUS_VERSION)) || "OnePlus".equalsIgnoreCase(Build.MANUFACTURER);
    }

    public static boolean isEmui() {
        return !TextUtils.isEmpty(getSystemProperty(KEY_EMUI_VERSION_CODE));
    }

    public static boolean isVivo() {
        return !TextUtils.isEmpty(getSystemProperty(KEY_VIVO_OS_VERSION));
    }

    public static boolean isOppo() {
        return !TextUtils.isEmpty(getSystemProperty(KEY_OPPO_VERSION));
    }

    public static boolean isRealme() {
        String manufacturer = Build.MANUFACTURER;
        return "Realme".equalsIgnoreCase(manufacturer);
    }

    public static boolean isSamsung() {
        String manufacturer = Build.MANUFACTURER;
        return "samsung".equalsIgnoreCase(manufacturer);
    }

    public static boolean isSmartisan() {
        return !TextUtils.isEmpty(getSystemProperty(KEY_SMARTISAN_VERSION));
    }
    
    public static boolean isFlyme() {
         String displayId = getSystemProperty(KEY_DISPLAY_ID);
         return !TextUtils.isEmpty(displayId) && displayId.toLowerCase().contains("flyme");
    }

    private static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }
}
