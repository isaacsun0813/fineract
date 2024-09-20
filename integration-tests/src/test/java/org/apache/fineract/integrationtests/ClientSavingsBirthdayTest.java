/*
 * Heavily inspired by ClientSavingsIntegrationTest.java. 
 * Utilizes the SavingsAccountHelper (with additional methods) to test the birthday filtering functionality.
 * 
 * The test cases are as follows:
 * 1. No birthday provided
 * 2. No match for birthday
 * 3. Matching birthday but non-matching year
 *    a. I felt like it made sense to query everything together with a matching birthday. The problem specs indicate that
 *       the year should be ignored, so this seems to be the most intuitive design decision.
 * 4. Perfect birthday match (day, month, year)
 * 
 * Key Limitations:
 * - The test cases are not exhaustive. There are many more edge cases that could be tested.
 * 
 * - The tests are brittle; they rely on the data that is already present in the database. This could be improved by 
 *   either creating mocks or relying on a different system
 * 
 * - We also are unable to run a full coverage of the entire codebase. The working branch 477b2fd has a few errors which do not seem
 *   related to our API calls, but could be due to the database setup or the API itself.
 * 
 * - If other tests have concurrency tests or anything that may modify these accounts, the tests may fail.
 * 
 * - I chose to validate by directing checking the number of accounts returned. This is not the best way to validate!
 *   A better way would be to directly check the birthdays. However I did not choose to include the birthday note in the API response 
 *   because I did not know if that would properly populate the dashboard or not. This would be a point in which I would ask for
 *   further clarification. 
 * 
 * - ALSO the tests are being ran with my local database, so the results may not be the same as the results on the working branch.
 *   I would recommend changing the dates of the birthdays to match those within the working database. This is done all on assumption 
 *   because I don't have access to any persistent data provided. 
 * 
 * 
 */
package org.apache.fineract.integrationtests;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.util.HashMap;
import java.util.List;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
public class ClientSavingsBirthdayTest {

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private SavingsAccountHelper savingsAccountHelper;

    @BeforeEach
    public void setup() {
        // Initialize REST-assured for tests
        Utils.initializeRESTAssured();
        
        // Configure the request and response specifications
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.requestSpec.header("Fineract-Platform-TenantId", "default");
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        
        // Initialize the SavingsAccountHelper instance
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
    }

    // Test Case 1: No birthday provided
    @Test
    public void testSavingsAccountNoBirthdayProvided() {
        // Retrieve all savings accounts without filtering by birthday
        Integer savingsAccounts = this.savingsAccountHelper.getSavingsAccounts();
        Assertions.assertNotNull(savingsAccounts);
        Assertions.assertTrue(savingsAccounts >0, "Savings accounts should not be empty.");

    }

    // Test Case 2: No match for birthday
    @Test
    public void testSavingsAccountNoBirthdayMatch() {
        // Provide invalid birthday parameters that don't exist
        List<HashMap> savingsAccounts = this.savingsAccountHelper.getSavingsAccountsByBirthday(32, 13); // Invalid day and month
        Assertions.assertTrue(savingsAccounts.isEmpty(), "No savings account should match this invalid birthday");
    }

    // Test Case 3: Matching birthday but non-matching year
    @Test
    public void testSavingsAccountMatchBirthdayButNotYear() {
        // Provide a matching birthday but with a non-matching year
        List<HashMap> savingsAccounts = this.savingsAccountHelper.getSavingsAccountsByBirthday(13, 8); // August 13
        Assertions.assertFalse(savingsAccounts.isEmpty(), "Savings account should match the birthday but ignore the year mismatch");
        Assertions.assertTrue(savingsAccounts.size() == 2, "We should have two savings accounts with the same birthday but different years");
    }

    // Test Case 4: Perfect birthday match (day, month, year)
    @Test
    public void testSavingsAccountPerfectBirthdayMatch() {
        // Provide an exact birthday match (day, month, year)
        List<HashMap> savingsAccounts = this.savingsAccountHelper.getSavingsAccountsByBirthdayAndYear(13, 8, 2003); // August 13, 2003
        Assertions.assertFalse(savingsAccounts.isEmpty(), "Savings account should match the exact birthday and year");
        Assertions.assertTrue(savingsAccounts.size() == 1, "We should have one savings account with the exact birthday and year");
    }
}
