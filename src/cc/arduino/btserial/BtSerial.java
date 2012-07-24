/**
 * you can put a one sentence description of your library here.
 *
 * ##copyright##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author		##author##
 * @modified	##date##
 * @version		##version##
 */

package cc.arduino.btserial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


/* TODO */
// ** available()
// ** read()
// ** connect()
// ** readChar()
// ** readBytes()
// readBytesUntil()
// readString()
// readStringUntil()
// ** buffer()
// bufferUntil()
// ** last()
// ** lastChar()
// ** list()
// ** write()
// ** clear()
// ** stop()
// btSerialEvent()

public class BtSerial implements Runnable {

	/* PApplet context */
	private Context ctx;

	public final static String VERSION = "##version##";

	/* Bluetooth */
	private BluetoothAdapter mAdapter;
	private BluetoothDevice mDevice;
	private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	/* Socket & streams for BT communication */
	private BluetoothSocket mSocket;
	private boolean connected = false;
	protected final InputStream mInStream;
	protected final OutputStream mOutStream;
	
	/* Buffer */
	private int bufferLength = 128;
	private int available = 0;
	private byte[] buffer;
	private byte[] rawbuffer;
	private int bufferIndex;
	private int bufferLast;

	/* Debug variables */
	public static boolean DEBUG = true;
	public static String DEBUGTAG = "##name## ##version## Debug message: ";

