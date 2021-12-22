package shsaddon.render;

import mtr.block.IBlock;
import mtr.data.IGui;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.Route;
import mtr.gui.ClientData;
import mtr.render.RenderTrains;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.WorldAccess;
import shsaddon.gui.IDrawing;

import java.util.*;

public class RenderPIDSSHS<T extends BlockEntity> extends BlockEntityRenderer<T> implements IGui {

    private final float Hscale = 60 ;
    private final float Vscale = 70;
    private final float totalScaledWidth;
    private final float destinationStart = 10;
    private final float destinationMaxWidth = Hscale * 20F / 16;
//    private final float platformMaxWidth;
    private final float arrivalMaxWidth = 10;
    private final int maxArrivals = 1;
    private final float maxHeight = 14;
    private final float maxWidth = 19;
    private final boolean rotate90;

    private final int reversingDwell = 20;
    private final int departingWarningTime = 40;
    private final int doorWarningTime = 55;

    private static final int SWITCH_LANGUAGE_TICKS = 60;
    private static final int SWITCH_FLASHING_TICKS = 20;
    private static final int CAR_TEXT_COLOR = 0xFF1900FF;
    private static final int CAR_TEXT_COLOR_ACCENT = 0xFFFFFFFF;
    private static final int CAR_TEXT_COLOR_WARNING = 0xFFFF7575;
    private static final int MAX_LIGHT_GLOWING = 0xa000A0;
    private static final int MAX_VIEW_DISTANCE = 16;

    private static final int startX = 19;
    private static final int startY = 14;
    private static final int startZ = 3;

    public RenderPIDSSHS(BlockEntityRenderDispatcher dispatcher, boolean rotate90) {
        super(dispatcher);
        totalScaledWidth = Hscale * maxWidth / 16;
        this.rotate90 = rotate90;
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        try {
            final WorldAccess world = entity.getWorld();
            if (world == null) {
                return;
            }

            final BlockPos pos = entity.getPos();
            final Direction facing = IBlock.getStatePropertySafe(world, pos, HorizontalFacingBlock.FACING);
            if (RenderTrains.shouldNotRender(pos, Math.min(MAX_VIEW_DISTANCE, RenderTrains.maxTrainRenderDistance), rotate90 ? null : facing)) {
                return;
            }

            try {
                final Set<Route.ScheduleEntry> schedules;

                final Platform platform = RailwayData.getClosePlatform(ClientData.PLATFORMS, pos, 5 ,0 ,6);
                if (platform == null) {
                    schedules = new HashSet<>();
                } else {
                    final Set<Route.ScheduleEntry> schedulesForPlatform = ClientData.SCHEDULES_FOR_PLATFORM.get(platform.id);
                    schedules = schedulesForPlatform == null ? new HashSet<>() : schedulesForPlatform;
                }

                final List<Route.ScheduleEntry> scheduleList = new ArrayList<>(schedules);
                Collections.sort(scheduleList);

                //language ticks
                final int languageTicks = (int) Math.floor(RenderTrains.getGameTicks()) / SWITCH_LANGUAGE_TICKS;
                final int flashingTicks = (int) Math.floor(RenderTrains.getGameTicks()) / SWITCH_FLASHING_TICKS;
                final String destinationString;

                matrices.push();
                matrices.translate(0.5, 0, 0.5);
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion((rotate90 ? 90 : 0) - facing.asRotation()));
                matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180));
                matrices.translate(2F / 16, -10F / 16, -3F / 16 - SMALL_OFFSET * 2);
                matrices.scale(1F / Vscale, 1F / Hscale, 1F / Hscale);

                final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                final VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

