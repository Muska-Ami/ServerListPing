package moe.xmcn.spcraft.slp.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author  XiaMoHuaHuo_CN
 */
public class FastFunction {

    /**
     * 快速获取信息
     *
     * @param host 服务器地址
     * @param port 服务器端口
     * @return 快速信息
     * @throws IOException IO异常
     */
    public static String FastInfo(String host, int port) throws IOException {
        ServerListPing slp = new ServerListPing();
        InetSocketAddress address = new InetSocketAddress(host, port);
        slp.setAddress(address);
        slp.setTimeout(5000);
        slp.fetchData();

        return "版本: " + ServerListPing.Response.getVersion() + "\n" +
                "协议: " + ServerListPing.Response.getProtocol() + "\n" +
                "玩家: " + ServerListPing.Response.getPlayerOnline() + "/" + ServerListPing.Response.getPlayerMax() + "\n" +
                "MOTD: " + ServerListPing.Response.getMOTD() + "\n" +
                "图标：" + ServerListPing.Response.getIcon() + "\n" +
                "地址：" + slp.getAddress().getHostString() + ':' + slp.getAddress().getPort();
    }

}
