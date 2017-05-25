package com.hocheol.pushmanager.websocket;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PrefUtil {

    private final SharedPreferences.Editor editor;
    private final SharedPreferences pref;
    private final String PREF_NAME = "";

    public PrefUtil(final Context context) {
        pref = context.getSharedPreferences(PREF_NAME+context.getPackageName().replace(".", ""), Context.MODE_WORLD_READABLE);
        editor = pref.edit();
    }

    public void removePref(String keydata) {
        try {
            editor.remove(keydata);
            editor.commit();
        } catch (Exception e) {
            Log.e("PrefUtil", e.getMessage());
        }
    }

    public void removeAllPref(){
        editor.clear();
        editor.commit();
    }

    public String getPrefData(final String keydata, final String data) {
        return pref.getString(keydata,data);
    }

    public boolean getPrefData(final String keydata, final boolean data) {
        return pref.getBoolean(keydata, data);
    }

    public void setPrefData(final String keydata, final String data) {
        editor.putString(keydata, data);
        editor.commit();
    }

    public void setPrefData(final String keydata, final boolean data) {
        editor.putBoolean(keydata, data);
        editor.commit();
    }

}