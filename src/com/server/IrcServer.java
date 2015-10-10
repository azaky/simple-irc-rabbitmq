package com.server;

import com.rabbitmq.client.*;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IrcServer {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5672;

    private static final String QUEUE_NAME = "q_nicknames";
    private static final Pattern REQUEST_PATTERN = Pattern.compile("(.*):(.*)");

    private final Set<String> usernames = new HashSet<>();
    private Connection connection;
    private boolean connectionEstablished = false;
    private ConnectionFactory factory = new ConnectionFactory();
    private Channel mainChannel;

    public IrcServer(String host, int port) {
        factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
    }

    void setupConnection() throws IOException, TimeoutException {
        connection = factory.newConnection();
        connectionEstablished = true;
    }

    void start() throws IOException, TimeoutException {
        if (!connectionEstablished) {
            setupConnection();
        }

        mainChannel = connection.createChannel();
        mainChannel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        // allowing request for new nickname
        usernames.add("");
        Consumer consumer = new DefaultConsumer(mainChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println(" [ ] Received '" + message + "'");
                Matcher matcher = REQUEST_PATTERN.matcher(message);
                if (!matcher.matches()) {
                    System.out.println(" [x] message was invalid request");
                } else {
                    String username = matcher.group(1);
                    String callbackQueueName = matcher.group(2);
                    if (callbackQueueName.isEmpty()) { // we want to delete username
                        usernames.remove(username);
                    } else {
                        while (usernames.contains(username)) {
                            username = RandomStringUtils.randomAlphanumeric(5);
                        }
                        try {
                            sendMessageToQueue(username, callbackQueueName);
                            usernames.add(username);
                        } catch (Exception e) {
                            System.out.printf("Failed to send message to %s\n", callbackQueueName);
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        mainChannel.basicConsume(QUEUE_NAME, true, consumer);
    }

    private void sendMessageToQueue(String message, String queueName) throws IOException, TimeoutException {
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, false, false, false, null);
        channel.basicPublish("", queueName, null, message.getBytes("UTF-8"));
        channel.close();
    }

    public void terminate() throws IOException, TimeoutException {
        mainChannel.close();
        connection.close();
    }

}
