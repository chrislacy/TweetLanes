package com.getpebble.android.kit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import com.getpebble.android.kit.util.PebbleDictionary;
import org.json.JSONException;

import java.util.UUID;

import static com.getpebble.android.kit.Constants.*;

/**
 * A helper class providing methods for interacting with third-party Pebble Smartwatch applications. Pebble-enabled
 * Android applications may use this class to assist in sending/receiving data between the watch and the phone.
 *
 * @author zulak@getpebble.com
 */
public final class PebbleKit {

    private static final int NAME_MAX_LENGTH = 32;
    private static final int ICON_MAX_DIMENSIONS = 32;

    private PebbleKit() {

    }

    /**
     * Send a message to the connected Pebble to "customize" a built-in PebbleKit watch-app. This is intended to allow
     * third-party Android applications to apply custom branding (both name & icon) on the watch without needing to
     * distribute a complete watch-app.
     *
     * @param context
     *         The context used to send the broadcast. (Protip: pass in the ApplicationContext here.)
     * @param appType
     *         The watch-app to be configured. Options are either {@link PebbleAppType#SPORTS} or {@link
     *         PebbleAppType#GOLF}.
     * @param name
     *         The custom name to be applied to the watch-app. Names must be less than 32 characters in length.
     * @param icon
     *         The custom icon to be applied to the watch-app. Icons must be black-and-white bitmaps no larger than 32px
     *         in either dimension.
     *
     * @throws IllegalArgumentException
     *         Thrown if the specified name or icon are invalid.
     */
    public static void customizeWatchApp(final Context context, final PebbleAppType appType, final String name,
                                         final Bitmap icon)
            throws IllegalArgumentException {

        if (appType == null) {
            throw new IllegalArgumentException("app type cannot be null");
        }

        if (name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("app name exceeds maximum length (%d)", NAME_MAX_LENGTH));
        }

        if (icon.getHeight() > ICON_MAX_DIMENSIONS || icon.getWidth() > ICON_MAX_DIMENSIONS) {
            throw new IllegalArgumentException(String.format(
                    "app icon exceeds maximum dimensions (32px x 32px); got (%dpx x %dpx)",
                    icon.getWidth(), icon.getHeight()));
        }

