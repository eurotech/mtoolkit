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
package org.tigris.mtoolkit.cdeditor.internal.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.integration.JDTModelHelper;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDInterface;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDProperties;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDProperty;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDReference;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDService;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.Validator;
import org.tigris.mtoolkit.cdeditor.internal.text.PlainDocumentElementNode;


public class ComponentDescriptionValidator {

	private static final String SERVICE_REFERENCE_CLASSNAME = "org.osgi.framework.ServiceReference";
	private static final String COMPONENT_CONTEXT_CLASSNAME = "org.osgi.service.component.ComponentContext";

	private static final String DEACTIVATE_METHOD_NAME = "deactivate";
	private static final String ACTIVATE_METHOD_NAME = "activate";

	public static final String SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.0.0";
	public static final String SCR_NAMESPACE_V11 = "http://www.osgi.org/xmlns/scr/v1.1.0";
	
	private static final int NAMESPACE_V10 = 1;
	private static final int NAMESPACE_V11 = 2;

	private IEclipseContext context;

	public ComponentDescriptionValidator(IEclipseContext context) {
		this.context = context;
	}

	public List validateDocument(IDocumentElementNode root) {
		List result = new ArrayList();
		doValidateDocument(root, result, true);
		return result;
	}

	private void doValidateDocument(IDocumentElementNode node, List results, boolean root) {
		String tagName = node.getXMLTagName();
		if (tagName != null && ICDComponent.TAG_COMPONENT.equals(ModelUtil.getElementLocalName(tagName))) {
			validateComponent(node, results, root);
		} else {
			IDocumentElementNode[] children = node.getChildNodes();
			for (int i = 0; i < children.length; i++) {
				IDocumentElementNode child = children[i];
				doValidateDocument(child, results, false);
			}
		}
	}

	private int validateComponentNamespace(IDocumentElementNode node, List results, boolean root) {
		if (node instanceof PlainDocumentElementNode) {
			String namespace = ((PlainDocumentElementNode) node).getNamespace();
			if (!SCR_NAMESPACE.equals(namespace) && !SCR_NAMESPACE_V11.equals(namespace) && !("".equals(namespace) && root)) {
				if ("".equals(namespace))
					addStatus(results, new Status(Status.ERROR, CDEditorPlugin.PLUGIN_ID, "Element '" + node.getXMLTagName() + "' must have correct namespace."), node);
				else
					addStatus(results, new Status(Status.ERROR, CDEditorPlugin.PLUGIN_ID, "Element '" + node.getXMLTagName() + "' has incorrect namespace '" + namespace + "'."), node);
			}
			if (SCR_NAMESPACE_V11.equals(namespace))
				return NAMESPACE_V11;
		}
		return NAMESPACE_V10;
	}

	private void validateInnerElementNamespace(IDocumentElementNode node, List results) {
		// inner elements must not be prefixed
		if (node instanceof PlainDocumentElementNode) {
			if (node.getXMLTagName().indexOf(':') != -1) {
				addStatus(results, new Status(Status.ERROR, CDEditorPlugin.PLUGIN_ID, "Element '" + node.getXMLTagName() + "' is local to the enclosing 'component' element and must be unqualified."), node);
			}
		}
	}

