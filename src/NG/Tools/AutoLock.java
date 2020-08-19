package NG.Tools;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Geert van Ieperen created on 7-3-2020.
 */
public interface AutoLock extends Lock {
    /**
     * locks until closed
     * @return a section object that unlocks this lock when it closes
     */
    AutoLock.Section open();

    interface Section extends AutoCloseable {
        @Override
        void close(); // without exception
    }

    /**
     * An {@link AutoCloseable} lock to use in Java 7 try-with-resource
     * @author Geert van Ieperen created on 27-2-2020.
     */
    class Instance extends ReentrantLock implements AutoLock {
        @Override
        public Section open() {
            lock();
            return this::unlock;
        }
    }

    class Wrapper implements AutoLock {
        private final Lock source;

        public Wrapper(Lock source) {
            this.source = source;
        }

        public Instance.Section open() {
            source.lock();
            return source::unlock;
        }

        // obligatory Java bloat //

        @Override
        public void lock() {
            source.lock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            source.lockInterruptibly();
        }

        @Override
        public boolean tryLock() {
            return source.tryLock();
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return source.tryLock(time, unit);
        }

        @Override
        public void unlock() {
            source.unlock();
        }

        @Override
        public Condition newCondition() {
            return source.newCondition();
        }
    }
}
