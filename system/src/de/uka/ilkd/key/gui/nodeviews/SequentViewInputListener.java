package de.uka.ilkd.key.gui.nodeviews;

import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.Sequent;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.pp.PosInSequent;
import de.uka.ilkd.key.proof.io.ProofSaver;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * This class implements all input listener interfaces for SequentView.
 *
 * @author Kai Wallisch <kai.wallisch@ira.uka.de>
 */
public class SequentViewInputListener implements KeyListener, MouseMotionListener, MouseListener {

    private final SequentView sequentView;
    private boolean showTermInfo = false;

    protected void showTermInfo(Point p) {
        if (showTermInfo) {
            PosInSequent mousePos = sequentView.getPosInSequent(p);
            String info = null;

            if ((mousePos != null)
                    && !("".equals(sequentView.getHighlightedText(mousePos)))) {

                Term t;
                final PosInOccurrence posInOcc = mousePos.getPosInOccurrence();
                if (posInOcc != null) {
                    t = posInOcc.subTerm();
                    String tOpClassString = t.op().getClass().toString();
                    String operator = tOpClassString.substring(
                            tOpClassString.lastIndexOf('.') + 1);
                    // The hash code is displayed here since sometimes terms with
                    // equal string representation are still different.
                    info = operator + ", Sort: " + t.sort() + ", Hash:" + t.hashCode();

                    Sequent seq = sequentView.getMainWindow().getMediator().getSelectedNode().sequent();
                    info += ProofSaver.posInOccurrence2Proof(seq, posInOcc);
                }
            }

            if (info == null) {
                sequentView.getMainWindow().setStandardStatusLine();
            } else {
                sequentView.getMainWindow().setStatusLine(info);
            }
        }
    }

    SequentViewInputListener(SequentView sequentView) {
        this.sequentView = sequentView;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // This method is required by KeyListener interface.
    }

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    @Override
    public void keyPressed(KeyEvent e) {
        if ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) {
            showTermInfo = true;
            showTermInfo(sequentView.getMousePosition());
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    @Override
    public void keyReleased(KeyEvent e) {
        if ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == 0 && showTermInfo) {
            showTermInfo = false;
            sequentView.getMainWindow().setStandardStatusLine();
        }
    }

    @Override
    public void mouseDragged(MouseEvent me) {
        // This method is required by MouseMotionListener interface.
    }

    @Override
    public void mouseMoved(MouseEvent me) {
        showTermInfo(me.getPoint());
        if (sequentView.refreshHighlightning) {
            sequentView.highlight(me.getPoint());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // This method is required by MouseListener interface.
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // This method is required by MouseListener interface.
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // This method is required by MouseListener interface.
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // This method is required by MouseListener interface.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (sequentView.refreshHighlightning) {
            sequentView.disableHighlights();
        }
    }

}
