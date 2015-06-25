package com.wubydax.romcontrol;

/*      Created by Roberto Mariani and Anna Berkovitch, 05/06/15
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


import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;


public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.ViewHolder> {

    private List<NavItem> mData;
    private NavigationDrawerCallbacks mNavigationDrawerCallbacks;
    private View mSelectedView;
    private int mSelectedPosition;
    private Context context;

    public NavigationDrawerAdapter(List<NavItem> data, Context c) {
        mData = data;
        context = c;
    }


    public void setNavigationDrawerCallbacks(NavigationDrawerCallbacks navigationDrawerCallbacks) {
        mNavigationDrawerCallbacks = navigationDrawerCallbacks;
    }
    //Setting view for a Holder for single item in Nav Drawer RecyclerView
    @Override
    public NavigationDrawerAdapter.ViewHolder onCreateViewHolder(final ViewGroup viewGroup, int i) {
        //Inflating layout for single row view
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.drawer_row, viewGroup, false);
        final ViewHolder viewHolder = new ViewHolder(v);
        viewHolder.itemView.setClickable(true);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                                   @Override
                                                   public void onClick(View v) {
                                                       if (mSelectedView != null) {
                                                           mSelectedView.setSelected(false);


                                                       }
                                                       mSelectedPosition = viewHolder.getPosition();
                                                       v.setSelected(true);
                                                       mSelectedView = v;
                                                        //Creating communication between the adapter and the fragment and the activity, using callback interface
                                                       //Otherwise we cannot access the onItemClickListener from the activity
                                                       if (mNavigationDrawerCallbacks != null)
                                                           mNavigationDrawerCallbacks.onNavigationDrawerItemSelected(viewHolder.getPosition());
                                                   }
                                               }
        );
        //Creating a choice for the overlay colors for the selected menu item, using attr, which differentiates between different themes and colors in them
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.nav_item_color_selector, typedValue, true);
        XmlResourceParser xrp = viewGroup.getContext().getResources().getXml(typedValue.resourceId);
        try {
            ColorStateList csl = ColorStateList.createFromXml(viewGroup.getContext().getResources(), xrp);
            viewHolder.textView.setTextColor(csl);

        } catch (Exception e) {  }

        return viewHolder;
    }
    //Binding the Holder to each item based on position in a list of NavItem class objects
    @Override
    public void onBindViewHolder(NavigationDrawerAdapter.ViewHolder viewHolder, int i) {
        viewHolder.textView.setText(mData.get(i).getText());
        viewHolder.icon.setImageResource((mData.get(i).getIcon()));
        if (mSelectedPosition == i) {
            if (mSelectedView != null) {
                mSelectedView.setSelected(false);
            }
            mSelectedPosition = i;
            mSelectedView = viewHolder.itemView;
            mSelectedView.setSelected(true);

        }
    }


    public void selectPosition(int position) {
        mSelectedPosition = position;
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

    //ViewHolder class finds each view in a row by its id in the row layout
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public ImageView icon;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.item_name);
            icon = (NavDrawerIcon) itemView.findViewById(R.id.nav_item_icon);
        }
    }
}