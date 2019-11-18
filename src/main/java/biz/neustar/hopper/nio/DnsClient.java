package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.handler.AdvancedClientMessageHandlerInvoker;
import biz.neustar.hopper.nio.handler.ClientMessageHandlerInvoker;
import biz.neustar.hopper.nio.handler.DNSMessageDecoder;
import biz.neustar.hopper.nio.handler.DNSMessageEncoder;
import biz.neustar.hopper.nio.handler.TCPDecoder;
import biz.neustar.hopper.nio.handler.TCPEncoder;

/**
 * A client for the DNS protocol. This clien can sends DNS messages via TCP or
 * UDP.
 */
public class DnsClient {

    /**
     * A builder of DNS clients.
     */
    public static final class Builder {

        /**
         * The default size of thread pool.
         */
        private static final int DEFAULT_POOL_SIZE = 10;

        /**
         * The default UDP timeout.
         */
//        private static final int UDP_TIMEOUT = 20;

        /** 
         * The flag advanced.
         */
        private boolean advanced = false;

        /**
         * The handler for client message.
         */
        private ClientMessageHandler clientMessageHandler;

        /** The advanced handler for client message. */
        private AdvancedClientMessageHandler advancedClientMessageHandler;
        /**
         * The number of threads in thread pool.
         */
        private int threadPoolSize = DEFAULT_POOL_SIZE;

        /**
         * Keep the binary data logging off.
         */
        private boolean logging = false;

        /**
         * Execturor to make sure the events from the same Channel are executed
         * sequentially.
         */
        private Executor threadPoolExecutor;

        /**
         * Factory to create a client-side NIO-based SocketChannel. It utilizes
         * the non-blocking I/O mode.
         */
        private NioClientSocketChannelFactory nioCSChannelFactory = new NioClientSocketChannelFactory();

        /**
         * Factory to create NIO-based datagram channel and a
         * Executors.newCachedThreadPool().
         */
        private NioDatagramChannelFactory nioDChannelFactory = new NioDatagramChannelFactory();

        /**
         * Options to set for channels.
         */
        private Map<String, Object> options = new HashMap<>();

        /**
         * Flag to close connection after receiving message.
         */
        private Boolean closeConnectionOnMessageReceipt = Boolean.FALSE;

        /**
         * Timeout for UDP receive.
         */
//        private int udpTimeoutSeconds = UDP_TIMEOUT;

        /**
         * The UDP client pipeline.
         */
        private final ChannelPipeline udpChannelPipeline = Channels.pipeline();

        /**
         * The TCP client pipeline.
         */
        private final ChannelPipeline tcpChannelPipeline = Channels.pipeline();

        /**
         * UDP options.
         */
        private Map<String, Object> udpOptions = new HashMap<>();

        /**
         * Default UDP buffer size.
         */
        private static final int UDP_BUF_SIZE = 16777216;

        /**
         * The build constructor.
         */
        private Builder() {
            udpOptions.put("receiveBufferSize", UDP_BUF_SIZE);
            udpOptions.put("sendBufferSize", UDP_BUF_SIZE);
            udpOptions.put("broadcast", "true");
        }

        /**
         * Instance.
         * 
         * @return the builder
         */
        public static Builder instance() {
            return new Builder();
        }

        /**
         * Set the application processing thread pool size.
         * 
         * @param threadPoolSizeArg
         *            The size of thread pool to set.
         * 
         * @return The builder.
         */
        public Builder threadPoolSize(final int threadPoolSizeArg) {
            this.threadPoolSize = threadPoolSizeArg;
            return this;
        }

        /**
         * Set the application thread pool executor. threadPoolSize is ignored
         * when this is set.
         * 
         * @param threadPoolExecutorArg
         *            The thread pool executor to set.
         * @return the builder
         */
        public Builder threadPoolExecutor(
                final Executor threadPoolExecutorArg) {
            this.threadPoolExecutor = threadPoolExecutorArg;
            return this;
        }

        /**
         * Register a client side message handler to be invoked when responses
         * are received from a server.
         * 
         * @param clientMessageHandlerArg
         *            the client message handler arg
         * @return the builder
         */
        public Builder clientMessageHandler(
                final ClientMessageHandler clientMessageHandlerArg) {
            this.clientMessageHandler = clientMessageHandlerArg;
            return this;
        }

        /**
         * Register a client side message handler to be invoked when responses
         * are received from a server.
         * 
         * @param advClientMessageHandlerArg
         *            the adv client message handler arg
         * @return the builder
         */
        public Builder advancedClientMessageHandler(
                final AdvancedClientMessageHandler advClientMessageHandlerArg) {
            this.advancedClientMessageHandler = advClientMessageHandlerArg;
            return this;
        }

