package ij.gui;

import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author omar
 */
public class ControlerPhotometerParams implements ActionListener {
    private static ControlerPhotometerParams instance = null;
    private ImagePlus imp;
    private OvalRoi in;

    public ControlerPhotometerParams() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        try {

            if ("bPlus".equals(e.getActionCommand())) {
                PhotometerParams.getInstance().setstatf(PhotometerParams.getInstance().getstatf() + 1);
            } else if ("bMoin".equals(e.getActionCommand())) {
                if (PhotometerParams.getInstance().getstatf() > 1) {
                    PhotometerParams.getInstance().setstatf(PhotometerParams.getInstance().getstatf() - 1);
                }
            }


            int t[] = PhotometerParams.getInstance().getCoord();
            imp = PhotometerParams.getInstance().getIMP();

            in = new OvalRoi(t[0] - PhotometerParams.getInstance().getstatf(), t[1] - PhotometerParams.getInstance().getstatf(), 2 * PhotometerParams.getInstance().getstatf(), 2 * PhotometerParams.getInstance().getstatf());
            imp.setRoi(in);

            instance = this;

        } catch (NumberFormatException ignored) {
        }
    }

}
