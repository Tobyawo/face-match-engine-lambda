package com.gm.facematch.engine.data;


import lombok.Data;

@Data
public class MatchResponseDto {
    private String transactionId;
    private double matchScore;
    private String message;
}