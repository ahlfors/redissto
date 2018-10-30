package com.aliyun.iotx.redissto.eventbus;

/**
 * @author jiehong.jh
 * @date 2018/10/15
 */
public class EventTopic {

    private String name;
    /**
     * 是否模式匹配
     */
    private boolean pattern;

    private EventTopic() {}

    /**
     * 给定名称作为Topic
     *
     * @param name
     * @return
     */
    public static EventTopic of(String name) {
        EventTopic eventTopic = new EventTopic();
        eventTopic.name = name;
        return eventTopic;
    }

    /**
     * 给定模式作为Topic
     * <pre>
     * Supported glob-style patterns:
     *    h?llo subscribes to hello, hallo and hxllo
     *    h*llo subscribes to hllo and heeeello
     *    h[ae]llo subscribes to hello and hallo, but not hillo
     * </pre>
     *
     * @param pattern
     * @return
     */
    public static EventTopic pattern(String pattern) {
        EventTopic eventTopic = new EventTopic();
        eventTopic.name = pattern;
        eventTopic.pattern = true;
        return eventTopic;
    }

    /**
     * 获取Topic
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * 是否模式订阅
     *
     * @return
     */
    public boolean isPattern() {
        return pattern;
    }
}
