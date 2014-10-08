package com.tweetlanes.android.core.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.crittercism.app.Crittercism;
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.ConsumerKeyConstants;
import com.tweetlanes.android.core.R;

/**
 * Created with IntelliJ IDEA.
 * User: Jason
 * Date: 3/4/13
 * Time: 8:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewAccountActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constant.ENABLE_CRASH_TRACKING) {
            Crittercism.initialize(getApplicationContext(), ConsumerKeyConstants.CRITTERCISM_APP_ID);
        }

        if (Constant.ENABLE_APP_DOT_NET) {
            setTheme(AppSettings.get().getCurrentThemeStyle());
            setContentView(R.layout.new_account);
        } else {
            Intent intent = new Intent(getApplicationContext(), TwitterAuthActivity.class);
            overridePendingTransition(0, 0);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }
    }

    public void accountClick(View view) {
        finish();

        Class c = view.getId() == R.id.new_account_twitter_button ? TwitterAuthActivity.class : AppDotNetAuthActivity
                .class;

        Intent intent = new Intent(getApplicationContext(), c);
        overridePendingTransition(0, 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }
}
