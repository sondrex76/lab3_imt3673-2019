package com.example.lab3;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MainActivityTest {
    @Test
    public void TestgetHeight() {
        MainActivity m = new MainActivity();

        assertEquals("Expected ", 1.28, Math.round(m.getHeight(5)), 2);
        assertTrue(5 == m.getSpeed(4, 3, 0));
    }
}
