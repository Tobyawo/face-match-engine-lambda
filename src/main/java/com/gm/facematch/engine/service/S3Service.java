package com.gm.facematch.engine.service;

import com.gm.facematch.engine.DatabaseConfig;
import com.gm.facematch.engine.data.MatchRequestDto;
import com.gm.facematch.engine.entity.MatchRecord;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import javax.sql.DataSource;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Base64;

import java.sql.Connection;
import java.sql.PreparedStatement;

@Component
public class S3Service {

    private final S3Client s3Client;
    private final DataSource dataSource = DatabaseConfig.getDataSource();

    private static final String INSERT_SQL = "INSERT INTO match_record (transaction_id, image1_url, image2_url, match_score, description, callback_url, user_name, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";


    public S3Service() {

        String awsS3Region = System.getenv("AWS_S3_REGION");
        if (awsS3Region == null || awsS3Region.isEmpty()) {
            throw new IllegalStateException("AWS_S3_REGION environment variable is not set");
        }
        this.s3Client = S3Client.builder()
                .region(Region.of(awsS3Region))
                .credentialsProvider(DefaultCredentialsProvider.create()) // Auto-picks AWS Lambda credentials
                .build();
    }

    public MatchRecord processMatchRequest(MatchRequestDto matchRequest) {
        MatchRecord record = new MatchRecord();
        record.setImage1Url(matchRequest.getImage1Url());
        record.setImage2Url(matchRequest.getImage2Url());
        record.setTransactionId(matchRequest.getTransactionId());
        record.setCreatedAt(LocalDateTime.now());
        record.setCode(0);
        record.setDescription("Image comparison completed successfully.");
        record.setUserName(matchRequest.getUserName());
        record.setCallBackUrl(matchRequest.getCallBackUrl());

        try {
            String base64Image1 = downloadImageFromS3(matchRequest.getImage1Url());
            String base64Image2 = downloadImageFromS3(matchRequest.getImage2Url());

            if (base64Image1 == null || base64Image2 == null) {
                record.setCode(-1);
                record.setDescription("Failed to download one or both images.");
            } else {
                double similarityScore = calculateSimilarity(base64Image1, base64Image2);
                record.setMatchScore(similarityScore);
            }

        } catch (Exception e) {
            record.setCode(-3);
            record.setDescription("Error processing image match request: " + e.getMessage());
        }


        try (Connection connection = dataSource.getConnection()) {
            saveMatchResultToDB(connection, record);
            if (record.getUserName() != null && !record.getUserName().isEmpty()) {
                SNSService snsService = new SNSService();
                snsService.publishToSNS(record);
            }
        } catch (Exception e) {
            record.setCode(-2);
            record.setDescription("Failed to save match result to database." + e.getMessage());
        }

        return record;
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

    private void saveMatchResultToDB(Connection connection, MatchRecord record) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_SQL)) {

            stmt.setString(1, record.getTransactionId());
            stmt.setString(2, record.getImage1Url());
            stmt.setString(3, record.getImage2Url());
            stmt.setDouble(4, record.getMatchScore());
            stmt.setString(5, record.getDescription());
            stmt.setString(6, record.getCallBackUrl());
            stmt.setString(7, record.getUserName());
            stmt.setObject(8, record.getCreatedAt());
            stmt.executeUpdate();
        }
    }
}
