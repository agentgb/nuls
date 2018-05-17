package io.nuls.network.module.impl;

import io.nuls.core.tools.network.IpUtil;
import io.nuls.kernel.cfg.NulsConfig;
import io.nuls.message.bus.manager.MessageManager;
import io.nuls.network.constant.NetworkConstant;
import io.nuls.network.constant.NetworkParam;
import io.nuls.network.manager.ConnectionManager;
import io.nuls.network.manager.NodeManager;
import io.nuls.network.message.filter.MessageFilterChain;
import io.nuls.network.message.filter.impl.MagicNumberFilter;
import io.nuls.network.module.AbstractNetworkModule;
import io.nuls.network.protocol.message.*;

import java.util.ArrayList;
import java.util.List;

import static io.nuls.network.constant.NetworkConstant.*;

/**
 * 用netty实现网络模块P2P通信
 *
 */
public class NettyNetworkModuleBootstrap extends AbstractNetworkModule {

    private ConnectionManager connectionManager = ConnectionManager.getInstance();

    private NodeManager nodeManager = NodeManager.getInstance();

    @Override
    public void init() {
        initNetworkParam();
        initOther();
        connectionManager.init();
        nodeManager.init();
    }

    /**
     * 读取配置文件里的网络模块参数
     * 包括服务器的端口号、网络通信魔法参数、主动连接最大数和被动连接最大数，种子节点信息等
     */
    private void initNetworkParam() {
        NetworkParam networkParam = NetworkParam.getInstance();
        networkParam.setPort(NulsConfig.MODULES_CONFIG.getCfgValue(NETWORK_SECTION, NETWORK_SERVER_PORT, 8003));
        networkParam.setPacketMagic(NulsConfig.MODULES_CONFIG.getCfgValue(NETWORK_SECTION, NETWORK_MAGIC, 123456789));
        networkParam.setMaxInCount(NulsConfig.MODULES_CONFIG.getCfgValue(NETWORK_SECTION, NETWORK_NODE_MAX_IN, 30));
        networkParam.setMaxOutCount(NulsConfig.MODULES_CONFIG.getCfgValue(NETWORK_SECTION, NETWORK_NODE_MAX_OUT, 10));
        networkParam.setLocalIps(IpUtil.getIps());
        String seedIp = NulsConfig.MODULES_CONFIG.getCfgValue(NetworkConstant.NETWORK_SECTION, NetworkConstant.NETWORK_SEED_IP, "192.168.1.131:8003");
        List<String> ipList = new ArrayList<>();
        for (String ip : seedIp.split(",")) {
            ipList.add(ip);
        }
        networkParam.setSeedIpList(ipList);
    }

    /**
     * 初始化并注册其他网络模块需要用到的服务
     */
    private void initOther() {
        MagicNumberFilter.getInstance().addMagicNum(NetworkParam.getInstance().getPacketMagic());
        MessageFilterChain.getInstance().addFilter(MagicNumberFilter.getInstance());
        MessageManager.putMessage(HandshakeMessage.class);
        MessageManager.putMessage(GetVersionMessage.class);
        MessageManager.putMessage(VersionMessage.class);
        MessageManager.putMessage(GetNodesMessage.class);
        MessageManager.putMessage(NodesMessage.class);
        MessageManager.putMessage(GetNodesIpMessage.class);
        MessageManager.putMessage(NodesIpMessage.class);
    }

    /**
     * 1.启动当前节点的服务，供其他节点发现并连接
     * 2.启动一个线程不停的询问更多可连接的节点，保证最大化连接，维持网络稳定运行
     */
    @Override
    public void start() {
        connectionManager.start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        nodeManager.start();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public String getInfo() {
        return null;
    }
}
