package com.raeden.ors_to_do.modules.dependencies.services;

import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SingleInstanceManager {

    /**
     * Attempts to register this application instance for the specific version.
     * Returns true if successful. Returns false if an instance of this version is already running.
     */
    public static boolean registerInstance(String version, Stage mainStage) {
        int versionPort = generatePortForVersion(version);

        try {
            // Attempt to bind to the version-specific port
            ServerSocket serverSocket = new ServerSocket(versionPort, 1, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));

            // If successful, start a background thread to listen for duplicate launch attempts
            Thread listenerThread = new Thread(() -> {
                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                        String msg = in.readLine();
                        if ("FOCUS".equals(msg)) {
                            // Another instance of THIS version tried to launch. The plain
                            // Platform.runLater(() -> mainStage.show(); toFront()) sequence is
                            // unreliable on Windows: the "foreground lock" lets the call succeed
                            // silently while leaving the window buried. WindowRestorer applies the
                            // alwaysOnTop-flip trick that actually works.
                            WindowRestorer.surface(mainStage);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();
            return true;

        } catch (Exception e) {
            // Port is already taken. An instance of this exact version is already running.
            focusExistingInstance(versionPort);
            return false;
        }
    }

    /**
     * Converts the version string (e.g., "v1.39") into a unique, consistent port number.
     */
    private static int generatePortForVersion(String version) {
        int hash = Math.abs(version.hashCode());
        // Map the hash to a port in the safe private range of 40000 - 60000
        return 40000 + (hash % 20000);
    }

    /**
     * Sends a signal to the currently running instance to bring itself to the front.
     */
    private static void focusExistingInstance(int port) {
        try (Socket socket = new Socket(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("FOCUS");
        } catch (Exception e) {
            // Ignore, the existing instance might be closing
        }
    }
}