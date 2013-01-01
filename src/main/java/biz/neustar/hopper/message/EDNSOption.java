// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)
package biz.neustar.hopper.message;

import java.io.IOException;
import java.util.Arrays;

import biz.neustar.hopper.exception.WireParseException;
import biz.neustar.hopper.message.impl.TrackedType;
import biz.neustar.hopper.message.impl.TrackedTypeRegistrar;
import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.util.Mnemonic;

/**
 * DNS extension options, as described in RFC 2671. The rdata of an OPT record
 * is defined as a list of options; this represents a single option.
 * 
 * @author Brian Wellington
 * @author Ming Zhou &lt;mizhou@bnivideo.com&gt;, Beaumaris Networks
 */
public abstract class EDNSOption {

    public static class Code extends TrackedType {
        private static final TrackedTypeRegistrar REGISTRAR = 
                new TrackedTypeRegistrar(Code.class, "CODE")
                    .allowNumericName(true)
                    .maxValue(0xFFFF);

        /** Name Server Identifier, RFC 5001 */
        public final static Code NSID = REGISTRAR.add(new Code(3, "NSID"));

        /** Client Subnet, defined in draft-vandergaast-edns-client-subnet-00 */
        public final static Code CLIENT_SUBNET = REGISTRAR.add(new Code(20730, "CLIENT_SUBNET"));

        private static Mnemonic codes = new Mnemonic("EDNS Option Codes",
                Mnemonic.CASE_UPPER);
        
        public Code(int value, String name, String... altNames) {
            super(value, name, altNames);
        }

        /**
         * Converts an EDNS Option Code into its textual representation
         */
        public static Code valueOf(int code) {
            return REGISTRAR.getOrCreateType(code);
        }

        /**
         * Converts a textual representation of an EDNS Option Code into its
         * numeric value.
         * 
         * @param s
         *            The textual representation of the option code
         * @return The option code, or -1 on error.
         */
        public static Code value(String code) {
            return REGISTRAR.getOrCreateType(code);
        }
    }

    private final Code code;

    /**
     * 
     * Creates an option with the given option code and data.
     */
    public EDNSOption(Code code) {
        this.code = code;
        //this.code = Record.checkU16("code", code);
    }
    
    
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");
        //sb.append(Code.valueOf(code).getName());
        sb.append(code.getName());
        sb.append(": ");
        sb.append(optionToString());
        sb.append("}");

        return sb.toString();
    }

    /**
     * Returns the EDNS Option's code.
     * 
     * @return the option code
     */
    public Code getCode() {
        return code;
    }

    /**
     * Returns the EDNS Option's data, as a byte array.
     * 
     * @return the option data
     */
    byte[] getData() {
        DNSOutput out = new DNSOutput();
        optionToWire(out);
        return out.toByteArray();
    }

    /**
     * Converts the wire format of an EDNS Option (the option data only) into
     * the type-specific format.
     * 
     * @param in
     *            The input Stream.
     */
    abstract void optionFromWire(DNSInput in) throws IOException;

    /**
     * Converts the wire format of an EDNS Option (including code and length)
     * into the type-specific format.
     * 
     * @param out
     *            The input stream.
     */
    public static EDNSOption fromWire(DNSInput in) throws IOException {
        int code, length;

        code = in.readU16();
        length = in.readU16();
        if (in.remaining() < length) {
            throw new WireParseException("truncated option");
        }
        int save = in.saveActive();
        in.setActive(length);
        EDNSOption option = new GenericEDNSOption(Code.valueOf(code));
        
        if (Code.NSID.getValue() == code) {
            option = new NSIDOption();
        } else if (Code.CLIENT_SUBNET.getValue() == code) {
            option = new ClientSubnetOption();
        }
        
        option.optionFromWire(in);
        in.restoreActive(save);

        return option;
    }

    /**
     * Converts the wire format of an EDNS Option (including code and length)
     * into the type-specific format.
     * 
     * @return The option, in wire format.
     */
    public static EDNSOption fromWire(byte[] b) throws IOException {
        return fromWire(new DNSInput(b));
    }

    /**
     * Converts an EDNS Option (the type-specific option data only) into wire
     * format.
     * 
     * @param out
     *            The output stream.
     */
    abstract void optionToWire(DNSOutput out);

    /**
     * Converts an EDNS Option (including code and length) into wire format.
     * 
     * @param out
     *            The output stream.
     */
    public void toWire(DNSOutput out) {
        out.writeU16(code.getValue());
        int lengthPosition = out.current();
        out.writeU16(0); /* until we know better */
        optionToWire(out);
        int length = out.current() - lengthPosition - 2;
        out.writeU16At(length, lengthPosition);
    }

    /**
     * Converts an EDNS Option (including code and length) into wire format.
     * 
     * @return The option, in wire format.
     */
    public byte[] toWire() throws IOException {
        DNSOutput out = new DNSOutput();
        toWire(out);
        return out.toByteArray();
    }

    /**
     * Determines if two EDNS Options are identical.
     * 
     * @param arg
     *            The option to compare to
     * @return true if the options are equal, false otherwise.
     */
    public boolean equals(Object arg) {
        if (arg == null || !(arg instanceof EDNSOption)) {
            return false;
        }
        EDNSOption opt = (EDNSOption) arg;
        if (code != opt.code) {
            return false;
        }
        return Arrays.equals(getData(), opt.getData());
    }

    /**
     * Generates a hash code based on the EDNS Option's data.
     */
    public int hashCode() {
        byte[] array = getData();
        int hashval = 0;
        for (int i = 0; i < array.length; i++) {
            hashval += ((hashval << 3) + (array[i] & 0xFF));
        }
        return hashval;
    }

    abstract String optionToString();

}
