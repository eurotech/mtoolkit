/*******************************************************************************
 * Copyright (c) 2005, 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.internal.pmp;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.Enumeration;

import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.rpc.Externalizable;

class PMPData extends ObjectInputStream {

	protected static final String[] TYPES1 = { "int", "byte", "char", "short", "long", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		"float", "double", "boolean", "void" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	protected static final String[] TYPES2 = { "java.lang.Integer", "java.lang.Byte", //$NON-NLS-1$ //$NON-NLS-2$
		"java.lang.Character", "java.lang.Short", "java.lang.Long", "java.lang.Float", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		"java.lang.Double", "java.lang.Boolean", "java.lang.Void", "java.lang.String" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	private static final String[] TYPES3 = { "I", "B", "F", "D", "S", "C", "J", "Z" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	private static final String JAVA_LANG = "java.lang"; //$NON-NLS-1$
	private static Class IS;
	private static Class EXT;
	private static Class DICT;
	static {
		IS = java.io.InputStream.class;
		EXT = Externalizable.class;
		DICT = Dictionary.class;
	}
	private static final String ERRMSG1 = "Read Error";
	private static final IOException exc = new IOException("Read Error");

	private Class clazz;
	private ClassLoader loader;

	public PMPData(InputStream is) throws IOException {
		super(is);
	}

  protected Class resolveClass(java.io.ObjectStreamClass osc) throws IOException, ClassNotFoundException {
    if (clazz.getName().equals(osc.getName())) {
      return clazz;
    }
    try {
      return loader.loadClass(osc.getName());
    } catch (ClassNotFoundException e) {
      return super.resolveClass(osc);
    }
  }

	/** writes a String in the OutputStream */
	protected static void writeString(String s, OutputStream os) throws IOException {
		if (s == null) {
      s = new String();
    }
		byte[] b = s.getBytes();
		writeInt(b.length, os);
		if (b.length > 0) {
      os.write(b);
    }
	}

	/** writes an int in the OutputStream */
	protected static void writeInt(int i, OutputStream os) throws IOException {
		os.write((i >>> 24) & 0xFF);
		os.write((i >>> 16) & 0xFF);
		os.write((i >>> 8) & 0xFF);
		os.write((i >>> 0) & 0xFF);
	}

	/** writes a long in the OutputStream */
	protected static void writeLong(long l, OutputStream os) throws IOException {
		os.write((int) (l >>> 56) & 0xFF);
		os.write((int) (l >>> 48) & 0xFF);
		os.write((int) (l >>> 40) & 0xFF);
		os.write((int) (l >>> 32) & 0xFF);
		os.write((int) (l >>> 24) & 0xFF);
		os.write((int) (l >>> 16) & 0xFF);
		os.write((int) (l >>> 8) & 0xFF);
		os.write((int) (l >>> 0) & 0xFF);
	}

	/** writes a short in the OutputStream */
	protected static void writeShort(short s, OutputStream os) throws IOException {
		os.write((s >>> 8) & 0xFF);
		os.write(s & 0xFF);
	}

	/** writes a char in the OutputStream */
	protected static void writeChar(char c, OutputStream os) throws IOException {
		os.write((c >>> 8) & 0xFF);
		os.write(c & 0xFF);
	}

	/** writes an Object in the OutputStream */
	protected static void writeObject(Object obj, PMPOutputStream os, boolean sendName) throws Exception {
		if (obj == null) {
			os.write(0);
			return;
		}
		os.write(1);
		if (obj instanceof InputStream) {
			if (sendName) {
				os.write(1);
				writeString(IS.getName(), os);
			} else {
				os.write(0);
			}
			os.write((InputStream) obj);
			return;
		}

		if (obj instanceof RemoteObjectImpl) {
			if (sendName) {
				os.write(1);
				writeString("RemoteObject", os); //$NON-NLS-1$
			} else {
				os.write(0);
			}
			writeInt(((RemoteObjectImpl) obj).IOR, os);
			return;
		}

		Class clazz = obj.getClass();
		String className = clazz.getName();
		if (sendName) {
			os.write(1);
			writeString(className, os);
		} else {
			os.write(0);
		}
		if (clazz.isArray()) {
			int size = Array.getLength(obj);
			if (size > 0) {
				if (className.length() == 2) {
					writePrimrtiveArray(className, obj, size, os);
				} else {
					className = className.substring(2, className.length() - 1);
					if (className.startsWith(JAVA_LANG) && writeLangArray(className, obj, size, os)) {
						return;
					}
					writeInt(size, os);
					for (int i = 0; i < size; i++) {
						Object difObj = Array.get(obj, i);
						if (difObj == null) {
              writeObject(difObj, os, false);
            } else if (difObj.getClass().getName().equals(className)) {
              writeObject(difObj, os, false);
            } else {
              writeObject(difObj, os, true);
            }
					}
					return;
				}
			} else {
				writeInt(0, os);
			}
		} else if (obj instanceof Externalizable) {
			try {
				((Externalizable) obj).writeObject(os);
			} catch (Exception exc) {
				throw new PMPException("Can't Serialize " + obj, exc);
			}
		} else if (obj instanceof Dictionary) {
			writeDictionary((Dictionary) obj, os);
		} else if (obj instanceof Serializable) {
			if (clazz == String.class) {
        writeString((String) obj, os);
      } else if (clazz == Long.class) {
        writeLong(((Long) obj).longValue(), os);
      } else if (clazz == Short.class) {
        writeShort(((Short) obj).shortValue(), os);
      } else if (clazz == Character.class) {
        writeChar(((Character) obj).charValue(), os);
      } else if (clazz == Byte.class) {
        os.write(((Byte) obj).byteValue());
      } else if (clazz == Float.class) {
        writeInt(Float.floatToIntBits(((Float) obj).floatValue()), os);
      } else if (clazz == Double.class) {
        writeLong(Double.doubleToLongBits(((Double) obj).doubleValue()), os);
      } else if (clazz == Boolean.class) {
        os.write(((Boolean) obj).booleanValue() ? 1 : 0);
      } else if (clazz == Integer.class) {
        writeInt(((Integer) obj).intValue(), os);
      } else {
				ObjectOutputStream oos = new ObjectOutputStream(os);
				oos.writeObject(obj);
				oos.flush();
			}
		} else {
			throw new PMPException("Don't Know How To Serialize " + obj);
		}
	}

	private static void writeDictionary(Dictionary obj, PMPOutputStream os) throws Exception {
		writeInt(obj.size(), os);
		for (Enumeration en = obj.keys(); en.hasMoreElements();) {
			Object key = en.nextElement();
			writeObject(key, os, true);
			writeObject(obj.get(key), os, true);
		}
	}

	private static void readDictionary(Dictionary obj, ClassLoader loader, int maxSize, int strLen, PMPInputStream is)
					throws Exception {
		int size = readInt(is);
		for (int i = 0; i < size; i++) {
			Object key = readObject(null, loader, is, null, maxSize, strLen, null);
			Object value = readObject(null, loader, is, null, maxSize, strLen, null);
			obj.put(key, value);
		}
	}

	private static void writePrimrtiveArray(String className, Object obj, int size, OutputStream os) throws IOException {
		writeInt(size, os);
		String type = className.substring(1, 2);
		if (type.equals(TYPES3[0])) {
			int[] iarr = (int[]) obj;
			for (int i = 0; i < size; i++) {
				writeInt(iarr[i], os);
			}
		} else if (type.equals(TYPES3[1])) {
			os.write((byte[]) obj);
		} else if (type.equals(TYPES3[2])) {
			float[] farr = (float[]) obj;
			for (int i = 0; i < size; i++) {
				writeInt(Float.floatToIntBits(farr[i]), os);
			}
		} else if (type.equals(TYPES3[3])) {
			double[] darr = (double[]) obj;
			for (int i = 0; i < size; i++) {
				writeLong(Double.doubleToLongBits(darr[i]), os);
			}
		} else if (type.equals(TYPES3[4])) {
			short[] sarr = (short[]) obj;
			for (int i = 0; i < size; i++) {
				writeShort(sarr[i], os);
			}
		} else if (type.equals(TYPES3[5])) {
			char[] carr = (char[]) obj;
			for (int i = 0; i < size; i++) {
				writeChar(carr[i], os);
			}
		} else if (type.equals(TYPES3[6])) {
			long[] larr = (long[]) obj;
			for (int i = 0; i < size; i++) {
				writeLong(larr[i], os);
			}
		} else if (type.equals(TYPES3[7])) {
			boolean[] zarr = (boolean[]) obj;
			for (int i = 0; i < size; i++) {
				os.write((zarr[i]) ? 1 : 0);
			}
		}
	}

	private static boolean writeLangArray(String className, Object obj, int size, OutputStream os) throws IOException {
		if (className.equals(TYPES2[9])) {
			writeInt(size, os);
			String[] strarr = (String[]) obj;
			for (int i = 0; i < size; i++) {
				if (strarr[i] == null) {
					os.write(0);
				} else {
					os.write(1);
					writeString(strarr[i], os);
				}
			}
		} else if (className.equals(TYPES2[0])) {
			writeInt(size, os);
			Integer[] iarr = (Integer[]) obj;
			for (int i = 0; i < size; i++) {
				if (iarr[i] == null) {
					os.write(0);
				} else {
					os.write(1);
					writeInt(iarr[i].intValue(), os);
				}
			}
		} else if (className.equals(TYPES2[4])) {
			writeInt(size, os);
			Long[] larr = (Long[]) obj;
			for (int i = 0; i < size; i++) {
				if (larr[i] == null) {
					os.write(0);
				} else {
					os.write(1);
					writeLong(larr[i].longValue(), os);
				}
			}
		} else if (className.equals(TYPES2[3])) {
			writeInt(size, os);
			Short[] sarr = (Short[]) obj;
			for (int i = 0; i < size; i++) {
				if (sarr[i] == null) {
					os.write(0);
				} else {
					os.write(1);
					writeShort(sarr[i].shortValue(), os);
				}
			}
		} else if (className.equals(TYPES2[2])) {
			writeInt(size, os);
			Character[] carr = (Character[]) obj;
			for (int i = 0; i < size; i++) {
				if (carr[i] == null) {
					os.write(0);
				} else {
					os.write(1);
					writeChar(carr[i].charValue(), os);
				}
			}
		} else if (className.equals(TYPES2[1])) {
			writeInt(size, os);
			Byte[] barr = (Byte[]) obj;
			for (int i = 0; i < size; i++) {
				if (barr[i] == null) {
					os.write(0);
				} else {
					os.write(1);
					os.write(barr[i].byteValue());
				}
			}
		} else if (className.equals(TYPES2[5])) {
			writeInt(size, os);
			Float[] farr = (Float[]) obj;
			for (int i = 0; i < size; i++) {
				if (farr[i] == null) {
					os.write(0);
				} else {
					os.write(1);
					writeInt(Float.floatToIntBits(farr[i].floatValue()), os);
				}
			}
		} else if (className.equals(TYPES2[6])) {
			writeInt(size, os);
			Double[] darr = (Double[]) obj;
			for (int i = 0; i < size; i++) {
				if (darr[i] == null) {
					os.write(0);
				} else {
					os.write(1);
					writeLong(Double.doubleToLongBits(darr[i].doubleValue()), os);
				}
			}
		} else if (className.equals(TYPES2[7])) {
			writeInt(size, os);
			Boolean[] zarr = (Boolean[]) obj;
			for (int i = 0; i < size; i++) {
				if (zarr[i] == null) {
					os.write(0);
				} else {
					os.write(1);
					os.write(zarr[i].booleanValue() ? 1 : 0);
				}
			}
		} else {
      return false;
    }
		return true;
	}

	private static final String ERRMSG_LS = "Can't Read Strings Longer Than ";

	/** reads a String from the InputStream */
	protected static String readString(InputStream is, int maxLength) throws IOException {
		int strlen = readInt(is);
		if (strlen == 0) {
      return new String();
    }
		if (maxLength > 0 && strlen > maxLength) {
			String errMsg = ERRMSG_LS + maxLength;
			throw new IOException(errMsg);
		}
		if (strlen > 0) {
			byte[] b = new byte[strlen];
			int len = 0;
			int tmp = 0;
			while (len < strlen) {
				tmp = is.read(b, len, strlen - len);
				if (tmp == -1) {
					throw new IOException(ERRMSG1);
				}
				len += tmp;
			}
			return new String(b);
		}
		String errMsg = "Negative String Size";
		throw new IOException(errMsg);
	}

	/** reads an int from the InputStream */
	protected static int readInt(InputStream is) throws IOException {
		int ch1 = is.read();
		int ch2 = is.read();
		int ch3 = is.read();
		int ch4 = is.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0) {
			throw new IOException(ERRMSG1);
		}
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	private static boolean checkFlag(InputStream is) throws IOException {
		int nullflag = is.read();
		if (nullflag == -1) {
      throw new IOException(ERRMSG1);
    }
		return (nullflag == 1);
	}

	protected static Object readObject(Class clazz, ClassLoader loader, PMPInputStream is, String newName, int maxSize,
					int strLen, String prevName) throws IOException {
		if (!checkFlag(is)) {
      return null;
    }
		String className = new String();
		try {
			if (checkFlag(is)) {
				className = readString(is, strLen);
			} else {
				className = prevName;
			}
			if (newName != null) {
        if (newName.length() != 0) {
					className = newName;
				}
      }
		} catch (IOException ioExc) {
			throw ioExc;
		}
		if (className.equals("RemoteObject")) { //$NON-NLS-1$
			return new RemoteObjectImpl(readInt(is), is.c.getConnection());
		}
		if (className.startsWith("[")) { //$NON-NLS-1$
			int size = readInt(is);
			if (maxSize > 0 && size > maxSize) {
				throw new IOException("Can't Read Arrays Longer Than " + maxSize);
			}
			className = className.substring(1, className.length());
			if (className.length() == 1) {
				return readPrimitiveArray(className, size, is);
			}
			className = className.substring(1, className.length() - 1);
			if (className.startsWith(JAVA_LANG)) {
				Object arr = readLangArray(className, size, is, strLen);
				if (arr != null) {
          return arr;
        }
			}
			if (clazz == null) {
				if (loader != null) {
					try {
						clazz = loader.loadClass(className);
					} catch (Throwable exc) {
						clazz = null;
					}
				}
			}
			if (clazz == null) {
				loader = PMPServiceImpl.loader;
				try {
					clazz = loader.loadClass(className);
				} catch (Throwable exc) {
					clazz = null;
				}
			}
			if (clazz == null) {
				throw new IOException("Can't resolve class " + className);
			}
			Object oArr = Array.newInstance(clazz, size);
			for (int i = 0; i < size; i++) {
				Object next = null;
				if (newName != null) {
					if (newName.length() != 0) {
						next = readObject(clazz, loader, is, className, maxSize, strLen, className);
					} else {
						next = readObject(null, loader, is, new String(), maxSize, strLen, className);
					}
				} else {
					next = readObject(null, loader, is, new String(), maxSize, strLen, className);
				}
				Array.set(oArr, i, next);
			}
			return oArr;
		}
		if (className.equals(TYPES2[9])) {
			return readString(is, strLen);
		}

		if (className.equals(TYPES1[0]) || className.equals(TYPES2[0])) {
			return new Integer(readInt(is));
		}
		if (className.equals(TYPES1[1]) || className.equals(TYPES2[1])) {
			int b = is.read();
			if (b == -1) {
        throw new IOException(ERRMSG1);
      }
			return new Byte((byte) b);
		}
		if (className.equals(TYPES1[2]) || className.equals(TYPES2[2])) {
			return new Character(readChar(is));
		}
		if (className.equals(TYPES1[3]) || className.equals(TYPES2[3])) {
			return new Short(readShort(is));
		}
		if (className.equals(TYPES1[4]) || className.equals(TYPES2[4])) {
			return new Long(readLong(is));
		}
		if (className.equals(TYPES1[5]) || className.equals(TYPES2[5])) {
			return new Float(Float.intBitsToFloat(readInt(is)));
		}
		if (className.equals(TYPES1[6]) || className.equals(TYPES2[6])) {
			return new Double(Double.longBitsToDouble(readLong(is)));
		}
		if (className.equals(TYPES1[7]) || className.equals(TYPES2[7])) {
			int b = is.read();
			if (b == -1) {
        throw new IOException(ERRMSG1);
      }
			return b == 1 ? Boolean.TRUE : Boolean.FALSE;
		}

		if (clazz == null) {
			if (loader != null) {
				try {
					clazz = loader.loadClass(className);
				} catch (Exception exc) {
					clazz = null;
				}
			}
		}
		if (clazz == null) {
			loader = PMPServiceImpl.loader;
			try {
				clazz = loader.loadClass(className);
			} catch (Exception exc) {
				clazz = null;
			}
		}
		if (clazz == null) {
			throw new IOException("Can't resolve class " + className);
		}
		if (IS.isAssignableFrom(clazz)) {
			return new FileReader(is);
		}
		try {
			if (EXT.isAssignableFrom(clazz)) {
				Externalizable obj = (Externalizable) clazz.newInstance();
				obj.readObject(is);
				return obj;
			}
			if (DICT.isAssignableFrom(clazz)) {
				Dictionary obj = (Dictionary) clazz.newInstance();
				readDictionary(obj, loader, maxSize, strLen, is);
				return obj;
			}
			PMPData ois = new PMPData(is);
			ois.clazz = clazz;
			ois.loader = loader;
			return ois.readObject();
		} catch (Exception exc) {
			throw exc instanceof IOException ? (IOException) exc : new IOException(exc.toString());
		}

	}

	/** reads a primitive array (int[], byte[], etc.) from the InputStream */
	private static Object readPrimitiveArray(String className, int size, InputStream is) throws IOException {
		if (className.equals("I")) { //$NON-NLS-1$
			int[] iarr = new int[size];
			for (int i = 0; i < size; i++) {
				iarr[i] = readInt(is);
			}
			return iarr;
		} else if (className.equals("B")) { //$NON-NLS-1$
			byte[] barr = new byte[size];
			int read = 0;
			int tmp = 0;
			while (read < size) {
				tmp = is.read(barr, read, size - read);
				if (tmp == -1) {
          throw exc;
        }
				read += tmp;
			}
			return barr;
		} else if (className.equals("S")) { //$NON-NLS-1$
			short[] sarr = new short[size];
			for (int i = 0; i < size; i++) {
				sarr[i] = readShort(is);
			}
			return sarr;
		} else if (className.equals("J")) { //$NON-NLS-1$
			long[] larr = new long[size];
			for (int i = 0; i < size; i++) {
				larr[i] = readLong(is);
			}
			return larr;
		} else if (className.equals("C")) { //$NON-NLS-1$
			char[] carr = new char[size];
			for (int i = 0; i < size; i++) {
				carr[i] = readChar(is);
			}
			return carr;
		} else if (className.equals("F")) { //$NON-NLS-1$
			float[] farr = new float[size];
			for (int i = 0; i < size; i++) {
				farr[i] = Float.intBitsToFloat(readInt(is));
			}
			return farr;
		} else if (className.equals("D")) { //$NON-NLS-1$
			double[] darr = new double[size];
			for (int i = 0; i < size; i++) {
				darr[i] = Double.longBitsToDouble(readLong(is));
			}
			return darr;
		} else if (className.equals("Z")) { //$NON-NLS-1$
			boolean[] zarr = new boolean[size];
			for (int i = 0; i < size; i++) {
				int b = is.read();
				if (b == -1) {
          throw exc;
        }
				zarr[i] = b == 1;
			}
			return zarr;
		} else {
			throw new IOException("Unknown Primitive Array Type: " + className);
		}
	}

	/**
	 * reads an array of wrapper class instancies (Integer[], Byte[], etc.) from
	 * the InputStream
	 */
	private static Object readLangArray(String className, int size, InputStream is, int len) throws IOException {
		if (className.equals(TYPES2[9])) {
			String[] strarr = new String[size];
			for (int i = 0; i < size; i++) {
				if (checkFlag(is)) {
					strarr[i] = readString(is, len);
				}
			}
			return strarr;
		} else if (className.equals(TYPES2[0])) {
			Integer[] iarr = new Integer[size];
			for (int i = 0; i < size; i++) {
				if (checkFlag(is)) {
					iarr[i] = new Integer(readInt(is));
				}
			}
			return iarr;
		} else if (className.equals(TYPES2[1])) {
			Byte[] barr = new Byte[size];
			for (int i = 0; i < size; i++) {
				if (checkFlag(is)) {
					int b = (byte) is.read();
					if (b == -1) {
            throw new IOException(ERRMSG1);
          }
					barr[i] = new Byte((byte) b);
				}
			}
			return barr;
		} else if (className.equals(TYPES2[2])) {
			Character[] carr = new Character[size];
			for (int i = 0; i < size; i++) {
				if (checkFlag(is)) {
					carr[i] = new Character(readChar(is));
				}
			}
			return carr;
		} else if (className.equals(TYPES2[3])) {
			Short[] sarr = new Short[size];
			for (int i = 0; i < size; i++) {
				if (checkFlag(is)) {
					sarr[i] = new Short(readShort(is));
				}
			}
			return sarr;
		} else if (className.equals(TYPES2[4])) {
			Long[] larr = new Long[size];
			for (int i = 0; i < size; i++) {
				if (checkFlag(is)) {
					larr[i] = new Long(readLong(is));
				}
			}
			return larr;
		} else if (className.equals(TYPES2[5])) {
			Float[] farr = new Float[size];
			for (int i = 0; i < size; i++) {
				if (checkFlag(is)) {
					farr[i] = new Float(Float.intBitsToFloat(readInt(is)));
				}
			}
			return farr;
		} else if (className.equals(TYPES2[6])) {
			Double[] darr = new Double[size];
			for (int i = 0; i < size; i++) {
				if (checkFlag(is)) {
					darr[i] = new Double(Double.longBitsToDouble(readLong(is)));
				}
			}
			return darr;
		} else if (className.equals(TYPES2[7])) {
			Boolean[] zarr = new Boolean[size];
			for (int i = 0; i < size; i++) {
				if (checkFlag(is)) {
					int b = is.read();
					if (b == -1) {
            throw new IOException(ERRMSG1);
          }
					zarr[i] = b == 1 ? Boolean.TRUE : Boolean.FALSE;
				}
			}
			return zarr;
		} else {
      return null;
    }
	}

	/** reads a char from the InputStream */
	protected static char readChar(InputStream is) throws IOException {
		int ch1 = is.read();
		int ch2 = is.read();
		if ((ch1 | ch2) < 0) {
      throw new IOException(ERRMSG1);
    }
		return (char) ((ch1 << 8) + (ch2 << 0));
	}

	/** reads a short from the InputStream */
	protected static short readShort(InputStream is) throws IOException {
		int ch1 = is.read();
		int ch2 = is.read();
		if ((ch1 | ch2) < 0) {
      throw new IOException(ERRMSG1);
    }
		return (short) ((ch1 << 8) + (ch2 << 0));
	}

	/** reads a long from the InputStream */
	protected static long readLong(InputStream is) throws IOException {
		return ((long) (readInt(is)) << 32) + (readInt(is) & 0xFFFFFFFFL);
	}

}
