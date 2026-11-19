package dev.cart3r.client;

public final class Setting {
	private final String name;
	private final double min;
	private final double max;
	private final double step;
	private final boolean toggle;
	private final String[] labels;
	private double value;
	public float anim;

	private Setting(String name, double min, double max, double step, boolean toggle, double value, String[] labels) {
		this.name = name;
		this.min = min;
		this.max = max;
		this.step = step;
		this.toggle = toggle;
		this.value = value;
		this.labels = labels;
		this.anim = toggle && value >= 0.5 ? 1f : 0f;
	}

	public static Setting number(String name, double min, double max, double def, double step) {
		return new Setting(name, min, max, step, false, def, null);
	}

	public static Setting toggle(String name, boolean def) {
		return new Setting(name, 0, 1, 1, true, def ? 1 : 0, null);
	}

	public static Setting options(String name, int def, String... labels) {
		return new Setting(name, 0, labels.length - 1, 1, false, def, labels);
	}

	public String name() {
		return name;
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public boolean isToggle() {
		return toggle;
	}

	public boolean isOptions() {
		return labels != null;
	}

	public boolean bool() {
		return value >= 0.5;
	}

	public double num() {
		return value;
	}

	public double fraction() {
		return max > min ? (value - min) / (max - min) : 0;
	}

	public void setFraction(double f) {
		setValue(min + Math.max(0, Math.min(1, f)) * (max - min));
	}

	public void setValue(double v) {
		if (toggle) {
			value = v >= 0.5 ? 1 : 0;
			return;
		}
		double c = Math.max(min, Math.min(max, v));
		if (step > 0) {
			c = min + Math.round((c - min) / step) * step;
		}
		value = Math.max(min, Math.min(max, c));
	}

	public void bump(int dir, boolean big) {
		if (toggle) {
			value = bool() ? 0 : 1;
			return;
		}
		double s = step * (big ? 5 : 1);
		double v = value + dir * s;
		if (v > max + 1.0E-9) {
			v = value >= max - 1.0E-9 ? min : max;
		} else if (v < min - 1.0E-9) {
			v = value <= min + 1.0E-9 ? max : min;
		}
		setValue(v);
	}

	public String format() {
		if (toggle) {
			return bool() ? "ON" : "OFF";
		}
		if (labels != null) {
			int i = (int) Math.round(value);
			return labels[Math.max(0, Math.min(labels.length - 1, i))];
		}
		if (step >= 1) {
			return Long.toString(Math.round(value));
		}
		return String.format("%.2f", value);
	}
}
