package com.client;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Client consumer for a channel
 */
public class ChannelConsumer extends DefaultConsumer {

    private final String channelName;

    private final LinkedBlockingQueue queueMessage;

    public ChannelConsumer(Channel channel, String channelName, LinkedBlockingQueue queueMessage) {
        super(channel);
        this.channelName = channelName;
        this.queueMessage = queueMessage;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body) throws IOException {
        String message = new String(body, "UTF-8");
        queueMessage.add("["+channelName+"] " + message);
    }
}
