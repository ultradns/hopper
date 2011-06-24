package com.ultradns.dnsjava.dns;

import junit.framework.TestCase;
import org.junit.Assert;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ZoneTransferInTest extends TestCase {

	public void test_connecttimeout() throws IOException, UnknownHostException, ZoneTransferException {
		String zoneName = "test.zone.";
		//Lets hope nobody turns on port 53 on this host ever, so it will be unreachable.
		String unreachableHost = "240.10.10.10";
		int connectTimeout = 10;
		int timeout = 900;

		ZoneTransferIn zoneTransferIn = ZoneTransferIn.newAXFR(new Name(zoneName), unreachableHost, null);
		zoneTransferIn.setConnectTimeout(connectTimeout);
		zoneTransferIn.setTimeout(timeout);

		long startTime = System.currentTimeMillis();
		long endTime, timeTakenInSecs;
		try {
			zoneTransferIn.run();
			Assert.fail("Zone transfer did not throw any exception");
		}
		catch (SocketTimeoutException e) {
			endTime = System.currentTimeMillis();
			timeTakenInSecs = (endTime - startTime)/1000;
			org.junit.Assert.assertTrue(timeTakenInSecs <= timeout);
		}
	}
}
