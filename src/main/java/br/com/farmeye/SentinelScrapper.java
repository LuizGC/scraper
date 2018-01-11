package br.com.farmeye;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SentinelScrapper {

	private final List<WebElement> nextBranchLinks;
	private final List<String> downloadLinks;
	private Boolean isLeaf = false;

	public SentinelScrapper(String url, JBrowserDriver driver) {

		driver.get(url);

		List<WebElement> elements = driver.findElements(By.tagName("a"))
				.parallelStream()
				.collect(Collectors.toList());

		isLeaf = elements.parallelStream()
				.anyMatch(link -> getHref(link).endsWith(".jp2"));

		nextBranchLinks = elements.parallelStream()
				.filter(link -> isBranchsLinks(link, url.length()))
				.collect(Collectors.toList());

		downloadLinks = elements.parallelStream()
				.filter(link -> canDownload(link))
				.map(this::getHref)
				.collect(Collectors.toList());

	}

	private String getHref(WebElement link) {
		return link.getAttribute("href");
	}

	private boolean isBranchsLinks(WebElement element, Integer sizeUrl) {
		String link = getHref(element);

		return link.length() >= sizeUrl &&
				link.substring(sizeUrl).length() > 0;
	}

	private boolean canDownload(WebElement element) {
		String link = getHref(element);
		return !link.endsWith("preview.jp2") && (link.endsWith(".jp2") || link.endsWith("tileInfo.json"));
	}

	public Boolean isLeaf(){

		return isLeaf;

	}

	public List<WebElement> getNextBranchLinks(){

		return new ArrayList<>(nextBranchLinks);

	}

	public List<String> getDownloadLinks() {
		return  new ArrayList<>(downloadLinks);
	}
}
