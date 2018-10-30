package com.aliyun.iotx.redissto.tablestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.aliyun.iotx.redissto.DualMonitor;
import com.aliyun.iotx.redissto.DualMonitorOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jiehong.jh
 * @date 2018/10/10
 */
@Configuration
public class MonitorConfiguration {

    @Bean
    public DualMonitor monitor() {
        DualMonitorOption option = new DualMonitorOption();
        option.setTime(3);
        option.setUnit(TimeUnit.SECONDS);
        List<Integer> intervalList = new ArrayList<>();
        intervalList.add(100);
        intervalList.add(10);
        intervalList.add(50);
        intervalList.add(300);
        intervalList.add(150);
        intervalList.add(500);
        option.setIntervalList(intervalList);
        option.setIntervalUnit(TimeUnit.MILLISECONDS);
        List<Integer> executeList = new ArrayList<>();
        executeList.add(100);
        executeList.add(300);
        executeList.add(500);
        option.setExecuteWarn(100);
        option.setExecuteList(executeList);
        option.setExecuteUnit(TimeUnit.MILLISECONDS);
        return new DualMonitor(option);
    }
}
