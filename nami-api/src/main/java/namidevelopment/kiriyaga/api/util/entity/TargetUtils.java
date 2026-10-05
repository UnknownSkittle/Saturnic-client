package namidevelopment.kiriyaga.api.util.entity;

import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.TargetFeatureConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.ShulkerBullet;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class TargetUtils {
    public static List<Entity> getTargets(int limit) {
        TargetFeatureConfig targetFeature = FeatureContractService.get(TargetFeatureConfig.class);
        if (targetFeature == null)
            return List.of();
        return getTargets(limit, targetFeature.getTargetRange());
    }

    public static List<Entity> getTargets(int limit, double range) {
        TargetFeatureConfig targetFeature = FeatureContractService.get(TargetFeatureConfig.class);
        if (MC.player == null || MC.level == null || targetFeature == null || limit < 1 || range < 0)
            return List.of();

        double maxRange = Math.min(targetFeature.getTargetRange(), range);
        List<Entity> candidates = EntityUtils.getAllEntities().stream()
                .filter(e -> e != MC.player)
                .filter(e -> e.distanceToSqr(MC.player) <= maxRange * maxRange)
                .filter(e -> {
                    if (e instanceof LivingEntity living) {
                        if (!living.isAlive() || e.tickCount < targetFeature.getMinTicksExisted())
                            return false;
                        return (targetFeature.targetPlayers() && e instanceof Player && !SOCIALS_SERVICE.isFriend(e.getName().getString()))
                                || (targetFeature.targetHostiles() && HostileUtils.isHostile(e))
                                || (targetFeature.targetNeutrals() && HostileUtils.isNeutral(e))
                                || (targetFeature.targetPassives() && HostileUtils.isPassive(e));
                    }
                    return targetFeature.targetProjectiles()
                            && (e instanceof ShulkerBullet || e instanceof LargeFireball);
                })
                .toList();

        Comparator<Entity> comparator = switch (targetFeature.getPriority()) {
            case HEALTH -> Comparator.comparingDouble(e -> e instanceof LivingEntity living
                    ? living.getHealth() : Double.MAX_VALUE);
            case SMART -> Comparator.comparingInt(TargetUtils::smartTargetRank)
                    .thenComparingDouble(e -> e.distanceToSqr(MC.player));
            case DISTANCE -> Comparator.comparingDouble(e -> e.distanceToSqr(MC.player));
        };

        List<Entity> sorted = new ArrayList<>(candidates);
        sorted.sort(comparator);
        return sorted.stream().limit(limit).toList();
    }

    private static int smartTargetRank(Entity entity) {
        if (entity instanceof Player)
            return 0;
        if (entity instanceof Creeper && entity.distanceToSqr(MC.player) <= 9.0)
            return 1;
        if (entity instanceof ShulkerBullet || entity instanceof LargeFireball)
            return 2;
        return 3;
    }

    public static Entity getTarget() {
        TargetFeatureConfig targetFeature = FeatureContractService.get(TargetFeatureConfig.class);
        if (MC.player == null || MC.level == null || targetFeature == null)
            return null;

        List<Entity> candidates = EntityUtils.getAllEntities().stream()
                .filter(e -> e != MC.player)
                .filter(e -> {
                    if (e instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) e;
                        if (!le.isAlive()) return false;
                        if (e.tickCount < targetFeature.getMinTicksExisted()) return false;
                        double distSq = e.distanceToSqr(MC.player);
                        if (distSq > targetFeature.getTargetRange() * targetFeature.getTargetRange()) return false;

                        return (targetFeature.targetPlayers() && e instanceof Player && !SOCIALS_SERVICE.isFriend(e.getName().getString()))
                                || (targetFeature.targetHostiles() && HostileUtils.isHostile(e))
                                || (targetFeature.targetNeutrals() && HostileUtils.isNeutral(e))
                                || (targetFeature.targetPassives() && HostileUtils.isPassive(e));
                    }

                    if (targetFeature.targetProjectiles()) {
                        return (e instanceof ShulkerBullet) || (e instanceof LargeFireball);
                    }

                    return false;
                })
                .collect(Collectors.toList());

        switch (targetFeature.getPriority()) {
            case HEALTH:
                return candidates.stream()
                        .filter(e -> e instanceof LivingEntity)
                        .min(Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth()))
                        .orElse(null);

            case DISTANCE:
                return candidates.stream()
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(MC.player)))
                        .orElse(null);

            case SMART:
                List<Entity> players = candidates.stream()
                        .filter(e -> e instanceof Player && !SOCIALS_SERVICE.isFriend(e.getName().getString()))
                        .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(MC.player)))
                        .toList();

                if (!players.isEmpty()) return players.get(0);

                List<Entity> creepers = candidates.stream()
                        .filter(e -> e instanceof Creeper)
                        .filter(e -> e.distanceToSqr(MC.player) <= 3 * 3) // yeah its not accurate at all, but its not required here i guess?
                        .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(MC.player)))
                        .toList();

                if (!creepers.isEmpty()) return creepers.get(0);

                List<Entity> projectiles = candidates.stream()
                        .filter(e -> e instanceof ShulkerBullet || e instanceof LargeFireball)
                        .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(MC.player)))
                        .toList();

                if (!projectiles.isEmpty()) return projectiles.get(0);

                List<Entity> others = candidates.stream()
                        .filter(e -> !(e instanceof Player)
                                && !(e instanceof ShulkerBullet)
                                && !(e instanceof LargeFireball))
                        .toList();

                return others.stream()
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(MC.player)))
                        .orElse(null);
        }

        return null;
    }
}
