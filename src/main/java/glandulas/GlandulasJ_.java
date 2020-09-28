package glandulas;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.DirectoryChooser;
import ij.plugin.ImageCalculator;
import ij.plugin.frame.RoiManager;
import net.imagej.ImageJ;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>GlandulasJ>main")

public class GlandulasJ_ implements Command {

	public void run() {

		List<String> files = searchFiles();
		String dir = files.get(0);
		files.remove(0);
		JFrame frame = new JFrame("Work in progress");
		JProgressBar progressBar = new JProgressBar();
		int n = 0;
		progressBar.setValue(0);
		progressBar.setString("");
		progressBar.setStringPainted(true);
		progressBar.setMaximum(files.size());
		Border border = BorderFactory.createTitledBorder("Processing...");
		progressBar.setBorder(border);
		Container content = frame.getContentPane();
		content.add(progressBar, BorderLayout.NORTH);
		frame.setSize(300, 100);
		frame.setVisible(true);

		// For each file in the folder we detect the esferoid on it.
		for (String name : files) {
			processImageHueSaturation(name);
			n++;
			progressBar.setValue(n);
		}
		frame.setVisible(false);
		frame.dispose();
		IJ.showMessage("Process finished");

	}

	private static List<String> searchFiles() {

		List<String> result = new ArrayList<String>();

		// We ask the user for a directory with nd2 images.
		DirectoryChooser dc = new DirectoryChooser("Select the folder containing the jpg images");
		String dir = dc.getDirectory();

		// We store the list of tiff files in the result list.
		File folder = new File(dir);

		Utils.search(".*jpg", folder, result);

		Collections.sort(result);
		result.add(0, dir);
		return result;

	}

	private void processImageHue(String path) {

		ImagePlus imp = IJ.openImage(path);

		// Detection phase
		ImagePlus imp2 = imp.duplicate();
		IJ.run(imp, "HSB Stack", "");
		ImageStack stack = imp.getStack();
		ImagePlus hue = new ImagePlus(stack.getSliceLabel(1), stack.getProcessor(1));
		IJ.run(hue, "Median...", "radius=10");

		int width = imp.getWidth();
		int heigth = imp.getHeight();
		int x1 = width / 4;
		int y1 = heigth / 2;
		int w = x1 * 2;
		int h = y1;

		hue.setRoi(x1, y1, w, h);

		ImagePlus huePart = hue.crop();

		IJ.setAutoThreshold(huePart, "Otsu Dark");
		double thresh = huePart.getProcessor().getMaxThreshold();
		//System.out.println(thresh);

		huePart.close();
		hue.deleteRoi();

		IJ.setRawThreshold(hue, thresh, 255, null);
		IJ.run(hue, "Convert to Mask", "");
		IJ.run(hue, "Median...", "radius=10");
		IJ.run(hue, "Watershed", "");
		IJ.run(hue, "Median...", "radius=10");
		IJ.run(hue, "Watershed", "");

		IJ.run(hue, "Analyze Particles...", "size=500-Infinity exclude add");
		hue.changes = false;
		// hue.show();
		hue.close();
		imp.changes = false;
		imp.close();
		// imp2.show();
		processOutput(imp2, path);

	}
	
	
	
