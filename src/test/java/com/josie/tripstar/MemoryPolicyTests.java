package com.josie.tripstar;

import com.josie.tripstar.utils.MemoryPolicy;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class MemoryPolicyTests {
    @Test
    void decayIsIndependentOfReadFrequency() {
        LocalDateTime begin = LocalDateTime.of(2026, 1, 1, 0, 0);
        BigDecimal once = MemoryPolicy.decay(new BigDecimal("8"), begin, begin.plusDays(2));
        BigDecimal daily = MemoryPolicy.decay(MemoryPolicy.decay(new BigDecimal("8"), begin, begin.plusDays(1)),
                begin.plusDays(1), begin.plusDays(2));
        assertThat(once).isEqualByComparingTo(daily).isEqualByComparingTo("7.5272");
        assertThat(MemoryPolicy.decay(new BigDecimal("8"), begin, begin.minusDays(1))).isEqualByComparingTo("8");
    }
    @Test
    void mergeBoostsAndCapsWeights() {
        assertThat(MemoryPolicy.merge(new BigDecimal("5"), new BigDecimal("4"))).isEqualByComparingTo("6.6");
        assertThat(MemoryPolicy.merge(new BigDecimal("9"), new BigDecimal("8"))).isEqualByComparingTo("10");
    }
}
