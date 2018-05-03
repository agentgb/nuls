package io.nuls.protocol.base.utils;

import io.nuls.account.entity.Address;
import io.nuls.consensus.poc.protocol.model.block.BlockRoundData;
import io.nuls.core.exception.NulsException;
import io.nuls.db.entity.BlockHeaderPo;
import io.nuls.protocol.model.BlockHeader;
import io.nuls.protocol.model.NulsDigestData;
import io.nuls.protocol.script.P2PKHScriptSig;
import io.nuls.protocol.utils.io.NulsByteBuffer;

/**
 * @author: Niels Wang
 * @date: 2018/4/19
 */
public class BlockHeaderTool {

    public static final BlockHeaderPo toPojo(BlockHeader header) {
        BlockHeaderPo po = new BlockHeaderPo();
        po.setTxCount(header.getTxCount());
        po.setPreHash(header.getPreHash().getDigestHex());
        po.setMerkleHash(header.getMerkleHash().getDigestHex());
        po.setHeight(header.getHeight());
        po.setCreateTime(header.getTime());
        po.setHash(header.getHash().getDigestHex());
        po.setSize(header.getSize());
        if (null != header.getScriptSig()) {
            po.setScriptSig(header.getScriptSig().serialize());
        }
        po.setTxCount(header.getTxCount());
        po.setConsensusAddress(Address.fromHashs(header.getPackingAddress()).getBase58());
        po.setExtend(header.getExtend());
        BlockRoundData data = new BlockRoundData();
        data.parse(header.getExtend());
        po.setRoundIndex(data.getRoundIndex());
        return po;
    }


    public static final BlockHeader fromPojo(BlockHeaderPo po) throws NulsException {
        if (null == po) {
            return null;
        }
        BlockHeader header = new BlockHeader();
        header.setHash(NulsDigestData.fromDigestHex(po.getHash()));
        header.setMerkleHash(NulsDigestData.fromDigestHex(po.getMerkleHash()));
        header.setPackingAddress(Address.fromHashs(po.getConsensusAddress()).getHash());
        header.setTxCount(po.getTxCount());
        header.setPreHash(NulsDigestData.fromDigestHex(po.getPreHash()));
        header.setTime(po.getCreateTime());
        header.setHeight(po.getHeight());
        header.setExtend(po.getExtend());
        header.setSize(po.getSize());
        header.setScriptSig((new NulsByteBuffer(po.getScriptSig()).readNulsData(new P2PKHScriptSig())));
        return header;
    }
}