        /**
         * Set the TCP channel factory. Defaults to a
         * NioClientSocketChannelFactory with CacheThreadPool executors.
         * 
         * @param nioCSChannelFactoryArg
         *            the nio cs channel factory arg
         * @return the builder
         */
        public Builder nioClientSocketChannelFactory(
                final NioClientSocketChannelFactory nioCSChannelFactoryArg) {
            this.nioCSChannelFactory = nioCSChannelFactoryArg;
            return this;
        }

        /**
         * Set the UDP channel factory. Defaults to a with a CacheThreadPool
         * executor
         * 
         * @param nioDChannelFactoryArg
         *            the nio d channel factory arg
         * @return the builder
         */
        public Builder nioDatagramChannelFactory(
                final NioDatagramChannelFactory nioDChannelFactoryArg) {
            this.nioDChannelFactory = nioDChannelFactoryArg;
            return this;
        }

        /**
         * Set the connection options.
         * 
         * @param optionsArg
         *            the options arg
         * @return the builder
         */
        public Builder options(final Map<String, Object> optionsArg) {
            this.options = optionsArg;
            return this;
        }

        /**
         * Set the UDP options.
         * 
         * @param udpOptionsArg
         *            the options arg
         * @return the builder
         */
        public Builder udpOptions(final Map<String, Object> udpOptionsArg) {
            this.udpOptions = udpOptionsArg;
            return this;
        }

        /**
         * How long to listen for a UDP response before giving up.
         * 
         * @param udpTimeoutSecondsArg
         *            the udp timeout seconds arg
         * @return the builder
         */
//        public Builder udpTimeoutSeconds(int udpTimeoutSecondsArg) {
//            this.udpTimeoutSeconds = udpTimeoutSecondsArg;
//            return this;
//        }

        /**
         * Indicate if the connection should be closed after the response is
         * received. Default is false.
         * 
         * @param closeConnectionOnMessageReceiptArg
         *            the close connection on message receipt arg
         * @return the builder
         */
        public Builder closeConnectionOnMessageReceipt(
                final Boolean closeConnectionOnMessageReceiptArg) {
            this.closeConnectionOnMessageReceipt = closeConnectionOnMessageReceiptArg;
            return this;
        }

        /**
         * Logging.
         * 
         * @param logging
         *            the logging
         * @return the builder
         */
        public Builder logging(boolean logging) {
            this.logging = logging;
            return this;
        }

        /**
         * Advanced.
         * 
         * @param advancedArg
         *            the advanced arg
         * @return the builder
         */
        public Builder advanced(boolean advancedArg) {
            this.advanced = advancedArg;
            return this;
        }

        /**
         * Obtain a new Client.
         * 
         * @return the dns client
         */
        public DnsClient build() {

            // Gotta have a client handler, otherwise what's the point?
            if (clientMessageHandler == null && !advanced) {
                throw new IllegalStateException(
                        "clientMessageHandler must be set");
            }
            if (advancedClientMessageHandler == null && advanced) {
                throw new IllegalStateException(
                        "advanced clientMessageHandler must be set");
            }

            // set up the application side thread pool
            Executor omaThreadPoolExecutorArg = this.threadPoolExecutor != null ? this.threadPoolExecutor
                    : Executors.newFixedThreadPool(threadPoolSize);
            // client handler invoker
            ClientMessageHandlerInvoker clientMessageHandlerInvoker = new ClientMessageHandlerInvoker(
                    clientMessageHandler, closeConnectionOnMessageReceipt);

            AdvancedClientMessageHandlerInvoker advancedClientMessageHandlerInvoker = new AdvancedClientMessageHandlerInvoker(
                    advancedClientMessageHandler,
                    closeConnectionOnMessageReceipt);
            // build the pipeline
            if (logging) {
                udpChannelPipeline.addLast("Logger", new LoggingHandler());
            }
            udpChannelPipeline.addLast("MessageDecoder",
                    new DNSMessageDecoder());
            udpChannelPipeline.addLast("MessageEncoder",
                    new DNSMessageEncoder());
            udpChannelPipeline.addLast("ApplicationThreadPool",
                    new ExecutionHandler(omaThreadPoolExecutorArg));
            if (!advanced) {
                udpChannelPipeline.addLast("ClientMessageHandlerInvoker",
                        clientMessageHandlerInvoker);
            } else {
                udpChannelPipeline.addLast(
                        "AdvancedClientMessageHandlerInvoker",
                        advancedClientMessageHandlerInvoker);
            }

            if (logging) {
                tcpChannelPipeline.addLast("Logger", new LoggingHandler());
            }
            tcpChannelPipeline.addLast("TCPDecoder", new TCPDecoder());
            tcpChannelPipeline.addLast("TCPEncoder", new TCPEncoder());
            tcpChannelPipeline.addLast("MessageDecoder",
                    new DNSMessageDecoder());
            tcpChannelPipeline.addLast("MessageEncoder",
                    new DNSMessageEncoder());
            tcpChannelPipeline.addLast("ApplicationThreadPool",
                    new ExecutionHandler(omaThreadPoolExecutorArg));
            if (!advanced) {
                tcpChannelPipeline.addLast("ClientMessageHandlerInvoker",
                        clientMessageHandlerInvoker);
            } else {
                tcpChannelPipeline.addLast(
                        "AdvancedClientMessageHandlerInvoker",
                        advancedClientMessageHandlerInvoker);
            }
            return new DnsClient(this);
        }
    }

