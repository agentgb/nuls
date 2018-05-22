package io.nuls.network.connection.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import io.nuls.network.model.Node;
import io.nuls.network.manager.NodeManager;

import static io.nuls.network.constant.NetworkConstant.CONNETCI_TIME_OUT;


public class NettyClient {

    public static EventLoopGroup worker = new NioEventLoopGroup();

    Bootstrap boot;

    private SocketChannel socketChannel;

    private Node node;

    private NodeManager nodeManager = NodeManager.getInstance();

    public NettyClient(Node node) {
        this.node = node;
        boot = new Bootstrap();

        AttributeKey<Node> key = null;
        synchronized (NettyClient.class) {
            if (AttributeKey.exists("node")) {
                key = AttributeKey.valueOf("node");
            } else {
                key = AttributeKey.newInstance("node");
            }
        }
        boot.attr(key, node);

        boot.group(worker)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNETCI_TIME_OUT)
                .handler(new NulsChannelInitializer<>(new ClientChannelHandler()));
    }

    public void start() {
        try {
            ChannelFuture future = boot.connect(node.getIp(), node.getSeverPort()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        socketChannel = (SocketChannel) future.channel();
                    } else {
                        System.out.println("Client connect to host fail: " + future.cause() + ", remove node: " + node.getId());
                        nodeManager.removeNode(node.getId());
                    }
                }
            });
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            //maybe time out or refused or something
            if (socketChannel != null) {
                socketChannel.close();
            }
            System.out.println("Client start exception:" + e.getMessage() + ", remove node: " + node.getId());
            nodeManager.removeNode(node.getId());
        }
    }

}
