# Tweet Lanes

Tweet Lanes is a fully functional Twitter client, targeted at Android devices running [API 14](http://developer.android.com/about/versions/android-4.0.html) and above. 

![Tweet Lanes](https://s3.amazonaws.com/tweetlanes/tweetlanes_github_promo_shot.png)

![https://play.google.com/store/apps/details?id=com.tweetlanes.android](https://developer.android.com/images/brand/en_app_rgb_wo_60.png)

This project contains the full source code to:

* The Tweet Lanes client application.
* The accompanying SocialNetLib project, which interfaces with the Twitter and App.net APIs.
* All art resources, notably the logo and Play Store listings.
* The App Engine project used for tweetlanes.com.

The only items used in the Play Store build of Tweet Lanes _not_ present in this repository are:

* The Twitter Application consumer and secret keys.
* The Play Store key used to sign the application.



##### Developed By
* Chris Lacy - [@chrismlacy](http://twitter.com/chrismlacy), [+Chris Lacy](https://plus.google.com/104649936579980037256/posts), <lacy@tweetlanes.com>

##### Credits
Tweet Lanes uses the following open-source software:

* [Android-PullToRefresh](https://github.com/chrisbanes/Android-PullToRefresh), Copyright 2013 Chris Banes.
* [AOSP](http://source.android.com/), Copyright 2008-2013 Android Open Source Project.
* [GestureImageView](https://github.com/jasonpolites/gesture-imageview), Copyright 2012 Jason Polites.
* [Twidere](https://github.com/mariotaku/twidere), Copyright (C) 2012 Mariotaku Lee.
* [Prime](https://github.com/DHuckaby/Prime), Copyright (C) 2012 Daniel Huckaby.
* [UrlImageViewHelper](https://github.com/koush/UrlImageViewHelper), Copyright 2013 Koushik Dutta.
* [ViewPagerIndicator](https://github.com/JakeWharton/Android-ViewPagerIndicator), Copyright 2012 Jake Wharton.

### Disclaimer

A few points to considering when browsing this code:

1. *Tweet Lanes was the first app I wrote in Java, as well as my first Android app.* It is both my Android and Java 'Hello World'. The project contains much code that causes me to cringe when looking at it now, but it exists because I simply didn't know better at the time of writing it. 
2. *Tweet Lanes was developed on the fly*. I often added a feature and shipped it that night. If that new code was buggy, I sometimes added a quick and dirty fix to get things going again. The intent was often to go back and clean such code up, but due to the sheer amount of work I had to do, that rarely ended up happening.

In conclusion: there is currently some dog-awful code in this project. I know this only too well. Please try not to judge my coding ability on the worst parts, but do feel free to fork 'n fix :)


### Usage

`\android\client` contains the bulk of the code. This project is dependent on the following libraries:

* `\android\libraries\SocialNetLib` - An abstracted library that interfaces with Twitter and App.net.
* `\android\libraries\SupportLibraryv4` - A slightly modified copy of the [Android Support Library](http://developer.android.com/tools/extras/support-library.html).

Additional notes: 

* If building the project for yourself, I would suggest [creating a new Twitter application](https://dev.twitter.com/apps). Be sure to update `CONSUMER_KEY` and `CONSUMER_SECRET` with your new values.
* The project is configured to use Twitter by default. If you want to use App.net, just set `SOCIAL_NET_TYPE` to `SocialNetConstant.Type.Appdotnet`, and update `CONSUMER_KEY` and `CONSUMER_SECRET` accordingly.
* Configuration files for Eclipse are provided. If using Eclipse/ADT, you should be able to import the client and libraries straight in. 

### License

Tweet Lanes is dual licensed. 

For non-commercial uses: Apache License, Version 2.0.

For commercial uses: You must buy a license for a very nominal fee. 

The following copyright notice is used in all files:

```
/*
 * Copyright (C) 2013 Chris Lacy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```