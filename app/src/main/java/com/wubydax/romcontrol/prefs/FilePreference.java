package com.wubydax.romcontrol.prefs;

import android.content.Context;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.wubydax.romcontrol.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/*      Created by Roberto Mariani and Anna Berkovitch, 29/06/15
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
public class FilePreference extends SwitchPreference implements CompoundButton.OnCheckedChangeListener, Preference.OnPreferenceClickListener {
    String key;
    String defaultNameSpace;
    CharSequence summaryOn, summaryOff;
    File file;
    boolean isOn;
    Context c;
    Switch swView;

    public FilePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        c = context;
        setWidgetLayoutResource(R.layout.file_preference_widget);
        defaultNameSpace = "http://schemas.android.com/apk/res/android";
        key = getStringForAttr(attrs, defaultNameSpace, "key", "file");
        summaryOn = getStringForAttr(attrs, defaultNameSpace, "summaryOn", "");
        summaryOff = getStringForAttr(attrs, defaultNameSpace, "summaryOff", "");
        file = new File(c.getFilesDir() + File.separator + key);
        isOn = file.exists() ? true : false;
        FilePreference.this.setOnPreferenceClickListener(this);
    }

    private String getStringForAttr(AttributeSet attrs, String ns, String attrName, String defaultValue) {
        String value = attrs.getAttributeValue(ns, attrName);
        if (value == null)
            value = defaultValue;
        return value;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        swView = (Switch) view.findViewById(R.id.fileSwitch);
        swView.setChecked(isOn);
        String isOnValue = String.valueOf(isOn);
        String persistedBoolValue = String.valueOf(getPersistedBoolean(false));
        if (!isOnValue.equals(persistedBoolValue)) {
            persistBoolean(isOn);
        }
        syncSummaryView(view);
        swView.setOnCheckedChangeListener(this);
    }

    void syncSummaryView(View view) {
        // Sync the summary view
        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
        if (summaryView != null) {
            boolean useDefaultSummary = true;
            if (isOn && !TextUtils.isEmpty(summaryOn)) {
                summaryView.setText(summaryOn);
                useDefaultSummary = false;
            } else if (!isOn && !TextUtils.isEmpty(summaryOff)) {
                summaryView.setText(summaryOff);
                useDefaultSummary = false;
            }
            if (useDefaultSummary) {
                final CharSequence summary = getSummary();
                if (!TextUtils.isEmpty(summary)) {
                    summaryView.setText(summary);
                    useDefaultSummary = false;
                }
            }
            int newVisibility = View.GONE;
            if (!useDefaultSummary) {
                // Someone has written to it
                newVisibility = View.VISIBLE;
            }
            if (newVisibility != summaryView.getVisibility()) {
                summaryView.setVisibility(newVisibility);
            }
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        persistBoolean(isChecked);
        notifyChanged();
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (swView.isChecked()) {
            file.delete();
            isOn = false;

        } else {
            try {
                file.createNewFile();
                BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(file), 16 * 1024);
                fout.close();
                isOn = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        swView.setChecked(isOn);
        return true;
    }
}
