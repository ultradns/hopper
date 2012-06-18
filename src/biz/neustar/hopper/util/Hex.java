/**
 * Copyright 2000-2012 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.util;

import javax.xml.bind.DatatypeConverter;

public class Hex {
    
    public static byte[] decode(char[] hexValues) {
        return DatatypeConverter.parseHexBinary(new String(hexValues));
    }
    
    public static byte[] decode(String hexValues) {
        return DatatypeConverter.parseHexBinary(hexValues);
    }
    
    public static String encode(byte[] hexData) {
        return DatatypeConverter.printHexBinary(hexData);
    }
    
}
