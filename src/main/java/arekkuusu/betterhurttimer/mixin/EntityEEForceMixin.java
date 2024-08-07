package arekkuusu.betterhurttimer.mixin;

import arekkuusu.betterhurttimer.common.IForceAttack;
import com.example.structure.entity.EntityAbstractBuffker;
import com.example.structure.entity.endking.EntityAbstractEndKing;
import com.example.structure.entity.trader.EntityAbstractAvalon;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({EntityAbstractEndKing.class, EntityAbstractBuffker.class, EntityAbstractAvalon.class})
public abstract class EntityEEForceMixin extends EntityLivingBase implements IForceAttack {

    public EntityEEForceMixin(World world) {
        super(world);
    }

    @Override
    public boolean wht$forceAttackEntityFrom(DamageSource source, float amount) {
        return super.attackEntityFrom(source, amount);
    }
}
