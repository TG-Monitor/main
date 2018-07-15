package ai.quantumsense.tgmonitor.main;

import ai.quantumsense.tgmonitor.backend.Interactor;
import ai.quantumsense.tgmonitor.backend.InteractorImpl;
import ai.quantumsense.tgmonitor.entities.Emails;
import ai.quantumsense.tgmonitor.entities.EmailsImpl;
import ai.quantumsense.tgmonitor.entities.Patterns;
import ai.quantumsense.tgmonitor.entities.PatternsImpl;
import ai.quantumsense.tgmonitor.entities.Peers;
import ai.quantumsense.tgmonitor.entities.PeersImpl;
import ai.quantumsense.tgmonitor.matching.PatternMatcherImpl;
import ai.quantumsense.tgmonitor.monitor.LoginCodePrompt;
import ai.quantumsense.tgmonitor.monitor.Monitor;
import ai.quantumsense.tgmonitor.monitor.MonitorImpl;
import ai.quantumsense.tgmonitor.notification.NotificatorImpl;
import ai.quantumsense.tgmonitor.notification.format.FormatterImpl;
import ai.quantumsense.tgmonitor.notification.send.MailgunSender;
import ai.quantumsense.tgmonitor.servicelocator.ServiceLocator;
import ai.quantumsense.tgmonitor.servicelocator.instances.EmailsLocator;
import ai.quantumsense.tgmonitor.servicelocator.instances.InteractorLocator;
import ai.quantumsense.tgmonitor.servicelocator.instances.LoginCodePromptLocator;
import ai.quantumsense.tgmonitor.servicelocator.instances.MonitorLocator;
import ai.quantumsense.tgmonitor.servicelocator.instances.PatternsLocator;
import ai.quantumsense.tgmonitor.servicelocator.instances.PeersLocator;
import ai.quantumsense.tgmonitor.telegram.TelegramImpl;
import ai.quantumsense.tgmonitor.telegram.datamapping.JsonGsonDataMapper;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Main {

    private static final String TG_API_ID = System.getenv("TG_API_ID");
    private static final String TG_API_HASH = System.getenv("TG_API_HASH");
    private static final String MAILGUN_API_KEY = System.getenv("MAILGUN_API_KEY");
    private static final String MAILGUN_DOMAIN = "quantumsense.ai";
    private static final String EMAIL_SENDING_ADDRESS = "tg-monitor@quantumsense.ai";
    private static final String EMAIL_SENDING_NAME = "TG-Monitor";
    private static final String PHONE_NUMBER = System.getenv("PHONE_NUMBER");
    private static final Set<String> PEERS = new HashSet<>(Arrays.asList(
            "alethena_official",
            "icocountdown",
            "icorankingreviews",
            "tezosico",
            "cryptobayto"
    ));
    private static final Set<String> PATTERNS = new HashSet<>(Arrays.asList(
            "bitcoin",
            "crash",
            "scam",
            "ethereum",
            "ico"
    ));
    private static final Set<String> EMAILS = new HashSet<>(Arrays.asList(
            "danielmweibel@gmail.com",
            "marco.fernandez@quantumsense.ai"
    ));

    public static void main(String[] args) {
        checkEnv();

        ServiceLocator<Monitor> monitorLocator = new MonitorLocator();
        ServiceLocator<Interactor> interactorLocator = new InteractorLocator();
        ServiceLocator<Peers> peersLocator = new PeersLocator();
        ServiceLocator<Patterns> patternsLocator = new PatternsLocator();
        ServiceLocator<Emails> emailsLocator = new EmailsLocator();
        ServiceLocator<LoginCodePrompt> loginCodePromptLocator = new LoginCodePromptLocator();

        new GuiLoginCodePrompt(loginCodePromptLocator);

        new InteractorImpl(
                new PatternMatcherImpl(interactorLocator, patternsLocator),
                new NotificatorImpl(new FormatterImpl(), new MailgunSender(MAILGUN_API_KEY, MAILGUN_DOMAIN, EMAIL_SENDING_ADDRESS, EMAIL_SENDING_NAME), emailsLocator),
                interactorLocator
        );

        new PeersImpl(
                new MonitorImpl(
                    new TelegramImpl(TG_API_ID, TG_API_HASH, new JsonGsonDataMapper(),  interactorLocator, loginCodePromptLocator),
                    monitorLocator),
                peersLocator
        );
        new PatternsImpl(patternsLocator);
        new EmailsImpl(emailsLocator);

        Monitor monitor = monitorLocator.getService();
        if (!monitor.isLoggedIn())
            monitor.login(PHONE_NUMBER);
        System.out.println("Now logged in");

        Emails emails = emailsLocator.getService();
        Patterns patterns = patternsLocator.getService();
        Peers peers = peersLocator.getService();

        System.out.println("Setting notification email addresses");
        emails.addEmails(EMAILS);
        System.out.println("Setting patterns");
        patterns.addPatterns(PATTERNS);
        System.out.println("Starting monitors");
        peers.addPeers(PEERS);

        sleep(60);

        System.out.println("Stopping monitors");
        peers.removePeers(PEERS);
    }

    private static void checkEnv() {
        String var = null;
        if (TG_API_ID == null) var = "TG_API_ID";
        else if (TG_API_HASH == null) var = "TG_API_HASH";
        else if (MAILGUN_API_KEY == null) var = "MAILGUN_API_KEY";
        else if (PHONE_NUMBER == null) var = "PHONE_NUMBER";
        if (var != null)
            throw new RuntimeException("Must set " + var + " environment variable");
    }

    private static void sleep(int sec) {
        try {
            Thread.sleep(sec * 1000);
        } catch (InterruptedException e) {

        }
    }

    private static class GuiLoginCodePrompt implements LoginCodePrompt {
        public GuiLoginCodePrompt(ServiceLocator<LoginCodePrompt> locator) {
            locator.registerService(this);
        }
        @Override
        public String promptLoginCode() {
            return JOptionPane.showInputDialog("Please enter login code");
        }
    }
}
