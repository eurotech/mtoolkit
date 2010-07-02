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
package org.tigris.mtoolkit.iagent;

import org.tigris.mtoolkit.iagent.internal.utils.ExceptionCodeHelper;

/**
 * Instrumentation Agent error codes.
 * 
 * Ranges:
 * 
 * <table>
 * <tr>
 * <td>Range</td>
 * <td>Description</td>
 * </tr>
 * <tr>
 * <td>-3000</td>
 * <td>VM Manager related errors. All possible errors are defined in
 * {@link IAgentErrors} class.</td>
 * </tr>
 * <tr>
 * <td>-4000</td>
 * <td>File Manager related errors. All possible errors are defined in
 * {@link IAgentErrors} class.</td>
 * </tr>
 * <tr>
 * <td>-5000</td>
 * <td>General IAgent related errors. All possible errors are defined in
 * {@link IAgentErrors} class.</td>
 * </tr>
 * <tr>
 * <td>-6000</td>
 * <td>Bundle related errors. More information about the exception can be found
 * in the exception message</td>
 * </tr>
 * <tr>
 * <td>-7000</td>
 * <td>Application related errors. Besides the values defined in
 * {@link IAgentErrors} class, additional codes are available from
 * ApplicationException class. You can convert IAgentErrors code to
 * ApplicationException code using the provided method
 * {@link #toApplicationExceptionCode(int)}.</td>
 * </tr>
 * <tr>
 * <td>-8000</td>
 * <td>Deployment packages related errors. Besides the values defined in
 * {@link IAgentErrors} class, additional codes are available from
 * DeploymentException class. You can convert IAgentErrors codes to
 * DeploymentException code using the provided method
 * {@link #toDeploymentExceptionCode(int)}</td>
 * </tr>
 * <tr>
 * <td>-9000</td>
 * <td>Reserved for internal use. Error codes from this range will not be
 * exposed to clients</td>
 * </tr>
 * </table>
 * 
 * @version 1.0
 */
public class IAgentErrors {

	public static final int GENERAL_ERROR = -1;

	public static final int UNSUPPORTED_OPERATION = -2;
	
	/**
	 * Code indicating that general error occured in VM Manager<br>
	 * 
	 * Note: Range from -3000 to -3999 is reserved for error codes related to VM
	 * Manager
	 */
	public static final int ERR_RUNTIME_UNKNOWN = -3000;

	/**
	 * Error code when RuntimeManager is not inited.
	 */
	public static final int ERR_RUNTIME_NOT_INITED = -3001;

	/**
	 * Error code when argument was not found if try to remove it.
	 */
	public static final int ERR_RUNTIME_ARG_NOT_FOUND = -3002;

	/**
	 * Error code when deleting fragment file.
	 */
	public static final int ERR_RUNTIME_FAILED_TO_DEL_FRGM = -3003;

	/**
	 * Error code when open fragment file for write.
	 */
	public static final int ERR_RUNTIME_FAILED_TO_OPEN_FRGM = -3004;

	/**
	 * Error code when failed to write to fragment file.
	 */
	public static final int ERR_RUNTIME_FAILED_TO_WRITE_FRGM = -3005;

	/**
	 * Invalid character in argument.
	 */
	public static final int ERR_RUNTIME_INVALID_CHAR_IN_ARG = -3006;

	/**
	 * Not allowed to add empty argument.
	 */
	public static final int ERR_RUNTIME_NOT_ALLOWED_EMPTY_ARG = -3007;

	/**
	 * Not allowed to add empty argument.
	 */
	public static final int ERR_RUNTIME_FAILED_START_DIAPROCESS = -3008;

	/**
	 * Dia server starting timeouted.
	 */
	public static final int ERR_RUNTIME_SERVERVM_START_TIMEOUTED = -3009;

	/**
	 * Dia server failed to start.
	 */
	public static final int ERR_RUNTIME_SERVERVM_START_ERROR = -3010;

	/**
	 * Dia exit.
	 */
	public static final int ERR_RUNTIME_DIA_EXIT = -3011;

	/**
	 * Required connection cannot be established and the operation has failed.
	 */
	public static final int ERROR_CANNOT_CONNECT = -5006;

	/**
	 * Device connector was disconnected from the remote OSGi framework and the
	 * connection cannot be reestablished.
	 */
	public static final int ERROR_DISCONNECTED = -5007;

	/**
	 * Code indicating that some unrecoverable internal error occurred
	 */
	public static final int ERROR_INTERNAL_ERROR = -5008;

	/**
	 * Instrumentation of the remote OSGi framework failed.
	 */
	public static final int ERROR_INSTRUMENT_ERROR = -5009;

	/**
	 * Code indicating that a required remote service is not available.
	 */
	public static final int ERROR_REMOTE_ADMIN_NOT_AVAILABLE = -5010;

