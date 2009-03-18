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
 * <td>Bundle related errors. Currently all bundle exceptions has value {@value
 * #ERROR_BUNDLE_UNKNOWN}. More information about the exception can be found in
 * the exception message</td>
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
	 * Error code indicating that general error occurred in File Manager<br>
	 * 
	 * Note: Range from -4000 to -4999 is reserved for error codes related to
	 * File Manager
	 */
	public static final int ERR_FM_UNKNOWN = -4000;
	/**
	 * Invalid File name was specified.
	 */
	public static final int ERR_FM_INVALID_FILE_NAME = -4001;

	/**
	 * Memory Allocation Error in FileManager
	 */
	public static final int ERR_FM_FILE_MEMORY_ERROR = -4002;

	/**
	 * Failed to open a file.
	 */
	public static final int ERR_FM_OPEN_FILE = -4003;

	/**
	 * Attempt to perform I/O on a file that is not open.
	 */
	public static final int ERR_FM_FILE_IS_NOT_OPEN = -4004;

	/**
	 * Attempt to offset the file pointer at an invalid position.
	 */
	public static final int ERR_FM_INVALID_OFFSET = -4005;

	/**
	 * Attempt to use an invalid file handle.
	 */
	public static final int ERR_FM_BAD_FILEDESCRIPTOR = -4006;

	/**
	 * Attempt to delete a file failed.
	 */
	public static final int ERR_FM_FAILED_TO_DELETE_FILE = -4007;

	/**
	 * Attempt to use an invalid path.
	 */
	public static final int ERR_FM_INVALID_PATH = -4008;

	/**
	 * Attempt to use file name that is larger then the native MAX_PATH
	 * constant.
	 */
	public static final int ERR_FM_NAME_TOO_LONG = -4009;

	/**
	 * Unknown error while performing file operations.
	 */
	public static final int ERR_FM_UNKNOWN_FILE_ERROR = -4010;

	/**
	 * Attempt to create an existing path.
	 */
	public static final int ERR_FM_PATH_EXISTS = -4011;

	/**
	 * File reading operation failed.
	 */
	public static final int ERR_FAILED_READ = -4012;

	/**
	 * Unsupported/Untested Operation.
	 */
	public static final int ERR_UNSUPPORTED_FILE_OPERATION = -4013;

	/**
	 * Invalid data length.
	 */
	public static final int ERR_INVALID_DATA_LENGTH = -4014;

	/**
	 * The DeploymentAdmin service is not available on the remote site.
	 */
	public static final int ERROR_DEPLOYMENT_ADMIN_NOT_ACTIVE = -5001;

	/**
	 * The ApplicationAdmin service is not available on the remote site.
	 */
	public static final int ERROR_APPLICATION_ADMIN_NOT_ACTIVE = -5002;

	/**
	 * The ApplicationAdmin interfaces are not available on the remote site.
	 * Usually means that the remote OSGi framework doesn't support
	 * ApplicationAdmin specification
	 */
	public static final int ERROR_APPLICATION_ADMIN_UNAVAILABLE = -5003;

	/**
	 * RemoteBundleAdmin service is not active. This usually means that the
	 * Instrumentation Agent RPC part was deactivated for some reason.
	 */
	public static final int ERROR_REMOTE_BUNDLE_ADMIN_NOT_ACTIVE = -5004;

	/**
	 * RemoteServiceAdmin service is not active. This usually means that the
	 * Instrumentation Agent RPC part was deactivated for some reason, or wasn't
	 * available.
	 */
	public static final int ERROR_REMOTE_SERVICE_ADMIN_NOT_ACTIVE = -5005;

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
	 * Code error indicating that bundle exception was thrown on the remote
	 * site.<br>
	 * 
	 */
	public static final int ERROR_BUNDLE_UNKNOWN = -6000;

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
	 * Code error indicating that deployment exception was thrown on the remote
	 * site.<br>
	 * 
	 */
	public static final int ERROR_DEPLOYMENT_UNKNOWN = -8000;

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

	/**
	 * Code indicates that there are too many applications defined in the
	 * bundle/DP
	 * 
	 * @deprecated This constant is deprecated in favor of
	 *             {@link #ERROR_APPLICATION_TOO_MANY_APPLICATIONS}
	 */
	public static final int ERR_TOO_MANY_APPLICATIONS = ERROR_APPLICATION_TOO_MANY_APPLICATIONS;

	/**
	 * Code indicates that no applications are defined in the bundle/DP
	 * 
	 * @deprecated This constant is deprecated in favor of
	 *             {@link #ERROR_APPLICATION_NO_APPLICATION_FOUND}
	 */
	public static final int ERR_NO_APPLICATION_FOUND = ERROR_APPLICATION_NO_APPLICATION_FOUND;

	/**
	 * Code indicates that application launching failed.
	 * 
	 * @deprecated This constant is deprecated and is replaced by the constants
	 *             defined in ApplicationAdmin error code range.
	 */
	public static final int ERROR_LAUNCHING_APPLICATION = -4017;
}
