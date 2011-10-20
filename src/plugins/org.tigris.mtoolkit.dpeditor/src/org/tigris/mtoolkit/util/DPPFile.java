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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.tigris.mtoolkit.dpeditor.util.ResourceManager;

/**
 * This class contains all the information needed to build a Deployment Package.
 * This includes info on bundles, resources, where to build the DP, certificates
 * used to sign the DP and so on.
 * 
 * @author Todor Cholakov
 * 
 */
public class DPPFile {

	private Vector bundleInfos = new Vector();
	private Vector resourceInfos = new Vector();
	private Vector certificateInfos = new Vector();
	private PackageHeaders packageHeaders;
	private BuildInfo buildInfo;
	private String projectLocation = "";
	boolean signBundles;

	/** Flag that shows if this dpp file needs to be saved */
	boolean needToSave = false;

	/**
	 * Physical storage
	 */
	File file;

	/**
	 * Creates a DPPFile object and reads its contents from a file (*.dpp)
	 * 
	 * @param file
	 *            the file to be read.
	 * @throws IOException
	 *             if the given file cannot be read.
	 */
	public DPPFile(File file) throws IOException {
		this.file = file;
		read();
	}

	/**
	 * Creates a DPPFile object and reads its contents from a file (*.dpp)
	 * 
	 * @param file
	 *            the file to be read.
	 * @param projectLocation
	 *            project relative paths are calculated according to this
	 *            location
	 * @throws IOException
	 *             if the given file cannot be read.
	 */
	public DPPFile(File file, String projectLocation) throws IOException {
		this.file = file;
		this.projectLocation = projectLocation;
		read();
	}

	/**
	 * Reloads the file from physical storage.All data is lost, the file is
	 * restored from previous data
	 * 
	 */
	public void restoreFromFile() {
		try {
			clearAll();
			read();
		} catch (IOException e) {
		}
	}

	/**
	 * Clears all the data contained here.
	 * 
	 */
	private void clearAll() {
		bundleInfos = new Vector();
		resourceInfos = new Vector();
		certificateInfos = new Vector();
		packageHeaders.clear();
		buildInfo.clear();
		signBundles = false;
	}

	/**
	 * Saves the data contained in this DPPFile object to the physical file.
	 * 
	 * @throws IOException
	 *             if the file file cannot be correctly written
	 * @throws InconsistentDataException
	 *             if there is inconsistency in the DPPFileData.
	 */
	public void save() throws IOException, InconsistentDataException {
		FileOutputStream fos = new FileOutputStream(file);
		save(fos);
		fos.close();
	}

	/**
	 * Saves the data contained in this DPPFile to the given output stream
	 * 
	 * @param os
	 *            the outputstream to write the data to
	 * @throws IOException
	 *             when there is a problem writing
	 * @throws InconsistentDataException
	 *             when there is inconsistency in the data itself
	 */
	public void save(OutputStream os) throws IOException, InconsistentDataException {
		String resultStr = save(true);
		os.write(resultStr.getBytes());
		os.flush();
	}

	/**
	 * This method stores the data int this DPPFile object to a string
	 * 
	 * @param isSaved
	 *            this parametere is not used
	 * @return a string representaation of the DPP File
	 * @throws IOException
	 *             when there is problem writing
	 * @throws InconsistentDataException
	 *             when the data in this DPPFile is inconsistent.
	 */
	public String save(boolean isSaved) throws IOException, InconsistentDataException {
		Properties props = saveInternal();
		String linesep = System.getProperty("line.separator");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		props.store(bos, "Deployment Plugin Project File");
		String result = bos.toString();
		StringTokenizer stok = new StringTokenizer(result, linesep);
		Vector rows = new Vector();
		String token = null;
		while (stok.hasMoreTokens()) {
			token = stok.nextToken();
			boolean inserted = false;
			for (int i = rows.size() - 1; i >= 0; i--) {
				if (token.compareTo((String) rows.elementAt(i)) >= 0) {
					rows.insertElementAt(token, i + 1);
					inserted = true;
					break;
				}
			}
			if (!inserted) {
				rows.insertElementAt(token, 0);
			}
		}
		StringBuffer end = new StringBuffer();
		for (int i = 0; i < rows.size(); i++) {
			end.append(rows.elementAt(i)).append(linesep);
		}
		return end.toString();
	}

