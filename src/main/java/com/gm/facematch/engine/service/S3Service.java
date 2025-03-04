package com.gm.facematch.engine.service;

import com.gm.facematch.engine.data.MatchRequestDto;
import com.gm.facematch.engine.data.MatchResponseDto;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.URL;
import java.util.Base64;

public class S3Service {

    private final S3Client s3Client;




//    public S3Service() {
//
//        this.s3Client = S3Client.builder()
//                .region(Region.US_EAST_2)
//                .credentialsProvider(StaticCredentialsProvider.create(
//                        AwsBasicCredentials.create(awsS3AccessKey, awsS3SecretKey)))
//                .build();
//
//    }

    public S3Service() {

        String awsS3Region = System.getenv("AWS_S3_REGION"); // Read from environment
        if (awsS3Region == null || awsS3Region.isEmpty()) {
            throw new IllegalStateException("AWS_S3_REGION environment variable is not set");
        }
        this.s3Client = S3Client.builder()
                .region(Region.of(awsS3Region))
                .credentialsProvider(DefaultCredentialsProvider.create()) // Auto-picks AWS Lambda credentials
                .build();
    }

    public MatchResponseDto processMatchRequest(MatchRequestDto matchRequest) {
        MatchResponseDto response = new MatchResponseDto();
        response.setTransactionId(matchRequest.getTransactionId());

        try {
            String base64Image1 = downloadImageFromS3(matchRequest.getImage1Url());
            String base64Image2 = downloadImageFromS3(matchRequest.getImage2Url());

            if (base64Image1 == null || base64Image2 == null) {
                response.setMatchScore(0.0);
                response.setMessage("Failed to download one or both images.");
                return response;
            }

            double similarityScore = calculateSimilarity(base64Image1, base64Image2);
            response.setMatchScore(similarityScore);
            response.setMessage("Image comparison completed successfully.");
        } catch (Exception e) {
            response.setMatchScore(0.0);
            response.setMessage("Error processing image match request: " + e.getMessage());
        }

        return response;
    }

    private String downloadImageFromS3(String imageUrl) throws Exception {
        URL url = new URL(imageUrl);
        String fileName = url.getPath().substring(1);
        String awsS3BucketName = System.getenv("AWS_S3_BUCKET_NAME"); // Read from environment
        if (awsS3BucketName == null || awsS3BucketName.isEmpty()) {
            throw new IllegalStateException("AWS_S3_BUCKET_NAME environment variable is not set");
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(fileName)
                .build();

        ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
        byte[] imageBytes = responseBytes.asByteArray();

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private double calculateSimilarity(String base64Image1, String base64Image2) {
        byte[] image1Bytes = Base64.getDecoder().decode(base64Image1);
        byte[] image2Bytes = Base64.getDecoder().decode(base64Image2);

        return image1Bytes.length == image2Bytes.length ? 1.0 : 0.5;
    }
}
