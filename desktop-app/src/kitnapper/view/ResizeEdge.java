package kitnapper.view;

import java.awt.Cursor;
import java.awt.Rectangle;

/**
 * Resize edge/corner geometry for the custom chrome (design.md §11): maps a
 * point in the layered pane to the edge under it and computes the bounds a
 * drag produces, clamped to the frame minimum. State and components live in
 * {@link ResizeBands}.
 */
enum ResizeEdge {

    N(false, false, true, false, Cursor.N_RESIZE_CURSOR),
    S(false, false, false, true, Cursor.S_RESIZE_CURSOR),
    W(true, false, false, false, Cursor.W_RESIZE_CURSOR),
    E(false, true, false, false, Cursor.E_RESIZE_CURSOR),
    NW(true, false, true, false, Cursor.NW_RESIZE_CURSOR),
    NE(false, true, true, false, Cursor.NE_RESIZE_CURSOR),
    SW(true, false, false, true, Cursor.SW_RESIZE_CURSOR),
    SE(false, true, false, true, Cursor.SE_RESIZE_CURSOR);

    final boolean west;
    final boolean east;
    final boolean north;
    final boolean south;
    final Cursor cursor;

    ResizeEdge(boolean west, boolean east, boolean north, boolean south, int cursorType) {
        this.west = west;
        this.east = east;
        this.north = north;
        this.south = south;
        this.cursor = Cursor.getPredefinedCursor(cursorType);
    }

    /**
     * Resolves the edge under a point in the layered pane's coordinate space,
     * or null when the point is not inside a band.
     */
    static ResizeEdge at(int x, int y, int width, int height, int band) {
        boolean n = y < band;
        boolean s = y >= height - band;
        boolean w = x < band;
        boolean e = x >= width - band;
        if (n && w) {
            return NW;
        }
        if (n && e) {
            return NE;
        }
        if (s && w) {
            return SW;
        }
        if (s && e) {
            return SE;
        }
        if (n) {
            return N;
        }
        if (s) {
            return S;
        }
        if (w) {
            return W;
        }
        if (e) {
            return E;
        }
        return null;
    }

    /** Applies a drag delta to a starting bound, never shrinking below the minimum. */
    Rectangle dragTo(Rectangle start, int dx, int dy, int minWidth, int minHeight) {
        int x = start.x;
        int y = start.y;
        int w = start.width;
        int h = start.height;
        if (west) {
            int shift = Math.min(dx, start.width - minWidth);
            x = start.x + shift;
            w = start.width - shift;
        }
        if (east) {
            w = Math.max(minWidth, start.width + dx);
        }
        if (north) {
            int shift = Math.min(dy, start.height - minHeight);
            y = start.y + shift;
            h = start.height - shift;
        }
        if (south) {
            h = Math.max(minHeight, start.height + dy);
        }
        return new Rectangle(x, y, w, h);
    }
}
