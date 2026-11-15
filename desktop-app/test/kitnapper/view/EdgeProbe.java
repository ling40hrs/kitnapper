package kitnapper.view;

import java.awt.Rectangle;

/**
 * Resize geometry probe (plan §6.4, §9.4): edge/corner resolution and the
 * min-size clamp in {@link ResizeEdge}. Package-private because ResizeEdge
 * is; runs headless. Exits non-zero on any failure.
 */
public final class EdgeProbe {

    private static int passed;
    private static int failed;

    private EdgeProbe() { }

    public static void main(String[] args) {
        check("corners",
            ResizeEdge.at(0, 0, 800, 600, 8) == ResizeEdge.NW
            && ResizeEdge.at(799, 0, 800, 600, 8) == ResizeEdge.NE
            && ResizeEdge.at(0, 599, 800, 600, 8) == ResizeEdge.SW
            && ResizeEdge.at(799, 599, 800, 600, 8) == ResizeEdge.SE);
        check("edges",
            ResizeEdge.at(400, 0, 800, 600, 8) == ResizeEdge.N
            && ResizeEdge.at(400, 599, 800, 600, 8) == ResizeEdge.S
            && ResizeEdge.at(0, 300, 800, 600, 8) == ResizeEdge.W
            && ResizeEdge.at(799, 300, 800, 600, 8) == ResizeEdge.E
            && ResizeEdge.at(400, 300, 800, 600, 8) == null);
        check("clamp-shrink",
            ResizeEdge.E.dragTo(new Rectangle(0, 0, 1280, 800), -400, 0, 1024, 640)
                .equals(new Rectangle(0, 0, 1024, 800))
            && ResizeEdge.S.dragTo(new Rectangle(0, 0, 1280, 800), 0, -300, 1024, 640)
                .equals(new Rectangle(0, 0, 1280, 640))
            && ResizeEdge.W.dragTo(new Rectangle(0, 0, 1280, 800), 400, 0, 1024, 640)
                .equals(new Rectangle(256, 0, 1024, 800))
            && ResizeEdge.N.dragTo(new Rectangle(0, 0, 1280, 800), 0, 300, 1024, 640)
                .equals(new Rectangle(0, 160, 1280, 640)));
        check("grow",
            ResizeEdge.E.dragTo(new Rectangle(0, 0, 1280, 800), 200, 0, 1024, 640)
                .equals(new Rectangle(0, 0, 1480, 800))
            && ResizeEdge.S.dragTo(new Rectangle(0, 0, 1280, 800), 0, 100, 1024, 640)
                .equals(new Rectangle(0, 0, 1280, 900))
            && ResizeEdge.W.dragTo(new Rectangle(0, 0, 1280, 800), -400, 0, 1024, 640)
                .equals(new Rectangle(-400, 0, 1680, 800))
            && ResizeEdge.N.dragTo(new Rectangle(0, 0, 1280, 800), 0, -300, 1024, 640)
                .equals(new Rectangle(0, -300, 1280, 1100)));

        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