	/**
	 * Error code indicating unknown bundle error.
	 */
	public static final int ERROR_BUNDLE_UNKNOWN = -6000;

	/**
	 * Error code indicating that bundle activator was in error.
	 */
	public static final int ERROR_BUNDLE_ACTIVATOR = -6001;

	/**
	 * Error code indicating that the install or update operation failed because
	 * another already installed bundle has the same symbolic name and version.
	 */
	public static final int ERROR_BUNDLE_DUPLICATE = -6002;

	/**
	 * Error code indicating that the operation was invalid.
	 */
	public static final int ERROR_BUNDLE_INVALID_OPERATION = -6003;

	/**
	 * Error code indicating that the bundle manifest was in error.
	 */
	public static final int ERROR_BUNDLE_MANIFEST = -6004;

	/**
	 * Error code indicating that the bundle could not be resolved due to an
	 * error with the Bundle-NativeCode header.
	 */
	public static final int ERROR_BUNDLE_NATIVECODE = -6005;

	/**
	 * Error code indicating that the bundle was not resolved.
	 */
	public static final int ERROR_BUNDLE_RESOLVE = -6006;

	/**
	 * Error code indicating that the operation failed due to insufficient
	 * permissions.
	 */
	public static final int ERROR_BUNDLE_SECURITY = -6007;

	/**
	 * Error code indicating that the start transient operation failed because
	 * the start level of the bundle is greater than the current framework start
	 * level
	 */
	public static final int ERROR_BUNDLE_START_TRANSIENT = -6008;

	/**
	 * Error code indicating that the operation failed to complete the requested
	 * lifecycle state change.
	 */
	public static final int ERROR_BUNDLE_STATECHANGE = -6009;

	/**
	 * Error code indicating that the operation was unsupported.
	 */
	public static final int ERROR_BUNDLE_UNSUPPORTED_OPERATION = -6010;

	/**
	 * Error code indicating that the requested operation was denied, because
	 * the target bundle is system and protected.
	 */
	public static final int ERROR_BUNDLE_SYSTEM = -6901;

	/**
	 * Error code indicating that the bundle has been uninstalled.
	 */
	public static final int ERROR_BUNDLE_UNINSTALLED = -6999;

	/**
	 * Code error indicating that application exception was thrown on the remote
	 * site.<br>
	 * 
	 */
	public static final int ERROR_APPLICATION_UNKNOWN = -7000;

	/**
	 * Code error indicating that no application found for the bundle/DP
	 */
	public static final int ERROR_APPLICATION_NO_APPLICATION_FOUND = -7901;

	/**
	 * Code error indicating that too many applications are defined in the
	 * bundle/DP
	 */
	public static final int ERROR_APPLICATION_TOO_MANY_APPLICATIONS = -7902;

	/**
	 * Code error indicating that the application has been uninstalled.
	 */
	public static final int ERROR_APPLICATION_UNINSTALLED = -7903;

	/**
	 * Code error indicating that deployment exception was thrown on the remote
	 * site.<br>
	 * 
	 */
	public static final int ERROR_DEPLOYMENT_UNKNOWN = -8000;

	/**
	 * Code error indicating that referenced deployment package has been
	 * uninstalled or updated.
	 */
	public static final int ERROR_DEPLOYMENT_STALE = -8999;

	/**
	 * Code error indicating that service related problem has occured, which
	 * cannot be described with the remaining codes.
	 */
	public static final int ERROR_SERVICE_UNKNOWN = -9000;

	/**
	 * Code error indicating that the service has been unregistered and it is no
	 * longer available.
	 */
	public static final int ERROR_SERVICE_UNREGISTERED = -9999;

	/**
	 * Converts error code found in IAgentException to the originating code
	 * found in DeploymentException. If this method returns 0, this means that
	 * the passed code isn't originated from DeploymentException.
	 * 
	 * @param iAgentCode
	 *            the error code from {@link IAgentException#getErrorCode()}
	 * @return the converted code or 0 if the code isn't originating from
	 *         DeploymentException
	 */
	public static int toDeploymentExceptionCode(int iAgentCode) {
		return ExceptionCodeHelper.toDeploymentExceptionCode(iAgentCode);
	}

	/**
	 * Converts error code found in {@link IAgentException} to the originating
	 * code found in ApplicationException. If this method returns 0, this means
	 * that the passed code isn't originating from ApplicationException.
	 * 
	 * @param iAgentCode
	 *            the error code from {@link IAgentException#getErrorCode()}
	 * @return the converted code or 0 if the code isn't originating from
	 *         ApplicationException
	 */
	public static int toApplicationExceptionCode(int iAgentCode) {
		return ExceptionCodeHelper.toApplicationExceptionCode(iAgentCode);
	}
}
