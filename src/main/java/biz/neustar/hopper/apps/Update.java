// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Opcode;
import biz.neustar.hopper.message.Rcode;
import biz.neustar.hopper.message.Section;
import biz.neustar.hopper.message.TSIG;
import biz.neustar.hopper.message.TTL;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.record.SOARecord;
import biz.neustar.hopper.resolver.Resolver;
import biz.neustar.hopper.resolver.SimpleResolver;
import biz.neustar.hopper.util.Tokenizer;

/** @author Brian Wellington &lt;bwelling@xbill.org&gt; */

public class Update {

    private Message query, response;
	private Resolver res;
	private String server = null;
	private Name zone = Name.root;
	private long defaultTTL;
	private DClass defaultClass = DClass.IN;
	private Writer log = null;

	void print(Object o) {
		System.out.println(o);
		if (log != null) {
			try {
			log.write(o.toString());
			} catch (IOException e) {
				System.err.println("Error writing to log.");
				e.printStackTrace();
			}
		}
	}

	public Message newMessage() {
		Message msg = new Message();
		msg.getHeader().setOpcode(Opcode.UPDATE);
		return msg;
	}

	public Update(InputStream in) throws IOException {
		List<Reader> inputs = new LinkedList<Reader>();
		List<InputStream> istreams = new LinkedList<InputStream>();

		query = newMessage();

		InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
		BufferedReader br = new BufferedReader(isr);

		inputs.add(br);
		istreams.add(in);

		while (true) {
			try {
				String line = null;
				do {
					InputStream is;
					is = (InputStream) istreams.get(0);
					br = (BufferedReader) inputs.get(0);

					if (is == System.in)
						System.out.print("> ");

					line = br.readLine();
					if (line == null) {
						br.close();
						inputs.remove(0);
						istreams.remove(0);
						if (inputs.isEmpty())
							return;
					}
				} while (line == null);

				if (log != null)
					print("> " + line);

				if (line.length() == 0 || line.charAt(0) == '#')
					continue;

				/* Allows cut and paste from other update sessions */
				if (line.charAt(0) == '>')
					line = line.substring(1);

				Tokenizer st = new Tokenizer(line);
				Tokenizer.Token token = st.get();

				if (token.isEOL())
					continue;
				String operation = token.value;

				if (operation.equals("server")) {
					server = st.getString();
					res = new SimpleResolver(server);
					token = st.get();
					if (token.isString()) {
						String portstr = token.value;
						res.setPort(Short.parseShort(portstr));
					}
				}

				else if (operation.equals("key")) {
					String keyname = st.getString();
					String keydata = st.getString();
					if (res == null)
						res = new SimpleResolver(server);
					res.setTSIGKey(new TSIG(keyname, keydata));
				}

				else if (operation.equals("edns")) {
					if (res == null)
						res = new SimpleResolver(server);
					res.setEDNS(st.getUInt16());
				}

				else if (operation.equals("port")) {
					if (res == null)
						res = new SimpleResolver(server);
					res.setPort(st.getUInt16());
				}

				else if (operation.equals("tcp")) {
					if (res == null)
						res = new SimpleResolver(server);
					res.setTCP(true);
				}

				else if (operation.equals("class")) {
					String classStr = st.getString();
					try {
					    defaultClass = DClass.valueOf(classStr);
					} catch (RuntimeException ex) {
					    print("Error converting class: " + classStr);
					    throw ex; // rethrow
					}
				}

				else if (operation.equals("ttl"))
					defaultTTL = st.getTTL();

				else if (operation.equals("origin") || operation.equals("zone")) {
					zone = st.getName(Name.root);
				}

				else if (operation.equals("require"))
					doRequire(st);

				else if (operation.equals("prohibit"))
					doProhibit(st);

				else if (operation.equals("add"))
					doAdd(st);

				else if (operation.equals("delete"))
					doDelete(st);

				else if (operation.equals("glue"))
					doGlue(st);

				else if (operation.equals("help") || operation.equals("?")) {
					token = st.get();
					if (token.isString())
						help(token.value);
					else
						help(null);
				}

				else if (operation.equals("echo"))
					print(line.substring(4).trim());

				else if (operation.equals("send")) {
					sendUpdate();
					query = newMessage();
				}

				else if (operation.equals("show")) {
					print(query);
				}

				else if (operation.equals("clear"))
					query = newMessage();

				else if (operation.equals("query"))
					doQuery(st);

				else if (operation.equals("quit") || operation.equals("q")) {
					if (log != null)
						log.close();
					Iterator<Reader> it = inputs.iterator();
					while (it.hasNext()) {
						BufferedReader tbr;
						tbr = (BufferedReader) it.next();
						tbr.close();
					}
					System.exit(0);
				}

				else if (operation.equals("file"))
					doFile(st, inputs, istreams);

				else if (operation.equals("log"))
					doLog(st);

				else if (operation.equals("assert")) {
					if (doAssert(st) == false)
						return;
				}

				else if (operation.equals("sleep")) {
					long interval = st.getUInt32();
					try {
						Thread.sleep(interval);
					} catch (InterruptedException e) {
					}
				}

				else if (operation.equals("date")) {
					Date now = new Date();
					token = st.get();
					if (token.isString() && token.value.equals("-ms"))
						print(Long.toString(now.getTime()));
					else
						print(now);
				}

				else
					print("invalid keyword: " + operation);
			} catch (TextParseException tpe) {
				System.out.println(tpe.getMessage());
			} catch (InterruptedIOException iioe) {
				System.out.println("Operation timed out");
			} catch (SocketException se) {
				System.out.println("Socket error");
			} catch (IOException ioe) {
				System.out.println(ioe);
			}
		}
	}

