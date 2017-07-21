package com.example.kasparasza.newsapp;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    // Fragment that loads settings_main xml resource layout
    // the layout contains menu items and their customisation settings
    public static class NewsAppPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener{
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // inflate the fragment with a layout resource
            addPreferencesFromResource(R.xml.settings_main);

            // we update the preference summary when the settings activity is launched
            // similar calls are necessary for each preference
            Preference fromDate = findPreference(getString(R.string.settings_from_date_key));
            bindPreferenceSummaryToValue(fromDate);

            Preference orderBy = findPreference(getString(R.string.settings_order_by_key));
            bindPreferenceSummaryToValue(orderBy);

            Preference productionOffice = findPreference(getString(R.string.settings_production_office_key));
            bindPreferenceSummaryToValue(productionOffice);
        }

        /*
        * method required to be implemented by OnPreferenceChangeListener;
        * method takes care of updating the displayed preference summary after it has been changed
        */
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            // a modification in implementation may be required depending on the type of preference (ListPreference, EditTextPreference, etc)
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(stringValue);
                if (prefIndex >= 0) {
                    CharSequence[] labels = listPreference.getEntries();
                    preference.setSummary(labels[prefIndex]);
                }
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }

        /*
        * helper method that sets the current NewsAppPreferenceFragment instance as the listener on each preference;
        * reads the current value of the preference stored in the SharedPreferences, and displays it in the preference summary
        */
        private void bindPreferenceSummaryToValue(Preference preference) {
            preference.setOnPreferenceChangeListener(this);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            String preferenceString = preferences.getString(preference.getKey(), "");
            onPreferenceChange(preference, preferenceString);
        }
    }
}
