package com.gm.facematch.engine.entity;


import lombok.Data;

import java.time.LocalDateTime;


@Data
public class MatchRecord {
    private Long id;
    private Integer code;
    private String description;
    private String transactionId;
    private String image1Url;
    private String image2Url;
    private double matchScore;
    private String callBackUrl;
    private String userName;
    private LocalDateTime createdAt;
}
