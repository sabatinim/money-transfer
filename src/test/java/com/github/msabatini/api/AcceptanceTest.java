package com.github.msabatini.api;

import com.github.msabatini.Application;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static com.github.msabatini.api.TransferRequestBuilder.aTransferRequest;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.client.Entity.json;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AcceptanceTest
{
  private static final String TRANSFER_PATH = "v1/customers/{customerId}/transfers";
  private static final String CUSTOMER_ID_PARAMETER = "customerId";
  private static final String CUSTOMER_ID_VALUE = "customer1";
  private static final String EUR = "EUR";
  private static final String GBP = "GBP";
  private static final String NOT_EXISTENT_CURRENCY = "notExistent";
  private static final String INVALID_AMOUNT = "-1";
  private static final String HUGE_AMOUNT = "100";

  private HttpServer server;
  private WebTarget target;

  @Before
  public void setUp()
  {
    server = Application.startServer();
    target = newClient().target(Application.BASE_URI);
  }

  @After
  public void tearDown()
  {
    server.shutdown();
  }

  @Test
  public void transferAccepted()
  {
    Response response = target.path(TRANSFER_PATH)
                              .resolveTemplate(CUSTOMER_ID_PARAMETER, CUSTOMER_ID_VALUE)
                              .request()
                              .post(json(aTransferRequest()
                                             .withAccountFrom(EUR)
                                             .withAccountTo(GBP)
                                             .build()));

    assertThat(response.getStatus(), is(202));
    assertThat(response.readEntity(String.class), is("Transfer is accepted"));
  }

  @Test
  public void accountNotFound()
  {
    Response response = target.path(TRANSFER_PATH)
                              .resolveTemplate(CUSTOMER_ID_PARAMETER, CUSTOMER_ID_VALUE)
                              .request()
                              .post(json(aTransferRequest()
                                             .withAccountFrom(NOT_EXISTENT_CURRENCY)
                                             .build()));

    assertThat(response.getStatus(), is(404));
    assertThat(response.readEntity(String.class), is("Account with id notExistent not found"));
  }

  @Test
  public void insufficientBalance()
  {
    Response response = target.path(TRANSFER_PATH)
                              .resolveTemplate(CUSTOMER_ID_PARAMETER, CUSTOMER_ID_VALUE)
                              .request()
                              .post(json(aTransferRequest().withAccountFrom(EUR)
                                                           .withAccountTo(GBP)
                                                           .withAmount(HUGE_AMOUNT)
                                                           .build()));

    assertThat(response.getStatus(), is(400));
    assertThat(response.readEntity(String.class), containsString("Balance insufficient"));
  }

  @Test
  public void aWrongRequest()
  {
    Response response = target.path(TRANSFER_PATH)
                              .resolveTemplate(CUSTOMER_ID_PARAMETER, CUSTOMER_ID_VALUE)
                              .request()
                              .post(json(aTransferRequest().withAccountFrom(EUR)
                                                           .withAccountTo(GBP)
                                                           .withAmount(INVALID_AMOUNT)
                                                           .build()));

    assertThat(response.getStatus(), is(500));
    assertThat(response.readEntity(String.class), is("Invalid value EUR -1. It has to be positive"));
  }

}