package com.netease.minecraft.autojump;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = ModAutoFish.MODID, useMetadata = true,
guiFactory = "com.netease.minecraft.autojump.AutoFishGuiFactory")
public class ModAutoFish {
    public static final String MODID = "mod_autojump";
    
    //!< com.163.minecraft
    public static Configuration configFile;
    public static boolean config_autofish_enable;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_ENABLE = true;
    public static boolean config_autofish_food_enable;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_FOOD_ENABLE = true;
    public static boolean config_autofish_multirod;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_MULTIROD = false;
    public static boolean config_autofish_preventBreak;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_PREVENTBREAK = false;
    
    @SidedProxy(clientSide="com.netease.minecraft.autojump.ClientProxy", serverSide="com.netease.minecraft.autojump.ServerProxy")
    public static CommonProxy proxy;
    
    public static AutoFishEventHandler eventHandler = new AutoFishEventHandler();
    
    public static ModAutoFish instance = new ModAutoFish();
            
    @EventHandler
    public void preInit(FMLPreInitializationEvent preInitEvent) {
        ModAutoFish.proxy.preInit(preInitEvent);
        configFile = new Configuration(preInitEvent.getSuggestedConfigurationFile());
        syncConfig();
    }
    
    @EventHandler
    public void init (FMLInitializationEvent event) {
        AutoFishLogger.info("Initializing " + ModAutoFish.MODID);
        ModAutoFish.proxy.init(event);
    }

    public static void syncConfig() {
        config_autofish_enable = configFile.getBoolean("自动钓鱼", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_ENABLE, "自动甩杆，当有鱼上钩时自动收杆");
        config_autofish_food_enable = configFile.getBoolean("边钓边吃", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_FOOD_ENABLE, "钓鱼时自动进食，专门针对方块帝国。");
        config_autofish_multirod = configFile.getBoolean("多鱼竿替换", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_MULTIROD, "自动替换备用鱼竿，备用杆需在Hotbar上");
        config_autofish_preventBreak = configFile.getBoolean("鱼竿保护", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_PREVENTBREAK, "当鱼竿快坏时，停止钓鱼或者更换备用杆");
        
        if (configFile.hasChanged()) {
            configFile.save();
        }
    }

}
