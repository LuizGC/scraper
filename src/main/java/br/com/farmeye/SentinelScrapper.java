package br.com.farmeye;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SentinelScrapper {

	private final File folderToSave;
	private final String url;
	private final List<String> links;

	public SentinelScrapper(String url, File folderToSave) {
		this.url = url;
		this.folderToSave = folderToSave;
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

		return links.parallelStream().anyMatch(link -> link.endsWith(".jp2"));

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
				.stream()
				.filter(link ->  link.endsWith(".jp2"))
				.forEach(link -> {
					File file = new File(folderToSave, FilenameUtils.getName(link));
					try {
						if(file.exists()){
							file.delete();
						}

						FileUtils.copyURLToFile(new URL(link), file, 1000, 1000);
						System.out.println("Sucesso: " + link);
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Erro: " + link);
					}
				});
	}

	private void nextBranch() {
		final int tamanho = url.length();
		links
				.stream()
				.filter(link -> link.length() >= tamanho)
				.filter(link -> link.substring(tamanho).length() > 0)
				.forEach(link -> {
					File file = new File(folderToSave, link.replaceAll(url, ""));
					if(!file.exists()){
						file.mkdir();
					}
					SentinelScrapper scrapper = new SentinelScrapper(link, file);
					scrapper.downloadFiles();
				});
	}


}
