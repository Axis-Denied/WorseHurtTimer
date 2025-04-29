package arekkuusu.betterhurttimer.api.capability;

import net.minecraft.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.WeakHashMap;

public final class Capabilities {
    @CapabilityInject(HurtCapability.class)
    public static final Capability<HurtCapability> HURT_LIMITER = null;
    public static final WeakHashMap<FakePlayer, HurtCapability> FAKE_PLAYER_HURT_CAPABILITIES = new WeakHashMap<>();

    public static Optional<HurtCapability> hurt(@Nullable Entity entity) {
        if(entity instanceof FakePlayer && FAKE_PLAYER_HURT_CAPABILITIES.containsKey(entity)){
            return Optional.ofNullable(FAKE_PLAYER_HURT_CAPABILITIES.get(entity));
        }
        Optional<HurtCapability> inst = entity != null ? Optional.ofNullable(entity.getCapability(HURT_LIMITER, null)) : Optional.empty();
        if(entity instanceof FakePlayer){
            inst.ifPresent(capability -> FAKE_PLAYER_HURT_CAPABILITIES.put((FakePlayer) entity, capability));
        }
        return inst;
    }
}
