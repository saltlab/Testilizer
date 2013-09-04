package com.crawljax.plugins.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class Utils {

	public static void print2DArray(double[][] distances) {

		for (double[] arr : distances) {
			System.out.println(Arrays.toString(arr));
		}
	}

	public static void printList(List cluster) {
		for (Object o : cluster) {
			System.out.println(cluster.size() + ": " + o);
		}
	}

	public static void printArrayList(List<Object> cluster) {
		System.out.println(cluster.size() + "::: ");
		for (Object o : cluster) {
			System.out.println("::: " + o);
		}
	}

	public static String[] convertToArrayofString(ArrayList<File> domFiles) {

		String[] array = new String[domFiles.size()];
		for (int i = 0; i < domFiles.size(); i++) {
			array[i] = domFiles.get(i).getName();
		}
		return array;
	}

	public static String getFileName(String file) {
		String name = new File(file).getName();
		name = (name.contains(".")) ? name.substring(0, name.indexOf(".")) : name;
		return name;
	}

	public static String getFileFullName(String file) {
		file = file.replace(System.getProperty("user.dir"), "");
		file = file.replace("/src/main/java/", "");
		file = (file.contains(".")) ? file.substring(0, file.indexOf(".")) : file;
		file = file.replace("/", ".");
		return file;
	}

	public static void writeArrayToFiles(List<String> doms, String loc) {
		try {
			FileUtils.deleteQuietly(new File(loc));
			for (int i = 0; i < doms.size(); i++) {
				FileUtils.writeStringToFile(new File(loc + i + ".html"), (String) doms.get(i));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void write2DArrayToFile(double[][] distances) {
		String str2write = "";
		try {
			FileUtils.deleteQuietly(new File("Distances.csv"));

			for (int i = 0; i < distances.length; i++) {
				String line = "";
				for (int j = 0; j < distances.length; j++) {
					line += distances[i][j] + ",";
				}
				line += "\n";
				str2write += line;
			}
			FileUtils.writeStringToFile(new File("Distances.csv"), str2write);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static double[][] loadArrayFromFile(File file) {
		try {

			List<String> fileLines = FileUtils.readLines(file);
			double[][] distances = new double[fileLines.size()][fileLines.size()];
			for (int i = 0; i < fileLines.size(); i++) {
				String line = fileLines.get(i);
				String[] split = line.split(",");
				for (int j = 0; j < split.length; j++) {
					distances[i][j] = Double.parseDouble(split[j]);
				}
			}
			return distances;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}
