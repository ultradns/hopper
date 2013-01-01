package biz.neustar.hopper.mock;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Flag;
import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Rcode;
import biz.neustar.hopper.message.Section;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.record.SOARecord;
import biz.neustar.hopper.util.Master;

/**
 * A XFR server for testing which returns a fixed response for a query
 * 
 * Keeps a lookup from query to file and returns the file contents for the
 * matched query. The file contains a text formatted AXFR or IXFR response (ex.
 * dig output piped to a file).
 * 
 * TCP only.
 * 
 * @author mkube
 * 
 */
public class XFRServer {

	/** Mapping from client query to response */
	final Map<QueryQuestion, File> responseMap = new HashMap<QueryQuestion, File>();
	private final int port;
	private final int backlog;
	private final InetAddress bindAddr;
	private ServerSocket serverSocket;

	/**
	 * Start a server on a specified port and interface
	 * 
	 * @param port
	 *            To listen on
	 * @param backlog
	 *            The client request backlog size
	 * @param bindAddr
	 *            The interface to bind to
	 */
	public XFRServer(int port, int backlog, InetAddress bindAddr) {
		this.port = port;
		this.backlog = backlog;
		this.bindAddr = bindAddr;
	}

	/**
	 * Start a local server the default port
	 * 
	 * @throws UnknownHostException
	 */
	public XFRServer() throws UnknownHostException {
		this(53, 128, InetAddress.getByName("localhost"));
	}

	/**
	 * Commence responding to request. Starts a ServerSocket if needed,
	 * otherwise does nothing.
	 * 
	 * @throws IOException
	 */
	protected synchronized void serve() throws IOException {

		if (serverSocket == null) {
			serverSocket = new ServerSocket(port, backlog, bindAddr);
			// Start a single accept thread
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (!serverSocket.isClosed()) {
						try {
							Socket accepted = serverSocket.accept();
							handleClientRequest(accepted);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}).start();
		}
	}

