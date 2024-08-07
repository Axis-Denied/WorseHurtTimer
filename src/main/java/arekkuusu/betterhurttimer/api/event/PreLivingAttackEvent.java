package arekkuusu.betterhurttimer.api.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PreLivingAttackEvent extends Event {

    private final EntityLivingBase entityLiving;
    private final DamageSource source;
    private float amount;
    private final boolean stalled;

    public PreLivingAttackEvent(EntityLivingBase entityLiving, DamageSource source, float amount, boolean stalled) {
        this.entityLiving = entityLiving;
        this.source = source;
        this.amount = amount;
        this.stalled = stalled;
    }
    public boolean wasStalled(){
        return stalled;
    }


    public EntityLivingBase getEntityLiving() {
        return entityLiving;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }
}
