package com.wenziyue.llmwork.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author wenziyue
 */
@Data
public class SlugDTO implements Serializable {

    private static final long serialVersionUID = -1385007193747605701L;

    private String title;

    private String content;

    private String summary;

    private String listenKey;

    private List<String> usedSlugs;

    private Long articleId;
}
