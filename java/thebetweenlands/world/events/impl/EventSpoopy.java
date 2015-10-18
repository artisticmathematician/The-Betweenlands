package thebetweenlands.world.events.impl;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import thebetweenlands.TheBetweenlands;
import thebetweenlands.world.events.EnvironmentEvent;
import thebetweenlands.world.events.EnvironmentEventRegistry;

public class EventSpoopy extends EnvironmentEvent {
	private static final long SPOOPY_DATE = new GregorianCalendar(Calendar.getInstance().get(Calendar.YEAR), 9, 21, 0, 0).getTime().getTime();

	private World lastWorld;
	private boolean chatSent = false;
	private float skyTransparency = 0.0F;
	private float lastSkyTransparency = 0.0F;

	public void setSkyTransparency(float transparency) {
		this.lastSkyTransparency = this.skyTransparency;
		this.skyTransparency = transparency;
	}

	public float getSkyTransparency(float partialTicks) {
		return (this.skyTransparency + (this.skyTransparency - this.lastSkyTransparency) * partialTicks) / 3.0F;
	}

	public EventSpoopy(EnvironmentEventRegistry registry) {
		super(registry);
	}

	public long getDayDiff() {
		return TimeUnit.DAYS.convert(Calendar.getInstance().getTime().getTime() - SPOOPY_DATE, TimeUnit.MILLISECONDS);
	}

	private boolean wasSet = false;

	@Override
	public String getEventName() {
		return "Spook";
	}

	@Override
	public void setActive(boolean active, boolean markDirty) {
		if(active && TheBetweenlands.proxy.getClientWorld() != null && this.lastWorld != TheBetweenlands.proxy.getClientWorld() && TheBetweenlands.proxy.getClientPlayer() != null) {
			this.lastWorld = TheBetweenlands.proxy.getClientWorld();
			EntityPlayer player = TheBetweenlands.proxy.getClientPlayer();
			player.addChatMessage(new ChatComponentText("You feel a chill in the air... the haunting season has begun!"));
		}
		super.setActive(active, markDirty);
	}

	@Override
	public void update(World world) {
		super.update(world);
		if(!world.isRemote) {
			long dayDiff = this.getDayDiff();
			if(dayDiff >= 0 && dayDiff <= 11) {
				if(!this.isActive() && !this.wasSet) {
					this.setActive(true, true);
					this.wasSet = true;
				}
			} else if(this.wasSet) {
				this.wasSet = false;
				this.setActive(false, true);
			}
		} else {
			if(this.isActive()) {
				if(this.skyTransparency < 1.0F) {
					this.setSkyTransparency(this.skyTransparency + 0.003F);
				}
				if(this.skyTransparency > 1.0F) {
					this.setSkyTransparency(1.0F);
				}
			} else {
				if(this.skyTransparency > 0.0F) {
					this.setSkyTransparency(this.skyTransparency - 0.003F);
				}
				if(this.skyTransparency < 0.0F) {
					this.setSkyTransparency(0.0F);
				}
			}
		}
	}

	@Override
	public void saveEventData() { 
		super.saveEventData();
		this.getData().setBoolean("wasSet", this.wasSet);
	}

	@Override
	public void loadEventData() { 
		super.loadEventData();
		this.wasSet = this.getData().getBoolean("wasSet");
	}
}
