package com.wenziyue.llmwork.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author wenziyue
 */
@Data
public class SummaryDTO implements Serializable {

    private static final long serialVersionUID = 2667775658334377012L;

    private String title;

    private String content;

    private List<String> usedSlugs;

    private Long articleId;

    private Integer version;
}
