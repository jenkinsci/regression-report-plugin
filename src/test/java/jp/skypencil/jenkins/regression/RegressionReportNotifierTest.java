package jp.skypencil.jenkins.regression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.CaseResult.Status;
import hudson.tasks.test.AbstractTestResultAction;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CaseResult.class)
public class RegressionReportNotifierTest {
	private BuildListener listener;
	private Launcher launcher;
	private AbstractBuild<?, ?> build;

	@Before
	public void setUp() throws Exception {
		listener = mock(BuildListener.class);
		launcher = mock(Launcher.class);
		build = mock(AbstractBuild.class);
		PrintStream logger = mock(PrintStream.class);
		doReturn("").when(build).getUrl();
		doReturn(logger).when(listener).getLogger();
	}

	@Test
	public void testCompileErrorOccured() throws InterruptedException, IOException {
		doReturn(null).when(build).getTestResultAction();
		RegressionReportNotifier notifier = new RegressionReportNotifier("", false);

		assertThat(notifier.perform(build, launcher, listener), is(true));
	}

	@Test
	public void testSend() throws InterruptedException, MessagingException {
		makeRegression();

		RegressionReportNotifier notifier = new RegressionReportNotifier("author@mail.com", false);
		MockedMailSender mailSender = new MockedMailSender();
		notifier.setMailSender(mailSender);

		assertThat(notifier.perform(build, launcher, listener), is(true));
		assertThat(mailSender.getSentMessage(), is(notNullValue()));
		Address[] to = mailSender.getSentMessage().getRecipients(RecipientType.TO);
		assertThat(to.length, is(1));
		assertThat(to[0].toString(), is(equalTo("author@mail.com")));
	}

	@Test
	public void testSendToCulprits() throws InterruptedException, MessagingException {
		makeRegression();

		RegressionReportNotifier notifier = new RegressionReportNotifier("author@mail.com", true);
		MockedMailSender mailSender = new MockedMailSender();
		notifier.setMailSender(mailSender);

		assertThat(notifier.perform(build, launcher, listener), is(true));
		assertThat(mailSender.getSentMessage(), is(notNullValue()));
		Address[] to = mailSender.getSentMessage().getRecipients(RecipientType.TO);
		assertThat(to.length, is(2));
		assertThat(to[0].toString(), is(equalTo("author@mail.com")));
		assertThat(to[1].toString(), is(equalTo("culprit@mail.com")));
	}

	private void makeRegression() {
		AbstractTestResultAction<?> result = mock(AbstractTestResultAction.class);
		doReturn(result).when(build).getTestResultAction();
		doReturn(Result.FAILURE).when(build).getResult();
		User culprit = mock(User.class);
		doReturn("culprit").when(culprit).getId();
		doReturn(new ChangeLogSetMock(build).withChangeBy(culprit)).when(build).getChangeSet();

		CaseResult failedTest = mock(CaseResult.class);
		doReturn(Status.REGRESSION).when(failedTest).getStatus();
		List<CaseResult> failedTests = Lists.newArrayList(failedTest);
		doReturn(failedTests).when(result).getFailedTests();
	}

	private static final class MockedMailSender implements RegressionReportNotifier.MailSender {
		private MimeMessage sentMessage;

		@Override
		public void send(MimeMessage message) throws MessagingException {
			sentMessage = message;
		}

		public MimeMessage getSentMessage() {
			return sentMessage;
		}
	}
}
