package net.sourceforge.cruisecontrol.publishers.email;

import org.apache.mailet.MailAddress;
import org.masukomi.aspirin.core.MailQue;
import org.masukomi.aspirin.core.MailWatcher;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Understands whether or not emails were successfully delivered by Aspirin.
 */
public class EmailTransmissionResult implements MailWatcher {

    private boolean success = true;

    public void deliverySuccess(final MailQue queue, final MimeMessage message, final MailAddress address) {
    }

    public void deliveryFailure(final MailQue queue, final MimeMessage message, final MailAddress address,
        final MessagingException e) {
        success = false;
    }

    public void deliveryFinished(final MailQue queue, final MimeMessage message) {
        queue.terminate();
    }

    public boolean isSuccess() {
        return success;
    }
}
