package org.jctools.queues.atomic;

import org.jctools.queues.MpmcArrayQueueSanityTest;
import org.jctools.queues.QueueSanityTest;
import org.jctools.queues.spec.ConcurrentQueueSpec;
import org.jctools.queues.spec.Ordering;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

import static org.jctools.util.JvmInfo.CPUs;

@RunWith(Parameterized.class)

public class AtomicMpmcArrayQueueSanityTest extends MpmcArrayQueueSanityTest {
    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        ArrayList<Object[]> list = new ArrayList<Object[]>();

        // Mpmc minimal size is 2
        list.add(makeAtomic(0, 0, 2, Ordering.FIFO, null));
        list.add(makeAtomic(0, 0, SIZE, Ordering.FIFO, null));
        return list;
    }

    public AtomicMpmcArrayQueueSanityTest(ConcurrentQueueSpec spec, Queue<Integer> queue) {
        super(spec, queue);
    }

}
