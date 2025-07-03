# wenziyue-blog-llm-worker



这是博客系统的 LLM 工作节点子服务，专注于**生成文章摘要和英文 slug**，通过异步消费 RocketMQ 消息，并调用本地部署的大语言模型（如 Ollama）完成摘要与 slug 的生成，最终将结果写回数据库。



------





## **✨ 项目特点**

- 💬 **异步消息处理**：监听 blog_summary RocketMQ Topic，异步触发内容摘要生成。
- 🧠 **大模型推理**：调用本地 Ollama 大模型（如 Qwen3:4b）生成摘要与 slug。
- 🔁 **版本校验**：使用 Redis 对文章版本进行校验，避免并发更新带来的数据不一致。
- 🛠 **插件化设计**：依赖自研 Starter（如 Redis、日志 TraceId 工具等），与主项目解耦清晰。
- 🧩 **智能提示模板**：自动构建 Prompt，支持 slug 唯一性校验。



------





## **📦 项目结构**



```
wenziyue-blog-llm-worker
├── rocketmq/
│   └── SummaryMessageListener.java   // 核心消息处理器
├── dao/
│   └── ArticleDao.java               // 用于更新文章 summary 和 slug
├── dto/
│   └── SummaryDTO.java               // MQ 中传输的结构
├── constant/
│   └── RedisConstant.java            // Redis Key 常量定义
├── LlmWorkerApplication.java         // 应用启动类
```



------





## **🔧 核心流程**





1. 博客主系统通过 MQ 向 blog_summary Topic 发送摘要生成请求（含文章内容、title、版本号等）。

2. 本服务消费消息后：

   

   - 校验 Redis 中的文章版本是否一致；
   - 构造 Prompt，发送给本地 Ollama 模型；
   - 解析模型返回的 JSON 字符串，提取 summary 和 slug；
   - 写入数据库，删除 Redis 缓存版本号；

   

3. 若版本不一致或发生异常，记录日志并中止处理。





------





## **📥 项目依赖**



- JDK 8+
- Spring Boot 2.7.18
- MySQL 8.x
- Redis 6.x
- RocketMQ 5.x
- Ollama 本地大模型（已通过 HTTP 接口对接）



### **依赖的自研 Starter**

- wenziyue-redis-starter
- wenziyue-framework-starter（统一日志与 TraceId 支持）



------



## **⚙️ 启动说明**



### **1. 配置环境变量或.env**



支持通过如下环境变量控制：

- DB_HOST, DB_NAME, DB_USER, DB_PASSWORD
- REDIS_HOST
- ROCKET_MQ_SERVER



或在 application.yml 中手动配置。





### **2. 启动 Ollama 模型服务**

```shell
ollama run qwen3:4b
```

或自定义模型路径，并确保监听在 http://localhost:11434/v1/chat/completions。



### **3. 启动应用**



```shell
mvn spring-boot:run
```

或打包后运行：

```shell
java -jar target/wenziyue-blog-llm-worker-1.0.0.jar
```



------





## **📤 消息格式示例（MQ）**



```
{
  "title": "测试文章",
  "content": "这是文章内容",
  "userId": 10001,
  "articleId": 20002,
  "version": 3,
  "usedSlugs": ["test-article", "article-1"]
}
```



------





## **📘 示例返回格式（模型返回）**



```
{
  "summary": "本文测试文章保存接口，并验证LLM生成摘要和slug的功能是否正常",
  "slug": "test-article-slug"
}
```



------





## **🧠 总结**



该服务旨在将大模型能力与业务逻辑解耦，提升生成摘要与 slug 的自动化程度，同时确保高并发下的**幂等性、准确性与数据一致性**。适合部署为独立 Worker 服务，与主项目保持异步协作关系。



------

