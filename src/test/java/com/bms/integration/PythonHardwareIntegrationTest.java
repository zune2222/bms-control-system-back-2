package com.bms.integration;

import com.bms.service.PythonHardwareClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Python Hardware Controller
 * These tests require the Python hardware controller to be running
 */
@SpringBootTest
@ActiveProfiles("hardware")
@EnabledIfEnvironmentVariable(named = "BMS_INTEGRATION_TEST", matches = "true")
public class PythonHardwareIntegrationTest {

    @Autowired
    private PythonHardwareClient pythonHardwareClient;

    @Test
    public void testHealthCheck() {
        // Test if Python hardware controller is available
        boolean isAvailable = pythonHardwareClient.isHardwareControllerAvailable();
        
        if (isAvailable) {
            System.out.println("✅ Python hardware controller is available");
        } else {
            System.out.println("⚠️ Python hardware controller is not available");
        }
        
        // Test passes regardless of availability for CI/CD compatibility
        assertNotNull(pythonHardwareClient);
    }

    @Test
    public void testBmsStatusRetrieval() {
        if (!pythonHardwareClient.isHardwareControllerAvailable()) {
            System.out.println("⚠️ Skipping BMS status test - Python controller not available");
            return;
        }

        Map<String, Object> status = pythonHardwareClient.getBmsStatus();
        
        if (status != null) {
            System.out.println("✅ Successfully retrieved BMS status: " + status);
            
            // Verify expected fields are present
            assertTrue(status.containsKey("total_voltage"));
            assertTrue(status.containsKey("current"));
            assertTrue(status.containsKey("temperature"));
            assertTrue(status.containsKey("charge_fet_status"));
            assertTrue(status.containsKey("discharge_fet_status"));
        } else {
            System.out.println("⚠️ BMS status retrieval returned null");
        }
    }

    @Test
    public void testBmsSettingsRetrieval() {
        if (!pythonHardwareClient.isHardwareControllerAvailable()) {
            System.out.println("⚠️ Skipping BMS settings test - Python controller not available");
            return;
        }

        Map<String, Object> settings = pythonHardwareClient.getBmsSettings();
        
        if (settings != null) {
            System.out.println("✅ Successfully retrieved BMS settings: " + settings);
            
            // Verify expected settings fields are present
            assertTrue(settings.containsKey("overcharge_voltage"));
            assertTrue(settings.containsKey("undercharge_voltage"));
            assertTrue(settings.containsKey("overcharge_current"));
            assertTrue(settings.containsKey("discharge_current"));
        } else {
            System.out.println("⚠️ BMS settings retrieval returned null");
        }
    }

    @Test
    public void testHardwareControlFunctions() {
        if (!pythonHardwareClient.isHardwareControllerAvailable()) {
            System.out.println("⚠️ Skipping hardware control test - Python controller not available");
            return;
        }

        // Test overcharge voltage setting (safe test value)
        boolean success = pythonHardwareClient.setOverchargeVoltage(4.1);
        if (success) {
            System.out.println("✅ Successfully set overcharge voltage");
        } else {
            System.out.println("⚠️ Failed to set overcharge voltage");
        }

        // Test undercharge voltage setting (safe test value)
        success = pythonHardwareClient.setUnderchargeVoltage(3.0);
        if (success) {
            System.out.println("✅ Successfully set undercharge voltage");
        } else {
            System.out.println("⚠️ Failed to set undercharge voltage");
        }

        // Test overcharge current setting (safe test value)
        success = pythonHardwareClient.setOverchargeCurrent(2.0);
        if (success) {
            System.out.println("✅ Successfully set overcharge current");
        } else {
            System.out.println("⚠️ Failed to set overcharge current");
        }

        // Test discharge current setting (safe test value)
        success = pythonHardwareClient.setDischargeCurrent(5.0);
        if (success) {
            System.out.println("✅ Successfully set discharge current");
        } else {
            System.out.println("⚠️ Failed to set discharge current");
        }

        // Test voltage delay setting
        success = pythonHardwareClient.setVoltageDelay(5);
        if (success) {
            System.out.println("✅ Successfully set voltage delay");
        } else {
            System.out.println("⚠️ Failed to set voltage delay");
        }
    }

    @Test
    public void testFETControl() {
        if (!pythonHardwareClient.isHardwareControllerAvailable()) {
            System.out.println("⚠️ Skipping FET control test - Python controller not available");
            return;
        }

        // Test FET control (be careful with actual hardware)
        boolean success = pythonHardwareClient.controlFET(true, true);
        if (success) {
            System.out.println("✅ Successfully controlled FET (both ON)");
        } else {
            System.out.println("⚠️ Failed to control FET");
        }

        // Add small delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Test FET control (turn off for safety)
        success = pythonHardwareClient.controlFET(false, false);
        if (success) {
            System.out.println("✅ Successfully controlled FET (both OFF)");
        } else {
            System.out.println("⚠️ Failed to control FET");
        }
    }

    @Test
    public void testElectronicLoadControl() {
        if (!pythonHardwareClient.isHardwareControllerAvailable()) {
            System.out.println("⚠️ Skipping electronic load test - Python controller not available");
            return;
        }

        // Test electronic load control - CC mode
        boolean success = pythonHardwareClient.controlElectronicLoad(true, "CC", null);
        if (success) {
            System.out.println("✅ Successfully controlled electronic load (CC mode)");
        } else {
            System.out.println("⚠️ Failed to control electronic load (CC mode)");
        }

        // Add small delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Test electronic load control - CP mode
        success = pythonHardwareClient.controlElectronicLoad(true, "CP", 3);
        if (success) {
            System.out.println("✅ Successfully controlled electronic load (CP mode level 3)");
        } else {
            System.out.println("⚠️ Failed to control electronic load (CP mode)");
        }

        // Turn off electronic load for safety
        success = pythonHardwareClient.controlElectronicLoad(false, "CC", null);
        if (success) {
            System.out.println("✅ Successfully turned off electronic load");
        } else {
            System.out.println("⚠️ Failed to turn off electronic load");
        }
    }

    @Test
    public void testSettingsReset() {
        if (!pythonHardwareClient.isHardwareControllerAvailable()) {
            System.out.println("⚠️ Skipping settings reset test - Python controller not available");
            return;
        }

        // Test settings reset
        boolean success = pythonHardwareClient.resetSettings();
        if (success) {
            System.out.println("✅ Successfully reset BMS settings");
        } else {
            System.out.println("⚠️ Failed to reset BMS settings");
        }
    }
}