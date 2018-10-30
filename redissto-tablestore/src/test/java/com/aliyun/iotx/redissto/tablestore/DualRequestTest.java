package com.aliyun.iotx.redissto.tablestore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.alicloud.openservices.tablestore.SyncClientInterface;
import com.alicloud.openservices.tablestore.model.DeleteRowRequest;
import com.alicloud.openservices.tablestore.model.GetRowRequest;
import com.alicloud.openservices.tablestore.model.GetRowResponse;
import com.alicloud.openservices.tablestore.model.PrimaryKey;
import com.alicloud.openservices.tablestore.model.PutRowRequest;
import com.alicloud.openservices.tablestore.model.RowDeleteChange;
import com.alicloud.openservices.tablestore.model.RowPutChange;
import com.alicloud.openservices.tablestore.model.SingleRowQueryCriteria;
import com.aliyun.iotx.fluentable.TableStoreOperations;
import com.aliyun.iotx.fluentable.annotation.TableStoreAnnotationParser;
import com.aliyun.iotx.fluentable.util.TableStorePkBuilder;
import com.aliyun.iotx.redissto.DualClient;
import com.aliyun.iotx.redissto.DualClient.DualReference;
import com.aliyun.iotx.redissto.DualRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author jiehong.jh
 * @date 2018/10/9
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JunitConfiguration.class, MonitorConfiguration.class})
public class DualRequestTest {

    private String tableName = "command_batch";
    @Autowired
    private SyncClientInterface syncClient;
    @Autowired
    private TableStoreOperations tableStoreTemplate;
    @Autowired
    private TableStoreAnnotationParser annotationParser;

    @Autowired
    private DualClient dualClient;

    private long id = 101;
    private PrimaryKey primaryKey;
    private DualReference reference;

    @Before
    public void setUp() {
        primaryKey = new TableStorePkBuilder().add("id", id).build();
        reference = dualClient.opsFor(tableName, primaryKey);
    }

    @Test
    public void get() throws InterruptedException {
        DualRequest<CommandBatch> request = dualClient.newCall(reference);
        CommandBatch batch = request.getIfAbsent(() -> {
            SingleRowQueryCriteria criteria = tableStoreTemplate.select().from(tableName)
                .where().pkEqual(primaryKey).rowQuery();
            GetRowResponse response = syncClient.getRow(new GetRowRequest(criteria));
            return annotationParser.parseLatest(response.getRow(), CommandBatch.class);
        }).cache().get();
        System.out.println(batch);
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    public void put() throws InterruptedException {
        DualRequest<CommandBatch> request = dualClient.newCall(reference);
        CommandBatch batch = new CommandBatch();
        batch.setId(id);
        batch.setSize(10);
        batch.setStatus("Dual");
        request.put(batch, () -> {
            RowPutChange rowPutChange = tableStoreTemplate.put(tableName)
                .with(annotationParser.parse(batch))
                .rowIgnore().rowChange();
            syncClient.putRow(new PutRowRequest(rowPutChange));
        });
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    public void delete() throws InterruptedException {
        DualRequest<CommandBatch> request = dualClient.newCall(reference);
        request.delete(() -> {
            RowDeleteChange deleteChange = tableStoreTemplate.delete(tableName)
                .where(primaryKey).rowIgnore().rowChange();
            syncClient.deleteRow(new DeleteRowRequest(deleteChange));
        });
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    public void monitor() throws InterruptedException {
        int nThreads = 50;
        CountDownLatch latch = new CountDownLatch(5000);
        Executor executor = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < nThreads; i++) {
            if (i % 2 == 0) {
                executor.execute(() -> {
                    int j = 100;
                    while (j-- > 0) {
                        latch.countDown();
                        DualRequest<CommandBatch> request = dualClient.newCall(reference);
                        CommandBatch batch = new CommandBatch();
                        batch.setId(id);
                        batch.setSize(10);
                        batch.setStatus("Dual");
                        request.put(batch, () -> {
                            RowPutChange rowPutChange = tableStoreTemplate.put(tableName)
                                .with(annotationParser.parse(batch))
                                .rowIgnore().rowChange();
                            syncClient.putRow(new PutRowRequest(rowPutChange));
                        });
                        try {
                            TimeUnit.MILLISECONDS.sleep(50);
                        } catch (InterruptedException e) {
                        }
                    }
                });
            } else {
                executor.execute(() -> {
                    int j = 100;
                    while (j-- > 0) {
                        latch.countDown();
                        DualRequest<CommandBatch> request = dualClient.newCall(reference);
                        request.delete(() -> {
                            RowDeleteChange deleteChange = tableStoreTemplate.delete(tableName)
                                .where(primaryKey).rowIgnore().rowChange();
                            syncClient.deleteRow(new DeleteRowRequest(deleteChange));
                        });
                        try {
                            TimeUnit.MILLISECONDS.sleep(80);
                        } catch (InterruptedException e) {
                        }
                    }
                });
            }
        }
        latch.await();
        TimeUnit.SECONDS.sleep(1);
    }
}
