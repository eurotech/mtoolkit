/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 ****************************************************************************/
package org.tigris.mtoolkit.iagent.internal.tcp;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * General data formater. 
 *
 * @author ivand
 * @version 1.1
 */
public class DataFormater {

  /**
   * Define IDs of basic symple types in message protocol.
   */
  public static final byte ID_C_CHAR = 0x01;
  public static final byte ID_C_SHORT = 0x02;
  public static final byte ID_C_INT = 0x03;
  public static final byte ID_C_LONG = 0x04;
  public static final byte ID_C_BYTE = 0x05;
  public static final byte ID_C_WORD = 0x06;
  public static final byte ID_C_DWORD = 0x07;
  public static final byte ID_C_QWORD = 0x08;
  public static final byte ARRAY_FLAG = (byte)0x80;

  /**
   * Define size in bytes of basic symple types in message protocol.
   */
  public static final int CHAR_SIZE = 1;
  public static final int SHORT_SIZE = 2;
  public static final int INT_SIZE = 4;
  public static final int LONG_SIZE = 8;
  public static final int BYTE_SIZE = 1;
  public static final int WORD_SIZE = 2;
  public static final int DWORD_SIZE = 4;
  public static final int QWORD_SIZE = 8;

  /**
   * Get mBSA type char/byte as Java byte type
   * 
   * @param is stream to read dat from
   * @return read byte
   * @throws IOException in case of read error
   */
  public static byte getByte(InputStream is) throws IOException {
    int byte0 = is.read();
    if ( byte0 < 0 ) {
      throw new EOFException();
    }
    return (byte)byte0;
  }

  /**
   * Get mBSA type char/byte[] as Java byte type[]
   * 
   * @param is stream to read dat from
   * @return read byte[]
   * @throws IOException in case of read error
   */
  public static byte[] getByteArray(InputStream is) throws IOException {
    int expectedLenght = getInt(is);
    byte[] array = new byte[expectedLenght];
    if (expectedLenght > 0) {
      int actualLenght = 0;
      int res = -1;
      while (actualLenght != expectedLenght) {
        res = is.read(array, actualLenght, expectedLenght - actualLenght);
        if (res == -1) {
          throw new EOFException("The actual data length is " + actualLenght
              + ", but the expected data length is " + expectedLenght + "!");
        }
        actualLenght += res;
      }
    }
    return array;
  }  

  /**
   * Appends java byte as mBSA byte type
   * 
   * @param os output stream to append to
   * @param data the byte which is needed to be appended
   * @throws IOException in case of output stream write error
   */
  public static void appendByte(OutputStream os, byte value) throws IOException {
    os.write(value);
  }

  /**
   * Appends java byte[] as mBSA byte[] type
   * 
   * @param os output stream to append to
   * @param array the byte[] which is needed to be appended
   * @throws IOException in case of output stream write error
   */
  public static void appendByteArray(OutputStream os, byte[] array) throws IOException {
    int len = array == null ? 0 : array.length;
    appendInt(os, len);
    if (len > 0) {
      os.write(array);
    }
  }

  public static void appendByteArray(OutputStream os, byte[] array, int off, int len) throws IOException {
    int l_len = array == null ? 0 : len;
    appendInt(os, l_len);
    if (l_len > 0) {
      os.write(array, off, len);
    }
  }  


  /**
   * Get mBSA type int/word as Java type int
   * 
   * @param is stram to read data from
   * @return read int
   * @throws IOException in case of read error
   */
  public static int getInt(InputStream is) throws IOException {
    int byte0 = is.read();
    int byte1 = is.read();
    int byte2 = is.read();
    int byte3 = is.read();
    if ((byte0 | byte1 | byte2 | byte3) < 0)
      throw new EOFException();
    return ((byte0&0xFF) << 24) | ((byte1&0xFF) << 16) | ((byte2&0xFF) << 8) | (byte3&0xFF); 
  }

  /**
   * Get mBSA type int/word[] as Java type int[]
   * 
   * @param is stram to read data from
   * @return read int[]
   * @throws IOException in case of read error
   */
  public static int[] getIntArray(InputStream is) throws IOException {
    int[] array = new int[getInt(is)];
    for (int i = 0; i < array.length; i++) {
      array[i] = getInt(is);
    }
    return array;
  }

  /**
   * Appends Java int as mBSA int/word 
   * 
   * @param os stream to write data in 
   * @param value the integer needed to be appended
   * @throws IOException in case of write error
   */
  public static void appendInt(OutputStream os, int value) throws IOException {
    os.write((value & 0xFF000000) >> 24);
    os.write((value & 0xFF0000) >> 16);
    os.write((value & 0xFF00) >> 8);
    os.write(value & 0xFF);
  }

