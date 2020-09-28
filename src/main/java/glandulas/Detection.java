package glandulas;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.ImageCalculator;

public class Detection {

	protected static ImagePlus processImageHue(String path) {

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
		// System.out.println(thresh);

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
		return imp2;
	}

	protected static ImagePlus processImageHueSaturation(String path) {

		ImagePlus imp = IJ.openImage(path);

		// Detection phase
		ImagePlus imp2 = imp.duplicate();
		IJ.run(imp, "HSB Stack", "");
		ImageStack stack = imp.getStack();

		// Hue processing
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

		// We try different algorithms searching for the best threshold
		IJ.setAutoThreshold(huePart, "Otsu Dark");
		double thresh = huePart.getProcessor().getMaxThreshold();

		IJ.setAutoThreshold(huePart, "Default Dark");
		if (huePart.getProcessor().getMaxThreshold() > thresh) {
			thresh = huePart.getProcessor().getMaxThreshold();
		}

		IJ.setAutoThreshold(huePart, "Triangle Dark");
		if (huePart.getProcessor().getMaxThreshold() > thresh) {
			thresh = huePart.getProcessor().getMaxThreshold();
		}

		huePart.close();
		hue.deleteRoi();

		IJ.setRawThreshold(hue, thresh, 255, null);
		IJ.run(hue, "Convert to Mask", "");
		IJ.run(hue, "Median...", "radius=10");
		IJ.run(hue, "Watershed", "");
		IJ.run(hue, "Median...", "radius=10");
		IJ.run(hue, "Watershed", "");

		// Saturation processing
		ImagePlus saturation = new ImagePlus(stack.getSliceLabel(2), stack.getProcessor(2));
		IJ.run(saturation, "Median...", "radius=10");

		saturation.setRoi(x1, y1, w, h);

		ImagePlus saturationPart = saturation.crop();

		IJ.setAutoThreshold(saturationPart, "Otsu");
		thresh = saturationPart.getProcessor().getMaxThreshold();

		IJ.run(saturationPart, "Convert to Mask", "");

		IJ.setAutoThreshold(saturationPart, "Default");
		if (saturationPart.getProcessor().getMaxThreshold() < thresh) {
			thresh = saturationPart.getProcessor().getMaxThreshold();
		}

		IJ.setAutoThreshold(saturationPart, "Triangle");
		if (saturationPart.getProcessor().getMaxThreshold() < thresh) {
			thresh = saturationPart.getProcessor().getMaxThreshold();
		}

		// System.out.println(thresh);

		saturationPart.close();
		saturation.deleteRoi();

		IJ.setRawThreshold(saturation, 0, thresh, null);
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
		saturation.changes = false;
		saturation.close();

		IJ.run(imp3, "Watershed", "");
		IJ.run(imp3, "Analyze Particles...", "size=500-Infinity exclude add");

		// hue.show();
		/*
		 * hue.show(); saturation.show(); imp2.show();
		 */

		imp3.changes = false;
		imp3.close();
		imp.changes = false;
		imp.close();
		//
		return imp2;
	}

	protected static ImagePlus processImageLAB(String path) {

		ImagePlus imp = IJ.openImage(path);

		// Detection phase
		ImagePlus imp2 = imp.duplicate();
		IJ.run(imp, "Lab Stack", "");
		ImageStack stack = imp.getStack();
		IJ.run("Stack to Images", "");
		// Hue processing
		ImagePlus impTemp = IJ.getImage();

		ImagePlus impL = WindowManager.getImage("L*");
		impL.close();

		ImagePlus impB1 = WindowManager.getImage("b*");
		ImagePlus impB = impB1.duplicate();
		impB1.close();

		ImagePlus impA1 = WindowManager.getImage("a*");
		ImagePlus impA = impA1.duplicate();
		impA1.close();
		IJ.run(impA, "Median...", "radius=10");

		int width = imp.getWidth();
		int heigth = imp.getHeight();
		int x1 = width / 4;
		int y1 = heigth / 2;
		int w = x1 * 2;
		int h = y1;

		impA.setRoi(x1, y1, w, h);

		ImagePlus impAPart = impA.crop();

		// We try different algorithms searching for the best threshold
		IJ.setAutoThreshold(impAPart, "Otsu Dark");
		double thresh = impAPart.getProcessor().getMaxThreshold();

		impAPart.close();
		impA.deleteRoi();

		IJ.setRawThreshold(impA, thresh, 255, null);
		IJ.run(impA, "Convert to Mask", "");
		IJ.run(impA, "Median...", "radius=10");
		IJ.run(impA, "Watershed", "");
		IJ.run(impA, "Median...", "radius=10");
		IJ.run(impA, "Watershed", "");

		// Saturation processing

		IJ.run(impB, "Median...", "radius=10");

		impB.setRoi(x1, y1, w, h);

		ImagePlus saturationPart = impB.crop();

		IJ.setAutoThreshold(saturationPart, "Otsu");
		thresh = saturationPart.getProcessor().getMaxThreshold();

		IJ.run(saturationPart, "Convert to Mask", "");

		// System.out.println(thresh);

		saturationPart.close();
		impB.deleteRoi();

		IJ.setRawThreshold(impB, 0, thresh, null);
		IJ.run(impB, "Convert to Mask", "");
		IJ.run(impB, "Median...", "radius=10");
		IJ.run(impB, "Watershed", "");
		IJ.run(impB, "Median...", "radius=10");
		IJ.run(impB, "Watershed", "");

		// Combination
		ImageCalculator ic = new ImageCalculator();
		ImagePlus imp3 = ic.run("OR create", impA, impB);
		impA.changes = false;
		impA.close();
		impB.changes = false;
		impB.close();

		IJ.run(imp3, "Watershed", "");
		IJ.run(imp3, "Analyze Particles...", "size=500-Infinity exclude add");

		// hue.show();
		/*
		 * hue.show(); saturation.show(); imp2.show();
		 */

		imp3.changes = false;
		imp3.close();
		imp.changes = false;
		imp.close();
		//
		return imp2;

	}

