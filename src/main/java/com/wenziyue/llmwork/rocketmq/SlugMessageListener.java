package com.wenziyue.llmwork.rocketmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenziyue.llmwork.dto.SlugDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * @author wenziyue
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "blog_slug",
        consumerGroup = "slug-consumer-group",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.ORDERLY, // 消费模式，默认并发模式，消费顺序消息使用顺序模式
        enableMsgTrace = true
)
public class SlugMessageListener implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private static final String OLLAMA_URL = "http://localhost:11434/v1/chat/completions";


    @Override
    public void onMessage(String message) {
        log.info("收到生成 slug 请求: {}", message);
        try {
            val slugDTO = objectMapper.readValue(message, SlugDTO.class);
            if (slugDTO == null) {
                log.error("转换 slugDTO 结果为null:{}",  message);
                return;
            }
            log.info("slugDTO:{}", slugDTO);
            // 发送http请求
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 设置请求体
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.exchange(OLLAMA_URL, HttpMethod.POST, request, String.class);
            log.info("response:{}", response);

        } catch (Exception e) {
            log.error("生成 slug 失败", e);
        }
    }
}
