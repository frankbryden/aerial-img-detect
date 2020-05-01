package uk.ac.soton.ecs.fjkb1u17;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

        import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.analysis.algorithm.HoughLines;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.math.geometry.line.Line2d;
import org.openimaj.math.geometry.line.Line2d.IntersectionResult;
import org.openimaj.math.geometry.line.Line2d.IntersectionType;
import org.openimaj.math.geometry.point.Point2d;

        /**
 050 * Example showing how to find the corners of a chessboard pattern using a hough
 051 * line detector.
 052 *
 053 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 054 */
        public class HoughLineChessboard {
        /**
 057         * Main method
 058         *
 059         * @param args
 060         *            ignored
 061         * @throws IOException
 062         *             if image can't be loaded
 063         */
                public static void main(String[] args) throws IOException {
                        final FImage chessboard = ImageUtilities.readF(new File("binaryRoadsUS.png"));

                        final HoughLines hlines = new HoughLines(1.f);
                        chessboard.process(new CannyEdgeDetector()).analyseWith(hlines);

                        final List<Line2d> lines = hlines.getBestLines(50);
        final List<Point2d> intersections = new ArrayList<Point2d>();
                        for (final Line2d inner : lines) {
                                    for (final Line2d outer : lines) {
                                                if (inner == outer)
                                                            continue;

                                                final IntersectionResult intersect = inner.getIntersection(outer);
                                                if (intersect.type == IntersectionType.INTERSECTING) {
                    intersections.add(intersect.intersectionPoint);
                                                   }
                                       }
                      }
                       // draw result
                       final MBFImage chessboardC = chessboard.toRGB();
                       chessboardC.drawLines(lines, 1, RGBColour.RED);
                       chessboardC.drawPoints(intersections, RGBColour.GREEN, 3);

                       DisplayUtilities.display(chessboardC);
                }
}
