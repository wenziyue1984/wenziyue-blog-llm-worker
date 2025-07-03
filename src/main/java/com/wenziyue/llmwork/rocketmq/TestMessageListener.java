package com.wenziyue.llmwork.rocketmq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author wenziyue
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "test_topic",
        consumerGroup = "test-consumer-group",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.ORDERLY, // 消费模式，默认并发模式，消费顺序消息使用顺序模式
        enableMsgTrace = true
)
public class TestMessageListener implements RocketMQListener<String>{
    @Override
    public void onMessage(String s) {
        log.info("收到消息: {}", s);
    }
}
