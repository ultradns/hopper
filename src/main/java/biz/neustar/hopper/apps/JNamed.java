// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.apps;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import biz.neustar.hopper.exception.NameTooLongException;
import biz.neustar.hopper.exception.ZoneTransferException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.ExtendedFlag;
import biz.neustar.hopper.message.Flag;
import biz.neustar.hopper.message.Header;
import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Opcode;
import biz.neustar.hopper.message.Rcode;
import biz.neustar.hopper.message.Section;
import biz.neustar.hopper.message.TSIG;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.message.Zone;
import biz.neustar.hopper.record.CNAMERecord;
import biz.neustar.hopper.record.DNAMERecord;
import biz.neustar.hopper.record.OPTRecord;
import biz.neustar.hopper.record.RRSet;
import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.record.TSIGRecord;
import biz.neustar.hopper.record.impl.Address;
import biz.neustar.hopper.resolver.Cache;
import biz.neustar.hopper.resolver.Credibility;
import biz.neustar.hopper.resolver.SetResponse;

/** @author Brian Wellington &lt;bwelling@xbill.org&gt; */

public class JNamed {

	static final int FLAG_DNSSECOK = 1;
	static final int FLAG_SIGONLY = 2;

	Map<Integer, Cache> caches;
	Map<Name, Zone> znames;
	Map<Name, TSIG> TSIGs;

	private static String addrport(InetAddress addr, int port) {
		return addr.getHostAddress() + "#" + port;
	}

	public JNamed(String conffile) throws IOException, ZoneTransferException {
		FileInputStream fs;
		InputStreamReader isr;
		BufferedReader br;
		List<Integer> ports = new ArrayList<Integer>();
		List<InetAddress> addresses = new ArrayList<InetAddress>();
		try {
			fs = new FileInputStream(conffile);
			isr = new InputStreamReader(fs, StandardCharsets.UTF_8);
			br = new BufferedReader(isr);
		} catch (Exception e) {
			System.out.println("Cannot open " + conffile);
			return;
		}

		try {
			caches = new HashMap<Integer, Cache>();
			znames = new HashMap<Name, Zone>();
			TSIGs = new HashMap<Name, TSIG>();

			String line = null;
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);
				if (!st.hasMoreTokens())
					continue;
				String keyword = st.nextToken();
				if (!st.hasMoreTokens()) {
					System.out.println("Invalid line: " + line);
					continue;
				}
				if (keyword.charAt(0) == '#')
					continue;
				if (keyword.equals("primary"))
					addPrimaryZone(st.nextToken(), st.nextToken());
				else if (keyword.equals("secondary"))
					addSecondaryZone(st.nextToken(), st.nextToken());
				else if (keyword.equals("cache")) {
					Cache cache = new Cache(st.nextToken());
					caches.put(DClass.IN.getValue(), cache);
				} else if (keyword.equals("key")) {
					String s1 = st.nextToken();
					String s2 = st.nextToken();
					if (st.hasMoreTokens())
						addTSIG(s1, s2, st.nextToken());
					else
						addTSIG("hmac-md5", s1, s2);
				} else if (keyword.equals("port")) {
					ports.add(Integer.valueOf(st.nextToken()));
				} else if (keyword.equals("address")) {
					String addr = st.nextToken();
					addresses.add(Address.getByAddress(addr));
				} else {
					System.out.println("unknown keyword: " + keyword);
				}

			}

			if (ports.size() == 0)
				ports.add(new Integer(53));

			if (addresses.size() == 0)
				addresses.add(Address.getByAddress("0.0.0.0"));

