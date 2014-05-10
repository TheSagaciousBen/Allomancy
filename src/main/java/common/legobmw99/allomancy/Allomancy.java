package common.legobmw99.allomancy;

import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import common.legobmw99.allomancy.common.AllomancyPackets;
import common.legobmw99.allomancy.common.Registry;
import common.legobmw99.allomancy.handlers.PlayerTrackerHandler;
import common.legobmw99.allomancy.network.PacketPipeline;
import common.legobmw99.allomancy.proxy.CommonProxy;
import common.legobmw99.allomancy.util.AllomancyConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Allomancy.MODID, version = Allomancy.VERSION)
public class Allomancy {
	public static final String MODID = "allomancy";
	public static final String VERSION = "2.0";
	public static EventHandler eventHandler;
	public static final PacketPipeline packetPipeline = new PacketPipeline();
	public static PlayerTrackerHandler playerTracker = new PlayerTrackerHandler();

	@Instance(value = "allomancy")
	public static Allomancy instance;

	@SidedProxy(clientSide = "common.legobmw99.allomancy.proxy.ClientProxy", serverSide = "common.legobmw99.allomancy.proxy.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		AllomancyConfig.initProps(event.getSuggestedConfigurationFile());
		Registry.ModContent();
		AllomancyPackets.init();
	}

	@EventHandler
	public void load(FMLInitializationEvent event) {
		FMLCommonHandler.instance().bus().register(playerTracker);
		packetPipeline.initalize();
		//FMLCommonHandler.instance().bus().register(eventHandler);
		ChestGenHooks.getInfo(ChestGenHooks.DUNGEON_CHEST).addItem(
				new WeightedRandomChestContent(new ItemStack(
						Registry.nuggetLerasium), 1, 1, 40));
		ChestGenHooks.getInfo(ChestGenHooks.MINESHAFT_CORRIDOR).addItem(
				new WeightedRandomChestContent(new ItemStack(
						Registry.nuggetLerasium), 1, 1, 40));
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		packetPipeline.postInitialize();
		proxy.init();
	}
}