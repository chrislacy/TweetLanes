package com.getpebble.android.kit.util;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A collection of key-value pairs of heterogeneous types. PebbleDictionaries are the primary structure used to exchange
 * data between the phone and watch.
 * <p/>
 * To accommodate the mixed-types contained within a PebbleDictionary, an internal JSON representation is used when
 * exchanging the dictionary between Android processes.
 *
 * @author zulak@getpebble.com
 */
public class PebbleDictionary implements Iterable<PebbleTuple> {

    private static final String KEY = "key";
    private static final String TYPE = "type";
    private static final String LENGTH = "length";
    private static final String VALUE = "value";

    protected final Map<Integer, PebbleTuple> tuples = new HashMap<Integer, PebbleTuple>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<PebbleTuple> iterator() {
        return tuples.values().iterator();
    }

    /**
     * Returns the number of key-value pairs in this dictionary.
     *
     * @return the number of key-value pairs in this dictionary
     */
    public int size() {
        return tuples.size();
    }

    /**
     * Returns true if this dictionary contains a mapping for the specified key.
     *
     * @param key
     *         key whose presence in this dictionary is to be tested
     *
     * @return true if this dictionary contains a mapping for the specified key
     */
    public boolean contains(final int key) {
        return tuples.containsKey(key);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key
     *         key to be removed from the dictionary
     */
    public void remove(final int key) {
        tuples.remove(key);
    }

    /**
     * Associate the specified byte array with the provided key in the dictionary. If another key-value pair with the
     * same key is already present in the dictionary, it will be replaced.
     *
     * @param key
     *         key with which the specified value is associated
     * @param bytes
     *         value to be associated with the specified key
     */
    public void addBytes(int key, byte[] bytes) {
        PebbleTuple t = PebbleTuple.create(key, PebbleTuple.TupleType.BYTES, PebbleTuple.Width.NONE, bytes);
        addTuple(t);
    }

    /**
     * Associate the specified String with the provided key in the dictionary. If another key-value pair with the same
     * key is already present in the dictionary, it will be replaced.
     *
     * @param key
     *         key with which the specified value is associated
     * @param value
     *         value to be associated with the specified key
     */
    public void addString(int key, String value) {
        PebbleTuple t =
                PebbleTuple.create(key, PebbleTuple.TupleType.STRING, PebbleTuple.Width.NONE, value);
        addTuple(t);
    }

    /**
     * Associate the specified signed byte with the provided key in the dictionary. If another key-value pair with the
     * same key is already present in the dictionary, it will be replaced.
     *
     * @param key
     *         key with which the specified value is associated
     * @param b
     *         value to be associated with the specified key
     */
    public void addInt8(final int key, final byte b) {
        PebbleTuple t = PebbleTuple.create(key, PebbleTuple.TupleType.INT, PebbleTuple.Width.BYTE, b);
        addTuple(t);
    }

    /**
     * Associate the specified unsigned byte with the provided key in the dictionary. If another key-value pair with the
     * same key is already present in the dictionary, it will be replaced.
     *
     * @param key
     *         key with which the specified value is associated
     * @param b
     *         value to be associated with the specified key
     */
    public void addUint8(final int key, final byte b) {
        PebbleTuple t = PebbleTuple.create(key, PebbleTuple.TupleType.UINT, PebbleTuple.Width.BYTE, b);
        addTuple(t);
    }

    /**
     * Associate the specified signed short with the provided key in the dictionary. If another key-value pair with the
     * same key is already present in the dictionary, it will be replaced.
     *
     * @param key
     *         key with which the specified value is associated
     * @param s
     *         value to be associated with the specified key
     */
    public void addInt16(final int key, final short s) {
        PebbleTuple t = PebbleTuple.create(key, PebbleTuple.TupleType.INT, PebbleTuple.Width.SHORT, s);
        addTuple(t);
    }

    /**
     * Associate the specified unsigned short with the provided key in the dictionary. If another key-value pair with
     * the same key is already present in the dictionary, it will be replaced.
     *
     * @param key
     *         key with which the specified value is associated
     * @param s
     *         value to be associated with the specified key
     */
    public void addUint16(final int key, final short s) {
        PebbleTuple t = PebbleTuple.create(key, PebbleTuple.TupleType.UINT, PebbleTuple.Width.SHORT, s);
        addTuple(t);
    }

    /**
     * Associate the specified signed int with the provided key in the dictionary. If another key-value pair with the
     * same key is already present in the dictionary, it will be replaced.
     *
     * @param key
     *         key with which the specified value is associated
     * @param i
     *         value to be associated with the specified key
     */
    public void addInt32(final int key, final int i) {
        PebbleTuple t = PebbleTuple.create(key, PebbleTuple.TupleType.INT, PebbleTuple.Width.WORD, i);
        addTuple(t);
    }

    /**
     * Associate the specified unsigned int with the provided key in the dictionary. If another key-value pair with the
     * same key is already present in the dictionary, it will be replaced.
     *
     * @param key
     *         key with which the specified value is associated
     * @param i
     *         value to be associated with the specified key
     */
    public void addUint32(final int key, final int i) {
        PebbleTuple t = PebbleTuple.create(key, PebbleTuple.TupleType.UINT, PebbleTuple.Width.WORD, i);
        addTuple(t);
    }

    private PebbleTuple getTuple(int key, PebbleTuple.TupleType type) {
        if (!tuples.containsKey(key) || tuples.get(key) == null) {
            return null;
        }

        PebbleTuple t = tuples.get(key);
        if (t.type != type) {
            throw new PebbleDictTypeException(key, type, t.type);
        }
        return t;
    }

    /**
     * Returns the signed integer to which the specified key is mapped, or null if the key does not exist in this
     * dictionary.
     *
     * @param key
     *         key whose associated value is to be returned
     *
     * @return value to which the specified key is mapped
     */
    public Long getInteger(int key) {
        PebbleTuple tuple = getTuple(key, PebbleTuple.TupleType.INT);
        if (tuple == null) {
            return null;
        }
        return (Long) tuple.value;
    }

    /**
     * Returns the unsigned integer to which the specified key is mapped, or null if the key does not exist in this
     * dictionary.
     *
     * @param key
     *         key whose associated value is to be returned
     *
     * @return value to which the specified key is mapped
     */
    public Long getUnsignedInteger(int key) {
        PebbleTuple tuple = getTuple(key, PebbleTuple.TupleType.UINT);
        if (tuple == null) {
            return null;
        }
        return (Long) tuple.value;
    }

    /**
     * Returns the byte array to which the specified key is mapped, or null if the key does not exist in this
     * dictionary.
     *
     * @param key
     *         key whose associated value is to be returned
     *
     * @return value to which the specified key is mapped
     */
    public byte[] getBytes(int key) {
        PebbleTuple tuple = getTuple(key, PebbleTuple.TupleType.BYTES);
        if (tuple == null) {
            return null;
        }
        return (byte[]) tuple.value;
    }

    /**
     * Returns the string to which the specified key is mapped, or null if the key does not exist in this dictionary.
     *
     * @param key
     *         key whose associated value is to be returned
     *
     * @return value to which the specified key is mapped
     */
    public String getString(int key) {
        PebbleTuple tuple = getTuple(key, PebbleTuple.TupleType.STRING);
        if (tuple == null) {
            return null;
        }
        return (String) tuple.value;
    }

    protected void addTuple(PebbleTuple tuple) {
        if (tuples.size() > 0xff) {
            throw new TupleOverflowException();
        }

        tuples.put(tuple.key, tuple);
    }

    public static class PebbleDictTypeException extends RuntimeException {
        public PebbleDictTypeException(long key, PebbleTuple.TupleType expected, PebbleTuple.TupleType actual) {
            super(String.format(
                    "Expected type '%s', but got '%s' for key 0x%08x", expected.name(), actual.name(), key));
        }
    }

    public static class TupleOverflowException extends RuntimeException {
        public TupleOverflowException() {
            super("Too many tuples in dict");
        }
    }

    /**
     * Returns a JSON representation of this dictionary.
     *
     * @return a JSON representation of this dictionary
     */
    public String toJsonString() {
        try {
            JSONArray array = new JSONArray();
            for (PebbleTuple t : tuples.values()) {
                array.put(serializeTuple(t));
            }
            return array.toString();
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return null;
    }

    /**
     * Deserializes a JSON representation of a PebbleDictionary.
     *
     * @param jsonString
     *         the JSON representation to be deserialized
     *
     * @throws JSONException
     *         thrown if the specified JSON representation cannot be parsed
     */
    public static PebbleDictionary fromJson(String jsonString) throws JSONException {
        PebbleDictionary d = new PebbleDictionary();

        JSONArray elements = new JSONArray(jsonString);
        for (int idx = 0; idx < elements.length(); ++idx) {
            JSONObject o = elements.getJSONObject(idx);
            final int key = o.getInt(KEY);
            final PebbleTuple.TupleType type = PebbleTuple.TYPE_NAMES.get(o.getString(TYPE));
            final PebbleTuple.Width width = PebbleTuple.WIDTH_MAP.get(o.getInt(LENGTH));

            switch (type) {
                case BYTES:
                    byte[] bytes = Base64.decode(o.getString(VALUE), Base64.NO_WRAP);
                    d.addBytes(key, bytes);
                    break;
                case STRING:
                    d.addString(key, o.getString(VALUE));
                    break;
                case INT:
                    if (width == PebbleTuple.Width.BYTE) {
                        d.addInt8(key, (byte) o.getInt(VALUE));
                    } else if (width == PebbleTuple.Width.SHORT) {
                        d.addInt16(key, (short) o.getInt(VALUE));
                    } else if (width == PebbleTuple.Width.WORD) {
                        d.addInt32(key, o.getInt(VALUE));
                    }
                    break;
                case UINT:
                    if (width == PebbleTuple.Width.BYTE) {
                        d.addUint8(key, (byte) o.getInt(VALUE));
                    } else if (width == PebbleTuple.Width.SHORT) {
                        d.addUint16(key, (short) o.getInt(VALUE));
                    } else if (width == PebbleTuple.Width.WORD) {
                        d.addUint32(key, o.getInt(VALUE));
                    }
                    break;
            }
        }

        return d;
    }

    private static JSONObject serializeTuple(PebbleTuple t) throws JSONException {
        JSONObject j = new JSONObject();
        j.put(KEY, t.key);
        j.put(TYPE, t.type.getName());
        j.put(LENGTH, t.width.value);

        switch (t.type) {
            case BYTES:
                j.put(VALUE, Base64.encodeToString((byte[]) t.value, Base64.NO_WRAP));
                break;
            case STRING:
            case INT:
            case UINT:
                j.put(VALUE, t.value);
                break;
        }

        return j;
    }
}
