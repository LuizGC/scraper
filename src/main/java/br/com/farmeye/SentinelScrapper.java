package br.com.farmeye;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SentinelScrapper {

	private final File folderToSave;
	private final String url;
	private final List<String> links;
	private final List<String> downloadErrors;

	public SentinelScrapper(String url, File folderToSave) {
		this.url = url;
		this.folderToSave = folderToSave;
		this.downloadErrors = new ArrayList<>();
//		Settings settings = Settings
//				.builder()
//				.headless(true)
//				.javascript(true)
//				.cache(false)
//				.build();
//		JBrowserDriver driver = new JBrowserDriver(settings);
		FirefoxOptions options = new FirefoxOptions().setHeadless(true);
		WebDriver driver = new FirefoxDriver(options);
		driver.get(url);
		driver.manage().timeouts().implicitlyWait(2, TimeUnit.MINUTES);
		links = driver.findElements(By.tagName("a"))
				.parallelStream()
				.map(link -> link.getAttribute("href"))
				.collect(Collectors.toList());
		driver.quit();

	}

	private Boolean isLeaf(){

		return links.parallelStream()
				.anyMatch(link -> link.endsWith(".jp2"));

	}

	public void downloadFiles(){

		if(isLeaf()){
			this.download();
		} else{
			this.nextBranch();
		}

	}

	private void download() {
		links
				.parallelStream()
				.filter(link -> canDownload(link))
				.forEach(link -> {
					File file = new File(folderToSave, FilenameUtils.getName(link));
					try {
						if(file.exists()){
							file.delete();
						}
						FileUtils.copyURLToFile(new URL(link), file, 2000, 2000);
						System.out.println("Success: " + link);
					} catch (IOException e) {
						e.printStackTrace();
						downloadErrors.add(link);
					}
				});
	}

	private boolean canDownload(String link) {
		return !link.endsWith("preview.jp2") && (link.endsWith(".jp2") || link.endsWith("tileInfo.json"));
	}

	private void nextBranch() {
		final int tamanho = url.length();
		links
				.parallelStream()
				.filter(link -> link.length() >= tamanho)
				.filter(link -> link.substring(tamanho).length() > 0)
				.forEach(link -> {
					File file = new File(folderToSave, link.replaceAll(url, ""));
					if(!file.exists()){
						file.mkdir();
					}
					SentinelScrapper scrapper = new SentinelScrapper(link, file);
					scrapper.downloadFiles();
					downloadErrors.addAll(scrapper.getDownloadErrors());
				});
	}

	public List<String> getDownloadErrors() {
		return new ArrayList<>(downloadErrors);
	}
}
