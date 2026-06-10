package net.fabricmc.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ExampleMod implements ClientModInitializer {
    private long lastDay = -1;
    private int goalDay = 500;
    private String normalSoundId = "minecraft:block.note_block.chime";
    private String milestoneSoundId = "minecraft:entity.wither.spawn";

    @Override
    public void onInitializeClient() {
        loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null) {
                long currentDay = client.world.getTimeOfDay() / 24000;

                if (lastDay == -1) {
                    lastDay = currentDay;
                    return;
                }

                if (currentDay > lastDay) {
                    lastDay = currentDay;
                    triggerDayNotification(client, currentDay);
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && client.player != null && !client.options.hudHidden) {
                long currentDay = client.world.getTimeOfDay() / 24000;
                String trackerText = "Day: " + currentDay + " / " + goalDay;
                int color = (currentDay >= goalDay) ? 0x55FF55 : 0xFFAA00;
                drawContext.drawText(client.textRenderer, trackerText, 10, 10, color, true);
            }
        });
    }

    private void triggerDayNotification(MinecraftClient client, long day) {
        Text titleText;
        Text subtitleText;
        String soundToPlay = normalSoundId;

        if (day == goalDay) {
            titleText = Text.literal("GOAL REACHED!").formatted(Formatting.RED, Formatting.BOLD, Formatting.OBFUSCATED);
            subtitleText = Text.literal("You hit Day " + goalDay + "!").formatted(Formatting.GOLD, Formatting.BOLD);
            soundToPlay = milestoneSoundId;
        } else {
            titleText = Text.literal("Day " + day).formatted(Formatting.GOLD);
            subtitleText = Text.literal("Keep surviving...").formatted(Formatting.GRAY);
        }

        client.inGameHud.setTitle(titleText);
        client.inGameHud.setSubtitle(subtitleText);
        client.inGameHud.setTitleTicks(10, 70, 20);

        try {
            client.world.playSound(
                client.player.getX(), client.player.getY(), client.player.getZ(),
                Registries.SOUND_EVENT.get(Identifier.of(soundToPlay)),
                net.minecraft.sound.SoundCategory.AMBIENT, 1.0f, 1.0f, false
            );
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("Invalid custom sound ID in config!").formatted(Formatting.RED), false);
        }
    }

    private void loadConfig() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("custom_day_counter.properties");
        Properties props = new Properties();

        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
                goalDay = Integer.parseInt(props.getProperty("goal_day", "500"));
                normalSoundId = props.getProperty("normal_day_sound", "minecraft:block.note_block.chime");
                milestoneSoundId = props.getProperty("goal_reached_sound", "minecraft:entity.wither.spawn");
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        } else {
            props.setProperty("goal_day", "500");
            props.setProperty("normal_day_sound", "minecraft:block.note_block.chime");
            props.setProperty("goal_reached_sound", "minecraft:entity.wither.spawn");
            try (OutputStream out = Files.newOutputStream(configPath)) {
                props.store(out, "Custom Day Counter Configuration");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
