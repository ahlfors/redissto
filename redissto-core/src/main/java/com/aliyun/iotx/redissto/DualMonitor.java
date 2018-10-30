package com.aliyun.iotx.redissto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import com.aliyun.iotx.redissto.support.AbstractBucket;
import com.aliyun.iotx.redissto.support.BucketContainer;
import com.aliyun.iotx.redissto.support.SlideWindow;
import com.aliyun.iotx.redissto.support.TimerSlideWindow;
import lombok.extern.slf4j.Slf4j;

/**
 * 异步双写延迟监控
 *
 * @author jiehong.jh
 * @date 2018/10/10
 */
@Slf4j
public class DualMonitor implements AutoCloseable {

    private static final int SLIDE_WINDOW_LENGTH = 5;

    private final SlideWindow<TimeEntry> slideWindow;

    private final AtomicBoolean closed = new AtomicBoolean();

    private Map<Long, LongAdder> intervalMap;
    private List<Long> intervalList;

    private long executeWarn;
    private Map<Long, LongAdder> executeMap;
    private List<Long> executeList;

    public DualMonitor(DualMonitorOption option) {
        long time = option.getTime();
        TimeUnit unit = option.getUnit();
        slideWindow = new TimerSlideWindow<>(SLIDE_WINDOW_LENGTH, new Bucket(), time, unit);

        List<Integer> optionIntervalList = option.getIntervalList();
        TimeUnit intervalUnit = option.getIntervalUnit();
        intervalMap = new HashMap<>(optionIntervalList.size() + 1);
        intervalMap.put(0L, new LongAdder());
        optionIntervalList.forEach(i -> intervalMap.put(intervalUnit.toMillis(i), new LongAdder()));
        intervalList = new ArrayList<>(intervalMap.keySet());
        Collections.sort(intervalList);

        executeWarn = option.getExecuteWarn();
        List<Integer> optionExecuteList = option.getExecuteList();
        TimeUnit executeUnit = option.getExecuteUnit();
        executeMap = new HashMap<>(optionExecuteList.size() + 1);
        executeMap.put(0L, new LongAdder());
        optionExecuteList.forEach(i -> executeMap.put(executeUnit.toMillis(i), new LongAdder()));
        executeList = new ArrayList<>(executeMap.keySet());
        Collections.sort(executeList);
        log.info("Dual monitor start.");
    }

    /**
     * 监控延迟
     *
     * @param millis 延迟时间（毫秒）
     */
    public void observeInterval(long millis) {
        if (isClose()) {
            return;
        }
        slideWindow.put(new TimeEntry(millis, intervalList, intervalMap));
    }

    /**
     * 监控执行
     *
     * @param millis 执行时间（毫秒）
     */
    public void observeExecute(long millis) {
        if (isClose()) {
            return;
        }
        if (millis >= executeWarn) {
            log.warn("Dual execute: {}ms", millis);
        }
        slideWindow.put(new TimeEntry(millis, executeList, executeMap));
    }

    /**
     * 监控是否被关闭
     *
     * @return
     */
    public boolean isClose() {
        return closed.get();
    }

    @Override
    public void close() throws Exception {
        if (closed.compareAndSet(false, true)) {
            log.info("Dual monitor close.");
            slideWindow.close();
        }
    }

    private LongAdder locate(TimeEntry timeEntry) {
        long time = timeEntry.millis;
        List<Long> list = timeEntry.list;
        Map<Long, LongAdder> map = timeEntry.map;
        int size = list.size();
        for (int i = 0; i < size; i++) {
            Long key = list.get(i);
            if (key.equals(time)) {
                return map.get(key);
            }
            if (time < key) {
                return map.get(list.get(i - 1));
            }
        }
        return map.get(list.get(size - 1));
    }

    private class Container implements BucketContainer<TimeEntry> {

        @Override
        public void onAccept(TimeEntry timeEntry) {
            locate(timeEntry).increment();
        }

        @Override
        public void onComplete(long ttl) {
            int size = intervalList.size();
            for (int i = 0; i < size; i++) {
                Long interval = intervalList.get(i);
                long count = intervalMap.get(interval).sumThenReset();
                if (count == 0) {
                    continue;
                }
                if (i == size - 1) {
                    log.info("The interval [{}, +oo): {}", interval, count);
                } else {
                    log.info("The interval [{}, {}): {}", interval, intervalList.get(i + 1), count);
                }
            }
            size = executeList.size();
            for (int i = 0; i < size; i++) {
                Long execute = executeList.get(i);
                long count = executeMap.get(execute).sumThenReset();
                if (count == 0) {
                    continue;
                }
                if (i == size - 1) {
                    log.info("The execute [{}, +oo): {}", execute, count);
                } else {
                    log.info("The execute [{}, {}): {}", execute, executeList.get(i + 1), count);
                }
            }
        }

        @Override
        public void onExpire() {
            // do nothing
        }
    }

    private class Bucket extends AbstractBucket<TimeEntry> {

        Bucket() {
            super(new Container());
        }

        @Override
        public AbstractBucket<TimeEntry> next() {
            return new Bucket();
        }
    }

    private class TimeEntry {
        long millis;
        List<Long> list;
        Map<Long, LongAdder> map;

        TimeEntry(long millis, List<Long> list, Map<Long, LongAdder> map) {
            this.millis = millis;
            this.list = list;
            this.map = map;
        }
    }
}
