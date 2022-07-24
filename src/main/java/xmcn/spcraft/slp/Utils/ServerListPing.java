package xmcn.spcraft.slp.Utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author  XiaMoHuaHuo_CN
 * @author  zh32 <zh32 at zh32.de>
 * @author  猫车车
 */
public class ServerListPing {

    private InetSocketAddress host;
    private int timeout = 7000;
    public void setAddress(InetSocketAddress host) {
        this.host = host;
    }

    public InetSocketAddress getAddress() {
        return host;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }

    public void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }

            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }

    public void fetchData() throws IOException {

        Socket socket = new Socket();
        OutputStream outputStream;
        DataOutputStream dataOutputStream;
        InputStream inputStream;
        InputStreamReader inputStreamReader;

        socket.setSoTimeout(this.timeout);

        socket.connect(host, timeout);

        outputStream = socket.getOutputStream();
        dataOutputStream = new DataOutputStream(outputStream);

        inputStream = socket.getInputStream();
        inputStreamReader = new InputStreamReader(inputStream);

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(b);
        handshake.writeByte(0x00); //packet id for handshake
        writeVarInt(handshake, 4); //protocol version
        writeVarInt(handshake, host.getHostString().length()); //host length
        handshake.writeBytes(host.getHostString()); //host string
        handshake.writeShort(host.getPort()); //port
        writeVarInt(handshake, 1); //state (1 for handshake)

        writeVarInt(dataOutputStream, b.size()); //prepend size
        dataOutputStream.write(b.toByteArray()); //write handshake packet


        dataOutputStream.writeByte(0x01); //size is only 1
        dataOutputStream.writeByte(0x00); //packet id for ping
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int size = readVarInt(dataInputStream); //size of packet
        int id = readVarInt(dataInputStream); //packet id

        if (id == -1) {
            throw new IOException("Premature end of stream.");
        }

        if (id != 0x00) { //we want a status response
            throw new IOException("Invalid packetID");
        }
        int length = readVarInt(dataInputStream); //length of json string

        if (length == -1) {
            throw new IOException("Premature end of stream.");
        }

        if (length == 0) {
            throw new IOException("Invalid string length.");
        }

        byte[] in = new byte[length];
        dataInputStream.readFully(in);  //read json string
        String json = new String(in);


        long now = System.currentTimeMillis();
        dataOutputStream.writeByte(0x09); //size of packet
        dataOutputStream.writeByte(0x01); //0x01 for ping
        dataOutputStream.writeLong(now); //time!?

        readVarInt(dataInputStream);
        id = readVarInt(dataInputStream);
        if (id == -1) {
            throw new IOException("Premature end of stream.");
        }

        if (id != 0x01) {
            throw new IOException("Invalid packetID");
        }
        //read response

        JSONObject object = JSON.parseObject(json);
        JSONObject players = JSON.parseObject(object.get("players").toString());
        JSONObject description = JSON.parseObject(object.get("description").toString());
        JSONObject version = JSON.parseObject(object.get("version").toString());
        List<Object> list = new ArrayList<Object>(JSON.parseArray(String.valueOf(description.get("extra"))));

        StringBuilder Motd = new StringBuilder();
        for (Object o : list) {
            JSONObject text = JSON.parseObject(o.toString());
            Motd.append(text.get("text"));
        }

        if (object.get("favicon").toString() != null) {
            ServerListPing.Response.setIcon(object.get("favicon").toString());
        } else {
            ServerListPing.Response.setIcon("");
        }

        ServerListPing.Response.setPlayerMax(players.get("max").toString());
        ServerListPing.Response.setPlayerOnline(players.get("online").toString());
        ServerListPing.Response.setProtocol(version.get("protocol").toString());
        ServerListPing.Response.setVersion(version.get("name").toString());
        ServerListPing.Response.setMOTD(Motd.toString());

        dataOutputStream.close();
        outputStream.close();
        inputStreamReader.close();
        inputStream.close();
        socket.close();
    }

    /**
     * 获取请求结果
     */
    public static class Response {

        private static String player_max;
        private static String protocol;
        private static String player_online;
        private static String version;
        private static String motd;
        private static String favicon;
        public static String getPlayerMax() {
            return Response.player_max;
        }

        public static String getPlayerOnline() {
            return Response.player_online;
        }

        public static String getProtocol() {
            return Response.protocol;
        }

        public static String getVersion() {
            return Response.version;
        }

        public static String getMOTD() {
            return Response.motd;
        }

        public static String getIcon() {
            return favicon;
        }

        // 内部变量 设置结果
        private static void setPlayerMax(String player_max) {
            Response.player_max = player_max;
        }

        private static void setPlayerOnline(String player_online) {
            Response.player_online = player_online;
        }

        private static void setProtocol(String protocol) {
            Response.protocol = protocol;
        }

        private static void setVersion(String version) {
            Response.version = version;
        }

        private static void setMOTD(String motd) {
            Response.motd = motd;
        }

        private static void setIcon(String favicon) {
            Response.favicon = favicon;
        }

    }

}