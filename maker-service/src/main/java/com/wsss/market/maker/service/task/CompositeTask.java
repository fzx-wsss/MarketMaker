package com.wsss.market.maker.service.task;

import com.wsss.market.maker.model.domain.SymbolInfo;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

public class CompositeTask extends AbstractAsyncTask<List> {
    private List<AsyncTask> tasks = new ArrayList<>();

    @Builder
    public CompositeTask(SymbolInfo symbolInfo) {
        super(symbolInfo);
    }

    @Override
    public List doCall() throws Exception {
        List res = new ArrayList(tasks.size());
        for(AsyncTask task : tasks) {
            res.add(task.call());
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
