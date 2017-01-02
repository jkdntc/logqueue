package org.cn.ian.logqueue;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Executor<T> implements Runnable {
    private LogAccessFile laf;
    private ExecutorService pool;
    private int maxCount;
    private final ExecutorListener<T> listener;
    private final Object lock;
    private Future<?> future;
    private boolean timeout = false;
    private Class<T> clazz;

    /**
     *
     * @param listener 队列数据进入此方法处理
     * @param fileName  日志文件名可包含路径
     * @param maxLength 每个日志文件大小，单位字节
     * @param delLog    是否删除读取过的日志文件
     * @param maxCount listener最多一次处理数据条数
     * @param clazz 队列数据类型
     * @throws IOException
     */
    public Executor(ExecutorListener<T> listener, String fileName, long maxLength, boolean delLog,
                    int maxCount, Class<T> clazz) throws IOException {
        this.clazz = clazz;
        this.listener = listener;
        laf = new LogAccessFile(fileName, maxLength, delLog);
        this.maxCount = maxCount;
        lock = new Object();
        pool = Executors.newSingleThreadExecutor();
        future = pool.submit(this);
    }

    public void submit(T log) throws IOException {
        laf.writeAsJson(log);
        if (timeout)
            synchronized (lock) {
                if (pool == null || pool.isShutdown()) {
                    pool = Executors.newSingleThreadExecutor();
                }
                if (future == null || future.isDone())
                    future = pool.submit(this);
            }
        else {
            if (pool == null || pool.isShutdown()) {
                pool = Executors.newSingleThreadExecutor();
            }
            if (future == null || future.isDone())
                future = pool.submit(this);
        }
    }

    public void run() {
        long idel = 0;
        List<T> list = new ArrayList<T>();
        while (pool != null && !pool.isShutdown()) {
            try {
                while (laf.ready()) {
                    idel = System.currentTimeMillis();
                    timeout = false;
                    while (laf.ready() && list.size() < maxCount) {
                        T t = laf.readLineFromJson(clazz);
                        if (t != null) {
                            list.add(t);
                        }
                    }
                    if (list.size() > 0) {
                        if (listener != null)
                            while (true) {
                                try {
                                    listener.call(list);
                                    break;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                        laf.mark();
                        list.clear();
                    }
                }
                if (System.currentTimeMillis() - idel > 3 * 1000) {
                    timeout = true;
                    synchronized (lock) {
                        if (!laf.ready()) {
                            pool.shutdown();
                            pool = null;
                        }
                    }
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        //  没事
                        e.printStackTrace();
                    }
                }
            } catch (IOException ex) {
                //  严重错误
                ex.printStackTrace();
            }
        }
        //System.out.println("Executor quit");
    }
}
