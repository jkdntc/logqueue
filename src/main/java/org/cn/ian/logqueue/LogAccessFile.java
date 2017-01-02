package org.cn.ian.logqueue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogAccessFile {
    protected final Object lock;
    FileWriter writer;
    BufferedReader reader;
    File file;
    RandomAccessFile maker;
    int count = 0;
    final String fileName;
    long maxLength = 100000000;
    boolean delLog = false;
    ObjectMapper mapper;

    /**
     * 默认每个日志文件大小，默认为100000000字节
     *
     * @param fileName 日志文件名可包含路径
     * @throws java.io.IOException
     */
    public LogAccessFile(String fileName) throws IOException {
        this(fileName, 100000000);
    }

    /**
     * 可设置每个日志文件大小
     *
     * @param fileName  日志文件名可包含路径
     * @param maxLength 每个日志文件大小，单位字节
     * @throws java.io.IOException
     */
    public LogAccessFile(String fileName, long maxLength) throws IOException {
        this(fileName, maxLength, false);
    }

    /**
     * 可设置每个日志文件大小，是否删除读取过的日志文件
     *
     * @param fileName  日志文件名可包含路径
     * @param maxLength 每个日志文件大小，单位字节
     * @param delLog    是否删除读取过的日志文件
     * @throws java.io.IOException
     */
    public LogAccessFile(String fileName, long maxLength, boolean delLog) throws IOException {
        this.fileName = fileName;
        this.maxLength = maxLength;
        this.delLog = delLog;
        lock = new Object();
        //如果没有此文件夹则建立之
        if (fileName.lastIndexOf('/') > 0) {
            File file = new File(fileName.substring(0, fileName.lastIndexOf('/')));
            if (!file.exists()) {
                if (!file.mkdirs())
                    throw new IOException("建立文件夹失败！");
            }
        }

        File markFile = new File(fileName + ".mark");
        if (markFile.exists()) {
            if (markFile.isFile() && markFile.canRead() && markFile.canWrite()) {
                maker = new RandomAccessFile(markFile, "rw");
                String strCount = maker.readLine();
                count = Integer.parseInt(strCount);
            } else {
                throw new IOException(fileName + ".mark 此文件异常");
            }
        } else {
            maker = new RandomAccessFile(markFile, "rw");
            mark();
        }

        //打开日志文件
        file = new File(fileName);

        writer = new FileWriter(file, true);
        reader = new BufferedReader(new FileReader(file));

        //挑到mark的已读的位置
        for (int i = 0; i < count && reader.ready(); i++) {
            reader.readLine();
        }
        //文件已达到最大字节数更名或删除之
        ready();

        mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    public int getCurrentLine() {
        return count;
    }

    public void mark() throws IOException {
        maker.seek(0);
        maker.writeBytes(count + "\r\n");
    }

    private void write(String str) throws IOException {
        synchronized (lock) {
            writer.write(str);
            writer.flush();
        }
    }

    public void close() throws IOException {
        writer.close();
        reader.close();
        maker.close();
    }

    private void changeNameOrDelete() {
        if (delLog) {
            while (!file.delete()) {
                System.out.println("文件：" + fileName + " 删除时失败！");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            File tempFile = new File(fileName
                    + new SimpleDateFormat("_yyyyMMddHHmmss").format(new Date()));
            while (!file.renameTo(tempFile)) {
                System.out.println("文件：" + fileName + " 改名为 " + tempFile.getName() + " 时失败！");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        count = 0;
        while (true) {
            try {
                maker.seek(0);
                maker.writeBytes(count + "\r\n");
                break;
            } catch (IOException e) {
                System.out.println("计数mark失败" + e.getMessage());
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    synchronized public boolean ready() throws IOException {
        boolean ready = reader.ready();
        if (!ready) {
            if (file.length() > maxLength) {
                synchronized (lock) {
                    // System.out.println(file.length());
                    if (reader.ready())
                        return true;
                    writer.close();
                    reader.close();
                    changeNameOrDelete();
                    writer = new FileWriter(file, true);
                    reader = new BufferedReader(new FileReader(file));
                    //System.out.println("新建文件 file=" + file.getName() + " length=" + file.length());
                }
            }
        }
        return ready;
    }

    public String readLine() throws IOException {
        String str = null;
        if (ready()) {
            str = reader.readLine();
            count++;
        }
        return str;
    }

    public void writeAsJson(Object obj) throws IOException {
        try {
            write(mapper.writeValueAsString(obj) + "\r\n");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public <T> T readLineFromJson(Class<T> tClass) throws IOException {
        return mapper.readValue(readLine(), tClass);
    }
}
