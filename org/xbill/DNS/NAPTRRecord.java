// Copyright (c) 2000 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.util.*;
import org.xbill.DNS.utils.*;

/**
 * Name Authority Pointer Record  - specifies rewrite rule, that when applied
 * to an existing string will produce a new domain.
 *
 * @author Chuck Santos
 */

public class NAPTRRecord extends Record {

private static NAPTRRecord member = new NAPTRRecord();

private short order, preference;
private String flags, service, regexp;
private Name replacement;

private NAPTRRecord() {}

private
NAPTRRecord(Name name, short dclass, int ttl) {
	super(name, Type.NAPTR, dclass, ttl);
}

static NAPTRRecord
getMember() {
	return member;
}

/**
 * Creates an NAPTR Record from the given data
 * @param order The order of this NAPTR.  Records with lower order are
 * preferred.
 * @param preference The preference, used to select between records at the
 * same order.
 * @param flags The control aspects of the NAPTRRecord.
 * @param service The service or protocol available down the rewrite path.
 * @param regexp The regular/substitution expression.
 * @param replacement The domain-name to query for the next DNS resource
 * record, depending on the value of the flags field.
 */
public
NAPTRRecord(Name _name, short _dclass, int _ttl, int _order, int _preference,
	    String _flags, String _service, String _regexp, Name _replacement)
{
	super(_name, Type.NAPTR, _dclass, _ttl);
	order = (short) _order;
	preference = (short) _preference;
	flags = _flags;
	service = _service;
	regexp = _regexp;
	replacement = _replacement;
	if (Options.check("verbose"))
		System.err.println(" NAPTR Set Member Constructor: " +
				   this.toString());
}

Record
rrFromWire(Name name, short type, short dclass, int ttl, int length,
	   DataByteInputStream in)
throws IOException
{
        NAPTRRecord rec = new NAPTRRecord(name, dclass, ttl);
	if (in == null)
		return rec;
	rec.order = (short) in.readUnsignedShort();
	rec.preference = (short) in.readUnsignedShort();
	rec.flags = in.readString();
	rec.service = in.readString();
	rec.regexp = in.readString();
	rec.replacement = new Name(in);
	return rec;
}

Record
rdataFromString(Name name, short dclass, int ttl, MyStringTokenizer st,
		Name origin)
throws TextParseException
{
	NAPTRRecord rec = new NAPTRRecord(name, dclass, ttl);
	rec.order = Short.parseShort(st.nextToken());
	rec.preference = Short.parseShort(st.nextToken());
	rec.flags = st.nextToken();
	rec.service = st.nextToken();
	rec.regexp = st.nextToken();
	rec.replacement = Name.fromString(st.nextToken(), origin);
	return rec;
}

/** Converts rdata to a String */
public String
rdataToString() {
	StringBuffer sb = new StringBuffer();
	if (replacement != null) {
		sb.append(order);
		sb.append(" ");
		sb.append(preference);
		sb.append(" ");
		sb.append(flags);
		sb.append(" ");
		sb.append(service);
		sb.append(" ");
		sb.append(regexp);
		sb.append(" ");
		sb.append(replacement);
	}
	if (Options.check("verbose"))
		System.err.println(" NAPTR toString(): : " + sb.toString());
	return sb.toString();
}

/** Returns the order */
public short
getOrder() {
	return order;
}

/** Returns the preference */
public short
getPreference() {
	return preference;
}

/** Returns flags */
public String
getFlags() {
	return flags;
}

/** Returns service */
public String
getService() {
	return service;
}

/** Returns regexp */
public String
getRegexp() {
	return regexp;
}

/** Returns the replacement domain-name */
public Name
getReplacement() {
	return replacement;
}

void rrToWire(DataByteOutputStream out, Compression c) throws IOException {
	if (replacement == null && regexp == null)
		return;
	out.writeShort(order);
	out.writeShort(preference);
	out.writeString(flags);
	out.writeString(service);
	out.writeString(regexp);
	replacement.toWire(out, null);
	if (Options.check("verbose"))
		System.err.println(" NAPTR rrToWire(): " + this.toString());
}

void rrToWireCanonical(DataByteOutputStream out) throws IOException {
	if (replacement == null && regexp == null)
		return;
	out.writeShort(order);
	out.writeShort(preference);
	out.writeString(flags);
	out.writeString(service);
	out.writeString(regexp);
	replacement.toWireCanonical(out);
	if (Options.check("verbose"))
		System.err.println(" NAPTR rrToWireCanonical(): " +
				   this.toString());
}

}
