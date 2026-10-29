package com.neuroshoot.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 局域网房间发现：主机在固定端口上应答探测包，客户端广播探测并收集应答。
 *
 * 用 UDP 是为了不依赖任何第三方库；端口选在 42900 附近减少冲突。
 * 部分安卓 ROM 限制广播，因此同时提供手动填 IP 的路径（见 LanScreen）。
 */
public final class Discovery {
    public static final int PORT = 42901;
    private static final String PROBE = "NEUROSHOOT_PROBE";
    private static final String REPLY = "NEUROSHOOT_ROOM:";

    public static final class Room {
        public final String host;
        public final int port;
        public final String name;
        public final int players;

        Room(String host, int port, String name, int players) {
            this.host = host;
            this.port = port;
            this.name = name;
            this.players = players;
        }

        public String label() {
            return name + "  " + host + "  " + players + "/8";
        }
    }

    /** 客户端：广播探测，收集 1.2 秒内的应答。 */
    public static List<Room> scan() {
        List<Room> rooms = new ArrayList<>();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(300);
            byte[] probe = PROBE.getBytes(StandardCharsets.UTF_8);
            DatagramPacket out = new DatagramPacket(probe, probe.length,
                    InetAddress.getByName("255.255.255.255"), PORT);
            socket.send(out);
            long until = System.currentTimeMillis() + 1200;
            byte[] buf = new byte[256];
            while (System.currentTimeMillis() < until) {
                try {
                    DatagramPacket in = new DatagramPacket(buf, buf.length);
                    socket.receive(in);
                    String text = new String(in.getData(), 0, in.getLength(),
                            StandardCharsets.UTF_8);
                    if (!text.startsWith(REPLY)) continue;
                    String[] parts = text.substring(REPLY.length()).split(",");
                    if (parts.length < 3) continue;
                    String host = in.getAddress().getHostAddress();
                    boolean dup = false;
                    for (Room r : rooms) if (r.host.equals(host)) dup = true;
                    if (!dup) {
                        rooms.add(new Room(host, Integer.parseInt(parts[2]), parts[0],
                                Integer.parseInt(parts[1])));
                    }
                } catch (SocketTimeoutException ignored) {
                    // 没收到就继续等到截止时间
                }
            }
        } catch (IOException e) {
            return rooms;
        }
        return rooms;
    }

    /** 主机：常驻应答线程。 */
    public static final class Responder implements Runnable {
        private volatile boolean running = true;
        private DatagramSocket socket;
        private final String roomName;

        public Responder(String roomName) {
            this.roomName = roomName;
        }

        public void stop() {
            running = false;
            if (socket != null) socket.close();
        }

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(PORT);
                socket.setBroadcast(true);
                socket.setSoTimeout(500);
                byte[] buf = new byte[64];
                while (running) {
                    try {
                        DatagramPacket in = new DatagramPacket(buf, buf.length);
                        socket.receive(in);
                        String text = new String(in.getData(), 0, in.getLength(),
                                StandardCharsets.UTF_8);
                        if (!PROBE.equals(text)) continue;
                        String reply = REPLY + roomName + ",1," + PORT;
                        byte[] out = reply.getBytes(StandardCharsets.UTF_8);
                        socket.send(new DatagramPacket(out, out.length,
                                in.getAddress(), in.getPort()));
                    } catch (SocketTimeoutException ignored) {
                        // 空闲等待
                    }
                }
            } catch (IOException ignored) {
                // 端口被占用时静默退出，界面会提示
            } finally {
                if (socket != null) socket.close();
            }
        }
    }

    private Discovery() {}
}
