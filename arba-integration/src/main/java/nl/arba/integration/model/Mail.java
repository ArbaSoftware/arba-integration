package nl.arba.integration.model;

import java.util.ArrayList;
import java.util.List;

public class Mail {
    private String subject;
    private byte[] content;
    private ArrayList<String> recipients = new ArrayList<>();
    private String from;

    public Mail(String subject, String from) {
        this.subject = subject;
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public String getFrom() {
        return from;
    }

    public void addRecipient(String recipient) {
        recipients.add(recipient);
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }
}