	void sendUpdate() throws IOException {
		if (query.getHeader().getCount(Section.UPDATE) == 0) {
			print("Empty update message.  Ignoring.");
			return;
		}
		if (query.getHeader().getCount(Section.ZONE) == 0) {
			Name updzone;
			updzone = zone;
			DClass dclass = defaultClass;
			if (updzone == null) {
				Record[] recs = query.getSectionArray(Section.UPDATE);
				for (int i = 0; i < recs.length; i++) {
					if (updzone == null)
						updzone = new Name(recs[i].getName(), 1);
					if (recs[i].getDClass() != DClass.NONE && recs[i].getDClass() != DClass.ANY) {
						dclass = recs[i].getDClass();
						break;
					}
				}
			}
			Record soa = Record.newRecord(updzone, Type.SOA, dclass);
			query.addRecord(soa, Section.ZONE);
		}

		if (res == null)
			res = new SimpleResolver(server);
		response = res.send(query);
		print(response);
	}

	/*
	 * <name> [ttl] [class] <type> <data> Ignore the class, if present.
	 */
	Record parseRR(Tokenizer st, DClass classValue, long TTLValue) throws IOException {
		Name name = st.getName(zone);
		long ttl;
		int type;
		Record record;

		String s = st.getString();

		try {
			ttl = TTL.parseTTL(s);
			s = st.getString();
		} catch (NumberFormatException e) {
			ttl = TTLValue;
		}

		DClass classToUse = null;
		// so apparently we use the classValue rather than in the text..hmm
		if ((classToUse = DClass.valueOf(s)) != null) {
			s = st.getString();
		} else {
		    classToUse = classValue;
		}

		if ((type = Type.value(s)) < 0)
			throw new IOException("Invalid type: " + s);

		record = Record.fromString(name, type, classToUse, ttl, st, zone);
		if (record != null)
			return (record);
		else
			throw new IOException("Parse error");
	}

	void doRequire(Tokenizer st) throws IOException {
		Tokenizer.Token token;
		Name name;
		Record record;
		int type;

		name = st.getName(zone);
		token = st.get();
		if (token.isString()) {
			if ((type = Type.value(token.value)) < 0)
				throw new IOException("Invalid type: " + token.value);
			token = st.get();
			boolean iseol = token.isEOL();
			st.unget();
			if (!iseol) {
				record = Record.fromString(name, type, defaultClass, 0, st, zone);
			} else {
				record = Record.newRecord(name, type, DClass.ANY, 0);
			}
		} else {
			record = Record.newRecord(name, Type.ANY, DClass.ANY, 0);
		}

		query.addRecord(record, Section.PREREQ);
		print(record);
	}

	void doProhibit(Tokenizer st) throws IOException {
		Tokenizer.Token token;
		Name name;
		Record record;
		int type;

		name = st.getName(zone);
		token = st.get();
		if (token.isString()) {
			if ((type = Type.value(token.value)) < 0)
				throw new IOException("Invalid type: " + token.value);
		} else
			type = Type.ANY;
		record = Record.newRecord(name, type, DClass.NONE, 0);
		query.addRecord(record, Section.PREREQ);
		print(record);
	}

	void doAdd(Tokenizer st) throws IOException {
		Record record = parseRR(st, defaultClass, defaultTTL);
		query.addRecord(record, Section.UPDATE);
		print(record);
	}

