package com.github.zkrejcov.bfg;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_COLOR_RENDERING;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_COLOR_RENDER_QUALITY;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

public class Main {

    // number of tree levels
    private static final int TREE_LEVELS = 6;

    // colors
    private static final Color PICTURE_BACKGROUND_COLOR = Color.WHITE;
    private static final Color FOREST_BACKGROUND_COLOR = Color.BLACK;

    // file path
    private static String PATH = "./bamboo-forest.png";
    private static String FORMAT = "png";

    // canvas size in px
    private static int SIZE_X = 1052;
    private static int SIZE_Y = 744;

    // bamboo
    private static int BASE_TREE_WIDTH;

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    public static void main(String[] args) {
        // divine the output path and picture (and therefore tree) size
        PATH = System.getProperty("outputPath", PATH);
        FORMAT = PATH.replaceAll(".*\\.", "");
        try {
            SIZE_X = Integer.parseInt(System.getProperty("width"));
        } catch (NumberFormatException nfe) {
            System.out.println("nfe");}
        try {
            SIZE_Y = Integer.parseInt(System.getProperty("height"));
        } catch (NumberFormatException nfe) {}
        System.out.println(SIZE_X + "x" + SIZE_Y);
        BASE_TREE_WIDTH = SIZE_X / 20;

        BufferedImage result = new BufferedImage(SIZE_X, SIZE_Y, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
        graphics.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);

        // forest background and border
        graphics.setBackground(PICTURE_BACKGROUND_COLOR);
        graphics.clearRect(0, 0, SIZE_X, SIZE_Y);
        graphics.setColor(FOREST_BACKGROUND_COLOR);
        graphics.fillRect(30, 30, SIZE_X - 60, SIZE_Y - 60);

        // trees - back to front
        for (int treeLevel = TREE_LEVELS - 1; treeLevel >= 0; treeLevel--) {
            int currentTreeWidth = (int) (BASE_TREE_WIDTH * (1 - (float) treeLevel / 10));
            int treesInLevel = (SIZE_X / currentTreeWidth) / 4;
            int singleTreeSectorWidth = SIZE_X / treesInLevel;

            for (int treeNumber = 0; treeNumber < treesInLevel; treeNumber++) {
                int segmentHeight = getSegmentHeight(currentTreeWidth);
                int segmentCount = SIZE_Y / segmentHeight + 1;
                int xPosition = RANDOM.nextInt(singleTreeSectorWidth - currentTreeWidth) + singleTreeSectorWidth * treeNumber;
                int segmentHeight30Percent = (int) (segmentHeight/10f*3);
                int initY = -segmentHeight30Percent + RANDOM.nextInt(segmentHeight30Percent); // -30% to -60% ((45 +- 15)%) of segment height

                new BambooTree(xPosition, initY, currentTreeWidth, segmentCount, segmentHeight, treeLevel)
                        .drawShape(graphics);
            }
        }

        // clear the picture border
        graphics.setColor(PICTURE_BACKGROUND_COLOR);
        graphics.setStroke(new BasicStroke(15));
        graphics.drawRect(7, 7, SIZE_X - 15, SIZE_Y - 15);

        // write it out
        result.flush();
        try {
            ImageIO.write(result, FORMAT, new File(PATH));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static int getSegmentHeight(int treeWidth) {
        return treeWidth * 4;
    }
}