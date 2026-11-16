package kitnapper.controller;

import java.awt.image.BufferedImage;
import java.util.Optional;

import kitnapper.service.AppContext;
import kitnapper.view.PhotoSource;

/**
 * Supplies item photos to the views (M5, design §12). The only class that
 * bridges {@link kitnapper.view.PhotoSource} to the service layer, so cards,
 * previews, and the inventory screen decode without importing services. Work
 * runs on the caller's background thread; {@code ImageStore.load} never throws.
 */
public final class ItemPhotos implements PhotoSource {

    private final AppContext context;

    public ItemPhotos(AppContext context) {
        this.context = context;
    }

    @Override
    public Optional<BufferedImage> load(String photoFile, int maxEdge) {
        return context.images().load(photoFile, maxEdge);
    }
}
