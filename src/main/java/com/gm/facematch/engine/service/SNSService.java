package com.gm.facematch.engine.service;

import com.gm.facematch.engine.entity.MatchRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

@Component
@Slf4j
public class SNSService {

    private final SnsClient snsClient;
    private final String topicArn = System.getenv("SNS_TOPIC_ARN");

    public SNSService() {
        this.snsClient = SnsClient.builder()
                .region(Region.US_EAST_1)  // Set AWS Region (Change if needed)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public void publishToSNS(MatchRecord record) {
        if (topicArn == null || topicArn.isEmpty()) {
            throw new IllegalStateException("SNS_TOPIC_ARN environment variable is not set");
        }

        SubscriptionStatus status = getEmailSubscriptionStatus(record.getUserName());

        log.info("Email Subscription status ===== " + status.name());

        // If not subscribed or pending, attempt to subscribe the email
        if (status == SubscriptionStatus.NOT_SUBSCRIBED) {
            subscribeEmailToTopic(topicArn, record.getUserName());
            log.info("Subscription initiated for email: {}", record.getUserName());
            return;
        }


        // Only publish a notification if the email is confirmed
        if (status == SubscriptionStatus.CONFIRMED) {
            String message = String.format("TransactionId: %s, Match Score: %s, User: %s",
                    record.getTransactionId(), record.getMatchScore(), record.getUserName());

            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .build();

            snsClient.publish(publishRequest);
            log.info("Published message to SNS: {}", message);
        } else {
            log.info("Skipping SNS publish. Subscription is still pending for email: {}", record.getUserName());
        }
    }

    public void subscribeEmailToTopic(String topicArn, String email) {
        SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("email")
                .endpoint(email)
                .build();

        SubscribeResponse response = snsClient.subscribe(request);
        log.info("Subscription request sent! Subscription ARN: {}", response.subscriptionArn());
    }

    private SubscriptionStatus getEmailSubscriptionStatus(String email) {
        try {
            ListSubscriptionsByTopicRequest listRequest = ListSubscriptionsByTopicRequest.builder()
                    .topicArn(topicArn)
                    .build();

            ListSubscriptionsByTopicResponse listResponse = snsClient.listSubscriptionsByTopic(listRequest);

            for (Subscription sub : listResponse.subscriptions()) {
                if (sub.endpoint().equalsIgnoreCase(email) && "email".equals(sub.protocol())) {
                    if (sub.subscriptionArn() == null || "PendingConfirmation".equals(sub.subscriptionArn())) {
                        return SubscriptionStatus.PENDING;
                    }
                    return SubscriptionStatus.CONFIRMED;
                }
            }
            return SubscriptionStatus.NOT_SUBSCRIBED;
        } catch (SnsException e) {
            log.error("Error checking SNS subscriptions: {}", e.getMessage());
            return SubscriptionStatus.NOT_SUBSCRIBED;
        }
    }

    private enum SubscriptionStatus {
        NOT_SUBSCRIBED, PENDING, CONFIRMED
    }
}
