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

import java.util.Arrays;
import java.util.Comparator;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.osgi.framework.InvalidSyntaxException;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;

/**
 * This class provides functionality for validating DS components.
 */
public class Validator {

	public static final int COMPONENT_NAME = 1;
	public static final int COMPONENT_IMPLCLASS = 2;
	public static final int REFERENCE_NAME = 11;
	public static final int REFERENCE_INTERFACE = 12;
	public static final int REFERENCE_TARGET = 13;
	public static final int REFERENCE_BIND = 14;
	public static final int REFERENCE_UNBIND = 15;
	public static final int SERVICE_NAME = 21;
	public static final int PROP_NAME = 31;
	public static final int PROP_SINGLE_VALUE = 32;
	public static final int PROP_MULTI_VALUE = 33;
	public static final String CDEDITOR_ID = CDEditorPlugin.PLUGIN_ID;

	public static IStatus validateComponentName(String name) {
		if (name == null || name.length() == 0) {
			return new Status(Status.ERROR, CDEDITOR_ID, "Component name is empty.");
		}
		if (!isXMLToken(name)) {
			return new Status(Status.ERROR, CDEDITOR_ID, "Component name '" + name + "' contains invalid characters.");
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateComponentUniqueness(String name, String[] comps) {
		if (!isComponentNameUnique(name, comps)) {
			return new Status(Status.ERROR, CDEDITOR_ID, "Component '" + name + "' already exists.");
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateComponentImplementation(String implClass) {
		if (implClass == null || implClass.length() == 0) {
			return new Status(Status.ERROR, CDEDITOR_ID, "Component must declare its implementation class.");
		}
		IStatus status = JavaConventions.validateJavaTypeName(implClass, CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
		if (status.matches(IStatus.ERROR)) {
			return new Status(IStatus.ERROR, CDEDITOR_ID, "Component implementation class name is not valid. " + status.getMessage());
		} else if (status.matches(IStatus.WARNING)) {
			return new Status(IStatus.WARNING, CDEDITOR_ID, "Component implementation class name is discouraged. " + status.getMessage());
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateComponentEnabled(int enabled) {
		if (enabled == ICDComponent.ENABLED_UNKNOWN) {
			return new Status(IStatus.ERROR, CDEDITOR_ID, "Component's enabled setting is not valid.");
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateComponentImmediate(int immediate) {
		if (immediate == ICDComponent.IMMEDIATE_UNKNOWN) {
			return new Status(IStatus.ERROR, CDEDITOR_ID, "Component's immediate setting is not valid.");
		}
		return Status.OK_STATUS;
	}

	public static IStatus validatePropertyName(String propName) {
		if (propName == null || propName.length() == 0) {
			return new Status(Status.ERROR, CDEDITOR_ID, "Property name is empty.");
		}
		return Status.OK_STATUS;
	}

	public static IStatus validatePropertyType(String name, int type) {
		if (type == ICDProperty.TYPE_UNKNOWN)
			return new Status(Status.ERROR, CDEDITOR_ID, "Property '" + name + "' has invalid type.");
		return Status.OK_STATUS;
	}

	public static IStatus validateMultiPropertyFlatValue(String name, String value, int type) {
		value = value.trim();
		if (value.length() == 0)
			return CDEditorPlugin.newStatus(IStatus.ERROR, "Property '" + name + "' must have at least one value defined.");
		StringTokenizer tokenizer = new StringTokenizer(value, "\n");
		while (tokenizer.hasMoreElements()) {
			String tempValue = (String) tokenizer.nextElement();
			IStatus tempValueStatus = validatePropertyValue(name, tempValue, type);
			if (tempValueStatus.getSeverity() == IStatus.ERROR) {
				return tempValueStatus;
			}
		}
		return Status.OK_STATUS;
	}

	private static IStatus validatePropertyValue(String name, String tempValue, int type) {
		tempValue = tempValue.replaceAll("\r", "");
		if (type == ICDProperty.TYPE_STRING) {
			return Status.OK_STATUS;
		}
		if (type == ICDProperty.TYPE_LONG) {
			try {
				Long.parseLong(tempValue);
				return Status.OK_STATUS;
			} catch (NumberFormatException e) {
				return new Status(Status.ERROR, CDEDITOR_ID, "Property '" + name + "' has errors. '" + tempValue + "' is not a valid long value.");
			}
		}

		if (type == ICDProperty.TYPE_DOUBLE) {
			try {
				Double.parseDouble(tempValue);
				return Status.OK_STATUS;
			} catch (NumberFormatException e) {
				return new Status(Status.ERROR, CDEDITOR_ID, "Property '" + name + "' has errors. '" + tempValue + "' is not a valid double value.");
			}
		}

		if (type == ICDProperty.TYPE_FLOAT) {
			try {
				Float.parseFloat(tempValue);
				return Status.OK_STATUS;
			} catch (NumberFormatException e) {
				return new Status(Status.ERROR, CDEDITOR_ID, "Property '" + name + "' has errors. '" + tempValue + "' is not a valid float value.");
			}
		}

		if (type == ICDProperty.TYPE_INTEGER) {
			try {
				Integer.parseInt(tempValue);
				return Status.OK_STATUS;
			} catch (NumberFormatException e) {
				return new Status(Status.ERROR, CDEDITOR_ID, "Property '" + name + "' has errors. '" + tempValue + "' is not a valid integer value.");
			}
		}

		if (type == ICDProperty.TYPE_BYTE) {
			try {
				Byte.parseByte(tempValue);
				return Status.OK_STATUS;
			} catch (NumberFormatException e) {
				return new Status(Status.ERROR, CDEDITOR_ID, "Property '" + name + "' has errors. '" + tempValue + "' is not a valid byte value.");
			}
		}

		if (type == ICDProperty.TYPE_CHAR) {
			if (tempValue.length() != 1) {
				return new Status(Status.ERROR, CDEDITOR_ID, "Property '" + name + "' has errors. '" + tempValue + "' is not a valid character.");
			} else {
				return Status.OK_STATUS;
			}
		}

		if (type == ICDProperty.TYPE_BOOLEAN) {
			if ((tempValue.equals("true") || tempValue.equals("false"))) {
				return Status.OK_STATUS;
			} else {
				return new Status(Status.ERROR, CDEDITOR_ID, "Property '" + name + "' has errors. '" + tempValue + "' is not a valid boolean value.");
			}
		}

		if (type == ICDProperty.TYPE_SHORT) {
			try {
				Short.parseShort(tempValue);
				return Status.OK_STATUS;
			} catch (NumberFormatException e) {
				return new Status(Status.ERROR, CDEDITOR_ID, "Property '" + name + "' has errors. '" + tempValue + "' is not a valid short value.");
			}
		}

		return Status.OK_STATUS;
	}

	public static IStatus validateMultiPropertyValue(String name, String[] values, int type) {
		for (int i = 0; i < values.length; i++) {
			IStatus status = validatePropertyValue(name, values[i], type);
			if (!status.isOK())
				return status;
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateSinglePropertyValue(String name, String value, int type) {
		if (value == null || value.length() == 0)
			return new Status(IStatus.ERROR, CDEDITOR_ID, "Property '" + name + "' must declare value.");
		return validatePropertyValue(name, value, type);
	}

	public static IStatus validateServiceFactory(int serviceFactory) {
		if (serviceFactory == ICDService.SERVICE_FACTORY_UNKNOWN) {
			return new Status(IStatus.ERROR, CDEDITOR_ID, "Service declaration has invalid service factory setting.");
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateServiceInterface(String serviceInterface) {
		if (serviceInterface == null || serviceInterface.length() == 0) {
			return new Status(Status.ERROR, CDEDITOR_ID, "Provided service interface name cannot be empty.");
		}
		IStatus status = JavaConventions.validateJavaTypeName(serviceInterface, CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
		if (status.matches(IStatus.ERROR)) {
			return new Status(IStatus.ERROR, CDEDITOR_ID, "Provided service interface name is not valid. " + status.getMessage());
		} else if (status.matches(IStatus.WARNING)) {
			return new Status(IStatus.WARNING, CDEDITOR_ID, "Provided service interface name is discouraged. " + status.getMessage());
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateReferenceCardinality(String refName, int cardinality) {
		if (cardinality == ICDReference.CARDINALITY_UNKNOWN) {
			return new Status(IStatus.ERROR, CDEDITOR_ID, "Reference '" + refName + "' has invalid cardinality.");
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateReferencePolicy(String refName, int policy) {
		if (policy == ICDReference.POLICY_UNKNOWN) {
			return new Status(IStatus.ERROR, CDEDITOR_ID, "Reference '" + refName + "' has invalid policy.");
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateReferenceUnbindMethod(String refName, String text) {
		if (text != null && text.length() > 0) {
			IStatus status = JavaConventions.validateMethodName(text, CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
			if (status.matches(IStatus.ERROR)) {
				return new Status(IStatus.ERROR, CDEDITOR_ID, "Reference '" + refName + "' unbind method name is not valid. " + status.getMessage());
			} else if (status.matches(IStatus.WARNING)) {
				return new Status(IStatus.WARNING, CDEDITOR_ID, "Reference '" + refName + "' unbind method name is discouraged. " + status.getMessage());
			}
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateReferenceBindMethod(String refName, String text) {
		if (text != null && text.length() > 0) {
			IStatus status = JavaConventions.validateMethodName(text, CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
			if (status.matches(IStatus.ERROR)) {
				return new Status(IStatus.ERROR, CDEDITOR_ID, "Reference '" + refName + "' bind method name is not valid. " + status.getMessage());
			} else if (status.matches(IStatus.WARNING)) {
				return new Status(IStatus.WARNING, CDEDITOR_ID, "Reference '" + refName + "' bind method name is discouraged. " + status.getMessage());
			}
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateReferenceTarget(String refName, String refTarget) {
		if (refTarget == null || refTarget.length() == 0) { // optional
			return Status.OK_STATUS;
		}
		try {
			CDEditorPlugin.getBundleContext().createFilter(refTarget);
			return Status.OK_STATUS;
		} catch (InvalidSyntaxException e) {
			return new Status(IStatus.ERROR, CDEDITOR_ID, "Reference \"" + refName + "\" target is not valid. " + e.getMessage());
		}
	}

	public static IStatus validateReferenceInterface(String refName, String refInterface) {
		if (refInterface == null || refInterface.length() == 0) {
			return new Status(Status.ERROR, CDEDITOR_ID, "Reference '" + refName + "' interface name cannot be empty.");
		}
		IStatus status = JavaConventions.validateJavaTypeName(refInterface, CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
		if (status.matches(IStatus.ERROR)) {
			return new Status(IStatus.ERROR, CDEDITOR_ID, "Reference '" + refName + "' interface name is not valid. " + status.getMessage());
		} else if (status.matches(IStatus.WARNING)) {
			return new Status(IStatus.WARNING, CDEDITOR_ID, "Reference '" + refName + "' interface name is discouraged. " + status.getMessage());
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateReferenceName(String name) {
		if (name == null || name.length() == 0) {
			return new Status(Status.ERROR, CDEDITOR_ID, "Reference name cannot be empty.");
		}
		if (!isXMLNMToken(name)) {
			return new Status(Status.ERROR, CDEDITOR_ID, "Reference '" + name + "' name contains invalid characters.");
		}
		return Status.OK_STATUS;
	}

	private static boolean isXMLNMToken(String name) {
		final String nmTokenChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.-_:";
		char[] chars = name.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (nmTokenChars.indexOf(chars[i]) == -1) {
				return false;
			}
		}
		return true;
	}

	public static IStatus validatePropertiesEntryName(String name) {
		if (name == null || name.length() == 0) {
			return new Status(Status.ERROR, CDEDITOR_ID, "Properties entry name cannot be empty.");
		}
		return Status.OK_STATUS;
	}

	private static boolean isXMLToken(String name) {
		if (name == null || name.equals("")) {
			return false;
		}
		if (!name.trim().equals(name)) {
			return false;
		}
		if (name.indexOf("  ") > -1) {
			return false;
		}
		if (name.indexOf('\r') > -1) {
			return false;
		}
		if (name.indexOf('\n') > -1) {
			return false;
		}
		if (name.indexOf('\t') > -1) {
			return false;
		}
		return true;
	}

	private static boolean isComponentNameUnique(String name, String[] comps) {
		if (comps == null)
			return true;
		for (int i = 0; i < comps.length; i++) {
			if (name.equals(comps[i])) {
				return false;
			}
		}
		return true;
	}

	public static IStatus validateComponent(String name, String implClass, String[] existingComponents) {
		return pickValidationStatus(new IStatus[] { validateComponentName(name), validateComponentUniqueness(name, existingComponents), validateComponentImplementation(implClass) });
	}

	public static IStatus pickValidationStatus(IStatus[] statuses) {
		Arrays.sort(statuses, new Comparator() {
			public int compare(Object o1, Object o2) {
				IStatus s1 = (IStatus) o1;
				IStatus s2 = (IStatus) o2;
				if (s1.getSeverity() < s2.getSeverity()) {
					return 1;
				} else if (s1.getSeverity() > s2.getSeverity()) {
					return -1;
				} else {
					return 0;
				}
			}

		});
		return statuses[0];
	}
}
