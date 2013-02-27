package biz.neustar.hopper.nio;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.ClientMessageHandler;

public class MessageReceivedTrap implements ClientMessageHandler {

    private final static Logger log = LoggerFactory.getLogger(MessageReceivedTrap.class);
    final public CountDownLatch latch;
    final private AtomicInteger counter = new AtomicInteger();

    public MessageReceivedTrap(int count) {

        latch = new CountDownLatch(count);
    }

    @Override
    public void handleResponse(Message response) {

        log.debug("Received message {}", counter.getAndIncrement());
        latch.countDown();
    }

    @Override
    public void handleException(Throwable throwable) {

        log.debug("exceptionCaught {}", throwable);
    }

}
