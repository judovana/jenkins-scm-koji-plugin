package org.fakekoji.jobmanager;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ManagementUtils {

    public static <T, U> BiConsumer<T, U> managementBiConsumerWrapper(ManagementBiConsumer<T, U> consumer) {
        return (key, value) -> {
            try {
                consumer.accept(key, value);
            } catch (ManagementException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T> Consumer<T> managementConsumerWrapper(ManagementConsumer<T> consumer) {
        return i -> {
            try {
                consumer.accept(i);
            } catch (ManagementException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public interface ManagementBiConsumer<T, U> {

        void accept(T t, U u) throws ManagementException;
    }

    public interface ManagementConsumer<T> {

        void accept(T t) throws ManagementException;
    }

}
