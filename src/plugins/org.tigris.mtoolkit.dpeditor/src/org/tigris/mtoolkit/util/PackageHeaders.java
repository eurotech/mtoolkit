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

import java.util.Vector;

/**
 * This class contains the common headers for this deployment package in the
 * manifest.
 *
 * @author todor
 *
 */
public class PackageHeaders {
  String          symbolicName;
  String          version;
  String          fixPack;

  String          copyRight;
  String          contactAddress;
  String          description;
  String          docURL;
  String          vendor;
  String          license;
  Vector          otherHeaders = new Vector();

  private boolean isFixPackSet = false;

  /**
   * returns the contact address of the supplyer of the deployment package. This
   * header is optional.
   *
   * @return the contact address
   */
  public String getContactAddress() {
    return contactAddress;
  }

  /**
   * sets the contact address of the supplier of the deployment package
   *
   * @param contactAddress
   *          the contact address to be set
   */
  public void setContactAddress(String contactAddress) {
    this.contactAddress = contactAddress;
  }

  /**
   * Gets the copyright owner for this package. This header is optional
   *
   * @return the copyright owner
   */
  public String getCopyRight() {
    return copyRight;
  }

  /**
   * sets the copyright owner for this deployment package
   *
   * @param copyRight
   *          the copyright owner to be set.
   */
  public void setCopyRight(String copyRight) {
    this.copyRight = copyRight;
  }

  /**
   * gets a short description of this deployment package
   *
   * @return the description of the deployment package
   */
  public String getDescription() {
    return description;
  }

  /**
   * sets the description of the deployment package
   *
   * @param description
   *          the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * retrieves the DocURL header
   *
   * @return the DocURL header as a string
   */
  public String getDocURL() {
    return docURL;
  }

  /**
   * sets a new value for the DocURL header.
   *
   * @param docURL
   *          a docURL to be set
   */
  public void setDocURL(String docURL) {
    this.docURL = docURL;
  }

  /**
   * returns the value of the fixpack header
   *
   * @return the FixPack header
   */
  public String getFixPack() {
    return fixPack;
  }

  /**
   * sets a new value for the FixPack header
   *
   * @param fixPack
   *          the new FixPack header value. This field contains a version or
   *          version to whom this fix pack is applicable.
   */
  public void setFixPack(String fixPack) {
    this.fixPack = fixPack;
    isFixPackSet = fixPack != null;
  }

  /**
   * returns the value of the license header
   *
   * @return the license
   */
  public String getLicense() {
    return license;
  }

  /**
   * sets a new value for the license header
   *
   * @param license
   *          the new value
   */
  public void setLicense(String license) {
    this.license = license;
  }

  /**
   * Returns the symbolic name for this package. This field is compulsory.
   *
   * @return the symbolic name of the package
   */
  public String getSymbolicName() {
    return symbolicName;
  }

  /**
   * sets the symbolic name for the package
   *
   * @param symbolicName
   *          the new symbolic name for the package.
   */
  public void setSymbolicName(String symbolicName) {
    this.symbolicName = symbolicName;
  }

  /**
   * returns the vendor of the package
   *
   * @return the vendor
   */
  public String getVendor() {
    return vendor;
  }

  /**
   * sets the name fo the vendor of this package. this field is optionsl.
   *
   * @param vendor
   *          the vendor name to be set
   */
  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  /**
   * gets the version of this deployment package. This field is compulsory.
   *
   * @return the version of this package
   */
  public String getVersion() {
    return version;
  }

  /**
   * sets the version of this package
   *
   * @param version
   *          the new version to be set
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Returns a vector containing all non standard headers set for this package.
   *
   * @return a vector with headers (instances of
   *         org.tigris.mtoolkit.util.Header).
   */
  public Vector getOtherHeaders() {
    return otherHeaders;
  }

  /**
   * Sets a vector containing all additional headers for this deployment package
   *
   * @param otherHeaders
   *          the new set of additional headers. The old headers if any are
   *          overridden.
   */
  public void setOtherHeaders(Vector otherHeaders) {
    this.otherHeaders = otherHeaders;
  }