    /** The Constant MILLIS_PER_SECOND. */
//    private static final int MILLIS_PER_SECOND = 1000;

    /**
     * The logger.
     */
    static final Logger LOGGER = LoggerFactory.getLogger(DnsClient.class);

    /**
     * UDP close timeout.
     */
//    private final int udpTimeoutSeconds;

    /**
     * The connection bootstrap.
     */
    private final ConnectionlessBootstrap udpBootstrap;

    /**
     * The client helper.
     */
    private final ClientBootstrap tcpBootstrap;

    private Channel channel;

    /**
     * Obtain a new client builder.
     * 
     * @return the builder
     */
    public static Builder builder() {
        return Builder.instance();
    }

    /**
     * Map from servers to connection open futures.
     */
    private final ConcurrentHashMap<SocketAddress, ChannelFuture> openTcpConnections = new ConcurrentHashMap<>();

    /**
     * Construct a new Client.
     * 
     * @param builder
     *            Which has the client configuration
     */
    public DnsClient(final Builder builder) {

        udpBootstrap = new ConnectionlessBootstrap(builder.nioDChannelFactory);
        udpBootstrap.setOptions(builder.udpOptions);
        udpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return builder.udpChannelPipeline;
            }
        });
//        udpTimeoutSeconds = builder.udpTimeoutSeconds;
        channel = udpBootstrap.bind(new InetSocketAddress(0));

        // Configure the TCP client.
        tcpBootstrap = new ClientBootstrap(builder.nioCSChannelFactory);
        tcpBootstrap.setOptions(builder.options);
        tcpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() {
                return builder.tcpChannelPipeline;
            }
        });

    }

    /**
     * Send udp.
     * 
     * @param message
     *            the message
     * @param destination
     *            the destination
     */
    public void sendUDP(final Message message, final SocketAddress destination) {

//        Channel channel = udpBootstrap.bind(new InetSocketAddress(0));
        ChannelFuture write = channel.write(message, destination);
        write.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(final ChannelFuture future)
                    throws Exception {

                LOGGER.debug("Message sent {}", message);
            }
        });

//        if (!channel.getCloseFuture().awaitUninterruptibly(
//                udpTimeoutSeconds * MILLIS_PER_SECOND)) {
//            LOGGER.error("Request timed out after {} seconds",
//                    udpTimeoutSeconds);
//            channel.close().awaitUninterruptibly();
//        }
    }

    /**
     * Send a message via TCP asynchronously. This method returns prior to
     * completion of the request. Add a handler in the pipeline to process the
     * returned message.
     * 
     * @param message
     *            The DNS message
     * @param destination
     *            the destination
     */
    public void sendTCP(final Message message, final SocketAddress destination) {

        connectTCP(destination).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future)
                    throws Exception {

                LOGGER.debug("operationComplete for channel {}",
                        future.getChannel());
                if (future.isSuccess()) {
                    future.getChannel().write(message);
                } else {
                    LOGGER.error("Could not open connection to {}", destination);
                }
            }
        });

    }

    /**
     * Shutdown the client. Close open channel and release resources.
     */
    public void stop() {

        for (ChannelFuture channelFuture : openTcpConnections.values()) {
            try {
                channelFuture.getChannel().close().await();
            } catch (Exception e) {
                // no worries, we are shutting down
                LOGGER.warn("Shutting down the client!!!!!");
            }
        }
        tcpBootstrap.releaseExternalResources();
        channel.close();
        udpBootstrap.releaseExternalResources();
    }

    /**
     * Asynchronously open a connection or return an already open connection.
     * 
     * @param destination
     *            To which the connection should be opened
     * @return A ChannelFuture for the connection operation
     */
    protected ChannelFuture connectTCP(final SocketAddress destination) {

        ChannelFuture toReturn = openTcpConnections.get(destination);
        if (toReturn == null) {
            // open a connection
            toReturn = tcpBootstrap.connect(destination);
            LOGGER.debug("Opened {}", toReturn);
            toReturn.getChannel().getCloseFuture()
                    .addListener(new ChannelFutureListener() {

                        @Override
                        public void operationComplete(final ChannelFuture arg0)
                                throws Exception {
                            openTcpConnections.remove(destination);
                        }
                    });
            ChannelFuture toUse = openTcpConnections.putIfAbsent(destination,
                    toReturn);
            if (toUse != null) {
                // Already opened, discard this one and use the other one
                toReturn.getChannel().close();
                toReturn = toUse;
            }
        }
        LOGGER.debug("Returning channelFuture {}", toReturn);
        return toReturn;
    }
}
