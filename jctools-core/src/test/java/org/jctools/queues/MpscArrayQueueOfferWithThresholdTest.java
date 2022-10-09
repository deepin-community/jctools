package org.jctools.queues;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MpscArrayQueueOfferWithThresholdTest {

    private MpscArrayQueue<Integer> queue;

    @Before
    public void setUp() throws Exception {
        this.queue = new MpscArrayQueue<Integer>(16);
    }

    @Test
    public void testOfferWithThreshold() {
        int i;
        for (i = 0; i < 8; ++i) {
            //Offers succeed because current size is below the HWM.
            Assert.assertTrue(this.queue.offerIfBelowThreshold(i, 8));
        }
        //Not anymore, our offer got rejected.
        Assert.assertFalse(this.queue.offerIfBelowThreshold(i, 8));
        Assert.assertFalse(this.queue.offerIfBelowThreshold(i, 7));
        Assert.assertFalse(this.queue.offerIfBelowThreshold(i, 1));
        Assert.assertFalse(this.queue.offerIfBelowThreshold(i, 0));

        //Also, the threshold is dynamic and different levels can be set for
        //different task priorities.
        Assert.assertTrue(this.queue.offerIfBelowThreshold(i, 9));
        Assert.assertTrue(this.queue.offerIfBelowThreshold(i, 16));
    }

}