  /**
   * Sets the additional headers for this deployment package as a string. The
   * different headers should be separated with ';' and the header and its value
   * with '\:'
   *
   * @param str
   *          the string containing all headers.
   */
  public void setOtherHeaders(String str) {
    setOtherHeaders(DPPUtilities.convertToVector(str));
  }

  /**
   * This method returns a string representation of all additional headers. The
   * different headers are separated using ';' and the header and its value by
   * '\:'
   *
   * @return the headers as String
   */
  public String otherHeadersToString() {
    String result = "";
    if (otherHeaders != null) {
      result = DPPUtilities.convertToString(otherHeaders);
    }
    return result;
  }

  /**
   * adds a single header no matter if its a standard or not.
   *
   * @param key
   *          the header name
   * @param value
   *          the header value
   */
  public void addElement(String key, String value) {
    if (key.trim().equals(DPPConstants.dpSymbolicNameHeader.trim())) {
      setSymbolicName(value);
    } else if (key.equals(DPPConstants.dpVersionHeader)) {
      setVersion(value);
    } else if (key.equals(DPPConstants.dpFixPackHeader)) {
      setIsFixPack(true);
      setFixPack(value);
    } else if (key.equals(DPPConstants.dpCopyrightHeader)) {
      setCopyRight(value);
    } else if (key.equals(DPPConstants.dpAddressHeader)) {
      setContactAddress(value);
    } else if (key.equals(DPPConstants.dpDescriptionHeader)) {
      setDescription(value);
    } else if (key.equals(DPPConstants.dpDocURLHeader)) {
      setDocURL(value);
    } else if (key.equals(DPPConstants.dpVendorHeader)) {
      setVendor(value);
    } else if (key.equals(DPPConstants.dpLicenseHeader)) {
      setLicense(value);
    } else {
      boolean found = false;
      for (int i = 0; i < otherHeaders.size(); i++) {
        Header header = (Header) otherHeaders.elementAt(i);
        if (header.getKey().equals(key)) {
          header.setValue(value);
          found = true;
          break;
        }
      }
      if (!found) {
        if (!found) {
          otherHeaders.addElement(new Header(key, value));
        }
      }
    }
  }

  /**
   * removes a single header no matter if its standard or not. The SymbolicName
   * and the Version headers cannot be removed.
   *
   * @param key
   *          the name of the header to be removed.
   */
  public void removeElement(String key) {
    if (key.equals(DPPConstants.dpSymbolicNameHeader)) {
      // do not remove the symbolic name
    } else if (key.equals(DPPConstants.dpVersionHeader)) {
      // do not remove the version
    } else if (key.equals(DPPConstants.dpFixPackHeader)) {
      setIsFixPack(false);
      setFixPack(null);
    } else if (key.equals(DPPConstants.dpCopyrightHeader)) {
      setCopyRight(null);
    } else if (key.equals(DPPConstants.dpAddressHeader)) {
      setContactAddress(null);
    } else if (key.equals(DPPConstants.dpDescriptionHeader)) {
      setDescription(null);
    } else if (key.equals(DPPConstants.dpDocURLHeader)) {
      setDocURL(null);
    } else if (key.equals(DPPConstants.dpVendorHeader)) {
      setVendor(null);
    } else if (key.equals(DPPConstants.dpLicenseHeader)) {
      setLicense(null);
    } else {
      for (int i = 0; i < otherHeaders.size(); i++) {
        Header header = (Header) otherHeaders.elementAt(i);
        if (header.getKey().equals(key)) {
          otherHeaders.removeElementAt(i);
          break;
        }
      }
    }
  }