  /**
   * Appends Java int[] as mBSA int/word[] 
   * 
   * @param os stream to write data in 
   * @param array the integer[] needed to be appended
   * @throws IOException in case of write error
   */
  public static void appendIntArray(OutputStream os, int[] array) throws IOException {    
    int len = array == null ? 0 : array.length;
    appendInt(os, len);
    if (array != null) {
      for (int i = 0; i < len; i++) {
        appendInt(os, array[i]);
      }
    }
  }  

  /**
   * Get mBSA type short as Java short type
   * 
   * @param is sream to read data from
   * @return read short
   */
  public static short getShort(InputStream is) throws IOException {
    int byte0 = is.read();
    int byte1 = is.read();
    if ((byte0 | byte1) < 0) {
      throw new EOFException();
    }
    return (short)(((byte0&0xFF) << 8) | (byte1&0xFF)); 
  }

  /**
   * Get mBSA type short[] as Java short[] type
   * 
   * @param is sream to read data from
   * @return read short[]
   */
  public static short[] getShortArray(InputStream is) throws IOException {
    short[] array = new short[getInt(is)];
    for (int i = 0; i < array.length; i++) {
      array[i] = getShort(is);
    }
    return array;
  }  

  /**
   * Append Java short as mBSA short
   * 
   * @param os stram to write data in
   * @param value the short needed to be appended
   * @throws IOException in case of write error
   */
  public static void appendShort(OutputStream os, short value) throws IOException {
    os.write((value & 0xFF00) >> 8);
    os.write(value & 0xFF);
  }

  /**
   * Append Java short[] as mBSA short[]
   * 
   * @param os stram to write data in
   * @param array the short[] needed to be appended
   * @throws IOException in case of write error
   */
  public static void appendShortArray(OutputStream os, short[] array) throws IOException {   
    int len = array == null ? 0 : array.length;
    appendInt(os, len);
    if (array != null) {
      for (int i = 0; i < len; i++) {
        appendShort(os, array[i]);
      }
    }    
  }  

  /**
   * Get mBSA type long/qword as Java long type
   * 
   * @param is stream to read data from
   * @return read long
   */
  public static long getLong(InputStream is) throws IOException {
    int byte0 = is.read();
    int byte1 = is.read();
    int byte2 = is.read();
    int byte3 = is.read();
    int byte4 = is.read();
    int byte5 = is.read();
    int byte6 = is.read();
    int byte7 = is.read();
    if ((byte0 | byte1 | byte2 | byte3 | byte4 | byte5 | byte6 | byte7) < 0)
      throw new EOFException();
    return ((byte0&0xFFL) << 56) | ((byte1&0xFFL) << 48) | ((byte2&0xFFL) << 40) | ((byte3&0xFFL) << 32) | ((byte4&0xFFL) << 24) | ((byte5&0xFFL) << 16) | ((byte6&0xFFL) << 8) | (byte7&0xFFL); 
  }

  /**
   * Get mBSA type long/qword[] as Java long[] type
   * 
   * @param is stream to read data from
   * @return read long[]
   */
  public static long[] getLongArray(InputStream is) throws IOException {
    long[] array = new long[getInt(is)];
    for (int i = 0; i < array.length; i++) {
      array[i] = getLong(is);
    }
    return array;
  }  

  /**
   * Append long as mBSA long
   * 
   * @param os stram to write data in
   * @param value long vallue needed to bea appended
   * @throws IOException in case of write error
   */
  public static void appendLong(OutputStream os, long value) throws IOException {
    os.write((int)((value & 0xFF00000000000000L) >> 56));
    os.write((int)((value & 0xFF000000000000L) >> 48));
    os.write((int)((value & 0xFF0000000000L) >> 40));
    os.write((int)((value & 0xFF00000000L) >> 32));
    os.write((int)((value & 0xFF000000L) >> 24));
    os.write((int)((value & 0xFF0000L) >> 16));
    os.write((int)((value & 0xFF00L) >> 8));
    os.write((int)(value & 0xFFL));
  }

  /**
   * Append long as mBSA long[]
   * 
   * @param os stram to write data in
   * @param array long[] vallue needed to bea appended
   * @throws IOException in case of write error
   */
  public static void appendLongArray(OutputStream os, long[] array) throws IOException {
    int len = array == null ? 0 : array.length;
    appendInt(os, len);
    if (array != null) {
      for (int i = 0; i < len; i++) {
        appendLong(os, array[i]);
      }
    }
  }

