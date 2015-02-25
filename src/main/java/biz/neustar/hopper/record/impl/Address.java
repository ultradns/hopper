// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record.impl;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.ARecord;
import biz.neustar.hopper.record.PTRRecord;
import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.resolver.Lookup;
import biz.neustar.hopper.util.ReverseMap;

import org.apache.commons.validator.routines.InetAddressValidator;

import com.google.common.net.InetAddresses;

/**
 * Routines dealing with IP addresses. Includes functions similar to those in
 * the java.net.InetAddress class.
 * 
 * @author Brian Wellington
 */

public final class Address {

	public static final int IPv4 = 1;
	public static final int IPv6 = 2;

	private Address() {
	}

	private static byte[] parseV4(String s) {
		return parseInetAddress(s);
	}

	private static byte[] parseV6(String s) {
		return parseInetAddress(s);
	}

	private static byte[] parseInetAddress(String s) {
		if (InetAddressValidator.getInstance().isValidInet6Address(s)
				|| InetAddressValidator.getInstance().isValidInet4Address(s)) {
			try {
				if(InetAddresses.isMappedIPv4Address(s)) {
					return getMappedAddress(s);
				}
				InetAddress addr = InetAddress.getByName(s);
				return addr.getAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * By default, Java parses IPv4 mapped IPv6 addresses to Inet4Address
	 * However, our AAAA records only take Inet6Address records
	 * So we're doctoring a byte array to get an Inet6Address from an
	 * IPv4 mapped IPv6 address.  Kind of a hack.
	 * @param s - the IPv4 mapped IPv6 address in string form
	 * @return A 16 byte array containing the IPv6 address
	 */
	private static byte[] getMappedAddress(String s) {
		byte [] addr = new byte[16];
		Arrays.fill(addr, (byte)0);
		addr[10] = (byte)0xFF;
		addr[11] = (byte)0xFF;
		try {
			InetAddress inet = InetAddress.getByName(s);
			System.arraycopy(inet.getAddress(), 0, addr, 12, 4); 			
		} catch (UnknownHostException e){
			//should never be thrown
		}
		return addr;
	}

	/**
	 * Convert a string containing an IP address to an array of 4 or 16
	 * integers.
	 * 
	 * @param s
	 *            The address, in text format.
	 * @param family
	 *            The address family.
	 * @return The address
	 */
	public static int[] toArray(String s, int family) {
		byte[] byteArray = toByteArray(s, family);
		if (byteArray == null) {
			return null;
		}
		int[] intArray = new int[byteArray.length];
		for (int i = 0; i < byteArray.length; i++) {
			intArray[i] = byteArray[i] & 0xFF;
		}
		return intArray;
	}

	/**
	 * Convert a string containing an IPv4 address to an array of 4 integers.
	 * 
	 * @param s
	 *            The address, in text format.
	 * @return The address
	 */
	public static int[] toArray(String s) {
		return toArray(s, IPv4);
	}

	/**
	 * Convert a string containing an IP address to an array of 4 or 16 bytes.
	 * 
	 * @param s
	 *            The address, in text format.
	 * @param family
	 *            The address family.
	 * @return The address
	 */
	public static byte[] toByteArray(String s, int family) {
		if (family == IPv4) {
			return parseV4(s);
		} else if (family == IPv6) {
			return parseV6(s);
		} else {
			throw new IllegalArgumentException("unknown address family");
		}
	}

	/**
	 * Determines if a string contains a valid IP address.
	 * 
	 * @param s
	 *            The string
	 * @return Whether the string contains a valid IP address
	 */
	public static boolean isDottedQuad(String s) {
		byte[] address = Address.toByteArray(s, IPv4);
		return (address != null);
	}

	/**
	 * Converts a byte array containing an IPv4 address into a dotted quad
	 * string.
	 * 
	 * @param addr
	 *            The array
	 * @return The string representation
	 */
	public static String toDottedQuad(byte[] addr) {
		return ((addr[0] & 0xFF) + "." + (addr[1] & 0xFF) + "."
				+ (addr[2] & 0xFF) + "." + (addr[3] & 0xFF));
	}

	/**
	 * Converts an int array containing an IPv4 address into a dotted quad
	 * string.
	 * 
	 * @param addr
	 *            The array
	 * @return The string representation
	 */
	public static String toDottedQuad(int[] addr) {
		return (addr[0] + "." + addr[1] + "." + addr[2] + "." + addr[3]);
	}

	private static Record[] lookupHostName(String name)
			throws UnknownHostException {
		try {
			Record[] records = new Lookup(name).run();
			if (records == null) {
				throw new UnknownHostException("unknown host");
			}
			return records;
		} catch (TextParseException e) {
			throw new UnknownHostException("invalid name");
		}
	}

	private static InetAddress addrFromRecord(String name, Record r)
			throws UnknownHostException {
		ARecord a = (ARecord) r;
		return InetAddress.getByAddress(name, a.getAddress().getAddress());
	}

	/**
	 * Determines the IP address of a host
	 * 
	 * @param name
	 *            The hostname to look up
	 * @return The first matching IP address
	 * @exception UnknownHostException
	 *                The hostname does not have any addresses
	 */
	public static InetAddress getByName(String name)
			throws UnknownHostException {
		try {
			return getByAddress(name);
		} catch (UnknownHostException e) {
			Record[] records = lookupHostName(name);
			return addrFromRecord(name, records[0]);
		}
	}

	/**
	 * Determines all IP address of a host
	 * 
	 * @param name
	 *            The hostname to look up
	 * @return All matching IP addresses
	 * @exception UnknownHostException
	 *                The hostname does not have any addresses
	 */
	public static InetAddress[] getAllByName(String name)
			throws UnknownHostException {
		try {
			InetAddress addr = getByAddress(name);
			return new InetAddress[] { addr };
		} catch (UnknownHostException e) {
			Record[] records = lookupHostName(name);
			InetAddress[] addrs = new InetAddress[records.length];
			for (int i = 0; i < records.length; i++) {
				addrs[i] = addrFromRecord(name, records[i]);
			}
			return addrs;
		}
	}

	/**
	 * Converts an address from its string representation to an IP address. The
	 * address can be either IPv4 or IPv6.
	 * 
	 * @param addr
	 *            The address, in string form
	 * @return The IP addresses
	 * @exception UnknownHostException
	 *                The address is not a valid IP address.
	 */
	public static InetAddress getByAddress(String addr)
			throws UnknownHostException {
		byte[] bytes;
		bytes = toByteArray(addr, IPv4);
		if (bytes != null && bytes.length == 4) {
			return InetAddress.getByAddress(bytes);
		}
		bytes = toByteArray(addr, IPv6);
		if (bytes != null && bytes.length == 16) {
			return Inet6Address.getByAddress(null, bytes, null);
		}
		throw new UnknownHostException("Invalid address: " + addr);
	}

	/**
	 * Converts an address from its string representation to an IP address in a
	 * particular family.
	 * 
	 * @param addr
	 *            The address, in string form
	 * @param family
	 *            The address family, either IPv4 or IPv6.
	 * @return The IP addresses
	 * @exception UnknownHostException
	 *                The address is not a valid IP address in the specified
	 *                address family.
	 */
	public static InetAddress getByAddress(String addr, int family)
			throws UnknownHostException {
		if (family != IPv4 && family != IPv6) {
			throw new IllegalArgumentException("unknown address family");
		}
		byte[] bytes;
		bytes = toByteArray(addr, family);
		if (bytes != null && bytes.length == 4) {
			return InetAddress.getByAddress(bytes);
		} else if (bytes != null && bytes.length == 16) {
			return Inet6Address.getByAddress(null, bytes, null);
		}
		throw new UnknownHostException("Invalid address: " + addr);
	}

	/**
	 * Determines the hostname for an address
	 * 
	 * @param addr
	 *            The address to look up
	 * @return The associated host name
	 * @exception UnknownHostException
	 *                There is no hostname for the address
	 */
	public static String getHostName(InetAddress addr)
			throws UnknownHostException {
		Name name = ReverseMap.fromAddress(addr);
		Record[] records = new Lookup(name, Type.PTR).run();
		if (records == null) {
			throw new UnknownHostException("unknown address");
		}
		PTRRecord ptr = (PTRRecord) records[0];
		return ptr.getTarget().toString();
	}

	/**
	 * Returns the family of an InetAddress.
	 * 
	 * @param address
	 *            The supplied address.
	 * @return The family, either IPv4 or IPv6.
	 */
	public static int familyOf(InetAddress address) {
		if (address instanceof Inet4Address) {
			return IPv4;
		}
		if (address instanceof Inet6Address) {
			return IPv6;
		}
		throw new IllegalArgumentException("unknown address family");
	}

	/**
	 * Returns the length of an address in a particular family.
	 * 
	 * @param family
	 *            The address family, either IPv4 or IPv6.
	 * @return The length of addresses in that family.
	 */
	public static int addressLength(int family) {
		if (family == IPv4) {
			return 4;
		}
		if (family == IPv6) {
			return 16;
		}
		throw new IllegalArgumentException("unknown address family");
	}

	/**
	 * Truncates an address to the specified number of bits. For example,
	 * truncating the address 10.1.2.3 to 8 bits would yield 10.0.0.0.
	 * 
	 * @param address
	 *            The source address
	 * @param maskLength
	 *            The number of bits to truncate the address to.
	 */
	public static InetAddress truncate(InetAddress address, int maskLength) {
		int family = familyOf(address);
		int maxMaskLength = addressLength(family) * 8;
		if (maskLength < 0 || maskLength > maxMaskLength) {
			throw new IllegalArgumentException("invalid mask length");
		}
		if (maskLength == maxMaskLength) {
			return address;
		}
		byte[] bytes = address.getAddress();
		for (int i = maskLength / 8 + 1; i < bytes.length; i++) {
			bytes[i] = 0;
		}
		int maskBits = maskLength % 8;
		int bitmask = 0;
		for (int i = 0; i < maskBits; i++) {
			bitmask |= (1 << (7 - i));
		}
		bytes[maskLength / 8] &= bitmask;
		try {
			return InetAddress.getByAddress(bytes);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("invalid address");
		}
	}

}