	private Properties saveInternal() throws IOException, InconsistentDataException {
		DPPUtilities.debug("Checking consistency before save");
		checkConsistency();
		DPPUtilities.debug("Starting save of DP to " + file.getAbsolutePath());
		if (file == null) {
			throw new IOException("Where to save?");
		}
		Properties props = new Properties();

		setProperty(props, "general.signbundles", "" + getSignBundles());
		// write Bundles
		int bundlesCount = bundleInfos.size();
		props.setProperty("bundles.count", "" + bundlesCount);
		for (int i = 0; i < bundlesCount; i++) {
			BundleInfo bInfo = (BundleInfo) bundleInfos.elementAt(i);
			setProperty(props, "bundles." + i + ".bundle_path", convertToRelative(bInfo.getBundlePath()));
			setProperty(props, "bundles." + i + ".name", bInfo.getName());
			setProperty(props, "bundles." + i + ".customizer", "" + bInfo.isCustomizer());
			setProperty(props, "bundles." + i + ".missing", "" + bInfo.isMissing());
			setProperty(props, "bundles." + i + ".symbolic_name", bInfo.getBundleSymbolicName());
			setProperty(props, "bundles." + i + ".version", bInfo.getBundleVersion());
			Vector other = bInfo.getOtherHeaders();
			int headersCount = other.size();
			setProperty(props, "bundles." + i + ".headers.count", "" + headersCount);
			for (int j = 0; j < headersCount; j++) {
				Header header = (Header) other.elementAt(j);
				setProperty(props, "bundles." + i + ".headers." + j + ".key", header.getKey());
				setProperty(props, "bundles." + i + ".headers." + j + ".value", header.getValue());
			}
		}

		// write Resources
		int resourcesCount = resourceInfos.size();
		setProperty(props, "resources.count", "" + resourcesCount);
		for (int i = 0; i < resourcesCount; i++) {
			ResourceInfo rInfo = (ResourceInfo) resourceInfos.elementAt(i);
			setProperty(props, "resources." + i + ".path", convertToRelative(rInfo.getResourcePath()));
			setProperty(props, "resources." + i + ".name", rInfo.getName());
			setProperty(props, "resources." + i + ".processor", rInfo.getResourceProcessor());
			setProperty(props, "resources." + i + ".missing", "" + rInfo.isMissing());
			Vector other = rInfo.getOtherHeaders();
			int headersCount = other.size();
			setProperty(props, "resources." + i + ".headers.count", "" + headersCount);
			for (int j = 0; j < headersCount; j++) {
				Header header = (Header) other.elementAt(j);
				setProperty(props, "resources." + i + ".headers." + j + ".key", header.getKey());
				setProperty(props, "resources." + i + ".headers." + j + ".value", header.getValue());
			}
		}

		// write Certificates
		int certificateCount = certificateInfos.size();
		setProperty(props, "certificates.count", "" + certificateCount);
		for (int i = 0; i < certificateCount; i++) {
			CertificateInfo cInfo = (CertificateInfo) certificateInfos.elementAt(i);
			setProperty(props, "certificates." + i + ".alias", cInfo.getAlias());
			setProperty(props, "certificates." + i + ".keystore", convertToRelative(cInfo.getKeystore()));
			setProperty(props, "certificates." + i + ".keypass", DPPUtilities.encodePassword(cInfo.getKeypass()));
			setProperty(props, "certificates." + i + ".storepass", DPPUtilities.encodePassword(cInfo.getStorepass()));
			setProperty(props, "certificates." + i + ".storetype", cInfo.getStoreType());
		}

		// write Build info
		setProperty(props, "build.ant.name", convertToRelative(buildInfo.getAntFileName()));
		setProperty(props, "build.location", convertToRelative(buildInfo.getBuildLocation()));
		setProperty(props, "build.dp.file", convertToRelative(buildInfo.getDpFileName()));

		// write Package headers
		setProperty(props, "headers.contact.address", packageHeaders.getContactAddress());
		setProperty(props, "headers.copyright", packageHeaders.getCopyRight());
		setProperty(props, "headers.description", packageHeaders.getDescription());
		setProperty(props, "headers.doc.url", packageHeaders.getDocURL());
		setProperty(props, "headers.fix.pack", packageHeaders.getFixPack());
		setProperty(props, "headers.license", packageHeaders.getLicense());
		setProperty(props, "headers.symbolic.name", packageHeaders.getSymbolicName());
		setProperty(props, "headers.vendor", packageHeaders.getVendor());
		setProperty(props, "headers.version", packageHeaders.getVersion());
		setProperty(props, "headers.other.headers", packageHeaders.otherHeadersToString());
		DPPUtilities.debug("Save successfully finished");
		return props;

	}

