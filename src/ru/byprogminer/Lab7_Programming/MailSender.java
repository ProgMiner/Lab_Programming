package ru.byprogminer.Lab7_Programming;

import ru.byprogminer.Lab7_Programming.logging.Loggers;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MailSender {

    private static final String MAIL_PROPERTIES = "/mail.properties";

    private static final Logger log = Loggers.getClassLogger(MailSender.class);
    private static final Properties properties = new Properties();
    private static final Session session;

    static {
        try {
            properties.load(MailSender.class.getResourceAsStream(MAIL_PROPERTIES));
            session = Session.getInstance(properties, null);
        } catch (Throwable e) {
            log.log(Level.SEVERE, "an error occurred while loading mail properties", e);
            throw new RuntimeException("an error occurred while loading mail properties", e);
        }
    }

    public static void send(String email, String subject, String text) throws MessagingException {
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(properties.getProperty("mail.smtp.user")));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
        message.setSubject(subject);
        message.setText(text, null, "html");

        final Transport transport = session.getTransport();
        transport.connect(properties.getProperty("mail.smtp.host"), properties.getProperty("mail.smtp.user"),
                properties.getProperty("mail.smtp.password"));
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
    }
}
