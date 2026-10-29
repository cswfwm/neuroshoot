package com.neuroshoot.world;

/** 格子类型。墙与柱子挡视线也挡子弹；门可以被打开或被霰弹/爆炸破坏。 */
public final class Tile {
    public static final byte FLOOR = 0;
    public static final byte GRASS = 1;
    public static final byte WALL = 2;
    public static final byte DOOR = 3;
    public static final byte PILLAR = 4;
    public static final byte STAIR = 5;
    /** 建筑外轮廓：不可通行的矮墙，能挡视线但不挡子弹（可越过射击）。 */
    public static final byte LOW_WALL = 6;

    private Tile() {}

    public static boolean solid(byte t) {
        return t == WALL || t == DOOR || t == PILLAR;
    }

    /** 是否阻挡视线。LOW_WALL 与 PILLAR 都挡视线，这是「柱子后看不见」的基础。 */
    public static boolean blocksSight(byte t) {
        return t == WALL || t == DOOR || t == PILLAR;
    }
}
