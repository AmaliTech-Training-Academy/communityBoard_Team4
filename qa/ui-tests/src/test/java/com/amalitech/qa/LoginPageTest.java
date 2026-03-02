package com.amalitech.qa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.testng.annotations.*;
import static org.testng.Assert.*;

public class LoginPageTest {

    private WebDriver driver;

    @BeforeClass
    public void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        driver = new ChromeDriver(options);
        driver.get("http://localhost:3000");
    }

    @Test
    public void testLoginPageLoads() {
        driver.get("http://localhost:3000/login");
        assertTrue(driver.getTitle().contains("CommunityBoard"));
        assertNotNull(driver.findElement(By.cssSelector("input[type=\"email\"]")));
        assertNotNull(driver.findElement(By.cssSelector("input[type=\"password\"]")));
    }

    // TODO: Add more UI tests
    // - testSuccessfulLogin
    // - testNavigationAfterLogin
    // - testCreatePostFlow
    // - testRegisterFlow

    @AfterClass
    public void teardown() {
        if (driver != null) driver.quit();
    }
}
