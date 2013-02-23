/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/mohitd2000/Developer/workspace/TweetLanes/src/com/tweetlanes/android/INotificationService.aidl
 */
package com.tweetlanes.android;
public interface INotificationService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.tweetlanes.android.INotificationService
{
private static final java.lang.String DESCRIPTOR = "com.tweetlanes.android.INotificationService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.tweetlanes.android.INotificationService interface,
 * generating a proxy if needed.
 */
public static com.tweetlanes.android.INotificationService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.tweetlanes.android.INotificationService))) {
return ((com.tweetlanes.android.INotificationService)iin);
}
return new com.tweetlanes.android.INotificationService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_getMentions:
{
data.enforceInterface(DESCRIPTOR);
long[] _arg0;
_arg0 = data.createLongArray();
long[] _arg1;
_arg1 = data.createLongArray();
int _result = this.getMentions(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_isMentionsRefreshing:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isMentionsRefreshing();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_hasActivatedTask:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.hasActivatedTask();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_test:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.test();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_startAutoRefresh:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.startAutoRefresh();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_stopAutoRefresh:
{
data.enforceInterface(DESCRIPTOR);
this.stopAutoRefresh();
reply.writeNoException();
return true;
}
case TRANSACTION_shutdownService:
{
data.enforceInterface(DESCRIPTOR);
this.shutdownService();
reply.writeNoException();
return true;
}
case TRANSACTION_clearNotification:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.clearNotification(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.tweetlanes.android.INotificationService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public int getMentions(long[] account_ids, long[] max_ids) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLongArray(account_ids);
_data.writeLongArray(max_ids);
mRemote.transact(Stub.TRANSACTION_getMentions, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean isMentionsRefreshing() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isMentionsRefreshing, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean hasActivatedTask() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_hasActivatedTask, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean test() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_test, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean startAutoRefresh() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_startAutoRefresh, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void stopAutoRefresh() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stopAutoRefresh, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void shutdownService() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_shutdownService, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void clearNotification(int id) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(id);
mRemote.transact(Stub.TRANSACTION_clearNotification, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_getMentions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_isMentionsRefreshing = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_hasActivatedTask = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_test = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_startAutoRefresh = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_stopAutoRefresh = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_shutdownService = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_clearNotification = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
}
public int getMentions(long[] account_ids, long[] max_ids) throws android.os.RemoteException;
public boolean isMentionsRefreshing() throws android.os.RemoteException;
public boolean hasActivatedTask() throws android.os.RemoteException;
public boolean test() throws android.os.RemoteException;
public boolean startAutoRefresh() throws android.os.RemoteException;
public void stopAutoRefresh() throws android.os.RemoteException;
public void shutdownService() throws android.os.RemoteException;
public void clearNotification(int id) throws android.os.RemoteException;
}
