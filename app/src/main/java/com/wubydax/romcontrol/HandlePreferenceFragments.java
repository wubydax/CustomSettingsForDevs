package com.wubydax.romcontrol;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.TimeoutException;


/*      Created by Roberto Mariani and Anna Berkovitch, 21/06/15
        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
public class HandlePreferenceFragments implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {
    PreferenceFragment pf;
    Context c;
    PreferenceManager pm;
    String spName;
    SharedPreferences prefs;
    SharedPreferences.Editor ed;
    ContentResolver cr;
    ListAdapter adapter;
    boolean isOutOfBounds;


    /*Main constructor, manages what we need to do in the onCreate of each PreferenceFragment. We instantiate
    * this class in the onCreate method of each fragment and setting: shared preference file name (String spName),
    * as well is adding preferences from resource, by using spName in getIdentifier.
    * Basically, the shared preference name and the preference xml file will have the same name.
    * In addition, all the class variables are set here*/
    public HandlePreferenceFragments(Context context, PreferenceFragment pf, String spName) {
        this.pf = pf;
        this.c = context;
        this.spName = spName;
        pm = pf.getPreferenceManager();
        pm.setSharedPreferencesName(spName);
        prefs = pm.getSharedPreferences();
        ed = prefs.edit();
        cr = c.getContentResolver();
        int id = c.getResources().getIdentifier(spName, "xml", c.getPackageName());
        pf.addPreferencesFromResource(id);
    }

    /*Called from onResume method in PreferenceFragment. This method will set all the preferences upon resuming fragment,
    * by integrating the defaultValue (must be set in xml for each "valuable" preference item) and data retrived using
    * ContentResolver from Settings.System. Here we also register the OnSharedPreferenceChangeListener, which we will later
    * unregister in onPauseFragment.
    *
    * OnPreferenceClickListener is also initiated here, so our preferences are ready to go.*/
    public void onResumeFragment() {
        prefs.registerOnSharedPreferenceChangeListener(this);
        initAllKeys();
        getAllPrefs();
    }

    private void getAllPrefs() {
        //Get all preferences in the main preference screen
        adapter = pf.getPreferenceScreen().getRootAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            Preference p = (Preference) adapter.getItem(i);
            if (p instanceof PreferenceScreen) {
                //Call allGroups method to retrieve all preferences in the nested Preference Screens
                allGroups(p);

            }
        }
    }

    public void allGroups(Preference p) {
        PreferenceScreen ps = (PreferenceScreen) p;
        ps.setOnPreferenceClickListener(this);

            /*Initiate icon view for preferences with keys that are interpreted as Intent
            *For more info see OnPreferenceClick method*/
        if (ps.getKey() != null) {
            if (ps.getKey().contains(".")) {
                int lastDot = ps.getKey().lastIndexOf(".");
                String pkgName = ps.getKey().substring(0, lastDot);
                try {
                    //if application package exists, we will set the icon successfully
                    Drawable icon = c.getPackageManager().getApplicationIcon(pkgName);
                    ps.setIcon(icon);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    /*In case of exception, icon will not be set and we will remove the preference to avoid crashes on clicks
                    *To find the parent for each preference screen we use HashMap to buil the parent tree*/
                    Map<Preference, PreferenceScreen> preferenceParentTree = buildPreferenceParentTree();
                    PreferenceScreen preferenceParent = preferenceParentTree.get(ps);
                    preferenceParent.removePreference(ps);

                }
            }
        }

        for (int i = 0; i < ps.getPreferenceCount(); i++) {
            Preference p1 = ps.getPreference(i);
            if (p1 instanceof PreferenceScreen) {
                /*As we descend further on a preference tree, if we meet another PreferenceScreen, we repeat the allGroups method.
                *This method will loop untill we don't have nested screeens anymore.*/
                allGroups(p1);

            }
        }
    }

    //Returns a map of preference tree
    public Map<Preference, PreferenceScreen> buildPreferenceParentTree() {
        final Map<Preference, PreferenceScreen> result = new HashMap<>();
        final Stack<PreferenceScreen> curParents = new Stack<>();
        curParents.add(pf.getPreferenceScreen());
        while (!curParents.isEmpty()) {
            final PreferenceScreen parent = curParents.pop();
            final int childCount = parent.getPreferenceCount();
            for (int i = 0; i < childCount; ++i) {
                final Preference child = parent.getPreference(i);
                result.put(child, parent);
                if (child instanceof PreferenceScreen)
                    curParents.push((PreferenceScreen) child);
            }
        }
        return result;
    }

    /*Main onResume method.
    * Here we create a map of all the keys in existence in each SharedPreference
    * Here: keys are all the keys in preferences
    *       entry.getValue is an object (? in map) for the entry: boolean, int, string and so on
    * We  work through all the entries and sort them by instances of their objects.
    * Knowing that our preferences return different objects in preferences (Checkbox/boolean... etc),
    * we can set specific values and even find specific preferences, as we loop through the map*/

    private void initAllKeys() {
        Map<String, ?> keys = pm.getSharedPreferences().getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {

            String key = entry.getKey();
            Preference p = pf.findPreference(key);

            if (entry.getValue() instanceof Boolean) {
                if (p instanceof FilePreference) {
                } else {
                    int prefInt;
                    int actualInt = 0;
                    boolean actualBoolean;
                    boolean boolValue = prefs.getBoolean(key, true);

                    prefInt = (boolValue) ? 1 : 0;

                    try {
                        actualInt = Settings.System.getInt(cr, key);
                    } catch (Settings.SettingNotFoundException e) {
                        Settings.System.putInt(cr, key, prefInt);
                        actualInt = prefInt;
                    }

                    actualBoolean = (actualInt == 0) ? false : true;
                    if (!String.valueOf(boolValue).equals(String.valueOf(actualBoolean))) {
                        ed.putBoolean(key, actualBoolean).commit();
                    }
                }
            } else if (entry.getValue() instanceof Integer) {
                int prefInt = prefs.getInt(key, 0);
                int actualInt = 0;
                try {
                    actualInt = Settings.System.getInt(cr, key);
                } catch (Settings.SettingNotFoundException e) {
                    Settings.System.putInt(cr, key, prefInt);
                    actualInt = prefInt;
                }
                if (prefInt != actualInt) {
                    ed.putInt(key, actualInt).commit();
                }

            } else if (entry.getValue() instanceof String) {
                String prefString = prefs.getString(key, "");
                String actualString = Settings.System.getString(cr, key);
                String t = (actualString == null) ? prefString : actualString;
                /*Big fix for the annoying and elusive IndexOutOfBoundsException on first install
                * Although the error never came back afterwards, it included copied out of bounds values to db
                * I had to catch exception and set boolean value accordingly to use it later on
                * That implies that on first install the summary for the first screen list preference will not be set
                * After that it will be just fine. No biggie for a great cause of not wiping my device anymore to try and catch the bastard*/
                try {
                    if (p instanceof MyListPreference) {
                        MyListPreference mlp = (MyListPreference) pf.findPreference(key);
                        CharSequence[] entries = mlp.getEntries();
                        //we specifically create string using the index. If it's out of bounds the string will be null
                        //And we have exception on index out of bounds
                        //Boolean isOutOfBounds returns false if "try" succeeded
                        String s = (String) entries[mlp.findIndexOfValue(t)];
                        Log.d("listview index", s);
                        isOutOfBounds = false;
                    }

                } catch (IndexOutOfBoundsException e) {
                    Log.d("listview index", "exception");
                    //boolean isOutOfBounds returns tru if exception was caught
                    isOutOfBounds = true;

                }
                if (p instanceof MyListPreference) {
                    //Any action on the rouge list preference will be performed only if there was no exception
                    if (!isOutOfBounds) {
                        if (actualString == null) {
                            Settings.System.putString(cr, key, prefString);
                        }
                        if (!prefString.equals(t)) {
                            Toast.makeText(c, t + "/" + prefString, Toast.LENGTH_SHORT).show();

                            ed.putString(key, t).commit();
                        }


                        MyListPreference l = (MyListPreference) pf.findPreference(key);
                        CharSequence[] mEntries = l.getEntries();
                        int mValueIndex = l.findIndexOfValue(t);
                        l.setSummary(mEntries[mValueIndex]);
                    }
                }
                if (p instanceof MyEditTextPreference) {
                    if (actualString == null) {
                        Settings.System.putString(cr, key, prefString);
                    }
                    if (!prefString.equals(t)) {
                        Toast.makeText(c, t + "/" + prefString, Toast.LENGTH_SHORT).show();

                        ed.putString(key, t).commit();
                    }
                    MyEditTextPreference et = (MyEditTextPreference) pf.findPreference(key);
                    et.setSummary(t);
                }
            }
        }

    }

    /*Method is called from OnPause of the preference fragment and it's main function is
    *to unregister the OnSharedPreferenceChangeListener*/
    public void onPauseFragment() {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    /*We sort through all the possibilities of changes preferences
    * A key is provided as param for the method so we use it to specify a preference
    * as well as retrieve a value from sharedpreferences or database*/
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference p = pf.findPreference(key);
        switch (p.getClass().getSimpleName()) {
            case "SwitchPreference":
                SwitchPreference s = (SwitchPreference) pf.findPreference(key);
                s.setChecked(sharedPreferences.getBoolean(key, true));
                break;
            case "CheckBoxPreference":
                CheckBoxPreference cbp = (CheckBoxPreference) pf.findPreference(key);
                cbp.setChecked(sharedPreferences.getBoolean(key, true));
                break;
            case "MyListPreference":
                MyListPreference l = (MyListPreference) pf.findPreference(key);
                String lValue = sharedPreferences.getString(key, "");
                //Any action on the rouge list preference will be performed only if there was no exception
                if (!isOutOfBounds) {
                    CharSequence[] mEntries = l.getEntries();
                    int mValueIndex = l.findIndexOfValue(lValue);
                    l.setSummary(mEntries[mValueIndex]);
                    l.setSummary(mEntries[l.findIndexOfValue(lValue)]);
                } else {
                    l.setSummary("");
                }
                break;
            case "MyEditTextPreference":
                MyEditTextPreference et = (MyEditTextPreference) pf.findPreference(key);
                String etValue = sharedPreferences.getString(key, "");
                if (etValue != null) {
                    et.setSummary(sharedPreferences.getString(key, ""));
                }
                break;
            case "ColorPickerPreference":
                ColorPickerPreference cpp = (ColorPickerPreference) pf.findPreference(key);
                cpp.setColor(sharedPreferences.getInt(key, Color.WHITE));
                break;
        }
        /*Calling main method to handle updating database based on preference changes*/
        if (p instanceof FilePreference) {
        } else {
            updateDatabase(key, p, sharedPreferences);
        }
    }

    private void updateDatabase(String key, Object o, SharedPreferences sp) {
        boolean isEnabled;
        int dbInt;
        String value = "";

        if (o instanceof SwitchPreference || o instanceof CheckBoxPreference) {
            isEnabled = sp.getBoolean(key, true);
            dbInt = (isEnabled) ? 1 : 0;
            Settings.System.putInt(cr, key, dbInt);
        } else if (o instanceof MyEditTextPreference || o instanceof MyListPreference || o instanceof IntentDialogPreference) {
            value = sp.getString(key, "");
            Settings.System.putString(cr, key, value);
        } else if (o instanceof ColorPickerPreference) {
            dbInt = sp.getInt(key, Color.WHITE);
            Settings.System.putInt(cr, key, dbInt);
        } else if (o instanceof SeekBarPreference) {
            dbInt = sp.getInt(key, 0);
            Settings.System.putInt(cr, key, dbInt);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey() != null && preference.getKey().contains("script#")) {
            /*We use a special char sequence (script#) to specify preference items that need to run shell script
            * Upon click, the key is broken down to the specifier and what comes after the hash - which is script name
            * Scripts are being copied from assets to the file dir of our app in onCreate of main activity
            * If the script is found on it's intended path, it's checked for being executable.
            * Although we chmod 755 all the files upon copying them in main activity,
            * We need to make sure, so we check and set it executable if it's not
            * Permission 700 (set by this method (setExecutable(true)) is sufficient for executing scripts)*/
            String scriptName = preference.getKey().substring(preference.getKey().lastIndexOf("#") + 1) + ".sh";
            String pathToScript = c.getFilesDir() + File.separator + "scripts" + File.separator + scriptName;
            File script = new File(pathToScript);
            if (script.exists()) {
                boolean isChmoded = script.canExecute() ? true : false;
                if (!isChmoded) {
                    script.setExecutable(true);
                }
                Command command = new Command(0, pathToScript) {
                    @Override
                    public void commandCompleted(int id, int exitcode) {
                        super.commandCompleted(id, exitcode);
                        if (exitcode != 0) {
                            Toast.makeText(c, String.valueOf(exitcode), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(c, "Executed Successfully", Toast.LENGTH_SHORT).show();

                        }
                    }
                };
                try {
                    RootTools.getShell(true).add(command);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } catch (RootDeniedException e) {
                    e.printStackTrace();
                }
            }
        /*If preference key contains a dot ".", we assume the dev meant to create an intent to another app
        * As per instructions, devs are required to enter full path to the main activity they wish to open in the intended app.
        * In the following condition the key is broken down to package name and class name (full key)
        * and we attempt to build intent.
        * We know from the allGroups() method that if the intent is not valid, the preference will not show at all.
        * Nevertheless. as precaution we catch an exception and show a toast that the app is not installed.*/
        } else if (preference.getKey() != null && preference.getKey().contains(".")) {
            String cls = preference.getKey();
            String pkg = cls.substring(0, cls.lastIndexOf("."));
            Intent intent = new Intent(Intent.ACTION_MAIN).setClassName(pkg,
                    cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setComponent(new ComponentName(pkg,
                            cls));
            try {
                c.startActivity(intent);
            } catch (ActivityNotFoundException anf) {
                Toast.makeText(c, "App not installed or intent not valid", Toast.LENGTH_SHORT).show();
            }

        } else if (preference.getKey() == null) {
//            setToolbarForNested(preference);
        }
        return true;
    }

//    private void setToolbarForNested(Preference p) {
//        PreferenceScreen ps = (PreferenceScreen) p;
//        Dialog d = ps.getDialog();
//        android.support.v7.widget.Toolbar tb;
//        LinearLayout ll = (LinearLayout) d.findViewById(android.R.id.list).getParent();
//        tb = (android.support.v7.widget.Toolbar) LayoutInflater.from(c).inflate(R.layout.toolbar_default, ll, false);
//        ll.addView(tb, 0);
//
//    }

}
