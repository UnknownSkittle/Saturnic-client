package namidevelopment.kiriyaga.nami.mixin;

import namidevelopment.kiriyaga.api.core.socials.SocialsStatus;
import namidevelopment.kiriyaga.nami.impl.feature.miscellaneous.BetterTabFeature;
import namidevelopment.kiriyaga.nami.impl.util.SaturnicBadge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.stream.Collectors;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@Mixin(PlayerTabOverlay.class)
public abstract class MixinPlayerTabOverlay {

    @Shadow @Final private static Comparator<PlayerInfo> PLAYER_COMPARATOR;
    @Shadow protected abstract Component decorateName(PlayerInfo entry, MutableComponent name);

    private final Map<String, SocialsStatus> cachedSocials = new HashMap<>();
    private long lastSocialCacheUpdate = 0;

    @SuppressWarnings("FieldCanBeLocal")
    private final long socialsCacheInterval = 1000;

    private void updateSocialsCache() {
        long now = System.currentTimeMillis();
        if (now - lastSocialCacheUpdate < socialsCacheInterval) return;

        cachedSocials.clear();

        SOCIALS_SERVICE.getSocials().forEach((name, status) -> {
            if (name != null && status != null)
                cachedSocials.put(name.toLowerCase(), status);
        });

        lastSocialCacheUpdate = now;
    }

    private boolean isSocial(PlayerInfo entry) {
        String name = entry.getProfile().name().toLowerCase();
        return cachedSocials.containsKey(name);
    }

    private SocialsStatus getStatus(PlayerInfo entry) {
        String name = entry.getProfile().name().toLowerCase();
        return cachedSocials.getOrDefault(name, null);
    }

    private String getColorByStatus(SocialsStatus status) {
        if (status == null) return "{global}";

        return switch (status) {
            case FRIEND -> "{friend}";
            case ENEMY -> "{enemy}";
            default -> "{global}";
        };
    }

    @Inject(method = "getPlayerInfos", at = @At("HEAD"), cancellable = true)
    private void collectPlayerEntries(CallbackInfoReturnable<List<PlayerInfo>> info) {
        BetterTabFeature betterTab = FEATURE_SERVICE.getStorage() != null
                ? FEATURE_SERVICE.getStorage().getByClass(BetterTabFeature.class)
                : null;

        if (betterTab == null || !betterTab.isEnabled()) return;
        if (MC == null || MC.player == null || MC.player.connection == null) return;

        Collection<PlayerInfo> allEntries = MC.player.connection.getListedOnlinePlayers();
        List<PlayerInfo> result;

        if (betterTab.socialsOnly.get()) {
            updateSocialsCache();

            int limit = betterTab.limit.get();
            result = new ArrayList<>(limit);

            for (PlayerInfo entry : allEntries) {
                if (isSocial(entry)) {
                    result.add(entry);
                    if (result.size() >= limit) break;
                }
            }

            result.sort(PLAYER_COMPARATOR);

        } else {
            result = allEntries.stream()
                    .limit(betterTab.limit.get())
                    .sorted(PLAYER_COMPARATOR)
                    .collect(Collectors.toList());
        }

        info.setReturnValue(result);
    }

    @Inject(method = "getNameForDisplay", at = @At("HEAD"), cancellable = true)
    private void getNameForDisplay(PlayerInfo entry, CallbackInfoReturnable<Component> info) {
        BetterTabFeature betterTab = FEATURE_SERVICE.getStorage() != null ? FEATURE_SERVICE.getStorage().getByClass(BetterTabFeature.class) : null;

        if (betterTab == null || !betterTab.isEnabled()) return;
        if (!betterTab.highlight.get()) return;
        String playerName = entry.getProfile().name();
        SocialsStatus status = SOCIALS_SERVICE.getStatus(playerName);
        if (status == null) return;

        String color = switch (status) {
            case FRIEND -> "{friend}";
            case ENEMY -> "{enemy}";
            default -> "{global}";
        };

        info.setReturnValue(decorateName(entry, CAT_FORMAT.format(color + playerName)));
    }

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void addSaturnicBadge(PlayerInfo entry, CallbackInfoReturnable<Component> info) {
        if (MC == null || MC.player == null || info.getReturnValue() == null) return;
        if (!MC.player.getUUID().equals(entry.getProfile().id())) return;

        info.setReturnValue(SaturnicBadge.appendTo(info.getReturnValue()));
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render(GuiGraphics drawContext, int width, Scoreboard scoreboard, @Nullable Objective objective, CallbackInfo ci) {
        BetterTabFeature betterTab = FEATURE_SERVICE.getStorage().getByClass(BetterTabFeature.class);

        if (betterTab != null && betterTab.isEnabled()) {
            float scale = betterTab.scale.get().floatValue();

            drawContext.pose().pushMatrix();

            float centerX = width / 2f;
            float centerY = 10f;

            drawContext.pose().translate(centerX, centerY);
            drawContext.pose().scale(scale, scale);
            drawContext.pose().translate(-centerX, -centerY);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void render2(GuiGraphics drawContext, int width, Scoreboard scoreboard, @Nullable Objective objective, CallbackInfo ci) {
        BetterTabFeature betterTab = FEATURE_SERVICE.getStorage().getByClass(BetterTabFeature.class);

        if (betterTab != null && betterTab.isEnabled()) {
            drawContext.pose().popMatrix();
        }
    }
}
