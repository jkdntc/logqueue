import org.cn.ian.logqueue.Executor;
import org.cn.ian.logqueue.ExecutorListener;

import java.io.IOException;
import java.util.List;

/**
 * Created by jiangkun on 17/1/2.
 */
public class Example {

    public static void main(String[] args) {
        try {
            Executor<SimpleModel> executor = new Executor(new ExecutorListener<SimpleModel>() {
                int count = 0;

                public void call(List<SimpleModel> list) throws Exception {
                    count += list.size();
                    System.out.println("批量处理数：" + list.size() + " 总数：" + count);
                }
            }, "simplelog.log", 100 * 1024 * 1024, false, 1000, SimpleModel.class);

            for (int i = 0; i < 100000; i++) {
                executor.submit(new SimpleModel("name_" + i, i));
            }
        } catch (IOException e) {
            // 硬盘错误，可能满了或坏了
            e.printStackTrace();
        }

    }
}
