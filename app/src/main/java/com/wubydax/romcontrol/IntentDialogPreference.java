package com.wubydax.romcontrol;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/*      Created by Roberto Mariani and Anna Berkovitch, 27/06/15
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
public class IntentDialogPreference extends DialogPreference implements AdapterView.OnItemClickListener {
    String mValue, mSummary;
    Drawable appIcon;
    String separator = "##";
    Context c;
    PackageManager pm;
    ApplicationInfo ai;
    ListView lv;
    ImageView prefAppIcon;
    EditText search;
    ProgressBar pb;
    AppListAdapter appListAdapter;
    List<ApplicationInfo> mAppList;
    AsyncTask<Void, Void, Void> loadApps;

    public IntentDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.c = context;
        pm = c.getPackageManager();
        setDialogLayoutResource(R.layout.intent_dialog_layout);
        setWidgetLayoutResource(R.layout.intent_preference_app_icon);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        getAppIcon();
        prefAppIcon = (ImageView) view.findViewById(R.id.iconForApp);
        prefAppIcon.setImageDrawable(appIcon);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        lv = (ListView) view.findViewById(R.id.appsList);
        lv.setOnItemClickListener(this);
        lv.setFastScrollEnabled(true);
        lv.setFadingEdgeLength(1);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        lv.setScrollingCacheEnabled(false);
        search = (EditText) view.findViewById(R.id.searchApp);
        pb = (ProgressBar) view.findViewById(R.id.progressBar);
        createList();
        search.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                appListAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (loadApps != null && loadApps.getStatus() == AsyncTask.Status.RUNNING) {
            loadApps.cancel(true);
            loadApps = null;
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            String value = getPersistedString(null);
            String appName = getAppName(value);
            setSummary(appName == null ? mSummary : appName);
        } else {
            persistString(null);
            setSummary(mSummary);
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        AlertDialog dialog = (AlertDialog) getDialog();
        dialog.show();
        Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        ok.setVisibility(View.GONE);
        cancel.setTextColor(typedValue.data);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
    }

    @Override
    public void onParentChanged(Preference parent, boolean disableChild) {
        super.onParentChanged(parent, disableChild);
    }

    public void setDefaultSummary(String summary) {
        mSummary = summary;
    }

    private String getAppName(String value) {
        String[] split = value.split(separator);
        String pkgName = split[0];
        String appName = null;
        try {
            appName = pm.getApplicationInfo(pkgName, 0).loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return appName;
    }

    private List<ApplicationInfo> createAppList() {
        ArrayList<ApplicationInfo> appList = new ArrayList<ApplicationInfo>();
        List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        int l = list.size();

        for (int i=0; i<l; i++) {
            try {
                if (pm.getLaunchIntentForPackage(list.get(i).packageName) != null) {
                    appList.add(list.get(i));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return appList;

    }

    public Drawable getAppIcon() {
        String intent = getPersistedString(null);
        if (intent != null) {
            String[] splitValue = intent.split(separator);
            String pkg = splitValue[0];
            try {
                appIcon = pm.getApplicationIcon(pkg);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            appIcon = c.getResources().getDrawable(R.drawable.ic_apps);
        }
        return appIcon;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ApplicationInfo appInfo = (ApplicationInfo) parent.getItemAtPosition(position);
        Intent intent = pm.getLaunchIntentForPackage(appInfo.packageName);
        ResolveInfo ri = pm.resolveActivity(intent, 0);
        String launchableActivity = ri.activityInfo.name;
        String intentString = String.format("%1$s%2$s%3$s", appInfo.packageName, separator, launchableActivity);
        setSummary(intentString == null ? mSummary : appInfo.loadLabel(pm));
        persistString(intentString);
        mValue = intentString;
        appIcon = appInfo.loadIcon(pm);
        getDialog().dismiss();

    }


    private void createList() {
        loadApps = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                pb.setVisibility(View.VISIBLE);
                pb.refreshDrawableState();
            }

            @Override
            protected Void doInBackground(Void... params) {
                mAppList = createAppList();
                Collections.sort(mAppList, new Comparator<ApplicationInfo>() {

                    @Override
                    public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                        return String.CASE_INSENSITIVE_ORDER.compare(lhs.loadLabel(pm).toString(), rhs.loadLabel(pm).toString());
                    }
                });
                appListAdapter = new AppListAdapter(mAppList);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                pb.setVisibility(View.GONE);
                lv.setAdapter(appListAdapter);
            }
        }.execute();
    }

    private class AppListAdapter extends BaseAdapter implements SectionIndexer, Filterable {

        List<ApplicationInfo> mAppList, filteredList;
        private HashMap<String, Integer> alphaIndexer;
        private String[] sections;

        public AppListAdapter(List<ApplicationInfo> appList) {

            this.mAppList = appList;
            filteredList = mAppList;
            //adding Indexer to display the first letter of an app while using fast scroll
            alphaIndexer = new HashMap<String, Integer>();
            for (int i = 0; i < filteredList.size(); i++) {
                String s = filteredList.get(i).loadLabel(pm).toString();
                String s1 = s.substring(0, 1).toUpperCase();
                if (!alphaIndexer.containsKey(s1))
                    alphaIndexer.put(s1, i);
            }

            Set<String> sectionLetters = alphaIndexer.keySet();
            ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);
            Collections.sort(sectionList);
            sections = new String[sectionList.size()];
            for (int i = 0; i < sectionList.size(); i++)
                sections[i] = sectionList.get(i);

        }

        @Override
        public Object[] getSections() {
            return sections;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return alphaIndexer.get(sections[sectionIndex]);
        }

        @Override
        public int getSectionForPosition(int position) {
            for (int i = sections.length - 1; i >= 0; i--) {
                if (position >= alphaIndexer.get(sections[i])) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults fr = new FilterResults();
                    ArrayList<ApplicationInfo> ai = new ArrayList<>();

                    for (int i = 0; i < mAppList.size(); i++) {
                        String label = mAppList.get(i).loadLabel(pm).toString();
                        if (label.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            ai.add(mAppList.get(i));
                        }
                    }

                    fr.count = ai.size();
                    fr.values = ai;

                    return fr;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredList = (List<ApplicationInfo>) results.values;
                    notifyDataSetChanged();
                }
            };
            return filter;
        }

        public class ViewHolder {
            public TextView mAppNames;
            public TextView mAppPackage;
            public ImageView mAppIcon;
        }


        @Override
        public int getCount() {
            if (filteredList != null) {
                return filteredList.size();
            }
            return 0;
        }

        @Override
        public ApplicationInfo getItem(int position) {
            if (filteredList != null) {
                return filteredList.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(c);
                convertView = inflater.inflate(R.layout.app_item, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.mAppNames = (TextView) convertView.findViewById(R.id.appName);
                viewHolder.mAppPackage = (TextView) convertView.findViewById(R.id.appPackage);
                viewHolder.mAppIcon = (ImageView) convertView.findViewById(R.id.appIcon);
                convertView.setTag(viewHolder);
            }
            final ViewHolder holder = (ViewHolder) convertView.getTag();
            final ApplicationInfo applicationInfo = filteredList.get(position);

            holder.mAppNames.setText(applicationInfo.loadLabel(pm));
            holder.mAppPackage.setText(applicationInfo.packageName);
            holder.mAppIcon.setImageDrawable(applicationInfo.loadIcon(pm));

            return convertView;
        }
    }
}
