package postanogov.dev.mynotesnew.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    //@Async // Чтобы регистрация не тормозила, пока письмо летит
    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("mynotesservice@inbox.ru");
        message.setTo(to);
        message.setSubject("Код подтверждения MyNotes");
        message.setText("Ваш код: " + code);
        mailSender.send(message);
    }
}