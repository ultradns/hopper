/*
 * *
 *  * Copyright 2000-2011 NeuStar, Inc. All rights reserved.
 *  * NeuStar, the Neustar logo and related names and logos are registered
 *  * trademarks, service marks or tradenames of NeuStar, Inc. All other
 *  * product names, company names, marks, logos and symbols may be trademarks
 *  * of their respective owners.
 *
 */
package biz.neustar.hopper.resolver;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Interface for TCP client
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 */
public interface TCPClient {

    void bind(SocketAddress addr) throws IOException;

    void connect(SocketAddress addr) throws IOException;

    void send(byte[] data) throws IOException;

    byte[] recv() throws IOException;

    void cleanup() throws IOException;
}