	void doDelete(Tokenizer st) throws IOException {
		Tokenizer.Token token;
		String s;
		Name name;
		Record record;
		int type;

		name = st.getName(zone);
		token = st.get();
		if (token.isString()) {
			s = token.value;
			if (DClass.valueOf(s) != null) {
				s = st.getString();
			}
			if ((type = Type.value(s)) < 0)
				throw new IOException("Invalid type: " + s);
			token = st.get();
			boolean iseol = token.isEOL();
			st.unget();
			if (!iseol) {
				record = Record.fromString(name, type, DClass.NONE, 0, st, zone);
			} else
				record = Record.newRecord(name, type, DClass.ANY, 0);
		} else
			record = Record.newRecord(name, Type.ANY, DClass.ANY, 0);

		query.addRecord(record, Section.UPDATE);
		print(record);
	}

	void doGlue(Tokenizer st) throws IOException {
		Record record = parseRR(st, defaultClass, defaultTTL);
		query.addRecord(record, Section.ADDITIONAL);
		print(record);
	}

	void doQuery(Tokenizer st) throws IOException {
		Record rec;
		Tokenizer.Token token;

		Name name = null;
		int type = Type.A;
		DClass dclass = defaultClass;

		name = st.getName(zone);
		token = st.get();
		if (token.isString()) {
			type = Type.value(token.value);
			if (type < 0) {
				throw new IOException("Invalid type");
			}
			token = st.get();
			if (token.isString()) {
				dclass = DClass.valueOf(token.value);
				if (dclass != null) {
					throw new IOException("Invalid class");
				}
			}
		}

		rec = Record.newRecord(name, type, dclass);
		Message newQuery = Message.newQuery(rec);
		if (res == null) {
			res = new SimpleResolver(server);
		}
		response = res.send(newQuery);
		print(response);
	}

