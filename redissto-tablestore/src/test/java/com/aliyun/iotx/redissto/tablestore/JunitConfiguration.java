package com.aliyun.iotx.redissto.tablestore;

import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.SyncClientInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * @author jiehong.jh
 * @date 2018/9/7
 */
@Configuration
@PropertySource("classpath:table-store.properties")
@Import(DualAutoConfiguration.class)
public class JunitConfiguration {

    @Value("${endpoint}")
    private String endpoint;
    @Value("${instance}")
    private String instance;
    @Value("${accessKey}")
    private String accessKey;
    @Value("${secretKey}")
    private String secretKey;

    @Value("${redis.host}")
    private String host;
    @Value("${redis.port}")
    private String port;
    @Value("${redis.password}")
    private String password;

    @Bean
    public SyncClientInterface syncClient() {
        return new SyncClient(endpoint, accessKey, secretKey, instance);
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson(ObjectMapper objectMapper) {
        String address = "redis://" + host + ":" + port;
        Config config = new Config();
        config.setCodec(new JsonJacksonCodec(objectMapper))
            .useSingleServer()
            .setAddress(address)
            .setPassword(password);
        return Redisson.create(config);
    }
}
