package com.gm.facematch.engine.data;


import lombok.Data;


@Data
public class MatchRequestDto {
    private String image1Url;
    private String image2Url;
    private String transactionId;
    private String callBackUrl;
    private String userName;

}
