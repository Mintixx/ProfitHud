package dev.mintixx.profithud.hud;

import dev.mintixx.profithud.config.ConfigManager;
import dev.mintixx.profithud.config.ProfitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Purely visual state: smooth number counters, payment flash, fade-in. Never touches statistics. */
public final class Animations {
	private final ConfigManager cm;

	public double dBalance = Double.NaN, dEarned, dSpent, dNet, dPerMinute, dPerHour, dEst;
	public float appear = 0f;

	private boolean init = false;
	private long lastNs = System.nanoTime();
	private long flashStartMs = -1;
	private boolean flashIncoming = true;

	public Animations(ConfigManager cm) { this.cm = cm; }

	private static double step(double cur, double target, double k) {
		if (Double.isNaN(target)) return Double.NaN;
		if (Double.isNaN(cur)) return target;
		double d = target - cur;
		if (Math.abs(d) < Math.max(0.5, Math.abs(target) * 1e-4)) return target;
		return cur + d * k;
	}

	/** Call once per rendered frame with the freshest values. */
	public void update(HudData t) {
		long now = System.nanoTime();
		double dt = Math.min(0.25, (now - lastNs) / 1e9);
		lastNs = now;
		ProfitConfig c = cm.get();

		appear = c.hudTransitions ? (float) (appear + (1f - appear) * (1 - Math.exp(-dt * 7))) : 1f;
		if (appear > 0.995f) appear = 1f;

		if (!init || !c.counterAnimation) {
			dBalance = t.balance(); dEarned = t.earned(); dSpent = t.spent(); dNet = t.net();
			dPerMinute = t.perMinute(); dPerHour = t.perHour(); dEst = t.estimatedInventory();
			init = true;
			return;
		}
		double k = 1 - Math.exp(-dt * 9);
		dBalance = step(dBalance, t.balance(), k);
		dEarned = step(dEarned, t.earned(), k);
		dSpent = step(dSpent, t.spent(), k);
		dNet = step(dNet, t.net(), k);
		dPerMinute = step(dPerMinute, t.perMinute(), k);
		dPerHour = step(dPerHour, t.perHour(), k);
		dEst = step(dEst, t.estimatedInventory(), k);
	}

	/** Restart the fade-in (called when leaving a world). */
	public void resetAppear() { appear = 0f; }

	public void onPayment(boolean incoming) {
		ProfitConfig c = cm.get();
		flashStartMs = System.currentTimeMillis();
		flashIncoming = incoming;
		if (c.sounds) playSound(incoming, c.soundVolume);
	}

	private static void playSound(boolean incoming, float volume) {
		try {
			Minecraft mc = Minecraft.getInstance();
			mc.getSoundManager().play(SimpleSoundInstance.forUI(
				incoming ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_YES,
				incoming ? 1.2f : 0.8f, Math.max(0f, Math.min(1f, volume))));
		} catch (Throwable ignored) {
			// Sound is optional; never let it break the HUD.
		}
	}

	/** 1 right after a payment, fading to 0. */
	public float flash() {
		ProfitConfig c = cm.get();
		if (flashStartMs < 0 || c.flashDurationMs <= 0) return 0f;
		float t = (System.currentTimeMillis() - flashStartMs) / (float) c.flashDurationMs;
		if (t >= 1f) return 0f;
		float inv = 1f - t;
		return inv * inv;
	}

	public boolean flashIncoming() { return flashIncoming; }

	/** Manual trigger used by the "Test HUD" button (visual only, no sound, no stats). */
	public void testFlash(boolean incoming) {
		flashStartMs = System.currentTimeMillis();
		flashIncoming = incoming;
	}
}
