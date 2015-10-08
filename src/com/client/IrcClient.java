package com.client;

import com.rabbitmq.client.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class IrcClient {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5672;

    private static final String NICK_QUEUE_NAME = "q_nicknames";

    private String nickname;
    private final AtomicBoolean isTerminated = new AtomicBoolean(false);

    private Connection connection;
    private boolean connectionEstablished = false;
    private ConnectionFactory factory = new ConnectionFactory();

    public IrcClient() {
        factory = new ConnectionFactory();
        factory.setHost(DEFAULT_HOST);
        factory.setPort(DEFAULT_PORT);
    }

    public IrcClient(String host, int port) {
        factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
    }

    void setupConnection() throws IOException, TimeoutException {
        connection = factory.newConnection();
        connectionEstablished = true;
    }

    public void launch() throws IOException, TimeoutException {
        if (!connectionEstablished) {
            setupConnection();
        }
        Thread inputHandler = getInputHandler();
        inputHandler.start();
        try {
            inputHandler.join();
            connection.close();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void terminate() {
        isTerminated.set(true);
    }

    private void showMessage(String message) {
        System.out.println(message);
    }

    private Thread getInputHandler() {
        final Scanner scanner = new Scanner(System.in);
        return new Thread() {
            @Override
            public void run() {
                while (!isTerminated.get()) {
                    String line = scanner.nextLine();
                    handleInput(line);
                }
            }
        };
    }

    private void handleInput(String input) {
        List<String> groups;

        if ((groups = CommandRegexes.NICK.match(input)) != null) {
            handleNick(groups.get(0));
        } else if ((groups = CommandRegexes.JOIN.match(input)) != null) {
            String channelName = groups.get(0);
            handleJoin(channelName);
        } else if ((groups = CommandRegexes.LEAVE.match(input)) != null) {
            String channelName = groups.get(0);
            handleLeave(channelName);
        } else if (CommandRegexes.EXIT.match(input) != null) {
            handleExit();
        } else if ((groups = CommandRegexes.MESSAGE_CHANNEL.match(input)) != null) {
            String channelName = groups.get(0);
            String message = groups.get(1);
            handleMessageChannel(message, channelName);
        } else { // Assuming broadcast
            handleBroadcast(input);
        }
    }

    private void handleBroadcast(String message) {
        // TODO
        System.out.println("This is not implemented yet");
    }

    private void handleMessageChannel(String message, String channelName) {
        // TODO
        System.out.println("This is not implemented yet");
    }

    private void handleLeave(String channelName) {
        // TODO
        System.out.println("This is not implemented yet");
    }

    private void handleJoin(String channelName) {
        // TODO
        System.out.println("This is not implemented yet");
    }

    private void handleExit() {
        try {
            deleteNickname();
            showMessage("Bye bye!");
            terminate();
        } catch (Exception e) {
            System.err.println("Something bad happened");
            e.printStackTrace();
        }
    }

    private void handleNick(String requestedNickname) {
        try {
            // remove old nickname
            deleteNickname();

            String tempQueueName = RandomStringUtils.randomAlphanumeric(20);
            if (requestedNickname == null) {
                requestedNickname = "";
            }
            sendMessageToQueue(requestedNickname + ":" + tempQueueName, NICK_QUEUE_NAME);
            String returnedNickname = getMessageFromQueue(tempQueueName);
            if (requestedNickname.isEmpty()) {
                showMessage("You have been assigned as [" + returnedNickname + "]. Welcome!");
            } else if (!requestedNickname.equals(returnedNickname)) {
                showMessage("ERROR: nickname " + nickname + " has already taken");
                showMessage("You have been assigned as [" + returnedNickname + "] instead. Welcome!");
            } else {
                showMessage("Welcome [" + returnedNickname + "]!");
            }
            nickname = returnedNickname;
        } catch (Exception e) {
            System.err.println("Something bad happened");
            e.printStackTrace();
        }
    }

    private void deleteNickname() throws IOException, TimeoutException {
        if (nickname != null) {
            sendMessageToQueue(nickname + ":", NICK_QUEUE_NAME);
        }
    }

    private String getMessageFromQueue(String queueName) throws IOException, InterruptedException {
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, false, false, false, null);
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);
        QueueingConsumer.Delivery delivery = consumer.nextDelivery(3000);
        return new String(delivery.getBody(), "UTF-8");
    }

    private void sendMessageToQueue(String message, String queueName) throws IOException, TimeoutException {
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, false, false, false, null);
        channel.basicPublish("", queueName, null, message.getBytes("UTF-8"));
        channel.close();
    }
}
