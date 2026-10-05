package com.xiaofeiwu.grandwitch;

/** The two brooms: how fast each goes (blocks a tick), how high above the ground it hovers and at most climbs, and how long it lasts (seconds of flight). */
public enum BroomKind {
    WOOD(0.28D, 1.0D, 3.0D, 300),
    GOLD(0.50D, 1.5D, 6.0D, 900);

    final double speed;
    final double hover;
    final double maxAltitude;
    final int durability;

    BroomKind(double speed, double hover, double maxAltitude, int durability) {
        this.speed = speed;
        this.hover = hover;
        this.maxAltitude = maxAltitude;
        this.durability = durability;
    }
}
