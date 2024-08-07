package arekkuusu.betterhurttimer.mixin;

import arekkuusu.betterhurttimer.BHT;
import arekkuusu.betterhurttimer.BHTConfig;
import arekkuusu.betterhurttimer.api.BHTAPI;
import arekkuusu.betterhurttimer.api.StallingClass;
import arekkuusu.betterhurttimer.api.capability.Capabilities;
import arekkuusu.betterhurttimer.api.capability.HurtCapability;
import arekkuusu.betterhurttimer.api.capability.data.AttackInfo;
import arekkuusu.betterhurttimer.api.event.PreLivingAttackEvent;
import arekkuusu.betterhurttimer.common.Events;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public abstract class HurtTimeMixin extends Entity {

    @Shadow
    public int hurtTime;
    @Shadow
    public float attackedAtYaw;
    public float preAttackedAtYaw;
    public int preHurtTime;
    public DamageSource preDamageSource;

    public HurtTimeMixin(World p_i1582_1_) {
        super(p_i1582_1_);
    }


    @SuppressWarnings("UnreachableCode")
    @Inject(require=1, method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void onLivingAttackMovedOver(DamageSource src, float amount, CallbackInfoReturnable<Boolean> info, @Local(argsOnly = true) LocalFloatRef amountRef) {
        EntityLivingBase entity = (EntityLivingBase)(Object)this;
        PreLivingAttackEvent event = new PreLivingAttackEvent(entity, src, amount, false);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            info.setReturnValue(false);
            info.cancel();
        } else {
            amountRef.set(event.getAmount());
            StallingClass.stallEventFire = true;
        }
    }


    @Inject(require=1, method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At("HEAD"))
    public void attackEntityFromBefore(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (this.hurtTime > 0) {
            this.preHurtTime = this.hurtTime;
        } else {
            this.preHurtTime = 0;
        }
        if (this.attackedAtYaw > 0) {
            this.preAttackedAtYaw = this.attackedAtYaw;
        } else {
            this.preAttackedAtYaw = 0;
        }
        this.preDamageSource = source;
        //noinspection ConstantConditions
        BHT.getProxy().setPreHurtTime((EntityLivingBase) ((Object) this));
    }

    @Redirect(require=1, method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;hurtResistantTime:I", ordinal = 0))
    public int attackResistantOverride(EntityLivingBase target) {
        if (Events.isAttack(this.preDamageSource)) {
            Entity attacker = this.preDamageSource.getTrueSource();
            HurtCapability capability = Capabilities.hurt(attacker).orElse(null);
            if (capability != null) {
                final AttackInfo attackInfo = capability.meleeMap.computeIfAbsent(target, BHTAPI.INFO_FUNCTION);
                if (attackInfo.override) {
                    attackInfo.override = false;
                    return target.maxHurtResistantTime;
                }
            }
        }
        return target.hurtResistantTime;
    }

    @Inject(require=1, method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;hurtTime:I", shift = At.Shift.AFTER))
    public void hurtResistantTime(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        this.hurtResistantTime = BHTConfig.CONFIG.damageFrames.hurtResistantTime;
    }

    @Redirect(require=1, method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setEntityState(Lnet/minecraft/entity/Entity;B)V", ordinal = 2))
    public void turnOffSound(World world, Entity entity, byte b) {
        if (b == 2 || b == 33 || b == 36 || b == 37) {
            if (this.preHurtTime == 0) {
                world.setEntityState(entity, b);
            }
        }
    }

    @Redirect(require=1, method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;playHurtSound(Lnet/minecraft/util/DamageSource;)V"))
    public void playHurtSound(EntityLivingBase that, DamageSource source) {
        if (this.preHurtTime == 0) {
            this.playHurtSound(source);
        }
    }

    @Inject(require=1, method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At("TAIL"))
    public void attackEntityFromAfter(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (this.preHurtTime > 0) {
            this.hurtTime = this.preHurtTime;
        }
        if (this.preAttackedAtYaw > 0) {
            this.attackedAtYaw = this.preAttackedAtYaw;
        }
    }

    @Inject(require=1, method = "playHurtSound(Lnet/minecraft/util/DamageSource;)V", at = @At("HEAD"), cancellable = true)
    public void playHurtSound(DamageSource source, CallbackInfo info) {
        if (this.preHurtTime > 0) {
            info.cancel();
        }
    }

    @Shadow
    protected abstract void playHurtSound(DamageSource source);
}
