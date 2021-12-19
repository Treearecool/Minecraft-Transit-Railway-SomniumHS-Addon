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

import java.util.*;

public class RenderPIDSSHS<T extends BlockEntity> extends BlockEntityRenderer<T> implements IGui {

	private final float scale;
	private final float totalScaledWidth;
	private final float destinationStart;
	private final float destinationMaxWidth;
	private final float platformMaxWidth;
	private final float arrivalMaxWidth;
	private final int maxArrivals = 1;
	private final float maxHeight = 15;
	private final float maxWidth = 19;
	private final boolean rotate90;

	private static final int SWITCH_LANGUAGE_TICKS = 80;
	private static final int CAR_TEXT_COLOR = 0xFF0000;
	private static final int CAR_TEXT_COLOR_ACCENT = 0x433CEC;
	private static final int MAX_VIEW_DISTANCE = 16;

	private static final int startX = 1;
	private static final int startY = 15;
	private static final int startZ = 16;
	
	public RenderPIDSSHS(BlockEntityRenderDispatcher dispatcher, boolean rotate90) {
		super(dispatcher);
		scale = 160 / maxHeight;
		totalScaledWidth = scale * maxWidth / 16;
		destinationStart = 0;
		destinationMaxWidth = totalScaledWidth * 0.7F;
		platformMaxWidth = 0;
		arrivalMaxWidth = totalScaledWidth - destinationStart - destinationMaxWidth - platformMaxWidth;
		this.rotate90 = rotate90;
	}

	@Override
	public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
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
			final Map<Long, String> platformIdToName = new HashMap<>();

			final Platform platform = RailwayData.getClosePlatform(ClientData.PLATFORMS, pos);
			if (platform == null) {
				schedules = new HashSet<>();
			} else {
				final Set<Route.ScheduleEntry> schedulesForPlatform = ClientData.SCHEDULES_FOR_PLATFORM.get(platform.id);
				schedules = schedulesForPlatform == null ? new HashSet<>() : schedulesForPlatform;
			}

			final List<Route.ScheduleEntry> scheduleList = new ArrayList<>(schedules);
			Collections.sort(scheduleList);

			final float carLengthMaxWidth = 0;

			//language ticks
			final int languageTicks = (int) Math.floor(RenderTrains.getGameTicks()) / SWITCH_LANGUAGE_TICKS;
			final String destinationString;
			if (scheduleList.size() != 0) {
				final String[] destinationSplit = scheduleList.get(0).destination.split("\\|");
				destinationString = IGui.textOrUntitled(destinationSplit[languageTicks % destinationSplit.length]);
			} else {
				destinationString = "";
			}

			matrices.push();
			matrices.translate(0.5, 0, 0.5);
			matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion((rotate90 ? 90 : 0) - facing.asRotation()));
			matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180));
			matrices.translate((startX - 8) / 16, -startY / 16 + 0 * maxHeight / maxArrivals / 16, (startZ - 8) / 16 - SMALL_OFFSET * 2);
			matrices.scale(1F / scale, 1F / scale, 1F / scale);

			final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

			final Route.ScheduleEntry currentSchedule = scheduleList.get(0);

			final Text arrivalText;
			final int seconds = (int) ((currentSchedule.arrivalMillis - System.currentTimeMillis()) / 1000);
			final boolean isCJK = destinationString.codePoints().anyMatch(Character::isIdeographic);
			if (seconds >= 60) {
				arrivalText = new TranslatableText(isCJK ? "gui.mtr.arrival_min_cjk" : "gui.mtr.arrival_min", seconds / 60).append(!isCJK ? "." : "");
			} else {
				arrivalText = seconds > 0 ? new TranslatableText(isCJK ? "gui.mtr.arrival_sec_cjk" : "gui.mtr.arrival_sec", seconds).append(!isCJK ? "." : "") : Text.of("Boarding");
			}

			final float newDestinationMaxWidth = destinationMaxWidth;

			matrices.push();
			matrices.translate(destinationStart, 0, 0);
			final int destinationWidth = textRenderer.getWidth(destinationString);
			if (destinationWidth > newDestinationMaxWidth) {
				matrices.scale(newDestinationMaxWidth / destinationWidth, 1, 1);
			}
			textRenderer.draw(matrices, destinationString, 0, 0, CAR_TEXT_COLOR);
			matrices.pop();

			if (arrivalText != null) {
				matrices.push();
				final int arrivalWidth = textRenderer.getWidth(arrivalText);
				if (arrivalWidth > arrivalMaxWidth) {
					matrices.translate(destinationStart + newDestinationMaxWidth + platformMaxWidth + carLengthMaxWidth, 0, 0);
					matrices.scale(arrivalMaxWidth / arrivalWidth, 1, 1);
				} else {
					matrices.translate(totalScaledWidth - arrivalWidth, 0, 0);
				}
				textRenderer.draw(matrices, arrivalText, 0, 0, CAR_TEXT_COLOR);
				matrices.pop();
			}

			matrices.pop();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}