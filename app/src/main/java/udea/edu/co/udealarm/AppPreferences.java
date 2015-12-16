package udea.edu.co.udealarm;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by Felipe on 16/12/15.
 */
public class AppPreferences extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
