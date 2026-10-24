package com.jsmc.spacemap.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InvitationMailService {
    private static final Logger log = LoggerFactory.getLogger(InvitationMailService.class);

    public void sendInvitation(String email, String projectTitle, String token) {
        log.info("Invitation email stub: to={}, project={}, tokenLength={}", email, projectTitle, token == null ? 0 : token.length());
    }
}
