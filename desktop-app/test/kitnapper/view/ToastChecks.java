package kitnapper.view;

/**
 * Toast wrapping checks (M6 WP5, findings F2/F3): a checkout or return toast
 * keeps its due date and its fee instead of ellipsising them away.
 */
final class ToastChecks {

    /** One extra line box: {@link ToastView} grows by 18 px for its second line. */
    private static final int SECOND_LINE = 18;

    private ToastChecks() { }

    static void run() {
        ToastView single = new ToastView("Demo data reset.");
        UiProbeSupport.check("toast-single-line",
            single.getPreferredSize().height == Toasts.HEIGHT);

        ToastView checkout = new ToastView("Checked out Canon EOS R6 · due 2026-11-19");
        UiProbeSupport.check("toast-checkout-two-lines",
            checkout.getPreferredSize().height == Toasts.HEIGHT + SECOND_LINE);

        ToastView returned = new ToastView("Returned Rode Wireless PRO kit · fee \u20b1200.00");
        UiProbeSupport.check("toast-return-two-lines",
            returned.getPreferredSize().height == Toasts.HEIGHT + SECOND_LINE);

        ToastView huge = new ToastView("Imported " + "a-very-long-name ".repeat(12)
            + "into inventory.");
        UiProbeSupport.check("toast-capped-lines",
            huge.getPreferredSize().height == Toasts.HEIGHT + SECOND_LINE);

        UiProbeSupport.check("toast-width-unchanged",
            single.getPreferredSize().width == Toasts.WIDTH
                && checkout.getPreferredSize().width == Toasts.WIDTH);

        String reason = "Equipment returned late; first offence on this borrower's record";
        String wrapped = PreviewLayout.wrapText(reason, 248, 3);
        UiProbeSupport.check("reason-wraps", wrapped.startsWith("<html>")
            && wrapped.contains("<br>"));
        UiProbeSupport.check("reason-short-stays-plain",
            PreviewLayout.wrapText("First offence", 248, 3).equals("First offence"));
        UiProbeSupport.check("reason-escapes-markup",
            PreviewLayout.wrapText("a <b> & c ".repeat(20), 248, 3).contains("&lt;b&gt;"));
    }
}
