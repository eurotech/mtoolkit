package org.tigris.mtoolkit.iagent.mbsa;

public class MBSAException extends Exception {

	private static final long serialVersionUID = -5425027013167367669L;

	public static final int CODE_UNKNOWN_ERROR = 300;
	public static final int CODE_INITIAL_HANDSHAKE = 301;
	public static final int CODE_PROTOCOL_ERROR = 302;
	public static final int CODE_UNSUPPORTED_OPERATION = 303;
	public static final int CODE_INCOMPATIBLE_PEER = 304;
	public static final int CODE_OPERATION_TIMEOUT = 305;
	public static final int CODE_CANNOT_CONNECT = 306;

	private Throwable cause;

	private int code;

	public MBSAException(int code) {
		this(code, null, null);
	}

	public MBSAException(int code, String msg) {
		this(code, msg, null);
	}

	public MBSAException(int code, Throwable cause) {
		this(code, cause != null ? cause.toString() : null, cause);
	}

	public MBSAException(int code, String msg, Throwable cause) {
		super(msg);
		this.code = code;
		this.cause = cause;
	}

	public int getCode() {
		return code;
	}

	public synchronized Throwable getCause() {
		return cause;
	}

	public synchronized Throwable initCause(Throwable cause) {
		if (this.cause != null)
			throw new IllegalStateException("Cause for this exception has already been set");
		if (cause == this)
			throw new IllegalArgumentException("An exception cannot be a cause for itself");
		this.cause = cause;
		return this;
	}

	public String toString() {
		if (cause != null)
			return super.toString() + " (caused by " + cause.toString() + ")";
		return super.toString();
	}


}
