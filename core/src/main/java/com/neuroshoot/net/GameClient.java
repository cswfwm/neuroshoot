package com.neuroshoot.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/** 客户端：连上主机，上报输入，接收快照。 */
public final class GameClient {
    private DatagramSocket socket;
    private InetAddress host;
    private int hostPort;
    private volatile String latest = "";
    private volatile boolean running;
    private Thread thread;

    public boolean connect(String hostIp) {
        try {
            host = InetAddress.getByName(hostIp);
            hostPort = GameHost.PORT;
            socket = new DatagramSocket();
            socket.setSoTimeout(800);
            socket.send(packet("J|"));
            running = true;
            thread = new Thread(this::loop, "neuroshoot-client");
            thread.setDaemon(true);
            thread.start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
    }

    private DatagramPacket packet(String text) {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(b, b.length, host, hostPort);
    }

    public void sendInput(String payload) {
        try {
            if (socket != null) socket.send(packet("I|" + payload));
        } catch (IOException ignored) {
            // 主机不在线时忽略
        }
    }

    public String latestSnapshot() {
        return latest;
    }

    private void loop() {
        byte[] buf = new byte[2048];
        while (running) {
            try {
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                socket.receive(in);
                String text = new String(in.getData(), 0, in.getLength(), StandardCharsets.UTF_8);
                if (text.startsWith("S|")) latest = text.substring(2);
            } catch (IOException e) {
                if (!running) return;
            }
        }
    }
}
