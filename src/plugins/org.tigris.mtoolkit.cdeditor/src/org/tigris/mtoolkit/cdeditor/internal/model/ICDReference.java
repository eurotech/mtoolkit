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
package org.tigris.mtoolkit.cdeditor.internal.model;

/**
 * ICDReference represents a reference element in one DS component. One 
 * component can have many references.
 */
public interface ICDReference extends ICDElement {

	public static final int CARDINALITY_UNKNOWN = 0;
	public static final int CARDINALITY_0_1 = 1;
	public static final int CARDINALITY_0_N = 2;
	public static final int CARDINALITY_1_1 = 3;
	public static final int CARDINALITY_1_N = 4;
	public static final int CARDINALITY_DEFAULT = CARDINALITY_1_1;
	
	public static final String[] CARDINALITY_NAMES_SHORT = {
		"0..1", //$NON-NLS-1$
		"0..n", //$NON-NLS-1$
		"1..1", //$NON-NLS-1$
		"1..n", //$NON-NLS-1$
	};
	
	public static final String[] CARDINALITY_NAMES_LONG = {
		"optional unary",
		"optional multiple",
		"mandatory unary",
		"mandatory multiple",
	};

	public static final int POLICY_UNKNOWN = 0;
	public static final int POLICY_STATIC  = 1;
	public static final int POLICY_DYNAMIC = 2;
	public static final int POLICY_DEFAULT = POLICY_STATIC;
	
	public static final String[] POLICY_NAMES = {
		"static",
		"dynamic",
	};
	
	public static final String ATTR_POLICY = "policy";
	public static final String ATTR_CARDINALITY = "cardinality";
	public static final String ATTR_UNBIND = "unbind";
	public static final String ATTR_TARGET = "target";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_INTERFACE = "interface";
	public static final String TAG_REFERENCE = "reference";
	public static final String ATTR_BIND = "bind";

	public String getName();
	public void setName(String name);

	public String getInterface();
	public void setInterface(String value);

	public int getCardinality();
	public String getRawCardinality();
	public void setCardinality(int cardinality);

	public int getPolicy();
	public String getRawPolicy();
	public void setPolicy(int policy);

	public String getTarget();
	public void setTarget(String target);

	public String getBind();
	public void setBind(String bind);

	public String getUnbind();
	public void setUnbind(String unbind);

}
