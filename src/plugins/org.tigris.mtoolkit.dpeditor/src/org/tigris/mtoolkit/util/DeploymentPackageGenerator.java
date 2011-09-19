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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;

/**
 * Objects from this class are used to build deployment packages or ant build
 * scripts using the data contained in a DPP file.
 * 
 * @author Todor Cholakov
 * 
 */
public class DeploymentPackageGenerator {

	String error = "";
	String outputStream = "";
	String errorStream = "";

	String baseDir = "";
	Vector baseDirPaths;
	private IProgressMonitor monitor;
	static String nl = System.getProperty("line.separator");

	/**
	 * This method generates a deployment package by the passed DPPFile object
	 * 
	 * @param dppFile
	 *            this object contains all the data needed to build the dp file.
	 * @param projectRootPath
	 *            the root path to the project. all paths are relative to it.
	 */
	public void generateDeploymentPackage(DPPFile dppFile,
			String projectRootPath) {
		error = "";
		outputStream = "";
		errorStream = "";
		DPPUtilities.debug("Deployment package generation started");
		if (dppFile.getBuildInfo().getDpFileName() == null) {
			genereateDefaultBuildProperties(dppFile);
		}
		String dpFileName = getPath(projectRootPath, dppFile.getBuildInfo()
				.getDpFileName());
		File tmpDpFile = new File(dpFileName);
		File tmpParentFile = tmpDpFile.getParentFile();
		if (tmpParentFile != null) {
			if (!tmpParentFile.exists()) {
				tmpParentFile.mkdirs();
			}
			dppFile.getBuildInfo().setDpFileName(tmpDpFile.getName());
			dppFile.getBuildInfo().setBuildLocation(
					tmpParentFile.getAbsolutePath());
		}
		boolean fixpack = dppFile.getPackageHeaders().getFixPack() != null
				&& dppFile.getPackageHeaders().getFixPack().trim().length() != 0;
		boolean signBundles = dppFile.getSignBundles();
		for (int i = 0; i < dppFile.getBundleInfos().size(); i++) {
			if (monitor.isCanceled()) {
				return;
			}
			BundleInfo bInfo = (BundleInfo) dppFile.getBundleInfos().elementAt(
					i);
			if (bInfo.isMissing() && fixpack) {
				continue;
			}
			if (bInfo.getBundlePath() == null) {
				error = "The bundle "
						+ bInfo.getBundleSymbolicName()
						+ " does not exist.\n The Deployment Packages was not created";
				return;
			}
			File f = new File(getPath(projectRootPath, bInfo.getBundlePath()));
			if (!f.exists()) {
				error = "The bundle "
						+ f.getAbsolutePath()
						+ " does not exist.\n The Deployment Packages was not created";
				return;
			}
		}
		for (int i = 0; i < dppFile.getResourceInfos().size(); i++) {
			if (monitor.isCanceled()) {
				return;
			}
			ResourceInfo rInfo = (ResourceInfo) dppFile.getResourceInfos()
					.elementAt(i);
			if ((rInfo.isMissing()) && fixpack) {
				continue;
			}
			File f = new File(getPath(projectRootPath, rInfo.getResourcePath()));
			if (!f.exists()) {
				error = "The resource "
						+ f.getAbsolutePath()
						+ " does not exist.\n The Deployment Packages was not created";
				return;
			}
		}
		String manifest = generateManifest(dppFile);
		String dpName = null;
		FileInputStream fis = null;
		JarOutputStream jos = null;
		try {
			if (dppFile.getBuildInfo().getDpFileName()
					.indexOf(File.separatorChar) >= 0) {
				dpName = dppFile.getBuildInfo().getDpFileName();
			} else {
				dpName = dppFile.getBuildInfo().getBuildLocation()
						+ File.separator
						+ dppFile.getBuildInfo().getDpFileName();
			}
			jos = new JarOutputStream(new FileOutputStream(dpName));
			JarEntry je = new JarEntry("META-INF/MANIFEST.MF");
			jos.putNextEntry(je);
			jos.write(manifest.getBytes());
			byte[] buffer = new byte[4096];
			int bLen = 0;
			for (int i = 0; i < dppFile.getBundleInfos().size(); i++) {
				if (monitor.isCanceled()) {
					jos.close();
					return;
				}
				BundleInfo bInfo = (BundleInfo) dppFile.getBundleInfos()
						.elementAt(i);
				DPPUtilities.debug("Adding bundle : " + bInfo.getBundlePath());
				if (bInfo.isMissing() && fixpack) {
					continue;
				}
				je = new JarEntry(bInfo.getName());
				jos.putNextEntry(je);

				String signedBundlePath = null;
				if (signBundles && dppFile.getCertificateInfos().size() != 0) {
					String bundle = getPath(projectRootPath,
							bInfo.getBundlePath());
					for (int j = 0; j < dppFile.getCertificateInfos().size(); j++) {
						if (monitor.isCanceled()) {
							if (j > 0)
								new File(bundle).delete();
							return;
						}
						final CertificateInfo ci = (CertificateInfo) dppFile
								.getCertificateInfos().elementAt(j);
						File tmpFile = File.createTempFile("dpgenerator",
								".jar");
						if (!signJar(bundle, tmpFile.getAbsolutePath(), ci)) {
							String signJarError = error;
							error = "DP file cannot be created, because bundle "
									+ bInfo.getName() + " cannot be signed.\n";
							if (signJarError != null
									&& signJarError.trim().length() > 0)
								error += "Reason: " + signJarError;
							tmpFile.delete();
							if (j > 0)
								new File(bundle).delete();
							return;
						}
						if (j > 0) // remove the old temporary file
							new File(bundle).delete();
						bundle = tmpFile.getAbsolutePath();
					}
					signedBundlePath = bundle;
					fis = new FileInputStream(bundle);
				} else {
					fis = new FileInputStream(getPath(projectRootPath,
							bInfo.getBundlePath()));
				}
				do {
					bLen = fis.read(buffer);
					if (bLen > 0) {
						jos.write(buffer, 0, bLen);
					}
				} while (bLen >= 0);
				fis.close();
				fis = null;
				if (signedBundlePath != null)
					new File(signedBundlePath).delete();
			}

			for (int i = 0; i < dppFile.getResourceInfos().size(); i++) {
				if (monitor.isCanceled()) {
					jos.close();
					return;
				}
				ResourceInfo rInfo = (ResourceInfo) dppFile.getResourceInfos()
						.elementAt(i);
				if (rInfo.isMissing() && fixpack) {
					continue;
				}
				DPPUtilities.debug("Adding resource : "
						+ rInfo.getResourcePath());
				je = new JarEntry(rInfo.getName());
				jos.putNextEntry(je);
				fis = new FileInputStream(getPath(projectRootPath,
						rInfo.getResourcePath()));
				do {
					bLen = fis.read(buffer);
					if (bLen > 0) {
						jos.write(buffer, 0, bLen);
					}
				} while (bLen >= 0);
				fis.close();
				fis = null;
			}
		} catch (FileNotFoundException e) {
			DPPUtilities.debug("Deployment package generator : ", e);
			error = "Deployment package was not created - some files were not found.";
			if (jos != null) {
				try {
					jos.close();
				} catch (IOException e1) {
				}
			}
			if (dpName != null) {
				new File(dpName).delete();
			}
		} catch (ZipException e) {
			DPPUtilities.debug("Deployment package generator : ", e);
			error = "Deployment package was not created due to unexpected error.\n"
					+ e.getMessage();
			if (jos != null) {
				try {
					jos.close();
				} catch (IOException e1) {
				}
			}
			if (dpName != null) {
				new File(dpName).delete();
			}
		} catch (IOException e) {
			DPPUtilities.debug("Deployment package generator : ", e);
			error = "Problem writing files while constructing the deployment package.\nMaybe the disk is full.\n The generated deployment package is not in consistent state.";
			if (jos != null) {
				try {
					jos.close();
				} catch (IOException e1) {
				}
			}
			if (dpName != null) {
				new File(dpName).delete();
			}
		} catch (Throwable t) {
		} finally {
			try {
				if (fis != null) {
					fis.close();
					fis = null;
				}
			} catch (IOException e) {
			}
			if (jos != null) {
				try {
					jos.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private boolean timeout = false;

	public void signDP(final DPPFile dppFile) {
		if (dppFile.getCertificateInfos().size() == 0)
			return;

		for (int i = 0; i < dppFile.getCertificateInfos().size(); i++) {
			if (monitor.isCanceled()) {
				return;
			}
			final CertificateInfo ci = (CertificateInfo) dppFile
					.getCertificateInfos().elementAt(i);
			DPPUtilities.debug("Signing with alias : " + ci.getAlias()
					+ " keystore : " + ci.getKeystore());
			String dpName = dppFile.getBuildInfo().getDpFileName();
			String buildLocation = dppFile.getBuildInfo().getBuildLocation();
			if (!"".equals(buildLocation)) {
				dpName = buildLocation + File.separator + dpName;
			}
			if (!signJar(dpName, null, ci))
				return;
		}

	}

	private boolean signJar(String jarName, String signedJar, CertificateInfo ci) {
		String jarSigner = CertUtils.getJarsignerLocation();
		if (jarSigner == null) {
			error = "The location of jarsigner tool was not correctly specified in Preferences.";
			return false;
		}
		File f = new File(jarSigner);
		if (!f.exists()) {
			error = "The jarsigner tool was not found in the specified path.\nPlease set the correct path in Preferences.\nThe package was successfully created but was not signed.";
			return false;
		}
		Vector v = new Vector();
		v.addElement(jarSigner);
		getOptionsLine("-keystore", ci.getKeystore(), v);
		getOptionsLine("-storepass", ci.getStorepass(), v);
		getOptionsLine("-keypass", ci.getKeypass(), v);
		getOptionsLine("-storetype", ci.getStoreType(), v);
		getOptionsLine("-signedjar", signedJar, v);
		v.addElement(jarName);
		v.addElement(ci.getAlias());
		boolean ret = true;
		try {

			final Process ps = Runtime.getRuntime().exec(
					(String[]) v.toArray(new String[0]));
			int result = -1;
			Thread errorReader = new Thread() {
				public void run() {
					Thread.currentThread().setName("[Jar Signer] Error Reader");
					try {
						byte[] buf = new byte[4096];
						int len = ps.getErrorStream().available();
						while (len != -1 && !timeout) {
							len = ps.getErrorStream().read(buf);
							if (len > 0)
								errorStream = errorStream
										+ new String(buf, 0, len);
							len = ps.getErrorStream().available();
						}
					} catch (IOException io) {
					}
				}
			};
			errorReader.start();

			Thread outputReader = new Thread() {
				public void run() {
					Thread.currentThread()
							.setName("[Jar Signer] Output Reader");
					try {
						byte[] buf = new byte[4096];
						int len = ps.getInputStream().available();
						while (len != -1 && !timeout) {
							len = ps.getInputStream().read(buf);
							if (len > 0)
								outputStream = outputStream
										+ new String(buf, 0, len);
							len = ps.getInputStream().available();
						}
					} catch (IOException io) {
					}
				}
			};
			outputReader.start();
			int retries = 150;
			while (!monitor.isCanceled()) {
				try {
					Thread.sleep(200);
					result = ps.exitValue();
					break;
				} catch (IllegalThreadStateException itse) {
					if (--retries == 0)
						break;
				}
			}
			// check whether process is canceled by user or just has finished
			// signing.
			if (monitor.isCanceled()) {
				ps.destroy();
				if (result != 0) {// the ps can finish during the 100ms in the
					// while
					// loop
					error = ResourceManager
							.getString("BuildExportWizard.errorSigningCanceled");
				}
				ret = false;
			} else if (result != 0) {
				error = "Could not sign deployment package using the '"
						+ ci.getAlias()
						+ "' alias.\nCheck that your settings are correct and that the jar signer preference is correctly set.\nThe package was successfully created but was not signed.";
				System.out.println(error);
				try {
					ps.getOutputStream().write("a\na\na\n".getBytes());
					ps.getOutputStream().flush();
				} catch (IOException e) {
					// ignore, most probably the pipe will be closed
				}
				// causes problems sometimes.(infinite wait)
				System.out.println(errorStream);
				System.out.println(outputStream);
				ret = false;
			}
			timeout = true;
		} catch (Throwable t) {
		}
		return ret;
	}

	private void genereateDefaultBuildProperties(DPPFile dppFile) {
		BuildInfo buildInfo = dppFile.getBuildInfo();
		String dppFileName = dppFile.getFile().getAbsolutePath();
		if (dppFileName.endsWith(".dpp")) {
			dppFileName = dppFileName
					.substring(0, dppFileName.lastIndexOf('.'));
		}
		buildInfo.setBuildLocation("");
		String value = buildInfo.getDpFileName();
		if (value == null || value.equals("")) {
			buildInfo.setDpFileName(dppFileName + ".dp");
		}
		value = buildInfo.getAntFileName();
		if (value == null || value.equals("")) {
			buildInfo.setAntFileName(dppFileName + "_build.xml");
		}
	}

	private String getPath(String projectRootPath, String path) {
		if (path != null && path.startsWith("<.>")) {
			return projectRootPath + path.substring(3);
		}
		return path;
	}

	/**
	 * Generates the deployment package manifest for passed dpp file.
	 * 
	 * @param dppFile
	 * @return
	 */
	public String generateManifest(DPPFile dppFile) {
		DPPUtilities.debug("Generating manifest");
		boolean fixpack = dppFile.getPackageHeaders().getFixPack() != null
				&& dppFile.getPackageHeaders().getFixPack().trim().length() != 0;
		StringBuffer manifest = new StringBuffer();
		manifest.append("Manifest-Version: 1.0" + nl);
		manifest.append(getManifestLine("DeploymentPackage-SymbolicName",
				dppFile.getPackageHeaders().getSymbolicName()));
		manifest.append(getManifestLine("DeploymentPackage-Version", dppFile
				.getPackageHeaders().getVersion()));
		if (fixpack)
			manifest.append(getManifestLine("DeploymentPackage-FixPack",
					dppFile.getPackageHeaders().getFixPack()));
		manifest.append(getManifestLine("DeploymentPackage-Copyright", dppFile
				.getPackageHeaders().getCopyRight()));
		manifest.append(getManifestLine("DeploymentPackage-ContactAddress",
				dppFile.getPackageHeaders().getContactAddress()));
		manifest.append(getManifestLine("DeploymentPackage-Description",
				dppFile.getPackageHeaders().getDescription()));
		manifest.append(getManifestLine("DeploymentPackage-DocURL", dppFile
				.getPackageHeaders().getDocURL()));
		manifest.append(getManifestLine("DeploymentPackage-Vendor", dppFile
				.getPackageHeaders().getVendor()));
		manifest.append(getManifestLine("DeploymentPackage-License", dppFile
				.getPackageHeaders().getLicense()));
		Vector otherHeaders = dppFile.getPackageHeaders().getOtherHeaders();
		for (int i = 0; i < otherHeaders.size(); i++) {
			Header header = (Header) otherHeaders.elementAt(i);
			manifest.append(getManifestLine(header.getKey(), header.getValue()));
		}

		for (int i = 0; i < dppFile.getBundleInfos().size(); i++) {
			manifest.append(nl);
			BundleInfo bInfo = (BundleInfo) dppFile.getBundleInfos().elementAt(
					i);
			manifest.append(getManifestLine("Name", bInfo.getName()));
			manifest.append(getManifestLine("Bundle-SymbolicName",
					bInfo.getBundleSymbolicName()));
			manifest.append(getManifestLine("Bundle-Version",
					bInfo.getBundleVersion()));
			if (fixpack && bInfo.isMissing()) {
				manifest.append(getManifestLine("DeploymentPackage-Missing",
						"true"));
			}
			if (bInfo.isCustomizer()) {
				manifest.append(getManifestLine("DeploymentPackage-Customizer",
						"true"));
			}
			Vector others = bInfo.getOtherHeaders();
			for (int j = 0; j < others.size(); j++) {
				Header header = (Header) others.elementAt(j);
				manifest.append(getManifestLine(header.getKey(),
						header.getValue()));
			}
		}

		for (int i = 0; i < dppFile.getResourceInfos().size(); i++) {
			manifest.append(nl);
			ResourceInfo rInfo = (ResourceInfo) dppFile.getResourceInfos()
					.elementAt(i);
			manifest.append(getManifestLine("Name", rInfo.getName()));
			manifest.append(getManifestLine("Resource-Processor",
					rInfo.getResourceProcessor()));
			if (fixpack && rInfo.isMissing()) {
				manifest.append(getManifestLine("DeploymentPackage-Missing",
						"true"));
			}
			Vector others = rInfo.getOtherHeaders();
			for (int j = 0; j < others.size(); j++) {
				Header header = (Header) others.elementAt(j);
				manifest.append(getManifestLine(header.getKey(),
						header.getValue()));
			}
		}

		return manifest.toString();
	}

	private String getManifestLine(String header, String value) {
		if (value == null) {
			return "";
		}
		return header + ": " + value + nl;
	}

	private void getOptionsLine(String option, String value, Vector v) {
		if (value == null || value.length() == 0) {
			return;
		}
		v.addElement(option);
		v.addElement(value);
	}

	private String getAntOptionsLine(String option, String value) {
		if (value == null || value.length() == 0) {
			return "";
		}
		return option + "=\"" + value + "\" ";
	}

	/**
	 * this matehod generates a deployment package by the passed file
	 * 
	 * @param file
	 *            theis file (*.dpp) contains all the data needed to build the
	 *            dp file.
	 * @param projectRootPath
	 *            the root path to the project. all paths are relative to it.
	 */
	public void generateDeploymentPackage(File file, String projectRootPath)
			throws IOException {
		DPPFile dppFile = new DPPFile(file, projectRootPath);
		generateDeploymentPackage(dppFile, projectRootPath);
		if (error.equals("") && !monitor.isCanceled()) {
			signDP(dppFile);
		}
	}

	private String substituteSpecialXMLCharacters(String strToParse) {
		strToParse = strToParse.replaceAll("&", "&amp;");
		strToParse = strToParse.replaceAll("'", "&apos;");
		strToParse = strToParse.replaceAll("<", "&lt;");
		strToParse = strToParse.replaceAll(">", "&gt;");
		strToParse = strToParse.replaceAll("\"", "&quot;");
		return strToParse;
	}

	/**
	 * This method generates an Ant script (xml) for headlessly building a
	 * deployment package
	 * 
	 * @param dppFile
	 *            the file to be build
	 * @param projectRootPath
	 *            the path to the project where the DPP lives.
	 * @param makeAllPathsRelative
	 *            - paths relative to the project are made relative always.
	 *            paths in other projects, or absolute may be calculated as
	 *            relative to the ant script if this flag is set. Still not
	 *            working(the last flag only)
	 */
	public void generateAntFile(DPPFile dppFile, String projectRootPath,
			boolean makeAllPathsRelative) throws IOException {
		DPPUtilities.debug("Generating ant script");
		if (dppFile.getBuildInfo().getAntFileName() == null) {
			genereateDefaultBuildProperties(dppFile);
		}
		dppFile.getBuildInfo()
				.setDpFileName(
						getPath(projectRootPath, dppFile.getBuildInfo()
								.getDpFileName()));
		dppFile.getBuildInfo().setBuildLocation(
				getPath(projectRootPath, dppFile.getBuildInfo()
						.getBuildLocation()));
		String antPath = getPath(projectRootPath, dppFile.getBuildInfo()
				.getAntFileName());
		dppFile.getBuildInfo().setAntFileName(antPath);
		antPath = new File(antPath).getParentFile().getAbsolutePath();
		boolean fixPack = dppFile.getPackageHeaders().getFixPack() != null
				&& dppFile.getPackageHeaders().getFixPack().trim().length() != 0;
		if (dppFile.getBuildInfo().getDpFileName().indexOf(File.separatorChar) >= 0) {
			String tempName = dppFile.getBuildInfo().getDpFileName();
			dppFile.getBuildInfo()
					.setDpFileName(
							tempName.substring(tempName
									.lastIndexOf(File.separator) + 1));
			dppFile.getBuildInfo()
					.setBuildLocation(
							tempName.substring(0,
									tempName.lastIndexOf(File.separator)));
		}

		// XML
		StringBuffer antFile = new StringBuffer();
		antFile.append("<?xml version=\"1.0\" ?>"
				+ nl
				+ "<project name=\"build_"
				+ substituteSpecialXMLCharacters(dppFile.getBuildInfo()
						.getDpFileName())
				+ "\" default=\"all\" basedir=\""
				+ substituteSpecialXMLCharacters(calculateBaseDir(dppFile
						.getBuildInfo().getAntFileName(), projectRootPath))
				+ "\" >" + nl + "");

		Vector bundleInfos = dppFile.getBundleInfos();
		for (int i = 0; i < bundleInfos.size(); i++) {
			if (monitor.isCanceled()) {
				return;
			}
			BundleInfo bInfo = (BundleInfo) bundleInfos.elementAt(i);
			if (!fixPack || !bInfo.isMissing()) {
				String path = substituteSpecialXMLCharacters(calculateRelative(getPath(
						projectRootPath, (bInfo.getBundlePath()))));

				antFile.append("<available property=\"file.exists.").append(
						path);
				antFile.append("\" file=\"").append(path).append("\"/>")
						.append(nl);
				;
				antFile.append("<target name=\"check.file.exists ")
						.append(path);
				antFile.append("\" unless=\"file.exists.").append(path)
						.append("\">").append(nl);
				antFile.append(
						"  <fail message=\"Can not find file " + path + "\"/>")
						.append(nl);
				antFile.append("</target>").append(nl);
			}
		}

		Vector resourceInfos = dppFile.getResourceInfos();
		for (int i = 0; i < resourceInfos.size(); i++) {
			if (monitor.isCanceled()) {
				return;
			}
			ResourceInfo rInfo = (ResourceInfo) resourceInfos.elementAt(i);

			if (!fixPack || !rInfo.isMissing()) {
				String path = substituteSpecialXMLCharacters(calculateRelative(getPath(
						projectRootPath, (rInfo.getResourcePath()))));
				antFile.append("<available property=\"file.exists.").append(
						path);
				antFile.append("\" file=\"").append(path).append("\"/>")
						.append(nl);
				;
				antFile.append("<target name=\"check.file.exists ")
						.append(path);
				antFile.append("\" unless=\"file.exists.").append(path)
						.append("\">").append(nl);
				antFile.append(
						"  <fail message=\"Can not find file " + path + "\"/>")
						.append(nl);
				antFile.append("</target>").append(nl);
			}
		}

		antFile.append("<target name=\"all\">" + nl);
		String path = substituteSpecialXMLCharacters(getPath(projectRootPath,
				(dppFile.getBuildInfo().getBuildLocation())));

		antFile.append("  <mkdir dir=\"" + path + "\"/>" + nl);

		for (int i = 0; i < bundleInfos.size(); i++) {
			if (monitor.isCanceled()) {
				return;
			}
			BundleInfo bInfo = (BundleInfo) bundleInfos.elementAt(i);
			if (!fixPack || !bInfo.isMissing()) {
				antFile.append("  <antcall target=\"check.file.exists ")
						.append(substituteSpecialXMLCharacters(calculateRelative(getPath(
								projectRootPath, (bInfo.getBundlePath())))))
						.append("\"/>").append(nl);
			}
		}
		for (int i = 0; i < resourceInfos.size(); i++) {
			if (monitor.isCanceled()) {
				return;
			}
			ResourceInfo rInfo = (ResourceInfo) resourceInfos.elementAt(i);
			if (!fixPack || !rInfo.isMissing()) {
				antFile.append("  <antcall target=\"check.file.exists ")
						.append(substituteSpecialXMLCharacters(calculateRelative(getPath(
								projectRootPath, (rInfo.getResourcePath())))))
						.append("\"/>").append(nl);
			}
		}

		antFile.append("  <jar destfile=\""
				+ substituteSpecialXMLCharacters(calculateRelative(getPath(
						projectRootPath, (dppFile.getBuildInfo()
								.getBuildLocation() + File.separator + dppFile
								.getBuildInfo().getDpFileName()))))
				+ "\" manifest=\""
				+ substituteSpecialXMLCharacters(calculateRelative(antPath
						+ File.separator
						+ stripDP(dppFile.getBuildInfo().getDpFileName())
						+ "_manifest.mf")) + "\">" + nl);
		for (int i = 0; i < bundleInfos.size(); i++) {
			BundleInfo bInfo = (BundleInfo) bundleInfos.elementAt(i);
			if (!fixPack || !bInfo.isMissing()) {
				antFile.append("    <zipfileset file=\""
						+ substituteSpecialXMLCharacters(calculateRelative(getPath(
								projectRootPath, (bInfo.getBundlePath()))))
						+ "\" fullpath=\""
						+ substituteSpecialXMLCharacters(bInfo.getName())
						+ "\" />" + nl);
			}
		}
		for (int i = 0; i < resourceInfos.size(); i++) {
			if (monitor.isCanceled()) {
				return;
			}
			ResourceInfo rInfo = (ResourceInfo) resourceInfos.elementAt(i);
			if (!fixPack || !rInfo.isMissing()) {
				antFile.append("    <zipfileset file=\""
						+ substituteSpecialXMLCharacters(calculateRelative(getPath(
								projectRootPath, (rInfo.getResourcePath()))))
						+ "\" fullpath=\""
						+ substituteSpecialXMLCharacters(rInfo.getName())
						+ "\" />" + nl);
			}
		}
		antFile.append("  </jar>" + nl + "");
		for (int i = 0; i < dppFile.getCertificateInfos().size(); i++) {
			if (monitor.isCanceled()) {
				return;
			}
			CertificateInfo ci = (CertificateInfo) dppFile
					.getCertificateInfos().elementAt(i);
			antFile.append("  <signjar jar=\""
					+ substituteSpecialXMLCharacters(calculateRelative(getPath(
							projectRootPath,
							(dppFile.getBuildInfo().getBuildLocation()
									+ File.separator + dppFile.getBuildInfo()
									.getDpFileName()))))
					+ "\" "
					+ substituteSpecialXMLCharacters(getAntOptionsLine("alias",
							ci.getAlias()))
					+ "storepass=\""
					+ (ci.getStorepass() == null ? ""
							: substituteSpecialXMLCharacters(ci.getStorepass()))
					+ "\" "
					+ substituteSpecialXMLCharacters(getAntOptionsLine(
							"keystore", ci.getKeystore()))
					+ substituteSpecialXMLCharacters(getAntOptionsLine(
							"keypass", ci.getKeypass()))
					+ substituteSpecialXMLCharacters(getAntOptionsLine(
							"storetype", ci.getStoreType())) + "/>" + nl + "");
		}
		antFile.append("</target>" + nl + "" + nl + "</project>" + nl + "");

		String antFileName = dppFile.getBuildInfo().getAntFileName();
		File tmpAntFile = new File(antFileName);
		File tmpParentFile = tmpAntFile.getParentFile();
		if (!tmpParentFile.exists()) {
			tmpParentFile.mkdirs();
		}

		FileOutputStream fos = new FileOutputStream(dppFile.getBuildInfo()
				.getAntFileName());
		fos.write(antFile.toString().getBytes());
		fos.close();
		fos = new FileOutputStream(antPath + File.separator
				+ stripDP(dppFile.getBuildInfo().getDpFileName())
				+ "_manifest.mf");
		fos.write(generateManifest(dppFile).getBytes());
		fos.close();
	}

	private String calculateRelative(String path) {
		if (baseDirPaths == null) {
			File f = new File(baseDir);
			Vector v = new Vector();
			while (f != null) {
				v.addElement(f);
				f = f.getParentFile();
			}
			baseDirPaths = v;
		}
		if (path != null) {
			if (path.startsWith("<prj>/")) {
				return path.substring(6);
			} else {
				File f = new File(path);
				String result = "";
				while (f != null && baseDirPaths.indexOf(f) < 0) {
					result = File.separator + f.getName() + result;
					f = f.getParentFile();
				}
				if (f == null) {
					return path;
				}
				int k = baseDirPaths.indexOf(f);
				if (k > 0) {
					result = ".." + result;
					k--;
				} else {
					return result.substring(1);
				}
				for (int i = 0; i < k; i++) {
					result = ".." + File.separator + result;
				}
				return result;
			}
		}
		return path;
	}

	private String stripDP(String dpFileName) {
		return dpFileName.substring(0, dpFileName.length() - 3);// cuts the
		// '.dp'
		// part
	}

	private String calculateBaseDir(String antFileName, String projectRootPath) {
		File f = new File(antFileName).getParentFile();
		String antPath = f.getAbsolutePath();
		String projectPath = new File(projectRootPath).getAbsolutePath();
		String result = "";
		if (antPath.startsWith(projectPath)
				&& antPath.length() != projectPath.length()) {
			// the ant file should be in the project
			antPath = antPath.substring(projectPath.length());
			StringTokenizer tok = new StringTokenizer(antPath, File.separator);
			while (tok.hasMoreTokens()) {
				tok.nextToken();
				result = result + "../";
			}
			if (result.length() > 0) {
				result = result.substring(0, result.length() - 1);
				baseDir = projectPath;
			} else {
				result = ".";
				baseDir = antPath;
			}
			return result;
		}
		baseDir = antPath;
		return ".";
	}

	/**
	 * If the generation finished with a warning it may be took throug this
	 * method
	 * 
	 * @deprecated use getError()
	 * @return the warning
	 */
	public String getWarning() {
		return getError();
	}

	public String getError() {
		return error;
	}

	public String getOutput() {
		return errorStream + outputStream;
	}

	public String getErrorStream() {
		return errorStream;
	}

	public String getOutputStream() {
		return outputStream;
	}

	public void setMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;

	}

}
