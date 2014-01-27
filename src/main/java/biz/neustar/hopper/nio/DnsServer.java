package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.nio.handler.AdvancedServerMessageHandlerTCPInvoker;
import biz.neustar.hopper.nio.handler.AdvancedServerMessageHandlerUDPInvoker;
import biz.neustar.hopper.nio.handler.DNSMessageDecoder;
import biz.neustar.hopper.nio.handler.DNSMessageEncoder;
import biz.neustar.hopper.nio.handler.ServerMessageHandlerTCPInvoker;
import biz.neustar.hopper.nio.handler.ServerMessageHandlerUDPInvoker;
import biz.neustar.hopper.nio.handler.TCPDecoder;
import biz.neustar.hopper.nio.handler.TCPEncoder;
import biz.neustar.hopper.nio.handler.TcpIdleChannelHandler;

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
     * The timer for TCP idle state.
     */
    private final Timer timer = new HashedWheelTimer();

    /**
     * Server builder.
     */
    public static class Builder {
        private static final int DNS_PORT = 53;
        private int port = DNS_PORT;
        private boolean advanced = false;
        private boolean logging = false;
        private ServerMessageHandler serverMessageHandler;
        private AdvancedServerMessageHandler advancedServerMessageHandler;

        private static final int DEFAULT_POOLSIZE = 10;
        private int udpThreadPoolSize = DEFAULT_POOLSIZE;
        private int tcpThreadPoolSize = DEFAULT_POOLSIZE;

        private static final int UDP_BUF_SIZE = 512;

        private int receiveBufferSize = UDP_BUF_SIZE;
        private int sendBufferSize = UDP_BUF_SIZE;

        private NioDatagramChannelFactory nioDatagramChannelFactory =
                new NioDatagramChannelFactory();
        private Map<String, Object> udpOptions = new HashMap<String, Object>();
        private NioServerSocketChannelFactory nioServerSocketChannelFactory = new NioServerSocketChannelFactory();
        private Map<String, Object> tcpOptions = new HashMap<String, Object>();
        private Executor udpExecutor;
        private Executor tcpExecutor;

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

        public Builder logging(boolean loggingArg) {
            this.logging = loggingArg;
            return this;
        }

        public Builder advanced(boolean advancedArg) {
            this.advanced = advancedArg;
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
         * The advanced message handler to be invoked when a request is received.
         */
        public Builder advancedServerMessageHandler(
                final AdvancedServerMessageHandler advancedServerMessageHandlerArg) {
            this.advancedServerMessageHandler = advancedServerMessageHandlerArg;
            return this;
        }


        /**
         * The application thread pool size. Default is 10.
         */
        public Builder udpThreadPoolSize(int threadPoolSizeArg) {

            this.udpThreadPoolSize = threadPoolSizeArg;
            return this;
        }

        /**
         * The application thread pool size. Default is 10.
         */
        public Builder tcpThreadPoolSize(int threadPoolSizeArg) {

            this.tcpThreadPoolSize = threadPoolSizeArg;
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
         * The executor use to call application
         * hooks. If this is set, threadPoolSize is ignored.
         *
         * @param executorArg
         * @return
         */
        public Builder udpExecutor(
                final Executor executorArg) {
            this.udpExecutor = executorArg;
            return this;
        }

        /**
         * The executor use to call application
         * hooks. If this is set, threadPoolSize is ignored.
         *
         * @param executorArg
         * @return
         */
        public Builder tcpExecutor(
                final Executor executorArg) {
            this.tcpExecutor = executorArg;
            return this;
        }

        /**
         * Create and start a new Server instance.
         *
         * @return A Server
         */
        public DnsServer build() {

            if (serverMessageHandler == null && !advanced) {
                throw new IllegalStateException(
                        "serverMessageHandler must be set");
            }

            if (advancedServerMessageHandler == null && advanced) {
                throw new IllegalStateException(
                        "advancedServerMessageHandler must be set");
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

        final Executor udpExecutor =
                builder.udpExecutor == null ? new OrderedMemoryAwareThreadPoolExecutor(
                        builder.udpThreadPoolSize, 0, 0) : builder.udpExecutor;
        builder.udpExecutor = udpExecutor;

        final Executor tcpExecutor =
                builder.tcpExecutor == null ? new OrderedMemoryAwareThreadPoolExecutor(
                        builder.tcpThreadPoolSize, 0, 0) : builder.tcpExecutor;
        builder.tcpExecutor = tcpExecutor;

        // Start listening for UDP request
        udpChannelFactory.set(builder.nioDatagramChannelFactory);
        ConnectionlessBootstrap udpBootstrap =
                new ConnectionlessBootstrap(udpChannelFactory.get());
        udpBootstrap.setOptions(builder.udpOptions);
        udpBootstrap.setPipelineFactory(getUdpChannelPipelineFactory(builder));
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
        tcpBootstrap.setPipelineFactory(getTcpChannelPipelineFactory(builder));
        LOGGER.info("Binding to {}", builder.port);
        Channel tcpChannel = tcpBootstrap.bind(
                new InetSocketAddress(this.boundTo.get().getPort()));
        channelGroup.add(tcpChannel);

        // let clients know what we are up to
        LOGGER.info("Bound to {}", udpChannel.getLocalAddress());
    }

    /**
     * This method returns the UDP channel pipeline factory.
     *
     * @param builder The DNS server builder.
     *
     * @return The TCP channel pipeline factory.
     */
    private ChannelPipelineFactory getUdpChannelPipelineFactory(
            final Builder builder) {
        return new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() {
                if (builder.logging) {
                    return Channels.pipeline(
                            new LoggingHandler(),
                            new DNSMessageDecoder(),
                            new DNSMessageEncoder(),
                            new ExecutionHandler(builder.udpExecutor),
                            getUdpServerHandler(builder));
                } else {
                    return Channels.pipeline(
                            new DNSMessageDecoder(),
                            new DNSMessageEncoder(),
                            new ExecutionHandler(builder.udpExecutor),
                            getUdpServerHandler(builder));
                }
            }
        };
    }

    /**
     * This method returns the UDP handler for server.
     *
     * @param builder The DNS server builder.
     *
     * @return The UDP server handler.
     */
    private SimpleChannelUpstreamHandler getUdpServerHandler(
            final Builder builder) {
        return builder.advanced
                ? new AdvancedServerMessageHandlerUDPInvoker(
                        builder.advancedServerMessageHandler)
                : new ServerMessageHandlerUDPInvoker(
                        builder.serverMessageHandler);

    }

    /**
     * This method returns the TCP channel pipeline factory.
     *
     * @param builder The DNS server builder.
     *
     * @return The TCP channel pipeline factory.
     */
    private ChannelPipelineFactory getTcpChannelPipelineFactory(
            final Builder builder) {
        return new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() {
                if (builder.logging) {
                    return Channels.pipeline(new LoggingHandler(),
                            new TCPDecoder(), new TCPEncoder(),
                            new DNSMessageDecoder(),
                            new DNSMessageEncoder(),
                            new ExecutionHandler(builder.tcpExecutor),
                            getTcpServerHandler(builder),
                            new IdleStateHandler(timer, 0, 0, 10),
                            new TcpIdleChannelHandler());
                } else {
                    return Channels.pipeline(
                            new TCPDecoder(), new TCPEncoder(),
                            new DNSMessageDecoder(),
                            new DNSMessageEncoder(),
                            new ExecutionHandler(builder.tcpExecutor),
                            getTcpServerHandler(builder),
                            new IdleStateHandler(timer, 0, 0, 10),
                            new TcpIdleChannelHandler());
                }
            }
        };
    }

    /**
     * This method returns the TCP handler for server.
     *
     * @param builder The DNS server builder.
     *
     * @return The TCP server handler.
     */
    private SimpleChannelUpstreamHandler getTcpServerHandler(
            final Builder builder) {
        return builder.advanced
                ? new AdvancedServerMessageHandlerTCPInvoker(
                        builder.advancedServerMessageHandler)
                : new ServerMessageHandlerTCPInvoker(
                        builder.serverMessageHandler);

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

