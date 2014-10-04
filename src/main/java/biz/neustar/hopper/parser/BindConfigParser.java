/**
 * Copyright 2000-2014 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */
package biz.neustar.hopper.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.TSIG;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 
 * This class is used to parse the bind configuration file
 * to get the meta-data associated with the zones.
 * It is restricted to the specific format of configuration file.
 *
 */
public class BindConfigParser {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BindConfigParser.class);

    /** Command line arguments **/
    private static final String CONF_FILE_OPTION = "c";
    private static final String ZONES_FILE_OPTION = "f";
    private final String HELP_OPTION = "help";


    public static void main(final String args[]) {
        try {
            BindConfigParser bindConfigParser = new BindConfigParser();
            ArgumentsParsingResult argumentsParsingResult =
                    bindConfigParser.validateArguments(args);
            if (argumentsParsingResult.getArgumentErrorCode() == 0) {
                Map<String, Map<String, TSIG>> zonesFullMetaData =
                        bindConfigParser.parseConfigFile(argumentsParsingResult.getBingConfigFile(),
                                argumentsParsingResult.getZonesName());
                LOGGER.debug("Zones with its fully required metatada are: {} ", zonesFullMetaData);
            } else {
                return;
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred: '{}'", e.getMessage());
        }
    }

    /**
     * This method validates the command line arguments.
     *
     * @param args The command line arguments.
     *
     * @return The argument parsing result.
     *
     * @throws Exception
     */
    public ArgumentsParsingResult validateArguments(final String args[]) throws Exception {
        ArgumentsParsingResult argumentsParsingResult = new ArgumentsParsingResult();

        Set<String> zonesName = Sets.newHashSet();
        BindConfigParser bindConfigParser = new BindConfigParser();
        CommandLine cmd = null;
        boolean zonesFilePresent = false;

        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("conf_file")
                .hasArg().withDescription("Configuration file that contains zones information")
                .create(CONF_FILE_OPTION));
        options.addOption(OptionBuilder.withArgName("zones_file")
                .hasArg().withDescription("Input file that contains zones name")
                .create(ZONES_FILE_OPTION));

        options.addOption(HELP_OPTION, false, "Prints this message");

        try {
            cmd = new GnuParser().parse(options, args);
        } catch (ParseException e) {
            LOGGER.error("Invalid command line! {}", e.getMessage());
            argumentsParsingResult.setArgumentErrorMessage(e.getMessage());
            argumentsParsingResult.setArgumentErrorCode((ExitCode.COMMAND_LINE_ERROR).getCode());
            return argumentsParsingResult;
        }

        if (cmd.hasOption(HELP_OPTION)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Bind NameServer Configuration file parser:", options);
            argumentsParsingResult.setArgumentErrorMessage((ExitCode.HELP_ARGUMENT).getDescription());
            argumentsParsingResult.setArgumentErrorCode((ExitCode.HELP_ARGUMENT).getCode());
            return argumentsParsingResult;
        }
        int index = 0;

        if (cmd.hasOption(CONF_FILE_OPTION)) {
            LOGGER.debug("Configuration file option is enabled");
            ++index;
        }

        if (cmd.hasOption(ZONES_FILE_OPTION)) {
            zonesFilePresent = true;
            LOGGER.debug("Zones name input file option is enabled");
            ++index;
        }

        if (cmd.hasOption(ZONES_FILE_OPTION) && !cmd.hasOption(CONF_FILE_OPTION)) {         
            LOGGER.error((ExitCode.MISSING_CONFIG_FILE).getDescription());
            argumentsParsingResult.setArgumentErrorMessage((ExitCode.MISSING_CONFIG_FILE).getDescription());
            argumentsParsingResult.setArgumentErrorCode((ExitCode.MISSING_CONFIG_FILE).getCode());
            return argumentsParsingResult;
        }

        if (index == 0) {
            LOGGER.error("Invalid command line!. One of the options -c, -f or -help must be specified");
            argumentsParsingResult.setArgumentErrorMessage((ExitCode.NO_ARGUMENT).getDescription());
            argumentsParsingResult.setArgumentErrorCode((ExitCode.NO_ARGUMENT).getCode());
            return argumentsParsingResult;
        }
        if (cmd.getOptions().length > 2) {
            LOGGER.error("Invalid command line!. Only two from these options -c, -f or -help can be specified");
            argumentsParsingResult.setArgumentErrorMessage((ExitCode.INVALID_NO_OF_ARGUMENTS).getDescription());
            argumentsParsingResult.setArgumentErrorCode((ExitCode.INVALID_NO_OF_ARGUMENTS).getCode());
            return argumentsParsingResult;
        }
        String confFile = cmd.getOptionValue(CONF_FILE_OPTION);

        if (!new File(confFile).exists()) {
            LOGGER.error("Invalid command line!. Input 'configuration file' does not exists");
            argumentsParsingResult.setArgumentErrorMessage((ExitCode.ARGUMENT_FILE_NOT_FOUND).getDescription());
            argumentsParsingResult.setArgumentErrorCode((ExitCode.ARGUMENT_FILE_NOT_FOUND).getCode());
            return argumentsParsingResult;
        }
        if (zonesFilePresent) {
            String zonesFile = cmd.getOptionValue(ZONES_FILE_OPTION);
            if (!new File(zonesFile).exists()) {
                LOGGER.error("Invalid command line!. Input 'zones name file' does not exists");
                argumentsParsingResult.setArgumentErrorMessage((ExitCode.ARGUMENT_FILE_NOT_FOUND).getDescription());
                argumentsParsingResult.setArgumentErrorCode((ExitCode.ARGUMENT_FILE_NOT_FOUND).getCode());
                return argumentsParsingResult;
            } else {
                //prepare a set of zones from the input file containing zone names.
                zonesName = bindConfigParser.extractZonesNameFromFile(zonesFile);
            }
        }
        argumentsParsingResult.setBingConfigFile(confFile);
        argumentsParsingResult.setZonesName(zonesName);
        return argumentsParsingResult;

    }
    /**
     * This method extract the zones name from the input file.
     * @param zonesFile The file containing zones name.
     * @return zonesName Set of zones name.
     * @throws Exception In case of any error.
     */
    public  Set<String> extractZonesNameFromFile(final String zonesFile) throws Exception {
        Set<String> zonesName = Sets.newLinkedHashSet();
        FileInputStream  fs = new FileInputStream(zonesFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(fs));
        try {
            String line = null;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                if (!st.hasMoreTokens()) {
                    continue;
                }
                String zoneFqdnName = null;
                String zoneName = st.nextToken();
                if (zoneName.charAt(zoneName.length() - 1) != '.') {
                    zoneFqdnName = zoneName + ".";
                    zonesName.add(zoneFqdnName);
                } else {
                    zonesName.add(zoneName);
                }
            }
        } finally {
            if (fs != null) {
                fs.close();
            }
        }
        return zonesName;
    }

    /**
     * This method parses the configuration file as per the set zonesName provided.
     * Parses whole file if set is empty otherwise for the selected zones only.
     * @param fileName Bind configuration file.
     * @param zonesName Set containing zones name for which bind file has to be parsed.
     * @return zonesFullMetaData Map representing the zones with its full required meta-data.
     * @throws Exception In case of any error.
     */
    public Map<String, Map<String,TSIG>> parseConfigFile(final String fileName, Set<String> zonesName)
            throws Exception {

        Map<String, TSIG> tsigNameToTSIGMap = Maps.newLinkedHashMap();
        Map<String, Map<String,String>> zonesPartialMetaData = Maps.newLinkedHashMap();
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(fileName);
            parseConfFile(fileInputStream, zonesPartialMetaData, tsigNameToTSIGMap, zonesName);
            LOGGER.debug("Zones with its partially required metatada are: {} " ,zonesPartialMetaData);
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
        LOGGER.debug("TSIG information associated with the zones are: {}", tsigNameToTSIGMap);
        return prepareZonesWithMetadata(zonesPartialMetaData, tsigNameToTSIGMap);
    }

    /**
     * 
     * @param fileInputStream Input stream to parse the file.
     * @param zonesPartialMetaData Map containing zone name as key and masterToTsigNameMap as value.
     * @param tsigNameToTSIGMap Map containing TSIG name as key and TSIG object as value. 
     * @param zonesName Set containing zone names.
     */
    private  void parseConfFile(FileInputStream fileInputStream,
            Map<String, Map<String,String>> zonesPartialMetaData, Map<String, TSIG> tsigNameToTSIGMap,
            Set<String> zonesName) {
        BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
        Map<String, String> masterToTsigNameMap = Maps.newLinkedHashMap();
        String line = null;
        Stack<String> zonesNameStack = new Stack<String>();
        try {
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                if (!st.hasMoreTokens())
                    continue;
                String keyword = st.nextToken();
                if (!st.hasMoreTokens()) {
                    continue;
                }
                if (keyword.charAt(0) == '#')
                    continue;
                if (keyword.equals("key")) {
                    String tsigAlgorithm = null, tsigKeyValue = null ;
                    String tsigKeyName = st.nextToken().replaceAll("\"","");
                    st = new StringTokenizer(br.readLine());
                    if(st.nextToken().equals("algorithm"))
                        tsigAlgorithm = st.nextToken().replaceAll(";","");
                    st = new StringTokenizer(br.readLine());
                    if(st.nextToken().equals("secret"))
                        tsigKeyValue = st.nextToken().replaceAll(";","").replaceAll("\"","");
                    TSIG tsig = new TSIG(tsigAlgorithm, tsigKeyName, tsigKeyValue);
                    tsigNameToTSIGMap.put(tsig.getName(), tsig);
                }    
                if (keyword.equals("zone")) {
                    String zoneName = null;
                    String name = st.nextToken().replaceAll("\"",""); 

                    if (CollectionUtils.isEmpty(zonesName)) {
                        LOGGER.debug("There is no zone name present,hence parse the complete file for all the zones.");
                        // true = parse the configuration file for all the zones
                        if (name.charAt(name.length() - 1) != '.') {
                            zoneName = name + ".";
                            zonesNameStack.push(zoneName);
                        } else {
                            zonesNameStack.push(name);
                        }
                    } else {
                        //get FQDN of zone
                        if (name.charAt(name.length() - 1) != '.') 
                            zoneName = name + ".";
                        else {
                            zoneName = name;
                        }
                        // false = check zone existence in the zones set
                        if (zonesName.contains(zoneName)) {  
                            zonesNameStack.push(zoneName);
                        } else {
                            continue;
                        }
                    }
                }
                if (keyword.equals("masters")) {
                    st = new StringTokenizer(br.readLine());
                    String master = st.nextToken().replaceAll(";", "");
                    if (!isValidIPV4Address(master)) {
                        master = null;
                        continue;
                    }
                    String tsigName = null;
                    if (st.hasMoreTokens()) {
                        if (st.nextToken().equals("key")) {
                            tsigName = st.nextToken().replaceAll(";", "");
                        }
                    }
                    line = br.readLine();
                    if (line != null) {
                        masterToTsigNameMap = Maps.newLinkedHashMap();
                        masterToTsigNameMap.put(master, tsigName);
                        st = new StringTokenizer(line);
                        master = st.nextToken().replaceAll(";", "");
                        if (master.equals("};")) {
                            zonesPartialMetaData.put(zonesNameStack.pop().toString(),
                                    masterToTsigNameMap);
                            continue;
                        }
                        //call checkNextIp method
                        checkNextIp(st, master, masterToTsigNameMap);
                    }
                    line = br.readLine();
                    if (line != null) {
                        st = new StringTokenizer(line);
                        master = st.nextToken().replaceAll(";", "");
                        if (master.equals("};")) {
                            zonesPartialMetaData.put(zonesNameStack.pop().toString(),
                                    masterToTsigNameMap);
                            continue;
                        }
                        //call checkNextIp method
                        checkNextIp(st, master, masterToTsigNameMap);
                    }
                    if (!zonesNameStack.empty()) {
                        zonesPartialMetaData.put(zonesNameStack.pop().toString(),
                                masterToTsigNameMap);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception '{}' occurred while parsing the configuration file to get metadata",
                    e.getMessage());
        }
    }

    /**
     * @param zonesPartialMetaData Map containing zone name as key and masterToTsigNameMap as value.
     * @param tsigNameToTSIGMap Map containing TSIG name as key and TSIG object as value. 
     * @return zonesFullMetaData Map representing the zones with its full required meta-data.
     */
    private Map<String, Map<String, TSIG>> prepareZonesWithMetadata(
            final Map<String, Map<String, String>> zonesPartialMetaData,
            final Map<String, TSIG> tsigNameToTSIGMap) {

        Map<String, Map<String,TSIG>> zonesFullMetaData = Maps.newLinkedHashMap();
        //Iterate map to associate zone name with the metadata having full TSIG details
        for (Entry<String, Map<String, String>> partialMetaData : zonesPartialMetaData.entrySet()) {
            String zoneName = partialMetaData.getKey();
            Map<String, String> mastersAndTsigNames = partialMetaData.getValue();
            Map<String, TSIG> mastersAndTsig = Maps.newLinkedHashMap();
            for(Entry<String, String> mastersAndTsigNamesEntry : mastersAndTsigNames.entrySet()) {
                TSIG tsig = null;
                if(tsigNameToTSIGMap.containsKey(mastersAndTsigNamesEntry.getValue())) {
                    tsig = tsigNameToTSIGMap.get(mastersAndTsigNamesEntry.getValue());
                    LOGGER.debug("tsig name: {}", tsig.getName());
                    LOGGER.debug("tsig key: {}", tsig.getKey());
                    LOGGER.debug("tsig algo: {}", tsig.getAlgorithm());
                }
                mastersAndTsig.put(mastersAndTsigNamesEntry.getKey(), tsig);
            }
            zonesFullMetaData.put(zoneName, mastersAndTsig);
        }
        return zonesFullMetaData;
    }

    /**
     * This method checks whether more than one IP(masters info) is associated with the zone.
     * @param token Token to check for the existence of IP.
     * @param ip Input String to be validated for IP check.
     * @param tsigName TSIG name associated with the master(IP).
     * @param masterToTsigNameMap Map containing IP as key and TSIG name as value.
     */
    private void checkNextIp(final StringTokenizer token, final String ip,
            final Map<String, String> masterToTsigNameMap) {
        String tsigName = null;
        if (isValidIPV4Address(ip)) {
            if (token.hasMoreTokens()) {
                String keyword2 = token.nextToken();
                if (keyword2.equals("key")) {
                    tsigName = token.nextToken().replaceAll(";", "");
                }
            }
            masterToTsigNameMap.put(ip, tsigName);
        }
    }
    /**
     * This method checks the given string whether its is a valid IP address.
     * @param ipV4Address IP address.
     * @return true if its a valid IP address otherwise false.
     */
    private  boolean isValidIPV4Address(final String ipV4Address) {
        final List<String> INVALID_IPV4_ADDRESSES = Arrays
                .asList(new String[] {"127.0.0.1"});
        boolean valid = false;
        if ((ipV4Address != null)
                && (!(INVALID_IPV4_ADDRESSES.contains(ipV4Address)))
                && (InetAddressValidator.getInstance().isValid(ipV4Address))
                && (isValidIPOctets(Arrays.asList(ipV4Address.split("\\."))))) {
            valid = true;
        }
        return valid;
    }

    private boolean isValidIPOctets(final List<String> octets) {
        boolean valid = true;
        if (octets.get(0).startsWith("0")) {
            return false;
        }
        if (!(CollectionUtils.isEmpty(octets))) {
            for (String octet : octets) {
                if ((octet.startsWith("0")) && (octet.length() > 1)) {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }

    /**
     * Enum representing the System Exit code.
     */
    enum ExitCode {
        COMMAND_LINE_ERROR(1,"Invalid command line arguments!"),
        NO_ARGUMENT(2,"Invalid command line!. One of the options -c, -f or -help must be specified"),
        INVALID_NO_OF_ARGUMENTS(3,"Invalid command line!. Only two from these options -c, -f or -help can be specified"),
        ARGUMENT_FILE_NOT_FOUND(4,"Invalid command line! Input file does not exist"),
        HELP_ARGUMENT(5,"Tool help"),
        MISSING_CONFIG_FILE(6,"Invalid command line! Configuration file is missing");

        private final int code;
        private final String description;

        private ExitCode(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

}
