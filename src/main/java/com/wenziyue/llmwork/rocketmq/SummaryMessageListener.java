package com.wenziyue.llmwork.rocketmq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenziyue.llmwork.dao.ArticleDao;
import com.wenziyue.llmwork.dto.SummaryDTO;
import com.wenziyue.redis.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.wenziyue.llmwork.constant.RedisConstant.ARTICLE_UPDATE_TIME_KEY;


/**
 * @author wenziyue
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "blog_summary",
        consumerGroup = "summary-consumer-group",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.ORDERLY,
        enableMsgTrace = true
)
public class SummaryMessageListener implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final RedisUtils redisUtils;
    private final ArticleDao articleDao;

    private static final String OLLAMA_URL = "http://localhost:11434/v1/chat/completions";

    @Override
    public void onMessage(String message) {
        log.info("收到生成 summary 请求: {}", message);
        SummaryDTO summaryDTO = null;
        try {
            summaryDTO = objectMapper.readValue(message, SummaryDTO.class);
            if (summaryDTO == null) {
                log.error("转换 summaryDTO 结果为null:{}", message);
                return;
            }
            if (isVersionMismatch(summaryDTO.getArticleId(), summaryDTO.getUpdateTime())) {
                return;
            }

            // 构造 tools 数组
            String toolsJson =
                    "[{\"name\":\"gen_summary_slug\",\"description\":\"生成摘要和 slug\","
                            + "\"parameters\":{\"type\":\"object\",\"properties\":{"
                            + "\"summary\":{\"type\":\"string\"},"
                            + "\"slug\":{\"type\":\"string\"}},"
                            + "\"required\":[\"summary\",\"slug\"]}}]";
            // 生成 messages
            String userContent = promptBuilder(summaryDTO); // 根据 DTO 构造提示
            String payload = "{"
                    + "\"model\":\"qwen3:4b\","
                    + "\"stream\":false,"
                    + "\"messages\":[{\"role\":\"user\",\"content\":" + objectMapper.writeValueAsString(userContent) + "}],"
                    + "\"tools\":" + toolsJson
                    + "}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> req = new HttpEntity<>(payload, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(OLLAMA_URL, req, String.class);
            // 解析顶层 JSON
            JsonNode root = objectMapper.readTree(resp.getBody());
            String content = root.at("/choices/0/message/content").asText();
            // 提取 <think>...</think> 之后的 JSON 部分，用正则去除 <think>...</think>
            String jsonPart = content.replaceAll("(?s)<think>.*?</think>", "").trim();
            // 再次解析成 JSON 对象
            JsonNode resultNode = objectMapper.readTree(jsonPart);

            String summary = resultNode.get("summary").asText();
            String slug = resultNode.get("slug").asText();

            log.info("生成 summary 结果: {}", summary);
            log.info("生成 slug 结果: {}", slug);

            if (isVersionMismatch(summaryDTO.getArticleId(), summaryDTO.getUpdateTime())) {
                return;
            }
            // 将 summary、slug 写入DB
            if (articleDao.updateArticleSummaryAndSlug(summaryDTO.getArticleId(), summary, slug,  LocalDateTime.parse(summaryDTO.getUpdateTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))) == 0) {
                log.error("更新文章 {} 的 summary、slug 失败", summaryDTO.getArticleId());
            }
        } catch (Exception e) {
            log.error("生成 summary 失败", e);
        } finally {
            // 删除版本号
            if (summaryDTO != null && summaryDTO.getArticleId() != null) {
                redisUtils.delete(ARTICLE_UPDATE_TIME_KEY + summaryDTO.getArticleId());
            }
        }
    }

    private String promptBuilder(SummaryDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("请先阅读以下博文内容，然后一步生成 100 字以内的中文摘要，")
                .append("和 20 字符以内的英文 slug（单词用 - 连接，全部小写）。");
        if (dto.getUsedSlugs() != null && !dto.getUsedSlugs().isEmpty()) {
            sb.append(" 请确保 slug 不与已使用列表重复：")
                    .append(dto.getUsedSlugs());
        }
        sb.append("\\n\\n标题：").append(dto.getTitle())
                .append("\\n正文：").append(dto.getContent());
        sb.append("\\n\\n仅以 JSON 形式给出，不要任何解释。格式：{\"summary\":\"...\",\"slug\":\"...\"}");
        return sb.toString();
    }

    /**
     * 检查文章当前版本和传入版本是否一致
     * @return true:一致， false:不一致
     */
//    @SuppressWarnings("BooleanMethodIsAlwaysInverted") //屏蔽inverted警告
    private boolean isVersionMismatch(Long articleId, String updateTime) {
        val versionNow = redisUtils.get(ARTICLE_UPDATE_TIME_KEY + articleId, String.class);
        if (versionNow == null) {
            log.error("文章 {} 的版本不存在", articleId);
            return true;
        }
        if (!versionNow.equals(updateTime)) {
            log.error("文章 {} 的版本不匹配", articleId);
            return true;
        }
        return false;
    }
}