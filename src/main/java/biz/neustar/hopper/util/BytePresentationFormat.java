/**
 * Copyright 2000-2015 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.util;

import java.text.DecimalFormat;
import org.apache.commons.lang3.ArrayUtils;
import biz.neustar.hopper.exception.TextParseException;

/**
 * BPF converter from/to a octet array and presentation format (RFC 1035:
 * Section 5.1)
 * 
 * @author vpoliboy
 */
public class BytePresentationFormat {

    private static final DecimalFormat byteFormat = new DecimalFormat();
    private static final char[] specialCharacters = { '"', '\\' };

    static {
        byteFormat.setMinimumIntegerDigits(3);
    }

    static public String toPresentationString(byte[] octets) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < octets.length; i++) {
            int b = octets[i] & 0xFF;
            if (b < 0x20 || b >= 0x7f) {
                sb.append('\\');
                sb.append(byteFormat.format(b));
            } else if (ArrayUtils.contains(specialCharacters, (char) b)) {
                sb.append('\\');
                sb.append((char) b);
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    static public byte[] toByteArray(String input) throws TextParseException {
        int octetsLength = 0;
        byte[] octets = new byte[input.length()];
        int digits = 0;
        int intval = 0;

        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            byte b = (byte) input.charAt(i);
            if (escaped) {
                if (b >= '0' && b <= '9' && digits < 3) {
                    digits++;
                    intval *= 10;
                    intval += (b - '0');
                    if (intval > 255) {
                        throw new TextParseException("'" + input
                            + "': bad escape-int overflow.");
                    }
                    if (digits < 3) {
                        continue;
                    }
                    b = (byte) intval;
                } else if (digits > 0 && digits < 3) {
                    throw new TextParseException("'" + input
                        + "': bad escape-insufficient digits.");
                }
                octets[octetsLength++] = b;
                escaped = false;
            } else if (b == '\\') {
                escaped = true;
                digits = 0;
                intval = 0;
            } else {
                octets[octetsLength++] = b;
            }
        }
        if (octetsLength < input.length()) {
            byte[] newOctets = new byte[octetsLength];
            System.arraycopy(octets, 0, newOctets, 0, octetsLength);
            return newOctets;
        }
        return octets;
    }

}
