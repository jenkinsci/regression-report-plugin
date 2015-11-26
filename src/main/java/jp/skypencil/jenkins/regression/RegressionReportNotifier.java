package jp.skypencil.jenkins.regression;

import static com.google.common.collect.Iterables.transform;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Mailer;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;

import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @version 1.0
 * @author eller86 (Kengo TODA)
 */
public final class RegressionReportNotifier extends Notifier {
    static interface MailSender {
        void send(MimeMessage message) throws MessagingException;
    }

    private static final int MAX_RESULTS_PER_MAIL = 20;
    private final String recipients;
    private final boolean sendToCulprits;
    private final boolean whenRegression;
    private final boolean whenProgression;
    private final boolean whenNewFailed;
    private final boolean whenNewPassed;

    private MailSender mailSender = new RegressionReportNotifier.MailSender() {
        @Override
        public void send(MimeMessage message) throws MessagingException {
            Transport.send(message);
        }
    };

    public RegressionReportNotifier(String recipients, boolean sendToCulprits) {
        this.recipients = recipients;
        this.sendToCulprits = sendToCulprits;
        this.whenRegression = true;
        this.whenProgression = false;
        this.whenNewFailed = false;
        this.whenNewPassed = false;       
    }

    @DataBoundConstructor
    public RegressionReportNotifier(
            String recipients, 
            boolean sendToCulprits,
            boolean whenRegression,
            boolean whenProgression,
            boolean whenNewFailed,
            boolean whenNewPassed
            ) {
        this.recipients = recipients;
        this.sendToCulprits = sendToCulprits;
        this.whenRegression = whenRegression;
        this.whenProgression = whenProgression;
        this.whenNewFailed = whenNewFailed;
        this.whenNewPassed = whenNewPassed;       
    }

    @VisibleForTesting
    void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getRecipients() {
        return recipients;
    }

    public boolean getSendToCulprits() {
        return sendToCulprits;
    }

    public boolean getWhenRegression() {
        return whenRegression;
    }

    public boolean getWhenProgression() {
        return whenProgression;
    }

    public boolean getWhenNewFailed() {
        return whenNewFailed;
    }

