package com.getpebble.android.kit;

import java.util.UUID;

/**
 * Constant values used by Pebble-enabled kit applications.
 *
 * @author zulak@getpebble.com
 */
public final class Constants {

    /**
     * Intent broadcast by pebble.apk when a new connection to a Pebble is established.
     */
    public static final String INTENT_PEBBLE_CONNECTED = "com.getpebble.action.PEBBLE_CONNECTED";
    /**
     * Intent broadcast by pebble.apk when the connection to a Pebble is closed or lost.
     */
    public static final String INTENT_PEBBLE_DISCONNECTED = "com.getpebble.action.PEBBLE_DISCONNECTED";

    /**
     * Intent broadcast to pebble.apk to indicate that a message was received from the watch. To avoid protocol timeouts
     * on the watch, applications <em>must</em> ACK or NACK all received messages.
     */
    public static final String INTENT_APP_ACK = "com.getpebble.action.app.ACK";

    /**
     * Intent broadcast to pebble.apk to indicate that a message was unsuccessfully received from the watch.
     */
    public static final String INTENT_APP_NACK = "com.getpebble.action.app.NACK";

    /**
     * Intent broadcast from pebble.apk containing one-or-more key-value pairs sent from the watch to the phone.
     */
    public static final String INTENT_APP_RECEIVE = "com.getpebble.action.app.RECEIVE";

    /**
     * Intent broadcast from pebble.apk indicating that a sent message was successfully received by a watch app.
     */
    public static final String INTENT_APP_RECEIVE_ACK = "com.getpebble.action.app.RECEIVE_ACK";

    /**
     * Intent broadcast from pebble.apk indicating that a sent message was not received by a watch app.
     */
    public static final String INTENT_APP_RECEIVE_NACK = "com.getpebble.action.app.RECEIVE_NACK";

    /**
     * Intent broadcast to pebble.apk containing one-or-more key-value pairs to be sent to the watch from the phone.
     */
    public static final String INTENT_APP_SEND = "com.getpebble.action.app.SEND";

    /**
     * Intent broadcast to pebble.apk responsible for launching a watch-app on the connected watch. This intent is
     * idempotent.
     */
    public static final String INTENT_APP_START = "com.getpebble.action.app.START";

    /**
     * Intent broadcast to pebble.apk responsible for closing a running watch-app on the connected watch. This intent is
     * idempotent.
     */
    public static final String INTENT_APP_STOP = "com.getpebble.action.app.STOP";

    /**
     * Intent broadcast to pebble.apk responsible for customzing the name and icon of the 'stock' Sports and Golf
     * applications included in the watch's firmware.
     */
    public static final String INTENT_APP_CUSTOMIZE = "com.getpebble.action.app.CONFIGURE";

    /**
     * The UUID corresponding to Pebble's built-in "Sports" application.
     */
    public static final UUID SPORTS_UUID = UUID.fromString("4dab81a6-d2fc-458a-992c-7a1f3b96a970");

    /**
     * The UUID corresponding to Pebble's built-in "Golf" application.
     */
    public static final UUID GOLF_UUID = UUID.fromString("cf1e816a-9db0-4511-bbb8-f60c48ca8fac");

    /**
     * The bundle-key used to store a message's transaction id.
     */
    public static final String TRANSACTION_ID = "transaction_id";
    /**
     * The bundle-key used to store a message's UUID.
     */
    public static final String APP_UUID = "uuid";
    /**
     * The bundle-key used to store a message's JSON payload send-to or received-from the watch.
     */
    public static final String MSG_DATA = "msg_data";
    /**
     * The bundle-key used to store the type of application being customized in a CUSTOMIZE intent.
     */
    public static final String CUST_APP_TYPE = "app_type";
    /**
     * The bundle-key used to store the custom name provided in a CUSTOMIZE intent.
     */
    public static final String CUST_NAME = "name";
    /**
     * The bundle-key used to store the custom icon provided in a CUSTOMIZE intent.
     */
    public static final String CUST_ICON = "icon";

    /**
     * The PebbleDictionary key corresponding to the 'time' field sent to the Sports watch-app.
     */
    public static final int SPORTS_TIME_KEY = 0x00;
    /**
     * The PebbleDictionary key corresponding to the 'distance' field sent to the Sports watch-app.
     */
    public static final int SPORTS_DISTANCE_KEY = 0x01;
    /**
     * The PebbleDictionary key corresponding to the 'data' field sent to the Sports watch-app.
     * The data field is paired with a variable label and can be used to display any data.
     */
    public static final int SPORTS_DATA_KEY = 0x02;
    /**
     * The PebbleDictionary key corresponding to the 'units' field sent to the Sports watch-app.
     */
    public static final int SPORTS_UNITS_KEY = 0x03;
    /**
     * The PebbleDictionary key corresponding to the 'state' field sent to the Sports watch-app. Both the watch and
     * phone-app may modify this field. The phone-application is responsible for performing any required state
     * transitions to stay in sync with the watch-app's state.
     */
    public static final int SPORTS_STATE_KEY = 0x04;
    /**
     * The PebbleDictionary key corresponding to the 'label' field sent to the Sports watch-app.
     * The label field controls the label above the 'data' field.
     */
    public static final int SPORTS_LABEL_KEY = 0x05;

    /**
     * PebbleDictionary value corresponding to 'imperial' units.
     */
    public static final int SPORTS_UNITS_IMPERIAL = 0x00;
    /**
     * PebbleDictionary value corresponding to 'metric' units.
     */
    public static final int SPORTS_UNITS_METRIC = 0x01;
    /**
     * PebbleDictionary value corresponding to 'speed' data.
     */
    public static final int SPORTS_DATA_SPEED = 0x00;
    /**
     * PebbleDictionary value corresponding to 'pace' data.
     */
    public static final int SPORTS_DATA_PACE = 0x01;

    public static final int SPORTS_STATE_INIT = 0x00;
    public static final int SPORTS_STATE_RUNNING = 0x01;
    public static final int SPORTS_STATE_PAUSED = 0x02;
    public static final int SPORTS_STATE_END = 0x03;

    public static final int GOLF_FRONT_KEY = 0x00;
    public static final int GOLF_MID_KEY = 0x01;
    public static final int GOLF_BACK_KEY = 0x02;
    public static final int GOLF_HOLE_KEY = 0x03;
    public static final int GOLF_PAR_KEY = 0x04;
    public static final int GOLF_CMD_KEY = 0x05;

    /**
     * Command sent by the golf-application to display the next hole.
     */
    public static final int GOLF_CMD_PREV = 0x01;

    /**
     * Command sent by the golf-application to display the previous hole.
     */
    public static final int GOLF_CMD_NEXT = 0x02;

    private Constants() {

    }

    public static enum PebbleAppType {
        SPORTS(0x00),
        GOLF(0x01),
        OTHER(0xff);

        public final int ord;

        private PebbleAppType(final int ord) {
            this.ord = ord;
        }
    }
}
