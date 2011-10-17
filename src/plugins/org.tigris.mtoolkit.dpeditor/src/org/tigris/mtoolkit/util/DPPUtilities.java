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
package org.tigris.mtoolkit.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.tigris.mtoolkit.common.PluginUtilities;

public class DPPUtilities {

	private static final String POINT_STRING = "."; //$NON-NLS-1$
	public static final String INSTALLED_PLATFORM = System.getProperty("os.name");
	public static final char[] INVALID_RESOURCE_CHARACTERS;
	public static final String[] INVALID_RESOURCE_NAMES;

	public static boolean DEBUG = Boolean.getBoolean("dpeditor.debug");

	static {
		char[] chars = null;
		String[] names = null;
		if (System.getProperty("os.name", "windows").toLowerCase().indexOf("win") != -1) {
			// taken from
			// http://support.microsoft.com/support/kb/articles/q177/5/06.asp
			chars = new char[] { '"', '*', '/', ':', '<', '>', '?', '\\', '|' };
			// list taken from
			// http://support.microsoft.com/support/kb/articles/Q216/6/54.ASP
			names = new String[] { "aux", "clock$", "com1", "com2", "com3", "com4", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"com5", "com6", "com7", "com8", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"com9", "con", "lpt1", "lpt2", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"lpt3", "lpt4", "lpt5", "lpt6", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"lpt7", "lpt8", "lpt9", "nul", "prn" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		} else {
			// only front slash and null char are invalid on UNIXes
			// taken from
			// http://www.faqs.org/faqs/unix-faq/faq/part2/section-2.html
			// backslash and colon are illegal path segments regardless of
			// filesystem.
			chars = new char[] { '\0', '/', ':', '\\' };
		}
		INVALID_RESOURCE_CHARACTERS = chars == null ? new char[0] : chars;
		INVALID_RESOURCE_NAMES = names == null ? new String[0] : names;
	}

	public static void debug(String s) {
		if (DEBUG) {
			System.out.println("[DPEditor] " + s);
		}
	}

	public static void debug(String s, Throwable t) {
		if (DEBUG) {
			System.out.println("[DPEditor] " + s);
			t.printStackTrace();
		}
	}

	public static Vector getVectorFromString(String str, String separator) {
		Vector result = new Vector();
		if ((str == null) || (str.equals(""))) {
			return result;
		}
		StringTokenizer tokens = new StringTokenizer(str, separator);
		while (tokens.hasMoreElements()) {
			String next = tokens.nextToken();
			result.addElement(next.trim());
		}
		return result;
	}

	/**
	 * Checks if given package name is correct package name and can be set as a
	 * package.
	 * 
	 * @param packageName
	 *            the package name, which will be tested.
	 * @return <code>true</code> if the package name may be the package of java
	 *         file, <code>false</code> otherwise.
	 */
	public static boolean isCorrectPackage(String packageName) {
		boolean correct = true;
		StringTokenizer dotTokenizer = new StringTokenizer(packageName, POINT_STRING);
		int tokensChecked = 0;
		while (correct && dotTokenizer.hasMoreTokens()) {
			String token = dotTokenizer.nextToken();
			int length = token.length();
			if (length != token.trim().length()) {
				return false;
			}
			if (token.indexOf(" ") != -1) {
				return false;
			}
			correct = isCorrectIdentifier(token.trim());
			tokensChecked++;
		}
		correct = correct && (tokensChecked == numberOfRepetitions(packageName, '.') + 1);
		return correct;
	}

	/**
	 * Determines if the specified name characters may be part of a Java
	 * identifier as other than the first character.
	 * 
	 * @param name
	 *            the name, which character will be tested.
	 * @return <code>true</code> if the name may be part of a Java identifier;
	 *         <code>false</code> otherwise.
	 */
	public static boolean isCorrectIdentifier(String name) {
		boolean returnFlag = true;
		boolean flag = false;
		if (name.length() <= 0) {
			return false;
		}
		for (int i = 1; i < name.length(); i++) {
			if (!Character.isJavaIdentifierPart(name.charAt(i))) {
				flag = true;
				break;
			}
		}
		if (!Character.isJavaIdentifierStart(name.charAt(0)) || flag) {
			returnFlag = false;
		}
		return returnFlag;
	}

	/**
	 * @return how many times does c meet in s
	 * @param s
	 *            - the string to count in
	 * @param c
	 *            - the character to be counted
	 */
	public static int numberOfRepetitions(String s, char c) {
		int counts = 0;
		String check = s;
		int len = check.length();
		int firstInd;
		while ((firstInd = check.indexOf(c)) != -1) {
			counts++;
			if (firstInd == len)
				break;
			check = check.substring(firstInd + 1);
		}
		return counts;
	}

	/**
	 * Checks if the given file path is correct file name
	 * 
	 * @param f
	 *            the file path
	 * @return <code>true</code> if the given file name is valid file name,
	 *         <code>false</code> otherwise
	 */
	public static boolean isCorrectFileName(String f) {
		int lastDot = f.lastIndexOf('*');
		if (lastDot > -1)
			return false;
		lastDot = f.lastIndexOf('.');
		if (lastDot > -1) {
			int indSlash_1 = f.indexOf('/');
			if (indSlash_1 > -1) {
				if (f.indexOf('\\') > -1) {
					return false;
				}
			}
			File file = new File(f);
			boolean result = file.isFile();
			if (!result) {
				try {
					File parent = file.getParentFile();
					if (parent != null)
						parent.mkdirs();
					result = file.createNewFile();
				} catch (IOException e) {
					result = false;
				}
				if (result) {
					File parentF = file.getParentFile();
					file.delete();
					while (parentF != null) {
						String[] list = parentF.list();
						File tmp = parentF.getParentFile();
						if (list.length == 0) {
							parentF.delete();
						} else {
							break;
						}
						parentF = tmp;
					}
				}
			}
			return result;
		}
		return isCorrectFileName(f + ".");
	}

	/**
	 * Replace in given <code>String</code> all appearance of the one string to
	 * the other one.
	 * 
	 * @param source
	 *            the <code>String</code> in which be made replacement
	 * @param toReplace
	 *            the text will be replace
	 * @param newString
	 *            the new text that will be set in the string
	 * @return the <code>String</code> with the replace string
	 */
	public static String replaceString(String source, String toReplace, String newString) {
		if (source == null)
			return null;
		StringBuffer buf = new StringBuffer();
		int inx = source.indexOf(toReplace);
		int oldinx = 0;
		while (inx >= 0) {
			buf.append(source.substring(oldinx, inx));
			buf.append(newString);
			oldinx = inx + toReplace.length();
			inx = source.indexOf(toReplace, oldinx);
		}
		buf.append(source.substring(oldinx));
		return buf.toString();
	}

	/**
	 * Validate specified version name for correctness
	 * 
	 * @param versionName
	 * @return
	 */
	public static boolean isValidVersion(String versionName) {
		try {
			new Version(versionName);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	/**
	 * Validate specified fix pack range for correctness
	 * 
	 * @param fixPack
	 * @return
	 */
	public static boolean isValidFixPack(String fixPack) {
		return isValidVersionRange(fixPack);
	}

	/**
	 * Validate specified cersions range for correctness
	 * 
	 * @param interval
	 *            - interval or single version
	 * @return
	 */
	public static boolean isValidVersionRange(String interval) {
		if (isValidVersion(interval))
			return true;
		char first = interval.charAt(0);
		if (first != '[' && first != '(') {
			return false;
		}
		char last = interval.charAt(interval.length() - 1);
		if (last != ']' && last != ')') {
			return false;
		}
		int comaInd = interval.indexOf(',');
		if (comaInd == -1) {
			return false;
		}
		try {
			Version v1 = new Version((interval.substring(1, comaInd)));
			Version v2 = new Version((interval.substring(comaInd + 1, interval.length() - 1)));
			return v1.compareTo(v2) < 0;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	static String line_Separator = System.getProperty("line.separator"); //$NON-NLS-1$

	/**
	 * This method parses exception from String into List items
	 * 
	 * @param str
	 * @return
	 */
	public static List getStringTokens(String str) {
		java.util.List tokens = new ArrayList();
		int i = str.indexOf(line_Separator);
		int br = 0;
		while (i != -1) {
			if (br == 0) {
				tokens.add(str.substring(0, i).trim());
			} else {
				tokens.add("    " + str.substring(0, i).trim()); //$NON-NLS-1$
			}
			str = str.substring(i + 1).trim();
			i = str.indexOf(line_Separator);
			br++;
		}
		return tokens;
	}

	public static String dumpToText(Throwable t) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
		PrintStream printOut = new PrintStream(stream);
		t.printStackTrace(printOut);
		return stream.toString();
	}

	public static void moveElement(Vector elements, Object movedElement, boolean up) {
		int index = elements.indexOf(movedElement);
		int newIndex = index;
		if (up) {
			newIndex -= 1;
			if (newIndex < 0)
				newIndex = index;
		} else {
			newIndex += 1;
			if (newIndex > elements.size())
				newIndex = index;
		}
		if (newIndex < index) {
			elements.insertElementAt(movedElement, newIndex);
			elements.removeElementAt(index + 1);
		} else {
			Object oldBundle = elements.elementAt(newIndex);
			elements.insertElementAt(oldBundle, index);
			elements.removeElementAt(newIndex + 1);
		}
	}

	public static String getStringValue(String str) {
		return str == null ? "" : str;
	}

	public static File findLastJar(File file) {
		File result = file;
		if (file.exists()) {
			return result;
		}
		String fileName = file.getName();
		String name = file.getName();
		int dashIndex = name.lastIndexOf("_");
		if (dashIndex == -1) {
			if (name.endsWith(".jar")) {
				name = name.substring(0, name.length() - 4);
			}
			name = findNameWithoutVersion(name);
		} else {
			name = name.substring(0, dashIndex + 1);
		}
		File parent = file.getParentFile();
		if (parent == null || !parent.exists()) {
			return result;
		}
		File[] listFiles = parent.listFiles();
		long resLastModified = result.lastModified();
		for (int i = 0; i < listFiles.length; i++) {
			File tmpFile = listFiles[i];
			String tmpName = tmpFile.getName();
			if (!tmpName.endsWith(".jar"))
				continue;
			if (tmpName.startsWith(name)) {
				int compareTo = fileName.compareToIgnoreCase(tmpName);
				if (compareTo < 0) {
					resLastModified = result.lastModified();
					long tmpLastModified = tmpFile.lastModified();
					if (resLastModified < tmpLastModified) {
						fileName = tmpName;
						result = tmpFile;
					}
				}
				if (compareTo > 0) {
					resLastModified = result.lastModified();
					long tmpLastModified = tmpFile.lastModified();
					if (resLastModified < tmpLastModified) {
						fileName = tmpName;
						result = tmpFile;
					}
				}
			}
		}
		return result;
	}

	public static String encodePassword(String pass) {
		if (pass == null) {
			return null;
		}
		String result = "";
		if (pass.length() == 0) {
			result = result + ((char) ('A' + 17 + (int) (Math.random() * 9)));
			int len = (int) (Math.random() * 11);
			for (int i = 0; i < len; i++) {
				result += ((char) ('A' + Math.random() * 34));
			}
			return result;
		}
		int random = (int) (Math.random() * 125 + 1) * 256 + (int) (Math.random() * 256);
		int len = pass.length();
		int r = 0;
		for (int i = 0; i < pass.length(); i++) {
			long ch = pass.charAt(i);
			ch = ch * ch + random * (random + i % 4);
			for (int j = 0; j < 8; j++) {
				result += (char) ('A' + ((r % 19) + (ch & 0xF)));
				ch = ch / 16;
				r++;
			}
		}
		long ch = pass.charAt(0);
		ch = ch * ch + random * (random + 1);
		for (int j = 0; j < 8; j++) {
			result += (char) ('A' + ((r % 19) + (ch & 15)));
			ch = ch / 16;
			r++;
		}
		len = (int) Math.random() * 7;
		for (int i = 0; i < len; i++) {
			result += ((char) ('A' + Math.random() * 34));
		}
		return result.toString();
	}

	public static String decodePassword(String pass) {
		if (pass == null || pass.charAt(0) - 'A' > 16) {
			return "";
		}
		long[] chars = new long[pass.length() / 8];
		int r = 0;
		char ch = 0;
		for (int i = 0; i < chars.length; i++) {
			for (int j = 0; j < 8; j++) {
				ch = pass.charAt(r);
				chars[i] = chars[i] + ((ch - (int) 'A' - (r % 19)) << (4 * j));
				r++;
			}
		}
		int random = (int) (chars[chars.length - 1] - chars[0]);
		String result = "";
		for (int i = 0; i < chars.length - 1; i++) {
			result += ((char) (isqrt(chars[i] - random * (random + i % 4))));
		}
		return result.toString();
	}

	static long isqrt(long number) {
		long n = 1;
		long n1 = (((n) + (number) / (n)) >> 1);

		while (Math.abs(n1 - n) > 1) {
			n = n1;
			n1 = (((n) + (number) / (n)) >> 1);
		}
		while ((n1 * n1) > number) {
			n1 -= 1;
		}
		return n1;
	}

	public static void main(String[] args) {
		java.io.DataInputStream isr = new java.io.DataInputStream(System.in);
		while (true) {
			System.out.print("Enter password : ");
			try {
				String password = isr.readLine();
				String encoded = encodePassword(password);
				String decoded = decodePassword(encoded);
				System.out.println("\noriginal: " + password);
				System.out.println("encoded : " + encoded);
				System.out.println("decoded : " + decoded);
				System.out.println("correct : " + decoded.equals(password) + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String findNameWithoutVersion(String name) {
		String result = name;
		StringTokenizer tokenizer = new StringTokenizer(name, ".");
		String tmp = "";
		while (tokenizer.hasMoreTokens()) {
			String nextToken = tokenizer.nextToken();
			try {
				Double.valueOf(nextToken);
				break;
			} catch (NumberFormatException e) {
				String nextTmp = "";
				if (nextToken.startsWith("v")) {
					nextTmp = nextToken.substring(1);
					try {
						Double.valueOf(nextTmp);
						break;
					} catch (NumberFormatException ex) {
					}
				}
				tmp += tmp.equals("") ? "" : ".";
				tmp += nextToken;
			}
		}
		if (!tmp.equals("")) {
			result = tmp;
		}
		return result;
	}

	public static boolean containsArticles(String pathName) {
		if (INSTALLED_PLATFORM.toLowerCase().indexOf("win") >= 0) {
			// on windows, filename suffixes are not relevant to name validity
			int dot = pathName.indexOf('.');
			pathName = dot == -1 ? pathName : pathName.substring(0, dot);
			return Arrays.binarySearch(INVALID_RESOURCE_NAMES, pathName.toLowerCase()) >= 0;
		}
		return false;
	}

	/**
	 * Validate specified path name for correctness
	 * 
	 * @param pathName
	 * @return
	 */
	public static boolean isValidPath(String pathName) {
		if (!containsArticles(pathName)) {
			for (int i = 0; i < pathName.length(); i++) {
				for (int c = 0; c < INVALID_RESOURCE_CHARACTERS.length; c++) {
					if (INVALID_RESOURCE_CHARACTERS[c] == pathName.charAt(i)) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Checks if the given file name and extension are contains invalid chars
	 * 
	 * @param fileName
	 *            the name of the file
	 * @param fileExt
	 *            the extension of the file
	 * @return <code>true</code> if the given file name and extension not
	 *         contains invalid chars, <code>false</code> otherwise
	 */
	public static boolean isCorrectStringFileName(String fileName, String fileExt) {
		for (int i = 0; i < INVALID_RESOURCE_CHARACTERS.length; i++) {
			if ((fileName.indexOf(INVALID_RESOURCE_CHARACTERS[i]) != -1) || (fileExt.indexOf(INVALID_RESOURCE_CHARACTERS[i]) != -1)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the given name is valid name for the file.
	 * 
	 * @param f
	 *            the file name
	 * @param extension
	 *            the extension of the file
	 * @return <code>true</code> if the given name is valid file name,
	 *         <code>false</code> otherwise
	 */
	public static boolean isCorrectFileName(String f, String extension) {
		if (!extension.startsWith(".") && !extension.equals("")) {
			extension = "." + extension;
		}
		int lastDot = f.lastIndexOf('*');
		if (lastDot > -1)
			return false;
		String temp = f;
		do {
			f = temp;
			temp = replaceString(f, "//", "/");
		} while (!temp.equals(f));
		lastDot = f.lastIndexOf('.');
		if (!extension.equals("")) {
			if (lastDot < 0) {
				return isCorrectFileName(f + "." + extension, extension);
			}
			String ext = f.substring(lastDot);
			if (!extension.equals("") && !new File(extension).equals(new File(ext))) {
				return false;
			}
		}
		StringTokenizer stringToken = new StringTokenizer(f, "/\\");
		if (!stringToken.hasMoreTokens()) {
			return false;
		}
		boolean result = true;
		while (stringToken.hasMoreTokens() && result) {
			String nextToken = stringToken.nextToken();
			if (nextToken.equals(".") || nextToken.equals(".."))
				return false;
			result = result & isCorrectStringFileName(nextToken, extension);
		}

		return result;
	}

	public static Vector convertToVector(String str) {
		Vector result = new Vector();
		if (str != null && !str.equals("")) {
			char ch[] = str.toCharArray();
			StringBuffer key = new StringBuffer();
			StringBuffer value = new StringBuffer();
			for (int i = 0; i < ch.length; i++) {
				while (ch[i] != ':') {
					key.append(ch[i]);
					i++;
				}
				i++;
				while (i < ch.length && !(ch[i] == ';' && ch[i - 1] != '\\')) {
					value.append(ch[i]);
					i++;
				}
				result.addElement(new Header(key.toString(), deescape(value.toString())));
				key.setLength(0);
				value.setLength(0);
			}
		}
		return result;
	}

	public static String convertToString(Vector headersVector) {
		String result = "";
		for (int i = 0; i < headersVector.size(); i++) {
			Header header = (Header) headersVector.elementAt(i);
			result = result + (result.equals("") ? "" : ";") + header.getKey() + ":" + escape(header.getValue());
		}
		return result;
	}

	public static String escape(String value) {
		StringBuffer sb = new StringBuffer();
		char ch[] = value.toCharArray();
		for (int i = 0; i < ch.length; i++) {
			if (ch[i] == ';' || ch[i] == '\\') {
				sb.append('\\');
			}
			sb.append(ch[i]);
		}
		return sb.toString();
	}

	public static String deescape(String value) {
		StringBuffer sb = new StringBuffer();
		char ch[] = value.toCharArray();
		for (int i = 0; i < ch.length; i++) {
			if (ch[i] == '\\') {
				i++;
			}
			sb.append(ch[i]);
		}
		return sb.toString();
	}

	// from org.osgi.framework.Version
	private static class Version {
		private int major;
		private int minor;
		private int micro;
		private String qualifier;

		public Version(String version) {
			int[] arrVer = new int[] { 0, 0, 0 };
			int begin = 0;
			int end = version.length();
			String qualifier = "";

			int index = 0;
			try {
				label: do {
					int i = begin;
					while (i < end && version.charAt(i) != '.')
						i++;
					switch (index) {
					case 0:
					case 1:
						arrVer[index++] = Integer.parseInt(version.substring(begin, i));
						break;
					case 2:
						arrVer[index++] = Integer.parseInt(version.substring(begin, i).trim());
						break;
					default:
						qualifier = version.substring(begin, end);
						break label;
					}
					if (i == end)
						break;
					begin = ++i;
				} while (true);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Invalid version format!");
			}

			/* correctness checks as per spec */
			if (index == 0) {
				throw new IllegalArgumentException("invalid format");
			}

			this.major = arrVer[0];
			this.minor = arrVer[1];
			this.micro = arrVer[2];
			this.qualifier = qualifier;
			validate();
		}

		private void validate() {
			if (major < 0) {
				throw new IllegalArgumentException("negative major"); //$NON-NLS-1$
			}
			if (minor < 0) {
				throw new IllegalArgumentException("negative minor"); //$NON-NLS-1$
			}
			if (micro < 0) {
				throw new IllegalArgumentException("negative micro"); //$NON-NLS-1$
			}
			int length = qualifier.length();
			for (int i = 0; i < length; i++) {
				char ch = qualifier.charAt(i);
				if (!(('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9') || ch == '_' || ch == '-')) {
					throw new IllegalArgumentException("invalid qualifier");
				}
			}
		}

		public int compareTo(Object object) {
			if (object == this) { // quicktest
				return 0;
			}

			Version other = (Version) object;

			int result = major - other.major;
			if (result != 0) {
				return result;
			}

			result = minor - other.minor;
			if (result != 0) {
				return result;
			}

			result = micro - other.micro;
			if (result != 0) {
				return result;
			}

			return qualifier.compareTo(other.qualifier);
		}

	}

	public static boolean isValidManifestHeader(String key) {
		boolean isValid = true;

		for (int i = 0; i < key.length(); i++) {
			int code = key.charAt(i);
			if (!(code >= '0' && code <= '9') && !(code >= 'A' && code <= 'Z') && !(code >= 'a' && code <= 'z') && (code != '-' && code != '_')) {
				isValid = false;
				break;
			}
		}

		if (key.length() > 0) {
			int code = key.charAt(0);
			if (code == '-' || code == '_') {
				isValid = false;
			}
		}
		return isValid;
	}

	public static String getPath(String str) {
		if (PluginUtilities.isValidPath(str)) {
			IPath path = new Path(str);
			return path.segmentCount() > 1 ? path.removeLastSegments(1).toString() + File.separator : "";
		} else
			return null;
	}
}