	/** fork a thread to handle a each client request */
	protected void handleClientRequest(final Socket s) {

		new Thread(new Runnable() {
			public void run() {
				try {
					respondToRequest(s);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * Read a single request and reply
	 * 
	 * @param s
	 *            The socket from which the request is coming from
	 * @throws IOException
	 */
	protected void respondToRequest(Socket s) throws IOException {

		// read question
		DataInputStream inputStream = new DataInputStream(s.getInputStream());
		int length = inputStream.readUnsignedShort();
		byte[] request = new byte[length];
		inputStream.readFully(request);

		// formulate answer
		Message query = new Message(request);
		byte[] response;
		try {
			Message responseMessage = generateAnswer(query);
			response = responseMessage.toWire();
		} catch (Exception e) {
			try {
				Message responseMessage = errorAnswer(query);
				response = responseMessage.toWire();
			} catch (Exception ee) {
				return;
			}
		}

		// send the answer
		DataOutputStream outputStream = new DataOutputStream(s.getOutputStream());
		outputStream.writeShort(response.length);
		outputStream.write(response);
	}

	/**
	 * Generate a server failure error message
	 * 
	 * @param query
	 *            For which a failure occured
	 * @return An error message
	 */
	protected Message errorAnswer(Message query) {

		Message response = getStubMessage(query);
		response.getHeader().setRcode(Rcode.SERVFAIL);
		for (int i = 0; i < 4; i++) {
			response.removeAllRecords(i);
		}
		return response;
	}

	/**
	 * Get a partial response message for a query.
	 * 
	 * @param query
	 *            The original query
	 * @return A message with the same header and question section as the passed
	 *         in query
	 */
	protected Message getStubMessage(Message query) {

		Message response = new Message();
		response.setHeader(query.getHeader());
		response.getHeader().setFlag(Flag.QR);
		for (int i = 0; i < 4; i++) {
			response.removeAllRecords(i);
		}
		response.addRecord(query.getQuestion(), Section.QUESTION);
		return response;
	}

	/**
	 * Lookup a response and return the corresponding message
	 * 
	 * @param query
	 *            For which response is looked up
	 * @return A answer message
	 * @throws IOException
	 *             If things go poorly
	 */
	protected Message generateAnswer(Message query) throws IOException {

		Message response = getStubMessage(query);
		response.getHeader().setRcode(Rcode.NOERROR);
		List<Record> answerRecords = readRecords(query);
		for (Record record : answerRecords) {
			response.addRecord(record, Section.ANSWER);
		}
		return response;
	}

	/**
	 * Lookup the file to read based on the query. Parse the file and return a
	 * list of records
	 * 
	 * @param query
	 *            From the client
	 * @return A list of records
	 * @throws IOException
	 *             If things go poorly
	 * @throws RuntimeException
	 *             If things go poorly
	 */
	protected List<Record> readRecords(Message query) throws IOException {

		Record question = query.getQuestion();
		long serial = 0;
		if (question.getType() == Type.IXFR) {
			Record[] sectionArray = query.getSectionArray(Section.AUTHORITY);
			SOARecord record = (SOARecord) sectionArray[0];
			serial = record.getSerial();
		}
		QueryQuestion key = new QueryQuestion(question, serial);
		File toRead = responseMap.get(key);
		Master master = new Master(new FileInputStream(toRead));
		List<Record> records = new LinkedList<Record>();
		Record record;
		while ((record = master.nextRecord()) != null) {
			records.add(record);
		}
		return records;
	}

	protected File getFileFromClasspath(String filename) {

		URL resource = XFRServer.class.getResource("/" + filename);
		File file = new File(resource.getFile());
		if (!file.exists() && file.canRead()) {
			throw new RuntimeException("cannot find " + filename);
		}
		return file;
	}

	public Map<QueryQuestion, File> getResponseMap() {
		return responseMap;
	}

	public int getPort() {
		return port;
	}

	public int getBacklog() {
		return backlog;
	}

	public InetAddress getBindAddr() {
		return bindAddr;
	}

	/**
	 * Add an AXFR response
	 * 
	 * @param name
	 *            The query name
	 * @param classpathFilename
	 *            Name of the repsonse file relative to the classpath
	 */
	public void addAxfrResponse(String name, String classpathFilename) {

		QueryQuestion qq;
		try {
			qq = new QueryQuestion(Record.newRecord(new Name(name), Type.AXFR, DClass.IN), 0l);
		} catch (TextParseException e) {
			throw new RuntimeException(e);
		}
		File response = getFileFromClasspath(classpathFilename);
		getResponseMap().put(qq, response);
	}

	/**
	 * Add an AXFR response
	 * 
	 * @param name
	 *            The query name
	 * @param classpathFilename
	 *            Name of the repsonse file relative to the classpath
	 * @param serial
	 *            The SOA serial number in the IXFR query
	 */
	public void addIxfrResponse(String name, long serial, String classpathFilename) {

		QueryQuestion qq;
		try {
			qq = new QueryQuestion(Record.newRecord(new Name(name), Type.IXFR, DClass.IN), serial);
		} catch (TextParseException e) {
			throw new RuntimeException(e);
		}
		File response = getFileFromClasspath(classpathFilename);
		getResponseMap().put(qq, response);
	}

	/**
	 * Start accepting client request.
	 * 
	 * Starts a server socket on another thread and return immediately. The
	 * server socket starts is one has not already been started, otherwise
	 * nothing is accomplished.
	 * 
	 * @throws RuntimeException
	 */
	public void start() {
		try {
			serve();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Stop accepting client request.
	 * 
	 * @throws RuntimeException
	 */
	public void stop() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws UnknownHostException, TextParseException {

		XFRServer xfrServer = new XFRServer(1053, 128, InetAddress.getByName("localhost"));

		// stage some responses
		xfrServer.addAxfrResponse("marty.biz.", "marty.biz.axfr.2002029056");
		xfrServer.addIxfrResponse("marty.biz.", 2002022423L, "marty.biz.ixfr.2002022423.2002022424.txt");

		xfrServer.start();
	}

}
