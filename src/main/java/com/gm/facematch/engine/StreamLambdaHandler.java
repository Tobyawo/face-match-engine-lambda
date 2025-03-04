package com.gm.facematch.engine;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.facematch.engine.data.MatchRequestDto;
import com.gm.facematch.engine.data.MatchResponseDto;
import com.gm.facematch.engine.service.S3Service;

import java.util.List;
import java.util.Map;

public class StreamLambdaHandler implements RequestHandler<Map<String, Object>, Void> {

    private final ObjectMapper objectMapper;
    private final S3Service s3Service;


    public StreamLambdaHandler() {
        this.objectMapper = new ObjectMapper();
        this.s3Service = new S3Service();
    }


    @Override
    public Void handleRequest(Map<String, Object> event, Context context) {
        try {
            Object recordsObj = event.get("Records");
            if (recordsObj instanceof List<?> records) {

                for (Object recordObj : records) {
                    if (recordObj instanceof Map) {
                        Map<String, Object> record = (Map<String, Object>) recordObj;
                        String body = (String) record.get("body");
                        context.getLogger().log("Received SQS message: " + body);

                        MatchRequestDto request = convertJsonToDto(body);

                        MatchResponseDto responseDto = s3Service.processMatchRequest(request);
                        context.getLogger().log("Response : " + responseDto);
                    } else {
                        context.getLogger().log("Invalid record type: " + recordObj.getClass());
                    }
                }
            } else {
                context.getLogger().log("No valid 'Records' found in SQS event.");
            }
        } catch (Exception e) {
            context.getLogger().log("Error processing SQS event: " + e.getMessage());
        }
        return null;
    }

    private MatchRequestDto convertJsonToDto(String json) {
        try {
            return objectMapper.readValue(json, MatchRequestDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing SQS message", e);
        }
    }
}
