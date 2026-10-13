package io.github.chema280101.logtech.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                TerminalSyncPayload.TYPE,
                TerminalSyncPayload.STREAM_CODEC,
                TerminalSyncPayload::handle
        );
        registrar.playToClient(
                DiscoveredVeinsPayload.TYPE,
                DiscoveredVeinsPayload.STREAM_CODEC,
                DiscoveredVeinsPayload::handle
        );
        registrar.playToClient(
                OpenVeinMapPayload.TYPE,
                OpenVeinMapPayload.STREAM_CODEC,
                OpenVeinMapPayload::handle
        );
        registrar.playToClient(
                TerminalStatsPayload.TYPE,
                TerminalStatsPayload.STREAM_CODEC,
                TerminalStatsPayload::handle
        );
        registrar.playToClient(
                ColdRetrievalPayload.TYPE,
                ColdRetrievalPayload.STREAM_CODEC,
                ColdRetrievalPayload::handle
        );
        registrar.playToServer(
                TerminalActionPayload.TYPE,
                TerminalActionPayload.STREAM_CODEC,
                TerminalActionPayload::handle
        );
        registrar.playToServer(
                TerminalTabPayload.TYPE,
                TerminalTabPayload.STREAM_CODEC,
                TerminalTabPayload::handle
        );
        registrar.playToClient(
                CoreSyncPayload.TYPE,
                CoreSyncPayload.STREAM_CODEC,
                CoreSyncPayload::handle
        );
        registrar.playToServer(
                CoreRuleUpdatePayload.TYPE,
                CoreRuleUpdatePayload.STREAM_CODEC,
                CoreRuleUpdatePayload::handle
        );
        registrar.playToServer(
                MachinePriorityPayload.TYPE,
                MachinePriorityPayload.STREAM_CODEC,
                MachinePriorityPayload::handle
        );
        registrar.playToClient(
                TerminalEnergyPayload.TYPE,
                TerminalEnergyPayload.STREAM_CODEC,
                TerminalEnergyPayload::handle
        );
        registrar.playToServer(
                TerminalPriorityPayload.TYPE,
                TerminalPriorityPayload.STREAM_CODEC,
                TerminalPriorityPayload::handle
        );
    }
}