	public String convertToRelative(String path) {
		if (projectLocation == null || projectLocation.length() == 0) {
			return path;
		}
		if (path == null) {
			return path;
		}
		if (path.startsWith(projectLocation + File.separator)) {
			path = "<.>" + path.substring(projectLocation.length());
		}
		return path;
	}

	private void setProperty(Properties props, String key, String value) {
		if (value == null) {
			return;
		}
		props.setProperty(key, value);
	}
	
	private boolean isBundleNameDuplicated(String bundleName, int index) {
		for (int i = 0; i < bundleInfos.size(); i++) {
			if (bundleName != null && bundleName.equals(((BundleInfo) bundleInfos.elementAt(i)).getName())
					&& index != i) {
				return true;
			}
		}
		return false;
	}
	
	private boolean areBundleSymbNameAndVersionDuplicated(String bundleSymbName, String version, int index) {
		for (int i = 0; i < bundleInfos.size(); i++) {
			if (bundleSymbName != null
					&& bundleSymbName.equals(((BundleInfo) bundleInfos.elementAt(i)).getBundleSymbolicName())
					&& version.equals(((BundleInfo) bundleInfos.elementAt(i)).getBundleVersion())
					&& index != i) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method should be called each time when a tab is switched and the dp
	 * may have become inconsistent. If it throws exception the tab should not
	 * be switched.
	 * 
	 * @throws InconsistentDataException
	 *             if the data here is inconsistent
	 */
	public void checkConsistency() throws InconsistentDataException {
		for (int i = 0; i < bundleInfos.size(); i++) {
			BundleInfo bInfo = (BundleInfo) bundleInfos.elementAt(i);
			String bundlePath = bInfo.getBundlePath();
			String bName = bInfo.getName();
			String bSymbName = bInfo.getBundleSymbolicName();
			String bVersion = bInfo.getBundleVersion();
			StringBuffer strBuilder = new StringBuffer();

			if (isEmptyOrNull(bundlePath)) {
				strBuilder.append("Bundle Path is empty!");
			} else if (areBundleSymbNameAndVersionDuplicated(bSymbName, bVersion, i)) {
				strBuilder.append((strBuilder.length() != 0 ? "\n" : "")
						+ ResourceManager.getString("DPPEditor.BundlesSection.BundleSymbNameAlreadyExists1")
						+ bSymbName
						+ ResourceManager.getString("DPPEditor.BundlesSection.BundleSymbNameAlreadyExists2"));
			} else if (isBundleNameDuplicated(bName, i)) {
				strBuilder.append((strBuilder.length() != 0 ? "\n" : "")
						+ ResourceManager.getString("DPPEditor.BundlesSection.BundleNameAlreadyExists1") + bName
						+ ResourceManager.getString("DPPEditor.BundlesSection.BundleNameAlreadyExists2"));
			} else if (!bundlePath.endsWith(".project")) {
				if (isEmptyOrNull(bName)) {
					strBuilder.append((strBuilder.length() != 0 ? "\n" : "") + "Bundle Name is empty!");
				}
				if (isEmptyOrNull(bSymbName)) {
					strBuilder.append((strBuilder.length() != 0 ? "\n" : "") + "Bundle Symbolic Name is empty!");
				}
				if (isEmptyOrNull(bVersion)) {
					strBuilder.append((strBuilder.length() != 0 ? "\n" : "") + "Bundle Version is empty!");
				}
			}

			if (strBuilder.length() > 0) {
				throw new InconsistentDataException(strBuilder.toString());
			}
		}

		for (int i = 0; i < resourceInfos.size(); i++) {
			ResourceInfo rInfo = (ResourceInfo) resourceInfos.elementAt(i);
			if (isEmptyOrNull(rInfo.getResourcePath()) || isEmptyOrNull(rInfo.getName())) {
				String message = "";
				if (isEmptyOrNull(rInfo.getResourcePath())) {
					message = "Resource Path is empty";
				}
				if (isEmptyOrNull(rInfo.getName())) {
					message += !message.equals("") ? "\n" : message;
					message += "Resource Name is empty";
				}
				throw new InconsistentDataException(message);
			} else {
				if (!isEmptyOrNull(rInfo.getResourcePath()) && !isEmptyOrNull(rInfo.getName()) && isEmptyOrNull(rInfo.getResourceProcessor())) {
					String message = "Resource Processor is not set";
					throw new InconsistentDataException(message);
				}
			}
		}
		for (int i = 0; i < certificateInfos.size(); i++) {
			CertificateInfo cInfo = (CertificateInfo) certificateInfos.elementAt(i);
			if (isEmptyOrNull(cInfo.getAlias())) {
				throw new InconsistentDataException("Alias is empty");
			}
		}
		if (isEmptyOrNull(packageHeaders.getSymbolicName()) || isEmptyOrNull(packageHeaders.getVersion())) {
			String message = "";
			if (isEmptyOrNull(packageHeaders.getSymbolicName())) {
				message = "Deployment Package Symbolic Name is empty";
			}
			String version = packageHeaders.getVersion();
			if (isEmptyOrNull(version)) {
				message += !message.equals("") ? "\n" : message;
				message += "Deployment Package Version is empty";
			} else {
				int index = version.lastIndexOf('.');
				String verTxt = version;
				if (verTxt.indexOf(" ") != -1 || ((index != -1) && (index == version.length()))) {
					message += !message.equals("") ? "\n" : message;
					message += "The version is not correct - it must start with digits separated \n" + "with points until after the last point where you can use letters, \n" + "for example: 1.0.0 or 1.2.3.build200501041230";
				}
				if (verTxt.indexOf(" ") == -1) {
					if (index != -1 && index != version.length()) {
						verTxt = version.substring(0, index);
					}
					if (!DPPUtilities.isValidVersion(verTxt)) {
						message += !message.equals("") ? "\n" : message;
						message += "The version is not correct - it must start with digits separated \n" + "with points until after the last point where you can use letters, \n" + "for example: 1.0.0 or 1.2.3.build200501041230";
					}
				}
			}
			throw new InconsistentDataException(message);
		} else {
			if (!isEmptyOrNull(packageHeaders.getSymbolicName())) {
				String message = "";
				String symbolicName = packageHeaders.getSymbolicName();
				if (!DPPUtilities.isCorrectPackage(symbolicName)) {
					if (symbolicName.startsWith(".") || symbolicName.endsWith(".")) {
						message = "Symbolic name '" + symbolicName + "' is not valid. \nA symbolic name cannot start or end with a dot.";
					} else if (symbolicName.indexOf(" ") != -1) {
						message = "Symbolic name '" + symbolicName + "' is not valid. \nA symbolic name cannot contain empty space.";
					} else {
						message = "Symbolic name is not valid.\n'" + symbolicName + "' is not a valid Java identifier.";
					}
					if (!message.equals("")) {
						throw new InconsistentDataException(message);
					}
				}
			}
			if (!isEmptyOrNull(packageHeaders.getVersion())) {
				String message = "";
				String version = packageHeaders.getVersion();
				if (isEmptyOrNull(version)) {
					message += !message.equals("") ? "\n" : message;
					message += "Deployment Package Version is empty";
				} else {
					if (!DPPUtilities.isValidVersion(version)) {
						message += !message.equals("") ? "\n" : message;
						message += "The version is not correct - it must start with digits separated \n" + "with points until after the last point where you can use letters, \n" + "for example: 1.0.0 or 1.2.3.build200501041230";
					}
				}
				if (!message.equals("")) {
					throw new InconsistentDataException(message);
				}
			}
		}
		String fixPack = packageHeaders.getFixPack();
		if (packageHeaders.isFixPackSet()) {
			if (!isEmptyOrNull(fixPack) && !DPPUtilities.isValidFixPack(fixPack)) {
				if (!DPPUtilities.isValidVersion(fixPack)) {
					String message = "Deployment Package Fix Pack is not correct.\n" + "The value must be similar like the example: (1.3, 5.7)\n" + "or contains only digit and points.";
					throw new InconsistentDataException(message);
				}
			} else if (packageHeaders.isFixPackSet() && fixPack != null && fixPack.equals("")) {
				throw new InconsistentDataException("Deployment Package Fix Pack cannot be empty");
			}
		}
		Vector otherHeaders = packageHeaders.getOtherHeaders();
		for (int i = 0; i < otherHeaders.size(); i++) {
			Header header = (Header) otherHeaders.elementAt(i);
			if (header.getKey().equals("")) {
				throw new InconsistentDataException("Header of Deployment Package cannot be empty");
			}
		}
	}

	private boolean isEmptyOrNull(String str) {
		return (str == null || str.equals(""));
	}

	/**
	 * This method reads the data from the physical storage into this DPPFile
	 * object.
	 * 
	 * @throws IOException
	 *             if there is problem reading the data.
	 */
	public void read() throws IOException {
		DPPUtilities.debug("Started reading file");
		if (!file.exists()) {
			return;
		}
		FileInputStream fis = null;
		Properties props = new Properties();
		try {
			fis = new FileInputStream(file);
			props.load(fis);
			fis.close();
			fis = null;
		} finally {
			if (fis != null) {
				fis.close();
			}
		}

		setSignBundles("true".equalsIgnoreCase(props.getProperty("general.signbundles")));
		// read Bundles
		int bundlesCount = parseInt("bundles.count", props.getProperty("bundles.count"));
		for (int i = 0; i < bundlesCount; i++) {
			BundleInfo bInfo = new BundleInfo();
			bInfo.setCustomizer("true".equalsIgnoreCase(props.getProperty("bundles." + i + ".customizer")));
			bInfo.setMissing("true".equalsIgnoreCase(props.getProperty("bundles." + i + ".missing")));
			bInfo.setBundlePath(convertToAbsolute(props.getProperty("bundles." + i + ".bundle_path")));
			String symbolicName = props.getProperty("bundles." + i + ".symbolic_name");
			//String version = props.getProperty("bundles." + i + ".version");
			String tmp = bInfo.getBundleSymbolicName();
			if (!needToSave && ((tmp != null && symbolicName == null) || (tmp == null && symbolicName != null) || (tmp == null && symbolicName == null) || !tmp.equals(symbolicName))) {
			}
			if (tmp == null || tmp.equals("") || tmp.equals("null")) {
				bInfo.setBundleSymbolicName(props.getProperty("bundles." + i + ".symbolic_name"));
			}
			String name = props.getProperty("bundles." + i + ".name");
			String bundleSetName = bInfo.getName();
			if (name != null && !name.equals("") && !name.equals(bundleSetName)) {
				bInfo.setName(name);
			}
			tmp = bInfo.getBundleVersion();
			if (tmp == null || tmp.equals("") || tmp.equals("null")) {
				bInfo.setBundleVersion(props.getProperty("bundles." + i + ".version"));
			}
			int hCount = parseInt("bundles." + i + ".headers.count", props.getProperty("bundles." + i + ".headers.count"));
			Vector other = new Vector();
			for (int j = 0; j < hCount; j++) {
				other.addElement(new Header(props.getProperty("bundles." + i + ".headers." + j + ".key"), props.getProperty("bundles." + i + ".headers." + j + ".value")));
			}
			bInfo.setOtherHeaders(other);
			bundleInfos.addElement(bInfo);
		}

		// read Resources
		int resourcesCount = parseInt("resources.count", props.getProperty("resources.count"));
		for (int i = 0; i < resourcesCount; i++) {
			ResourceInfo rInfo = new ResourceInfo();
			rInfo.setResourcePath(convertToAbsolute(props.getProperty("resources." + i + ".path")));
			rInfo.setName(props.getProperty("resources." + i + ".name"));
			rInfo.setMissing("true".equalsIgnoreCase(props.getProperty("resources." + i + ".missing")));
			rInfo.setResourceProcessor(props.getProperty("resources." + i + ".processor"));

			int hCount = parseInt("resources." + i + ".headers.count", props.getProperty("resources." + i + ".headers.count"));
			Vector other = new Vector();
			for (int j = 0; j < hCount; j++) {
				other.addElement(new Header(props.getProperty("resources." + i + ".headers." + j + ".key"), props.getProperty("resources." + i + ".headers." + j + ".value")));
			}
			rInfo.setOtherHeaders(other);

			resourceInfos.addElement(rInfo);
		}

		// read Certificates
		int certificatesCount = parseInt("certificates.count", props.getProperty("certificates.count"));
		for (int i = 0; i < certificatesCount; i++) {
			CertificateInfo cInfo = new CertificateInfo();
			cInfo.setAlias(props.getProperty("certificates." + i + ".alias"));
			cInfo.setKeypass(DPPUtilities.decodePassword(props.getProperty("certificates." + i + ".keypass")));
			cInfo.setStorepass(DPPUtilities.decodePassword(props.getProperty("certificates." + i + ".storepass")));
			cInfo.setKeystore(convertToAbsolute(props.getProperty("certificates." + i + ".keystore")));
			cInfo.setStoreType(props.getProperty("certificates." + i + ".storetype"));
			certificateInfos.addElement(cInfo);
		}

		// read Build info
		buildInfo = new BuildInfo();
		buildInfo.setAntFileName(convertToAbsolute(props.getProperty("build.ant.name")));
		buildInfo.setBuildLocation(convertToAbsolute(props.getProperty("build.location")));
		buildInfo.setDpFileName(convertToAbsolute(props.getProperty("build.dp.file")));

		// read Package headers
		packageHeaders = new PackageHeaders();
		packageHeaders.setContactAddress(props.getProperty("headers.contact.address"));
		packageHeaders.setCopyRight(props.getProperty("headers.copyright"));
		packageHeaders.setDescription(props.getProperty("headers.description"));
		packageHeaders.setDocURL(props.getProperty("headers.doc.url"));
		packageHeaders.setFixPack(props.getProperty("headers.fix.pack"));
		packageHeaders.setLicense(props.getProperty("headers.license"));
		packageHeaders.setSymbolicName(props.getProperty("headers.symbolic.name"));
		packageHeaders.setVendor(props.getProperty("headers.vendor"));
		packageHeaders.setVersion(props.getProperty("headers.version"));
		packageHeaders.setOtherHeaders(props.getProperty("headers.other.headers"));

		checkNonExistingValues();
		DPPUtilities.debug("File was successfully read");
	}

	private void checkNonExistingValues() {
		File file = getFile();
		String fileName = file.getName();
		int index = fileName.lastIndexOf(".");
		if (index != -1) {
			fileName = fileName.substring(0, index);
		}
		String dppFileName = file.getParentFile().getAbsolutePath() + File.separator + fileName;
		buildInfo.setBuildLocation("");
		String value = buildInfo.getDpFileName();
		if (value == null || value.equals("")) {
			buildInfo.setDpFileName(dppFileName + ".dp");
		}
		value = buildInfo.getAntFileName();
		if (value == null || value.equals("")) {
			buildInfo.setAntFileName(dppFileName + "_build.xml");
		}
		value = packageHeaders.getSymbolicName();
		if (value == null || value.equals("")) {
			packageHeaders.setSymbolicName(fileName);
		}
		value = packageHeaders.getVersion();
		if (value == null || value.equals("")) {
			packageHeaders.setVersion("1.0.0");
		}
	}

	public String convertToAbsolute(String path) {
		if (projectLocation == null || projectLocation.length() == 0) {
			return path;
		}
		if (path == null || path.equals("")) {
			return path;
		}
		if (path.startsWith("<.>")) {
			return projectLocation + path.substring(3);
		}
		return path;
	}

	/**
	 * Returns if current dpp file is need to be saved.
	 * 
	 * @return <code>true</code> if the dpp file is need to be saved, otherwise
	 *         <code>false</code>
	 */
	public boolean isNeedToSave() {
		return needToSave;
	}

	/**
	 * Sets the given <code>boolean</code> of the need to save flag.
	 * 
	 * @param flag
	 *            the <code>boolean</code> flag that will be set
	 */
	public void setNeedToSave(boolean flag) {
		needToSave = flag;
	}

	/**
	 * This method gets the PackageHeaders object that the scribes the package
	 * headers of this deployment package.
	 * 
	 * @return a PackageHeaders object. The object is the original or changes to
	 *         it are reflected to the DPPFile itself.
	 */
	public PackageHeaders getPackageHeaders() {
		return packageHeaders;
	}

	/**
	 * sets a new PackageHeaders object for this package. Use with extreame care
	 * because setting these headers replaces any old ones.
	 * 
	 * @param packageHeaders
	 *            the new package headers to be set.
	 */
	public void setPackageHeaders(PackageHeaders packageHeaders) {
		this.packageHeaders = packageHeaders;
	}

	/**
	 * return the information of all bundles that should be contained in this
	 * package.
	 * 
	 * @return a vector containing BundleInfo objects. This is the original
	 *         vector so be careful when modifying.
	 */
	public Vector getBundleInfos() {
		return bundleInfos;
	}

	/**
	 * Returns a vector of CertificateInfo objects that are used for signing the
	 * deployment package.
	 * 
	 * @return a vector contining CertificateInfo objects. Be careful because
	 *         this is the original vector.
	 */
	public Vector getCertificateInfos() {
		return certificateInfos;
	}

	/**
	 * Returns the infos for all the resources that should be contained in this
	 * deployment package.
	 * 
	 * @return a vector containing ResourceInfo objects. Be careful beacause
	 *         this is the original vector.
	 */
	public Vector getResourceInfos() {
		return resourceInfos;
	}

	/**
	 * Sets the file to be used for saving and loading the data for this DPPFile
	 * object.
	 * 
	 * @param file
	 *            the file to which all io operations to be done.
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * returns the file responding to this DPPFile object.
	 * 
	 * @return a File
	 */
	public File getFile() {
		return file;
	}

	/**
	 * This method returns the build info for this DPPFile
	 * 
	 * @return a BuildInfo object containing information about where to put the
	 *         resulting files.
	 */
	public BuildInfo getBuildInfo() {
		return buildInfo;
	}

	/**
	 * Sets a new Buildinfo object for this this dpp file.
	 * 
	 * @param buildInfo
	 *            the BuildInfo object to be set.
	 */
	public void setBuildInfo(BuildInfo buildInfo) {
		this.buildInfo = buildInfo;
	}

	/**
	 * writes the contnet of this DPPFile to a PrintWriter
	 * 
	 * @param writer
	 *            the printWritetr to write the file to.
	 * @throws IOException
	 *             if there is problem writing
	 * @throws InconsistentDataException
	 *             if the data in this DPPFile object is inconsistent
	 */
	public void write(PrintWriter writer) throws IOException, InconsistentDataException {
		DPPUtilities.debug("Internal save to source started");
		String buff = save(true);
		writer.print(buff);
		writer.println();
		writer.close();
	}

	private int parseInt(String key, String value) {
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException nfe) {
			throw new NumberFormatException("The row '" + key + "=" + value + "' is not valid. Fix it using a text editor and try to open the file again.");
		}
	}

	public void setProjectLocation(String projectLocation) {
		this.projectLocation = projectLocation;
	}

	public String getProjectLocation() {
		return projectLocation;
	}

	public boolean getSignBundles() {
		return signBundles;
	}

	public void setSignBundles(boolean signBundles) {
		this.signBundles = signBundles;
	}
}