                if (scheduleList.size() != 0) {
                    final String[] destinationSplit = scheduleList.get(0).destination.split("\\|");
                    destinationString = IGui.textOrUntitled(destinationSplit[languageTicks % destinationSplit.length]);
                    final int constantDwell = platform.getDwellTime();
                    Route.ScheduleEntry currentSchedule = scheduleList.get(0);

                    Text arrivalText;
                    final int seconds = (int) ((currentSchedule.arrivalMillis - System.currentTimeMillis()) / 1000);
                    final boolean isCJK = destinationString.codePoints().anyMatch(Character::isIdeographic);
                    final boolean isDeparting = seconds <= - departingWarningTime;
                    if (seconds >= reversingDwell) {
                        arrivalText = Text.of(String.format("%02d", seconds / 60) + ":" + String.format("%02d", seconds % 60));
                    } else if (seconds > 0) {
                        arrivalText = new TranslatableText(isCJK ? "gui.shsaddon.arriving_cjk" : "gui.shsaddon.arriving");
                    } else if (!isDeparting){
                        arrivalText = new TranslatableText(isCJK ? "gui.shsaddon.boarding_cjk" : "gui.shsaddon.boarding");
                    } else {
                        arrivalText = new TranslatableText(seconds >= - doorWarningTime ? (isCJK ? "gui.shsaddon.departing_cjk" : "gui.shsaddon.departing") : (isCJK ? "gui.shsaddon.door_closing_cjk" : "gui.shsaddon.door_closing"));
                    }

                    matrices.push();
                    matrices.scale(1.1F, 1.1F, 1.1F);
                    final int destinationWidth = textRenderer.getWidth(destinationString);
                    if (destinationWidth > destinationMaxWidth) {
                        matrices.scale(destinationMaxWidth / destinationWidth, 1, 1);
                    }
                    IDrawing.drawStringWithFont(matrices, textRenderer, immediate, destinationString, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, 0, 10, 1, CAR_TEXT_COLOR, false, MAX_LIGHT_GLOWING, null);
                    matrices.pop();

                    if (arrivalText != null && !isDeparting) {
                        IDrawing.drawStringWithFont(matrices, textRenderer, immediate, arrivalText.getString(), HorizontalAlignment.LEFT, VerticalAlignment.CENTER, 0, 0, 1.1F, CAR_TEXT_COLOR, false, MAX_LIGHT_GLOWING, null);
                    } else if (isDeparting) {
                        IDrawing.drawStringWithFont(matrices, textRenderer, immediate, arrivalText.getString(), HorizontalAlignment.LEFT, VerticalAlignment.CENTER, 0, 0, 1.1F, flashingTicks % 2 == 0 ? CAR_TEXT_COLOR : CAR_TEXT_COLOR_WARNING, false, MAX_LIGHT_GLOWING, null);
                    }


                    matrices.push();
                    String viaString = new TranslatableText(isCJK ? "gui.shsaddon.terminating_cjk" : "gui.shsaddon.terminating").getString();
                    matrices.translate(0, 20, 0);
                    matrices.scale(0.8F, 0.8F, 0.8F);
                    if (!currentSchedule.isTerminating) {
                        viaString = "via ";
                    }
                    final int viaWidth = textRenderer.getWidth(viaString);
                    if (viaWidth > destinationMaxWidth / 0.8F) {
                        matrices.scale(destinationMaxWidth / 0.8F / viaWidth, 1, 1);
                    }
                    IDrawing.drawStringWithFont(matrices, textRenderer, immediate, viaString, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, 0, 0, 1, CAR_TEXT_COLOR, false, MAX_LIGHT_GLOWING, null);
                    matrices.pop();

                    if (scheduleList.size() > 1) {
                        final String destinationStringNext;

                        final String[] destinationSplitNext = scheduleList.get(1).destination.split("\\|");
                        final String arrivalTime;
                        destinationStringNext = IGui.textOrUntitled(destinationSplitNext[languageTicks % destinationSplitNext.length]);
                        currentSchedule = scheduleList.get(1);
                        int minimumSpacing = constantDwell + reversingDwell;
                        int secondsNext = (int) ((currentSchedule.arrivalMillis - System.currentTimeMillis()) / 1000);
                        Text arrivalTextNext = null;
                        final boolean delayed = secondsNext < reversingDwell || secondsNext - seconds <= minimumSpacing;
                        if (delayed) {
                            arrivalTextNext = secondsNext > 0 ? new TranslatableText(isCJK ? "gui.shsaddon.delayed_cjk" : "gui.shsaddon.delayed") : new TranslatableText(isCJK ? "gui.shsaddon.boarding_cjk" : "gui.shsaddon.boarding");
                            secondsNext = seconds + minimumSpacing;
                        }

                        String arrivalNext = null;
                        arrivalTime = String.format("%02d", secondsNext / 60) + ":" + String.format("%02d", secondsNext % 60);

                        if (arrivalTextNext == null) {
                            arrivalNext = new TranslatableText("gui.shsaddon.next_cjk").getString() + " / " + new TranslatableText("gui.shsaddon.next").getString() + " : " + arrivalTime + " " + destinationStringNext;
                        } else {
                            arrivalNext =  new TranslatableText("gui.shsaddon.next_cjk").getString() + " / " + new TranslatableText("gui.shsaddon.next").getString() + " : " + arrivalTextNext.getString() + " " + destinationStringNext;
                        }
                        matrices.push();
                        matrices.scale(0.7F, 0.7F, 0.7F);
                        final float destinationWidthNext = textRenderer.getWidth(arrivalNext);
                        if (destinationWidthNext > destinationMaxWidth / 0.7F) {
                            matrices.scale(destinationMaxWidth / 0.7F / destinationWidthNext, 1, 1);
                        }
                        IDrawing.drawStringWithFont(matrices, textRenderer, immediate, arrivalNext, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, 0, 55, 1, delayed ? CAR_TEXT_COLOR_WARNING : CAR_TEXT_COLOR_ACCENT, false, MAX_LIGHT_GLOWING, null);
                        matrices.pop();
                    }

                } else {
                    IDrawing.drawStringWithFont(matrices, textRenderer, immediate, new TranslatableText("gui.shsaddon.not_in_use").getString(), HorizontalAlignment.LEFT, VerticalAlignment.CENTER, 0, 25, 1, CAR_TEXT_COLOR, false, MAX_LIGHT_GLOWING, null);
                    IDrawing.drawStringWithFont(matrices, textRenderer, immediate, new TranslatableText("gui.shsaddon.not_in_use_cjk").getString(), HorizontalAlignment.LEFT, VerticalAlignment.CENTER, 0, 10, 0.5F, CAR_TEXT_COLOR, false, MAX_LIGHT_GLOWING, null);
                }

                String platformName = null;
                if (platform != null) {
                    platformName = platform.name;
                }
                IDrawing.drawStringWithFont(matrices, textRenderer, immediate, platformName != null ? platformName : "#", HorizontalAlignment.CENTER, VerticalAlignment.CENTER, -24.5F, 5.7F, 0.6F, CAR_TEXT_COLOR, false, MAX_LIGHT_GLOWING, null);
                IDrawing.drawStringWithFont(matrices, textRenderer, immediate, new TranslatableText("gui.shsaddon.platform").getString(), HorizontalAlignment.LEFT, VerticalAlignment.CENTER, -30, -5F, 2.5F, CAR_TEXT_COLOR, false, MAX_LIGHT_GLOWING, null);

                matrices.pop();
                immediate.draw();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}