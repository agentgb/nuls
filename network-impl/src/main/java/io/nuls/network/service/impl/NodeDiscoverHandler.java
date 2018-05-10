/**
 * MIT License
 * *
 * Copyright (c) 2017-2018 nuls.io
 * *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.network.service.impl;

import io.nuls.core.constant.NulsConstant;
import io.nuls.core.thread.manager.TaskManager;
import io.nuls.core.utils.date.DateUtil;
import io.nuls.core.utils.date.TimeService;
import io.nuls.core.utils.log.Log;
import io.nuls.db.dao.NodeDataService;
import io.nuls.db.entity.NodePo;
import io.nuls.network.NetworkContext;
import io.nuls.network.entity.Node;
import io.nuls.network.entity.NodeTransferTool;
import io.nuls.network.entity.param.NetworkParam;
import io.nuls.network.message.entity.GetNodeEvent;
import io.nuls.network.message.entity.GetNodesIpEvent;
import io.nuls.network.message.entity.GetVersionEvent;
import io.nuls.protocol.context.NulsContext;
import io.nuls.protocol.model.Block;

import java.util.*;

/**
 * @author vivi
 * @date 2017/11/21
 */
public class NodeDiscoverHandler implements Runnable {

    private NetworkParam network;

    private NodesManager nodesManager;

    private NodeDataService nodeDao;

    private BroadcastHandler broadcaster;

    private boolean running;

    private NodeDiscoverHandler() {

    }

    private static NodeDiscoverHandler instance = new NodeDiscoverHandler();

    public static NodeDiscoverHandler getInstance() {
        return instance;
    }

    public void start() {
        running = true;
        TaskManager.createAndRunThread(NulsConstant.MODULE_ID_NETWORK, "NetworkNodeDiscover", this);
    }

//    public List<Node> getLocalNodes() {
//        Set<String> ipList = new HashSet<>();
//        for (Node node : nodesManager.getNodes().values()) {
//            ipList.add(node.getIp());
//        }
//        List<NodePo> nodePos = getNodeDao().getNodePoList(size, ipList);
//
//        List<Node> nodes = new ArrayList<>();
//        if (nodePos == null || nodePos.isEmpty()) {
//            return nodes;
//        }
//    }

    // get nodes from local database
    public List<Node> getLocalNodes(int size, Set<String> ipSet) {
        List<NodePo> nodePos = getNodeDao().getNodePoList(size, ipSet);
        List<Node> nodes = new ArrayList<>();
        if (nodePos == null || nodePos.isEmpty()) {
            return nodes;
        }
        for (NodePo po : nodePos) {
            Node node = new Node();
            NodeTransferTool.toNode(node, po);
            node.setType(Node.OUT);
            node.setMagicNumber(network.getPacketMagic());
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * Inquire more of the other nodes to the connected nodes
     *
     * @param size
     */
    public void findOtherNode(int size) {
//        GetNodeEvent event = new GetNodeEvent(size);
//        List<Node> nodeList = new ArrayList<>(nodesManager.getAvailableNodes());
//        Collections.shuffle(nodeList);
//        for (int i = 0; i < nodeList.size(); i++) {
//            if (i == 2) {
//                break;
//            }
//            Node node = nodeList.get(i);
//            broadcaster.broadcastToNode(event, node, true);
//        }
    }

    /**
     * do ping/pong and ask versionMessage
     */
    private static int count = 0;

    @Override
    public void run() {
//        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
//
//        while (running) {
//            count++;
//            Collection<Node> nodeList = nodesManager.getAvailableNodes();
//            Block block = NulsContext.getInstance().getBestBlock();
//            GetVersionEvent event = new GetVersionEvent(network.getPort(), block.getHeader().getHeight(), block.getHeader().getHash().getDigestHex());
//            GetNodesIpEvent ipEvent = new GetNodesIpEvent();
//            for (Node node : nodeList) {
//                if (node.getType() == Node.OUT) {
//                    broadcaster.broadcastToNode(event, node, true);
//                }
//                if (count == 3) {
//                    broadcaster.broadcastToNode(ipEvent, node, true);
//                }
//            }
//
//            long now = TimeService.currentTimeMillis();
//            if (count == 3) {
//                count = 0;
//                List<String> list = new ArrayList<>();
//                for (Map.Entry<String, Long> entry : NetworkContext.ipMap.entrySet()) {
//                    if (now - entry.getValue() > DateUtil.MINUTE_TIME * 2) {
//                        list.add(entry.getKey());
//                    }
//                }
//                for (String ip : list) {
//                    NetworkContext.ipMap.remove(ip);
//                }
//            }
//
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                Log.error(e);
//            }
//        }
    }

    public void setNetwork(NetworkParam network) {
        this.network = network;
    }

    public void setNodesManager(NodesManager nodesManager) {
        this.nodesManager = nodesManager;
    }

    public void setBroadcaster(BroadcastHandler broadcaster) {
        this.broadcaster = broadcaster;
    }

    private NodeDataService getNodeDao() {
        if (nodeDao == null) {
            nodeDao = NulsContext.getServiceBean(NodeDataService.class);
        }
        return nodeDao;
    }
}
