package com.netease.minecraft.autojump;


import java.lang.reflect.Field;

import org.apache.logging.log4j.Level;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class AutoFishEventHandler {

    private Minecraft minecraft;
    private EntityPlayer player;
    private long castScheduledAt = 0L;
    private long startedReelDelayAt = 0L;
    private long startedCastDelayAt = 0L;
    private boolean isFishing = false;


    
    private static final int TICKS_PER_SECOND = 20;

    /** How long to suppress checking for a bite after starting to reel in.  If we check for a bite while reeling
        in, we may think we have a bite and try to reel in again, which will actually cause a re-cast and lose the fish */
    private static final int REEL_TICK_DELAY = 10;

    /** How long to wait after casting to check for Entity Clear.  If we check too soon, the hook entity
        isn't in the world yet, and will trigger a false alarm and cause infinite recasting. */
    private static final int CAST_TICK_DELAY = 5;

    /** When Break Prevention is enabled, how low to let the durability get before stopping or switching rods */
    private static final int AUTOFISH_BREAKPREVENT_THRESHOLD = 2;

    /** The threshold for vertical movement of the fish hook that determines when a fish is biting, if using
        the movement method of detection. */
    private static final double MOTION_Y_THRESHOLD = -0.05d;

    //!< time for the stone-pickaxe to pick the stone.
    private static final int PICKAXE_ACTION_DELAY1 = 20;

    //!< time for the stone regenerated.
    private static final int PICKAXE_ACTION_DELAY2 = 35;

    
    //!< -------------------------------- ><8 ---------------------------------
    private long lastCastAt = 0L;
    private long lastPickAxe = 0L;
    private boolean pickaxe_enable = true;
    
    //!<-----------------------------------------------------------------------
    
	private static final int MODE_STONE = 1;
	private static final int MODE_STONE_FOOD = 2;
	
	private boolean isFoodEating = false;
    private int foodTick = 0;
	private int lastFoodHealth = 8;
    
    public AutoFishEventHandler() {
        this.minecraft = FMLClientHandler.instance().getClient();
    }
    
    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent event) {
    	
        //!< XXX auto pick stone.
        if (ModAutoFish.config_autofast_stone_enable 
        		&& !this.minecraft.isGamePaused() && this.minecraft.player != null) {
        	this.player = this.minecraft.player;
        	
            if(lastPickAxe == 0)
            {
                this.lastPickAxe = this.minecraft.world.getTotalWorldTime();
                pickaxe_enable = false;
                AutoFishLogger.log(Level.INFO, "player stop pickaxe[1] ... %d", this.lastPickAxe);
                return;
            }

            if(pickaxe_enable)
            {
                long cur_pickaxe = this.minecraft.world.getTotalWorldTime();

                if (cur_pickaxe < 1000 && this.lastPickAxe >= 192000L) {
                    this.lastPickAxe = cur_pickaxe;
                }

                if(cur_pickaxe - this.lastPickAxe >= 40) //!< 
                {
                    stopUsePickAxe();
                    this.lastPickAxe = this.minecraft.world.getTotalWorldTime();
                    pickaxe_enable = false;

                    AutoFishLogger.log(Level.INFO, "player stop pickaxe[2] ... %d", this.lastPickAxe);
                }
                else
                {
                    if(playerCanUserPickAxe())
                    {
                        tryUsingPickAxe();
                    }
                    else
                    {
                        stopUsePickAxe();
                        this.lastPickAxe = this.minecraft.world.getTotalWorldTime();
                        pickaxe_enable = false;
                        AutoFishLogger.log(Level.INFO, "player stop pickaxe[3] ... %d", this.lastPickAxe);
                    }
                }
            }
            else
            {
            	
            	if(isFoodEating){
            		if (this.player.getFoodStats().getFoodLevel() >= 18) {
            			isFoodEating = false;
            			RightKeyReset();
            			return;
            		}
            		
					if (tryToSwitchFood() == true) {
						AutoFishLogger.log(Level.INFO,	"player try to eat ..., lastFoodHealth = " 	+ lastFoodHealth);
						RightKeyRepeat();

						return;
					} else {
						isFoodEating = false;
						AutoFishLogger.log(Level.INFO, "WARNING: player no food to eat ...");
					}
            		
            		return;
            	}else{
            		if (this.player.getFoodStats().getFoodLevel() <= 12) {
            			isFoodEating = true;
            			return;
            		}
            	}
				
            	
                long cur_pickaxe = this.minecraft.world.getTotalWorldTime();
                if (cur_pickaxe < 1000 && this.lastPickAxe >= 192000L) {
                    this.lastPickAxe = cur_pickaxe;
                }

                if(cur_pickaxe - this.lastPickAxe > 10)//!< 35 --> 5
                {
                    tryToSwitchPickAxe();
                    this.lastPickAxe = this.minecraft.world.getTotalWorldTime();
                    pickaxe_enable = true;

                    AutoFishLogger.log(Level.INFO, "player start using pickaxe ... %d", this.lastPickAxe);
                }
            }
            return;
        }
        
        
        //!< XXX auto pick stone.
        if (ModAutoFish.config_autostone_enable 
        		&& !this.minecraft.isGamePaused() && this.minecraft.player != null) {
        	this.player = this.minecraft.player;
            if(lastPickAxe == 0)
            {
                this.lastPickAxe = this.minecraft.world.getTotalWorldTime();
                pickaxe_enable = false;
                return;
            }

            if(pickaxe_enable)
            {
                long cur_pickaxe = this.minecraft.world.getTotalWorldTime();

                if (cur_pickaxe < 1000 && this.lastPickAxe >= 192000L) {
                    this.lastPickAxe = cur_pickaxe;
                }

                if(cur_pickaxe - this.lastPickAxe >= PICKAXE_ACTION_DELAY1)
                {
                    stopUsePickAxe();
                    this.lastPickAxe = this.minecraft.world.getTotalWorldTime();
                    pickaxe_enable = false;

                    AutoFishLogger.log(Level.INFO, "player stop pickaxe ... %d", this.lastPickAxe);
                }
                else
                {
                    if(playerCanUserPickAxe())
                    {
                        tryUsingPickAxe();
                    }
                    else
                    {
                        stopUsePickAxe();
                        this.lastPickAxe = this.minecraft.world.getTotalWorldTime();
                        pickaxe_enable = false;
                    }
                }
            }
            else
            {
                long cur_pickaxe = this.minecraft.world.getTotalWorldTime();
                if (cur_pickaxe < 1000 && this.lastPickAxe >= 192000L) {
                    this.lastPickAxe = cur_pickaxe;
                }

                if(cur_pickaxe - this.lastPickAxe > PICKAXE_ACTION_DELAY2)
                {
                    tryToSwitchPickAxe();
                    this.lastPickAxe = this.minecraft.world.getTotalWorldTime();
                    pickaxe_enable = true;

                    AutoFishLogger.log(Level.INFO, "player start using pickaxe ... %d", this.lastPickAxe);
                }
            }
            return;
        }

        if (ModAutoFish.config_autofish_enable && !this.minecraft.isGamePaused() && this.minecraft.player != null) {
            this.player = this.minecraft.player;

            if (playerIsHoldingRod()) {
                if (playerHookInWater() && !isDuringReelDelay() && isFishBiting()) {
                    startReelDelay();
					if (lastCastAt != 0) {
						long tick = this.minecraft.world.getTotalWorldTime();
						long diff = tick - lastCastAt;
						AutoFishLogger.log(Level.INFO,
										"[2]player Reeling : timestamp = %d, %f seconds",
										tick, diff / 20.0f);
					} else {
						AutoFishLogger.log(Level.INFO,
								"[2]player Reeling : timestamp = %d tick",
								this.minecraft.world.getTotalWorldTime());
					}
					
                    reelIn();
                    scheduleNextCast();
                    lastCastAt = 0L;
                } else if (isTimeToCast()) {
                    if (needToSwitchRods()) {
                        tryToSwitchRods();
                    }
                    if (playerCanCast()) {
                    	AutoFishLogger.log(Level.INFO, "[1]player Use Rod : timestamp =  %d", this.minecraft.world.getTotalWorldTime());
                        startFishing();
                        lastCastAt = this.minecraft.world.getTotalWorldTime();
                    }
                    // Resetting these values is not strictly necessary, but will improve the performance
                    // of the check that potentially occurs every tick.
                    resetReelDelay();
                    resetCastSchedule();
                }
				else {
					if(lastCastAt != 0) {
						long cur = this.minecraft.world.getTotalWorldTime();
						if (cur + 1000 < this.lastCastAt) {
							//AutoFishLogger.log(Level.INFO, "WARNING: current %d, lastCastAt = %d ...", cur, lastCastAt);
							cur += 192000L;
						}
						
						//!< 150 seconds??
						if(cur > this.lastCastAt + 2400 + 100) 	{
							AutoFishLogger.log(Level.INFO, "WARNING: player fishing timeout ...");
							
							if(playerHookInWater()) {
								AutoFishLogger.log(Level.INFO, "Timeout Recover: start reel ...");
								// !< start tick to avoid mutltiple reeling.
								startReelDelay();
								// !< Reel !
								reelIn();
								
								// !< next time to cast fish-rop.
								scheduleNextCast();
								lastCastAt = 0;
							} else
							{
								AutoFishLogger.log(Level.INFO, "Timeout Recover: cast fish-rod ...");
		                        startFishing();
		                        lastCastAt = this.minecraft.world.getTotalWorldTime();
							}

							return;
						}
						else if(cur > this.lastCastAt + 100)
						{
							if(!playerHookInWater())
							{
								AutoFishLogger.log(Level.INFO, "WARNING: player cast timeout ...");
								
								// !< next time to cast fish-rop.
								scheduleNextCast();
								lastCastAt = 0;
							}
							
							return;
						}
					}
				}
                
//                if (ModAutoFish.config_autofish_entityClearProtect && this.isFishing && !isDuringCastDelay() && this.player.fishEntity == null) {
//                    AutoFishLogger.info("Entity Clear detected.  Re-casting.");
//                    this.isFishing = false;
//                    startFishing();
//                }
                
            } else {
                this.isFishing = false;
            }
        }
    }
    
    @SubscribeEvent
    public void onPlayerUseItem(PlayerInteractEvent.RightClickItem event) {
    	if (ModAutoFish.config_autostone_enable){
    		return;
    	}
    	
        // Only do this on the client side
        if (event.getWorld().isRemote && playerIsHoldingRod()) {
            this.isFishing = !this.isFishing;
//            AutoFishLogger.info("Player %s fishing", this.isFishing ? "started" : "stopped");
            if (this.isFishing) {
                startCastDelay();
            }
        }
    }
    
    private void reelIn() {
        playerUseRod();
    }

    private void startFishing() {
        playerUseRod();
        startCastDelay();
    }

    private void resetCastSchedule() {
        this.castScheduledAt = 0;
    }

    private boolean needToSwitchRods() {
        return /*ModAutoFish.config_autofish_multirod && */!playerCanCast();
    }

    private void scheduleNextCast() {
        this.castScheduledAt = this.minecraft.world.getTotalWorldTime();
    }

    /*
     *  Trigger a delay so we don't use the rod multiple times for the same bite,
     *  which can persist for 2-3 ticks.
     */
    private void startReelDelay() {
        this.startedReelDelayAt = this.minecraft.world.getTotalWorldTime();
    }

    /*
     * Trigger a delay so that entity clear protection doesn't kick in during cast.
     */
    private void startCastDelay() {
        this.startedCastDelayAt = this.minecraft.world.getTotalWorldTime();
    }

    private void resetReelDelay() {
        startedReelDelayAt = 0;
    }

    private boolean isDuringReelDelay() {
        return (this.startedReelDelayAt != 0 && this.minecraft.world.getTotalWorldTime() < this.startedReelDelayAt + REEL_TICK_DELAY);
    }
    
    private boolean isDuringCastDelay() {
        return (this.startedCastDelayAt != 0 && this.minecraft.world.getTotalWorldTime() < this.startedCastDelayAt + CAST_TICK_DELAY);
    }
    
    private boolean playerHookInWater() {
        return this.player.fishEntity != null
                && this.player.fishEntity.isInWater();
    }

    private boolean playerIsHoldingRod() {
    	try{
    		this.player = this.minecraft.player;
	        ItemStack heldItem = this.player.getHeldItemMainhand();
	
	        return (heldItem != null
	                && heldItem.getItem() == Items.FISHING_ROD
	                && heldItem.getItemDamage() <= heldItem.getMaxDamage());
    	}catch(Exception ex){
    		return false;
    	}
    }

    private boolean isFishBiting() {
        EntityPlayer serverPlayerEntity = getServerPlayerEntity();
        if (serverPlayerEntity != null) {
            /* If single player (integrated server), we can actually check to see if something
             * is catchable, but it's fragile (other mods could break it)
             * If anything goes wrong, fall back to the safer but less reliable method
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
        if (fishEntity != null 
                // Checking for no X and Z motion prevents a false alarm when the hook is moving through the air
                && fishEntity.motionX == 0 
                && fishEntity.motionZ == 0 
                && fishEntity.motionY < MOTION_Y_THRESHOLD) {
            return true;
        }
        return false;
    }

    private boolean isFishBiting_fromServerEntity(EntityPlayer serverPlayerEntity) throws NumberFormatException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        /*
         * The fish hook entity on the server side knows whether a fish is catchable at any given time.  However,
         * that field is private and not exposed in any way.  So we must use reflection to access that field.
         */
        EntityFishHook serverFishEntity = serverPlayerEntity.fishEntity;
        int ticksCatchable = getPrivateIntFieldFromObject(serverFishEntity, "ticksCatchable", "field_146045_ax");
        if (ticksCatchable > 0) {
            return true;
        }
        return false;
    }

    /**
     * Using Java reflection APIs, access a private member data of type int
     * 
     * @param object The target object
     * @param fieldName The name of the private data field in object
     * 
     * @return The int value of the private member data from object with fieldName
     */
    private int getPrivateIntFieldFromObject(Object object, String forgeFieldName, String vanillaFieldName) throws NoSuchFieldException, SecurityException, NumberFormatException, IllegalArgumentException, IllegalAccessException {
        Field targetField = null;
        try {
            targetField = object.getClass().getDeclaredField(forgeFieldName);
        } catch (NoSuchFieldException e) {
            targetField = object.getClass().getDeclaredField(vanillaFieldName);
        }
        if (targetField != null) {
            targetField.setAccessible(true);
            return Integer.valueOf(targetField.get(object).toString()).intValue();
        } else {
            return 0;
        }
            
    }

    private EntityPlayer getServerPlayerEntity() {
        if (this.minecraft.getIntegratedServer() == null || this.minecraft.getIntegratedServer().getEntityWorld() == null) {
            return null;
        } else {
            return this.minecraft.getIntegratedServer().getEntityWorld().getPlayerEntityByName(this.minecraft.player.getName());
        }
    }

    private EnumActionResult playerUseRod() {
    	this.player = this.minecraft.player;
        return this.minecraft.playerController.processRightClick(
                this.player, 
                this.minecraft.world, 
                EnumHand.MAIN_HAND);
    }
    
	private boolean tryToSwitchFood() {
		int health = 0;
		InventoryPlayer inventory = this.player.inventory;
		for (int i = 0; i < 8/*9*/; i++) {
			//ItemStack curItemStack = inventory.mainInventory[i];
			ItemStack curItemStack = inventory.getItemStack();
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


    private void tryToSwitchPickAxe() {
    	this.player = this.minecraft.player;
        if(this.player == null){
        	return;
        }
        
        InventoryPlayer inventory = this.player.inventory;
        if(inventory == null){
        	return;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack curItemStack = inventory.getStackInSlot(i);
            //ItemStack curItemStack = inventory.mainInventory[i];
            if (curItemStack != null
            		&& isItemPickAxe(curItemStack)
                    && (curItemStack.getMaxDamage() - curItemStack.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD))
            {
                inventory.currentItem = i;

                AutoFishLogger.log(Level.INFO, "try To Switch PickAxe, currentItem = " + inventory.currentItem);
                this.player.setHeldItem(EnumHand.MAIN_HAND, curItemStack);
                break;
            }
        }
    }
    
    private boolean isItemPickAxe(ItemStack heldItem) {
    	
    	if(heldItem.getItemDamage() < heldItem.getMaxDamage()){
    		
    		if(heldItem.getItem() == Items.STONE_PICKAXE
    				|| heldItem.getItem() == Items.IRON_PICKAXE
    				|| heldItem.getItem() == Items.DIAMOND_PICKAXE){
    	
    			return true;    			
    		}
    	}

    	return false;
    }
    

	private boolean playerCanUserPickAxe() {
		try {
			this.player = this.minecraft.player;
			if (this.player == null) {
				return false;
			}
			ItemStack heldItem = this.player.getHeldItemMainhand();

			if (heldItem != null && isItemPickAxe(heldItem)) {
				if (heldItem.getMaxDamage() - heldItem.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD) {
					return true;
				}
			}

			AutoFishLogger.log(Level.INFO, "Current pickaxe cannot be used");
			return false;
		} catch (Exception ex) {

		}

		return false;
	}
	
    private boolean RightKeyRepeat() {
    	this.player = this.minecraft.player;
        if(this.player == null){
        	return false;
        }

        ItemStack heldItem = this.player.getHeldItemMainhand();
        if (heldItem != null) {

            int keyCode = this.minecraft.gameSettings.keyBindAttack.getKeyCode();

            KeyBinding.setKeyBindState(keyCode, true);
            KeyBinding.onTick(keyCode);

        } else {
            return false;
        }

        return true;
    }
    
	private EnumActionResult RightKeyReset() {
		KeyBinding.unPressAllKeys();
		return EnumActionResult.SUCCESS;
	}

    // !< XXX
    private boolean tryUsingPickAxe() {
    	this.player = this.minecraft.player;
        if(this.player == null){
        	return false;
        }

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
        return (this.castScheduledAt != 0 && this.minecraft.world.getTotalWorldTime() > this.castScheduledAt + (ModAutoFish.config_autofish_recastDelay * TICKS_PER_SECOND));
    }

    private void tryToSwitchRods() {
        if(this.player == null){
        	return;
        }
        InventoryPlayer inventory = this.player.inventory;
        for (int i = 0; i < 9; i++) {
            ItemStack curItemStack = inventory.mainInventory.get(i);
            if (curItemStack != null 
                    && curItemStack.getItem() == Items.FISHING_ROD
                    && (!ModAutoFish.config_autofish_preventBreak || (curItemStack.getMaxDamage() - curItemStack.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD))
                ) {
                inventory.currentItem = i;
                break;
            }
        }
    }

    private boolean playerCanCast() {
        if (!playerIsHoldingRod()) {
            return false;
        } else {
            ItemStack heldItem = this.player.getHeldItemMainhand();
    
            return (!ModAutoFish.config_autofish_preventBreak 
                    || (heldItem.getMaxDamage() - heldItem.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD)
                    );
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if (eventArgs.getModID().equals(ModAutoFish.MODID)) {
            ModAutoFish.syncConfig();
        }
    }
    
}