package br.com.farmeye;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.apache.commons.validator.UrlValidator;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class App {

	private static final String PATH_WEBDRIVER = App.class.getClassLoader().getResource("geckodriver").getPath();

	private static File chooseFolder(JFrame frame){

		JFileChooser fileChooser = new JFileChooser();

		fileChooser.setDialogTitle("Select folder to download images from AWS");

		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		fileChooser.setAcceptAllFileFilterUsed(false);

		if(fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION){

			return fileChooser.getSelectedFile();

		} else {

			JOptionPane.showMessageDialog(frame, "Folder path was not selected!");
			System.exit(0);
			return null;
		}

	}

	private static String insertPath(JFrame frame){

		String textDescription = "You can search at https://remotepixel.ca/projects/satellitesearch.html\n" +
				"Example Sentinel: http://sentinel-s2-l1c.s3-website.eu-central-1.amazonaws.com/#tiles/21/L/UE/\n" +
				//"Example Landsat: https://landsatonaws.com/L8/227/070";
				"Download Landsat data still in development.";
		String textQuestion = "What is the aws url that you want download?";


		String url = JOptionPane.showInputDialog(
				frame,
				textDescription,
				textQuestion,
				JOptionPane.PLAIN_MESSAGE
		);

		UrlValidator urlValidator = new UrlValidator();

		if(urlValidator.isValid(url)){

			return url;

		} else {

			JOptionPane.showMessageDialog(frame, "Url format is wrong!");
			System.exit(0);
			return null;

		}
	}

	public static void main( String[] args ) throws IOException {

		System.setProperty("webdriver.gecko.driver", PATH_WEBDRIVER);

		System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,"/dev/null");

//		JFrame frame = new JFrame();
//
//		String url = insertPath(frame);
		String url = "http://sentinel-s2-l1c.s3-website.eu-central-1.amazonaws.com/#tiles/21/L/WE/2016/11/";
//
//		File folderToSave = chooseFolder(frame);
		File folderToSave = new File("/home/luiz/Downloads/teste");

		if(folderToSave.exists()){
			FileUtils.deleteDirectory(folderToSave);
		}

		folderToSave.mkdir();

		SentinelScrapper scrapper = new SentinelScrapper(url, folderToSave);
		scrapper.downloadFiles();


		System.exit(0);
	}
}
