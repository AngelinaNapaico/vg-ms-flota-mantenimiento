package vg_ms.flota.orden.interfaces.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenesSwaggerSeleniumIT {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void abrirSwagger() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        String baseUrl = System.getProperty(
                "selenium.baseUrl",
                System.getenv().getOrDefault("SELENIUM_BASE_URL", "http://localhost:9012"));
        driver.get(baseUrl + "/swagger-ui.html");
    }

    @AfterEach
    void cerrarNavegador() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void swaggerUiCargaLaDocumentacion() {
        wait.until(webDriver -> webDriver.findElement(By.cssSelector(".swagger-ui")).isDisplayed());

        assertTrue(driver.getTitle().contains("Swagger UI"));
    }

    @Test
    void swaggerUiPublicaLosEndpointsDeOrdenes() {
        wait.until(webDriver -> webDriver.findElement(By.cssSelector(".swagger-ui")).isDisplayed());
        wait.until(webDriver -> webDriver.findElement(By.tagName("body"))
                .getText().contains("/api/flota-mantenimiento/ordenes"));

        assertTrue(driver.findElement(By.tagName("body")).getText()
                .contains("Órdenes de Mantenimiento de Flota"));
    }
}
