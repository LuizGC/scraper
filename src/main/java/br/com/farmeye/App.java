package br.com.farmeye;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class App {

	public static void main( String[] args ){

		String urlDownload = args[0];
		Double cloudPercentege = args.length < 2 ? 10 : Double.valueOf(args[1]);
		String filename = args.length < 3 ? "downloads" : args[3];

		Settings settings = Settings
				.builder()
				.headless(false)
				.javascript(true)
				.cache(true)
				.connectTimeout(0)
				.socketTimeout(0)
				.build();

		JBrowserDriver driver = new JBrowserDriver(settings);

		driver.get(urlDownload);

		String windowName = driver.getWindowHandle();

		driver.manage().timeouts().implicitlyWait(2, TimeUnit.MINUTES);

		driver.findElements(By.tagName("a"))
				.stream()
				.filter(link ->  isBranchsLinks(link, urlDownload.length()))
				.forEach(link -> {
					System.out.println(getHref(link));
					new Actions(driver)
							.keyDown(Keys.CONTROL)
							.click(link)
							.keyUp(Keys.CONTROL)
							.build()
							.perform();
					driver.switchTo().window(windowName);
				});

		driver.quit();


	}

	private static String getHref(WebElement link) {
		return link.getAttribute("href");
	}

	private static boolean isBranchsLinks(WebElement element, Integer sizeUrl) {
		String link = getHref(element);

		return link.length() >= sizeUrl &&
				link.substring(sizeUrl).length() > 0;
	}

}
