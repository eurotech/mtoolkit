package org.tigris.mtoolkit.iagent.internal.rpc.console;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.iagent.internal.pmp.InvocationThread;
import org.tigris.mtoolkit.iagent.internal.utils.CircularBuffer;
import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteConsole;

import com.prosyst.util.parser.ParserService;

public class ProSystRemoteConsole extends RemoteConsoleServiceBase implements Remote {
	private ServiceTracker parserServiceTrack;
	private ServiceReference parserServiceReference;
	private ParserService parserInstance;
	private Object parserServiceLock = new Object();

	public Class[] remoteInterfaces() {
		return new Class[] { RemoteConsole.class };
	}

	public void register(BundleContext bundleContext) {
		// check we are on mBS
		ParserService.class.getName();

		parserServiceTrack = new ServiceTracker(bundleContext, ParserService.class.getName(), null) {

			public void removedService(ServiceReference reference, Object service) {
				super.removedService(reference, service);
				if (reference.equals(parserServiceReference)) {
					parserServiceReference = null;
					releaseParserService();
				}
			}

		};
		parserServiceTrack.open();

		super.register(bundleContext);
	}

	private void initParserInstance(WriteDispatcher writeDispatcher) {
		parserServiceReference = parserServiceTrack.getServiceReference();
		if (parserServiceReference != null) {
			ParserService rootParserService = (ParserService) parserServiceTrack.getService(parserServiceReference);
			if (rootParserService != null) {
				parserInstance = rootParserService.getInstance();
				parserInstance.setOutputStream(new PrintStream(new DispatcherOutput(writeDispatcher)));
			}
		}
	}

	private void releaseParserService() {
		synchronized (parserServiceLock) {
			if (parserInstance != null) {
				parserInstance.release();
				parserInstance = null;
			}
		}
	}

	protected WriteDispatcher createDispatcher(PMPConnection conn, CircularBuffer buffer, RemoteObject remoteObject)
			throws PMPException {
		return new ProSystWriteDispatcher(conn, buffer, remoteObject);
	}

	public void unregister() {
		super.unregister();
		parserServiceTrack.close();
	}

	public void executeCommand(String line) {
		PMPConnection conn = InvocationThread.getContext().getConnection();
		ProSystWriteDispatcher disp = (ProSystWriteDispatcher) getDispatcher(conn);
		ParserService parser = disp.getParser();
		if (parser != null) {
			parser.parseCommand(line);
		}
	}

	private class ProSystWriteDispatcher extends WriteDispatcher {

		public ProSystWriteDispatcher(PMPConnection conn, CircularBuffer buffer, RemoteObject object)
				throws PMPException {
			super(conn, buffer, object);
		}

		public void run() {
			super.run();
			releaseParserService();
		}

		public ParserService getParser() {
			synchronized (parserServiceLock) {
				if (parserInstance == null) {
					initParserInstance(this);
				}
				return parserInstance;
			}
		}

	}

	private class DispatcherOutput extends OutputStream {
		private WriteDispatcher dispatcher;
		private byte[] singleByte = new byte[1];

		DispatcherOutput(WriteDispatcher dispatcher) {
			this.dispatcher = dispatcher;
		}

		public void write(byte[] var0, int var1, int var2) throws IOException {
			dispatcher.buffer.write(var0, var1, var2);
			synchronized (dispatcher) {
				dispatcher.notifyAll();
			}
		}

		public void write(int arg0) throws IOException {
			singleByte[0] = (byte) (arg0 & 0xFF);
			write(singleByte, 0, 1);
		}
	}
}
