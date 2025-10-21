package com.Learning.EmailReaderApplication.Service;

import com.sun.mail.imap.IMAPFolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.*;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EmailListenerService {

    private static final Logger log = LoggerFactory.getLogger(EmailListenerService.class);

    // Values injected from application.properties (good practice instead of hardcoding)
    @Value("${mail.host}")
    private String host;

    @Value("${mail.port}")
    private int port;

    @Value("${mail.username}")
    private String username;

    @Value("${mail.password}")
    private String password;

    @Value("${mail.protocol}")
    private String protocol;  // e.g. "imaps"

    // JavaMail objects
    private Store store;
    private Folder inbox;

    // Executor thread to run email listener asynchronously
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Flag to control the running state of the listener
    private volatile boolean running = true;

    /**
     * Called automatically when the Spring service starts.
     * Starts the email listener in a separate thread.
     */
    @PostConstruct
    public void start() {
        executor.submit(this::listenForEmails);
    }

    /**
     * Main email listening loop.
     * Connects to IMAP server, listens for new messages using IDLE, and retries on errors.
     */
    private void listenForEmails() {
        while (running) {
            try {
                log.info("Connecting to {}:{} as {}", host, port, username);

                // Setup mail session properties
                Properties props = new Properties();
                props.put("mail.store.protocol", protocol);
                props.put("mail." + protocol + ".ssl.enable", "true");  // enable SSL
                props.put("mail." + protocol + ".port", port);

                // Create session and connect to the mail store
                Session session = Session.getInstance(props);
                store = session.getStore(protocol);
                store.connect(host, username, password);

                // Open the INBOX folder in read-only mode
                inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);

                // Add listener for new incoming messages
                inbox.addMessageCountListener(new MessageCountAdapter() {
                    @Override
                    public void messagesAdded(MessageCountEvent event) {
                        for (Message message : event.getMessages()) {
                            try {
                                String subject = message.getSubject();
                                String body = getTextFromMessage(message);
                                log.info("📩 New Email - Subject: {}", subject);
                                log.info("📄 Body: {}", body);
                            } catch (Exception e) {
                                log.error("Error reading email", e);
                            }
                        }
                    }
                });

                // IMAP IDLE: keeps the connection open and waits for new emails in real-time
                log.info("Listening for new emails via IMAP IDLE...");
                while (running && inbox.isOpen()) {
                    try {
                        ((IMAPFolder) inbox).idle(); // waits until new mail arrives
                    } catch (FolderClosedException fce) {
                        log.warn("Folder closed, reopening...");
                        break; // exit loop so it reconnects
                    }
                }

            } catch (Exception e) {
                log.error("Error in email listener, retrying in 10s", e);
                try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
            } finally {
                // Cleanup resources before retrying
                closeQuietly(inbox);
                closeQuietly(store);
            }
        }
    }

    /**
     * Extracts plain text body from email message.
     */
    private String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    return part.getContent().toString();
                }
            }
        }
        return "";
    }

    /**
     * Safely closes a folder without throwing exceptions.
     */
    private void closeQuietly(Folder folder) {
        try {
            if (folder != null && folder.isOpen()) folder.close();
        } catch (Exception ignored) {}
    }

    /**
     * Safely closes a store without throwing exceptions.
     */
    private void closeQuietly(Store store) {
        try {
            if (store != null && store.isConnected()) store.close();
        } catch (Exception ignored) {}
    }

    /**
     * Called when the Spring app shuts down.
     * Stops the email listener and closes resources.
     */
    @PreDestroy
    public void cleanup() {
        running = false;
        executor.shutdownNow();
        closeQuietly(inbox);
        closeQuietly(store);
    }
}