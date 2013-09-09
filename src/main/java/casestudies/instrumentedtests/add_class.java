package casestudies.instrumentedtests;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class add_class {

    private WebDriver driver;

    private String baseUrl;

    private boolean acceptNextAlert = true;

    private StringBuffer verificationErrors = new StringBuffer();

    @Before
    public void setUp() throws Exception {
        driver = new FirefoxDriver();
        baseUrl = "http://watersmc.ece.ubc.ca:8888/";
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @Test
    public void testAddClass() throws Exception {
        driver.get(baseUrl + "/claroline-1.11.7/index.php");
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.id("login"))).clear();
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.id("login"))).sendKeys(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getInput("nainy"));
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.id("password"))).clear();
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.id("password"))).sendKeys(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getInput("nainy"));
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.cssSelector("button[type=\"submit\"]"))).click();
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.linkText("Platform administration"))).click();
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.linkText("Manage classes"))).click();
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.linkText("Create a new class"))).click();
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.name("class_name"))).clear();
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.name("class_name"))).sendKeys(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getInput("EG"));
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.cssSelector("input[type=\"submit\"]"))).click();
        // Warning: verifyTextPresent may require manual changes 
        try {
            assertTrue(driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.cssSelector("BODY"))).getText().matches("^[\\s\\S]*The new class has been created[\\s\\S]*$"));
        } catch (Error e) {
            verificationErrors.append(e.toString());
        }
        driver.findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy(By.linkText("Logout"))).click();
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            fail(verificationErrorString);
        }
    }

    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean isAlertPresent() {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    private String closeAlertAndGetItsText() {
        try {
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            if (acceptNextAlert) {
                alert.accept();
            } else {
                alert.dismiss();
            }
            return alertText;
        } finally {
            acceptNextAlert = true;
        }
    }
}
public class Tracker {

    public static void main(String[] args) {
        System.out.println("hello, world");
    }
}
