package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
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
        private static final int UDP_TIMEOUT = 20;

        /**
         * The handler for client message.
         */
        private ClientMessageHandler clientMessageHandler;

        /**
         * The number of threads in thread pool.
         */
        private int threadPoolSize = DEFAULT_POOL_SIZE;

        /**
         * Execturor to make sure the events from the same Channel are executed
         * sequentially.
         */
        private OrderedMemoryAwareThreadPoolExecutor omaThreadPoolExecutor;

        /**
         * Factory to create a client-side NIO-based SocketChannel. It utilizes
         * the non-blocking I/O mode.
         */
        private NioClientSocketChannelFactory nioCSChannelFactory =
                new NioClientSocketChannelFactory();

        /**
         * Factory to create NIO-based datagram channel and a
         * Executors.newCachedThreadPool().
         */
        private NioDatagramChannelFactory nioDChannelFactory =
                new NioDatagramChannelFactory();

        /**
         * Options to set for channels.
         */
        private Map<String, Object> options = new HashMap<String, Object>();

        /**
         * Flag to close connection after receiving message.
         */
        private Boolean closeConnectionOnMessageReceipt = Boolean.FALSE;

        /**
         * Timeout for UDP receive.
         */
        private int udpTimeoutSeconds = UDP_TIMEOUT;

        /**
         * The UDP client pipeline.
         */
        private final ChannelPipeline udpChannelPipeline = Channels.pipeline();

        /**
         * The TCP client pipeline.
         */
        private final ChannelPipeline tcpChannelPipeline = Channels.pipeline();

        /**
         * The build constructor.
         */
        private Builder() {
        }

        public static Builder instance() {
            return new Builder();
        }
        /**
         * Set the application processing thread pool size.
         *
         * @param threadPoolSizeArg The size of thread pool to set.
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
         * @param omaThreadPoolExecutorArg The thread pool executor to set.
         */
        public Builder orderedMemoryAwareThreadPoolExecutor(
                final OrderedMemoryAwareThreadPoolExecutor
                omaThreadPoolExecutorArg) {
            this.omaThreadPoolExecutor = omaThreadPoolExecutorArg;
            return this;
        }

        /**
         * Register a client side message handler to be invoked when responses
         * are received from a server.
         */
        public Builder clientMessageHandler(
                final ClientMessageHandler clientMessageHandlerArg) {
            this.clientMessageHandler = clientMessageHandlerArg;
            return this;
        }

        /**
         * Set the TCP channel factory. Defaults to a
         * NioClientSocketChannelFactory with CacheThreadPool executors.
         */
        public Builder nioClientSocketChannelFactory(
                final NioClientSocketChannelFactory nioCSChannelFactoryArg) {
            this.nioCSChannelFactory = nioCSChannelFactoryArg;
            return this;
        }

        /**
         * Set the UDP channel factory. Defaults to a with a CacheThreadPool
         * executor
         */
        public Builder nioDatagramChannelFactory(
                final NioDatagramChannelFactory nioDChannelFactoryArg) {
            this.nioDChannelFactory = nioDChannelFactoryArg;
            return this;
        }

        /**
         * Set the connection options.
         */
        public Builder options(final Map<String, Object> optionsArg) {
            this.options = optionsArg;
            return this;
        }

        /**
         * How long to listen for a UDP response before giving up.
         */
        public Builder udpTimeoutSeconds(int udpTimeoutSecondsArg) {
            this.udpTimeoutSeconds = udpTimeoutSecondsArg;
            return this;
        }

        /**
         * Indicate if the connection should be closed after the response is
         * received. Default is false.
         */
        public Builder closeConnectionOnMessageReceipt(
                final Boolean closeConnectionOnMessageReceiptArg) {
            this.closeConnectionOnMessageReceipt =
                    closeConnectionOnMessageReceiptArg;
            return this;
        }

        /**
         * Obtain a new Client.
         */
        public DnsClient build() {

            // Gotta have a client handler, otherwise what's the point?
            if (clientMessageHandler == null) {
                throw new IllegalStateException("clientMessageHandler must be set");
            }

            // set up the application side thread pool
            OrderedMemoryAwareThreadPoolExecutor omaThreadPoolExecutorArg =
                    this.omaThreadPoolExecutor != null ? this.omaThreadPoolExecutor
                            : new OrderedMemoryAwareThreadPoolExecutor(threadPoolSize, 0, 0);
            // client handler invoker
            ClientMessageHandlerInvoker clientMessageHandlerInvoker = new ClientMessageHandlerInvoker(
                    clientMessageHandler, closeConnectionOnMessageReceipt);

            // build the pipeline
            udpChannelPipeline.addLast("Logger", new LoggingHandler());
            udpChannelPipeline.addLast("MessageDecoder",
                    new DNSMessageDecoder());
            udpChannelPipeline.addLast("MessageEncoder",
                    new DNSMessageEncoder());
            udpChannelPipeline.addLast("ApplicationThreadPool",
                    new ExecutionHandler(
                            omaThreadPoolExecutorArg));
            udpChannelPipeline.addLast("ClientMessageHandlerInvoker",
                    clientMessageHandlerInvoker);

            tcpChannelPipeline.addLast("Logger", new LoggingHandler());
            tcpChannelPipeline.addLast("TCPDecoder", new TCPDecoder());
            tcpChannelPipeline.addLast("TCPEncoder", new TCPEncoder());
            tcpChannelPipeline.addLast("MessageDecoder",
                    new DNSMessageDecoder());
            tcpChannelPipeline.addLast("MessageEncoder",
                    new DNSMessageEncoder());
            tcpChannelPipeline.addLast("ApplicationThreadPool",
                    new ExecutionHandler(omaThreadPoolExecutorArg));
            tcpChannelPipeline.addLast("ClientMessageHandlerInvoker",
                    clientMessageHandlerInvoker);

            return new DnsClient(this);
        }
    }

    private static final int MILLIS_PER_SECOND = 1000;

    /**
     * The logger.
     */
    static final Logger LOGGER = LoggerFactory.getLogger(DnsClient.class);

    /**
     * UDP close timeout.
     */
    private final int udpTimeoutSeconds;

    /**
     * The connection bootstrap.
     */
    private final ConnectionlessBootstrap udpBootstrap;

    /**
     * The client helper.
     */
    private final ClientBootstrap tcpBootstrap;

    /**
     * Obtain a new client builder.
     */
    public static Builder builder() {
        return Builder.instance();
    }

    /**
     * Map from servers to connection open futures.
     */
    private final
    ConcurrentHashMap<SocketAddress, ChannelFuture> openTcpConnections =
            new ConcurrentHashMap<SocketAddress, ChannelFuture>();

    /**
     * Construct a new Client.
     *
     * @param builder
     *            Which has the client configuration
     */
    public DnsClient(final Builder builder) {

        udpBootstrap = new ConnectionlessBootstrap(builder.nioDChannelFactory);
        udpBootstrap.setOptions(builder.options);
        udpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return builder.udpChannelPipeline;
            }
        });
        udpTimeoutSeconds = builder.udpTimeoutSeconds;

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

    public void sendUDP(final Message message,
            final SocketAddress destination) {

        Channel channel = udpBootstrap.bind(new InetSocketAddress(0));
        ChannelFuture write = channel.write(message, destination);
        write.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(
                    final ChannelFuture future) throws Exception {

                LOGGER.debug("Message sent {}", message);
            }
        });

        if (!channel.getCloseFuture().awaitUninterruptibly(
                udpTimeoutSeconds * MILLIS_PER_SECOND)) {
            LOGGER.error("Request timed out after {} seconds",
                    udpTimeoutSeconds);
            channel.close().awaitUninterruptibly();
        }
    }

    /**
     * Send a message via TCP asynchronously. This method returns prior to
     * completion of the request. Add a handler in the pipeline to process the
     * returned message.
     *
     * @param message
     *            The DNS message
     */
    public void sendTCP(final Message message,
            final SocketAddress destination) {

        connectTCP(destination).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {

                LOGGER.debug("operationComplete for channel {}", future.getChannel());
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
            toReturn.getChannel().getCloseFuture().addListener(
                    new ChannelFutureListener() {

                        @Override
                        public void operationComplete(
                                final ChannelFuture arg0) throws Exception {
                            openTcpConnections.remove(destination);
                        }
                    });
            ChannelFuture toUse = openTcpConnections.putIfAbsent(
                    destination, toReturn);
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

