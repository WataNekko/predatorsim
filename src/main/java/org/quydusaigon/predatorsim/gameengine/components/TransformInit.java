package org.quydusaigon.predatorsim.gameengine.components;

public class TransformInit {
    public final double posX;
    public final double posY;
    public final double rotate;
    public final double scaleX;
    public final double scaleY;

    public static final TransformInit ZERO = new TransformInit(0,0,0,0,0);

    public TransformInit(double posX, double posY, double rotate, double scaleX, double scaleY) {
        this.posX = posX;
        this.posY = posY;
        this.rotate = rotate;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public TransformInit(double posX, double posY, double rotate) {
        this.posX = posX;
        this.posY = posY;
        this.rotate = rotate;
        scaleX = 0;
        scaleY = 0;
    }

    public TransformInit(double posX, double posY) {
        this.posX = posX;
        this.posY = posY;
        rotate = 0;
        scaleX = 0;
        scaleY = 0;
    }
}
