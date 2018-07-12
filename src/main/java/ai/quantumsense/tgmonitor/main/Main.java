package ai.quantumsense.tgmonitor.main;

import ai.qantumsense.tgmonitor.telethon.TelegramImpl;
import ai.qantumsense.tgmonitor.telethon.datamapping.JsonGsonDataMapper;
import ai.quantumsense.tgmonitor.backend.AuthenticatorImpl;
import ai.quantumsense.tgmonitor.backend.ExecutorImpl;
import ai.quantumsense.tgmonitor.backend.InteractorFactory;
import ai.quantumsense.tgmonitor.backend.InteractorImpl;
import ai.quantumsense.tgmonitor.backend.InteractorImplFactory;
import ai.quantumsense.tgmonitor.backend.Notificator;
import ai.quantumsense.tgmonitor.backend.PatternMatcher;
import ai.quantumsense.tgmonitor.backend.Telegram;
import ai.quantumsense.tgmonitor.matching.PatternMatcherImpl;
import ai.quantumsense.tgmonitor.monitor.LoginCodeReader;
import ai.quantumsense.tgmonitor.monitor.control.Authenticator;
import ai.quantumsense.tgmonitor.monitor.control.Executor;
import ai.quantumsense.tgmonitor.monitor.control.MonitorControl;
import ai.quantumsense.tgmonitor.monitor.control.MonitorControlImpl;
import ai.quantumsense.tgmonitor.monitor.data.MonitorData;
import ai.quantumsense.tgmonitor.monitor.data.MonitorDataFactory;
import ai.quantumsense.tgmonitor.monitor.data.MonitorDataImpl;
import ai.quantumsense.tgmonitor.monitor.data.MonitorDataImplFactory;
import ai.quantumsense.tgmonitor.notification.NotificatorImpl;
import ai.quantumsense.tgmonitor.notification.Sender;
import ai.quantumsense.tgmonitor.notification.format.FormatterImpl;
import ai.quantumsense.tgmonitor.notification.send.MailgunSender;

import javax.swing.JOptionPane;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Main {

    private static final String TG_API_ID = System.getenv("TG_API_ID");
    private static final String TG_API_HASH = System.getenv("TG_API_HASH");
    private static final String MAILGUN_API_KEY = System.getenv("MAILGUN_API_KEY");
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
            "danielmweibel@gmail.com"
    ));

    public static void main(String[] args) {
        checkEnv();

        System.out.println("Creating monitor");
        LoginCodeReader loginCodeReader = () -> JOptionPane.showInputDialog("Please enter login code");
        InteractorFactory interactorFactory = new InteractorImplFactory();
        MonitorDataFactory monitorDataFactory = new MonitorDataImplFactory();
        Telegram tg = new TelegramImpl(TG_API_ID, TG_API_HASH, new JsonGsonDataMapper(), loginCodeReader, interactorFactory);
        PatternMatcher patternMatcher = new PatternMatcherImpl(interactorFactory, monitorDataFactory);
        Sender sender = new MailgunSender(MAILGUN_API_KEY, "quantumsense.ai", "tg-monitor@quantumsense.ai", "TG-Monitor");
        Notificator notificator = new NotificatorImpl(new FormatterImpl(), sender, monitorDataFactory);
        Authenticator auth = new AuthenticatorImpl(tg);
        Executor exec = new ExecutorImpl(tg, monitorDataFactory);
        new InteractorImpl(patternMatcher, notificator);
        MonitorControl monitorControl = new MonitorControlImpl(auth, exec);
        MonitorData monitorData = new MonitorDataImpl();

        System.out.println("Setting monitor data");
        monitorData.setPeers(PEERS);
        monitorData.setPatterns(PATTERNS);
        monitorData.setEmails(EMAILS);

        System.out.println("Monitor state: " + monitorControl.getState());
        System.out.println("Logging in with " + PHONE_NUMBER + "...");
        monitorControl.login(PHONE_NUMBER);
        System.out.println("Login done");
        System.out.println("Monitor state: " + monitorControl.getState());

        System.out.println("Starting monitor");
        monitorControl.start();
        System.out.println("Monitor state: " + monitorControl.getState());

        sleep(30);

        System.out.println("Pausing monitor");
        monitorControl.pause();
        System.out.println("Monitor state: " + monitorControl.getState());

        sleep(10);

        System.out.println("Starting monitor");
        monitorControl.start();
        System.out.println("Monitor state: " + monitorControl.getState());

        sleep(30);

        System.out.println("Pausing monitor");
        monitorControl.pause();
        System.out.println("Monitor state: " + monitorControl.getState());

        System.out.println("Logging out");
        monitorControl.logout();
        System.out.println("Monitor state: " + monitorControl.getState());
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
}