	void doFile(Tokenizer st, List<Reader> inputs, List<InputStream> istreams) throws IOException {
		String s = st.getString();
		InputStream is;
		try {
			if (s.equals("-")) {
				is = System.in;
			} else {
				is = new FileInputStream(s);
			}
			istreams.add(0, is);
			inputs.add(0, new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
		} catch (FileNotFoundException e) {
			print(s + " not found");
		}
	}

	void doLog(Tokenizer st) throws IOException {
		String s = st.getString();
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(s), StandardCharsets.UTF_8));
			log = writer;
		} catch (Exception e) {
			print("Error opening " + s);
		}
	}

	boolean doAssert(Tokenizer st) throws IOException {
		String field = st.getString();
		String expected = st.getString();
		String value = null;
		boolean flag = true;
		int section;

		if (response == null) {
			print("No response has been received");
			return true;
		}
		if (field.equalsIgnoreCase("rcode")) {
			int rcode = response.getHeader().getRcode();
			if (rcode != Rcode.value(expected)) {
				value = Rcode.string(rcode);
				flag = false;
			}
		} else if (field.equalsIgnoreCase("serial")) {
			Record[] answers = response.getSectionArray(Section.ANSWER);
			if (answers.length < 1 || !(answers[0] instanceof SOARecord))
				print("Invalid response (no SOA)");
			else {
				SOARecord soa = (SOARecord) answers[0];
				long serial = soa.getSerial();
				if (serial != Long.parseLong(expected)) {
					value = Long.toString(serial);
					flag = false;
				}
			}
		} else if (field.equalsIgnoreCase("tsig")) {
			if (response.isSigned()) {
				if (response.isVerified()) {
					value = "ok";
				} else {
					value = "failed";
				}
			} else
				value = "unsigned";
			if (!value.equalsIgnoreCase(expected))
				flag = false;
		} else if ((section = Section.value(field)) >= 0) {
			int count = response.getHeader().getCount(section);
			if (count != Integer.parseInt(expected)) {
				value = new Integer(count).toString();
				flag = false;
			}
		} else
			print("Invalid assertion keyword: " + field);

		if (flag == false) {
			print("Expected " + field + " " + expected + ", received " + value);
			while (true) {
				Tokenizer.Token token = st.get();
				if (!token.isString())
					break;
				print(token.value);
			}
			st.unget();
		}
		return flag;
	}

	static void help(String topic) {
		System.out.println();
		if (topic == null) {
			System.out.println("The following are supported commands:\n"
					+ "add      assert   class    clear    date     delete\n"
					+ "echo     edns     file     glue     help     key\n"
					+ "log      port     prohibit query    quit     require\n"
					+ "send     server   show     sleep    tcp      ttl\n" + "zone     #\n");
			return;
		}
		topic = topic.toLowerCase();

		if (topic.equals("add"))
			System.out.println("add <name> [ttl] [class] <type> <data>\n\n" + "specify a record to be added\n");
		else if (topic.equals("assert"))
			System.out.println("assert <field> <value> [msg]\n\n" + "asserts that the value of the field in the last\n"
					+ "response matches the value specified.  If not,\n"
					+ "the message is printed (if present) and the\n"
					+ "program exits.  The field may be any of <rcode>,\n"
					+ "<serial>, <tsig>, <qu>, <an>, <au>, or <ad>.\n");
		else if (topic.equals("class"))
			System.out.println("class <class>\n\n" + "class of the zone to be updated (default: IN)\n");
		else if (topic.equals("clear"))
			System.out.println("clear\n\n" + "clears the current update packet\n");
		else if (topic.equals("date"))
			System.out.println("date [-ms]\n\n" + "prints the current date and time in human readable\n"
					+ "format or as the number of milliseconds since the\n" + "epoch");
		else if (topic.equals("delete"))
			System.out.println("delete <name> [ttl] [class] <type> <data> \n" + "delete <name> <type> \n"
					+ "delete <name>\n\n" + "specify a record or set to be deleted, or that\n"
					+ "all records at a name should be deleted\n");
		else if (topic.equals("echo"))
			System.out.println("echo <text>\n\n" + "prints the text\n");
		else if (topic.equals("edns"))
			System.out.println("edns <level>\n\n" + "EDNS level specified when sending messages\n");
		else if (topic.equals("file"))
			System.out.println("file <file>\n\n" + "opens the specified file as the new input source\n"
					+ "(- represents stdin)\n");
		else if (topic.equals("glue"))
			System.out.println("glue <name> [ttl] [class] <type> <data>\n\n" + "specify an additional record\n");
		else if (topic.equals("help"))
			System.out.println("help\n" + "help [topic]\n\n" + "prints a list of commands or help about a specific\n"
					+ "command\n");
		else if (topic.equals("key"))
			System.out.println("key <name> <data>\n\n" + "TSIG key used to sign messages\n");
		else if (topic.equals("log"))
			System.out.println("log <file>\n\n" + "opens the specified file and uses it to log output\n");
		else if (topic.equals("port"))
			System.out.println("port <port>\n\n" + "UDP/TCP port messages are sent to (default: 53)\n");
		else if (topic.equals("prohibit"))
			System.out.println("prohibit <name> <type> \n" + "prohibit <name>\n\n"
					+ "require that a set or name is not present\n");
		else if (topic.equals("query"))
			System.out.println("query <name> [type [class]] \n\n" + "issues a query\n");
		else if (topic.equals("q") || topic.equals("quit"))
			System.out.println("quit\n\n" + "quits the program\n");
		else if (topic.equals("require"))
			System.out.println("require <name> [ttl] [class] <type> <data> \n" + "require <name> <type> \n"
					+ "require <name>\n\n" + "require that a record, set, or name is present\n");
		else if (topic.equals("send"))
			System.out.println("send\n\n" + "sends and resets the current update packet\n");
		else if (topic.equals("server"))
			System.out.println("server <name> [port]\n\n" + "server that receives send updates/queries\n");
		else if (topic.equals("show"))
			System.out.println("show\n\n" + "shows the current update packet\n");
		else if (topic.equals("sleep"))
			System.out.println("sleep <milliseconds>\n\n" + "pause for interval before next command\n");
		else if (topic.equals("tcp"))
			System.out.println("tcp\n\n" + "TCP should be used to send all messages\n");
		else if (topic.equals("ttl"))
			System.out.println("ttl <ttl>\n\n" + "default ttl of added records (default: 0)\n");
		else if (topic.equals("zone") || topic.equals("origin"))
			System.out.println("zone <zone>\n\n" + "zone to update (default: .\n");
		else if (topic.equals("#"))
			System.out.println("# <text>\n\n" + "a comment\n");
		else
			System.out.println("Topic '" + topic + "' unrecognized\n");
	}

	public static void main(String args[]) throws IOException {

		InputStream in = null;
		if (args.length >= 1) {
			try {
				in = new FileInputStream(args[0]);
			} catch (FileNotFoundException e) {
				System.out.println(args[0] + " not found.");
				System.exit(1);
			}
		} else
			in = System.in;
		new Update(in);
	}

}