    public boolean getWhenNewPassed() {
        return whenNewPassed;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException {
        PrintStream logger = listener.getLogger();

        if (build.getResult() == Result.SUCCESS) {
            logger.println("regression reporter doesn't run because build is success.");
            return true;
        }

        AbstractTestResultAction<?> testResultAction = build
                .getAction(AbstractTestResultAction.class);
        if (testResultAction == null) {
            // maybe compile error occurred
            logger.println("regression reporter doesn't run because test doesn\'t run.");
            return true;
        }

        logger.println("regression reporter starts now...");

        List<CaseResult> regressionedTests = listRegressions(testResultAction);
        
        List<Tuple<CaseResult, CaseResult>> testTuples = new ArrayList<Tuple<CaseResult, CaseResult>>();
        AbstractBuild<?, ?> prevBuild = build.getPreviousBuild();
        if (prevBuild != null) {
            testTuples = RegressionReportHelper.matchTestsBetweenBuilds(build, prevBuild);
        }

        List<Tuple<CaseResult, CaseResult>> progressionTuples = Lists.newArrayList(Iterables.filter(testTuples, new ProgressionPredicate()));
        List<CaseResult> progressions = Lists.newArrayList(Iterables.transform(progressionTuples, new TupleToFirst()));

        List<Tuple<CaseResult, CaseResult>> newTestTuples = Lists.newArrayList(Iterables.filter(testTuples, new NewTestPredicate()));
        List<CaseResult> newTests = Lists.newArrayList(Iterables.transform(newTestTuples, new TupleToFirst()));

        List<CaseResult> newTestsPassed = Lists.newArrayList(Iterables.filter(newTests, new PassedPredicate()));
        List<CaseResult> newTestsFailed = Lists.newArrayList(Iterables.filter(newTests, new FailedPredicate()));

        writeToConsole(regressionedTests, listener);
        try {
            mailReport(regressionedTests, progressions, newTestsFailed, newTestsPassed, recipients, listener, build);
        } catch (MessagingException e) {
            e.printStackTrace(listener.error("failed to send mails."));
        }

        logger.println("regression reporter ends.");
        return true;
    }

    private List<CaseResult> listRegressions(
            AbstractTestResultAction<?> testResultAction) {
        List<? extends TestResult> failedTest = testResultAction.getFailedTests();
        Iterable<? extends TestResult> filtered = Iterables.filter(failedTest, new RegressionPredicate());
        List<CaseResult> regressionedTests =
                Lists.newArrayList(Iterables.transform(filtered, new TestResultToCaseResult()));
        return regressionedTests;
    }

    private void writeToConsole(List<CaseResult> regressions,
            BuildListener listener) {
        if (regressions.isEmpty()) {
            return;
        }

        PrintStream oStream = listener.getLogger();
        // TODO link to test result page
        for (CaseResult result : regressions) {
            // listener.hyperlink(url, text)
            oStream.printf("[REGRESSION]%s - description: %s%n",
                    result.getFullName(), result.getErrorDetails());
        }
    }

    private void mailReport(
            List<CaseResult> regressions,
            List<CaseResult> progressions,
            List<CaseResult> newTestsFailed,
            List<CaseResult> newTestsPassed, 
            String recipients,
            BuildListener listener,
            AbstractBuild<?, ?> build
            ) throws MessagingException {

        if (
            (regressions.isEmpty() || !whenRegression) &&
            (progressions.isEmpty() || !whenProgression) &&
            (newTestsFailed.isEmpty() || !whenNewFailed) &&
            (newTestsPassed.isEmpty() || !whenNewPassed)
            ) {
            return;
        }

        // TODO link to test result page
        StringBuilder builder = new StringBuilder();
        String rootUrl = "";
        Session session = null;
        InternetAddress adminAddress = null;
        if (Jenkins.getInstance() != null) {
            rootUrl = Jenkins.getInstance().getRootUrl();
            session = Mailer.descriptor().createSession();
            adminAddress = new InternetAddress(
                    JenkinsLocationConfiguration.get().getAdminAddress());
        }
        builder.append(Util.encode(rootUrl));
        builder.append(Util.encode(build.getUrl()));
        builder.append("\n\n");

        if (whenRegression) {
            builder.append(regressions.size() + " regressions found.");
            appendTests(regressions, builder);
        }

        if (whenProgression) {
            builder.append(progressions.size() + " progressions found.");
            appendTests(progressions, builder);
        }

        if (whenNewPassed) {
            builder.append(newTestsPassed.size() + " passing new tests found.");
            appendTests(newTestsPassed, builder);
        }

        if (whenNewFailed) {
            builder.append(newTestsFailed.size() + " failing new tests found.");
            appendTests(newTestsFailed, builder);
        }

        List<Address> recipentList = parse(recipients, listener);
        if (sendToCulprits) {
            recipentList.addAll(loadAddrOfCulprits(build, listener));
        }

        MimeMessage message = new MimeMessage(session);
        message.setSubject(Messages.RegressionReportNotifier_MailSubject());
        message.setRecipients(RecipientType.TO,
                recipentList.toArray(new Address[recipentList.size()]));
        message.setContent("", "text/plain");
        message.setFrom(adminAddress);
        message.setText(builder.toString());
        message.setSentDate(new Date());

        mailSender.send(message);
    }

    private Set<Address> loadAddrOfCulprits(AbstractBuild<?, ?> build,
            BuildListener listener) {
        Set<User> authorSet = Sets.newHashSet(transform(build.getChangeSet(),
                new ChangeSetToAuthor()));
        Set<Address> addressSet = Sets.newHashSet(transform(authorSet,
                new UserToAddr(listener.getLogger())));
        return addressSet;
    }

    private List<Address> parse(String recipients, BuildListener listener) {
        List<Address> list = Lists.newArrayList();
        StringTokenizer tokens = new StringTokenizer(recipients);
        while (tokens.hasMoreTokens()) {
            String address = tokens.nextToken();
            try {
                list.add(new InternetAddress(address));
            } catch (AddressException e) {
                e.printStackTrace(listener.error(e.getMessage()));
            }
        }

        return list;
    }

    private void appendTests(List<CaseResult> tests, StringBuilder builder) {
        builder.append("\n");
        for (int i = 0, max = Math.min(tests.size(), MAX_RESULTS_PER_MAIL); i < max; ++i) {
            // to save heap to avoid OOME.
            CaseResult result = tests.get(i);
            builder.append("  ");
            builder.append(result.getFullName());
            builder.append("\n");
        }
        if (tests.size() > MAX_RESULTS_PER_MAIL) {
            builder.append("  ...");
            builder.append("\n");
        }
    }

    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.RegressionReportNotifier_DisplayName();
        }
    }
}
