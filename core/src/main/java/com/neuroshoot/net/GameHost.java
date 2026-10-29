package com.neuroshoot.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * 主机权威同步：主机跑完整模拟，按固定频率把「谁在哪、朝哪、多少血」快照发给客户端。
 * 快照格式 {@code S|序号|x,y,angle,hp|...}；输入上报 {@code I|移动|角度|开火}。
 */
public final class GameHost {
    public static final int PORT = 42902;
    private DatagramSocket socket;
    private InetAddress client;
    private int clientPort;
    private volatile boolean running;
    private volatile String lastInput = "";
    private Thread thread;

    public boolean start() {
        try {
            socket = new DatagramSocket(PORT);
            socket.setSoTimeout(400);
        } catch (IOException e) {
            return false;
        }
        running = true;
        thread = new Thread(this::loop, "neuroshoot-host");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
    }

    public boolean hasClient() {
        return client != null;
    }

    private void loop() {
        byte[] buf = new byte[2048];
        while (running) {
            try {
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                socket.receive(in);
                String text = new String(in.getData(), 0, in.getLength(), StandardCharsets.UTF_8);
                if (text.startsWith("J|")) {
                    client = in.getAddress();
                    clientPort = in.getPort();
                    send("W|1");
                } else if (text.startsWith("I|")) {
                    lastInput = text.substring(2);
                }
            } catch (SocketTimeoutException ignored) {
                // 空转等待
            } catch (IOException e) {
                if (!running) return;
            }
        }
    }

    public String takeInput() {
        String s = lastInput;
        lastInput = "";
        return s;
    }

    public void sendSnapshot(String payload) {
        send("S|" + payload);
    }

    private void send(String text) {
        if (client == null) return;
        try {
            byte[] out = text.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(out, out.length, client, clientPort));
        } catch (IOException ignored) {
            // 对端掉线由上层超时处理
        }
    }
}