  /**
   * First writes ID_C_BYTE and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeByte(OutputStream os, byte value) throws IOException {
    appendByte(os, ID_C_BYTE);
    appendByte(os, value);
  }

  /**
   * First reads ID_C_BYTE and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static byte readByte(InputStream is) throws IOException {
    if (getByte(is) != ID_C_BYTE) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString(ID_C_BYTE));
    }
    return getByte(is);
  }

  /**
   * First writes ID_C_BYTE | ARRAY_FLAG array size and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeByteArray(OutputStream os, byte[] value) throws IOException {
    appendByte(os, (byte)(ID_C_BYTE | ARRAY_FLAG));
    appendByteArray(os, value);
  }

  /**
   * First writes ID_C_BYTE | ARRAY_FLAG array size and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeByteArray(OutputStream os, byte[] value, int off, int len) throws IOException {
    appendByte(os, (byte)(ID_C_BYTE | ARRAY_FLAG));
    appendByteArray(os, value, off, len);
  }  


  /**
   *  First reads ID_C_BYTE | ARRAY_FLAG array size and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static byte[] readByteArray(InputStream is) throws IOException {
    if (getByte(is) != (byte)(ID_C_BYTE | ARRAY_FLAG)) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString((byte)(ID_C_BYTE | ARRAY_FLAG)));
    }
    return getByteArray(is);
  }

  /**
   * First writes ID_C_SHORT and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeShort(OutputStream os, short value) throws IOException {
    appendByte(os, ID_C_SHORT);
    appendShort(os, value);
  }

  /**
   * First reads ID_C_SHORT and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static short readShort(InputStream is) throws IOException {
    if (getByte(is) != ID_C_SHORT) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString(ID_C_SHORT));
    }
    return getShort(is);
  }

  /**
   * First writes ID_C_SHORT | ARRAY_FLAG array size and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeShortArray(OutputStream os, short[] value) throws IOException {
    appendByte(os, (byte)(ID_C_SHORT | ARRAY_FLAG));
    appendShortArray(os, value);
  }  

  /**
   * First reads ID_C_SHORT | ARRAY_FLAG array size and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static short[] readShortArray(InputStream is) throws IOException {
    if (getByte(is) != (byte)(ID_C_SHORT | ARRAY_FLAG)) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString((byte)(ID_C_SHORT | ARRAY_FLAG)));
    }
    return getShortArray(is);
  }  

  /**
   * First writes ID_C_WORD and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeWord(OutputStream os, short value) throws IOException {
    appendByte(os, ID_C_WORD);
    appendShort(os, value);
  }

  /**
   * First reads ID_C_WORD and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static short readWord(InputStream is) throws IOException {
    if (getByte(is) != ID_C_WORD) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString(ID_C_WORD));
    }
    return getShort(is);
  }

  /**
   * First writes ID_C_WORD | ARRAY_FLAG array size and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeWordArray(OutputStream os, short[] value) throws IOException {
    appendByte(os, (byte)(ID_C_WORD | ARRAY_FLAG));
    appendShortArray(os, value);
  }  

  /**
   * First reads ID_C_WORD | ARRAY_FLAG array size and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static short[] readWordArray(InputStream is) throws IOException {
    if (getByte(is) != (byte)(ID_C_WORD | ARRAY_FLAG)) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString((byte)(ID_C_WORD | ARRAY_FLAG)));
    }
    return getShortArray(is);
  }  

  /**
   * First writes ID_C_INT and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeInt(OutputStream os, int value) throws IOException {
    appendByte(os, ID_C_INT);
    appendInt(os, value);
  }

  /**
   * First reads ID_C_INT and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static int readInt(InputStream is) throws IOException {
    if (getByte(is) != ID_C_INT) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString(ID_C_INT));
    }
    return getInt(is);
  }  

  /**
   * First writes ID_C_INT | ARRAY_FLAG array size and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeIntArray(OutputStream os, int[] value) throws IOException {
    appendByte(os, (byte)(ID_C_INT | ARRAY_FLAG));
    appendIntArray(os, value);
  }  

  /**
   * First reads ID_C_INT | ARRAY_FLAG array size and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static int[] readIntArray(InputStream is) throws IOException {
    if (getByte(is) != (byte)(ID_C_INT | ARRAY_FLAG)) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString((byte)(ID_C_INT | ARRAY_FLAG)));
    }
    return getIntArray(is);
  }  

  /**
   * First writes ID_C_DWORD and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeDWord(OutputStream os, int value) throws IOException {
    appendByte(os, ID_C_DWORD);
    appendInt(os, value);
  }

  /**
   * First reads ID_C_DWORD and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static int readDWord(InputStream is) throws IOException {
    if (getByte(is) != ID_C_DWORD) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString(ID_C_DWORD));
    }
    return getInt(is);
  }  

  /**
   * First writes ID_C_DWORD | ARRAY_FLAG array size and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeDWordArray(OutputStream os, int[] value) throws IOException {
    appendByte(os, (byte)(ID_C_DWORD | ARRAY_FLAG));
    appendIntArray(os, value);
  }  

  /**
   * First reads ID_C_DWORD | ARRAY_FLAG array size and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static int[] readDWordArray(InputStream is) throws IOException {
    if (getByte(is) != (byte)(ID_C_DWORD | ARRAY_FLAG)) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString((byte)(ID_C_DWORD | ARRAY_FLAG)));
    }
    return getIntArray(is);
  } 

  /**
   * First writes ID_C_LONG and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeLong(OutputStream os, long value) throws IOException {
    appendByte(os, ID_C_LONG);
    appendLong(os, value);
  }

  /**
   * First reads ID_C_LONG and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static long readLong(InputStream is) throws IOException {
    if (getByte(is) != ID_C_LONG) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString(ID_C_LONG));
    }
    return getLong(is);
  }  

  /**
   * First writes ID_C_LONG | ARRAY_FLAG array size and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeLongArray(OutputStream os, long[] value) throws IOException {
    appendByte(os, (byte)(ID_C_LONG | ARRAY_FLAG));
    appendLongArray(os, value);
  }  

  /**
   * First reads ID_C_LONG | ARRAY_FLAG array size and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static long[] readLongArray(InputStream is) throws IOException {
    if (getByte(is) != (byte)(ID_C_LONG | ARRAY_FLAG)) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString((byte)(ID_C_LONG | ARRAY_FLAG)));
    }
    return getLongArray(is);
  }

  /**
   * First writes ID_C_QWORD and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeQWord(OutputStream os, long value) throws IOException {
    appendByte(os, ID_C_QWORD);
    appendLong(os, value);
  }

  /**
   * First reads ID_C_QWORD and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static long readQWord(InputStream is) throws IOException {
    if (getByte(is) != ID_C_QWORD) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString(ID_C_QWORD));
    }
    return getLong(is);
  }  

  /**
   * First writes ID_C_QWORD | ARRAY_FLAG array size and then the value.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeQWordArray(OutputStream os, long[] value) throws IOException {
    appendByte(os, (byte)(ID_C_QWORD | ARRAY_FLAG));
    appendLongArray(os, value);
  }  

  /**
   * First reads ID_C_QWORD | ARRAY_FLAG array size and then the value.
   * 
   * @param is
   * @throws IOException
   */
  public static long[] readQWordArray(InputStream is) throws IOException {
    if (getByte(is) != (byte)(ID_C_QWORD | ARRAY_FLAG)) {
      throw new IOException("Expects data type: 0x" + Integer.toHexString((byte)(ID_C_QWORD | ARRAY_FLAG)));
    }
    return getLongArray(is);
  }

