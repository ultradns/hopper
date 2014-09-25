/**
 * Copyright 2000-2014 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */
package biz.neustar.hopper.parser;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import biz.neustar.hopper.message.TSIG;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

/**
 * The class used to test {@link BindConfigParser}.
 */
public class BindConfigParserTest {

    /**
     * The parser to parse the configuration file based on the command line input.
     */
    BindConfigParser bindConfigParser = new BindConfigParser();

    /**
     * This stores the result of validation of command line arguments.
     */
    ArgumentsParsingResult argumentsParsingResult = new ArgumentsParsingResult();

    /**
     * This method test the scenarios when invalid arguments
     * are passed from command line.
     * @throws Exception In case of any unexpected error.
     */
    @Test
    public void testParserForInvalidArguments() throws Exception {
        argumentsParsingResult = bindConfigParser.validateArguments(new String[]{"-p"});
        Assert.assertEquals(1, argumentsParsingResult.getArgumentErrorCode());

        argumentsParsingResult = bindConfigParser.validateArguments(new String[]{"-cD:/named.conf","-p"});
        Assert.assertEquals(1, argumentsParsingResult.getArgumentErrorCode());

        //configuration file is missing
        argumentsParsingResult = bindConfigParser.validateArguments(new String[]{"-c"});
        Assert.assertEquals(1, argumentsParsingResult.getArgumentErrorCode());

        //no argument is passed
        argumentsParsingResult = bindConfigParser.validateArguments(new String[]{""});
        Assert.assertEquals(2, argumentsParsingResult.getArgumentErrorCode());
        Assert.assertEquals("Invalid command line!. One of the options -c, -f or -help must be specified",
                argumentsParsingResult.getArgumentErrorMessage());

        //when more than 2 arguments is passed 
        argumentsParsingResult = bindConfigParser.validateArguments(new String[]{"-cD:/named.conf","-fD:/zones.txt","-cD:/named.conf"});
        Assert.assertEquals(3, argumentsParsingResult.getArgumentErrorCode());
        Assert.assertEquals("Invalid command line!. Only two from these options -c, -f or -help can be specified",
                argumentsParsingResult.getArgumentErrorMessage());

        //configuration file doesn't exist 
        argumentsParsingResult = bindConfigParser.validateArguments(new String[]{"-c", "named1.conf"});
        Assert.assertEquals(4, argumentsParsingResult.getArgumentErrorCode());
        Assert.assertEquals("Invalid command line! Input file does not exist",
                argumentsParsingResult.getArgumentErrorMessage());

        //help as an argument is passed
        argumentsParsingResult = bindConfigParser.validateArguments(new String[]{"-help"});
        Assert.assertEquals(5, argumentsParsingResult.getArgumentErrorCode());
        Assert.assertEquals("Tool help",
                argumentsParsingResult.getArgumentErrorMessage());
    }

    /**
     * This method tests the scenario when valid argument having
     * configuration file is passed from command line and input file
     * of zones name is not present.
     * @throws Exception In case of any unexpected error.
     */
    @Test
    public void testMainForArgumentValidConfigFile() throws Exception {
        String namedFile = Resources.getResource("named.conf").getFile();
        argumentsParsingResult = bindConfigParser.validateArguments(
                (new String[]{"-c", namedFile}));
        Assert.assertEquals(0, argumentsParsingResult.getArgumentErrorCode());
        Map<String, Map<String, TSIG>> actualZonesFullMetaData =
                bindConfigParser.parseConfigFile(argumentsParsingResult.getBingConfigFile(),
                        argumentsParsingResult.getZonesName());
        Map<String, Map<String,TSIG>> expectedZonesFullMetaData = Maps.newLinkedHashMap();
        Map<String,TSIG> metaDataMap = Maps.newLinkedHashMap();
        TSIG tsig1 = new TSIG("hmac-md5", "1766.4512.nominum.", "5lFtjHexNIhuWtBfqIbVLajCbDjL01vB");
        TSIG tsig2 = new TSIG("hmac-md5", "4109.14145.nominum.", "u9UW1/miH3xmRu5XC55F4CNInSjJEpJZ");
        TSIG tsig3 = null;

        metaDataMap.put("1.1.1.1", tsig3);
        metaDataMap.put("2.2.2.2", tsig3);
        metaDataMap.put("3.3.3.3", tsig3);
        expectedZonesFullMetaData.put("gmon-n.invalid.", metaDataMap);

        metaDataMap = Maps.newLinkedHashMap();
        metaDataMap.put("66.120.18.226", tsig2);
        metaDataMap.put("205.252.8.100", tsig1);
        expectedZonesFullMetaData.put("dlmc-1.com.", metaDataMap);

        metaDataMap = Maps.newLinkedHashMap();
        metaDataMap.put("10.31.41.18", tsig3);
        metaDataMap.put("10.31.41.19", tsig3);
        expectedZonesFullMetaData.put("gmon-o.invalid.", metaDataMap);
        //Assert the result of parser.
        assertZones(expectedZonesFullMetaData, actualZonesFullMetaData);
    }