  /**
   * Returns all headers as a vector of Header objects
   *
   * @return a vector contining all (including non standard) heeaders of this
   *         package
   */
  public Vector getHeadersAsVector() {
    Vector vector = new Vector();
    String str = getSymbolicName();
    Header header = new Header(DPPConstants.dpSymbolicNameHeader, (str == null ? "" : str));
    vector.addElement(header);
    str = getVersion();
    header = new Header(DPPConstants.dpVersionHeader, (str == null ? "" : str));
    vector.addElement(header);
    str = getFixPack();
    if (str != null /* && !str.equals("") */) {
      header = new Header(DPPConstants.dpFixPackHeader, str);
      vector.addElement(header);
    }
    str = getCopyRight();
    if (str != null /* && !str.equals("") */) {
      header = new Header(DPPConstants.dpCopyrightHeader, str);
      vector.addElement(header);
    }
    str = getContactAddress();
    if (str != null /* && !str.equals("") */) {
      header = new Header(DPPConstants.dpAddressHeader, str);
      vector.addElement(header);
    }
    str = getDescription();
    if (str != null /* && !str.equals("") */) {
      header = new Header(DPPConstants.dpDescriptionHeader, str);
      vector.addElement(header);
    }
    str = getDocURL();
    if (str != null /* && !str.equals("") */) {
      header = new Header(DPPConstants.dpDocURLHeader, str);
      vector.addElement(header);
    }
    str = getVendor();
    if (str != null /* && !str.equals("") */) {
      header = new Header(DPPConstants.dpVendorHeader, str);
      vector.addElement(header);
    }
    str = getLicense();
    if (str != null /* && !str.equals("") */) {
      header = new Header(DPPConstants.dpLicenseHeader, str);
      vector.addElement(header);
    }
    Vector otherHeaders = getOtherHeaders();
    for (int i = 0; i < otherHeaders.size(); i++) {
      vector.addElement(otherHeaders.elementAt(i));
    }
    return vector;
  }

  /**
   * This method edits an a header.
   *
   * @param oldKey
   *          the key on which the header was previously set.
   * @param key
   *          the new header name
   * @param value
   *          the new header value.
   */
  public void editElement(String oldKey, String key, String value) {
    if (key.equals(DPPConstants.dpSymbolicNameHeader)) {
      setSymbolicName(value);
    } else if (key.equals(DPPConstants.dpVersionHeader)) {
      setVersion(value);
    } else if (key.equals(DPPConstants.dpFixPackHeader)) {
      setFixPack(value);
    } else if (key.equals(DPPConstants.dpCopyrightHeader)) {
      setCopyRight(value);
    } else if (key.equals(DPPConstants.dpAddressHeader)) {
      setContactAddress(value);
    } else if (key.equals(DPPConstants.dpDescriptionHeader)) {
      setDescription(value);
    } else if (key.equals(DPPConstants.dpDocURLHeader)) {
      setDocURL(value);
    } else if (key.equals(DPPConstants.dpVendorHeader)) {
      setVendor(value);
    } else if (key.equals(DPPConstants.dpLicenseHeader)) {
      setLicense(value);
    } else {
      boolean found = false;
      for (int i = 0; i < otherHeaders.size(); i++) {
        Header header = (Header) otherHeaders.elementAt(i);
        if (header.getKey().equals(oldKey)) {
          header.setKey(key);
          header.setValue(value);
          found = true;
          break;
        }
      }
      if (!found) {
        for (int i = 0; i < otherHeaders.size(); i++) {
          Header header = (Header) otherHeaders.elementAt(i);
          if (header.getKey().equals(key)) {
            found = true;
            break;
          }
        }
        if (!found) {
          otherHeaders.addElement(new Header(key, value));
        }
      }
    }
    if (!key.equals(oldKey)) {
      removeElement(oldKey);
    }
  }

  public boolean isFixPackSet() {
    return isFixPackSet;
  }

  public void setIsFixPack(boolean flag) {
    isFixPackSet = flag;
  }

  /**
   * Clears this object
   *
   */
  public void clear() {
    symbolicName = (symbolicName != null && symbolicName.equals("")) ? "" : symbolicName;
    version = (version != null && version.equals("")) ? "" : version;
    fixPack = (fixPack != null && fixPack.equals("")) ? "" : fixPack;
    copyRight = (copyRight != null && copyRight.equals("")) ? "" : copyRight;
    contactAddress = (contactAddress != null && contactAddress.equals("")) ? "" : contactAddress;
    description = (description != null && description.equals("")) ? "" : description;
    docURL = (docURL != null && docURL.equals("")) ? "" : docURL;
    vendor = (vendor != null && vendor.equals("")) ? "" : vendor;
    license = (license != null && license.equals("")) ? "" : license;
    otherHeaders = new Vector();
  }
}
