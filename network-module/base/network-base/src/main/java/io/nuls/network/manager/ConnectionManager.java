package io.nuls.network.manager;

import io.nuls.core.tools.log.Log;
import io.nuls.kernel.context.NulsContext;
import io.nuls.kernel.thread.manager.TaskManager;
import io.nuls.message.bus.service.MessageBusService;
import io.nuls.network.constant.NetworkParam;
import io.nuls.network.connection.netty.NettyClient;
import io.nuls.network.connection.netty.NettyServer;
import io.nuls.network.constant.NetworkConstant;
import io.nuls.network.model.NetworkEventResult;
import io.nuls.network.model.Node;
import io.nuls.network.message.filter.MessageFilterChain;
import io.nuls.network.protocol.handler.BaseNetworkMeesageHandler;
import io.nuls.protocol.message.base.BaseMessage;
import io.nuls.protocol.message.base.MessageHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 网络连接服务器
 * P2P节点的连接，消息的接收都在服务器里处理
 */
public class ConnectionManager {

    private static ConnectionManager instance = new ConnectionManager();

    private ConnectionManager() {
    }

    public static ConnectionManager getInstance() {
        return instance;
    }

    private NetworkParam network = NetworkParam.getInstance();

    private NettyServer nettyServer;

    private NodeManager nodeManager;

    private BroadcastHandler broadcastHandler;

    private NetworkMessageHandlerFactory messageHandlerFactory = NetworkMessageHandlerFactory.getInstance();

    private MessageBusService messageBusService = NulsContext.getServiceBean(MessageBusService.class);

    /**
     * 初始化节点服务器
     */
    public void init() {
        nodeManager = NodeManager.getInstance();
        broadcastHandler = BroadcastHandler.getInstance();
        nettyServer = new NettyServer(network.getPort());
        nettyServer.init();
    }

    /**
     * 启动节点服务器
     */
    public void start() {
        TaskManager.createAndRunThread(NetworkConstant.NETWORK_MODULE_ID, "node connection", new Runnable() {
            @Override
            public void run() {
                try {
                    nettyServer.start();
                } catch (InterruptedException e) {
                    Log.error(e);
                }
            }
        }, false);
    }

    /**
     * 启动一个线程，尝试主动连接其他P2P节点
     * @param node
     */
    public void connectionNode(Node node) {
        TaskManager.createAndRunThread(NetworkConstant.NETWORK_MODULE_ID, "node connection", new Runnable() {
            @Override
            public void run() {
                node.setStatus(Node.WAIT);
                NettyClient client = new NettyClient(node);
                client.start();
            }
        }, true);
    }

    /**
     * 收到网络消息后，在这里过滤，
     * 有恶意节点时，直接断链
     * @param buffer
     * @param node
     */
    public void receiveMessage(ByteBuffer buffer, Node node) {
        List<BaseMessage> list;
        try {
            buffer.flip();
            if (!node.isAlive()) {
                buffer.clear();
                return;
            }
            //收到的信息可能是多条消息，需要依次反序列化
            list = new ArrayList<>();
            byte[] bytes = buffer.array();
            int offset = 0;
            while (offset < bytes.length - 1) {
                MessageHeader header = new MessageHeader();
                header.parse(bytes);
                BaseMessage message = getMessageBusService().getMessageInstance(header.getModuleId(), header.getMsgType()).getData();
                message.parse(bytes);
                list.add(message);
                offset = message.serialize().length;
                if (bytes.length > offset) {
                    byte[] subBytes = new byte[bytes.length - offset];
                    System.arraycopy(bytes, offset, subBytes, 0, subBytes.length);
                    bytes = subBytes;
                    offset = 0;
                }
            }

            //消息过滤，不合法的消息直接和节点断链
            for (BaseMessage message : list) {
                if (MessageFilterChain.getInstance().doFilter(message)) {
                    MessageHeader header = message.getHeader();

                    if (node.getMagicNumber() == 0) {
                        node.setMagicNumber(header.getMagicNumber());
                    }

                    processMessage(message, node);
                } else {
                    node.setStatus(Node.BAD);
//                    System.out.println("-------------------- receive message filter remove node ---------------------------");
                    nodeManager.removeNode(node.getId());
                }
            }
        } catch (Exception e) {
            Log.error("remoteAddress: " + node.getId());
            Log.error(e);
            return;
        } finally {
            buffer.clear();
        }
    }

    /**
     * 处理消息
     * 如果是网络模块的消息，直接交给网络消息处理器处理
     * 如果是其他消息则转到messageBusService里，交由其他模块处理
     * @param message
     * @param node
     */
    private void processMessage(BaseMessage message, Node node) {
        if (message == null) {
//            Log.error("---------------------message is null--------------------------------");
            return;
        }
        //System.out.println("-----------------=-=-=-=-=-=-=-==-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-------"+message.getClass());
        if (isNetworkMessage(message)) {
            if (node.getStatus() != Node.HANDSHAKE && !isHandShakeMessage(message)) {
                return;
            }
            // System.out.println( sdf.format(System.currentTimeMillis()) + "-----------processMessage------------node:" + node.getId() + "------------moduleId: " + event.getHeader().getModuleId() + "," + "eventType:" + event.getHeader().getEventType());
            asynExecute(message, node);
        } else {
            if (!node.isHandShake()) {
                return;
            }
            messageBusService.receiveMessage(message, node);
        }
    }

    /**
     * 新启线程异步处理网络模块信息
     * @param message
     * @param node
     */
    private void asynExecute(BaseMessage message, Node node) {
        BaseNetworkMeesageHandler handler = messageHandlerFactory.getHandler(message);
        TaskManager.asynExecuteRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    NetworkEventResult messageResult = handler.process(message, node);
                    processMessageResult(messageResult, node);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.error(e);
                }
            }

            @Override
            public String toString() {
                StringBuilder log = new StringBuilder();
                log.append("event: " + message.toString())
                        .append(", hash: " + message.getHash())
                        .append(", Node: " + node.toString());
                return log.toString();
            }
        });
    }

    /**
     * 处理完信息后，如果有需要返回的信息，则同步返回
     * @param messageResult
     * @param node
     * @throws IOException
     */
    public void processMessageResult(NetworkEventResult messageResult, Node node) throws IOException {
        if (node.getStatus() == Node.CLOSE) {
            return;
        }
        if (messageResult == null || !messageResult.isSuccess()) {
            return;
        }
        if (messageResult.getReplyMessage() != null) {
            broadcastHandler.broadcastToNode((BaseMessage) messageResult.getReplyMessage(), node, true);
        }
    }

    private boolean isNetworkMessage(BaseMessage message) {
        return message.getHeader().getModuleId() == NetworkConstant.NETWORK_MODULE_ID;
    }

    private boolean isHandShakeMessage(BaseMessage message) {
        if (message.getHeader().getMsgType() == NetworkConstant.NETWORK_HANDSHAKE) {
            return true;
        }
        return false;
    }

    public MessageBusService getMessageBusService() {
        if (messageBusService == null) {
            messageBusService = NulsContext.getServiceBean(MessageBusService.class);
        }
        return messageBusService;
    }
}
