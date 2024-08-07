package arekkuusu.betterhurttimer.common.core;

import com.google.common.collect.Lists;
import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LateMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        List<String> configs = new ArrayList<>();
        if(Loader.isModLoaded("ee")) configs.add("assets/betterhurttimer/betterhurttimer.ee.mixins.json");
        return configs;
    }
}
