package com.multibana.recallpotion;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.compress.compressors.lz77support.LZ77Compressor;
import org.apache.logging.log4j.core.jmx.Server;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class RecallEffect extends StatusEffect {
    public RecallEffect(StatusEffectCategory statusEffectCategory, int color) {
        super(statusEffectCategory, color);
    }

    void failedTeleport(LivingEntity target, ServerPlayerEntity player){
        Vec3d pos = target.getPos();
        target.world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.PLAYERS, 1f, 1f);
    }

    // This method teleports a LivingEntity target to the spawn point of a ServerPlayerEntity player
    void teleportTargetToPlayerSpawn(LivingEntity target, ServerPlayerEntity player){
        BlockPos spawn = player.getSpawnPointPosition(); // Get the spawn point position of the player
        ServerWorld serverWorld = (ServerWorld) player.world; // Get the world of the player as a ServerWorld
        RegistryKey<World> spawnDimension = player.getSpawnPointDimension(); // Get the dimension of the player's spawn point
        ServerWorld destination = ((ServerWorld) player.world).getServer().getWorld(spawnDimension); // Get the ServerWorld object of the spawn dimension

        // If the destination is null or in a different dimension than the player's spawn dimension, fail and play a sound at the target's position
        //System.out.println("[RecallPotion] Printing destination: " + destination);
        BlockState blockState = destination.getBlockState(spawn);
        if (destination == null || !(spawnDimension.equals(serverWorld.getRegistryKey())) && blockState.isIn(BlockTags.BEDS)) {
            failedTeleport(target, player);
            player.sendMessage(Text.of("The 'Potion of Recall' is not powerful enough to teleport across dimensions!"), false);
            return;
        }

        // If the player doesn't have a spawn point, use the world spawn point instead
        if (spawn == null) {
            //spawn = ((ServerWorld) player.world).getSpawnPos();
            failedTeleport(target, player);
            player.sendMessage(Text.of("You do not have a home bed!"), false);
            return;
        }
        // Find a suitable respawn position for the target in the destination world
        Optional<Vec3d> a = PlayerEntity.findRespawnPosition(destination, spawn, 0, true, true);

        // If a suitable respawn position is found, set the spawn point to that position
        if(a.isPresent()){
            //BlockState blockState = destination.getBlockState(spawn);

            if(blockState.isIn(BlockTags.BEDS)){
                spawn = new BlockPos(a.get());
                //System.out.println("[RecallPotion] {if(blockState.isIn(BlockTags.BEDS)} Setting player spawn location to: " + spawn);
            }
            else if(!blockState.isIn(BlockTags.BEDS)){
                failedTeleport(target, player);
                player.sendMessage(Text.of("You do not have a home bed!"), false);
                //System.out.println("[RecallPotion] You have no home bed or charged respawn anchor, or it was obstructed: " + destination);
                return;
            }
            // If no suitable respawn position is found, try using the world spawn point instead
            else{
                //Optional<Vec3d> b = PlayerEntity.findRespawnPosition(destination, ((ServerWorld) player.world).getSpawnPos(), 0, true, true);
                //spawn = b.map(BlockPos::new).orElseGet(() -> ((ServerWorld) player.world).getSpawnPos());
                //System.out.println("[RecallPotion] {else} Setting player spawn location to: " + spawn);

                // If the target is the same as the player, set the spawn point to the new respawn position
                //if (target.isPlayer()){
                   // ServerPlayerEntity tPlayer = (ServerPlayerEntity) target;
                   // if(tPlayer.equals(player)){
                        //player.setSpawnPoint(serverWorld.getRegistryKey(), spawn, 0, true, false);
                    //}
                //}
            }
        }

        target.stopRiding();
        player.fallDistance = 0;
        target.teleport(spawn.getX() + 0.5F,spawn.getY()+0.6F,spawn.getZ()+ 0.5F);
        target.world.playSound(null, spawn.getX() + 0.5F, spawn.getY()+0.6F, spawn.getZ() + 0.5F, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
    }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier){
        if (pLivingEntity.world.isClient || !pLivingEntity.isPlayer()) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) pLivingEntity;
        teleportTargetToPlayerSpawn(pLivingEntity, player);
    }
    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier){
        return pDuration == 1;
    }
}