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

import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.annotations.VisibleForTesting;
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
	private MailSender mailSender = new RegressionReportNotifier.MailSender() {
	    @Override
	    public void send(MimeMessage message) throws MessagingException {
	        Transport.send(message);
	    }
	};

	@DataBoundConstructor
	public RegressionReportNotifier(String recipients, boolean sendToCulprits) {
		this.recipients = recipients;
		this.sendToCulprits = sendToCulprits;
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

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException {
		PrintStream logger = listener.getLogger();

		if (build.getResult() == Result.SUCCESS) {
			logger.println("regression reporter doesn't run because build is success.");
			return true;
		}

		AbstractTestResultAction<?> testResultAction = build.getTestResultAction();
		if (testResultAction == null) {
			// maybe compile error occurred
			logger.println("regression reporter doesn't run because test doesn\'t run.");
			return true;
		}

		logger.println("regression reporter starts now...");
		List<CaseResult> failedTest = testResultAction.getFailedTests();
		List<CaseResult> regressionedTests = filterRegressions(failedTest);

		writeToConsole(regressionedTests, listener);
		try {
			mailReport(regressionedTests, recipients, listener, build);
		} catch (MessagingException e) {
			e.printStackTrace(listener.error("failed to send mails."));
		}

		logger.println("regression reporter ends.");
		return true;
	}

	private void writeToConsole(List<CaseResult> regressions, BuildListener listener) {
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

	private void mailReport(List<CaseResult> regressions, String recipients, BuildListener listener, AbstractBuild<?, ?> build) throws MessagingException {
		if (regressions.isEmpty()) {
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
		    adminAddress = new InternetAddress(Mailer.descriptor().getAdminAddress());
		}
		builder.append(Util.encode(rootUrl));
		builder.append(Util.encode(build.getUrl()));
		builder.append("\n\n");
		builder.append(regressions.size() + " regressions found.");
		builder.append("\n");
		for (int i = 0, max = Math.min(regressions.size(), MAX_RESULTS_PER_MAIL); i < max; ++i) {	// to save heap to avoid OOME.
			CaseResult result = regressions.get(i);
			builder.append("  ");
			builder.append(result.getFullName());
			builder.append("\n");
		}
		if (regressions.size() > MAX_RESULTS_PER_MAIL) {
			builder.append("  ...");
			builder.append("\n");
		}
		List<Address> recipentList = parse(recipients, listener);
		if (sendToCulprits) {
		    recipentList.addAll(loadAddrOfCulprits(build, listener));
		}

		MimeMessage message = new MimeMessage(session);
		message.setSubject(Messages.RegressionReportNotifier_MailSubject());
		message.setRecipients(RecipientType.TO, recipentList.toArray(new Address[recipentList.size()]));
		message.setContent("", "text/plain");
		message.setFrom(adminAddress);
		message.setText(builder.toString());
		message.setSentDate(new Date());

		mailSender.send(message);
	}

	private Set<Address> loadAddrOfCulprits(AbstractBuild<?, ?> build,
			BuildListener listener) {
		Set<User> authorSet = Sets.newHashSet(transform(
				build.getChangeSet(),
				new ChangeSetToAuthor()));
		Set<Address> addressSet = Sets.newHashSet(transform(
				authorSet, new UserToAddr(listener.getLogger())));
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

	@VisibleForTesting
	List<CaseResult> filterRegressions(List<CaseResult> fails) {
		List<CaseResult> filtered = Lists.newArrayList();

		for (CaseResult result : fails) {
			if (result.getStatus().isRegression()) {
				filtered.add(result);
			}
		}

		return filtered;
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
