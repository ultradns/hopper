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
import biz.neustar.hopper.message.Flags;
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

	/** Mapping from question to response */
	final Map<QueryQuestion, File> responseMap = new HashMap<QueryQuestion, File>();

	private final int port;
	private final int backlog;
	private final InetAddress bindAddr;

	public XFRServer(int port, int backlog, InetAddress bindAddr) {
		this.port = port;
		this.backlog = backlog;
		this.bindAddr = bindAddr;
	}

	public XFRServer() throws UnknownHostException {
		this(53, 128, InetAddress.getByName("localhost"));
	}

	/**
	 * Commence responding to request
	 */
	public void serve() {

		try {
			ServerSocket sock;
			sock = new ServerSocket(port, backlog, bindAddr);
			while (true) {
				final Socket s = sock.accept();
				Thread t;
				t = new Thread(new Runnable() {
					public void run() {
						try {
							respondToRequest(s);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				t.start();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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
		response.getHeader().setFlag(Flags.QR);
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
	public List<Record> readRecords(Message query) throws IOException {

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

	public static void main(String[] args) throws UnknownHostException, TextParseException {

		XFRServer xfrServer = new XFRServer(1053, 128, InetAddress.getByName("localhost"));

		// stage an AXFR response
		QueryQuestion qq = new QueryQuestion(Record.newRecord(new Name("marty.biz."), Type.AXFR, DClass.IN), 0l);
		File response = getFileFromClasspath("marty.biz.axfr.2002029056");
		xfrServer.getResponseMap().put(qq, response);

		// stage an IXFR response
		qq = new QueryQuestion(Record.newRecord(new Name("marty.biz."), Type.IXFR, DClass.IN), 2002022423l);
		response = getFileFromClasspath("marty.biz.ixfr.2002022423.2002022424");
		xfrServer.getResponseMap().put(qq, response);

		xfrServer.serve();

	}

	public static File getFileFromClasspath(String filename) {

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

}
