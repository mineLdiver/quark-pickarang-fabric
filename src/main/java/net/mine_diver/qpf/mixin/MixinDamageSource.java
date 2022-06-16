package net.mine_diver.qpf.mixin;

import net.mine_diver.qpf.QuarkPickarangFabric;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSource.class)
public class MixinDamageSource {

    @Inject(
            method = "player(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/entity/damage/DamageSource;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void playerAttack(PlayerEntity player, CallbackInfoReturnable<DamageSource> callbackInfoReturnable) {
        DamageSource damage = QuarkPickarangFabric.createDamageSource(player);
        if(damage != null)
            callbackInfoReturnable.setReturnValue(damage);
    }
}
