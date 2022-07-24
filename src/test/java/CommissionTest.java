import async.MainApplication;
import async.commission.demo.MissionProcess;
import async.commission.util.SpringUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.ExecutionException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MainApplication.class)
public class CommissionTest {

    @Test
    public void testAnno() throws ExecutionException, InterruptedException {
        MissionProcess missionProcess = (MissionProcess) SpringUtil.getBean("MissionProcess");
        missionProcess.run();
    }
}
