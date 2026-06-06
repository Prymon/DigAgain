package com.huhu.digagain;

import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = DigAgain.MODID, name = DigAgain.NAME, version = DigAgain.VERSION, acceptableRemoteVersions = "*")
public class DigAgain {

    public static final String MODID = "digagain";
    public static final String NAME = "Dig Again";
    public static final String VERSION = Tags.VERSION;

    public static Logger logger;
    public static DigAgainConfig config;
    public static GtOreDetector oreDetector;
    public static RestoreScheduler restoreScheduler;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        config = new DigAgainConfig(event.getSuggestedConfigurationFile());
        config.load();
        oreDetector = new GtOreDetector(config);
        restoreScheduler = new RestoreScheduler(config);
        MinecraftForge.EVENT_BUS.register(new BreakCaptureHandler(config, oreDetector, restoreScheduler));
        FMLCommonHandler.instance()
            .bus()
            .register(restoreScheduler);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandDigAgain(config, oreDetector, restoreScheduler));
    }
}
