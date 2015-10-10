package com.server;

/**
 * Server Main
 */
public class ServerLauncher {

    public static void main(String[] args) {
        String host = IrcServer.DEFAULT_HOST;
        int port = IrcServer.DEFAULT_PORT;
        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        try {
            IrcServer server = new IrcServer(host, port);
            server.start();
        } catch (Exception e) {
            System.err.println("Something bad happened when trying to start server");
            e.printStackTrace();
        }
    }

}
