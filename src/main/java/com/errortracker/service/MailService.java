package com.errortracker.service;

import com.errortracker.model.ErrorEvent;
import com.errortracker.model.Mailbox;
import com.errortracker.model.Project;
import com.errortracker.repository.MailboxRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender sender;

    private final MailboxRepository mailboxRepo;

    @Value("${mail.switch}")
    private boolean enabled;

    @Value("${spring.mail.username}")
    private String from;

    public void sendErrorMail(Project project, ErrorEvent error, String to){

        if(!enabled){ return; }

        Mailbox mail= Mailbox.builder().project(project).fromEmail(from).toEmail(to).subject("Error Alert • " + project.getName()
                        ).status("PENDING").build();

        try{

            String html= loadTemplate().replace("${project}", project.getName())
                            .replace("${error}", error.getErrorType())
                            .replace("${class}", error.getClassName())
                            .replace("${method}", error.getMethodName())
                            .replace("${message}", error.getMessage());

            MimeMessage msg= sender.createMimeMessage();

            MimeMessageHelper helper= new MimeMessageHelper(msg, true,"UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(mail.getSubject());
            helper.setText(html, true);
            sender.send(msg);
            mail.setStatus("SENT");
            mail.setHtmlContent(html);
            mail.setSentAt(java.time.LocalDateTime.now());

        }
        catch(Exception ex){

            mail.setStatus("FAILED");
            mail.setFailureReason(ex.getMessage());
        }
        mailboxRepo.save(mail);

    }

    private String loadTemplate() throws Exception{

        ClassPathResource r= new ClassPathResource("templates/error-notification.html");
        return new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

    }

}