    /**
     * This method tests the scenario when valid arguments having both
     * configuration file and zones name file are passed from command line.
     * @throws Exception In case of any unexpected error.
     */
    @Test
    public void testMainForArgumentForValidInputZonesFile() throws Exception {
        String namedFile = Resources.getResource("named.conf").getFile();
        String zoneFile = Resources.getResource("zones.txt").getFile();
        argumentsParsingResult = bindConfigParser.validateArguments((new String[]{"-c", namedFile,
                "-f", zoneFile}));
        Assert.assertEquals(0,argumentsParsingResult.getArgumentErrorCode());
        Map<String, Map<String, TSIG>> actualZonesFullMetaData =
                bindConfigParser.parseConfigFile(argumentsParsingResult.getBingConfigFile(),
                        argumentsParsingResult.getZonesName());
        Map<String, Map<String,TSIG>> expectedZonesFullMetaData = Maps.newLinkedHashMap();
        Set<String> zonesName  = Sets.newHashSet();
        zonesName.add("gmon-n.invalid.");
        zonesName.add("dlmc-1.com.");
        Map<String,TSIG> metaDataMap = Maps.newLinkedHashMap();
        TSIG tsig1 = new TSIG("hmac-md5", "1766.4512.nominum.", "5lFtjHexNIhuWtBfqIbVLajCbDjL01vB");
        TSIG tsig2 = new TSIG("hmac-md5", "4109.14145.nominum.", "u9UW1/miH3xmRu5XC55F4CNInSjJEpJZ");
        TSIG tsig3 = null;

        metaDataMap.put("1.1.1.1", tsig3);
        metaDataMap.put("2.2.2.2", tsig3);
        metaDataMap.put("3.3.3.3", tsig3);
        expectedZonesFullMetaData.put("gmon-n.invalid.", metaDataMap);

        metaDataMap = Maps.newLinkedHashMap();
        metaDataMap.put("66.120.18.226", tsig2);
        metaDataMap.put("205.252.8.100", tsig1);
        expectedZonesFullMetaData.put("dlmc-1.com.", metaDataMap);
        //Assert the result of parser.
        assertZones(expectedZonesFullMetaData, actualZonesFullMetaData);
    }

    private void assertZones(Map<String, Map<String,TSIG>>  expectedZonesFullMetaData,
            Map<String, Map<String,TSIG>>  actualZonesFullMetaData){

        Iterator<Entry<String, Map<String, TSIG>>> expectedZones = expectedZonesFullMetaData.entrySet().iterator();
        Iterator<Entry<String, Map<String, TSIG>>> actualZones = actualZonesFullMetaData.entrySet().iterator();

        while (expectedZones.hasNext() && actualZones.hasNext()) {
            Entry<String, Map<String, TSIG>> expectedEntry = expectedZones.next();
            Entry<String, Map<String, TSIG>> actualEntry = actualZones.next();
            Assert.assertEquals(expectedEntry.getKey(), actualEntry.getKey());

            Map<String, TSIG> expectedMastersAndTsigNames = expectedEntry.getValue();
            Map<String, TSIG> actualMastersAndTsigNames = actualEntry.getValue();

            Iterator<Entry<String, TSIG>> iterator3 = expectedMastersAndTsigNames.entrySet().iterator();
            Iterator<Entry<String, TSIG>> iterator4 = actualMastersAndTsigNames.entrySet().iterator();

            while (iterator3.hasNext() && iterator4.hasNext()) {
                Entry<String, TSIG> expectedEntry1 = iterator3.next();
                Entry<String, TSIG> actualEntry1 = iterator4.next();
                Assert.assertEquals(expectedEntry1.getKey(), actualEntry1.getKey());
                TSIG expectedTsig = expectedEntry1.getValue();
                TSIG actualTsig = actualEntry1.getValue();
                if (actualTsig != null && expectedTsig != null) {
                    Assert.assertEquals(expectedTsig.getAlgorithm(), actualTsig.getAlgorithm());
                    Assert.assertEquals(expectedTsig.getKey(), actualTsig.getKey());
                    Assert.assertEquals(expectedTsig.getName(), actualTsig.getName());
                }
            }
        } 
    }
}
