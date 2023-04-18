package com.wsss.market.maker.service.task;

import com.wsss.market.maker.model.domain.SymbolInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAsyncTask<T> implements AsyncTask<T> {
    private volatile boolean finish = false;

    @Getter
    protected SymbolInfo symbolInfo;

    public AbstractAsyncTask(SymbolInfo symbolInfo) {
        this.symbolInfo = symbolInfo;
    }

    @Override
    public final T call() throws Exception {
        try {
            return doCall();
        } catch (Exception e) {
            log.error("invoke call error:{}",e);
        } finally {
            finish();
        }
        return null;
    }

    protected abstract T doCall() throws Exception;

    public void finish() {
        finish = true;
    }

    public boolean isFinish() {
        return finish;
    }


}
