package com.errortracker.dto;

import lombok.Data;

import java.util.List;

@Data
public class NotificationRecipientRequest {

    private List<String> emails;

}