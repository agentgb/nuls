/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.ledger.entity;

import io.nuls.core.constant.NulsConstant;
import io.nuls.core.utils.date.TimeService;
import io.nuls.core.utils.str.StringUtils;
import io.nuls.protocol.model.BaseNulsData;
import io.nuls.protocol.model.NulsDigestData;
import io.nuls.protocol.script.P2PKHScript;

/**
 * Created by win10 on 2017/10/30.
 */
public class UtxoOutput extends BaseNulsData {

    private transient NulsDigestData txHash;

    private int index;

    private long value;

    private transient String address;

    private long lockTime;

    private P2PKHScript p2PKHScript;

    private transient OutPutStatusEnum status;

    /**
     * ------ redundancy ------
     */
    private transient long createTime;

    private transient int txType;

    // key = txHash + "-" + index, a key that will not be serialized, only used for caching
    private transient String key;


    public UtxoOutput() {
    }

    public UtxoOutput(NulsDigestData txHash) {
        this.txHash = txHash;
    }

    public NulsDigestData getTxHash() {
        return txHash;
    }

    public void setTxHash(NulsDigestData txHash) {
        this.txHash = txHash;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getLockTime() {
        return lockTime;
    }

    public void setLockTime(long lockTime) {
        this.lockTime = lockTime;
    }

    public P2PKHScript getP2PKHScript() {
        return p2PKHScript;
    }

    public void setP2PKHScript(P2PKHScript p2PKHScript) {
        this.p2PKHScript = p2PKHScript;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public OutPutStatusEnum getStatus() {
        return status;
    }

    public void setStatus(OutPutStatusEnum status) {
        this.status = status;
    }

    public String getKey() {
        if (StringUtils.isBlank(key)) {
            key = this.getTxHash().getDigestHex() + "-" + index;
        }
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getTxType() {
        return txType;
    }

    public void setTxType(int txType) {
        this.txType = txType;
    }

    public boolean isUsable(long height) {
        isLocked(height);
        return OutPutStatusEnum.UTXO_UNSPENT == status || null == status;
    }

    public boolean isSpend() {
        return OutPutStatusEnum.UTXO_SPENT == status;
    }

    public boolean isLocked(long height) {
        if (OutPutStatusEnum.UTXO_CONSENSUS_LOCK == status) {
            return true;
        }
        long currentTime = TimeService.currentTimeMillis();
        if (lockTime <= NulsConstant.BlOCKHEIGHT_TIME_DIVIDE && lockTime >= height) {
            status = OutPutStatusEnum.UTXO_TIME_LOCK;
            return true;
        } else if (lockTime > NulsConstant.BlOCKHEIGHT_TIME_DIVIDE && lockTime >= currentTime) {
            status = OutPutStatusEnum.UTXO_TIME_LOCK;
            return true;
        }
        status = OutPutStatusEnum.UTXO_UNSPENT;
        return false;
    }

    public byte[] getOwner() {
        return this.getP2PKHScript().getPublicKeyDigest().getDigestBytes();
    }

}
