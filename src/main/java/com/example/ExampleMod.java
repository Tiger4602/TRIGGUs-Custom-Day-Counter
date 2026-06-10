package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;

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
            if (client.level != null && client.player != null) {
                long currentDay = client.level.getDayTime() / 24000;

                if (lastDay == -1) {
                    lastDay = currentDay;
                    return;
                }

                if (currentDay > lastDay) {
                    lastDay = currentDay;
                    triggerDayNotification(client, currentDay);
                }

                // Persistent Action Bar Tracker (Displays safely right above your hotbar)
                String trackerText = "Day: " + currentDay + " / " + goalDay;
                ChatFormatting color = (currentDay >= goalDay) ? ChatFormatting.GREEN : ChatFormatting.GOLD;
                client.player.displayClientMessage(Component.literal(trackerText).withStyle(color), true);
            }
        });
    }

    private void triggerDayNotification(Minecraft client, long day) {
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

        if (client.gui != null) {
            client.gui.setTitle(titleText);
            client.gui.setSubtitle(subtitleText);
            client.gui.setTimes(10, 70, 20);
        }

        try {
            client.level.playLocalSound(
                client.player.getX(), client.player.getY(), client.player.getZ(),
                BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse(soundToPlay)),
                SoundSource.AMBIENT, 1.0f, 1.0f, false
            );
        } catch (Exception e) {
            client.player.displayClientMessage(Component.literal("Invalid custom sound ID in config!").withStyle(ChatFormatting.RED), false);
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
