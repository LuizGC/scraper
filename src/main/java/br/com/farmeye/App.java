package br.com.farmeye;

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class App {

	private static final String PATH_WEBDRIVER = App.class.getClassLoader().getResource("geckodriver").getPath();
	private static final Integer POOL_SIZE = Runtime.getRuntime().availableProcessors() + 1;
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(POOL_SIZE);

	private static List<String> nextBranch(String url) throws InterruptedException {

		FirefoxOptions options =
				new FirefoxOptions()
						.setHeadless(true);

		WebDriver driver = new FirefoxDriver(options);

		driver.get(url);
		driver.manage().timeouts().implicitlyWait(2, TimeUnit.MINUTES);

		WebElement breadcrumbs = driver.findElement(By.id("breadcrumbs"));

		boolean isEnd = !(breadcrumbs.findElements(By.tagName("a")).size() < 9);

		Set<String> listFilesToDownload = new HashSet<String>();

		if(isEnd){
			listFilesToDownload.addAll(getLeafs(driver));
		}else{
			listFilesToDownload.addAll(searchNewBranch(url.length(), driver));
		}


		driver.quit();

		return new ArrayList<>(listFilesToDownload);
	}

	private static List<String> getLeafs(WebDriver driver) {
		return driver.findElements(By.tagName("a"))
				.parallelStream()
				.filter(a -> a.getText().contains("."))
				.map(a-> a.getAttribute("href"))
				.collect(Collectors.toList());
	}

	private static List<String> searchNewBranch(Integer urlLength, WebDriver driver) throws InterruptedException {
		List<Callable<List<String>>> callables = driver.findElements(By.tagName("a"))
				.parallelStream()
				.map(a-> a.getAttribute("href"))
				.filter(a -> a.length() > urlLength)
				.map(href -> (Callable<List<String>>) () -> nextBranch(href))
				.collect(Collectors.toList());

		return EXECUTOR.invokeAll(callables)
				.parallelStream()
				.map(future -> {
					try {
						return future.get();
					}
					catch (Exception e) {
						throw new IllegalStateException(e);
					}
				})
				.reduce(new ArrayList<>(), (a, b) -> {
					a.addAll(b);
					return a;
				});
	}

	private static void createSaveFile(File whereToSave, String url) {
		String path = url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","");
		List<String> pathArray = new ArrayList<>(Arrays.asList(path.split("/")));
		pathArray.remove(0);

		File file = new File(whereToSave, String.join("/", pathArray)) ;

		File parent = file.getParentFile();

		if (!parent.exists()) {
			parent.mkdirs();
		}

		if(file.exists()){
			file.delete();
		}

		try {

			if(file.createNewFile()){
				System.out.println("Start download: " + url);
				FileUtils.copyURLToFile(new URL(url), file);
				System.out.println("End download: " + url);
			}

		} catch (IOException e) {
			System.out.println("file wasn't created: " + path);
		}
	}


	public static void main( String[] args ) throws Exception {

		System.setProperty("webdriver.gecko.driver", PATH_WEBDRIVER);
		System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,"/dev/null");

		JFrame frame = new JFrame("InputDialog Example #1");

		JFileChooser fileChooser = new JFileChooser();

		fileChooser.setDialogTitle("Select folder to download images from AWS");

		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		fileChooser.setAcceptAllFileFilterUsed(false);

		String url = JOptionPane.showInputDialog(
				frame,
				"You can search at https://remotepixel.ca/projects/satellitesearch.html\n" +
						"Example Sentinel: http://sentinel-s2-l1c.s3-website.eu-central-1.amazonaws.com/#tiles/21/L/UE/\n" +
						"Example Landsat: https://landsatonaws.com/L8/227/070",
				"What is the aws url that you want download?",
				JOptionPane.PLAIN_MESSAGE
		);

		if (!Strings.isNullOrEmpty(url) && fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {

			nextBranch(url)
					.parallelStream()
					.forEach(href -> createSaveFile(fileChooser.getSelectedFile(), href));

		} else {
			System.out.println("No Selection ");
		}

		System.exit(0);
	}
}
