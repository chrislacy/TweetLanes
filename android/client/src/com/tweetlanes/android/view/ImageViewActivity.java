/*
 * Copyright (C) 2013 Chris Lacy Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.tweetlanes.android.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crittercism.app.Crittercism;
import com.tweetlanes.android.App;
import com.tweetlanes.android.Constant;
import com.tweetlanes.android.R;
import com.tweetlanes.android.util.Util;
import com.tweetlanes.android.widget.gestureimageview.GestureImageView;
import com.tweetlanes.android.widget.urlimageviewhelper.UrlImageViewCallback;
import com.tweetlanes.android.widget.urlimageviewhelper.UrlImageViewHelper;

public class ImageViewActivity extends FragmentActivity {

    GestureImageView mZoomableImageView;

    static final String KEY_MEDIA_URL = "mediaUrl";
    static final String KEY_SOURCE_URL = "sourceUrl";
    static final String KEY_AUTHOR_SCREEN_NAME = "authorScreenName";

    /*
	 * 
	 */
    public static void createAndStartActivity(Activity currentActivity,
            String mediaUrl, String sourceUrl, String authorScreenName) {
        Intent intent = new Intent(App.getContext(), ImageViewActivity.class);
        intent.putExtra(KEY_MEDIA_URL, mediaUrl);
        intent.putExtra(KEY_SOURCE_URL, sourceUrl);
        intent.putExtra(KEY_AUTHOR_SCREEN_NAME, authorScreenName);
        currentActivity.startActivity(intent);
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constant.ENABLE_CRASH_TRACKING) {
            Crittercism.init(getApplicationContext(),
                    Constant.CRITTERCISM_APP_ID);
        }

        String imageUrl = getMediaUrl();
        if (imageUrl == null) {
            finish();
            return;
        }

        setContentView(R.layout.image_view);

        mZoomableImageView = (GestureImageView) findViewById(R.id.image_view);
        UrlImageViewHelper.setUrlDrawable(mZoomableImageView, imageUrl,
                new UrlImageViewCallback() {
					
					@Override
					public void onLoaded(ImageView imageView, Drawable loadedDrawable,
							String url, boolean loadedFromCache) {
						ProgressBar loadingView = (ProgressBar) findViewById(R.id.imageViewLoading);
                        loadingView.setVisibility(View.GONE);
                        if (loadedDrawable == null) {
                            TextView errorTextView = (TextView) findViewById(R.id.errorTextView);
                            errorTextView.setVisibility(View.VISIBLE);
                        }
						
					}
				}); 

        getActionBar().setTitle("@" + getAuthorScreenName() + "'s image");
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.image_view_action_bar, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (super.onOptionsItemSelected(item) == true) {
            return true;
        }

        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;

        case R.id.action_website: {
            String sourceUrl = getSourceUrl();
            if (sourceUrl != null) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(sourceUrl));
                startActivity(browserIntent);
                return true;
            }
            break;
        }

        case R.id.action_save: {
            String mediaUrl = getMediaUrl();
            if (mediaUrl != null) {
                String toastMessage = getString(R.string.image_not_loaded);

                String existingFilename = UrlImageViewHelper
                        .getFilenameForUrl(mediaUrl);
                if (existingFilename != null) {
                    File existingFile = getFileStreamPath(existingFilename);
                    if (existingFile.exists()) {
                        toastMessage = getString(R.string.unknown_error);
                        try {
                            String timeStamp = new SimpleDateFormat(
                                    "yyyyMMdd_HHmmss").format(new Date());
                            String newFileName = Constant.JPEG_FILE_PREFIX
                                    + timeStamp + "_";

                            File albumFile = Util.getAlbumDir("Tweet Lanes");
                            File newFile = File.createTempFile(newFileName,
                                    Constant.JPEG_FILE_SUFFIX, albumFile);

                            String existingPath = existingFile
                                    .getAbsolutePath();
                            String newPath = newFile.getAbsolutePath();

                            InputStream in = new FileInputStream(existingPath);
                            OutputStream out = new FileOutputStream(newPath);
                            Util.copyFile(in, out);
                            in.close();
                            in = null;
                            out.flush();
                            out.close();
                            out = null;

                            MediaStore.Images.Media.insertImage(
                                    getContentResolver(), newPath, "Image",
                                    null);
                            // MediaScannerConnection.scanFile(context, new
                            // String[]{newPath}, null, null);
                            toastMessage = getString(R.string.image_save_success);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (getApplicationContext() != null) {
                        Toast.makeText(getApplicationContext(), toastMessage,
                                Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
                    }
                }
                return true;
            }
            break;
        }

        default:
            return false;
        }

        return false;
    }

    /*
	 * 
	 */
    String getMediaUrl() {
        String mediaUrl = getIntent().getStringExtra(KEY_MEDIA_URL);
        if (mediaUrl != null) {
            return mediaUrl;
        }

        final String prefix = "com.tweetlanes.android.mediaview://";

        String dataString = getIntent().getDataString();
        if (dataString != null && dataString.contains(prefix)) {
            return dataString.replace(prefix, "");
        }

        return null;
    }

    String getSourceUrl() {
        return getIntent().getStringExtra(KEY_SOURCE_URL);
    }

    String getAuthorScreenName() {
        return getIntent().getStringExtra(KEY_AUTHOR_SCREEN_NAME);
    }

}
