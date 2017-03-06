package com.netease.minecraft.autojump;

import java.lang.reflect.Field;

import org.apache.logging.log4j.Level;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class AutoFishEventHandler {

	private Minecraft minecraft;
	private EntityPlayer player;
	private long castScheduledAt = 0L;
	private long startedReelDelayAt = 0L;
	private long lastEatFoodAt = 0L;
	private long lastCastAt = 0L;
	
	private long lastPickAxe = 0L;
	
	private long nextAdvetist = 0L;
	private static final int CAST_QUEUE_TICK_DELAY = 30;
	private static final int REEL_TICK_DELAY = 5;
	private static final int AUTOFISH_BREAKPREVENT_THRESHOLD = 2;
	private static final double MOTION_Y_THRESHOLD = -0.02d;

	private static final int FOOD_QUEUE_TICK_DELAY = 100;
	private static final int FOOD_READY_TICK_DELAY = 300; // !< 15 seconds.
	
	//!< time for the stone-pickaxe to pick the stone.
	private static final int PICKAXE_ACTION_DELAY1 = 20;
	
	//!< time for the stone regenerated.
	private static final int PICKAXE_ACTION_DELAY2 = 35;

	private static final int MODE_FISH = 1;
	private static final int MODE_FISH_FOOD = 2;

	public String msg1 = "[广告]本店收购金锭铁锭信标以及泥土，并有大量实用附魔书出售，有意者欢迎光临，/is warp elf9527";
	public String msg2 = "[广告]本店附魔书主要包括经验修补、精确采集、耐久3，效率4，锋利4，抢夺3等等，品种多多，欢迎光临，/is warp elf9527";
	public String msg3 = "[广告]本店经营5神弓，6神弓，4神杆，另有末影珍珠烈焰棒粘液球等难获取之物，有意者欢迎光临，/is warp elf9527";
	public String msg4 = "[广告]本店还没想好广告词，有意者欢迎光临，/is warp elf9527";
	public String msg5 = "[广告]海底小店，404 Not found，欢迎光临，/is warp elf9527";
	public String msg6 = "本店收购金锭铁锭信标以及泥土，并有大量实用附魔书出售，有意者欢迎光临，/is warp elf9527";
	public String msg7 = "本店附魔书主要包括经验修补、精确采集、耐久3，效率4，锋利4，抢夺3等等，品种多多，欢迎光临海底小店，/is warp elf9527";
	public String msg8 = "本店经营5神弓，6神弓，4神杆，另有末影珍珠烈焰棒粘液球等难获取之物，有意者欢迎光临，/is warp elf9527";
	public String msg9 = "本店还没想好广告词，有意者欢迎光临，/is warp elf9527";
	public String msg10 = "本店404 Not found，欢迎光临，/is warp elf9527";
	public String msg11 = "发多了容易被系统墙，所以我也不知道该打什么广告，欢迎光临海底小店，/is warp elf9527";
	
	private static final int ADV_READY_TICK_DELAY = 240 * 20; // !< 60*4 seconds.
	
	// !< default fish mode.
	private int mode = MODE_FISH;

	private int foodTick = 0;
	private int lastFoodHealth = 8;
	
	private int lastAdVIndex = 0;
	
	private boolean pickaxe_enable = true;

	@SubscribeEvent
	public void onClientTickEvent(ClientTickEvent event) {
		this.minecraft = Minecraft.getMinecraft();
		
		if ((!this.minecraft.isGamePaused()) && (this.minecraft.thePlayer != null)) {
			this.player = this.minecraft.thePlayer;
			
			if(ModAutoFish.config_autoadv_enable)
			{
				if(nextAdvetist == 0L)
				{
					nextAdvetist = this.minecraft.theWorld.getTotalWorldTime() + 10 * 60 * 20;//!< 600 seconds later
				}
				else
				{
					long cur_time = this.minecraft.theWorld.getTotalWorldTime();
					
					if(cur_time > this.nextAdvetist)
					{
						this.nextAdvetist = cur_time + ADV_READY_TICK_DELAY;

						switch(lastAdVIndex){
						default:
						case 0:
							this.minecraft.thePlayer.sendChatMessage(msg1);
							break;
							
						case 1:
							this.minecraft.thePlayer.sendChatMessage(msg2);
							break;
							
						case 2:
							this.minecraft.thePlayer.sendChatMessage(msg3);
							break;
						case 3:
							this.minecraft.thePlayer.sendChatMessage(msg4);
							break;
							
						case 4:
							this.minecraft.thePlayer.sendChatMessage(msg5);
							break;
							
						case 5:
							this.minecraft.thePlayer.sendChatMessage(msg6);
							break;
							
						case 6:
							this.minecraft.thePlayer.sendChatMessage(msg7);
							break;
							
						case 7:
							this.minecraft.thePlayer.sendChatMessage(msg8);
							break;
							
						case 8:
							this.minecraft.thePlayer.sendChatMessage(msg9);
							break;
							
						case 9:
							this.minecraft.thePlayer.sendChatMessage(msg10);
							break;
							
						case 10:
							this.minecraft.thePlayer.sendChatMessage(msg11);
							break;
						}
						
						lastAdVIndex ++;
						if(lastAdVIndex >= 11)
						{
							lastAdVIndex = 0;
						}
					}
				}
			}

			//!< XXX auto pick stone.
			if (ModAutoFish.config_autostone_enable) {
				if(lastPickAxe == 0)
				{
					this.lastPickAxe = this.minecraft.theWorld.getTotalWorldTime();
					pickaxe_enable = false;
					return;
				}
				
				if(pickaxe_enable)
				{
					long cur_pickaxe = this.minecraft.theWorld.getTotalWorldTime();
					if(cur_pickaxe - this.lastPickAxe >= PICKAXE_ACTION_DELAY1)
					{
						stopUsePickAxe();
						this.lastPickAxe = this.minecraft.theWorld.getTotalWorldTime();
						pickaxe_enable = false;
						
						//AutoFishLogger.log(Level.INFO, "player stop pickaxe ... %d", this.lastPickAxe);
					}
					else
					{
						if(playerCanUserPickAxe())
						{
							tryUsingPickAxe();
						}
					}
				}
				else
				{
					long cur_pickaxe = this.minecraft.theWorld.getTotalWorldTime();
					if(cur_pickaxe - this.lastPickAxe > PICKAXE_ACTION_DELAY2)
					{
						tryToSwitchPickAxe();
						this.lastPickAxe = this.minecraft.theWorld.getTotalWorldTime();
						pickaxe_enable = true;
						
						//AutoFishLogger.log(Level.INFO, "player start using pickaxe ... %d", this.lastPickAxe);
					}
				}
				return;
			}

			//!< XXX auto fishing.
			if (ModAutoFish.config_autofish_enable) {
				if (playerHookInWater() && !isDuringReelDelay()	&& isFishBiting()) {
					// !< start tick to avoid mutltiple reeling.
					startReelDelay();

					AutoFishLogger.log(Level.INFO, "player Reeling ...: %d tick", this.minecraft.theWorld.getTotalWorldTime());
					// !< Reel !
					playerUseRod();

					// !< next time to cast fish-rop.
					scheduleNextCast();

					mode = MODE_FISH;

					lastCastAt = 0L;
				} else if (isTimeToCast()) {

					if (mode == MODE_FISH) {
						if (this.player.getFoodStats().getFoodLevel() <= 4) {
							AutoFishLogger.log(Level.INFO,	"WARNING: player is too hungry to perform fishing ...");
							ModAutoFish.config_autofish_enable = false;
							return;
						}

						if (this.player.getFoodStats().getFoodLevel() <= (20 - lastFoodHealth)) {

							AutoFishLogger.log(Level.INFO,	
									"Hungery? FoodLevel = " + this.player.getFoodStats().getFoodLevel() 
											+ ", lastFoodHealth = " + lastFoodHealth);

							if (tryToSwitchFood() == true) {
								mode = MODE_FISH_FOOD;
								AutoFishLogger.log(Level.INFO,	"player try to eat ..., lastFoodHealth = " 	+ lastFoodHealth);
								RightKeyRepeat();
								foodTick = FOOD_QUEUE_TICK_DELAY;

								return;
							} else {
								AutoFishLogger.log(Level.INFO, "WARNING: player no food to eat ...");
							}
						}

						if (needToSwitchRods()) {
							tryToSwitchRods();
						}

						if (playerCanCast()) {
							AutoFishLogger.log(Level.INFO, "player Use Rod ...: %d tick", this.minecraft.theWorld.getTotalWorldTime());
							playerUseRod();
							lastCastAt = this.minecraft.theWorld.getTotalWorldTime();
						}

						// Resetting these values is not strictly necessary, but
						// will improve the performance
						// of the check that potentially occurs every tick.
						resetReelDelay();
						resetCastSchedule();
					} else if (mode == MODE_FISH_FOOD) {

						if (this.player.getFoodStats().getFoodLevel() <= 19) {
							// !< continue eating.
							RightKeyRepeat();
						} else {
							RightKeyReset();

							// !< switch to fish mode.
							mode = MODE_FISH;
							this.lastEatFoodAt = 0;
							this.foodTick = 0;

							AutoFishLogger.log(Level.INFO,	"Player Eat Success ...");
							if (!playerIsHoldingRod()) {
								tryToSwitchRods();
							}

							scheduleNextCast();
							return;
						}

						this.foodTick--;
						if (this.foodTick <= 0) {
							mode = MODE_FISH;
							this.foodTick = 0;
							RightKeyReset();
							scheduleNextCast();

							AutoFishLogger.log(Level.INFO, "Player Eat Fail ...");

							this.lastEatFoodAt = this.minecraft.theWorld.getTotalWorldTime();

							if (!playerIsHoldingRod()) {
								tryToSwitchRods();
							}
						}
					}
				}
				else
				{
					if(lastCastAt != 0)
					{
						if(this.minecraft.theWorld.getTotalWorldTime() > this.lastCastAt + 1800 + 100)
						{
							AutoFishLogger.log(Level.INFO, "WARNING: player fishing timeout ...");
							
							if(playerHookInWater())
							{
								// !< start tick to avoid mutltiple reeling.
								startReelDelay();
								// !< Reel !
								playerUseRod();
							}
							
							lastCastAt = 0;
							return;
						}
						else if(this.minecraft.theWorld.getTotalWorldTime() > this.lastCastAt + 100)
						{
							if(!playerHookInWater())
							{
								AutoFishLogger.log(Level.INFO, "WARNING: player cast timeout ...");
								
								// !< next time to cast fish-rop.
								scheduleNextCast();
								mode = MODE_FISH;
								lastCastAt = 0;
							}
							
							return;
						}
					}
				}
				
				
				/*
				else if (isFishingTimeout()) {

					AutoFishLogger.log(Level.INFO, "WARNING: player fishing timeout ...");

					if(playerHookInWater())
					{
						// !< start tick to avoid mutltiple reeling.
						startReelDelay();
						// !< Reel !
						playerUseRod();
					}

					// !< next time to cast fish-rop.
					scheduleNextCast();

					mode = MODE_FISH;

					lastCastAt = 0L;
				}	
*/
			}
		}
		
//		
//		if (ModAutoFish.config_autofish_enable 	&& !this.minecraft.isGamePaused() && this.minecraft.thePlayer != null) {
//			this.player = this.minecraft.thePlayer;
//
//			if (playerHookInWater() && !isDuringReelDelay() && isFishBiting()) {
//				// !< start tick to avoid mutltiple reeling.
//				startReelDelay();
//
//				AutoFishLogger.log(Level.INFO, "player Reeling ...");
//				// !< Reel !
//				playerUseRod();
//
//				// !< next time to cast fish-rop.
//				scheduleNextCast();
//
//				mode = MODE_FISH;
//
//				lastCastAt = 0L;
//			} else if (isTimeToCast()) {
//
//				if (mode == MODE_FISH) {
//					if (this.player.getFoodStats().getFoodLevel() <= 4) {
//						AutoFishLogger
//								.log(Level.INFO,
//										"WARNING: player is too hungry to perform fishing ...");
//						ModAutoFish.config_autofish_enable = false;// !< force
//																	// disable
//																	// the
//																	// function.
//						return;
//					}
//
//					if (this.player.getFoodStats().getFoodLevel() <= (20 - lastFoodHealth)) {
//
//						AutoFishLogger.log(Level.INFO, "Hungery? FoodLevel = "
//								+ this.player.getFoodStats().getFoodLevel()
//								+ ", lastFoodHealth = " + lastFoodHealth);
//
//						// if (!ModAutoFish.config_autofish_food_enable) {
//						// !< stop fishing.
//						// resetReelDelay();
//						// resetCastSchedule();
//						// return;
//						// }
//
//						// if (isFoodEatenTimeout())
//						{
//							if (tryToSwitchFood() == true) {
//								mode = MODE_FISH_FOOD;
//								AutoFishLogger.log(Level.INFO, "player try to eat ..., lastFoodHealth = " + lastFoodHealth);
//								RightKeyRepeat();
//								foodTick = FOOD_QUEUE_TICK_DELAY;
//
//								return;
//							} else {
//								AutoFishLogger.log(Level.INFO, "WARNING: player no food to eat ...");
//							}
//						}
//					}
//
//					if (needToSwitchRods()) {
//						tryToSwitchRods();
//					}
//
//					if (playerCanCast()) {
//						AutoFishLogger.log(Level.INFO, "player Use Rod ...");
//						playerUseRod();
//						lastCastAt = this.minecraft.theWorld.getTotalWorldTime();
//					}
//
//					// Resetting these values is not strictly necessary, but
//					// will improve the performance
//					// of the check that potentially occurs every tick.
//					resetReelDelay();
//					resetCastSchedule();
//
//				} else if (mode == MODE_FISH_FOOD) {
//
//					if (this.player.getFoodStats().getFoodLevel() <= 19) {
//						// !< continue eating.
//						RightKeyRepeat();
//					} else {
//						RightKeyReset();
//
//						// !< switch to fish mode.
//						mode = MODE_FISH;
//						this.lastEatFoodAt = 0;
//						this.foodTick = 0;
//
//						AutoFishLogger
//								.log(Level.INFO, "Player Eat Success ...");
//						if (!playerIsHoldingRod()) {
//							tryToSwitchRods();
//						}
//
//						scheduleNextCast();
//
//						return;
//					}
//
//					this.foodTick--;
//					if (this.foodTick <= 0) {
//						mode = MODE_FISH;
//						this.foodTick = 0;
//						RightKeyReset();
//						scheduleNextCast();
//
//						AutoFishLogger.log(Level.INFO, "Player Eat Fail ...");
//
//						this.lastEatFoodAt = this.minecraft.theWorld
//								.getTotalWorldTime();
//
//						if (!playerIsHoldingRod()) {
//							tryToSwitchRods();
//						}
//					}
//				}
//			} else if (isCastTimeout()) {
//
//				AutoFishLogger.log(Level.INFO, "WARNING: player cast timeout ...");
//
//				startReelDelay();
//
//				// !< Reel !
//				playerUseRod();
//
//				// !< next time to cast fish-rop.
//				scheduleNextCast();
//
//				mode = MODE_FISH;
//				lastCastAt = 0L;
//			}
//		}
	}

	private void resetCastSchedule() {
		this.castScheduledAt = 0;
	}

	private boolean needToSwitchRods() {
		return ModAutoFish.config_autofish_multirod && !playerCanCast();
	}

	private void scheduleNextCast() {
		this.castScheduledAt = this.minecraft.theWorld.getTotalWorldTime();
	}

	/*
	 * Trigger a delay so we don't use the rod multiple times for the same bite,
	 * which can persist for 2-3 ticks.
	 */
	private void startReelDelay() {
		this.startedReelDelayAt = this.minecraft.theWorld.getTotalWorldTime();
	}

	private void resetReelDelay() {
		startedReelDelayAt = 0;
	}

//	private boolean isFoodEatenTimeout() {
//		return (this.minecraft.theWorld.getTotalWorldTime() > this.lastEatFoodAt
//				+ FOOD_READY_TICK_DELAY);
//	}

	private boolean isDuringReelDelay() {
		return (this.startedReelDelayAt != 0 && this.minecraft.theWorld
				.getTotalWorldTime() < this.startedReelDelayAt
				+ REEL_TICK_DELAY);
	}

	private boolean playerHookInWater() {
		return this.player.fishEntity != null
				&& this.player.fishEntity.isInWater();
	}

	private boolean playerIsHoldingRod() {
		ItemStack heldItem = this.player.getHeldItemMainhand();

		return (heldItem != null && heldItem.getItem() == Items.FISHING_ROD 
				&& heldItem.getItemDamage() <= heldItem.getMaxDamage());
	}

	private boolean isFishBiting() {
		EntityPlayer serverPlayerEntity = getServerPlayerEntity();
		if (serverPlayerEntity != null) {
			/*
			 * If single player (integrated server), we can actually check to
			 * see if something is catchable, but it's fragile (other mods could
			 * break it) If anything goes wrong, fall back to the safer but less
			 * reliable method
			 */
			try {
				return isFishBiting_fromServerEntity(serverPlayerEntity);
			} catch (Exception e) {
				return isFishBiting_fromMovement();
			}
		} else {
			return isFishBiting_fromMovement();
		}
	}

	private boolean isFishBiting_fromMovement() {
		EntityFishHook fishEntity = this.player.fishEntity;
		if (fishEntity != null && fishEntity.motionX == 0
				&& fishEntity.motionZ == 0
				&& fishEntity.motionY < MOTION_Y_THRESHOLD) {
			return true;
		}
		return false;
	}

	private boolean isFishBiting_fromServerEntity(
			EntityPlayer serverPlayerEntity) throws NumberFormatException,
			NoSuchFieldException, SecurityException, IllegalArgumentException,
			IllegalAccessException {
		/*
		 * The fish hook entity on the server side knows whether a fish is
		 * catchable at any given time. However, that field is private and not
		 * exposed in any way. So we must use reflection to access that field.
		 */
		EntityFishHook serverFishEntity = serverPlayerEntity.fishEntity;
		int ticksCatchable = getPrivateIntFieldFromObject(serverFishEntity,
				"ticksCatchable", "field_146045_ax");
		if (ticksCatchable > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Using Java reflection APIs, access a private member data of type int
	 * 
	 * @param object
	 *            The target object
	 * @param fieldName
	 *            The name of the private data field in object
	 * 
	 * @return The int value of the private member data from object with
	 *         fieldName
	 */
	private int getPrivateIntFieldFromObject(Object object,
			String forgeFieldName, String vanillaFieldName)
			throws NoSuchFieldException, SecurityException,
			NumberFormatException, IllegalArgumentException,
			IllegalAccessException {
		Field targetField = null;
		try {
			targetField = object.getClass().getDeclaredField(forgeFieldName);
		} catch (NoSuchFieldException e) {
			targetField = object.getClass().getDeclaredField(vanillaFieldName);
		}
		if (targetField != null) {
			targetField.setAccessible(true);
			return Integer.valueOf(targetField.get(object).toString())
					.intValue();
		} else {
			return 0;
		}

	}

	private EntityPlayer getServerPlayerEntity() {
		if (this.minecraft.getIntegratedServer() == null
				|| this.minecraft.getIntegratedServer().getEntityWorld() == null) {
			return null;
		} else {
			return this.minecraft.getIntegratedServer().getEntityWorld()
					.getPlayerEntityByName(this.minecraft.thePlayer.getName());
		}
	}

	private EnumActionResult playerUseRod() {
		return this.minecraft.playerController.processRightClick(this.player,
				this.minecraft.theWorld, this.player.getHeldItemMainhand(),
				EnumHand.MAIN_HAND);
	}

	// !< XXX
	private EnumActionResult RightKeyRepeat() {

		ItemStack heldItem = this.player.getHeldItemMainhand();
		if (heldItem != null && isItemFood(heldItem)) {
			int keyCode = this.minecraft.gameSettings.keyBindUseItem.getKeyCode();

			KeyBinding.setKeyBindState(keyCode, true);
			KeyBinding.onTick(keyCode);

		} else {
			AutoFishLogger.log(Level.INFO, "Damm server, What are you eating?!");
		}

		return EnumActionResult.SUCCESS;
	}

	private EnumActionResult RightKeyReset() {
		KeyBinding.unPressAllKeys();
		return EnumActionResult.SUCCESS;
	}
	
	private void tryToSwitchPickAxe() {
		InventoryPlayer inventory = this.player.inventory;
		for (int i = 0; i < 8/*9*/; i++) {
			ItemStack curItemStack = inventory.mainInventory[i];
			if (curItemStack != null && curItemStack.getItem() == Items.STONE_PICKAXE
					&& (curItemStack.getMaxDamage() - curItemStack.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD)) 
			{
				inventory.currentItem = i;

				AutoFishLogger.log(Level.INFO, "try To Switch PickAxe, currentItem = " + inventory.currentItem);
				this.player.setHeldItem(EnumHand.MAIN_HAND, curItemStack);
				break;
			}
		}
	}
	
	private boolean playerCanUserPickAxe() {
		ItemStack heldItem = this.player.getHeldItemMainhand();
		
		if (heldItem != null && isItemPickAxe(heldItem))
		{
			if(heldItem.getMaxDamage() - heldItem.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD)
			{
				return true;
			}
		}

		AutoFishLogger.log(Level.INFO, "Current pickaxe cannot be used");
		return false;
	}
	
	// !< XXX
	private boolean tryUsingPickAxe() {

		ItemStack heldItem = this.player.getHeldItemMainhand();
		if (heldItem != null && isItemPickAxe(heldItem)) {
			
			int keyCode = this.minecraft.gameSettings.keyBindAttack.getKeyCode();

			KeyBinding.setKeyBindState(keyCode, true);
			KeyBinding.onTick(keyCode);

		} else {
			AutoFishLogger.log(Level.INFO, "try use pickaxe failed.");
			return false;
		}

		return true;
	}

	private boolean stopUsePickAxe() {
		KeyBinding.unPressAllKeys();
		return true;
	}
	
	private boolean isTimeToCast() {
		return (this.castScheduledAt != 0 
				&& this.minecraft.theWorld.getTotalWorldTime() > this.castScheduledAt + CAST_QUEUE_TICK_DELAY);
	}

//	private boolean isFishingTimeout() {
//		if(ModAutoFish.config_autofish_longtimer)
//		{
//			return false;
//		}
//		return (this.lastCastAt != 0 && this.minecraft.theWorld.getTotalWorldTime() > this.lastCastAt + 1800 + 100);// 900*2/20.0 seconds
//	}
//	
//	private boolean isCastTimeout() {
//		return (this.lastCastAt != 0 && this.minecraft.theWorld.getTotalWorldTime() > this.lastCastAt + 100);// 5 seconds for the fish-pod cast.
//	}

	private void tryToSwitchRods() {
		InventoryPlayer inventory = this.player.inventory;
		for (int i = 0; i < 8/*9*/; i++) {
			ItemStack curItemStack = inventory.mainInventory[i];
			if (curItemStack != null
					&& curItemStack.getItem() == Items.FISHING_ROD
					&& (!ModAutoFish.config_autofish_preventBreak || (curItemStack.getMaxDamage() - curItemStack.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD))) {
				inventory.currentItem = i;

				AutoFishLogger.log(Level.INFO,
						"try To Switch Fishing Rods, currentItem = "
								+ inventory.currentItem);
				this.player.setHeldItem(EnumHand.MAIN_HAND, curItemStack);
				break;
			}
		}
	}

	private boolean tryToSwitchFood() {
		int health = 0;
		InventoryPlayer inventory = this.player.inventory;
		for (int i = 0; i < 8/*9*/; i++) {
			ItemStack curItemStack = inventory.mainInventory[i];
			health = getItemFoodHealth(curItemStack);
			if (health > 0) {
				inventory.currentItem = i;
				lastFoodHealth = health;
				this.player.setHeldItem(EnumHand.MAIN_HAND, curItemStack);
				AutoFishLogger.log(Level.INFO,
						"try To Switch Food, currentItem = " + inventory.currentItem + ", FoodHealth = " + health);
				return true;
			}
		}

		AutoFishLogger.log(Level.INFO, "WARNING: Switch food fail...");
		return false;
	}

	private int getItemFoodHealth(ItemStack itemStack) {
		Item item = itemStack.getItem();

		if (itemStack.getItem() == Items.FISH) {
			return 0;
		}

		if (item instanceof ItemFood) {
			ItemFood food = (ItemFood) item;

			if (food.getHealAmount(itemStack) >= 4) {
				return food.getHealAmount(itemStack);
			}
		}

		return 0;
	}

	private boolean isItemPickAxe(ItemStack itemStack) {
		if (itemStack.getItem() == Items.STONE_PICKAXE) {
			return true;
		}	
		
		if (itemStack.getItem() == Items.IRON_PICKAXE) {
			return true;
		}	
		
		//TODO		
		return false;
	}
	
	private boolean isItemFood(ItemStack itemStack) {
		Item item = itemStack.getItem();

		if (itemStack.getItem() == Items.FISH) {
			return false;
		}

		if (item instanceof ItemFood) {
			ItemFood food = (ItemFood) item;

			if (food.getHealAmount(itemStack) >= 4) {
				return true;
			}
		}
		/*
		 * if (itemStack.getItem() == Items.COOKED_BEEF) { return true; }
		 * 
		 * if (itemStack.getItem() == Items.COOKED_PORKCHOP) { return true; }
		 * 
		 * if (itemStack.getItem() == Items.COOKED_MUTTON) { return true; }
		 * 
		 * if (itemStack.getItem() == Items.COOKED_CHICKEN) { return true; }
		 * 
		 * if (itemStack.getItem() == Items.COOKED_FISH) { return true; }
		 * 
		 * if (itemStack.getItem() == Items.COOKED_RABBIT) { return true; }
		 */
		return false;
	}

	private boolean playerCanCast() {
		if (!playerIsHoldingRod()) {
			return false;
		} else {
			ItemStack heldItem = this.player.getHeldItemMainhand();

			return (!ModAutoFish.config_autofish_preventBreak || (heldItem
					.getMaxDamage() - heldItem.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD));
		}
	}

	@SubscribeEvent
	public void onConfigChanged(
			ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
		if (eventArgs.getModID().equals(ModAutoFish.MODID)) {

			AutoFishLogger.log(Level.INFO, "ModAutoFish.syncConfig().");
			ModAutoFish.syncConfig();
		}
	}

}
