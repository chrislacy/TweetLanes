# Tweet Lanes

Tweet Lanes is a fully functional Twitter client, targeted at Android devices running [API 14](http://developer.android.com/about/versions/android-4.0.html) and above. 

![Tweet Lanes](https://s3.amazonaws.com/tweetlanes/tweetlanes_github_promo_shot.png)

[![Tweet Lanes on the Google Play Store](https://developer.android.com/images/brand/en_app_rgb_wo_60.png)](https://play.google.com/store/apps/details?id=com.tweetlanes.android)


This project contains the full source code to:

* The Tweet Lanes client application.
* The accompanying SocialNetLib project, which interfaces with the Twitter and App.net APIs.
* All art resources, notably the logo and Play Store listings.
* The App Engine project used for tweetlanes.com.

The only items used in the Play Store build of Tweet Lanes _not_ present in this repository are:

* The Twitter Application consumer and secret keys.
* The App.net consumer and secret keys.
* The Play Store key used to sign the application.


## Credentials

To build and use the app, you'll need to create your own Twitter application (and App.net application, if you have a developer account).

### Twitter

* [Create a Twitter application](https://dev.twitter.com/apps)
* Don't worry about the callback url, just put a valid http url.
* Once you create it, go to it's settings.
* Inside *Application Type*:

	* Make sure _Allow this application to be used to Sign in with Twitter_ is checked
	* Set the _Access_ to Read, Write and Access direct messages

* Now that you have the Twitter app configured, open the java file: [android/client/src/com/tweetlanes/android/ConsumerKeyConstants.java](https://github.com/chrislacy/TweetLanes/blob/master/android/client/src/com/tweetlanes/android/ConsumerKeyConstants.java) and modify your _TWITTER_CONSUMER_KEY_ and _TWITTER_CONSUMER_SECRET_ variables with the ones from your Twitter app.

### App.net

App.net usage requires a similar set of steps.

* [Create an App.net application](https://account.app.net/developer/app/create/)
* Don't worry about the callback url, just put a valid http url.
* Once you create it, go to it's settings.
* Add the following callback URL: tweetlanes-auth-callback:///
* Now that you have the App.net app configured, open the java file: [android/client/src/com/tweetlanes/android/ConsumerKeyConstants.java](https://github.com/chrislacy/TweetLanes/blob/master/android/client/src/com/tweetlanes/android/ConsumerKeyConstants.java) and modify your _APPDOTNET_CONSUMER_KEY_ and _APPDOTNET_CONSUMER_SECRET_ variables with the ones from your app.


## Developed By
* Chris Lacy - [@chrismlacy](http://twitter.com/chrismlacy), [+Chris Lacy](https://plus.google.com/104649936579980037256/posts), <lacy@tweetlanes.com>

## Credits
Tweet Lanes uses the following open-source software:

* [Android-PullToRefresh](https://github.com/chrisbanes/Android-PullToRefresh), Copyright 2013 Chris Banes.
* [AOSP](http://source.android.com/), Copyright 2008-2013 Android Open Source Project.
* [GestureImageView](https://github.com/jasonpolites/gesture-imageview), Copyright 2012 Jason Polites.
* [Prime](https://github.com/DHuckaby/Prime), Copyright (C) 2012 Daniel Huckaby.
* [Twidere](https://github.com/mariotaku/twidere), Copyright (C) 2012 Mariotaku Lee.
* [Twitter4J](https://github.com/yusuke/twitter4j), Copyright 2007-2013 Yusuke Yamamoto.
* [UrlImageViewHelper](https://github.com/koush/UrlImageViewHelper), Copyright 2013 Koushik Dutta.
* [ViewPagerIndicator](https://github.com/JakeWharton/Android-ViewPagerIndicator), Copyright 2012 Jake Wharton.
* [Inscription](https://github.com/MartinvanZ/Inscription) Copyright 2012 Martin van Zuilekom.

## Disclaimer

A few points to considering when browsing this code:

1. *Tweet Lanes was the first app I wrote in Java, as well as my first Android app.* It is both my Android and Java 'Hello World'. The project contains much code that causes me to cringe when looking at it now, but it exists because I simply didn't know better at the time of writing it. 
2. *Tweet Lanes was developed on the fly*. I often added a feature and shipped it that night. If that new code was buggy, I sometimes added a quick and dirty fix to get things going again. The intent was often to go back and clean such code up, but due to the sheer amount of work I had to do, that rarely ended up happening.

In conclusion: there is currently some dog-awful code in this project. I know this only too well. Please try not to judge my coding ability on the worst parts, but do feel free to fork 'n fix :)


## Usage

`\android\client` contains the bulk of the code. This project is dependent on the following libraries:

* `\android\libraries\SocialNetLib` - An abstracted library that interfaces with Twitter and App.net.

Additional notes: 

* Configuration files for Eclipse are provided. If using Eclipse/ADT, you should be able to import the client and libraries straight in. 

## License

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


## Contributions

Contributions are most welcome. In fact, they are actively encouraged. 

[Have a read here](https://github.com/chrislacy/TweetLanes/wiki/What-needs-to-be-done) for what I consider to be the most pressing tasks in need to attention.

Before contributing please have a read of [our wiki](https://github.com/chrislacy/TweetLanes/wiki) which contains useful information to get you started and also where to send your pull requests.

## Note

I retain the copyright and ownership of the Tweet Lanes name. If you choose to release a fork of this code, please use a different name for your project.
