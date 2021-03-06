package org.dbzl.schedule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DivisionTest {

    @Test
    public final void testDivisionalWeeks(){
        Assertions.assertEquals(3, Division.NORTH_KAI.calculateDivisionalWeek(0));
    }
}