	private void processImageHueSaturation(String path) {

		ImagePlus imp = IJ.openImage(path);

		// Detection phase
		ImagePlus imp2 = imp.duplicate();
		IJ.run(imp, "HSB Stack", "");
		ImageStack stack = imp.getStack();
		
		//Hue processing
		ImagePlus hue = new ImagePlus(stack.getSliceLabel(1), stack.getProcessor(1));
		IJ.run(hue, "Median...", "radius=10");

		int width = imp.getWidth();
		int heigth = imp.getHeight();
		int x1 = width / 4;
		int y1 = heigth / 2;
		int w = x1 * 2;
		int h = y1;

		hue.setRoi(x1, y1, w, h);

		ImagePlus huePart = hue.crop();

		IJ.setAutoThreshold(huePart, "Otsu Dark");
		double thresh = huePart.getProcessor().getMaxThreshold();
		//System.out.println(thresh);

		huePart.close();
		hue.deleteRoi();

		IJ.setRawThreshold(hue, thresh, 255, null);
		IJ.run(hue, "Convert to Mask", "");
		IJ.run(hue, "Median...", "radius=10");
		IJ.run(hue, "Watershed", "");
		IJ.run(hue, "Median...", "radius=10");
		IJ.run(hue, "Watershed", "");
		
		
		//Saturation processing
		ImagePlus saturation = new ImagePlus(stack.getSliceLabel(2), stack.getProcessor(2));
		IJ.run(saturation, "Median...", "radius=10");

		saturation.setRoi(x1, y1, w, h);

		ImagePlus saturationPart = saturation.crop();

		IJ.setAutoThreshold(saturationPart, "Triangle");
		thresh = saturationPart.getProcessor().getMaxThreshold();
		System.out.println(thresh);

		saturationPart.close();
		saturation.deleteRoi();

		IJ.setRawThreshold(saturation, 0,125, null);
		IJ.run(saturation, "Convert to Mask", "");
		IJ.run(saturation, "Median...", "radius=10");
		IJ.run(saturation, "Watershed", "");
		IJ.run(saturation, "Median...", "radius=10");
		IJ.run(saturation, "Watershed", "");
		
		// Combination
		ImageCalculator ic = new ImageCalculator();
		ImagePlus imp3 = ic.run("OR create", hue, saturation);
		hue.changes = false;
		hue.close();
		saturation.changes=false;
		saturation.close();
		
		
		IJ.run(imp3, "Watershed", "");
		IJ.run(imp3, "Analyze Particles...", "size=500-Infinity exclude add");
		
		
		// hue.show();
		//hue.show();
		//saturation.show();
		imp3.changes=false;
		imp3.close();
		imp.changes = false;
		imp.close();
		// imp2.show();
		processOutput(imp2, path);

	}

	private void processImageSaturation(String path) {

		ImagePlus imp = IJ.openImage(path);

		// Detection phase
		ImagePlus imp2 = imp.duplicate();
		IJ.run(imp, "HSB Stack", "");
		ImageStack stack = imp.getStack();
		ImagePlus saturation = new ImagePlus(stack.getSliceLabel(2), stack.getProcessor(2));
		IJ.run(saturation, "Median...", "radius=10");
		IJ.setAutoThreshold(saturation, "Default");
		IJ.setRawThreshold(saturation, 0, 135, null);
		IJ.run(saturation, "Convert to Mask", "");
		IJ.run(saturation, "Watershed", "");
		IJ.run(saturation, "Analyze Particles...", "size=500-Infinity exclude add");
		saturation.close();
		imp.changes = false;
		imp.close();
		imp2.show();
		processOutput(imp2, path);

	}

	private void processOutput(ImagePlus imp2, String path) {
		// Processing the output
		RoiManager rm = RoiManager.getInstance();
		if (rm != null) {
			rm.setVisible(false);
		}

		int rois = rm.getRoisAsArray().length;
		for (int i = 0; i < rois; i++) {

			imp2.setRoi(rm.getRoi(0));
			//if (imp2.getProcessor().getHistogram()[255] == 0) {
				// rm.select(0);
				IJ.run(imp2, "Fit Circle", "");
				rm.addRoi(imp2.getRoi());
			//}
			rm.select(0);
			rm.runCommand(imp2, "Delete");
		}

		rm.runCommand(imp2, "Show All without labels");
		rm.runCommand(imp2, "Draw");
		rm.runCommand(imp2, "Delete");

		String name = path.substring(0, path.lastIndexOf("."));
		IJ.saveAs(imp2, "JPG", name + "_pred.jpg");
		imp2.close();

	}

}
