package com.netease.minecraft.autojump;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class KeyInputHandler {

    public KeyBinding options;
    
    public KeyInputHandler() {
        init();
    }
    
    public void init() {
        options = new KeyBinding("key.options", Keyboard.KEY_V, "key.categories.mod_autojump");
        ClientRegistry.registerKeyBinding(options);
    }
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (this.options.isPressed()) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (playerIsHoldingFishingRod(player)) {
                Minecraft.getMinecraft().displayGuiScreen(new AutoFishConfigGui(Minecraft.getMinecraft().currentScreen));
            }
        }
    }

    private boolean playerIsHoldingFishingRod(EntityPlayer player) {
    	
    	if(!Minecraft.getMinecraft().isGamePaused())
    	{
    		if(player != null && player.getHeldItemMainhand() != null)
    		{
    			Item item = player.getHeldItemMainhand().getItem();
    			if(item == Items.FISHING_ROD 
    					|| item == Items.STONE_PICKAXE 
    					|| item == Items.IRON_PICKAXE)
    			{
    				return true;
    			}
    		}
    	}
    	
    	return false;
    	
    	/*
        return (!Minecraft.getMinecraft().isGamePaused()
                && player != null
                && player.getHeldItemMainhand() != null
                && (player.getHeldItemMainhand().getItem() == Items.FISHING_ROD 
                || player.getHeldItemMainhand().getItem() == Items.STONE_PICKAXE)
                
        		);
        		*/
    }
    
}
