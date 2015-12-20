/*
 * Author: Balch
 * Created: 9/4/14 12:26 AM
 *
 * This file is part of MockTrade.
 *
 * MockTrade is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MockTrade is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MockTrade.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2014
 */

package com.balch.android.app.framework;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.balch.android.app.framework.nav.NavBar;

public abstract class NavBarActivity extends AppCompatActivity implements ActivityProvider {
    protected NavBar navBar;
    protected LinearLayout rootLayout;
    protected FrameLayout frameLayout;
    protected Menu optionsMenu;

    abstract protected void configureNavBar(NavBar navBar, Bundle savedInstanceState);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);

        setContentView(R.layout.template_view);
        this.navBar = (NavBar)findViewById(R.id.nav_bar_main);
        this.rootLayout = (LinearLayout)findViewById(R.id.template_layout);
        this.frameLayout = (FrameLayout)findViewById(R.id.template_place_holder);

        configureNavBar(this.navBar, savedInstanceState);

        if (savedInstanceState == null) {
        }

        final FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                int backStackCount = fragmentManager.getBackStackEntryCount();
                if (backStackCount > 0) {
                    int selectedIndex = Integer.parseInt(fragmentManager.getBackStackEntryAt(backStackCount - 1).getName());
                    if (selectedIndex != NavBarActivity.this.navBar.getSelectedIndex()) {
                        NavBarActivity.this.navBar.setSelectedIndex(selectedIndex);
                    }
                } else {
                    // required b/c the first stack frame has an empty fragment
                    finish();
                }
            }
        });

        this.navBar.completeConfiguration();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_bar_menu_refresh) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.template_place_holder);

            if (fragment instanceof Refreshable) {
                ((Refreshable) fragment).refresh();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void showRefreshMenuOption(boolean show) {
        if (this.optionsMenu != null) {
            this.optionsMenu.findItem(R.id.action_bar_menu_refresh).setVisible(show);
        }
    }

    @Override
    public void showProgress() {
        if (this.optionsMenu != null) {
            MenuItem menuItem = this.optionsMenu.findItem(R.id.action_bar_menu_refresh);
            menuItem.setActionView(R.layout.action_bar_progress);
            menuItem.expandActionView();
        }
    }

    @Override
    public void hideProgress() {
        if (this.optionsMenu != null) {
            MenuItem menuItem = this.optionsMenu.findItem(R.id.action_bar_menu_refresh);
            menuItem.collapseActionView();
            menuItem.setActionView(null);        }
    }

    @Override
    public void showView(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.template_place_holder, fragment)
                .addToBackStack(String.valueOf(this.navBar.getSelectedIndex()))
                .commit();

        this.showRefreshMenuOption((fragment instanceof Refreshable) && ((Refreshable) fragment).showRefreshMenu());
    }

    @Override
    public FragmentManager getFragManager() {
        return getSupportFragmentManager();
    }

    public void setBackground(Drawable background) {
        this.rootLayout.setBackground(background);
        this.frameLayout.setBackground(background);
    }


}
