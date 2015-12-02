// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.util;

import org.apache.commons.codec.binary.Base32;

import com.google.common.base.Charsets;

/**
 * Routines for converting between Strings of base32-encoded data and arrays
 * of binary data.  This currently supports the base32 and base32hex alphabets
 * specified in RFC 4648, sections 6 and 7.
 *
 * @author Brian Wellington
 */

public class base32 {

    public static class Alphabet {
        private Alphabet() {}

        public static final String BASE32 =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567=";
        public static final String BASE32HEX =
            "0123456789ABCDEFGHIJKLMNOPQRSTUV=";
    };

    private String alphabet;
    private boolean padding, lowercase;

    /**
     * Creates an object that can be used to do base32 conversions.
     * @param alphabet Which alphabet should be used
     * @param padding Whether padding should be used
     * @param lowercase Whether lowercase characters should be used.
     * default parameters (The standard base32 alphabet, no padding, uppercase)
     */
    public
    base32(String alphabet, boolean padding, boolean lowercase) {
        this.alphabet = alphabet;
        this.padding = padding;
        this.lowercase = lowercase;
    }

    /**
     * Convert binary data to a base32-encoded String
     *
     * @param b An array containing binary data
     * @return A String containing the encoded data
     */
    public String
    toString(byte [] b) {
    	Base32 base32 = new Base32(true);
    	return base32.encodeAsString(b);
    }

    /**
     * Convert a base32-encoded String to binary data
     *
     * @param str A String containing the encoded data
     * @return An array containing the binary data, or null if the string is invalid
     */
    public byte[]
    fromString(String str) {
    	Base32 base32 = new Base32(true);
    	return base32.decode(str);   	
    }

}
