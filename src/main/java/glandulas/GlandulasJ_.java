package glandulas;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Polygon;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.frame.RoiManager;
import net.imagej.ImageJ;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>GlandulasJ")

public class GlandulasJ_ implements Command {

	@Parameter(label = "Black cells")
	private boolean black = true;

	private HashMap<String, double[]> results = new HashMap<String, double[]>();

	public void run() {

		// Setting the scale
		ImagePlus imp = IJ.createImage("Untitled", "8-bit white", 1, 1, 1);
		IJ.run(imp, "Set Scale...", "");
		imp.close();

		List<String> files = searchFiles();
		String dir = files.get(0);
		// Create save directory
		new File(dir + "/preds").mkdir();

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
			if (!black) {
				imp = Detection.processImageHueSaturation(name);
			} else {
				imp = Detection.processImageSaturation2(name);
			}
			// double area = computeLeafArea(name);
			processOutput(imp, dir, name);//
			n++;
			progressBar.setValue(n);
		}
		frame.setVisible(false);
		frame.dispose();
		SaveExcel(dir);
		RoiManager rm = RoiManager.getRoiManager();
		rm.setVisible(true);
		rm.close();
		IJ.showMessage("Process finished");

	}

	private double computeLeafArea(ImagePlus imp) {
		IJ.run(imp, "8-bit", "");
		IJ.setAutoThreshold(imp, "Default dark");
		IJ.setRawThreshold(imp, 0, 245, null);
		IJ.run(imp, "Convert to Mask", "");
		IJ.run(imp, "Analyze Particles...", "size=20000-Infinity pixel add");
		RoiManager rm = RoiManager.getInstance();
		rm.setVisible(false);
		double area = Utils.getArea(rm.getRoi(0).getPolygon());
		for (int i = 0; i < rm.getRoisAsArray().length; i++) {
			rm.select(0);
			rm.runCommand(imp, "Delete");
		}
		imp.close();
		return area;

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

	private void processOutput(ImagePlus imp2, String dir, String path) {
		String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));

		// ImagePlus imp = imp2.duplicate();
		// Processing the output
		RoiManager rm = RoiManager.getInstance();
		if (rm != null) {
			rm.setVisible(false);

			//double imageMean = Utils.mean(imp2.getProcessor().getHistogram());
			//System.out.println(imageMean);
			//System.out.println("----------------------");
			int rois = rm.getRoisAsArray().length;
			double roiMean;
			for (int i = 0; i < rois; i++) {

				imp2.setRoi(rm.getRoi(0));
				roiMean = Utils.mean(imp2.getProcessor().getHistogram());
				//System.out.println(roiMean);
				if (roiMean < 200) {
					//rm.select(0);
					IJ.run(imp2, "Fit Circle", "");
					rm.addRoi(imp2.getRoi());
				}
				rm.select(0);
				rm.runCommand(imp2, "Delete");
			}

			ResultsTable rt = ResultsTable.getResultsTable();

			int cells = rt.getCounter();
			DescriptiveStatistics stats = new DescriptiveStatistics();
			for (int i = 0; i < cells; i++) {
				// stats.addValue(Utils.getArea(rm.getRoi(i).getPolygon()));
				stats.addValue(rt.getValue("Area", i));
			}

			convertToExcel(dir + "/preds/" + name + "_res.xls", rt);
			rt.reset();

			rm.runCommand(imp2, "Show All without labels");
			rm.runCommand(imp2, "Draw");
			rm.runCommand("Save", dir + "/preds/" + name + ".zip");
			rm.runCommand(imp2, "Delete");
			IJ.saveAs(imp2, "JPG", dir + "/preds/" + name + "_pred.jpg");

			double[] value = { cells, stats.getMean(), stats.getStandardDeviation(), stats.getMax(), stats.getMin(),
					stats.getSum() };
			results.put(name, value);
		}
		// imp2.close();

	}

	private void SaveExcel(String dir) {
		String filename = dir + "results.xls";

		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("Results");
//		HSSFCellStyle style = workbook.createCellStyle();
//		style.setFillBackgroundColor(IndexedColors.ROYAL_BLUE.getIndex());
//		CellStyle style2 = workbook.createCellStyle();
//		style.setFillBackgroundColor(IndexedColors.LIGHT_BLUE.getIndex()); 

		HSSFRow rowhead = sheet.createRow((short) 0);

		String[] headings = { "File", "# Cells", "Mean area", "Std", "Max area", "Min area", "Covered area" };
		for (int i = 0; i < headings.length; i++) {
			rowhead.createCell((short) i).setCellValue(headings[i]);
		}

		Set<String> keys = results.keySet();

		HSSFRow row;
		int i = 0;
		for (String key : keys) {
			row = sheet.createRow((short) i + 1);

			double[] rowi = results.get(key);
			row.createCell((short) 0).setCellValue(key);
			for (int j = 0; j < headings.length - 1; j++) {
				row.createCell((short) j + 1).setCellValue(rowi[j]);
			}
			i++;
		}

		FileOutputStream fileOut;
		try {
			fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void convertToExcel(String filename, ResultsTable rt) {

		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("Results");
//		HSSFCellStyle style = workbook.createCellStyle();
//		style.setFillBackgroundColor(IndexedColors.ROYAL_BLUE.getIndex());
//		CellStyle style2 = workbook.createCellStyle();
//		style.setFillBackgroundColor(IndexedColors.LIGHT_BLUE.getIndex()); 

		HSSFRow rowhead = sheet.createRow((short) 0);

		String[] headings = rt.getHeadings();
		for (int i = 0; i < headings.length; i++) {
			rowhead.createCell((short) i).setCellValue(headings[i]);
		}

		int rows = rt.getCounter();

		HSSFRow row;
		for (int i = 0; i < rows; i++) {
			row = sheet.createRow((short) i + 1);
			String[] rowi = rt.getRowAsString(i).split("\\t");
			for (int j = 1; j <= headings.length; j++) {

				row.createCell((short) j - 1).setCellValue(rowi[j]);
			}
		}

		FileOutputStream fileOut;
		try {
			fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
