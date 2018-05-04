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
package io.nuls.ledger.entity.tx;

import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.utils.date.TimeService;
import io.nuls.core.validate.NulsDataValidator;
import io.nuls.ledger.entity.CoinData;
import io.nuls.ledger.entity.params.CoinTransferData;
import io.nuls.ledger.entity.validator.CoinDataValidator;
import io.nuls.ledger.entity.validator.CoinTransactionValidatorManager;
import io.nuls.ledger.service.intf.CoinDataProvider;
import io.nuls.protocol.context.NulsContext;
import io.nuls.protocol.model.BaseNulsData;
import io.nuls.protocol.model.Transaction;
import io.nuls.protocol.utils.io.NulsByteBuffer;

import java.util.List;

/**
 * @author Niels
 * @date 2017/11/14
 */
public abstract class AbstractCoinTransaction<T extends BaseNulsData> extends Transaction<T> {

    protected static CoinDataProvider coinDataProvider;

    protected CoinData coinData;
    private boolean skipInputValidator;

    public AbstractCoinTransaction(int type) {
        super(type);
        List<NulsDataValidator> list = CoinTransactionValidatorManager.getValidators();
        for (NulsDataValidator validator : list) {
            this.registerValidator(validator);
        }
        this.registerValidator(CoinDataValidator.getInstance());
        initCoinDataProvider();
    }

    public AbstractCoinTransaction(int type, CoinTransferData coinParam, String password) throws NulsException {
        this(type);
        if (null != coinParam) {
            this.coinData = coinDataProvider.createByTransferData(this, coinParam, password);
            this.fee = coinParam.getFee();
        }
        this.time = TimeService.currentTimeMillis();
    }

    private void initCoinDataProvider() {
        if (null == coinDataProvider) {
            coinDataProvider = NulsContext.getServiceBean(CoinDataProvider.class);
        }
    }

    @Override
    public void afterParse() {
        this.forceCalcHash();
        coinDataProvider.afterParse(coinData, this);
    }

    public void parseCoinData(NulsByteBuffer byteBuffer) throws NulsException {
        this.coinData = coinDataProvider.parse(byteBuffer);
    }

    public CoinDataProvider getCoinDataProvider() {
        return coinDataProvider;
    }

    public final CoinData getCoinData() {
        return coinData;
    }

    public void setCoinData(CoinData coinData) {
        this.coinData = coinData;
    }

    @Override
    public T parseTxData(byte[] bytes) {
        throw new NulsRuntimeException(ErrorCode.DATA_ERROR, "The transaction never provided the method:parseTxData");
    }

    public void setSkipInputValidator(boolean skipInputValidator) {
        this.skipInputValidator = skipInputValidator;
    }

    public boolean isSkipInputValidator() {
        return skipInputValidator;
    }
}
