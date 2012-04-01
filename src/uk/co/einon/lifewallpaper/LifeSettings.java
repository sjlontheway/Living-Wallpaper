/*
 * LifeSettings.java
 * 
 * Copyright 2012 Mark Einon <mark.einon@gmail.com>
 *
 * This file is part of LivingWallpaper.
 *
 * LivingWalpaper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LivingWallpaper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LivingWallpaper. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.co.einon.lifewallpaper;

import com.google.ads.*;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import uk.co.einon.lifewallpaper.R;

public class LifeSettings extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle icicle) {
    	
        super.onCreate(icicle);
        getPreferenceManager().setSharedPreferencesName(
                Life.SHARED_PREFS_NAME);
        setContentView(R.layout.main);
        
        addPreferencesFromResource(R.xml.life_settings);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                this);
        
        // sets the background to opaque
        getWindow().setBackgroundDrawableResource(android.R.drawable.editbox_background_normal);
        
        AdView adView = (AdView)this.findViewById(R.id.adView);
        AdRequest request = new AdRequest();           
        adView.loadAd(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        super.onDestroy();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
    }
}
