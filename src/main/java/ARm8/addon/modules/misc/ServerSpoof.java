package ARm8.addon.modules.misc;

import io.netty.buffer.Unpooled;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixin.CustomPayloadC2SPacketAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.ResourcePackSendS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

public class ServerSpoof extends Module {
	private final SettingGroup sgGeneral = settings.getDefaultGroup();

	private final Setting<String> brand = sgGeneral.add(new StringSetting.Builder()
		.name("brand")
		.description("Specify the brand that will be send to the server.")
		.defaultValue("vanilla")
		.build()
	);

	private final Setting<Boolean> resourcePack = sgGeneral.add(new BoolSetting.Builder()
		.name("resource-pack")
		.description("Spoof accepting server resource pack.")
		.defaultValue(false)
		.build()
	);

	public ServerSpoof() {
		super(Categories.Misc, "server-spoof+", "Spoof client brand and/or resource pack.");

		MeteorClient.EVENT_BUS.subscribe(new Listener());
	}

	private class Listener {
		@EventHandler
		private void onPacketSend(PacketEvent.Send event) {
			if (isActive()) {
				if (event.packet instanceof CustomPayloadC2SPacket) {
					CustomPayloadC2SPacketAccessor packet = (CustomPayloadC2SPacketAccessor) event.packet;
					Identifier id = packet.getChannel();

					if (id.equals(CustomPayloadC2SPacket.BRAND)) {
						packet.setData(new PacketByteBuf(Unpooled.buffer()).writeString(brand.get()));
					} else if (StringUtils.containsIgnoreCase(packet.getData().toString(StandardCharsets.UTF_8), "fabric") && brand.get().equalsIgnoreCase("fabric")) {
						event.cancel();
					} else if (id.toString().equals("fabric:registry/sync")) {
						event.cancel();
					} else if (id.toString().equals("minecraft:register")) {
						event.cancel();
					}
				}
			}
		}
		@EventHandler
		private void onPacketSent(PacketEvent.Sent event) {
			if (event.packet instanceof CustomPayloadS2CPacket payload) {
				if (payload.getChannel().toString().equals("fabric:registry/sync")) {
					event.setCancelled(true);
				}
				else if (payload.getChannel().toString().equals("minecraft:register")) {
					event.cancel();
				}
			}
		}

		@EventHandler
		private void onPacketRecieve(PacketEvent.Receive event) {
			if (!isActive()) return;
			if (resourcePack.get() && event.packet instanceof ResourcePackSendS2CPacket packet) {
				event.cancel();
				MutableText msg = Text.literal("This server has ");
				msg.append(packet.isRequired() ? "a required " : "an optional ");
				MutableText link = Text.literal("resource pack");
				link.setStyle(link.getStyle()
					.withColor(Formatting.BLUE)
					.withUnderline(true)
					.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, packet.getURL()))
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to download")))
				);
				msg.append(link);
				msg.append(".");
				info(msg);
			}
			if (event.packet instanceof CustomPayloadS2CPacket payload) {
				if (payload.getChannel().toString().equals("fabric:registry/sync")) {
					event.cancel();
				}
			}
		}
	}
}
