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
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.tigris.mtoolkit.common.PluginUtilities;
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
		checkHeadersConsistency();
		checkCertificatesConsistency();
		checkResourcesConsistency();
		checkBundlesConsistency();
		
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
	
	private boolean isFieldValueDuplicated(Vector objVector, int objIndex, String fldName, String fldValue) {
		if (objVector != null && objVector.size() > 0 && fldValue != null) {
			try {
				Class clazz = objVector.get(0).getClass();
				Field field = clazz.getDeclaredField(fldName);

				if (field != null) {
					for (int i = 0; i < objVector.size(); i++) {
						if (objIndex != i && fldValue.equals(field.get(objVector.elementAt(i)))) {
							return true;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
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
	public void checkBundlesConsistency() throws InconsistentDataException {
		for (int i = 0; i < bundleInfos.size(); i++) {
			BundleInfo bInfo = (BundleInfo) bundleInfos.elementAt(i);
			String bName = bInfo.getName();
			String bSymbName = bInfo.getBundleSymbolicName();

			if (isEmptyOrNull(bInfo.getBundlePath())) {
				throw new InconsistentDataException("Bundle Path is empty!");
			}
			if (isEmptyOrNull(bName)) {
				throw new InconsistentDataException("Bundle Name is empty!");
			}
			if (isEmptyOrNull(bSymbName)) {
				throw new InconsistentDataException("Bundle Symbolic Name is empty!");
			}
			if (isEmptyOrNull(bInfo.getBundleVersion())) {
				throw new InconsistentDataException("Bundle Version is empty!");
			}
			if (isFieldValueDuplicated(bundleInfos, i, "bundleSymbolicName", bSymbName)) {
				throw new InconsistentDataException(
						ResourceManager.getString("DPPEditor.BundlesSection.BundleSymbNameAlreadyExists1") + bSymbName
								+ ResourceManager.getString("DPPEditor.BundlesSection.BundleSymbNameAlreadyExists2"));
			}
			if (isFieldValueDuplicated(bundleInfos, i, "name", bName)) {
				throw new InconsistentDataException(
						ResourceManager.getString("DPPEditor.BundlesSection.BundleNameAlreadyExists1") + bName
								+ ResourceManager.getString("DPPEditor.BundlesSection.BundleNameAlreadyExists2"));
			}
		}
	}

	/**
	 * This method should be called each time when a tab is switched and the dp
	 * may have become inconsistent. If it throws exception the tab should not
	 * be switched.
	 * 
	 * @throws InconsistentDataException
	 *             if the data here is inconsistent
	 */
	public void checkResourcesConsistency() throws InconsistentDataException {
		for (int i = 0; i < resourceInfos.size(); i++) {
			ResourceInfo rInfo = (ResourceInfo) resourceInfos.elementAt(i);

			if (isEmptyOrNull(rInfo.getResourcePath())) {
				throw new InconsistentDataException("Resource Path is empty");
			}
			
			String resourcename = rInfo.getName();
			if (isEmptyOrNull(resourcename)) {
				throw new InconsistentDataException("Resource Name is empty");
			}
			if (resourcename.indexOf(":") != -1 || resourcename.endsWith("/") || resourcename.endsWith("\\")
					|| resourcename.equals("\\") || resourcename.equals("/")
					|| !PluginUtilities.isValidPath(resourcename)) {
				throw new InconsistentDataException(
						ResourceManager.getString("DPPEditor.ResourcesSection.InvalidResourceName1") + resourcename
								+ ResourceManager.getString("DPPEditor.ResourcesSection.InvalidResourceName2"));
			}
			if (isEmptyOrNull(rInfo.getResourceProcessor())) {
				throw new InconsistentDataException("Resource Processor is not set");
			}
		}
	}

	/**
	 * This method should be called each time when a tab is switched and the dp
	 * may have become inconsistent. If it throws exception the tab should not
	 * be switched.
	 * 
	 * @throws InconsistentDataException
	 *             if the data here is inconsistent
	 */
	public void checkCertificatesConsistency() throws InconsistentDataException {
		for (int i = 0; i < certificateInfos.size(); i++) {
			CertificateInfo cInfo = (CertificateInfo) certificateInfos.elementAt(i);

			if (isEmptyOrNull(cInfo.getAlias())) {
				throw new InconsistentDataException("Alias is empty");
			}
		}
	}

	/**
	 * This method should be called each time when a tab is switched and the dp
	 * may have become inconsistent. If it throws exception the tab should not
	 * be switched.
	 * 
	 * @throws InconsistentDataException
	 *             if the data here is inconsistent
	 */
	public void checkHeadersConsistency() throws InconsistentDataException {
		String version = packageHeaders.getVersion();
		String symbolicName = packageHeaders.getSymbolicName();
		String fixPack = packageHeaders.getFixPack();

		if (isEmptyOrNull(packageHeaders.getSymbolicName())) {
			throw new InconsistentDataException("The Deployment Package Symbolic Name is empty");
		}
		if (isEmptyOrNull(version)) {
			throw new InconsistentDataException("The Deployment Package Version is empty");
		}
		if (version.indexOf(" ") != -1 || version.endsWith(".")) {
			throw new InconsistentDataException("The version is incorrect. It should contain only digits, separated by points," + 
					"\nand digits and letters in its last section. (example: 1.2.3.build2005)");
		}
		if (version.indexOf(" ") == -1 && !DPPUtilities.isValidVersion(version.substring(0, version.lastIndexOf('.')))) {
			throw new InconsistentDataException("The version is incorrect. It should contain only digits, separated by points," + 
					"\nand digits and letters in its last section. (example: 1.2.3.build2005)");
		}
		if (!DPPUtilities.isCorrectPackage(symbolicName)) {
			if (symbolicName.startsWith(".") || symbolicName.endsWith(".")) {
				throw new InconsistentDataException("The Symbolic name '" + symbolicName
						+ "' is not valid. \nA symbolic name can't start or end with a dot.");
			}
			if (symbolicName.indexOf(" ") != -1) {
				throw new InconsistentDataException("The Symbolic name '" + symbolicName
						+ "' is invalid. \nThe Symbolic name can't contain empty spaces.");
			}

			throw new InconsistentDataException("The Symbolic name is invalid.\n'" + symbolicName
					+ "' is not a valid Java identifier.");
		}

		if (packageHeaders.isFixPackSet()) {
			if (!isEmptyOrNull(fixPack)
					&& (!DPPUtilities.isValidFixPack(fixPack) || !DPPUtilities.isValidVersion(fixPack))) {
				throw new InconsistentDataException("The Deployment Package Fix Pack is incorrect.\n"
						+ "The value should be similar to the example: (1.3, 5.7)\n"
						+ "or contain only digit and points.");
			}
			if (fixPack != null && fixPack.equals("")) {
				throw new InconsistentDataException("The Deployment Package Fix Pack can't be empty");
			}
		}

		Vector otherHeaders = packageHeaders.getOtherHeaders();

		for (int i = 0; i < otherHeaders.size(); i++) {
			Header header = (Header) otherHeaders.elementAt(i);
			if (header.getKey().equals("")) {
				throw new InconsistentDataException("The Header of Deployment Package can't be empty");
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
