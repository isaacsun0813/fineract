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
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.requestSpec.header("Fineract-Platform-TenantId", "default");
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
    }

    // // Test Case 1: No birthday provided
    // @Test
    // public void testSavingsAccountNoBirthdayProvided() {
    //     List<HashMap> savingsAccounts = this.savingsAccountHelper.retrieveAllSavingsAccounts(null, null, null);
    //     Assertions.assertNotNull(savingsAccounts);
    //     Assertions.assertFalse(savingsAccounts.isEmpty(), "Savings accounts should be returned when no birthday is provided");
    // }

    // // Test Case 2: No match for birthday
    // @Test
    // public void testSavingsAccountNoBirthdayMatch() {
    //     List<HashMap> savingsAccounts = this.savingsAccountHelper.retrieveAllSavingsAccounts(32, 13, null); // Invalid
    //                                                                                                         // day and
    //                                                                                                         // month
    //     Assertions.assertTrue(savingsAccounts.isEmpty(), "No savings account should match this invalid birthday");
    // }

    // // Test Case 3: Matching birthday but non-matching year
    // @Test
    // public void testSavingsAccountMatchBirthdayButNotYear() {
    //     List<HashMap> savingsAccounts = this.savingsAccountHelper.retrieveAllSavingsAccounts(13, 8, 1999); // August 13,
    //                                                                                                        // year 1999
    //     Assertions.assertFalse(savingsAccounts.isEmpty(), "Savings account should match the birthday but ignore the year mismatch");
    //     for (HashMap account : savingsAccounts) {
    //         Assertions.assertEquals("13", account.get("birthDay").toString());
    //         Assertions.assertEquals("8", account.get("birthMonth").toString());
    //     }
    // }

    // // Test Case 4: Perfect birthday match (day, month, year)
    // @Test
    // public void testSavingsAccountPerfectBirthdayMatch() {
    //     List<HashMap> savingsAccounts = this.savingsAccountHelper.retrieveAllSavingsAccounts(13, 8, 2003); // August 13,
    //                                                                                                        // 2003
    //     Assertions.assertFalse(savingsAccounts.isEmpty(), "Savings account should match the exact birthday and year");
    //     for (HashMap account : savingsAccounts) {
    //         Assertions.assertEquals("13", account.get("birthDay").toString());
    //         Assertions.assertEquals("8", account.get("birthMonth").toString());
    //         Assertions.assertEquals("2003", account.get("birthYear").toString());
    //     }
    // }
}
