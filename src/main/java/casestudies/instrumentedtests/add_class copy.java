package casestudies.instrumentedtests;

import java.io.FileWriter;
import java.io.IOException;
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
    
    //Amin injected
    private By lastUsedBy = null;
	private static String seleniumExecutionTrace = "SeleniumExecutionTrace.txt";


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
        driver.findElement(getBy(By.id("login"))).findElement(foundClear(lastUsedBy)).clear();
        driver.findElement(getBy(By.id("login"))).sendKeys(getInput("nainy"));
        driver.findElement(getBy(By.id("password"))).clear();
        driver.findElement(getBy(By.id("password"))).sendKeys(getInput("nainy"));
        driver.findElement(getBy(By.cssSelector("button[type=\"submit\"]"))).click();
        driver.findElement(getBy(By.linkText("Platform administration"))).click();
        driver.findElement(getBy(By.linkText("Manage classes"))).click();
        driver.findElement(getBy(By.linkText("Create a new class"))).click();
        driver.findElement(getBy(By.name("class_name"))).clear();
        driver.findElement(getBy(By.name("class_name"))).sendKeys(getInput("EG"));
        driver.findElement(getBy(By.cssSelector("input[type=\"submit\"]"))).click();
        // Warning: verifyTextPresent may require manual changes 
        try {
            assertTrue(driver.findElement(getBy(By.cssSelector("BODY"))).getText().matches("^[\\s\\S]*The new class has been created[\\s\\S]*$"));
        } catch (Error e) {
            verificationErrors.append(e.toString());
        }
        driver.findElement(getBy(By.linkText("Logout"))).click();
        System.out.println("INJECTED!");
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            fail(verificationErrorString);
        }
    }

    
	private By getBy(By by) {
		try {
		    FileWriter fw = new FileWriter(seleniumExecutionTrace,true); //appending new data
		    fw.write(by.toString() + "\n");
		    fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		    System.err.println("IOException: " + e.getMessage());
		}
		System.out.println(by.toString());
		lastUsedBy = by;
		return by;
	}
	
	private String getInput(String input) {
		try {
		    FileWriter fw = new FileWriter(seleniumExecutionTrace,true); //appending new data
		    fw.write("sendKeys: " + input + "\n");
		    fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("sendKeys: " + input);

		return input;
	}	

	
	private By foundClear(By by) {
		try {
		    FileWriter fw = new FileWriter(seleniumExecutionTrace,true); //appending new data
		    fw.write("clear\n");
		    fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("clear");

		return by;
	}
}
