package net.sourceforge.cruisecontrol.publishers.email;

import junit.framework.TestCase;
import org.masukomi.aspirin.core.MailQue;
import org.masukomi.aspirin.core.MailWatcher;

public class EmailTransmissionResultTest extends TestCase {

    private class MockMailQue extends MailQue {
        private boolean terminated;

        @Override
        public void terminate() {
            super.terminate();
            terminated = true;
        }

        public boolean isTerminated() {
            return terminated;
        }
    }

    public void testShouldImplementMailWatcherInterface() throws Exception {
        final EmailTransmissionResult result = new EmailTransmissionResult();

        assertEquals(true, result instanceof MailWatcher);
    }

    public void testShouldBeSuccessfulWhenThereAreNoDeliveryFailures() throws Exception {
        final EmailTransmissionResult result = new EmailTransmissionResult();

        assertEquals(true, result.isSuccess());
    }

    public void testShouldNotBeSuccessfulWhenThereIsAtLeastOneDeliveryFailure() throws Exception {
        final EmailTransmissionResult result = new EmailTransmissionResult();
        result.deliveryFailure(null, null, null, null);

        assertEquals(false, result.isSuccess());
    }

    public void testShouldTerminateMailQueWhenFinished() throws Exception {
        final MockMailQue queue = new MockMailQue();

        final EmailTransmissionResult result = new EmailTransmissionResult();
        result.deliveryFinished(queue, null);

        assertEquals(true, queue.isTerminated());
    }
}
