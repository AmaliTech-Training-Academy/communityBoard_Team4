package com.amalitech.communityboard.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Service
@Slf4j
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "ses")
public class SesEmailDeliveryService implements EmailDeliveryService {

    @Value("${app.mail.aws.region:${AWS_REGION:eu-west-1}}")
    private String awsRegion;

    private SesV2Client sesClient;

    @Override
    public void send(String from, String to, String subject, String body) {
        try {
            client().sendEmail(buildRequest(from, to, subject, body));
        } catch (Exception ex) {
            log.warn("Failed to send SES email to {}: {}", to, ex.getMessage());
        }
    }

    private SendEmailRequest buildRequest(String from, String to, String subject, String body) {
        return SendEmailRequest.builder()
                .fromEmailAddress(from)
                .destination(Destination.builder().toAddresses(to).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).charset("UTF-8").build())
                                .body(Body.builder()
                                        .text(Content.builder().data(body).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private SesV2Client client() {
        if (sesClient == null) {
            sesClient = SesV2Client.builder()
                    .region(Region.of(awsRegion))
                    .build();
        }
        return sesClient;
    }

    @PreDestroy
    void close() {
        if (sesClient != null) {
            sesClient.close();
        }
    }
}
