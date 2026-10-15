package io.github.chema280101.logtech.machine.firmware;

import io.github.chema280101.logtech.machine.HeatModel;
import io.github.chema280101.logtech.network.EnergyPriority;
import io.github.chema280101.logtech.network.PriorityEnergyDistributor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FirmwareEffectsTest {

    @Test
    @DisplayName("Valores base sin firmware (NONE)")
    void testNoneFirmware() {
        assertEquals(1.0, FirmwareEffects.getSpeedMultiplier(FirmwareType.NONE));
        assertEquals(1.0, FirmwareEffects.getEnergyMultiplier(FirmwareType.NONE));
        assertEquals(1.0, FirmwareEffects.getHeatMultiplier(FirmwareType.NONE));
        assertEquals(40, FirmwareEffects.calculateEnergyCost(40, FirmwareType.NONE, 0.5, 2.0));
        assertEquals(100, FirmwareEffects.calculateRequestedEnergy(100, 1.0, FirmwareType.NONE, 0.5, 2.0));
    }

    @Test
    @DisplayName("Firmware Eco: velocidad x0.7, energía x0.5, calor x0.5")
    void testEcoFirmware() {
        assertEquals(0.7, FirmwareEffects.getSpeedMultiplier(FirmwareType.ECO));
        assertEquals(0.5, FirmwareEffects.getEnergyMultiplier(FirmwareType.ECO));
        assertEquals(0.5, FirmwareEffects.getHeatMultiplier(FirmwareType.ECO));

        assertEquals(20, FirmwareEffects.calculateEnergyCost(40, FirmwareType.ECO, 0.5, 2.0));
        assertEquals(50, FirmwareEffects.calculateRequestedEnergy(100, 1.0, FirmwareType.ECO, 0.5, 2.0));
        assertEquals(0.7, FirmwareEffects.calculateEffectiveSpeed(FirmwareType.ECO, 1.0, 0.7, 1.5), 0.001);
        assertEquals(0.35, FirmwareEffects.calculateEffectiveSpeed(FirmwareType.ECO, 0.5, 0.7, 1.5), 0.001);
    }

    @Test
    @DisplayName("Firmware Turbo: velocidad x1.5, energía x2.0, calor x3.0")
    void testTurboFirmware() {
        assertEquals(1.5, FirmwareEffects.getSpeedMultiplier(FirmwareType.TURBO));
        assertEquals(2.0, FirmwareEffects.getEnergyMultiplier(FirmwareType.TURBO));
        assertEquals(3.0, FirmwareEffects.getHeatMultiplier(FirmwareType.TURBO));

        assertEquals(80, FirmwareEffects.calculateEnergyCost(40, FirmwareType.TURBO, 0.5, 2.0));
        assertEquals(200, FirmwareEffects.calculateRequestedEnergy(100, 1.0, FirmwareType.TURBO, 0.5, 2.0));
        assertEquals(1.5, FirmwareEffects.calculateEffectiveSpeed(FirmwareType.TURBO, 1.0, 0.7, 1.5), 0.001);
        assertEquals(0.75, FirmwareEffects.calculateEffectiveSpeed(FirmwareType.TURBO, 0.5, 0.7, 1.5), 0.001);
    }

    @Test
    @DisplayName("Firmware Seguro: va a tope hasta 85, corta a 85, se reanuda a 30 y nunca ralentiza")
    void testSafeFirmwareBehaviorWithHeatModel() {
        HeatModel model = new HeatModel();
        FirmwareType safe = FirmwareType.SAFE;

        model.setOverheatThreshold(FirmwareEffects.getOverheatThreshold(safe, 100.0, 85.0));
        model.setResumeThreshold(FirmwareEffects.getResumeThreshold(safe, 40.0, 30.0));
        model.setSlowdownThreshold(FirmwareEffects.getSlowdownThreshold(safe, 70.0, 85.0));
        model.setSlowdownSpeed(FirmwareEffects.getSlowdownSpeed(safe, 0.5));

        // Por debajo de 85 °C trabaja a velocidad completa (1.0x), sin ralentizarse a 70 °C
        model.setHeat(75.0);
        assertEquals(1.0, model.getSpeedMultiplier(), "A 75 °C el firmware Seguro debe continuar a velocidad 1.0x");

        model.setHeat(84.0);
        assertEquals(1.0, model.getSpeedMultiplier(), "A 84 °C el firmware Seguro continúa a 1.0x");

        // Al alcanzar o superar 85 °C se detiene (overheated)
        model.tick(true, 2.0, 1.0, 0.0); // sube de 84 a 86
        assertTrue(model.isOverheated(), "A 86 °C (>= 85) debe entrar en sobrecalentamiento");
        assertEquals(0.0, model.getSpeedMultiplier(), "Sobrecalentada su velocidad debe ser 0.0x");

        // Enfriándose: a 50 °C o 40 °C todavía sigue parada (umbral de reanudación es 30 °C)
        model.setHeat(50.0);
        model.tick(false, 0.0, 1.0, 2.0);
        assertTrue(model.isOverheated(), "A 48 °C aún no debe reanudarse (requiere <= 30)");

        model.setHeat(35.0);
        model.tick(false, 0.0, 1.0, 2.0);
        assertTrue(model.isOverheated(), "A 33 °C aún no debe reanudarse");

        // Al alcanzar <= 30 °C se reanuda automáticamente
        model.setHeat(30.0);
        model.tick(false, 0.0, 1.0, 0.0);
        assertFalse(model.isOverheated(), "A <= 30 °C debe reactivarse");
        assertEquals(1.0, model.getSpeedMultiplier(), "Al reactivarse debe operar inmediatamente a 1.0x");
    }

    @Test
    @DisplayName("Firmware Selectivo: filtro de automatización por tolva/tubería")
    void testSelectiveAutomationExtraction() {
        String itemDust = "logtech:neoferrite_dust";
        String itemSludge = "logtech:sludge";

        // Con firmware selectivo filtrando por "sludge"
        assertTrue(FirmwareEffects.canExtractAutomated(FirmwareType.SELECTIVE, itemSludge, itemSludge));
        assertFalse(FirmwareEffects.canExtractAutomated(FirmwareType.SELECTIVE, itemSludge, itemDust));

        // Si el firmware selectivo aún no tiene filtro configurado, no bloquea
        assertTrue(FirmwareEffects.canExtractAutomated(FirmwareType.SELECTIVE, null, itemDust));

        // Otros firmwares no filtran la salida
        assertTrue(FirmwareEffects.canExtractAutomated(FirmwareType.TURBO, itemSludge, itemDust));
        assertTrue(FirmwareEffects.canExtractAutomated(FirmwareType.ECO, itemSludge, itemDust));
        assertTrue(FirmwareEffects.canExtractAutomated(FirmwareType.NONE, itemSludge, itemDust));
    }

    @Test
    @DisplayName("Reparto de energía T34 con máquinas en Turbo")
    void testPriorityEnergyDistributionWithTurboMachines() {
        PriorityEnergyDistributor distributor = new PriorityEnergyDistributor(0);

        // Máquina A (Normal, sin firmware): déficit 100 -> pide 100
        int reqA = FirmwareEffects.calculateRequestedEnergy(100, 1.0, FirmwareType.NONE, 0.5, 2.0);
        assertEquals(100, reqA);

        // Máquina B (Normal, con Turbo): déficit 100 -> pide 200
        int reqB = FirmwareEffects.calculateRequestedEnergy(100, 1.0, FirmwareType.TURBO, 0.5, 2.0);
        assertEquals(200, reqB);

        Map<String, PriorityEnergyDistributor.Request<String>> requests = new LinkedHashMap<>();
        requests.put("Crusher_Normal", new PriorityEnergyDistributor.Request<>("Crusher_Normal", reqA, EnergyPriority.NORMAL));
        requests.put("Crusher_Turbo", new PriorityEnergyDistributor.Request<>("Crusher_Turbo", reqB, EnergyPriority.NORMAL));

        // Con presupuesto de 150 FE: se reparte proporcionalmente a su demanda (1:2)
        PriorityEnergyDistributor.DistributionResult<String> result = distributor.distribute(requests, 150);
        assertEquals(50, result.allocations().get("Crusher_Normal").allocatedEnergy());
        assertEquals(100, result.allocations().get("Crusher_Turbo").allocatedEnergy());
        assertEquals(150, result.totalDistributed());
    }
}