			Iterator<InetAddress> iaddr = addresses.iterator();
			while (iaddr.hasNext()) {
				InetAddress addr = iaddr.next();
				Iterator<Integer> iport = ports.iterator();
				while (iport.hasNext()) {
					int port = iport.next().intValue();
					addUDP(addr, port);
					addTCP(addr, port);
					System.out.println("jnamed: listening on " + addrport(addr, port));
				}
			}
			System.out.println("jnamed: running");
		} finally {
			fs.close();
		}
	}

	public void addPrimaryZone(String zname, String zonefile) throws IOException {
		Name origin = null;
		if (zname != null)
			origin = Name.fromString(zname, Name.root);
		Zone newzone = new Zone(origin, zonefile);
		znames.put(newzone.getOrigin(), newzone);
	}

	public void addSecondaryZone(String zone, String remote) throws IOException, ZoneTransferException {
		Name zname = Name.fromString(zone, Name.root);
		Zone newzone = new Zone(zname, DClass.IN, remote);
		znames.put(zname, newzone);
	}

	public void addTSIG(String algstr, String namestr, String key) throws IOException {
		Name name = Name.fromString(namestr, Name.root);
		TSIGs.put(name, new TSIG(algstr, namestr, key));
	}

	public Cache getCache(DClass in) {
		Cache c = caches.get(in.getValue());
		if (c == null) {
			c = new Cache(in);
			caches.put(in.getValue(), c);
		}
		return c;
	}

	public Zone findBestZone(Name name) {
		Zone foundzone = null;
		foundzone = znames.get(name);
		if (foundzone != null)
			return foundzone;
		int labels = name.labels();
		for (int i = 1; i < labels; i++) {
			Name tname = new Name(name, i);
			foundzone = znames.get(tname);
			if (foundzone != null)
				return foundzone;
		}
		return null;
	}

	public RRSet findExactMatch(Name name, int type, DClass in, boolean glue) {
		Zone zone = findBestZone(name);
		if (zone != null)
			return zone.findExactMatch(name, type);
		else {
			RRSet[] rrsets;
			Cache cache = getCache(in);
			if (glue)
				rrsets = cache.findAnyRecords(name, type);
			else
				rrsets = cache.findRecords(name, type);
			if (rrsets == null)
				return null;
			else
				return rrsets[0]; /* not quite right */
		}
	}

	void addRRset(Name name, Message response, RRSet rrset, int section, int flags) {
		for (int s = 1; s <= section; s++)
			if (response.findRRset(name, rrset.getType(), s))
				return;
		if ((flags & FLAG_SIGONLY) == 0) {
			Iterator it = rrset.rrs();
			while (it.hasNext()) {
				Record r = (Record) it.next();
				if (r.getName().isWild() && !name.isWild())
					r = r.withName(name);
				response.addRecord(r, section);
			}
		}
		if ((flags & (FLAG_SIGONLY | FLAG_DNSSECOK)) != 0) {
			Iterator it = rrset.sigs();
			while (it.hasNext()) {
				Record r = (Record) it.next();
				if (r.getName().isWild() && !name.isWild())
					r = r.withName(name);
				response.addRecord(r, section);
			}
		}
	}

	private final void addSOA(Message response, Zone zone) {
		response.addRecord(zone.getSOA(), Section.AUTHORITY);
	}

	private final void addNS(Message response, Zone zone, int flags) {
		RRSet nsRecords = zone.getNS();
		addRRset(nsRecords.getName(), response, nsRecords, Section.AUTHORITY, flags);
	}

	private final void addCacheNS(Message response, Cache cache, Name name) {
		SetResponse sr = cache.lookupRecords(name, Type.NS, Credibility.HINT);
		if (!sr.isDelegation())
			return;
		RRSet nsRecords = sr.getNS();
		Iterator it = nsRecords.rrs();
		while (it.hasNext()) {
			Record r = (Record) it.next();
			response.addRecord(r, Section.AUTHORITY);
		}
	}

	private void addGlue(Message response, Name name, int flags) {
		RRSet a = findExactMatch(name, Type.A, DClass.IN, true);
		if (a == null)
			return;
		addRRset(name, response, a, Section.ADDITIONAL, flags);
	}

	private void addAdditional2(Message response, int section, int flags) {
		Record[] records = response.getSectionArray(section);
		for (int i = 0; i < records.length; i++) {
			Record r = records[i];
			Name glueName = r.getAdditionalName();
			if (glueName != null)
				addGlue(response, glueName, flags);
		}
	}

	private final void addAdditional(Message response, int flags) {
		addAdditional2(response, Section.ANSWER, flags);
		addAdditional2(response, Section.AUTHORITY, flags);
	}

	byte addAnswer(Message response, Name name, int type, DClass dclass, int iterations, int flags) {
		SetResponse sr;
		byte rcode = Rcode.NOERROR;

		if (iterations > 6)
			return Rcode.NOERROR;

		if (type == Type.SIG || type == Type.RRSIG) {
			type = Type.ANY;
			flags |= FLAG_SIGONLY;
		}

		Zone zone = findBestZone(name);
		if (zone != null)
			sr = zone.findRecords(name, type);
		else {
			Cache cache = getCache(dclass);
			sr = cache.lookupRecords(name, type, Credibility.NORMAL);
		}

		if (sr.isUnknown()) {
			addCacheNS(response, getCache(dclass), name);
		}
		if (sr.isNXDOMAIN()) {
			response.getHeader().setRcode(Rcode.NXDOMAIN);
			if (zone != null) {
				addSOA(response, zone);
				if (iterations == 0)
					response.getHeader().setFlag(Flag.AA);
			}
			rcode = Rcode.NXDOMAIN;
		} else if (sr.isNXRRSET()) {
			if (zone != null) {
				addSOA(response, zone);
				if (iterations == 0)
					response.getHeader().setFlag(Flag.AA);
			}
		} else if (sr.isDelegation()) {
			RRSet nsRecords = sr.getNS();
			addRRset(nsRecords.getName(), response, nsRecords, Section.AUTHORITY, flags);
		} else if (sr.isCNAME()) {
			CNAMERecord cname = sr.getCNAME();
			RRSet rrset = new RRSet(cname);
			addRRset(name, response, rrset, Section.ANSWER, flags);
			if (zone != null && iterations == 0)
				response.getHeader().setFlag(Flag.AA);
			rcode = addAnswer(response, cname.getTarget(), type, dclass, iterations + 1, flags);
		} else if (sr.isDNAME()) {
			DNAMERecord dname = sr.getDNAME();
			RRSet rrset = new RRSet(dname);
			addRRset(name, response, rrset, Section.ANSWER, flags);
			Name newname;
			try {
				newname = name.fromDNAME(dname);
			} catch (NameTooLongException e) {
				return Rcode.YXDOMAIN;
			}
			rrset = new RRSet(new CNAMERecord(name, dclass, 0, newname));
			addRRset(name, response, rrset, Section.ANSWER, flags);
			if (zone != null && iterations == 0)
				response.getHeader().setFlag(Flag.AA);
			rcode = addAnswer(response, newname, type, dclass, iterations + 1, flags);
		} else if (sr.isSuccessful()) {
			RRSet[] rrsets = sr.answers();
			for (int i = 0; i < rrsets.length; i++)
				addRRset(name, response, rrsets[i], Section.ANSWER, flags);
			if (zone != null) {
				addNS(response, zone, flags);
				if (iterations == 0)
					response.getHeader().setFlag(Flag.AA);
			} else
				addCacheNS(response, getCache(dclass), name);
		}
		return rcode;
	}

	byte[] doAXFR(Name name, Message query, TSIG tsig, TSIGRecord qtsig, Socket s) {
		Zone zone = znames.get(name);
		boolean first = true;
		if (zone == null)
			return errorMessage(query, Rcode.REFUSED);
		Iterator it = zone.AXFR();
		try {
			DataOutputStream dataOut;
			dataOut = new DataOutputStream(s.getOutputStream());
			int id = query.getHeader().getID();
			while (it.hasNext()) {
				RRSet rrset = (RRSet) it.next();
				Message response = new Message(id);
				Header header = response.getHeader();
				header.setFlag(Flag.QR);
				header.setFlag(Flag.AA);
				addRRset(rrset.getName(), response, rrset, Section.ANSWER, FLAG_DNSSECOK);
				if (tsig != null) {
					tsig.applyStream(response, qtsig, first);
					qtsig = response.getTSIG();
				}
				first = false;
				byte[] out = response.toWire();
				dataOut.writeShort(out.length);
				dataOut.write(out);
			}
		} catch (IOException ex) {
			System.out.println("AXFR failed");
		}
		try {
			s.close();
		} catch (IOException ex) {
		}
		return null;
	}

	/*
	 * Note: a null return value means that the caller doesn't need to do
	 * anything. Currently this only happens if this is an AXFR request over
	 * TCP.
	 */
	byte[] generateReply(Message query, byte[] in, int length, Socket s) throws IOException {
		Header header;
		int maxLength;
		int flags = 0;

		header = query.getHeader();
		if (header.isFlagSet(Flag.QR))
			return null;
		if (header.getRcode() != Rcode.NOERROR)
			return errorMessage(query, Rcode.FORMERR);
		if (header.getOpcode() != Opcode.QUERY)
			return errorMessage(query, Rcode.NOTIMP);

		Record queryRecord = query.getQuestion();

		TSIGRecord queryTSIG = query.getTSIG();
		TSIG tsig = null;
		if (queryTSIG != null) {
			tsig = TSIGs.get(queryTSIG.getName());
			if (tsig == null || tsig.verify(query, in, length, null) != Rcode.NOERROR)
				return formerrMessage(in);
		}

		OPTRecord queryOPT = query.getOPT();
		if (queryOPT != null && queryOPT.getVersion() > 0) {
		    // bridge to nowhere? TODO: why is this here?
		}

		if (s != null) {
			maxLength = 65535;
		} else if (queryOPT != null) {
			maxLength = Math.max(queryOPT.getPayloadSize(), 512);
		} else {
			maxLength = 512;
		}

		if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlag.DO.getValue()) != 0)
			flags = FLAG_DNSSECOK;

		Message response = new Message(query.getHeader().getID());
		response.getHeader().setFlag(Flag.QR);
		if (query.getHeader().isFlagSet(Flag.RD))
			response.getHeader().setFlag(Flag.RD);
		response.addRecord(queryRecord, Section.QUESTION);

		Name name = queryRecord.getName();
		int type = queryRecord.getType();
		DClass dclass = queryRecord.getDClass();
		if (type == Type.AXFR && s != null)
			return doAXFR(name, query, tsig, queryTSIG, s);
		if (!Type.isRR(type) && type != Type.ANY)
			return errorMessage(query, Rcode.NOTIMP);

		byte rcode = addAnswer(response, name, type, dclass, 0, flags);
		if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN)
			return errorMessage(query, rcode);

		addAdditional(response, flags);

		if (queryOPT != null) {
			//int optflags = (flags == FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
		    // TODO: optrecord to use extendedflags
		    int optflags = (flags == FLAG_DNSSECOK) ? ExtendedFlag.DO.getValue() : 0;
			OPTRecord opt = new OPTRecord((short) 4096, rcode, (byte) 0, optflags);
			response.addRecord(opt, Section.ADDITIONAL);
		}

		response.setTSIG(tsig, Rcode.NOERROR, queryTSIG);
		return response.toWire(maxLength);
	}

	byte[] buildErrorMessage(Header header, int rcode, Record question) {
		Message response = new Message();
		response.setHeader(header);
		for (int i = 0; i < 4; i++)
			response.removeAllRecords(i);
		if (rcode == Rcode.SERVFAIL)
			response.addRecord(question, Section.QUESTION);
		header.setRcode(rcode);
		return response.toWire();
	}

	public byte[] formerrMessage(byte[] in) {
		Header header;
		try {
			header = new Header(in);
		} catch (IOException e) {
			return null;
		}
		return buildErrorMessage(header, Rcode.FORMERR, null);
	}

	public byte[] errorMessage(Message query, int rcode) {
		return buildErrorMessage(query.getHeader(), rcode, query.getQuestion());
	}

	public void TCPclient(Socket s) {
		try {
			int inLength;
			DataInputStream dataIn;
			DataOutputStream dataOut;
			byte[] in;

			InputStream is = s.getInputStream();
			dataIn = new DataInputStream(is);
			inLength = dataIn.readUnsignedShort();
			in = new byte[inLength];
			dataIn.readFully(in);

			Message query;
			byte[] response = null;
			try {
				query = new Message(in);
				response = generateReply(query, in, in.length, s);
				if (response == null)
					return;
			} catch (IOException e) {
				response = formerrMessage(in);
			}
			dataOut = new DataOutputStream(s.getOutputStream());
			dataOut.writeShort(response.length);
			dataOut.write(response);
		} catch (IOException e) {
			System.out.println("TCPclient(" + addrport(s.getLocalAddress(), s.getLocalPort()) + "): " + e);
		} finally {
			try {
				s.close();
			} catch (IOException e) {
			}
		}
	}

	public void serveTCP(InetAddress addr, int port) {
		try {
			ServerSocket sock = new ServerSocket(port, 128, addr);
			while (true) {
				final Socket s = sock.accept();
				Thread t;
				t = new Thread(new Runnable() {
					public void run() {
						TCPclient(s);
					}
				});
				t.start();
			}
		} catch (IOException e) {
			System.out.println("serveTCP(" + addrport(addr, port) + "): " + e);
		}
	}

	public void serveUDP(InetAddress addr, int port) {
		try {
			DatagramSocket sock = new DatagramSocket(port, addr);
			final short udpLength = 512;
			byte[] in = new byte[udpLength];
			DatagramPacket indp = new DatagramPacket(in, in.length);
			DatagramPacket outdp = null;
			while (true) {
				indp.setLength(in.length);
				try {
					sock.receive(indp);
				} catch (InterruptedIOException e) {
					continue;
				}
				Message query;
				byte[] response = null;
				try {
					query = new Message(in);
					response = generateReply(query, in, indp.getLength(), null);
					if (response == null)
						continue;
				} catch (IOException e) {
					response = formerrMessage(in);
				}
				if (outdp == null)
					outdp = new DatagramPacket(response, response.length, indp.getAddress(), indp.getPort());
				else {
					outdp.setData(response);
					outdp.setLength(response.length);
					outdp.setAddress(indp.getAddress());
					outdp.setPort(indp.getPort());
				}
				sock.send(outdp);
			}
		} catch (IOException e) {
			System.out.println("serveUDP(" + addrport(addr, port) + "): " + e);
		}
	}

	public void addTCP(final InetAddress addr, final int port) {
		Thread t;
		t = new Thread(new Runnable() {
			public void run() {
				serveTCP(addr, port);
			}
		});
		t.start();
	}

	public void addUDP(final InetAddress addr, final int port) {
		Thread t;
		t = new Thread(new Runnable() {
			public void run() {
				serveUDP(addr, port);
			}
		});
		t.start();
	}

	public static void main(String[] args) {
		if (args.length > 1) {
			System.out.println("usage: jnamed [conf]");
			System.exit(0);
		}
		try {
			String conf;
			if (args.length == 1)
				conf = args[0];
			else
				conf = "jnamed.conf";
			new JNamed(conf);
		} catch (IOException e) {
			System.out.println(e);
		} catch (ZoneTransferException e) {
			System.out.println(e);
		}
	}

}
