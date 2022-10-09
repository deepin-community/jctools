/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jctools.queues;

import java.util.Queue;

import org.jctools.queues.MessagePassingQueue.Consumer;
import org.jctools.queues.MessagePassingQueue.ExitCondition;
import org.jctools.queues.MessagePassingQueue.Supplier;
import org.jctools.queues.MessagePassingQueue.WaitStrategy;
import org.jctools.util.UnsafeAccess;
import org.jctools.util.UnsafeRefArrayAccess;

abstract class FFBufferL1Pad<E> extends ConcurrentCircularArrayQueue<E> {
    public long p10, p11, p12, p13, p14, p15, p16;
    public long p30, p31, p32, p33, p34, p35, p36, p37;

    public FFBufferL1Pad(int capacity) {
        super(capacity);
    }
}

abstract class FFBufferTailField<E> extends FFBufferL1Pad<E> {
    protected long consumerIndex;

    public FFBufferTailField(int capacity) {
        super(capacity);
    }
}

abstract class FFBufferL2Pad<E> extends FFBufferTailField<E> {
    public long p20, p21, p22, p23, p24, p25, p26;
    public long p30, p31, p32, p33, p34, p35, p36, p37;

    public FFBufferL2Pad(int capacity) {
        super(capacity);
    }
}

abstract class FFBufferHeadField<E> extends FFBufferL2Pad<E> {
    protected long producerIndex;

    public FFBufferHeadField(int capacity) {
        super(capacity);
    }
}

abstract class FFBufferL3Pad<E> extends FFBufferHeadField<E> {
    public long p40, p41, p42, p43, p44, p45, p46;
    public long p30, p31, p32, p33, p34, p35, p36, p37;

    public FFBufferL3Pad(int capacity) {
        super(capacity);
    }
}

public final class FFBuffer<E> extends FFBufferL3Pad<E> implements Queue<E> {
    private final static long P_INDEX_OFFSET;
    private final static long C_INDEX_OFFSET;
    static {
        try {
            P_INDEX_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(FFBufferHeadField.class
                    .getDeclaredField("producerIndex"));
            C_INDEX_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(FFBufferTailField.class
                    .getDeclaredField("consumerIndex"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public FFBuffer(final int capacity) {
        super(capacity);
    }

    public final long lvConsumerIndex() {
        return UnsafeAccess.UNSAFE.getLongVolatile(this, C_INDEX_OFFSET);
    }

    public final long lvProducerIndex() {
        return UnsafeAccess.UNSAFE.getLongVolatile(this, P_INDEX_OFFSET);
    }

    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }

        final E[] lb = buffer;
        final long t = consumerIndex;
        final long offset = calcElementOffset(t);
        if (null != UnsafeRefArrayAccess.lvElement(lb, offset)) { // read acquire
            return false;
        }
        UnsafeRefArrayAccess.soElement(lb, offset, e); // write release
        consumerIndex = t + 1;
        return true;
    }

    public E poll() {
        final long offset = calcElementOffset(producerIndex);
        final E[] lb = buffer;
        final E e = UnsafeRefArrayAccess.lvElement(lb, offset); // write acquire
        if (null == e) {
            return null;
        }
        UnsafeRefArrayAccess.soElement(lb, offset, null); // read release
        producerIndex++;
        return e;
    }

    @Override
    public E peek() {
        long currentHead = lvProducerIndex();
        return UnsafeRefArrayAccess.lvElement(buffer, calcElementOffset(currentHead));
    }

    @Override
	public boolean relaxedOffer(E message) {
		return offer(message);
	}

	@Override
	public E relaxedPoll() {
		return poll();
	}

	@Override
	public E relaxedPeek() {
		return peek();
	}

    @Override
    public int drain(Consumer<E> c) {
        final int limit = capacity();
        return drain(c,limit);
    }

    @Override
    public int fill(Supplier<E> s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drain(Consumer<E> c, int limit) {
        for (int i=0;i<limit;i++) {
            E e = relaxedPoll();
            if(e==null){
                return i;
            }
            c.accept(e);
        }
        return limit;
    }

    @Override
    public int fill(Supplier<E> s, int limit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void drain(Consumer<E> c,
            WaitStrategy wait,
            ExitCondition exit) {
        int idleCounter = 0;
        while (exit.keepRunning()) {
            E e = relaxedPoll();
            if(e==null){
                idleCounter = wait.idle(idleCounter);
                continue;
            }
            idleCounter = 0;
            c.accept(e);
        }
    }

    @Override
    public void fill(Supplier<E> s,
            WaitStrategy wait,
            ExitCondition exit) {
        int idleCounter = 0;
        while (exit.keepRunning()) {
            E e = s.get();
            while (!relaxedOffer(e)) {
                idleCounter = wait.idle(idleCounter);
                continue;
            }
            idleCounter = 0;
        }
    }
}
