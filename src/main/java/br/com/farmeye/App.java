package br.com.farmeye;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.validator.UrlValidator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class App {

	private static JTextArea textArea;

	public static void main( String[] args ){

		JFrame frame = new JFrame("Log");

		String urlDownload = setUrlDownload();
		File folderToSave = chooseFolder();

		textArea = new JTextArea();

		textArea.setEditable(false);
		textArea.setText("Download Scenes from " + urlDownload + "\n");
		frame.getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(new Dimension(800, 600));
		frame.setVisible(true);

		List<String> linksDownload = getLinksDownload(urlDownload);
		List<String> errorLinks = new ArrayList<>();

		linksDownload
				.parallelStream()
				.map(link -> {
					try {
						return new URL(link);
					} catch (MalformedURLException e) {
						throw new RuntimeException(e);
					}
				})
				.forEach(url -> {
					try {
						download(folderToSave, url);
					} catch (IOException e) {
						errorLinks.add(url.toString());
					}
				});


		errorLinks.removeAll(errorLinks.parallelStream().filter(url -> {
			try {
				download(folderToSave, new URL(url));
				return true;
			} catch (IOException e) {
				textArea.append(url + "\n");
				textArea.setCaretPosition(textArea.getDocument().getLength());
				return false;
			}

		}).collect(Collectors.toList()));

		textArea.append("\n");
		textArea.append("\n");
		textArea.append("Next Urls had problems to Download:\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
		errorLinks.parallelStream().forEach(System.out::println);

		textArea.append("\n");
		textArea.append("Finished");
		textArea.setCaretPosition(textArea.getDocument().getLength());

	}

	private static void download(File folderToSave, URL url) throws IOException {
		String filename = FilenameUtils.getName(url.getPath());
		String folderPath = url.getPath().replace(filename, "");

		File folder = new File(folderToSave, folderPath);

		if(!folder.exists()){
			folder.mkdirs();
		}

		File file = new File(folder, filename);
		String intern = url.toString().intern();
		synchronized(intern){
			textArea.append("Downloading " +url+ "\n");
			textArea.setCaretPosition(textArea.getDocument().getLength());
		}

		FileUtils.copyURLToFile(url, file, 5000, 5000);

		synchronized(intern){
			textArea.append("Finished " +url+ "\n");
			textArea.setCaretPosition(textArea.getDocument().getLength());
		}
	}

	private static String getHref(WebElement link) {
		return link.getAttribute("href");
	}


	private static List<String> getUrls(List<WebElement> links, int urlLength){
		if(isLeaf(links)){

			return links.parallelStream()
					.map(App::getHref)
					.filter(link ->  canDownload(link))
					.collect(Collectors.toList());

		} else {

			return links.parallelStream()
					.filter(link ->  isBranchsLinks(link, urlLength))
					.map(App::getHref)
					.collect(Collectors.toList());

		}
	}

	private static List<String> getLinksDownload(String urlDownload){

		Settings settings = Settings
				.builder()
				.quickRender(true)
				.headless(true)
				.javascript(true)
				.cache(true)
				.ajaxResourceTimeout(10000)
				.connectTimeout(0)
				.socketTimeout(0)
				.build();

		JBrowserDriver driver = new JBrowserDriver(settings);

		driver.get(urlDownload);

		driver.manage().timeouts().implicitlyWait(3, TimeUnit.MINUTES);

		List<String> links = getUrls(driver.findElements(By.tagName("a")), urlDownload.length());
		List<String> linksDownload = new ArrayList<>();

		while (!links.isEmpty()) {
			String url = links.get(0);
			textArea.append("Broswing at " +url+ "\n");
			textArea.setCaretPosition(textArea.getDocument().getLength());
			driver.navigate().to(url);
			List<WebElement> linksElements =  driver.findElements(By.tagName("a"));
			List<String> urls = getUrls(linksElements, url.length());
			if(isLeaf(linksElements)){
				linksDownload.addAll(urls);
			} else {
				links.addAll(urls);
			}
			links.remove(0);
		}

		driver.quit();

		return linksDownload;
	}

	private static boolean canDownload(String link) {
		return !link.equals("preview.jp2") &&
				(link.endsWith(".jp2") ||
						link.equals("tileInfo.json") ||
						link.equals("preview.jpg"));
	}

	private static boolean isBranchsLinks(WebElement element, Integer sizeUrl) {
		String link = getHref(element);

		return link.length() >= sizeUrl &&
				link.substring(sizeUrl).length() > 0;
	}

	private static Boolean isLeaf(List<WebElement> links){

		return links
				.parallelStream()
				.map(App::getHref)
				.anyMatch(link -> link.endsWith(".jp2"));

	}

	private static String setUrlDownload(){

		String textDescription = "You can search at https://remotepixel.ca/projects/satellitesearch.html\n" +
				"Example Sentinel: http://sentinel-s2-l1c.s3-website.eu-central-1.amazonaws.com/#tiles/21/L/UE/\n" +
				//"Example Landsat: https://landsatonaws.com/L8/227/070";
				"Download Landsat data still in development.";
		String textQuestion = "What is the aws url that you want download?";


		String url = JOptionPane.showInputDialog(
				null,
				textDescription,
				textQuestion,
				JOptionPane.PLAIN_MESSAGE
		);

		UrlValidator urlValidator = new UrlValidator();

		if(urlValidator.isValid(url)){

			return url;

		} else {

			JOptionPane.showMessageDialog(null, "Url format is wrong!");
			System.exit(0);
			return null;

		}
	}

	private static File chooseFolder(){

		JFileChooser fileChooser = new JFileChooser();

		fileChooser.setDialogTitle("Select folder to download images from AWS");

		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		fileChooser.setAcceptAllFileFilterUsed(false);

		if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){

			return fileChooser.getSelectedFile();

		} else {

			JOptionPane.showMessageDialog(null, "Folder path was not selected!");
			System.exit(0);
			return null;
		}

	}

}
