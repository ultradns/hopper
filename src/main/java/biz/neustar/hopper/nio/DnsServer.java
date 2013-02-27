package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.nio.handler.DNSMessageDecoder;
import biz.neustar.hopper.nio.handler.DNSMessageEncoder;
import biz.neustar.hopper.nio.handler.ServerMessageHandlerTCPInvoker;
import biz.neustar.hopper.nio.handler.ServerMessageHandlerUDPInvoker;
import biz.neustar.hopper.nio.handler.TCPDecoder;
import biz.neustar.hopper.nio.handler.TCPEncoder;

/**
 * A Server for the DNS protocol that handles TCP and UPD request. Register a
 * ServerMessageHandler to process client request.<br/>
 *
 * Create a message Handler:
 *
 * <pre>
 * public class EchoHandler implements ServerMessageHandler {
 *  public Message handleRequest(Message request) {
 *      return request;
 *  }
 * }
 * </pre>
 *
 * And start the server:
 *
 * <pre>
 * Server.builder().port(1053).serverMessageHandler(new EchoHandler()).build();
 * </pre>
 *
 * <p>
 * The server will have a instance of
 * org.jboss.netty.handler.logging.LoggingHandler as the first handler bound to
 * SLF4j as the implementation.
 * </p>
 *
 */
