package nl.surfnet.coin.selfservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import nl.surfnet.coin.selfservice.domain.*;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CsaImplTest {

  private CsaImpl subject = new CsaImpl("http://localhost:8889/oauth/token", "client", "secret", "actions cross-idp-services stats", "http://localhost:8889");

  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(8889);

  private String idp = "idp";


  @Test
  public void testGetServicesForIdp() throws Exception {
    stubResponse("csa-json/protected-services.json", "/api/protected/idp/services.json\\?idpEntityId=idp&lang=en");
    List<Service> services = subject.getServicesForIdp(idp);
    assertEquals(11, services.size());

    Service service = services.get(0);
    ARP arp = service.getArp();
    Map<String, List<Object>> attributes = arp.getAttributes();
    assertEquals(19, attributes.size());

    assertEquals(2, service.getCategories().size());

  }

  @Test
  public void testGetInstitutionIdentityProviders() throws Exception {
    stubResponse("csa-json/institution-identity-providers.json", "/api/protected/identityproviders.json\\?identityProviderId=idp");
    List<InstitutionIdentityProvider> providers = subject.getInstitutionIdentityProviders(idp);
    assertEquals(2, providers.size());
  }

  @Test
  public void testGetAllInstitutionIdentityProviders() throws Exception {
    stubResponse("csa-json/all-institution-identity-providers.json", "/api/protected/all-identityproviders.json");
    List<InstitutionIdentityProvider> providers = subject.getAllInstitutionIdentityProviders();
    assertEquals(4, providers.size());
  }

  @Test
  public void testGetJiraActions() throws Exception {
    stubResponse("csa-json/actions.json", "/api/protected/actions.json\\?idpEntityId=idp");
    List<Action> jiraActions = subject.getJiraActions(idp);
    assertEquals(3, jiraActions.size());
  }

  @Test
  public void testGetTaxonomy() throws Exception {
    stubResponse("csa-json/taxonomy_en.json", "/api/public/taxonomy.json\\?lang=en");
    Taxonomy taxonomy = subject.getTaxonomy();
    assertEquals(2, taxonomy.getCategories().size());
    assertEquals(2, taxonomy.getCategories().get(0).getValues().size());
  }

  @Test
  public void testGetServiceForIdp() throws Exception {
    stubResponse("csa-json/service.json", "/api/protected/services/1.json\\?idpEntityId=idp&lang=en");
    Service service = subject.getServiceForIdp(idp, 1);
    assertEquals("Mock Institution Name", service.getLicense().getInstitutionName());
  }

  @Test
  public void testServiceUsedBy() throws IOException {
    stubResponse("csa-json/all-institution-identity-providers.json", "/api/protected/services-usage.json\\?serviceId=1");
    List<InstitutionIdentityProvider> providers = subject.serviceUsedBy(1);
    assertEquals(4, providers.size());
  }

  @Test
  public void testLicenseContactPerson() throws IOException {
    stubResponse("csa-json/license-contact-person.json", "/api/protected/licensecontactperson.json\\?identityProviderId=idp");
    Optional<LicenseContactPerson> personOptional = subject.licenseContactPerson(idp);
    assertTrue(personOptional.isPresent());
    assertEquals(personOptional.get().getName(), "John Doe");
  }

  @Test
  public void testLicenseContactPersonNotFound() throws IOException {
    stubAccessToken();
    wireMockRule.stubFor(get(urlMatching("/api/protected/licensecontactperson.json\\?identityProviderId=idp")).willReturn(aResponse().withStatus(404)));
    Optional<LicenseContactPerson> personOptional = subject.licenseContactPerson(idp);
    assertFalse(personOptional.isPresent());
  }

  @Test
  public void testCreateAction() throws Exception {
    Action action = action();

    stubAccessToken();
    String response = IOUtils.toString(new ClassPathResource("csa-json/action.json").getInputStream());
    wireMockRule.stubFor(post(urlMatching("/api/protected/action.json")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(response)));

    Action result = subject.createAction(action);
    assertEquals(JiraTask.Status.OPEN, result.getStatus());
  }

  private Action action() {
    Action action = new Action();
    action.setUserId("uuid");
    action.setUserEmail("test@example.org");
    action.setUserName("John Doe");
    action.setType(JiraTask.Type.LINKREQUEST);
    action.setBody("comments");
    action.setIdpId("idpEntityId");
    action.setSpId("spEntityId");
    action.setInstitutionId("institutionId");
    return action;
  }

  private void stubResponse(String jsonFileName, String url) throws IOException {
    stubAccessToken();

    String response = IOUtils.toString(new ClassPathResource(jsonFileName).getInputStream());
    wireMockRule.stubFor(get(urlMatching(url)).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(response)));
  }

  private void stubAccessToken() {
    String json = "{\"access_token\": \"123456\",\"token_type\": \"client-credentials\"}";
    wireMockRule.stubFor(post(urlEqualTo("/oauth/token")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(json)));
  }
}
