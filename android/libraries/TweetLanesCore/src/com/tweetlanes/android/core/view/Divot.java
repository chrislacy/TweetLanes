// Copyright 2011 Google Inc.
// All Rights Reserved.

package com.tweetlanes.android.core.view;

import android.widget.ImageView;

public interface Divot {

    // Distance, in dips, from the corner of the image to the start of the
    // divot.
    // Used for non-middle positions. For middle positions this distance is
    // basically
    // to the middle of edge.
    static final float CORNER_OFFSET = 12F;
    static final float WIDTH = 6F;
    static final float HEIGHT = 16F;

    // Where to draw the divot. LEFT_UPPER, for example, means the upper edge
    // but to the
    // left. TOP_RIGHT means the right edge but to the top.
    public static final int LEFT_UPPER = 1;
    public static final int LEFT_MIDDLE = 2;
    public static final int LEFT_LOWER = 3;

    public static final int RIGHT_UPPER = 4;
    public static final int RIGHT_MIDDLE = 5;
    public static final int RIGHT_LOWER = 6;

    public static final int TOP_LEFT = 7;
    public static final int TOP_MIDDLE = 8;
    public static final int TOP_RIGHT = 9;

    public static final int BOTTOM_LEFT = 10;
    public static final int BOTTOM_MIDDLE = 11;
    public static final int BOTTOM_RIGHT = 12;

    static final String[] sPositionChoices = new String[]{"", "left_upper",
            "left_middle", "left_lower",

            "right_upper", "right_middle", "right_lower",

            "top_left", "top_middle", "top_right",

            "bottom_left", "bottom_middle", "bottom_right",};

    public void setPosition(int position);

    public int getPosition();

    public float getCloseOffset();

    public float getFarOffset();

    public ImageView asImageView();
    // public void assignContactFromEmail(String emailAddress);
}