public class DnsServer {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DnsServer.class);

    /**
     * Server builder.
     */
    public static class Builder {
        private static final int DNS_PORT = 53;
        private int port = DNS_PORT;
        private ServerMessageHandler serverMessageHandler;

        private static final int DEFAULT_POOLSIZE = 10;
        private int threadPoolSize = DEFAULT_POOLSIZE;

        private static final int UDP_BUF_SIZE = 512;

        private int receiveBufferSize = UDP_BUF_SIZE;
        private int sendBufferSize = UDP_BUF_SIZE;

        private NioDatagramChannelFactory nioDatagramChannelFactory =
                new NioDatagramChannelFactory();
        private Map<String, Object> udpOptions = new HashMap<String, Object>();
        private NioServerSocketChannelFactory nioServerSocketChannelFactory = new NioServerSocketChannelFactory();
        private Map<String, Object> tcpOptions = new HashMap<String, Object>();
        private OrderedMemoryAwareThreadPoolExecutor omaThreadPoolExecutor;

        public Builder() {
            udpOptions.put("receiveBufferSize", receiveBufferSize);
            udpOptions.put("sendBufferSize", sendBufferSize);
        }

        public Builder udpOptions(final Map<String, Object> udpOptionsArg) {
            udpOptions = udpOptionsArg;
            return this;
        }

        /**
         * The port the server will listen on. Default is port 53
         */
        public Builder port(int portArg) {
            this.port = portArg;
            return this;
        }

        public Builder receiveBufferSize(int receiveBufferSizeArg) {
            this.receiveBufferSize = receiveBufferSizeArg;
            return this;
        }

        public Builder sendBufferSize(int sendBufferSizeArg) {
            this.sendBufferSize = sendBufferSizeArg;
            return this;
        }

        /**
         * The message handler to be invoked when a request is received.
         */
        public Builder serverMessageHandler(
                final ServerMessageHandler serverMessageHandlerArg) {
            this.serverMessageHandler = serverMessageHandlerArg;
            return this;
        }

        /**
         * The application thread pool size. Default is 10.
         */
        public Builder threadPoolSize(int threadPoolSizeArg) {

            this.threadPoolSize = threadPoolSizeArg;
            return this;
        }

        /**
         * The UDP channel factory. Default is a channel factory that uses a
         * Cache Thread pool for the worker thread pool
         */
        public Builder nioDatagramChannelFactory(
                final NioDatagramChannelFactory nioDatagramChannelFactoryArg) {
            this.nioDatagramChannelFactory = nioDatagramChannelFactoryArg;
            return this;
        }

        /**
         * The UDP channel factory. Default is a channel factory that uses a
         * Cache Thread pool for the boss and worker thread pool
         */
        public Builder nioServerSocketChannelFactory(
                final NioServerSocketChannelFactory nioServerSocketChannelFactoryArg) {
            this.nioServerSocketChannelFactory = nioServerSocketChannelFactoryArg;
            return this;
        }

        /**
         * The UPD connection options. Default is send and receive buffer size
         * of 512
         */
        public Builder tcpOptions(Map<String, Object> tcpOptionsArg) {

            this.tcpOptions = tcpOptionsArg;
            return this;
        }

        /**
         * The omaThreadPoolExecutor use to call application
         * hooks. If this is set, threadPoolSize is ignored.
         * 
         * @param omaThreadPoolExecutor
         * @return
         */
        public Builder omaThreadPoolExecutor(
                OrderedMemoryAwareThreadPoolExecutor omaThreadPoolExecutorArg) {
            this.omaThreadPoolExecutor = omaThreadPoolExecutorArg;
            return this;
        }

        /**
         * Create and start a new Server instance.
         *
         * @return A Server
         */
        public DnsServer build() {

            if (serverMessageHandler == null) {
                throw new IllegalStateException("serverMessageHandler must be set");
            }
            return new DnsServer(this);
        }
    }

    /**
     * Obtain a new builder instance.
     *
     * @return A Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The TCP Channel factory.
     */
    private final AtomicReference<ChannelFactory> tcpChannelFactory =
            new AtomicReference<ChannelFactory>();

    /**
     * The UDP Channel factory.
     */
    private final AtomicReference<ChannelFactory> udpChannelFactory =
            new AtomicReference<ChannelFactory>();

    /**
     * The local address to which the server is bound. If port 0 is requested,
     * the server will pick a random port which is readable from this attribute.
     */
    private final AtomicReference<InetSocketAddress> boundTo =
            new AtomicReference<InetSocketAddress>();

    /**
     * Open channels that need to be shutdown upon server shutdown.
     */
    private final ChannelGroup channelGroup = new DefaultChannelGroup();

    /**
     * Load the SLF4J binding for org.jboss.netty.handler.logging.LoggingHandler
     */
    static {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
    }

    /**
     * Start a server with settings specified in builder.
     *
     * @param builder
     *            The builder.
     */
    public DnsServer(final Builder builder) {

        LOGGER.debug("Binding to {}", builder.port);

        final OrderedMemoryAwareThreadPoolExecutor omaThreadPoolExecutor =
                builder.omaThreadPoolExecutor == null ? new OrderedMemoryAwareThreadPoolExecutor(
                builder.threadPoolSize, 0, 0) : builder.omaThreadPoolExecutor;

                // Start listening for UDP request
                udpChannelFactory.set(builder.nioDatagramChannelFactory);
                ConnectionlessBootstrap udpBootstrap =
                        new ConnectionlessBootstrap(udpChannelFactory.get());
                udpBootstrap.setOptions(builder.udpOptions);
                udpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

                    @Override
                    public ChannelPipeline getPipeline() {
                        return Channels.pipeline(
                                new LoggingHandler(), new DNSMessageDecoder(),
                                new DNSMessageEncoder(),
                                new ExecutionHandler(omaThreadPoolExecutor),
                                new ServerMessageHandlerUDPInvoker(
                                        builder.serverMessageHandler));
                    }
                });
                Channel udpChannel = udpBootstrap.bind(
                        new InetSocketAddress(builder.port));
                channelGroup.add(udpChannel);
                this.boundTo.set(
                        ((InetSocketAddress) udpChannel.getLocalAddress()));

                // Start listening for TCP request on the same port
                tcpChannelFactory.set(builder.nioServerSocketChannelFactory);
                ServerBootstrap tcpBootstrap = new ServerBootstrap(
                        tcpChannelFactory.get());
                tcpBootstrap.setOptions(builder.tcpOptions);
                tcpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

                    @Override
                    public ChannelPipeline getPipeline() {
                        return Channels.pipeline(new LoggingHandler(),
                                new TCPDecoder(), new TCPEncoder(),
                                new DNSMessageDecoder(),
                                new DNSMessageEncoder(),
                                new ExecutionHandler(omaThreadPoolExecutor),
                                new ServerMessageHandlerTCPInvoker(
                                                builder.serverMessageHandler));
                    }
                });
                LOGGER.info("Binding to {}", builder.port);
                Channel tcpChannel = tcpBootstrap.bind(
                        new InetSocketAddress(this.boundTo.get().getPort()));
                channelGroup.add(tcpChannel);

                // let clients know what we are up to
                LOGGER.info("Bound to {}", udpChannel.getLocalAddress());
    }

    /**
     * The address upon which the server is listening.
     *
     * @return The local address
     */
    protected InetSocketAddress getLocalAddress() {
        return boundTo.get();
    }

    /**
     * Shutdown the server.
     */
    public void stop() {

        LOGGER.info("Stopping...");
        channelGroup.close().awaitUninterruptibly();
        tcpChannelFactory.get().releaseExternalResources();
        udpChannelFactory.get().releaseExternalResources();
        LOGGER.info("Stopped");
    }

    /**
     * A friendly and helpful message suitable for framing.
     */
    @Override
    public String toString() {
        return "DNSServer [" + boundTo.get() + "]";
    }
}

