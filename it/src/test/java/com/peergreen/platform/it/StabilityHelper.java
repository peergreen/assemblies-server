package com.peergreen.platform.it;

import static java.lang.String.format;

import org.apache.felix.ipojo.extender.queue.QueueService;

/**
 * User: guillaume
 * Date: 29/05/13
 * Time: 10:40
 */
public class StabilityHelper {
    private final QueueService queueService;

    public StabilityHelper(final QueueService queueService) {
        this.queueService = queueService;
    }

    /**
     * This should be moved into chameleon osgi-helper module
     * @param timeout milliseconds
     * @throws Exception
     */
    public void waitForStability(long timeout) throws Exception {

        if (isStable()) {
            return;
        }
        long segment = timeout / 10;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(segment);
            if (isStable()) {
                return;
            }
        }

        throw new Exception(format("Stability not reached after %d ms", timeout));
    }

    private boolean isStable() {
        return queueService.getWaiters() == 0 && queueService.getFinished() > 0;
    }

}
