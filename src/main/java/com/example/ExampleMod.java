package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ExampleMod implements ModInitializer {
    private long lastDay = -1;
    private int goalDay = 500;
    private String normalSoundId = "minecraft:block.note_block.chime";
    private String milestoneSoundId = "minecraft:entity.wither.spawn";

    @Override
    public void onInitialize() {
        loadConfig();

        // Listens to the internal game clock safely from the server side
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.overworld() != null) {
                long currentDay = server.overworld().getDayTime() / 24000;

                if (lastDay == -1) {
                    lastDay = currentDay;
                    return;
                }

                boolean dayChanged = currentDay > lastDay;
                if (dayChanged) {
                    lastDay = currentDay;
                }

                // Sends the tracking data to every active player in the world
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (dayChanged) {
                        triggerDayNotification(player, currentDay);
                    }

                    // Persistent Action Bar Tracker (displays smoothly right above your health bar)
                    String trackerText = "Day: " + currentDay + " / " + goalDay;
                    ChatFormatting color = (currentDay >= goalDay) ? ChatFormatting.GREEN : ChatFormatting.GOLD;
                    player.displayClientMessage(Component.literal(trackerText).withStyle(color), true);
                }
            }
        });
    }

    private void triggerDayNotification(ServerPlayer player, long day) {
        Component titleText;
        Component subtitleText;
        String soundToPlay = normalSoundId;

        if (day == goalDay) {
            titleText = Component.literal("GOAL REACHED!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD, ChatFormatting.OBFUSCATED);
            subtitleText = Component.literal("You hit Day " + goalDay + "!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            soundToPlay = milestoneSoundId;
        } else {
            titleText = Component.literal("Day " + day).withStyle(ChatFormatting.GOLD);
            subtitleText = Component.literal("Keep surviving...").withStyle(ChatFormatting.GRAY);
        }

        // Sends the title screens directly to the player's client via safe network packets
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(titleText));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleText));

        try {
            player.level().playSound(
                null, player.getX(), player.getY(), player.getZ(),
                BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse(soundToPlay)),
                SoundSource.AMBIENT, 1.0f, 1.0f
            );
        } catch (Exception e) {
            player.displayClientMessage(Component.literal("Invalid custom sound ID in config!").withStyle(ChatFormatting.RED), false);
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
