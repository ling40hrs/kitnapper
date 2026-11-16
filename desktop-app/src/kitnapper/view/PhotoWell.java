package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.Path;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import kitnapper.model.EquipmentCategory;

/**
 * The equipment dialog's photo well (design §12): the current or staged photo
 * at the preview size, a chooser and a remove button, and one status line.
 * A busy import shows the media skeleton with both buttons disabled, per
 * design §13.1 ("photo import"), and the dialog owns the actual staging — the
 * well only forwards the chosen file.
 */
final class PhotoWell extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 432;

    /** What the dialog does with the well's two user actions. */
    interface Listener {
        void photoChosen(Path source);

        void photoRemoved();
    }

    private final PhotoSource photos;
    private final Listener listener;
    private final JPanel media = new JPanel(new BorderLayout());
    private final JButton choose = UiFactory.secondaryButton("Choose photo…");
    private final JButton remove = UiFactory.secondaryButton("Remove photo");
    private final JLabel status = UiFactory.label("", "secondary");
    private boolean removeAllowed;

    PhotoWell(PhotoSource photos, Listener listener) {
        this.photos = photos == null ? PhotoSource.NONE : photos;
        this.listener = listener;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        media.setOpaque(false);
        media.setAlignmentX(0f);
        media.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.round(WIDTH * 9f / 16f)));
        add(media);
        add(Box.createVerticalStrut(8));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(0f);
        buttons.add(choose);
        buttons.add(remove);
        add(buttons);
        add(Box.createVerticalStrut(4));
        status.setAlignmentX(0f);
        status.setVisible(false);
        add(status);
        choose.addActionListener(e -> chooseFile());
        remove.addActionListener(e -> listener.photoRemoved());
        setRemoveEnabled(false);
    }

    /** Paints the photo (or the category fallback) that saving would keep. */
    void showFile(String photoFile, EquipmentCategory category, String label) {
        EquipmentCategory safe = category == null ? EquipmentCategory.OTHER : category;
        media.removeAll();
        media.add(new PhotoTile(photoFile, safe, label, WIDTH, photos,
            PhotoTile.PREVIEW_EDGE_PX), BorderLayout.CENTER);
        media.revalidate();
        media.repaint();
    }

    /** Busy import: skeleton over the media block, both buttons disabled. */
    void setBusy(boolean busy) {
        choose.setEnabled(!busy);
        remove.setEnabled(!busy && removeAllowed);
        if (busy) {
            media.removeAll();
            media.add(new SkeletonPanel(SkeletonPanel.Variant.MEDIA), BorderLayout.CENTER);
            media.revalidate();
            media.repaint();
        }
    }

    void setRemoveEnabled(boolean enabled) {
        removeAllowed = enabled;
        remove.setEnabled(enabled);
    }

    /** One line under the buttons; {@code error} switches it to DANGER. */
    void setStatus(String text, boolean error) {
        boolean visible = text != null && !text.isBlank();
        status.setText(visible ? text : "");
        status.putClientProperty(UiFactory.ROLE, error ? "error" : "secondary");
        status.setForeground(error ? Theme.danger : Theme.textSecondary);
        status.setVisible(visible);
        revalidate();
        repaint();
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a photo");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Images (png, jpg, jpeg, gif, bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null) {
                listener.photoChosen(file.toPath());
            }
        }
    }
}