	private final String TAG = "System.out";
	
	
	public BtSerial(Context ctx) {
		InputStream tmpIn = null;
		OutputStream tmpOut = null;

		// Get the input and output streams, using temp objects because
		// member streams are final
		try {
			tmpIn = mSocket.getInputStream();
			tmpOut = mSocket.getOutputStream();
		} catch (IOException e) { }

		mInStream = tmpIn;
		mOutStream = tmpOut;	

		buffer = new byte[bufferLength];  // buffer store for the stream
		
		this.ctx = ctx;
		welcome();

		/* Init the adapter */
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mAdapter = BluetoothAdapter.getDefaultAdapter();
			}
		});
		Log.i(TAG, "started");
	}

	/**
	 * Returns the status of the connection.
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Returns whether the adapter is enabled.
	 * 
	 * @return
	 */
	public boolean isEnabled() {
		if (mAdapter!=null) 
			return mAdapter.isEnabled();
		else return false;
	}

	/**
	 * Returns the list of bonded devices.
	 * 
	 * @return
	 */
	public String[] list() {
		Vector<String> list = new Vector<String>();
		Set<BluetoothDevice> devices;

		try {
			devices = mAdapter.getBondedDevices();
			// convert the devices 'set' into an array so that we can
			// perform string functions on it
			Object[] deviceArray = devices.toArray();
			// step through it and assign each device in turn to
			// remoteDevice and then print it's name
			for (int i = 0; i < devices.size(); i++) {
				BluetoothDevice thisDevice = mAdapter
				.getRemoteDevice(deviceArray[i].toString());
				list.addElement(thisDevice.getAddress());
			}
		} catch (UnsatisfiedLinkError e) {
			// errorMessage("devices", e);
		} catch (Exception e) {
			// errorMessage("devices", e);
		}

		String outgoing[] = new String[list.size()];
		list.copyInto(outgoing);
		return outgoing;
	}

	/*
	 * Some stubs for future implementation:
	 * 
	 */
	public void startDiscovery() {
		// this method will start a separate thread to handle discovery
	}

	public void pairWith(String thisAddress) {
		// this method will pair with a device given a MAC address
	}
	public boolean discoveryComplete() {
		// this method will return whether discovery is complete,
		// so the user can then list devices
		return false;
	}


	public String getName() {
		if (mDevice !=null) return mDevice.getName();
		else return "no device connected";
	}


	public synchronized boolean connect(String mac) {
		/* Before we connect, make sure to cancel any discovery! */
		if (mAdapter.isDiscovering()) {
			mAdapter.cancelDiscovery();
			Log.i(TAG, "Cancelled ongoing discovery");
		}

		/* Make sure we're using a real bluetooth address to connect with */
		if (BluetoothAdapter.checkBluetoothAddress(mac)) {
			/* Get the remote device we're trying to connect to */
			mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
			/* Create the RFCOMM sockets */
			try {
				mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
				mSocket.connect();

				Log.i(TAG, "Connected to device " + mDevice.getName()
						+ " [" + mDevice.getAddress() + "]");
				// Set the status 
				connected = true;
				return connected;
			} catch (IOException e) {
				Log.i(TAG, "Couldn't get a connection");
				connected = false;
				return connected;
			}

		} else {
			Log.i(TAG, "Address is not Bluetooth, please verify MAC.");
			connected = false;
			return connected;
		}
	}

	/**
	 * Returns the available number of bytes in the buffer.
	 * 
	 * @return
	 */
	public int available() {
		return (bufferLast - bufferIndex);
	}

	/**
	 * 
	 */
	private void welcome() {
		Log.i(TAG, "##name## ##version## by ##author##");
	}

	/**
	 * return the version of the library.
	 * 
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}

	@Override
	public void run() {
		Log.i(TAG, "running");
		/* Init the buffer */
		buffer = new byte[bufferLength];
		rawbuffer = new byte[bufferLength];
		
		/* Set the connected state */
		connected = true;

		int bytes; // bytes returned from read()

		// Keep listening to the InputStream until an exception occurs
		while (true) {
			try {
				// Read from the InputStream
				while (mInStream.available() > 0) {
					String outputMessage = mInStream.available() + " bytes available";
					Log.i(TAG, outputMessage);
					synchronized (buffer) {
						if (bufferLast == buffer.length) {
							byte temp[] = new byte[bufferLast << 1];
							System.arraycopy(buffer, 0, temp, 0, bufferLast);
							buffer = temp;
						}
						buffer[bufferLast++] = (byte) mInStream.read();
					}
				}
			} catch (IOException e) {
				break;
			}
		}
	}
	

	/**
	 * Writes a byte[] buffer to the output stream.
	 * 
	 * @param buffer
	 */
	public void write(byte[] bytes) {
		try {
			mOutStream.write(bytes);
		} catch (IOException e) { 
			e.printStackTrace();
		}
	}

	/**
	 * Writes a  String to the output stream.
	 * 
	 * @param thisString
	 */
	public  void write(String thisString) {
		byte[] thisBuffer = thisString.getBytes();
		write(thisBuffer);
	}

	/**
	 * Writes a  String to the output stream.
	 * 
	 * @param thisInt
	 */
	public  void write(int thisInt) {
		byte[] thisBuffer = {(byte)thisInt};
		write(thisBuffer);
	}


	/**
	 * Returns the next byte in the buffer as an int (0-255);
	 * 
	 * @return
	 */
	public int read() {
	    if (bufferIndex == bufferLast) return -1;

	    synchronized (buffer) {
	      int outgoing = buffer[bufferIndex++] & 0xff;
	      if (bufferIndex == bufferLast) {  // rewind
	        bufferIndex = 0;
	        bufferLast = 0;
	      }
	      return outgoing;
	    }
	}

	/**
	 * Returns the whole byte buffer.
	 * 
	 * @return
	 */
	public byte[] readBytes() {
	    if (bufferIndex == bufferLast) return null;

	    synchronized (buffer) {
	      int length = bufferLast - bufferIndex;
	      byte outgoing[] = new byte[length];
	      System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

	      bufferIndex = 0;  // rewind
	      bufferLast = 0;
	      return outgoing;
	    }
	}

	/**
	 * Returns the available number of bytes in the buffer, and copies the
	 * buffer contents to the passed byte[]
	 * 
	 * @param buffer
	 * @return
	 */
	public int readBytes(byte outgoing[]) {
	    if (bufferIndex == bufferLast) return 0;

	    synchronized (buffer) {
	      int length = bufferLast - bufferIndex;
	      if (length > outgoing.length) length = outgoing.length;
	      System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

	      bufferIndex += length;
	      if (bufferIndex == bufferLast) {
	        bufferIndex = 0;  // rewind
	        bufferLast = 0;
	      }
	      return length;
	    }
	}

	/**
	 * Returns a bytebuffer until the byte b. If the byte b doesn't exist in the
	 * current buffer, null is returned.
	 * 
	 * @param b
	 * @return
	 */
	public byte[] readBytesUntil(byte interesting) {
	    if (bufferIndex == bufferLast) return null;
	    byte what = interesting;

	    synchronized (buffer) {
	      int found = -1;
	      for (int k = bufferIndex; k < bufferLast; k++) {
	        if (buffer[k] == what) {
	          found = k;
	          break;
	        }
	      }
	      if (found == -1) return null;

	      int length = found - bufferIndex + 1;
	      byte outgoing[] = new byte[length];
	      System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

	      bufferIndex += length;
	      if (bufferIndex == bufferLast) {
	        bufferIndex = 0;  // rewind
	        bufferLast = 0;
	      }
	      return outgoing;
	    }
	}

	/**
	 * TODO
	 * 
	 * @param b
	 * @param buffer
	 */
	public void readBytesUntil(byte b, byte[] buffer) {
		Log.i(TAG, "Will do a.s.a.p.");
	}

	/**
	 * Returns the next byte in the buffer as a char, if nothing is there it
	 * returns -1.
	 * 
	 * @return
	 */
	public char readChar() {
		return (char) read();
	}

	/**
	 * Returns the buffer as a string.
	 * 
	 * @return
	 */
	public String readString() {
		String returnstring = new String(readBytes());
		return returnstring;
	}

	/**
	 * Returns the buffer as string until character c.
	 * 
	 * @param c
	 * @return
	 */
	public String readStringUntil(char c) {
		/* Get the buffer as string */
		String stringbuffer = readString();

		int index;
		/* Make sure that the character exists in the string */
		if ((index = stringbuffer.indexOf(c)) > 0) {
			return stringbuffer.substring(0, index);
		} else {
			return null;
		}
	}

	/**
	 * Sets the number of bytes to buffer.
	 * 
	 * @param bytes
	 * @return
	 */
	public int buffer(int bytes) {
		bufferLength = bytes;

		buffer = new byte[bytes];
		rawbuffer = buffer.clone();

		return bytes;
	}

	/**
	 * Returns the last byte in the buffer.
	 * 
	 * @return
	 */
	public int last() {
		return buffer[buffer.length - 1];
	}

	/**
	 * Returns the last byte in the buffer as char.
	 * 
	 * @return
	 */
	public char lastChar() {
		return (char) buffer[buffer.length - 1];
	}

	/**
	 * Clears the byte buffer.
	 */
	public void clear() {
		buffer = new byte[bufferLength];
	    bufferLast = 0;
	    bufferIndex = 0;
	}

	/**
	 * Disconnects the bluetooth socket.
	 * 

	 */
	public synchronized void disconnect() {
		if (connected) {
			try {
				/* Close the socket */
				mSocket.close();

				/* Set the connected state */
				connected = false;
				/* If it successfully closes I guess we just return a success? */
				//return 0;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				/* Otherwise we'll go ahead and say "no, this didn't work well!" */
				//return 1;
			}
		}
	}

	/**
	 * Kills the main thread. Shouldn't stop when the connection disconnects.
	 * 
	 * @return
	 */
	public void stop() {

	}
}

