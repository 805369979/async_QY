package async.commission.demo;

import async.commission.Async;
import async.commission.entity.Context;
import async.commission.executor.timer.SystemClock;
import async.commission.template.AbstractNode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component("MissionProcess")
public class MissionProcess {

    public void run() throws InterruptedException, ExecutionException {
        Node1 node1 = new Node1();
        Node2 node2 = new Node2();
        Node3 node3 = new Node3();
        Node4 node4 = new Node4();
        Node5 node5 = new Node5();
        Node6 node6 = new Node6();
        Node7 node7 = new Node7();
        Node8 node8 = new Node8();

        // 编排任务
        node1.setSon(node2,node3);

        node2.setFather(node1);
        node2.setSon(node4,node5);

        node3.setFather(node1);
        node3.setSon(node6);

        node4.setFather(node2);
        node4.setSon(node7);

        node5.setFather(node2);
        node5.setSon(node7);

        node6.setFather(node3);
        node6.setSon(node7);

        node7.setFather(node4,node5,node6);
        node7.setSon(node8);

        node8.setFather(node7);

        // 设置上下文
        Context context1 = new Context();

        long now = SystemClock.now();
        System.out.println("begin-" + now);
        // 开始执行任务
        Map<String, AbstractNode> results = Async.beginWork(2500, context1, node1);
        results.forEach((k,v)->{
            System.out.println(v.getWorkResult());
        });
        System.out.println("end-" + SystemClock.now());
        System.err.println("cost-" + (SystemClock.now() - now));
        Async.shutDown();
    }
}
