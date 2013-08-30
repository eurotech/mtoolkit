/*
 * Copyright (c) 2011 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of ProSyst Software GmbH. You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with ProSyst.
 */
package org.tigris.mtoolkit.common.installation;

import java.io.File;

/**
 * @author Ivo Karabashev
 * @version 1.0
 */
public final class ExternalFileItem extends BaseFileItem {

  public ExternalFileItem(File aFile, String aMimeType) {
    super(aFile, aMimeType);
  }

}
