package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.item.ScannerItem;
import io.github.chema280101.logtech.ore.DepositType;
import io.github.chema280101.logtech.ore.VeinInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Pantalla de visualización 2D para las vetas de mineral descubiertas por el jugador.
 * Muestra una vista centrada en el jugador con círculos para cada veta coloreados según su tipo.
 * Todos los datos mostrados representan el estado exacto en el momento de apertura de la pantalla.
 */
public class VeinMapScreen extends Screen {

    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 4.0;

    private final List<VeinInfo> veins;
    private final double playerStartX;
    private final double playerStartZ;
    private final float playerStartYaw;

    private double zoom = 1.0;
    private double panOffsetX = 0.0;
    private double panOffsetY = 0.0;

    public VeinMapScreen(List<VeinInfo> snapshotVeins) {
        super(Component.translatable("gui.logtech.vein_map.title"));
        this.veins = new ArrayList<>(snapshotVeins);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.playerStartX = mc.player.getX();
            this.playerStartZ = mc.player.getZ();
            this.playerStartYaw = mc.player.getYRot();
        } else {
            this.playerStartX = 0.0;
            this.playerStartZ = 0.0;
            this.playerStartYaw = 0.0F;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            this.zoom = Math.min(MAX_ZOOM, this.zoom * 1.25);
            return true;
        } else if (scrollY < 0) {
            this.zoom = Math.max(MIN_ZOOM, this.zoom / 1.25);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 || button == 1 || button == 2) {
            this.panOffsetX += dragX;
            this.panOffsetY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_R) {
            this.panOffsetX = 0.0;
            this.panOffsetY = 0.0;
            this.zoom = 1.0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fondo atenuado general
        guiGraphics.fill(0, 0, this.width, this.height, 0xC00E1218);

        int mapMarginX = 24;
        int mapMarginTop = 36;
        int mapMarginBottom = 30;

        int mapLeft = mapMarginX;
        int mapTop = mapMarginTop;
        int mapRight = this.width - mapMarginX;
        int mapBottom = this.height - mapMarginBottom;

        int mapWidth = mapRight - mapLeft;
        int mapHeight = mapBottom - mapTop;
        int mapCenterX = mapLeft + mapWidth / 2;
        int mapCenterY = mapTop + mapHeight / 2;

        // Marco del mapa (fondo oscuro técnico)
        guiGraphics.fill(mapLeft, mapTop, mapRight, mapBottom, 0xEE141923);

        // Habilitar recorte para que nada sobresalga del visor
        guiGraphics.enableScissor(mapLeft, mapTop, mapRight, mapBottom);

        // Líneas de cuadrícula / ejes de radar respecto al jugador
        int playerScreenX = mapCenterX + (int) Math.round(this.panOffsetX);
        int playerScreenY = mapCenterY + (int) Math.round(this.panOffsetY);

        // Cuadrícula sutil de 64 bloques
        int gridStep = (int) Math.round(64 * this.zoom);
        if (gridStep >= 16) {
            int startX = playerScreenX % gridStep;
            while (startX < mapLeft) startX += gridStep;
            for (int gx = startX; gx < mapRight; gx += gridStep) {
                guiGraphics.fill(gx, mapTop, gx + 1, mapBottom, 0x184A607A);
            }

            int startY = playerScreenY % gridStep;
            while (startY < mapTop) startY += gridStep;
            for (int gy = startY; gy < mapBottom; gy += gridStep) {
                guiGraphics.fill(mapLeft, gy, mapRight, gy + 1, 0x184A607A);
            }
        }

        // Ejes cardinales principales pasando por el jugador
        guiGraphics.fill(mapLeft, playerScreenY, mapRight, playerScreenY + 1, 0x303880A8);
        guiGraphics.fill(playerScreenX, mapTop, playerScreenX + 1, mapBottom, 0x303880A8);

        // Radio de escaneo del escáner según configuración
        int scanRadiusPixels = (int) Math.round(io.github.chema280101.logtech.LogTechConfig.SCANNER_RADIUS.get() * this.zoom);
        if (scanRadiusPixels > 0) {
            drawCircleOutline(guiGraphics, playerScreenX, playerScreenY, scanRadiusPixels, 0x2400E5FF);
        }

        VeinInfo hoveredVein = null;
        boolean mouseInMap = mouseX >= mapLeft && mouseX <= mapRight && mouseY >= mapTop && mouseY <= mapBottom;

        // Renderizar las vetas descubiertas
        for (VeinInfo vein : this.veins) {
            int veinScreenX = mapCenterX + (int) Math.round((vein.centerX() - this.playerStartX) * this.zoom + this.panOffsetX);
            int veinScreenY = mapCenterY + (int) Math.round((vein.centerZ() - this.playerStartZ) * this.zoom + this.panOffsetY);
            int screenRadius = Math.max(3, (int) Math.round(vein.radius() * this.zoom));

            boolean isHovered = false;
            if (mouseInMap) {
                double distSq = Math.pow(mouseX - veinScreenX, 2) + Math.pow(mouseY - veinScreenY, 2);
                if (distSq <= Math.pow(Math.max(screenRadius, 5), 2)) {
                    isHovered = true;
                    hoveredVein = vein;
                }
            }

            boolean isNeoferrite = vein.getType() == DepositType.NEOFERRITE;
            int fillColor = isNeoferrite
                    ? (isHovered ? 0x9000E5FF : 0x5500D4FF)
                    : (isHovered ? 0x90E040FB : 0x55E040FB);
            int borderColor = isNeoferrite ? 0xFF00E5FF : 0xFFE040FB;

            drawFilledCircle(guiGraphics, veinScreenX, veinScreenY, screenRadius, fillColor, borderColor);
            // Punto central de la veta
            guiGraphics.fill(veinScreenX - 1, veinScreenY - 1, veinScreenX + 2, veinScreenY + 2, borderColor);
        }

        // Renderizar el jugador (punto blanco y flecha de rotación)
        drawPlayerMarker(guiGraphics, playerScreenX, playerScreenY, this.playerStartYaw);

        // Fin del recorte del mapa
        guiGraphics.disableScissor();

        // Borde decorativo exterior del visor
        drawMapBorder(guiGraphics, mapLeft, mapTop, mapRight, mapBottom);

        // Textos del encabezado
        Component title = Component.translatable("gui.logtech.vein_map.title");
        guiGraphics.drawCenteredString(this.font, title, this.width / 2, 10, 0xFFFFFF);

        Component snapshotNotice = Component.translatable("gui.logtech.vein_map.snapshot_notice");
        guiGraphics.drawCenteredString(this.font, snapshotNotice, this.width / 2, 22, 0x8FA4B8);

        // Barra inferior de estado y ayuda
        if (this.veins.isEmpty()) {
            Component emptyText = Component.translatable("gui.logtech.vein_map.no_veins");
            guiGraphics.drawCenteredString(this.font, emptyText, this.width / 2, this.height - 20, 0xDAA520);
        } else {
            String helpText = Component.translatable(
                    "gui.logtech.vein_map.help",
                    this.veins.size(),
                    this.zoom * 100.0
            ).getString();
            guiGraphics.drawCenteredString(this.font, Component.literal(helpText), this.width / 2, this.height - 20, 0x7E92A2);
        }

        // Tooltip del jugador si se pasa el ratón por encima
        boolean hoveredPlayer = mouseInMap && Math.hypot(mouseX - playerScreenX, mouseY - playerScreenY) <= 6;
        if (hoveredPlayer && hoveredVein == null) {
            List<Component> playerTooltip = List.of(
                    Component.translatable("gui.logtech.vein_map.player").withStyle(s -> s.withBold(true).withColor(0xFFFFFF)),
                    Component.literal("X: " + (int) Math.floor(this.playerStartX) + " | Z: " + (int) Math.floor(this.playerStartZ)).withStyle(s -> s.withColor(0xAAAAAA))
            );
            guiGraphics.renderComponentTooltip(this.font, playerTooltip, mouseX, mouseY);
        }

        // Tooltip de la veta si se pasa el ratón por encima
        if (hoveredVein != null) {
            renderVeinTooltip(guiGraphics, hoveredVein, mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderVeinTooltip(GuiGraphics guiGraphics, VeinInfo vein, int mouseX, int mouseY) {
        boolean isNeoferrite = vein.getType() == DepositType.NEOFERRITE;
        String typeColorPrefix = isNeoferrite ? "§b" : "§d";
        String typeName = isNeoferrite ? vein.getType().getSpanishName() : vein.getType().getSpanishName();

        int remaining = vein.remainingReserve();
        int initial = vein.initialReserve();
        int percent = initial > 0 ? (int) Math.round((remaining * 100.0) / initial) : 0;
        int dist = (int) Math.round(Math.hypot(vein.centerX() - this.playerStartX, vein.centerZ() - this.playerStartZ));

        List<Component> lines = new ArrayList<>();
        // Título del mineral
        lines.add(Component.literal(typeColorPrefix + typeName + " §7(" + vein.id() + ")"));
        // Pureza
        lines.add(Component.translatable("gui.logtech.vein_map.purity", vein.purity()).withStyle(s -> s.withColor(0xE0E0E0)));
        // Coordenadas y distancia
        lines.add(Component.translatable("gui.logtech.vein_map.coordinates", vein.centerX(), vein.centerZ())
                .append(Component.literal(" §7(" + dist + "m)")).withStyle(s -> s.withColor(0xCCCCCC)));
        // Porcentaje y reserva
        lines.add(Component.translatable("gui.logtech.vein_map.reserve", String.format("%,d", remaining), String.format("%,d", initial), percent)
                .withStyle(s -> s.withColor(0x00FF88)));
        // Indicación del valor en el momento de apertura
        lines.add(Component.translatable("gui.logtech.vein_map.static_value_note").withStyle(s -> s.withItalic(true).withColor(0x888888)));

        guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private void drawPlayerMarker(GuiGraphics graphics, int px, int py, float yaw) {
        // Círculo central blanco con borde negro
        drawFilledCircle(graphics, px, py, 3, 0xFFFFFFFF, 0xFF000000);

        // Flecha o puntero de orientación en rojo
        double rad = Math.toRadians(yaw);
        int tipX = px - (int) Math.round(Math.sin(rad) * 7);
        int tipY = py + (int) Math.round(Math.cos(rad) * 7);
        graphics.fill(tipX - 1, tipY - 1, tipX + 2, tipY + 2, 0xFFFF3333);
    }

    private void drawMapBorder(GuiGraphics graphics, int left, int top, int right, int bottom) {
        int borderColor = 0xFF354457;
        int cornerColor = 0xFF00E5FF;

        // Bordes principales
        graphics.fill(left - 1, top - 1, right + 1, top, borderColor);
        graphics.fill(left - 1, bottom, right + 1, bottom + 1, borderColor);
        graphics.fill(left - 1, top - 1, left, bottom + 1, borderColor);
        graphics.fill(right, top - 1, right + 1, bottom + 1, borderColor);

        // Acentos de esquinas de alta tecnología
        graphics.fill(left - 1, top - 1, left + 4, top, cornerColor);
        graphics.fill(left - 1, top - 1, left, top + 4, cornerColor);

        graphics.fill(right - 4, top - 1, right + 1, top, cornerColor);
        graphics.fill(right, top - 1, right + 1, top + 4, cornerColor);

        graphics.fill(left - 1, bottom, left + 4, bottom + 1, cornerColor);
        graphics.fill(left - 1, bottom - 3, left, bottom + 1, cornerColor);

        graphics.fill(right - 4, bottom, right + 1, bottom + 1, cornerColor);
        graphics.fill(right, bottom - 3, right + 1, bottom + 1, cornerColor);
    }

    private void drawFilledCircle(GuiGraphics graphics, int cx, int cy, int radius, int fillColor, int borderColor) {
        if (radius <= 0) return;

        // Relleno horizontal hilera por hilera
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
            graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, fillColor);
        }

        // Borde circular
        drawCircleOutline(graphics, cx, cy, radius, borderColor);
    }

    private void drawCircleOutline(GuiGraphics graphics, int cx, int cy, int radius, int borderColor) {
        if (radius <= 0) return;

        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
            graphics.fill(cx - dx, cy + dy, cx - dx + 1, cy + dy + 1, borderColor);
            graphics.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, borderColor);
        }
        for (int dx = -radius; dx <= radius; dx++) {
            int dy = (int) Math.round(Math.sqrt((double) radius * radius - (double) dx * dx));
            graphics.fill(cx + dx, cy - dy, cx + dx + 1, cy - dy + 1, borderColor);
            graphics.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, borderColor);
        }
    }
}
