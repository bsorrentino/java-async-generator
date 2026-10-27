package org.bsc.async.executor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNullElse;

public interface DefaultExecutor {


    class Builder {
        private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

        private String name;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Executor build() {

            return Executors.newCachedThreadPool(runnable ->
                    new Thread(runnable, "%s[%d]".formatted( requireNonNullElse(name, "AsyncGenerator"), ID_GENERATOR.getAndIncrement())));
        }
    }

    static Builder builder() {
        return new Builder();
    }
}
