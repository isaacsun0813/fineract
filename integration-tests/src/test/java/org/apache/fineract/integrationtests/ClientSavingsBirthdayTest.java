package org.apache.fineract.integrationtests;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.ArrayList;
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
        
        // Validate that the day and month match, but the year does not matter
        for (HashMap account : savingsAccounts) {
            Assertions.assertEquals("13", account.get("birthDay").toString());
            Assertions.assertEquals("8", account.get("birthMonth").toString());
        }
    }

    // Test Case 4: Perfect birthday match (day, month, year)
    @Test
    public void testSavingsAccountPerfectBirthdayMatch() {
        // Provide an exact birthday match (day, month, year)
        List<HashMap> savingsAccounts = this.savingsAccountHelper.getSavingsAccountsByBirthdayAndYear(13, 8, 2003); // August 13, 2003
        Assertions.assertFalse(savingsAccounts.isEmpty(), "Savings account should match the exact birthday and year");
        
        // Validate that both the birthday and the year match
        for (HashMap account : savingsAccounts) {
            Assertions.assertEquals("13", account.get("birthDay").toString());
            Assertions.assertEquals("8", account.get("birthMonth").toString());
            Assertions.assertEquals("2003", account.get("birthYear").toString());
        }
    }
}