	protected static ImagePlus processImageLABSaturation(String path) {

		ImagePlus imp = IJ.openImage(path);

		// Detection phase
		ImagePlus imp2 = imp.duplicate();
		ImagePlus imp4 = imp.duplicate();
		IJ.run(imp, "Lab Stack", "");
		ImageStack stack = imp.getStack();
		IJ.run("Stack to Images", "");
		// Hue processing

		ImagePlus impL = WindowManager.getImage("L*");
		impL.close();

		ImagePlus impB = WindowManager.getImage("b*");
		impB.close();

		ImagePlus impA1 = WindowManager.getImage("a*");
		ImagePlus impA = impA1.duplicate();
		impA1.close();
		IJ.run(impA, "Median...", "radius=10");

		int width = imp.getWidth();
		int heigth = imp.getHeight();
		int x1 = width / 4;
		int y1 = heigth / 2;
		int w = x1 * 2;
		int h = y1;

		impA.setRoi(x1, y1, w, h);

		ImagePlus impAPart = impA.crop();

		// We try different algorithms searching for the best threshold
		IJ.setAutoThreshold(impAPart, "Otsu Dark");
		double thresh = impAPart.getProcessor().getMaxThreshold();

		impAPart.close();
		impA.deleteRoi();

		IJ.setRawThreshold(impA, thresh, 255, null);
		IJ.run(impA, "Convert to Mask", "");
		IJ.run(impA, "Median...", "radius=10");
		IJ.run(impA, "Watershed", "");
		IJ.run(impA, "Median...", "radius=10");
		IJ.run(impA, "Watershed", "");

		IJ.run(imp4, "HSB Stack", "");
		stack = imp4.getStack();
		// Saturation processing
		ImagePlus saturation = new ImagePlus(stack.getSliceLabel(2), stack.getProcessor(2));
		IJ.run(saturation, "Median...", "radius=10");

		saturation.setRoi(x1, y1, w, h);

		ImagePlus saturationPart = saturation.crop();

		IJ.setAutoThreshold(saturationPart, "Otsu");
		thresh = saturationPart.getProcessor().getMaxThreshold();

		IJ.run(saturationPart, "Convert to Mask", "");

		IJ.setAutoThreshold(saturationPart, "Default");
		if (saturationPart.getProcessor().getMaxThreshold() < thresh) {
			thresh = saturationPart.getProcessor().getMaxThreshold();
		}

		IJ.setAutoThreshold(saturationPart, "Triangle");
		if (saturationPart.getProcessor().getMaxThreshold() < thresh) {
			thresh = saturationPart.getProcessor().getMaxThreshold();
		}

		// System.out.println(thresh);

		saturationPart.close();
		saturation.deleteRoi();

		IJ.setRawThreshold(saturation, 0, thresh, null);
		IJ.run(saturation, "Convert to Mask", "");
		IJ.run(saturation, "Median...", "radius=10");
		IJ.run(saturation, "Watershed", "");
		IJ.run(saturation, "Median...", "radius=10");
		IJ.run(saturation, "Watershed", "");

		// Combination
		ImageCalculator ic = new ImageCalculator();
		ImagePlus imp3 = ic.run("OR create", impA, saturation);
		impA.changes = false;
		impA.close();
		saturation.changes = false;
		saturation.close();

		IJ.run(imp3, "Watershed", "");
		IJ.run(imp3, "Analyze Particles...", "size=500-Infinity exclude add");

		// hue.show();
		/*
		 * hue.show(); saturation.show(); imp2.show();
		 */

		imp3.changes = false;
		imp3.close();
		imp.changes = false;
		imp.close();
		imp4.changes = false;
		imp4.close();
		//
		return imp2;

	}

	protected static ImagePlus processImageSaturation(String path) {

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
		IJ.run(saturation, "Analyze Particles...", "size=300-Infinity exclude pixel add");
		saturation.close();
		imp.changes = false;
		imp.close();
		// imp2.show();
		return imp2;

	}

}
