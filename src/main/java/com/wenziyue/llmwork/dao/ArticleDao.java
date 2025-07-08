package com.wenziyue.llmwork.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * @author wenziyue
 */
@Repository
@RequiredArgsConstructor
public class ArticleDao {

    private final JdbcTemplate jdbc;

    @SuppressWarnings("SqlResolve")
    public int updateArticleSummaryAndSlug(Long articleId, String summary, String slug, LocalDateTime updateTime) {
        return jdbc.update("update TB_WZY_BLOG_ARTICLE " +
                        "set summary = ?, slug = ?" +
                        "where id = ? and update_time = ?",
                summary, slug, articleId, updateTime);
    }
}
