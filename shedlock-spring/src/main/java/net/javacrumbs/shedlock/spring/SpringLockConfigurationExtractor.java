/**
 * Copyright 2009-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.javacrumbs.shedlock.spring;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockConfigurationExtractor;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.requireNonNull;

/**
 * Extracts configuration form Spring scheduled task. Is able to extract information from
 * <ol>
 * <li>Annotation based scheduler</li>
 * </ol>
 */
public class SpringLockConfigurationExtractor implements LockConfigurationExtractor {
    static final Duration DEFAULT_LOCK_AT_MOST_FOR = Duration.of(1, ChronoUnit.HOURS);
    private final Logger logger = LoggerFactory.getLogger(SpringLockConfigurationExtractor.class);

    private final TemporalAmount defaultLockAtMostFor;

    public SpringLockConfigurationExtractor() {
        this(DEFAULT_LOCK_AT_MOST_FOR);
    }

    public SpringLockConfigurationExtractor(TemporalAmount defaultLockAtMostFor) {
        this.defaultLockAtMostFor = requireNonNull(defaultLockAtMostFor);
    }

    @Override
    public Optional<LockConfiguration> getLockConfiguration(Runnable task) {
        if (task instanceof ScheduledMethodRunnable) {
            Method method = ((ScheduledMethodRunnable) task).getMethod();
            SchedulerLock annotation = AnnotationUtils.findAnnotation(method, SchedulerLock.class);
            if (shouldLock(annotation)) {
                return Optional.of(new LockConfiguration(annotation.name(), now().plus(getLockAtMostFor(annotation))));
            }
        } else {
            logger.debug("Unknown task type " + task);
        }
        return Optional.empty();
    }

    TemporalAmount getLockAtMostFor(SchedulerLock annotation) {
        long valueFromAnnotation = annotation.lockAtMostFor();
        return valueFromAnnotation >= 0 ? Duration.of(valueFromAnnotation, MILLIS) : defaultLockAtMostFor;
    }


    private boolean shouldLock(SchedulerLock annotation) {
        return annotation != null;
    }
}
