package io.github.omegaplex.ankerstone.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.omegaplex.ankerstone.AnkerstoneAttributionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonBaseBlock.class)
public abstract class AnkerstonePistonAttributionMixin {
    @ModifyExpressionValue(method = "moveBlocks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToPush()Ljava/util/List;"))
    private List<BlockPos> ankerstone$movePlacers(List<BlockPos> positions,
            Level level, BlockPos piston, Direction facing, boolean extending) {
        if (level instanceof ServerLevel serverLevel) {
            Direction movement = extending ? facing : facing.getOpposite();
            AnkerstoneAttributionData.get(serverLevel).movePlacers(
                    serverLevel.dimension(), positions, movement);
        }
        return positions;
    }
}
