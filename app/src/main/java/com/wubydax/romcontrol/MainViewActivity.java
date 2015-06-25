package com.wubydax.romcontrol;

/*      Created by Roberto Mariani and Anna Berkovitch, 2015
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.software.shell.fab.ActionButton;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;


public class MainViewActivity extends AppCompatActivity
        implements NavigationDrawerCallbacks, View.OnClickListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private Toolbar mToolbar;
    int[] ids;
    ActionButton[] rebootFabs;
    ActionButton reboot, hotboot, recovery, bl, ui;
    View overlay;
    AssetManager am;
    HandleScripts hs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        /*Calling theme selector class to set theme upon start activity*/
        ThemeSelectorUtility theme = new ThemeSelectorUtility(this);
        theme.onActivityCreateSetTheme(this);
        //Getting root privileges upon first boot or if was not yet given su
        CheckSu suPrompt = new CheckSu();
        suPrompt.execute();


        // populate the navigation drawer

    }

    //Creates a list of NavItem objects to retrieve elements for the Navigation Drawer list of choices
    public List<NavItem> getMenu() {
        List<com.wubydax.romcontrol.NavItem> items = new ArrayList<>();
        /*String array of item names is located in strings.xml under name nav_drawer_items
        * If you wish to add more items you need to:
        * 1. Add item to nav_drawer_items array
        * 2. Add a valid material design icon/image to dir drawable
        * 3. Add that image ID to the integer array below (int[] mIcons
        * 4. The POSITION of your new item in the string array MUST CORRESPOND to the position of your image in the integer array mIcons
        * 5. Create new PreferenceFragment or your own fragment or a method that you would like to invoke when a user clicks on your new item
        * 6. Continue down this file to a method onNavigationDrawerItemSelected(int position) - next method
        * 7. Add an action based on position. Remember that positions in array are beginning at 0. So if your item is number 6 in array, it will have a position of 5... etc
        * 8. You need to add same items to the int array in NavigationDrawerFragment, which has the same method*/
        String[] mTitles = getResources().getStringArray(R.array.nav_drawer_items);
        int[] mIcons = {R.drawable.ic_ui_mods,
                R.drawable.ic_phone_mods,
                R.drawable.ic_general_framework,
                R.drawable.ic_apps,
                R.drawable.ic_settings};
        for (int i = 0; i < mTitles.length && i < mIcons.length; i++) {
            com.wubydax.romcontrol.NavItem current = new com.wubydax.romcontrol.NavItem();
            current.setText(mTitles[i]);
            current.setDrawable(mIcons[i]);
            items.add(current);
        }

        return items;
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        /* update the main content by replacing fragments
        * See more detailed instructions on the thread or in annotations to the previous method*/

        setTitle(getMenu().get(position).getText());
        switch (position) {
            case 0:
                getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.container, new UIPrefsFragment()).commitAllowingStateLoss();
                break;
            case 1:
                getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.container, new PhonePrefsFragment()).commitAllowingStateLoss();
                break;
            case 2:
                getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.container, new FrameworksGeneralFragment()).commitAllowingStateLoss();
                break;
            case 3:
                getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.container, new AppLinksFragment()).commitAllowingStateLoss();
                break;
            case 4:
                showThemeChooserDialog();
                break;

        }

    }


    @Override
    public void onBackPressed() {
        if (mNavigationDrawerFragment.isDrawerOpen())
            mNavigationDrawerFragment.closeDrawer();
        else if (overlay.getVisibility() == View.VISIBLE) {
            showHideRebootMenu();
        } else
            super.onBackPressed();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main_view, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.reboot_menu) {
            showHideRebootMenu();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*Handling onClick event for the Reboot Menu (round Action Buttons array)
    * For now we handle them under su, later on, since app is intended to be a system app,
    * we will add PowerManager for items: Reboot, Reboot recovery and Reboot Download*/
    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            /*Handles the onClick event for the semi transparent white overlay
            * Once clicked, we consider it a click outside the Reboot Menu and it invokes methos showHideRebootMenu()*/
            case R.id.overlay:
                showHideRebootMenu();
                break;
            case R.id.action_reboot:
                getRebootAction("reboot");
                break;
            case R.id.action_reboot_hotboot:
                getRebootAction("busybox killall system_server");
                break;
            case R.id.action_reboot_recovery:
                getRebootAction("reboot recovery");
                break;
            case R.id.action_reboot_bl:
                getRebootAction("reboot download");
                break;
            case R.id.action_reboot_systemUI:
                getRebootAction("pkill com.android.systemui");
                break;
        }


    }

    //Gets string for shell command to activate reboot menu items, using stericson RootTools lib
    private void getRebootAction(String command) {
        Command c = new Command(0, command);
        try {
            RootTools.getShell(true).add(c);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }
    }

    //Initializes the reboot menu as arrray of views, finds by id and sets animations and onClickListener to each in a loop
    private void initRebootMenu() {
        ids = new int[]{R.id.action_reboot, R.id.action_reboot_hotboot, R.id.action_reboot_recovery, R.id.action_reboot_bl, R.id.action_reboot_systemUI};
        rebootFabs = new ActionButton[]{reboot, hotboot, recovery, bl, ui};
        overlay = findViewById(R.id.overlay);
        int l = ids.length;
        for (int i = 0; i < l; i++) {
            rebootFabs[i] = (ActionButton) findViewById(ids[i]);
            rebootFabs[i].hide();
            rebootFabs[i].setHideAnimation(ActionButton.Animations.ROLL_TO_RIGHT);
            rebootFabs[i].setShowAnimation(ActionButton.Animations.ROLL_FROM_RIGHT);
        }
    }

    //Show/Hide reboot menu with animation depending on the view's visibility
    public void showHideRebootMenu() {

        for (int i = 0; i < rebootFabs.length; i++) {
            if (rebootFabs[i].isShown()) {
                overlay.setVisibility(View.GONE);
                rebootFabs[i].hide();
            } else {
                overlay.setVisibility(View.VISIBLE);
                rebootFabs[i].show();

            }
        }
    }

    //Activates a chosen theme based on single choice list dialog, which opens upon selecting item at position 4 in nav drawer list
    private void showThemeChooserDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        Adapter adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_single_choice, getResources().getStringArray(R.array.theme_items));
        b.setTitle(getString(R.string.theme_chooser_dialog_title))
                .setSingleChoiceItems((ListAdapter) adapter, PreferenceManager.getDefaultSharedPreferences(this).getInt("theme_prefs", 0), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Invokes method initTheme(int) - next method based on chosen theme
                        initTheme(which);
                    }
                })
        ;
        AlertDialog d = b.create();
        d.show();
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = this.getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);

        Button cancel = d.getButton(AlertDialog.BUTTON_NEGATIVE);
        cancel.setTextColor(typedValue.data);
        Button ok = d.getButton(AlertDialog.BUTTON_POSITIVE);
        ok.setTextColor(typedValue.data);
        d.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        ListView lv = d.getListView();
        int paddingTop = Math.round(this.getResources().getDimension(R.dimen.dialog_listView_top_padding));
        int paddingBottom = Math.round(this.getResources().getDimension(R.dimen.dialog_listView_bottom_padding));
        lv.setPadding(0, paddingTop, 0, paddingBottom);
    }

    /*Writes the chosen position integer (in theme chooser dialog) into common shared preferences.
    * Based on that integer (currently 0 or 1), a helper class ThemeSelectorUtility (which is called at the very beginning of onCreate)
    * then reads that integer when it's instantiated and sets the theme for the activity.
    * The activity is them rebooted, overriding pending transitions, to make the theme switch seemless.*/
    private void initTheme(int i) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("theme_prefs", i).commit();
        finish();
        this.overridePendingTransition(0, R.animator.fadeout);
        startActivity(new Intent(this, MainViewActivity.class));
        this.overridePendingTransition(R.animator.fadein, 0);

    }

    //Asynchronous class to ask for su rights at the beginning of the activity. If the root rights have been denied or the device is not rooted, the app will not run.
    public class CheckSu extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainViewActivity.this);
            mProgressDialog.setMessage(getString(R.string.gaining_root));
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            //Accessing the ability of the device to get root and the ability of app to achieve su privileges.
            if (RootTools.isAccessGiven()) {
                return null;

            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mProgressDialog.dismiss();
            //If the device is not rooted or su has been denied the app will not run.
            //A dialog will be shown announcing that with a single button, upon clicking which the activity will finish.
            if (!RootTools.isAccessGiven()) {
                //If no su access detected, throw and alert dialog with single button that will finish the activity
                AlertDialog.Builder mNoSuBuilder = new AlertDialog.Builder(MainViewActivity.this);
                mNoSuBuilder.setTitle(R.string.missing_su_title);
                mNoSuBuilder.setMessage(R.string.missing_su);
                mNoSuBuilder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                mNoSuBuilder.create();
                Dialog mNoSu = mNoSuBuilder.create();
                mNoSu.show();


            }else{
            //Provided the su privileges have been established, we run the activity as usual, beginning with setting content view
            setContentView(R.layout.activity_main_view);
            mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
            setSupportActionBar(mToolbar);

            mNavigationDrawerFragment = (NavigationDrawerFragment)
                    getFragmentManager().findFragmentById(R.id.fragment_drawer);

            // Set up the drawer. Look in NavigationDrawerFragment for more details
            mNavigationDrawerFragment.setup(R.id.fragment_drawer, (DrawerLayout) findViewById(R.id.drawer), mToolbar, MainViewActivity.this);
            initRebootMenu();
            am = getAssets();
            //Calling the helper class HandleScripts to copy scripts to the files folder and chmod 755.
            //Scripts can be then accessed and executed using script#scriptname key for PreferenceScreen in PreferenceFragments
            hs = new HandleScripts(MainViewActivity.this);
            hs.copyAssetFolder();
            }


        }
    }
}
