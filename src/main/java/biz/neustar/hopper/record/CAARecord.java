/**
 * Copyright 2000-2015 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.record;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.codec.binary.StringUtils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.util.BytePresentationFormat;
import biz.neustar.hopper.util.Tokenizer;

public class CAARecord extends Record {

    private static final long serialVersionUID = -4080571985476339191L;

    /**
     * The Flags field of the CAA record.
     */
    private int flags;

    /**
     * Tag values MAY contain US-ASCII characters 'a' through 'z', 'A'
     * through 'Z', and the numbers 0 through 9.
     */
    private String tag;

    /**
     * A sequence of octets representing the property value.
     * Property values are encoded as binary values and MAY employ sub-
     * formats.
     */
    private byte[] value;

    public CAARecord() {

    }

    public CAARecord(Name name, DClass dclass, long ttl, int flags, String tag,
            String value) throws TextParseException {
        super(name, Type.CAA, dclass, ttl);
        this.flags = flags;
        setTag(tag);
        setValue(value);
    }

    @Override
    protected Record getObject() {
        return new CAARecord();
    }

    @Override
    protected void rrFromWire(DNSInput in) throws IOException {
        flags = in.readU8();
        int tagLength = in.readU8();
        setTag(in.readByteArray(tagLength));
        setValue(in.readByteArray());
    }

    public void setTag(byte[] input) {
        setTag(new String(input, Charsets.US_ASCII));
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public void setTag(String input) {
        if (1 > input.length() || input.length() > 255) {
            throw new IllegalArgumentException(
                "Tag length should be non-zero and fitting unsigned 8.");
        }

        if (!CharMatcher.ASCII.matchesAllOf(input)) {
            throw new IllegalArgumentException(
                    "Tag should be a sequence of US-ASCII characters.");
        }
        this.tag = input.toLowerCase();
    }

    public void setValue(String value) throws TextParseException {
        String trimmedInput = value;
        if (value.startsWith("\"") && value.endsWith("\"")) {
            trimmedInput = CharMatcher.is('"').trimFrom(value);
        } else if (value.startsWith("\"") || value.endsWith("\"")) {
            throw new IllegalArgumentException(
                    "Value should be either a QuoteWithAscii or octetstream.");
        }
        this.value = BytePresentationFormat.toByteArray(trimmedInput);
    }

    public int getFlags() {
        return flags;
    }

    public String getTag() {
        return tag;
    }

    public String getValuePresentationFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        sb.append(BytePresentationFormat.toPresentationString(value));
        sb.append('"');
        return sb.toString();
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public String rrToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(flags);
        sb.append(' ');
        sb.append(tag);
        sb.append(' ');
        sb.append(getValuePresentationFormat());
        return sb.toString();
    }

    @Override
    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        flags = st.getUInt8();
        setTag(st.getString());
        setValue(st.getString());
    }

    @Override
    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeU8(flags);
        out.writeCountedString(tag.getBytes(Charsets.US_ASCII));
        out.writeByteArray(value);
    }

}
