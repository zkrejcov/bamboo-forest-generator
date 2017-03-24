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
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;

public class Main {

    // file path
    private static final String PATH = "./bamboo-forest.png";

    // canvas size in px
    private static final int SIZE_X = 1052;
    private static final int SIZE_Y = 744;

    // number of tree levels
    private static final int TREE_LEVELS = 6;

    // colors
    private static final Color PICTURE_BACKGROUND_COLOR = Color.WHITE;
    private static final Color FOREST_BACKGROUND_COLOR = Color.BLACK;
    private static final float BASE_HUE = 72f / 360;
    private static final float BASE_SATURATION = 0.73f;
    private static final float BASE_BRIGHTNESS = 0.78f;
    private static final float BRIGHTNESS_STEP = BASE_BRIGHTNESS / (TREE_LEVELS + 1);

    // bamboo
    private static final int BASE_TREE_WIDTH = SIZE_X / 20;
    private static int currentSegmentCount;

    private static final double[] SEGMENT = new double[6];
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static Graphics2D graphics;

    public static void main(String[] args) {
        BufferedImage result = new BufferedImage(SIZE_X, SIZE_Y, BufferedImage.TYPE_INT_RGB);
        graphics = result.createGraphics();
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
            int treeMaxCount = (SIZE_X / currentTreeWidth) / 4;
            int singleTreeSectorWidth = SIZE_X / treeMaxCount;

            Color treeFill = Color.getHSBColor(BASE_HUE, BASE_SATURATION, BASE_BRIGHTNESS - BRIGHTNESS_STEP * treeLevel);
            Color treeOutline = Color.getHSBColor(BASE_HUE, BASE_SATURATION, BASE_BRIGHTNESS - BRIGHTNESS_STEP * (treeLevel + 0.5f));
            Color leafFill = Color.getHSBColor(BASE_HUE, BASE_SATURATION, BASE_BRIGHTNESS - BRIGHTNESS_STEP * treeLevel - 0.05f);
            Color leafOutline = Color.getHSBColor(BASE_HUE, BASE_SATURATION, BASE_BRIGHTNESS - BRIGHTNESS_STEP * (treeLevel + 0.5f) - 0.05f);

            for (int treeNumber = 0; treeNumber < treeMaxCount; treeNumber++) {
                int xPosition = RANDOM.nextInt(singleTreeSectorWidth - currentTreeWidth) + singleTreeSectorWidth * treeNumber;
                int initY = -70 + RANDOM.nextInt(20);

                graphics.setStroke(new BasicStroke(2));
                GeneralPath currentTree = getTree(currentTreeWidth, xPosition, initY);
                drawShape(currentTree, treeFill, treeOutline);

                graphics.setStroke(new BasicStroke(1));
                int clusterCount = RANDOM.nextInt(3) + 1;
                int lowestViableSegment = getLowestViableTreeSegment(initY, getSegmentHeight(currentTreeWidth));
                for (int leafClusterNumber = 0; leafClusterNumber < clusterCount; leafClusterNumber++) {
                    GeneralPath leafCluster = getLeafCluster(currentTree, currentTreeWidth);
                    // move the cluster to a random point on the bottom of a random segment of the tree
                    int segmentIndex = getTreeSegment(lowestViableSegment, clusterCount, leafClusterNumber);
                    positionLeafCluster(currentTree, leafCluster, segmentIndex);
                    drawShape(leafCluster, leafFill, leafOutline);
                }
            }
        }

        // clear the picture border
        graphics.setColor(PICTURE_BACKGROUND_COLOR);
        graphics.setStroke(new BasicStroke(15));
        graphics.drawRect(7, 7, SIZE_X - 15, SIZE_Y - 15);

