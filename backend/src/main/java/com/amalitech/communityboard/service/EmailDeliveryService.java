package com.amalitech.communityboard.service;

public interface EmailDeliveryService {

    void send(String from, String to, String subject, String body);
}