        final Intent customizeAppIntent = new Intent(INTENT_APP_CUSTOMIZE);
        customizeAppIntent.putExtra(CUST_APP_TYPE, appType.ord);
        customizeAppIntent.putExtra(CUST_NAME, name);
        customizeAppIntent.putExtra(CUST_ICON, icon);
        context.sendBroadcast(customizeAppIntent);
    }

    /**
     * Synchronously query the Pebble application to see if an active Bluetooth connection to a watch currently exists.
     *
     * @param context
     *         The Android context used to perform the query.
     *         <p/>
     *         <em>Protip:</em> You probably want to use your ApplicationContext here.
     *
     * @return true if an active connection to the watch currently exists, otherwise false. This method will also return
     *         false if the Pebble application is not installed on the user's handset.
     */
    public static boolean isWatchConnected(final Context context) {
        Cursor c = context.getContentResolver().query(Uri.parse("content://com.getpebble.android.provider/state"),
                null, null, null, null);
        if (c == null || !c.moveToNext()) {
            return false;
        }
        return c.getInt(0) == 1;
    }

    /**
     * Synchronously query the Pebble application to see if the connected watch is running a firmware version that
     * supports PebbleKit messages.
     *
     * @param context
     *         The Android context used to perform the query.
     *         <p/>
     *         <em>Protip:</em> You probably want to use your ApplicationContext here.
     *
     * @return true if the watch supports PebbleKit messages, otherwise false. This method will always return false if
     *         no Pebble is currently connected to the handset.
     */
    public static boolean areAppMessagesSupported(final Context context) {
        Cursor c = context.getContentResolver().query(Uri.parse("content://com.getpebble.android.provider/state"),
                null, null, null, null);
        if (c == null || !c.moveToNext()) {
            return false;
        }
        return c.getInt(1) == 1;
    }

    /**
     * Send a message to the connected Pebble to launch an application identified by a UUID. If another application is
     * currently running it will be terminated and the new application will be brought to the foreground.
     *
     * @param context
     *         The context used to send the broadcast.
     * @param watchappUuid
     *         A UUID uniquely identifying the target application. UUIDs for the stock PebbleKit applications are
     *         available in {@link Constants}.
     *
     * @throws IllegalArgumentException
     *         Thrown if the specified UUID is invalid.
     */
    public static void startAppOnPebble(final Context context, final UUID watchappUuid)
            throws IllegalArgumentException {

        if (watchappUuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }

        final Intent startAppIntent = new Intent(INTENT_APP_START);
        startAppIntent.putExtra(APP_UUID, watchappUuid);
        context.sendBroadcast(startAppIntent);
    }

    /**
     * Send a message to the connected Pebble to close an application identified by a UUID. If this application is not
     * currently running, the message is ignored.
     *
     * @param context
     *         The context used to send the broadcast.
     * @param watchappUuid
     *         A UUID uniquely identifying the target application. UUIDs for the stock kit applications are available in
     *         {@link Constants}.
     *
     * @throws IllegalArgumentException
     *         Thrown if the specified UUID is invalid.
     */
    public static void closeAppOnPebble(final Context context, final UUID watchappUuid)
            throws IllegalArgumentException {

        if (watchappUuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }

        final Intent stopAppIntent = new Intent(INTENT_APP_STOP);
        stopAppIntent.putExtra(APP_UUID, watchappUuid);
        context.sendBroadcast(stopAppIntent);
    }

    /**
     * Send one-or-more key-value pairs to the watch-app identified by the provided UUID. This is the primary method for
     * sending data from the phone to a connected Pebble.
     * <p/>
     * The watch-app and phone-app must agree of the set and type of key-value pairs being exchanged. Type mismatches or
     * missing keys will cause errors on the receiver's end.
     *
     * @param context
     *         The context used to send the broadcast.
     * @param watchappUuid
     *         A UUID uniquely identifying the target application. UUIDs for the stock kit applications are available in
     *         {@link Constants}.
     * @param data
     *         A dictionary containing one-or-more key-value pairs. For more information about the types of data that
     *         can be stored, see {@link PebbleDictionary}.
     *
     * @throws IllegalArgumentException
     *         Thrown in the specified PebbleDictionary or UUID is invalid.
     */
    public static void sendDataToPebble(final Context context, final UUID watchappUuid, final PebbleDictionary data)
            throws IllegalArgumentException {

        sendDataToPebbleWithTransactionId(context, watchappUuid, data, -1);
    }

    /**
     * Send one-or-more key-value pairs to the watch-app identified by the provided UUID.
     * <p/>
     * The watch-app and phone-app must agree of the set and type of key-value pairs being exchanged. Type mismatches or
     * missing keys will cause errors on the receiver's end.
     *
     * @param context
     *         The context used to send the broadcast.
     * @param watchappUuid
     *         A UUID uniquely identifying the target application. UUIDs for the stock kit applications are available in
     *         {@link Constants}.
     * @param data
     *         A dictionary containing one-or-more key-value pairs. For more information about the types of data that
     *         can be stored, see {@link PebbleDictionary}.
     *
     * @param transactionId
     *         An integer uniquely identifying the transaction. This can be used to correlate messages sent to the
     *         Pebble and ACK/NACKs received from the Pebble.
     *
     * @throws IllegalArgumentException
     *         Thrown in the specified PebbleDictionary or UUID is invalid.
     */
    public static void sendDataToPebbleWithTransactionId(final Context context, final UUID watchappUuid,
                                            final PebbleDictionary data, final int transactionId)
            throws IllegalArgumentException {

        if (watchappUuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }

        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }

        if (data.size() == 0) {
            return;
        }

        final Intent sendDataIntent = new Intent(INTENT_APP_SEND);
        sendDataIntent.putExtra(APP_UUID, watchappUuid);
        sendDataIntent.putExtra(TRANSACTION_ID, transactionId);
        sendDataIntent.putExtra(MSG_DATA, data.toJsonString());
        context.sendBroadcast(sendDataIntent);
    }

    /**
     * Send a message to the connected watch acknowledging the receipt of a PebbleDictionary.  To avoid protocol
     * timeouts on the watch, applications <em>must</em> ACK or NACK all received messages.
     *
     * @param context
     *         The context used to send the broadcast.
     * @param transactionId
     *         The transaction id of the message in which the data was received. Valid transaction IDs are between (0,
     *         255).
     *
     * @throws IllegalArgumentException
     *         Thrown if an invalid transaction id is specified.
     */
    public static void sendAckToPebble(final Context context, final int transactionId)
            throws IllegalArgumentException {

        if ((transactionId & ~0xff) != 0) {
            throw new IllegalArgumentException(
                    String.format("transaction id must be between (0, 255); got '%d'", transactionId));
        }

        final Intent ackIntent = new Intent(INTENT_APP_ACK);
        ackIntent.putExtra(TRANSACTION_ID, transactionId);
        context.sendBroadcast(ackIntent);
    }

    /**
     * Send a message to the connected watch that the previously sent PebbleDictionary was not received successfully. To
     * avoid protocol timeouts on the watch, applications <em>must</em> ACK or NACK all received messages.
     *
     * @param context
     *         The context used to send the broadcast.
     * @param transactionId
     *         The transaction id of the message in which the data was received. Valid transaction IDs are between (0,
     *         255).
     *
     * @throws IllegalArgumentException
     *         Thrown if an invalid transaction id is specified.
     */
    public static void sendNackToPebble(final Context context, final int transactionId)
            throws IllegalArgumentException {

        if ((transactionId & ~0xff) != 0) {
            throw new IllegalArgumentException(
                    String.format("transaction id must be between (0, 255); got '%d'", transactionId));
        }

        final Intent nackIntent = new Intent(INTENT_APP_NACK);
        nackIntent.putExtra(TRANSACTION_ID, transactionId);
        context.sendBroadcast(nackIntent);
    }

    /**
     * A convenience function to assist in programatically registering a broadcast receiver for the 'CONNECTED' intent.
     * <p/>
     * To avoid leaking memory, activities registering BroadcastReceivers <em>must</em> unregister them in the
     * Activity's {@link android.app.Activity#onPause()} method.
     *
     * @param context
     *         The context in which to register the BroadcastReceiver.
     * @param receiver
     *         The receiver to be registered.
     *
     * @return The registered receiver.
     *
     * @see Constants#INTENT_PEBBLE_CONNECTED
     */
    public static BroadcastReceiver registerPebbleConnectedReceiver(final Context context,
                                                                    final BroadcastReceiver receiver) {
        return registerBroadcastReceiverInternal(context, INTENT_PEBBLE_CONNECTED, receiver);
    }

    /**
     * A convenience function to assist in programatically registering a broadcast receiver for the 'DISCONNECTED'
     * intent.
     * <p/>
     * Go avoid leaking memory, activities registering BroadcastReceivers <em>must</em> unregister them in the
     * Activity's {@link android.app.Activity#onPause()} method.
     *
     * @param context
     *         The context in which to register the BroadcastReceiver.
     * @param receiver
     *         The receiver to be registered.
     *
     * @return The registered receiver.
     *
     * @see Constants#INTENT_PEBBLE_DISCONNECTED
     */
    public static BroadcastReceiver registerPebbleDisconnectedReceiver(final Context context,
                                                                       final BroadcastReceiver receiver) {
        return registerBroadcastReceiverInternal(context, INTENT_PEBBLE_DISCONNECTED, receiver);
    }

    /**
     * A convenience function to assist in programatically registering a broadcast receiver for the 'RECEIVE' intent.
     * <p/>
     * To avoid leaking memory, activities registering BroadcastReceivers <em>must</em> unregister them in the
     * Activity's {@link android.app.Activity#onPause()} method.
     *
     * @param context
     *         The context in which to register the BroadcastReceiver.
     * @param receiver
     *         The receiver to be registered.
     *
     * @return The registered receiver.
     *
     * @see Constants#INTENT_APP_RECEIVE
     */
    public static BroadcastReceiver registerReceivedDataHandler(final Context context,
                                                                final PebbleDataReceiver receiver) {
        return registerBroadcastReceiverInternal(context, INTENT_APP_RECEIVE, receiver);
    }

    /**
     * A convenience function to assist in programatically registering a broadcast receiver for the 'RECEIVE_ACK'
     * intent.
     * <p/>
     * To avoid leaking memory, activities registering BroadcastReceivers <em>must</em> unregister them in the
     * Activity's {@link android.app.Activity#onPause()} method.
     *
     * @param context
     *         The context in which to register the BroadcastReceiver.
     * @param receiver
     *         The receiver to be registered.
     *
     * @return The registered receiver.
     *
     * @see Constants#INTENT_APP_RECEIVE_ACK
     */
    public static BroadcastReceiver registerReceivedAckHandler(final Context context,
                                                               final PebbleAckReceiver receiver) {
        return registerBroadcastReceiverInternal(context, INTENT_APP_RECEIVE_ACK, receiver);
    }

    /**
     * A convenience function to assist in programatically registering a broadcast receiver for the 'RECEIVE_NACK'
     * intent.
     * <p/>
     * To avoid leaking memory, activities registering BroadcastReceivers <em>must</em> unregister them in the
     * Activity's {@link android.app.Activity#onPause()} method.
     *
     * @param context
     *         The context in which to register the BroadcastReceiver.
     * @param receiver
     *         The receiver to be registered.
     *
     * @return The registered receiver.
     *
     * @see Constants#INTENT_APP_RECEIVE_NACK
     */
    public static BroadcastReceiver registerReceivedNackHandler(final Context context,
                                                                final PebbleNackReceiver receiver) {
        return registerBroadcastReceiverInternal(context, INTENT_APP_RECEIVE_NACK, receiver);
    }

    private static BroadcastReceiver registerBroadcastReceiverInternal(final Context context, final String action,
                                                                       final BroadcastReceiver receiver) {
        if (receiver == null) {
            return null;
        }

        IntentFilter filter = new IntentFilter(action);
        context.registerReceiver(receiver, filter);
        return receiver;
    }

    /**
     * A special-purpose BroadcastReceiver that makes it easy to handle 'RECEIVE' intents broadcast from pebble.apk.
     */
    public static abstract class PebbleDataReceiver extends BroadcastReceiver {
        private final UUID subscribedUuid;

        protected PebbleDataReceiver(final UUID subscribedUuid) {
            this.subscribedUuid = subscribedUuid;
        }

        /**
         * Perform some work on the data received from the connected watch.
         *
         * @param context
         *         The BroadcastReceiver's context.
         * @param transactionId
         *         The transaction ID of the message in which the data was received. This is required when ACK/NACKing
         *         the received message.
         * @param data
         *         A dictionary of one-or-more key-value pairs received from the connected watch.
         */
        public abstract void receiveData(final Context context, final int transactionId, final PebbleDictionary data);

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final UUID receivedUuid = (UUID) intent.getSerializableExtra(APP_UUID);

            // Pebble-enabled apps are expected to be good citizens and only inspect broadcasts containing their UUID
            if (!subscribedUuid.equals(receivedUuid)) {
                return;
            }

            final int transactionId = intent.getIntExtra(TRANSACTION_ID, -1);
            final String jsonData = intent.getStringExtra(MSG_DATA);
            if (jsonData == null || jsonData.isEmpty()) {
                return;
            }

            try {
                final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
                receiveData(context, transactionId, data);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    /**
     * A special-purpose BroadcastReceiver that makes it easy to handle 'RECEIVE_ACK' intents broadcast from pebble
     * .apk.
     */
    public static abstract class PebbleAckReceiver extends BroadcastReceiver {
        private final UUID subscribedUuid;

        protected PebbleAckReceiver(final UUID subscribedUuid) {
            this.subscribedUuid = subscribedUuid;
        }

        /**
         * Handle the ACK received from the connected watch.
         *
         * @param context
         *         The BroadcastReceiver's context.
         * @param transactionId
         *         The transaction ID of the message for which the ACK was received. This indicates which message was
         *         successfully received.
         */
        public abstract void receiveAck(final Context context, final int transactionId);

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int transactionId = intent.getIntExtra(TRANSACTION_ID, -1);
            receiveAck(context, transactionId);

        }
    }

    /**
     * A special-purpose BroadcastReceiver that makes it easy to handle 'RECEIVE_NACK' intents broadcast from pebble
     * .apk.
     */
    public static abstract class PebbleNackReceiver extends BroadcastReceiver {
        private final UUID subscribedUuid;

        protected PebbleNackReceiver(final UUID subscribedUuid) {
            this.subscribedUuid = subscribedUuid;
        }

        /**
         * Handle the NACK received from the connected watch.
         *
         * @param context
         *         The BroadcastReceiver's context.
         * @param transactionId
         *         The transaction ID of the message for which the NACK was received. This indicates which message was
         *         not received.
         */
        public abstract void receiveNack(final Context context, final int transactionId);

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int transactionId = intent.getIntExtra(TRANSACTION_ID, -1);
            receiveNack(context, transactionId);

        }
    }
}
