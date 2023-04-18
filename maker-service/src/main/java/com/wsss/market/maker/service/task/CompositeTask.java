package com.wsss.market.maker.service.task;

import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.service.thread.pool.MarkerMakerThreadPool;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class CompositeTask extends AbstractAsyncTask<List> {
    private List<AsyncTask> tasks = new ArrayList<>();
    private MarkerMakerThreadPool pool;

    @Builder
    public CompositeTask(SymbolInfo symbolInfo, MarkerMakerThreadPool pool) {
        super(symbolInfo);
        this.pool = pool;
    }

    @Override
    public List doCall() throws Exception {
        List res = new ArrayList(tasks.size());
        for(AsyncTask task : tasks) {
            Future future = pool.execAsyncTask(task);
            res.add(future.get());
        }
        return res;
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    public void addTask(AsyncTask task) {
        if(task == null) {
            return;
        }
        tasks.add(task);
    }


}
