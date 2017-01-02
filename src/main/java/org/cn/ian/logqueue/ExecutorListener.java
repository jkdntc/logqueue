package org.cn.ian.logqueue;

import java.util.List;

public interface ExecutorListener<T> {
    public void call(List<T> list) throws Exception;
}
