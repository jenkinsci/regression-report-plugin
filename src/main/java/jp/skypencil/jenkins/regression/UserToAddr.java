package jp.skypencil.jenkins.regression;

import java.io.PrintStream;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import hudson.model.User;
import hudson.tasks.Mailer;

import com.google.common.base.Function;
import com.google.common.base.Strings;

/**
 * Convert User to its email address. Returned value can be null.
 */
final class UserToAddr implements Function<User, Address> {
    private final PrintStream logger;
    private final Function<String, Address> idToAddr;

    UserToAddr(PrintStream logger) {
        this.logger = logger;
        this.idToAddr = new UserIdToAddr(logger);
    }

    @Override
    public Address apply(User user) {
        Mailer.UserProperty mailProperty = user.getProperty(Mailer.UserProperty.class);
        if (mailProperty == null) {
            return idToAddr.apply(user.getId());
        }

        String address = mailProperty.getAddress();
        if (Strings.isNullOrEmpty(address)) {
            return idToAddr.apply(user.getId());
        }

        try {
            return new InternetAddress(address);
        } catch (AddressException e) {
            logger.format("Email address of this user (%s) is invalid. I will try to use 'user-id + default-suffix' instead.%n", user.getId());
            e.printStackTrace(logger);
            return idToAddr.apply(user.getId());
        }
    }
}