  /**
   * Writes string as byte array in UTF8.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeString(OutputStream os, String value) throws IOException {
    byte[] utf8;
    if (value == null || value.length() == 0) {
      utf8 = null;
    } else {
      utf8 = value.getBytes("UTF8");
    }
    writeByteArray(os, utf8);
  }

  /**
   * String array is represend as ID_C_INT numbers of strings and following UTF8 byte arrays.
   * 
   * @param os
   * @param value
   * @throws IOException
   */
  public static void writeStringArray(OutputStream os, String[] value) throws IOException {
    int len = value == null ? 0 : value.length;
    writeInt(os, len);
    if (value != null) {
      for (int i = 0; i < len; i++) {
        writeString(os, value[i]);
      }
    }
  }   

  /**
   * Reads string as byte array in UTF8.
   * 
   * @param is
   * @throws IOException
   */
  public static String readString(InputStream is) throws IOException {
    byte[] utf8 = readByteArray(is);
    if (utf8 == null || utf8.length == 0) {
      return "";
    } else { 
      return new String(utf8, "UTF8");
    }
  }

  /**
   * First reads ID_C_INT which is number of strings followd and then
   * UTF8 byte arrays.
   * 
   * @param is
   * @throws IOException
   */
  public static String[] readStringArray(InputStream is) throws IOException {
    int len = readInt(is);
    String[] arr = new String[len];
    for (int i = 0; i < len; i++) {
      arr[i] = readString(is);
    }
    return arr;
  }

  public static final void closeOutputStream(OutputStream os) {
    if ( os != null ) {
      try {
        os.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  public static final void closeInputStream(InputStream is) {
    if ( is != null ) {
      try {
        is.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }  
}

