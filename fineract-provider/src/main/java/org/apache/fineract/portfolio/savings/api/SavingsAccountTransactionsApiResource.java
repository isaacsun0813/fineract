/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.savings.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.portfolio.paymenttype.service.PaymentTypeReadPlatformService;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Path("/v1/savingsaccounts/{savingsId}/transactions")
@Component
@Tag(name = "Savings Account Transactions", description = "")
@RequiredArgsConstructor
public class SavingsAccountTransactionsApiResource {

    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<SavingsAccountTransactionData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final SavingsAccountReadPlatformService savingsAccountReadPlatformService;
    private final PaymentTypeReadPlatformService paymentTypeReadPlatformService;

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveTemplate(@PathParam("savingsId") final Long savingsId,
            // @QueryParam("command") final String commandParam,
            @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

        // FIXME - KW - for now just send back generic default information for
        // both deposit/withdrawal templates
        SavingsAccountTransactionData savingsAccount = this.savingsAccountReadPlatformService.retrieveDepositTransactionTemplate(savingsId,
                DepositAccountType.SAVINGS_DEPOSIT);
        final Collection<PaymentTypeData> paymentTypeOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        savingsAccount = SavingsAccountTransactionData.templateOnTop(savingsAccount, paymentTypeOptions);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, savingsAccount,
                SavingsApiSetConstants.SAVINGS_TRANSACTION_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{transactionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("savingsId") final Long savingsId, @PathParam("transactionId") final Long transactionId,
            @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);
        SavingsAccountTransactionData transactionData = this.savingsAccountReadPlatformService.retrieveSavingsTransaction(savingsId,
                transactionId, DepositAccountType.SAVINGS_DEPOSIT);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        if (settings.isTemplate()) {
            final Collection<PaymentTypeData> paymentTypeOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
            transactionData = SavingsAccountTransactionData.templateOnTop(transactionData, paymentTypeOptions);
        }

        return this.toApiJsonSerializer.serialize(settings, transactionData,
                SavingsApiSetConstants.SAVINGS_TRANSACTION_RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SavingsAccountTransactionsApiResourceSwagger.PostSavingsAccountTransactionsRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SavingsAccountTransactionsApiResourceSwagger.PostSavingsAccountTransactionsResponse.class))) })
    public String transaction(@PathParam("savingsId") final Long savingsId, @QueryParam("command") final String commandParam,
            final String apiRequestBodyAsJson) {
        try {
            final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

            CommandProcessingResult result = null;
            if (is(commandParam, "deposit")) {
                final CommandWrapper commandRequest = builder.savingsAccountDeposit(savingsId).build();
                result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            } else if (is(commandParam, "gsimDeposit")) {
                final CommandWrapper commandRequest = builder.gsimSavingsAccountDeposit(savingsId).build();
                result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            } else if (is(commandParam, "withdrawal")) {
                final CommandWrapper commandRequest = builder.savingsAccountWithdrawal(savingsId).build();
                result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            } else if (is(commandParam, "postInterestAsOn")) {
                final CommandWrapper commandRequest = builder.savingsAccountInterestPosting(savingsId).build();
                result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            } else if (is(commandParam, SavingsApiConstants.COMMAND_HOLD_AMOUNT)) {
                final CommandWrapper commandRequest = builder.holdAmount(savingsId).build();
                result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            }

            if (result == null) {
                //
                throw new UnrecognizedQueryParamException("command", commandParam,
                        new Object[] { "deposit", "withdrawal", SavingsApiConstants.COMMAND_HOLD_AMOUNT });
            }

            return this.toApiJsonSerializer.serialize(result);
        } catch (ObjectOptimisticLockingFailureException lockingFailureException) {
            throw new PlatformDataIntegrityException("error.msg.savings.concurrent.operations",
                    "Concurrent Transactions being made on this savings account: " + lockingFailureException.getMessage(),
                    lockingFailureException);
        } catch (CannotAcquireLockException cannotAcquireLockException) {
            throw new PlatformDataIntegrityException("error.msg.savings.concurrent.operations.unable.to.acquire.lock",
                    "Unable to acquir lock for this transaction: " + cannotAcquireLockException.getMessage(), cannotAcquireLockException);
        }
    }

    @POST
    @Path("{transactionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Undo/Reverse/Modify/Release Amount transaction API", description = "Undo/Reverse/Modify/Release Amount transaction API\n\n"
            + "Example Requests:\n" + "\n" + "\n" + "savingsaccounts/{savingsId}/transactions/{transactionId}?command=reverse\n" + "\n"
            + "Accepted command = undo, reverse, modify, releaseAmount")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SavingsAccountTransactionsApiResourceSwagger.PostSavingsAccountBulkReversalTransactionsRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SavingsAccountTransactionsApiResourceSwagger.PostSavingsAccountBulkReversalTransactionsRequest.class)))) })
    public String adjustTransaction(@PathParam("savingsId") final Long savingsId, @PathParam("transactionId") final Long transactionId,
            @QueryParam("command") final String commandParam, final String apiRequestBodyAsJson) {

        String jsonApiRequest = apiRequestBodyAsJson;
        if (StringUtils.isBlank(jsonApiRequest)) {
            jsonApiRequest = "{}";
        }

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(jsonApiRequest);

        CommandProcessingResult result = null;
        if (is(commandParam, SavingsApiConstants.COMMAND_UNDO_TRANSACTION)) {
            final CommandWrapper commandRequest = builder.undoSavingsAccountTransaction(savingsId, transactionId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, SavingsApiConstants.COMMAND_REVERSE_TRANSACTION)) {
            final CommandWrapper commandRequest = builder.reverseSavingsAccountTransaction(savingsId, transactionId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, SavingsApiConstants.COMMAND_ADJUST_TRANSACTION)) {
            final CommandWrapper commandRequest = builder.adjustSavingsAccountTransaction(savingsId, transactionId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, SavingsApiConstants.COMMAND_RELEASE_AMOUNT)) {
            final CommandWrapper commandRequest = builder.releaseAmount(savingsId, transactionId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            //
            throw new UnrecognizedQueryParamException("command", commandParam,
                    new Object[] { SavingsApiConstants.COMMAND_UNDO_TRANSACTION, SavingsApiConstants.COMMAND_ADJUST_TRANSACTION,
                            SavingsApiConstants.COMMAND_RELEASE_AMOUNT, SavingsApiConstants.COMMAND_REVERSE_TRANSACTION });
        }

        return this.toApiJsonSerializer.serialize(result);
    }
}
