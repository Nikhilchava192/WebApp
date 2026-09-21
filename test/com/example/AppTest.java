package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppTest {

    @Test
    void testAppExecution() {
        // Basic smoke test to verify test execution in Maven
        boolean isPipelineWorking = true;
        assertTrue(isPipelineWorking, "The pipeline smoke test should evaluate to true");
    }

    @Test
    void testSampleLogic() {
        String expected = "Java-Web-App";
        String actual = "Java-Web-App";
        assertEquals(expected, actual, "Application name should match");
    }
}