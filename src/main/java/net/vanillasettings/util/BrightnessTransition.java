package net.vanillasettings.util;

/**
 * A linear "fade" of the lightmap gamma multiplier (1 = vanilla ... 15 = max).
 * <p>
 * The important property is that a new fade always starts from wherever the
 * value <i>currently is</i>, and always moves at the same constant speed
 * ({@code unitsPerSecond}). That gives the behaviour you would expect when the
 * mod is toggled while a fade is still running:
 * <ul>
 *   <li>ON at 3/15 with a 3 s transition: after 2 s the value is 1 + 2*(2/3) = 2.33</li>
 *   <li>OFF at that moment: the way back down takes exactly 2 s, not another 3 s,
 *       and not an instant jump.</li>
 * </ul>
 * This class deliberately has no Minecraft dependencies and takes the current
 * time as a parameter, so its behaviour can be tested in isolation.
 */
public final class BrightnessTransition {

    /** Multiplier value that means "vanilla, no change". */
    public static final double VANILLA = 1.0;

    private double from = VANILLA;
    private double to = VANILLA;
    private long startNanos = 0L;
    private long durationNanos = 0L;

    /** The value of the fade at the given moment. */
    public double valueAt(long nowNanos) {
        if (durationNanos <= 0L) {
            return to;
        }
        long elapsed = nowNanos - startNanos;
        if (elapsed >= durationNanos) {
            return to;
        }
        if (elapsed <= 0L) {
            return from;
        }
        double progress = (double) elapsed / (double) durationNanos;
        return from + (to - from) * progress;
    }

    /**
     * Starts a fade from the current value towards {@code target}, moving at
     * {@code unitsPerSecond} multiplier-levels per second.
     * <p>
     * If the speed is not positive the value snaps straight to the target.
     */
    public void startTowards(double target, double unitsPerSecond, long nowNanos) {
        double current = valueAt(nowNanos);
        double distance = Math.abs(target - current);

        if (distance < 1.0e-9 || !(unitsPerSecond > 0.0)) {
            snapTo(target);
            return;
        }

        this.from = current;
        this.to = target;
        this.startNanos = nowNanos;
        this.durationNanos = Math.max(1L, Math.round(distance / unitsPerSecond * 1.0e9));
    }

    /**
     * Fades towards {@code target} at the speed implied by the user's settings:
     * a full swing between vanilla (1) and {@code strength} takes exactly
     * {@code seconds}. Because the speed is constant, a fade that is reversed
     * half way simply takes half as long.
     *
     * @param target   level to fade to (1 = vanilla, otherwise the configured strength)
     * @param strength the configured crystal brightness (1-15)
     * @param seconds  the configured transition time for a full swing
     */
    public void fadeTo(double target, double strength, double seconds, long nowNanos) {
        double current = valueAt(nowNanos);
        // Normally this is just "strength". max() only matters if the strength
        // setting was lowered while the mod was on, so the fade never gets slower
        // than one full swing per configured transition time.
        double fullSwing = Math.max(Math.max(strength, target), current) - VANILLA;
        double safeSeconds = Math.max(0.05, seconds);
        startTowards(target, fullSwing / safeSeconds, nowNanos);
    }

    /** Jumps to {@code value} immediately and cancels any running fade. */
    public void snapTo(double value) {
        this.from = value;
        this.to = value;
        this.durationNanos = 0L;
    }

    /** Where the current fade is heading. */
    public double target() {
        return to;
    }

    /** True while a fade is still in progress at the given moment. */
    public boolean isRunning(long nowNanos) {
        return durationNanos > 0L && (nowNanos - startNanos) < durationNanos;
    }
}
