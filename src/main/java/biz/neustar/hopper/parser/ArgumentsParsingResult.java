/**
 * Copyright 2000-2014 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */
package biz.neustar.hopper.parser;

import java.util.Set;

/**
 * The class is used to store the result of validation of command line arguments
 * provided to parse bind configuration file.
 *
 */
public class ArgumentsParsingResult {

    /**
     * Bind configuration file.
     */
    private String bingConfigFile;

    /**
     * Set of zones name.
     */
    private Set<String> zonesName;

    /**
     * Error message for the invalid argument.
     */
    private String argumentErrorMessage;

    /**
     * Error code for the invalid argument.
     */
    private int argumentErrorCode;

    /**
     * This method returns the bind configuration file.
     * @return bingConfigFile Bind configuration file.
     */
    public String getBingConfigFile() {
        return bingConfigFile;
    }

    /**
     * This method sets the bind configuration file.
     * @param bingConfigFile Bind configuration file.
     */
    public void setBingConfigFile(final String bingConfigFile) {
        this.bingConfigFile = bingConfigFile;
    }

    /**
     * This method returns the set containing zones name. 
     * @return zonesName Set of zones name.
     */
    public Set<String> getZonesName() {
        return zonesName;
    }

    /**
     * This method sets the set containing zones name. 
     * @param zonesName Set of zones name.
     */
    public void setZonesName(final Set<String> zonesName) {
        this.zonesName = zonesName;
    }

    /**
     * This method returns the error code for the invalid argument.
     * @return argumentErrorCode The error code. 
     */
    public int getArgumentErrorCode() {
        return argumentErrorCode;
    }

    /**
     * This method sets the error code for the invalid argument.
     * @param argumentErrorCode The error code.
     */
    public void setArgumentErrorCode(final int argumentErrorCode) {
        this.argumentErrorCode = argumentErrorCode;
    }

    /**
     * This method returns the error code for the invalid argument.
     * @return argumentErrorMessage The error message.
     */
    public String getArgumentErrorMessage() {
        return argumentErrorMessage;
    }

    /**
     * This method sets the error message for the invalid argument.
     * @param argumentErrorMessage The error message.
     */
    public void setArgumentErrorMessage(final String argumentErrorMessage) {
        this.argumentErrorMessage = argumentErrorMessage;
    }


}
