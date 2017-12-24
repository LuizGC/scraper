package br.com.farmeye;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class App {

	//private static final String URL = "http://sentinel-s2-l1c.s3-website.eu-central-1.amazonaws.com/#tiles/21/L/UE/";
	//private static final String URL = "http://sentinel-s2-l1c.s3-website.eu-central-1.amazonaws.com/#tiles/21/L/UE/2015/11/";
	private static final String URL = "http://sentinel-s2-l1c.s3-website.eu-central-1.amazonaws.com/#tiles/21/L/UE/2015/11/17/";
	private static final String PATH_WEBDRIVER = App.class.getClassLoader().getResource("geckodriver").getPath();
	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

	private static List<String> nextBranch(String url) throws InterruptedException {

		FirefoxOptions options =
				new FirefoxOptions()
						.setHeadless(true);

		WebDriver driver = new FirefoxDriver(options);

		driver.get(url);
		driver.manage().timeouts().implicitlyWait(2, TimeUnit.MINUTES);

		WebElement breadcrumbs = driver.findElement(By.id("breadcrumbs"));

		boolean isEnd = !(breadcrumbs.findElements(By.tagName("a")).size() < 9);

		List<String> listFilesToDownload = new ArrayList<>();

		if(isEnd){
			listFilesToDownload.addAll(getLeafs(driver));
		}else{
			listFilesToDownload.addAll(searchNewBranch(url.length(), driver));
		}


		driver.quit();

		return listFilesToDownload;
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


	public static void main( String[] args ) throws Exception {

		System.setProperty("webdriver.gecko.driver", PATH_WEBDRIVER);
		System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,"/dev/null");

		nextBranch(URL)
				.parallelStream()
				.forEach(System.out::println);

	}
}
