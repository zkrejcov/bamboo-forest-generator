package com.github.zkrejcov.bfg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BambooTree {

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    // number of tree levels
    private static final int TREE_LEVELS = 6;

    private static final float BASE_HUE = 72f / 360;
    private static final float BASE_SATURATION = 0.73f;
    private static final float BASE_BRIGHTNESS = 0.78f;
    private static final float BRIGHTNESS_STEP = BASE_BRIGHTNESS / (TREE_LEVELS + 1);

    private static final double[] SEGMENT = new double[6];

    private final GeneralPath trunk = new GeneralPath();
    private final List<GeneralPath> branches = new ArrayList<>();

    private final int segmentHeight;

    private Color trunkFill;
    private Color trunkOutline;
    private Color branchFill;
    private Color branchOutline;

    public BambooTree(int startX, int startY, int width, int segmentCount, int segmentHeight, int treeLevel) {
        this.segmentHeight = segmentHeight;
        setColors(treeLevel);
        createTreeTrunk(width, segmentCount);
        positionTree(startX, startY);
        createBranches(width, segmentCount);
    }

    private void setColors(int treeLevel) {
        trunkFill = Color.getHSBColor(BASE_HUE, BASE_SATURATION, BASE_BRIGHTNESS - BRIGHTNESS_STEP * treeLevel);
        trunkOutline = Color.getHSBColor(BASE_HUE, BASE_SATURATION, BASE_BRIGHTNESS - BRIGHTNESS_STEP * (treeLevel + 0.5f));
        branchFill = Color.getHSBColor(BASE_HUE, BASE_SATURATION, BASE_BRIGHTNESS - BRIGHTNESS_STEP * treeLevel - 0.05f);
        branchOutline = Color.getHSBColor(BASE_HUE, BASE_SATURATION, BASE_BRIGHTNESS - BRIGHTNESS_STEP * (treeLevel + 0.5f) - 0.05f);
    }

    private void createTreeTrunk(int width, int segmentCount) {
        int currentSegmentTop = 0;
        for (int i = 0; i < segmentCount; i++) {
            trunk.moveTo(0, currentSegmentTop);
            trunk.lineTo(0 + width, currentSegmentTop);
            trunk.lineTo(0 + width, currentSegmentTop + segmentHeight);
            trunk.lineTo(0, currentSegmentTop + segmentHeight);
            trunk.lineTo(0, currentSegmentTop);
            currentSegmentTop += segmentHeight + 3;
        }
    }

    private void createBranches(int treeWidth, int treeSegmentCount) {
        int branchCount = RANDOM.nextInt(2) + 2; // 2 or 3
        int lowestViableSegment = treeSegmentCount - 1;
        for (int branchNumber = 0; branchNumber < branchCount; branchNumber++) {
            int segmentIndex = get1BasedTreeSegmentIndex(lowestViableSegment, branchCount, branchNumber);
            GeneralPath branch = getBranch(treeWidth);
            positionBranch(branch, segmentIndex);
            branches.add(branch);
        }
    }

    private GeneralPath getBranch(int treeWidth) {
        // each branch has from 1 to 3 leaves, arrayed around one center point and sharing the same half-plane/half-circle
        GeneralPath branch = new GeneralPath();
        // get 2 or 3 leaves
        for (int i = 0; i < RANDOM.nextInt(2) + 2; i++) {
            // each leaf rotated randomly to spread them ~0 - 180 degrees around the root
            GeneralPath leaf = getSingleLeaf(RANDOM.nextInt(45) + 70 * i, treeWidth);
            // add the leaf to the leaf cluster shape
            branch.append(leaf, false);
        }
        // rotate the branch randomly around its root
        branch.transform(AffineTransform.getRotateInstance(Math.toRadians(RANDOM.nextInt(360))));

        return branch;
    }

    private GeneralPath getSingleLeaf(int rotationDegrees, int treeWidth) {
        int rootToLeafDist = 2;
        double leafLength = treeWidth * 1.5;
        double controlPointX = leafLength / 2;
        double leafHalfWidth = treeWidth/4;

        GeneralPath leafPath = new GeneralPath();
        leafPath.moveTo(0, 0); // root point
        leafPath.moveTo(rootToLeafDist, 0); // leaf start
        leafPath.curveTo(controlPointX + rootToLeafDist, -leafHalfWidth, controlPointX + rootToLeafDist, -leafHalfWidth, leafLength + rootToLeafDist, 0);
        leafPath.curveTo(controlPointX + rootToLeafDist, leafHalfWidth, controlPointX + rootToLeafDist, leafHalfWidth, rootToLeafDist, 0);

        leafPath.transform(AffineTransform.getRotateInstance(Math.toRadians(rotationDegrees)));

        return leafPath;
    }

    private int get1BasedTreeSegmentIndex(int lowestViableSegment, int branchCount, int branchNumber) {
        // 1-based, not using the last segment
        int maxSectionSegment = (int) Math.ceil(lowestViableSegment / branchCount);
        int segmentIndex = RANDOM.nextInt(maxSectionSegment) + maxSectionSegment * branchNumber;
        if (segmentIndex > lowestViableSegment) {
            segmentIndex = lowestViableSegment;
        }
        return segmentIndex;
    }

    private void positionBranch(GeneralPath branch, int segmentIndex) {
        // get the specified segment bottom
        PathIterator pathIterator = trunk.getPathIterator(null);
        for (int i = 0; i < segmentIndex * 5 + 2; i++) { // skip all the unwanted bits, 5 elements in a segment
            pathIterator.next();
        }

        Point.Double lrc = extractPoint(pathIterator);
        pathIterator.next();
        Point.Double llc = extractPoint(pathIterator);

        // get a random spot on the bottom - but not too close to the edges
        double distanceFromLeftEnd = (RANDOM.nextInt(81) + 10) / 100d; // in percent/100
        double rootX = llc.x + (lrc.x - llc.x) * distanceFromLeftEnd;
        double rootY = llc.y + (lrc.y - llc.y) * distanceFromLeftEnd - (segmentHeight * 0.03); // 3% tree segment height above the bottom

        // move the cluster
        branch.transform(AffineTransform.getTranslateInstance(rootX, rootY));
    }

    private Point.Double extractPoint(PathIterator pathIterator) {
        pathIterator.currentSegment(SEGMENT); // not interested in the type
        return new Point.Double(SEGMENT[0], SEGMENT[1]);
    }

    private void positionTree(int startX, int startY) {
        if (RANDOM.nextBoolean()) {
            trunk.transform(AffineTransform.getRotateInstance(Math.toRadians(RANDOM.nextGaussian() * 3)));
        }
        trunk.transform(AffineTransform.getTranslateInstance(startX, startY));
    }

    public void drawShape(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(2));
        graphics.setColor(trunkFill);
        graphics.fill(trunk);
        graphics.setColor(trunkOutline);
        graphics.draw(trunk);

        graphics.setStroke(new BasicStroke(1));
        for (GeneralPath branch : branches) {
            graphics.setColor(branchFill);
            graphics.fill(branch);
            graphics.setColor(branchOutline);
            graphics.draw(branch);
        }
    }
}