        // write it out
        result.flush();
        try {
            ImageIO.write(result, "png", new File(PATH));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void drawShape(GeneralPath shape, Color fillColor, Color outlineColor) {
        graphics.setColor(fillColor);
        graphics.fill(shape);
        graphics.setColor(outlineColor);
        graphics.draw(shape);
    }

    private static GeneralPath getTree(int width, int startX, int startY) {
        int segmentHeight = getSegmentHeight(width);
        currentSegmentCount = SIZE_Y / segmentHeight + 1;
        int currentSegmentTop = 0;
        GeneralPath tree = new GeneralPath();
        for (int i = 0; i < currentSegmentCount; i++) {
            tree.moveTo(0, currentSegmentTop);
            tree.lineTo(0 + width, currentSegmentTop);
            tree.lineTo(0 + width, currentSegmentTop + segmentHeight);
            tree.lineTo(0, currentSegmentTop + segmentHeight);
            tree.lineTo(0, currentSegmentTop);
            currentSegmentTop += segmentHeight + 3;
        }

        if (RANDOM.nextBoolean()) {
            tree.transform(AffineTransform.getRotateInstance(Math.toRadians(RANDOM.nextGaussian() * 3)));
        }
        tree.transform(AffineTransform.getTranslateInstance(startX, startY));

        return tree;
    }

    private static int getSegmentHeight(int treeWidth) {
        return treeWidth * 4;
    }

    private static int getLowestViableTreeSegment(int topY, int segmentHeight) {
        return (SIZE_Y + topY) / segmentHeight;
    }

    private static GeneralPath getLeafCluster(GeneralPath tree, int currentTreeWidth) {
        // each clster has from 1 to 3 leaves, arrayed around one center point and sharing the same half-plane/half-circle
        GeneralPath leafCluster = new GeneralPath();
        // get up to 3 leaves
        for (int i = 0; i < RANDOM.nextInt(3) + 1; i++) { // so that there is at least one
            // each leaf rotated randomly to spread them 0 - 180 degrees around the root
            GeneralPath leaf = getSingleLeaf(0, 0, RANDOM.nextInt(45) + 70 * i, currentTreeWidth);
            // add the leaf to the leaf cluster shape
            leafCluster.append(leaf, false);
        }
        // rotate the cluster randomly around its root
        leafCluster.transform(AffineTransform.getRotateInstance(Math.toRadians(RANDOM.nextInt(360))));

        return leafCluster;
    }

    private static GeneralPath getSingleLeaf(int startX, int startY, int rotationDegrees, int currentTreeWidth) {
        int rootToLeafDist = 2;
        double leafLength = currentTreeWidth * 1.5;
        double controlPointX = leafLength / 2;
        double leafHalfWidth = currentTreeWidth/4;

        GeneralPath leafPath = new GeneralPath();
        leafPath.moveTo(0, 0); // root point
        leafPath.moveTo(rootToLeafDist, 0); // leaf start
        leafPath.curveTo(controlPointX + rootToLeafDist, -leafHalfWidth, controlPointX + rootToLeafDist, -leafHalfWidth, leafLength + rootToLeafDist, 0);
        leafPath.curveTo(controlPointX + rootToLeafDist, leafHalfWidth, controlPointX + rootToLeafDist, leafHalfWidth, rootToLeafDist, 0);

        leafPath.transform(AffineTransform.getRotateInstance(Math.toRadians(rotationDegrees)));
        leafPath.transform(AffineTransform.getTranslateInstance(startX, startY));

        return leafPath;
    }

    private static int getTreeSegment(int lowestViableSegment, int clusterCount, int clusterIndex) {
        // 1-based, not using the last segment
        int maxSectionSegment = (int) Math.ceil(lowestViableSegment / clusterCount);
        int segmentIndex = RANDOM.nextInt(maxSectionSegment) + maxSectionSegment * clusterIndex;
        if (segmentIndex > lowestViableSegment) {
            segmentIndex = lowestViableSegment;
        }
        return segmentIndex;
    }

    private static void positionLeafCluster(GeneralPath tree, GeneralPath leafCluster, int segmentIndex) {
        // get the specified segment bottom ==> ~y
        int partsInSegment = 5;
        PathIterator pathIterator = tree.getPathIterator(null);
        for (int i = 0; i < segmentIndex * partsInSegment + 2; i++) { // skip all the unwanted bits
            pathIterator.next();
        }

        pathIterator.currentSegment(SEGMENT); // not interested in the type
        Point.Double lrc = new Point.Double(SEGMENT[0], SEGMENT[1]);
        pathIterator.next();
        pathIterator.currentSegment(SEGMENT); // not interested in the type
        Point.Double llc = new Point.Double(SEGMENT[0], SEGMENT[1]);

        // get a random spot on the bottom ==> ~x
        double distanceFromLeftEnd = RANDOM.nextInt(101) / 100d; // in percent/100
        double rootX = llc.x + (lrc.x - llc.x) * distanceFromLeftEnd;
        double rootY = llc.y + (lrc.y - llc.y) * distanceFromLeftEnd - 4;

        // get exact x and y and move the cluster
        leafCluster.transform(AffineTransform.getTranslateInstance(rootX, rootY));
    }
}