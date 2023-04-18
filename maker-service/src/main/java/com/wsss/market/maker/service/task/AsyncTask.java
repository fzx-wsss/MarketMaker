package com.wsss.market.maker.service.task;

import java.util.concurrent.Callable;

public interface AsyncTask<T> extends Callable<T> {
}
