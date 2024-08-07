package arekkuusu.betterhurttimer.common;

import arekkuusu.betterhurttimer.BHT;
import arekkuusu.betterhurttimer.BHTConfig;
import arekkuusu.betterhurttimer.api.BHTAPI;
import arekkuusu.betterhurttimer.api.capability.Capabilities;
import arekkuusu.betterhurttimer.api.capability.HurtCapability;
import arekkuusu.betterhurttimer.api.capability.data.AttackInfo;
import arekkuusu.betterhurttimer.api.capability.data.HurtSourceInfo.HurtSourceData;
import arekkuusu.betterhurttimer.api.event.PreLivingAttackEvent;
import arekkuusu.betterhurttimer.api.event.PreLivingKnockBackEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.Arrays;

@Mod.EventBusSubscriber(modid = BHT.MOD_ID)
public class Events {

    @SubscribeEvent
    public static void onConfigUpdated(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(BHT.MOD_ID)) {
            ConfigManager.sync(BHT.MOD_ID, Config.Type.INSTANCE);
        }
    }


    public static boolean onAttackEntityOverride = true;
    public static int maxHurtResistantTime = 20;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityUpdate(LivingEvent.LivingUpdateEvent event) {
        if (isClientWorld(event.getEntity())) return;
        Capabilities.hurt(event.getEntity()).ifPresent(capability -> {
            //Source Damage i-Frames
            if (!capability.hurtMap.isEmpty()) {
                capability.hurtMap.forEach((s, data) -> {
                    ++data.lastHurtTick;
                    if (data.tick > 0) {
                        --data.tick;
                    }
                    //if(event.getEntity() instanceof EntityPlayer) BHT.LOG.info("Ticking hurtmap for data {} with source {}", data, data.damageSource);
                    if (data.info.doFrames && data.tick == 0 && !data.canApply) {
                        Events.onAttackEntityOverride = false;
                        if(BHTConfig.doLogging) BHT.LOG.info("Applying accumulated damage with amount {}", data.amount);
                        data.apply(event.getEntity());
                        Events.onAttackEntityOverride = true;
                    }
                });
            }
            //Melee i-Frames
            if (!capability.meleeMap.isEmpty()) {
                capability.meleeMap.forEach((e, a) -> a.ticksSinceLastMelee++);
            }
            //Armor i-Frames
            if (capability.ticksToArmorDamage > 0) {
                --capability.ticksToArmorDamage;
            } else {
                capability.lastArmorDamage = 0;
            }
            //Shield i-Frames
            if (capability.ticksToShieldDamage > 0) {
                --capability.ticksToShieldDamage;
            } else {
                capability.lastShieldDamage = 0;
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntityFromPre(PreLivingAttackEvent event) {
        if (isClientWorld(event.getEntityLiving())) return;
        if (!Events.onAttackEntityOverride) return;
        boolean log = BHTConfig.doLogging;
        if(log){
            BHT.LOG.info("Found a PreLivingAttackEvent, fired from {}, with amount {}", event.wasStalled() ? "ForgeHooks.onLivingAttack" : "EntityLivingBase.attackEntityFrom", event.getAmount());
        }
        DamageSource source = event.getSource();
        if(log){
            BHT.LOG.info("Found a Damage Source {} with name {}", source, source.getDamageType());
        }
        if (Events.isAttack(source)) return; //If my source is melee, return
        if(log){
            BHT.LOG.info("It is non-melee. Proceeding.");
        }
        EntityLivingBase entity = event.getEntityLiving();
        HurtSourceData data = BHTAPI.get(entity, source);
        if(log){
            BHT.LOG.info("Got its HurtSourceData. The HurtInfo's frames behavior is: {}", data.info.doFrames);
            BHT.LOG.info("Got its HurtSourceData. The HurtInfo's waitTime is: {}", data.info.waitTime);
        }

        data.damageSource = source; //Last source to do the damage gets the kill
        if(log) {
            BHT.LOG.info("The data's pre-trigger amount is {}. This isn't the actual damage source's amount, just what WHT has stored for prior damage events.", data.amount);
            BHT.LOG.info("The data's pre-trigger canApply is {}", data.canApply);
            BHT.LOG.info("The data's pre-trigger tick value is {}", data.tick);
            BHT.LOG.info("The data's pre-trigger lastHurtTick value is {}", data.lastHurtTick);
        }
        if (data.tick == 0 && data.canApply) {
            data.trigger();
        }
        if(log){
            BHT.LOG.info("The data's canApply is {}", data.canApply);
            BHT.LOG.info("The data's tick value is {}", data.tick);
            BHT.LOG.info("The data's lastHurtTick value is {}", data.lastHurtTick);
            BHT.LOG.info("The data's lastHurtAmount is {}", data.lastHurtAmount);
        }
        if(log) BHT.LOG.info("Checking if lastHurtTick is less than waitTime: {}", data.lastHurtTick < data.info.waitTime);
        if (data.info.doFrames) {
            if (data.lastHurtTick < data.info.waitTime) {
                if(log) BHT.LOG.info("Accumulating damage, old amount: {}", data.amount);
                data.accumulate(event.getAmount());
                if(log){
                    BHT.LOG.info("Accumulating damage, new amount: {}", data.amount);
                    BHT.LOG.info("The damage event was also cancelled.");
                }
                event.setCanceled(true);
            }
        } else if (data.tick != 0) {
            float lastAmount = event.getAmount();
            if(log) BHT.LOG.info("PreLivingAttackEvent's stored attack amount is {}", lastAmount);
            if (data.lastHurtTick < data.info.waitTime) {
                if(log) BHT.LOG.info("Comparing to PreLivingAttackEvent's stored damage amount");
                if (Double.compare(Math.max(0, data.lastHurtAmount + BHTConfig.CONFIG.damageFrames.nextAttackDamageDifference), event.getAmount()) < 0) {
                    event.setAmount(lastAmount - Math.max(0, data.lastHurtAmount));
                    if(log) BHT.LOG.info("The attack will not be cancelled. It's new amount is {}", event.getAmount());
                    data.lastHurtAmount = lastAmount;
                } else {
                    if(log) BHT.LOG.info("WHT cancelled the attack. The HurtSourceData's lastHurtAmount + {} is below 0.", BHTConfig.CONFIG.damageFrames.nextAttackDamageDifference);
                    event.setCanceled(true);
                }
            } else {
                data.lastHurtAmount = lastAmount;
            }
        } else {
            data.canApply = true;
        }
        data.lastHurtTick = 0;
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (isClientWorld(event.getEntity())) return;
        Capabilities.hurt(event.getEntityPlayer()).ifPresent(capability -> {
            final AttackInfo attackInfo = capability.meleeMap.computeIfAbsent(event.getTarget(), BHTAPI.INFO_FUNCTION);
            Entity target = event.getTarget();
            Entity attacker = event.getEntityPlayer();
            int ticksSinceLastHurt = Events.getHurtTime(target, attacker);
            try {
                int ticksSinceLastMelee = BHTAPI.field.getInt(event.getEntityPlayer());
                if (ticksSinceLastMelee > ticksSinceLastHurt) {
                    attackInfo.ticksSinceLastMelee = ticksSinceLastMelee;
                }
            } catch (Exception ignored) {
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityAttack(LivingAttackEvent event) {
        if (isClientWorld(event.getEntity())) return;
        DamageSource source = event.getSource();
        if (!(source.getImmediateSource() instanceof EntityLivingBase) || event.getAmount() <= 0) return;
        if (!Events.isAttack(source)) return;
        Entity target = event.getEntity();
        Entity attacker = source.getImmediateSource();
        Capabilities.hurt(attacker).ifPresent(capability -> {

            //Calculate last hurt time required
            final AttackInfo attackInfo = capability.meleeMap.computeIfAbsent(target, BHTAPI.INFO_FUNCTION);
            int ticksSinceLastHurt = Events.getHurtTime(target, attacker);
            int ticksSinceLastMelee = attackInfo.ticksSinceLastMelee;
            if (ticksSinceLastMelee < ticksSinceLastHurt) {
                // What needs to be done to fix other peoples shit.
                if (attackInfo.ticksSinceLastMelee == 0 && (!(attacker instanceof EntityPlayer) || ((EntityPlayer) attacker).getCooledAttackStrength(0) == 0)) {
                    attackInfo.override = true;
                } else {
                    event.setCanceled(true);
                }
            } else {
                attackInfo.ticksSinceLastMelee = 0;
            }
        });
    }

    public static int getHurtTime(Entity target, Entity attacker) {
        double threshold = Events.getThreshold(attacker);

        if (attacker instanceof EntityLivingBase && Events.canSwing((EntityLivingBase) attacker)) {
            return (int) (Events.getCoolPeriod((EntityLivingBase) attacker) * threshold);
        } else {
            double maxHurtResistantTime = Events.getHurtResistantTime(target);
            double attackerAttackSpeed = Events.getAttackSpeed(attacker);
            return (int) (maxHurtResistantTime * (attackerAttackSpeed * threshold));
        }
    }

    public static boolean canSwing(EntityLivingBase entity) {
        ItemStack stack = entity.getHeldItem(EnumHand.MAIN_HAND);
        Item item = stack.getItem();
        boolean canSwing = false;
        try {
            canSwing = BHTAPI.field.getInt(entity) >= 0 && item.getAttributeModifiers(
                    EntityEquipmentSlot.MAINHAND,
                    stack
            ).containsKey(SharedMonsterAttributes.ATTACK_SPEED.getName());
        } catch(Exception ignored) {
        }
        return canSwing;
    }

    public static double getCoolPeriod(EntityLivingBase entity) {
        return (1D / entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED).getAttributeValue() * Events.maxHurtResistantTime);
    }

    public static double getHurtResistantTime(Entity entity) {
        return entity instanceof EntityLivingBase ?
                ((EntityLivingBase) entity).maxHurtResistantTime
                : Events.maxHurtResistantTime;
    }

    public static double getAttackSpeed(Entity entity) {
        double attackSpeed = SharedMonsterAttributes.ATTACK_SPEED.getDefaultValue();
        IAttributeInstance attribute = null;
        if (entity instanceof EntityLivingBase) {
            attribute = ((EntityLivingBase) entity).getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        }
        if (attribute != null) {
            attackSpeed = attribute.getAttributeValue();
        }
        return 1.2D - (1.2D / (1.2D / (attackSpeed * 1.2) * 20D));
    }

    public static double getThreshold(Entity entity) {
        if (entity instanceof EntityLivingBase) {
            ResourceLocation itemLocation = ((EntityLivingBase) entity).getHeldItemMainhand().getItem().getRegistryName();
            if (BHTAPI.ATTACK_ITEM_THRESHOLD_MAP.containsKey(itemLocation)) {
                return BHTAPI.ATTACK_ITEM_THRESHOLD_MAP.get(itemLocation);
            }
        }
        ResourceLocation location = EntityList.getKey(entity.getClass());
        double threshold = BHTConfig.CONFIG.attackFrames.attackThresholdDefault;
        if (entity instanceof EntityPlayer)
            threshold = BHTConfig.CONFIG.attackFrames.attackThresholdPlayer;
        if (location != null && BHTAPI.ATTACK_THRESHOLD_MAP.containsKey(location))
            threshold = BHTAPI.ATTACK_THRESHOLD_MAP.get(location);
        return threshold;
    }

    public static boolean isAttack(DamageSource source) {
        return Arrays.asList(BHTConfig.CONFIG.attackFrames.attackSources).contains(source.getDamageType());
    }

    @SubscribeEvent()
    public static void onKnockback(PreLivingKnockBackEvent event) {
        if (isClientWorld(event.getEntityLiving())) return;
        if (Arrays.asList(BHTConfig.CONFIG.knockbackFrames.knockbackExemptSource).contains(event.getSource().getDamageType())) {
            event.setCanceled(true);
        }
    }

    public static boolean isClientWorld(Entity entity) {
        return entity.getEntityWorld().isRemote;
    }
}
