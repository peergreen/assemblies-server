package com.peergreen.platform.it;

import static java.lang.String.format;

import org.apache.felix.ipojo.extender.queue.QueueService;

/**
 * User: guillaume
 * Date: 29/05/13
 * Time: 10:40
 */
public class StabilityHelper {
    public static final int INCREMENT_IN_MS = 100;
    public static final int THIRTY_SECONDS = 30000;
    public static final int DEFAULT_TIMEOUT = THIRTY_SECONDS;

    private final QueueService queueService;

    public StabilityHelper(final QueueService queueService) {
        this.queueService = queueService;
    }

    public void waitForStability() throws Exception {
        waitForStability(DEFAULT_TIMEOUT);
    }

    /**
     * This should be moved into chameleon osgi-helper module
     * @param timeout milliseconds
     * @throws Exception
     */
    public void waitForStability(long timeout) throws Exception {

        long sleepTime = 0;
        long startupTime = System.currentTimeMillis();
        do {
            long elapsedTime = System.currentTimeMillis() - startupTime;
            if (isStable()) {
                //System.out.printf("Stability reached after %d ms%n", elapsedTime);
                return;
            }

            if (elapsedTime >= timeout) {
                throw new Exception(format("Stability not reached after %d ms%n", timeout));
            }

            // Not stable, re-compute sleep time
            long nextSleepTime = sleepTime + INCREMENT_IN_MS;
            if ((elapsedTime + nextSleepTime) > timeout) {
                // Last increment is too large, shrink it to fit in the timeout boundaries
                sleepTime = timeout - sleepTime;
            } else {
                sleepTime = nextSleepTime;
            }

            //System.out.printf("Waiting for %d ms (%d elapsed)%n", sleepTime, elapsedTime);
            Thread.sleep(sleepTime);
        } while (true);

    }

    private boolean isStable() {
        return queueService.getWaiters() == 0 && queueService.getFinished() > 0;
    }

}
