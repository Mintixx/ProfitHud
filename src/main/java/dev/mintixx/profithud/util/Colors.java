package dev.mintixx.profithud.util;

/** ARGB colour helpers. */
public final class Colors {
	private Colors() {}

	public static int a(int c) { return c >>> 24; }
	public static int r(int c) { return (c >> 16) & 0xFF; }
	public static int g(int c) { return (c >> 8) & 0xFF; }
	public static int b(int c) { return c & 0xFF; }

	public static int argb(int a, int r, int g, int b) {
		return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
	}

	public static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

	public static int lerp(int c1, int c2, float t) {
		if (c1 == c2) return c1;
		t = Math.max(0f, Math.min(1f, t));
		return argb(
			Math.round(a(c1) + (a(c2) - a(c1)) * t),
			Math.round(r(c1) + (r(c2) - r(c1)) * t),
			Math.round(g(c1) + (g(c2) - g(c1)) * t),
			Math.round(b(c1) + (b(c2) - b(c1)) * t));
	}

	public static int withAlpha(int c, int alpha) { return (clamp(alpha) << 24) | (c & 0xFFFFFF); }

	public static int mulAlpha(int c, float m) { return withAlpha(c, Math.round(a(c) * Math.max(0f, Math.min(1f, m)))); }

	public static int brighten(int c, float f) {
		return argb(a(c), Math.round(r(c) * f), Math.round(g(c) * f), Math.round(b(c) * f));
	}

	public static String hex(int c) { return String.format("#%08X", c); }
}
