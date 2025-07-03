package com.wenziyue.llmwork.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author wenziyue
 */
@Repository
@RequiredArgsConstructor
public class ArticleDao {

    private final JdbcTemplate jdbc;

    public int updateArticleSummaryAndSlug(Long articleId, String summary, String slug, Integer version) {
        return jdbc.update("update TB_WZY_BLOG_ARTICLE " +
                        "set summary = ?, slug = ?, version = ? " +
                        "where id = ? and version = ?",
                summary, slug, version + 1, articleId, version);
    }
}