	private void validateComponent(IDocumentElementNode node, List results, boolean root) {
		List visited = new ArrayList();

		IDocumentAttributeNode attrName = node.getDocumentAttribute(ICDComponent.ATTR_NAME);
		if (attrName != null)
			addStatus(results, Validator.validateComponentName(attrName.getAttributeValue()), attrName);
		else
			addStatus(results, Validator.validateComponentName(null), node);

		int namespaceVersion = validateComponentNamespace(node, results, root); 

		IDocumentAttributeNode attrEnabled = node.getDocumentAttribute(ICDComponent.ATTR_ENABLED);
		if (attrEnabled != null) {
			int enabled = ModelUtil.parseEnumerateValue(attrEnabled.getAttributeValue(), ICDComponent.ENABLED_NAMES, ICDComponent.ENABLED_DEFAULT, ICDComponent.ENABLED_UNKNOWN);
			addStatus(results, Validator.validateComponentEnabled(enabled), attrEnabled);
		}

		int immediate = ICDComponent.IMMEDIATE_DEFAULT;
		IDocumentAttributeNode attrImmediate = node.getDocumentAttribute(ICDComponent.ATTR_IMMEDIATE);
		if (attrImmediate != null) {
			immediate = ModelUtil.parseEnumerateValue(attrImmediate.getAttributeValue(), ICDComponent.IMMEDIATE_NAMES, ICDComponent.IMMEDIATE_DEFAULT, ICDComponent.IMMEDIATE_UNKNOWN);
			addStatus(results, Validator.validateComponentImmediate(immediate), attrImmediate);
		}

		IType implementationType = null;
		IDocumentElementNode[] implementationNodes = ModelUtil.findChildNode(node, ICDComponent.TAG_IMPLEMENTATION);
		if (implementationNodes.length == 0) {
			addStatus(results, Validator.validateComponentImplementation(null), node);
		} else {
			IDocumentElementNode implementationNode = implementationNodes[0];
			IDocumentAttributeNode attrClass = implementationNode.getDocumentAttribute(ICDComponent.ATTR_CLASS);
			if (attrClass != null) {
				addStatus(results, Validator.validateComponentImplementation(attrClass.getAttributeValue()), attrClass);

				if (context != null) {
					implementationType = context.findBundleClass(attrClass.getAttributeValue());
					if (implementationType == null)
						addStatus(results, new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Component implementation " + attrClass.getAttributeValue() + " is not accessible for the bundle."), attrClass);
					else {
						if (isTypeAbstract(implementationType))
							addStatus(results, new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Component implementation " + attrClass.getAttributeValue() + " is an abstract type and cannot be instantiated."), attrClass);

						if (!hasTypeDefaultPublicConstructor(implementationType))
							addStatus(results, new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Component implementation " + attrClass.getAttributeValue() + " doesn't have public constructor without arguments and cannot be instantiated."), attrClass);

						if (namespaceVersion != NAMESPACE_V11) {
						validateActivationMethod(results, implementationType, attrClass, ACTIVATE_METHOD_NAME);
						validateActivationMethod(results, implementationType, attrClass, DEACTIVATE_METHOD_NAME);
						}

						if (isInnerNonStaticType(implementationType))
							addStatus(results, new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Component implementation " + attrClass.getAttributeValue() + " is inner non-static class and cannot be instantiated."), attrClass);
					}
				}
			} else
				addStatus(results, Validator.validateComponentImplementation(null), implementationNode);

			validateInnerElementNamespace(implementationNode, results);
			visited.add(implementationNode);

			if (implementationNodes.length > 1) {
				for (int i = 1; i < implementationNodes.length; i++) {
					implementationNode = implementationNodes[i];
					addStatus(results, new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Component description cannot contain more than one implementation declaration"), implementationNode);
					visited.add(implementationNode);
				}
			}
		}

		IDocumentElementNode[] referenceNodes = ModelUtil.findChildNode(node, ICDReference.TAG_REFERENCE);
		for (int i = 0; i < referenceNodes.length; i++) {
			IDocumentElementNode referenceNode = referenceNodes[i];
			validateInnerElementNamespace(referenceNode, results);
			validateReferenceNode(referenceNode, implementationType, namespaceVersion, results);
			visited.add(referenceNode);
		}

		IDocumentElementNode[] propertyNodes = ModelUtil.findChildNode(node, ICDProperty.TAG_PROPERTY);
		for (int i = 0; i < propertyNodes.length; i++) {
			IDocumentElementNode propertyNode = propertyNodes[i];
			validateInnerElementNamespace(propertyNode, results);
			validatePropertyNode(propertyNode, results);
			visited.add(propertyNode);
		}

		IDocumentElementNode[] propertiesNodes = ModelUtil.findChildNode(node, ICDProperties.TAG_PROPERTIES);
		for (int i = 0; i < propertiesNodes.length; i++) {
			IDocumentElementNode propertiesNode = propertiesNodes[i];
			validateInnerElementNamespace(propertiesNode, results);
			validatePropertiesNode(propertiesNode, results);
			visited.add(propertiesNode);
		}

		IDocumentElementNode[] servicesNodes = ModelUtil.findChildNode(node, ICDService.TAG_SERVICE);
		if (servicesNodes.length > 0) {
			IDocumentElementNode serviceNode = servicesNodes[0];
			validateInnerElementNamespace(serviceNode, results);
			validateServiceNode(serviceNode, implementationType, results);
			visited.add(serviceNode);
			if (servicesNodes.length > 1) {
				for (int i = 0; i < servicesNodes.length; i++) {
					serviceNode = servicesNodes[i];
					validateInnerElementNamespace(serviceNode, results);
					addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Component description cannot contain more than one service declaration"), serviceNode);
					visited.add(serviceNode);
				}
			}
		}

		IDocumentAttributeNode attrFactory = node.getDocumentAttribute(ICDComponent.ATTR_FACTORY);

		// validate immediate attribute after we know whether we have service
		// node
		if (attrImmediate != null && attrImmediate.getAttributeValue().trim().length() > 0 && immediate != ICDComponent.IMMEDIATE_UNKNOWN) {
			/*
			 * OSGi Specification excerpt: If this attribute (immediate) is
			 * specified, its value must be true unless the service element is
			 * also specified.
			 */
			if (attrFactory != null && attrFactory.getAttributeValue().trim().length() > 0) {
				if (immediate == ICDComponent.IMMEDIATE_YES)
					addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Factory component cannot be immediate."), attrImmediate);
			} else {
				if (immediate != ICDComponent.IMMEDIATE_YES && servicesNodes.length == 0)
					addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Component must be immediate, because it doesn't contain service element."), attrImmediate);
			}
		}

		IDocumentElementNode[] children = node.getChildNodes();
		for (int i = 0; i < children.length; i++) {
			IDocumentElementNode child = children[i];
			if (!visited.contains(child)) {
				addStatus(results, CDEditorPlugin.newStatus(IStatus.WARNING, "Component description contains unrecognized element: " + child.getXMLTagName()), child);
				// no need to add it to visited list
			}
		}
	}

	private boolean isInnerNonStaticType(IType implementationType) {
		if (implementationType.getDeclaringType() != null) {
			try {
				if (!Flags.isStatic(implementationType.getFlags())) {
					return true;
				}
			} catch (JavaModelException e) {
			}
		}
		return false;
	}

	private void validateActivationMethod(List results, IType implementationType, IDocumentAttributeNode attrClass, String methodName) {
		IMethod activationMethod = findActivationMethod(implementationType, methodName);
		if (activationMethod != null) {
			if (!isMethodAtLeastProtected(activationMethod))
				addStatus(results, new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Component implementation " + attrClass.getAttributeValue() + " contains '" + methodName + "' method with not enough visibility. Activation methods must be at least protected."), attrClass);
			else if (isMethodPublic(activationMethod))
				addStatus(results, new Status(IStatus.WARNING, CDEditorPlugin.PLUGIN_ID, "Component implementation " + attrClass.getAttributeValue() + " contains '" + methodName + "' method with public visibility. OSGi specification recommends that activation methods are protected."), attrClass);
		} else {
			if (context.findMethod(implementationType, methodName, null, false) != null)
				addStatus(results, new Status(IStatus.WARNING, CDEditorPlugin.PLUGIN_ID, "Component implementation " + attrClass.getAttributeValue() + " contains '" + methodName + "' method with unrecognized signature."), attrClass);
		}
	}

	private void validatePropertiesNode(IDocumentElementNode node, List results) {
		IDocumentAttributeNode attrEntry = node.getDocumentAttribute(ICDProperties.ATTR_ENTRY);
		if (attrEntry != null) {
			addStatus(results, Validator.validatePropertiesEntryName(attrEntry.getAttributeValue()), attrEntry);
			if (context != null) {
				if (!context.doesBundleFileExist(new Path(attrEntry.getAttributeValue())))
					addStatus(results, new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Referenced resource " + attrEntry.getAttributeValue() + " cannot be found."), attrEntry);
				else if (!context.isBundleFilePackaged(new Path(attrEntry.getAttributeValue())))
					addStatus(results, new Status(IStatus.WARNING, CDEditorPlugin.PLUGIN_ID, "Referenced resource " + attrEntry.getAttributeValue() + " is not packaged."), attrEntry);
			}
		} else {
			addStatus(results, Validator.validatePropertiesEntryName(null), node);
		}
	}

	private void validateServiceNode(IDocumentElementNode node, IType implementationType, List results) {
		IDocumentAttributeNode attrServiceFactory = node.getDocumentAttribute(ICDService.ATTR_SERVICEFACTORY);
		if (attrServiceFactory != null) {
			int serviceFactory = ModelUtil.parseEnumerateValue(attrServiceFactory.getAttributeValue(), ICDService.SERVICE_FACTORY_NAMES, ICDService.SERVICE_FACTORY_DEFAULT, ICDService.SERVICE_FACTORY_UNKNOWN);
			addStatus(results, Validator.validateServiceFactory(serviceFactory), attrServiceFactory);
			if (serviceFactory == ICDService.SERVICE_FACTORY_YES) {
				IDocumentElementNode parentComponent = node.getParentNode();
				String factory = parentComponent.getXMLAttributeValue(ICDComponent.ATTR_FACTORY);
				if (factory != null && factory.length() > 0) {
					addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Factory component cannot provide service factory."), attrServiceFactory);
				} else {
					int immediateAttribute = ModelUtil.parseEnumerateValue(parentComponent.getXMLAttributeValue(ICDComponent.ATTR_IMMEDIATE), ICDComponent.IMMEDIATE_NAMES, ICDComponent.IMMEDIATE_DEFAULT, ICDComponent.IMMEDIATE_UNKNOWN);
					if (immediateAttribute == ICDComponent.IMMEDIATE_YES)
						addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "An immediate component cannot provide service factory."), attrServiceFactory);
				}
			}
		}

		List visited = new ArrayList();

		IDocumentElementNode[] provideNodes = ModelUtil.findChildNode(node, ICDService.TAG_PROVIDE);
		if (provideNodes.length == 0) {
			addStatus(results, new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Service element must have at least one interface declared."), node);
		} else {
			for (int i = 0; i < provideNodes.length; i++) {
				IDocumentElementNode provideNode = provideNodes[i];
				validateInnerElementNamespace(provideNode, results);
				IDocumentAttributeNode attrInterface = provideNode.getDocumentAttribute(ICDInterface.ATTR_INTERFACE);
				if (attrInterface != null) {
					addStatus(results, Validator.validateServiceInterface(attrInterface.getAttributeValue()), attrInterface);

					if (context != null && implementationType != null) {
						if (!context.doesTypeExtend(implementationType, attrInterface.getAttributeValue())) {
							addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Component implementation type doesn't extend " + attrInterface.getAttributeValue() + "."), attrInterface);
						}
						IType serviceType = context.findBundleClass(attrInterface.getAttributeValue());
						if (serviceType != null) {
							try {
								if (!serviceType.isInterface()) {
									addStatus(results, CDEditorPlugin.newStatus(IStatus.WARNING, "Provided element " + attrInterface.getAttributeValue() + " is not an interface."), attrInterface);
								}
							} catch (JavaModelException e) {
								CDEditorPlugin.log(e);
							}
						}
					}
				} else {
					addStatus(results, Validator.validateServiceInterface(null), provideNode);
				}

				visited.add(provideNode);
			}
		}

		IDocumentElementNode[] children = node.getChildNodes();
		for (int i = 0; i < children.length; i++) {
			IDocumentElementNode child = children[i];
			if (!visited.contains(child)) {
				addStatus(results, new Status(IStatus.WARNING, CDEditorPlugin.PLUGIN_ID, "Service element contains unrecognized element: " + child.getXMLTagName()), child);
				// no need to add to the visited node
			}
		}
	}

	private void validatePropertyNode(IDocumentElementNode node, List results) {
		IDocumentAttributeNode attrName = node.getDocumentAttribute(ICDProperty.ATTR_NAME);
		String name;
		if (attrName != null) {
			addStatus(results, Validator.validatePropertyName(attrName.getAttributeValue()), attrName);
			name = attrName.getAttributeValue();
		} else {
			addStatus(results, Validator.validatePropertyName(null), node);
			name = null;
		}

		IDocumentAttributeNode attrType = node.getDocumentAttribute(ICDProperty.ATTR_TYPE);
		int propertyType = ICDProperty.TYPE_DEFAULT;
		if (attrType != null) {
			propertyType = ModelUtil.parseEnumerateValue(attrType.getAttributeValue(), ICDProperty.TYPE_NAMES, ICDProperty.TYPE_DEFAULT, ICDProperty.TYPE_UNKNOWN);
			addStatus(results, Validator.validatePropertyType(name, propertyType), attrType);
		}

		// validate the property value only if we know what type it is
		if (propertyType != ICDProperty.TYPE_UNKNOWN) {
			IDocumentAttributeNode attrValue = node.getDocumentAttribute(ICDProperty.ATTR_VALUE);
			if (attrValue != null && attrValue.getAttributeValue().length() > 0) {
				addStatus(results, Validator.validateSinglePropertyValue(name, attrValue.getAttributeValue(), propertyType), attrValue);
			} else if (node.getTextNode() != null) {
				addStatus(results, Validator.validateMultiPropertyFlatValue(name, node.getTextNode().getText(), propertyType), node);
			} else {
				addStatus(results, Validator.validateSinglePropertyValue(name, null, propertyType), node);
			}
		}
	}

	private void validateReferenceNode(IDocumentElementNode node, IType implementationType, int namespaceVersion, List results) {
		IDocumentAttributeNode attrName = node.getDocumentAttribute(ICDReference.ATTR_NAME);
		String referenceName;
		if (attrName != null) {
			referenceName = attrName.getAttributeValue();
			if (namespaceVersion != NAMESPACE_V11)
			addStatus(results, Validator.validateReferenceName(attrName.getAttributeValue()), attrName);
		} else {
			referenceName = null;
			if (namespaceVersion != NAMESPACE_V11)
			addStatus(results, Validator.validateReferenceName(null), node);
		}

		IDocumentAttributeNode attrInterface = node.getDocumentAttribute(ICDReference.ATTR_INTERFACE);
		if (attrInterface != null) {
			addStatus(results, Validator.validateReferenceInterface(referenceName, attrInterface.getAttributeValue()), attrInterface);

			if (context != null) {
				IType type = context.findBundleClass(attrInterface.getAttributeValue());
				if (type == null)
					addStatus(results, new Status(IStatus.ERROR, CDEditorPlugin.PLUGIN_ID, "Referenced type " + attrInterface.getAttributeValue() + " is not accessible for the bundle."), attrInterface);
			}
		} else {
			addStatus(results, Validator.validateReferenceInterface(referenceName, null), node);
		}

		IDocumentAttributeNode attrCardinality = node.getDocumentAttribute(ICDReference.ATTR_CARDINALITY);
		if (attrCardinality != null) {
			int cardinality = ModelUtil.parseEnumerateValue(attrCardinality.getAttributeValue(), ICDReference.CARDINALITY_NAMES_SHORT, ICDReference.CARDINALITY_DEFAULT, ICDReference.CARDINALITY_UNKNOWN);
			addStatus(results, Validator.validateReferenceCardinality(referenceName, cardinality), attrCardinality);
		}

		IDocumentAttributeNode attrPolicy = node.getDocumentAttribute(ICDReference.ATTR_POLICY);
		if (attrPolicy != null) {
			int policy = ModelUtil.parseEnumerateValue(attrPolicy.getAttributeValue(), ICDReference.POLICY_NAMES, ICDReference.POLICY_DEFAULT, ICDReference.POLICY_UNKNOWN);
			addStatus(results, Validator.validateReferencePolicy(referenceName, policy), attrPolicy);
		}

		IDocumentAttributeNode attrTarget = node.getDocumentAttribute(ICDReference.ATTR_TARGET);
		if (attrTarget != null) {
			addStatus(results, Validator.validateReferenceTarget(referenceName, attrTarget.getAttributeValue()), attrTarget);
		} else {
			addStatus(results, Validator.validateReferenceTarget(referenceName, null), node);
		}

		IDocumentAttributeNode attrBind = node.getDocumentAttribute(ICDReference.ATTR_BIND);
		if (attrBind != null) {
			addStatus(results, Validator.validateReferenceBindMethod(referenceName, attrBind.getAttributeValue()), attrBind);
			if (namespaceVersion != NAMESPACE_V11 && context != null && implementationType != null && attrInterface != null && attrBind.getAttributeValue().length() > 0) {
				IMethod foundMethod = findBindingMethod(implementationType, attrBind.getAttributeValue(), attrInterface.getAttributeValue(), null);
				if (foundMethod == null) {
					if (context.findMethod(implementationType, attrBind.getAttributeValue(), null, false) != null) {
						addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Bind method " + attrBind.getAttributeValue() + " has invalid signature."), attrBind);
					} else {
						addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Bind method " + attrBind.getAttributeValue() + " cannot be found."), attrBind);
					}
				} else if (!isMethodAtLeastProtected(foundMethod))
					addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Bind method " + attrBind.getAttributeValue() + " doesn't have enough visibility. Methods specified as bind/unbind must be at least protected."), attrBind);
				else if (isMethodPublic(foundMethod))
					addStatus(results, CDEditorPlugin.newStatus(IStatus.WARNING, "Bind method " + attrBind.getAttributeValue() + " is public. It is recommended that bind/unbind methods are protected, so they don't appear on the component public API."), attrBind);
			}
		} else {
			addStatus(results, Validator.validateReferenceBindMethod(referenceName, null), node);
		}

		IDocumentAttributeNode attrUnbind = node.getDocumentAttribute(ICDReference.ATTR_UNBIND);
		if (attrUnbind != null) {
			addStatus(results, Validator.validateReferenceUnbindMethod(referenceName, attrUnbind.getAttributeValue()), attrUnbind);
			if (namespaceVersion != NAMESPACE_V11 && context != null && implementationType != null && attrInterface != null && attrUnbind.getAttributeValue().length() > 0) {
				IMethod foundMethod = findBindingMethod(implementationType, attrUnbind.getAttributeValue(), attrInterface.getAttributeValue(), null);
				if (foundMethod == null) {
					if (context.findMethod(implementationType, attrUnbind.getAttributeValue(), null, false) != null) {
						addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Unbind method " + attrUnbind.getAttributeValue() + " has invalid signature."), attrUnbind);
					} else {
						addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Unbind method " + attrUnbind.getAttributeValue() + " cannot be found."), attrUnbind);
					}
				} else if (!isMethodAtLeastProtected(foundMethod))
					addStatus(results, CDEditorPlugin.newStatus(IStatus.ERROR, "Unbind method " + attrUnbind.getAttributeValue() + " doesn't have enough visibility. Methods specified as bind/unbind must be at least protected."), attrUnbind);
				else if (isMethodPublic(foundMethod))
					addStatus(results, CDEditorPlugin.newStatus(IStatus.WARNING, "Unbind method " + attrUnbind.getAttributeValue() + " is public. It is recommended that bind/unbind methods are protected, so they don't appear on the component public API."), attrUnbind);
			}
		} else {
			addStatus(results, Validator.validateReferenceUnbindMethod(referenceName, null), node);
		}
	}

	private boolean hasTypeDefaultPublicConstructor(IType type) {
		Assert.isNotNull(type);
		try {
			final IMethod[] result = new IMethod[1];
			final IType[] parameters = new IType[0];
			JDTModelHelper.visitTypeSuperclasses(type, new JDTModelHelper.ITypeHierarchyVisitor() {
				public boolean visit(IType visitingType) {
					IMethod method = context.findMethod(visitingType, visitingType.getElementName(), parameters, false);
					if (method == null && context.findMethod(visitingType, visitingType.getElementName(), null, false) == null)
						// continue to superclass if not found and the type
						// doesn't have other constructors
						return true;
					result[0] = method;
					return false;
				}
			});
			if (result[0] == null)
				return false;
			return Flags.isPublic(result[0].getFlags());
		} catch (JavaModelException e) {
			CDEditorPlugin.log(e);
			return false;
		}
	}

	private IMethod findActivationMethod(IType type, final String methodName) {
		try {
			Assert.isNotNull(type);
			Assert.isNotNull(methodName);
			final IType[] componentContextParam = JDTModelHelper.findTypes(type, new String[] { COMPONENT_CONTEXT_CLASSNAME });
			if (componentContextParam == null)
				return null;
			final IMethod[] result = new IMethod[1];
			JDTModelHelper.visitTypeSuperclasses(type, new JDTModelHelper.ITypeHierarchyVisitor() {
				public boolean visit(IType visitingType) {
					IMethod method = context.findMethod(visitingType, methodName, componentContextParam, false);
					if (method != null) {
						result[0] = method;
						return false;
					} else {
						return true;
					}
				}
			});
			return result[0];
		} catch (JavaModelException e) {
			CDEditorPlugin.log(e);
			return null;
		}
	}

	private boolean isTypeAbstract(IType type) {
		Assert.isNotNull(type);
		int flags;
		try {
			flags = type.getFlags();
			if (Flags.isAbstract(flags) || Flags.isInterface(flags))
				return true;
		} catch (JavaModelException e) {
			CDEditorPlugin.log(e);
		}
		return false;
	}

	private IMethod findBindingMethod(IType implementation, final String methodName, String serviceInterface, ITypeHierarchy hierarchy) {
		try {
			Assert.isNotNull(implementation);
			Assert.isNotNull(serviceInterface);
			final IType[] serviceRefTypeParam = JDTModelHelper.findTypes(implementation, new String[] { SERVICE_REFERENCE_CLASSNAME });
			final IType[] objectTypeParam = JDTModelHelper.findTypes(implementation, new String[] { serviceInterface });
			final IMethod[] result = new IMethod[1];
			JDTModelHelper.visitTypeSuperclasses(implementation, new JDTModelHelper.ITypeHierarchyVisitor() {
				public boolean visit(IType type) {
					IMethod method = null;
					if (serviceRefTypeParam != null)
						method = context.findMethod(type, methodName, serviceRefTypeParam, false);
					if (method == null && objectTypeParam != null)
						method = context.findMethod(type, methodName, objectTypeParam, true);
					if (method == null)
						return true;
					result[0] = method;
					return false;
				}
			});
			return result[0];
		} catch (JavaModelException e) {
			CDEditorPlugin.log(e);
			return null;
		}
	}

	private boolean isMethodPublic(IMethod method) {
		Assert.isNotNull(method);
		try {
			int methodFlags = method.getFlags();
			if (Flags.isPublic(methodFlags))
				return true;
			if (method.getDeclaringType().isInterface() && Flags.isPackageDefault(methodFlags))
				return true;
		} catch (JavaModelException e) {
		}
		return false;
	}

	private boolean isMethodAtLeastProtected(IMethod method) {
		Assert.isNotNull(method);
		try {
			int methodFlags = method.getFlags();
			if (Flags.isPublic(methodFlags) || Flags.isProtected(methodFlags))
				return true;
			if (method.getDeclaringType().isInterface() && Flags.isPackageDefault(methodFlags))
				return true;
		} catch (JavaModelException e) {
		}
		return false;
	}

	private void addStatus(List list, IStatus status, IDocumentRange element) {
		if (!status.isOK()) {
			list.add(new ValidationResult(status, element));
		}
	}

	private void addStatus(List list, IStatus status, IDocumentRange element, int offset, int length) {
		if (!status.isOK()) {
			list.add(new ValidationResult(status, element));
		}
	}
}
