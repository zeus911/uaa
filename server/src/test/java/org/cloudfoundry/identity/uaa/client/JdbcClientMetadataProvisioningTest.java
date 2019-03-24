package org.cloudfoundry.identity.uaa.client;

import org.cloudfoundry.identity.uaa.test.JdbcTestBase;
import org.cloudfoundry.identity.uaa.util.PredicateMatcher;
import org.cloudfoundry.identity.uaa.zone.IdentityZone;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneHolder;
import org.cloudfoundry.identity.uaa.zone.MultitenantJdbcClientDetailsService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;

import java.net.URL;
import java.util.List;

import static org.cloudfoundry.identity.uaa.test.ModelTestUtils.getResourceAsString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class JdbcClientMetadataProvisioningTest extends JdbcTestBase {

    private String createdBy;
    private RandomValueStringGenerator randomValueStringGenerator;

    private JdbcClientMetadataProvisioning jdbcClientMetadataProvisioning;

    @Before
    public void createDatasource() {
        randomValueStringGenerator = new RandomValueStringGenerator(8);
        createdBy = "createdBy-" + randomValueStringGenerator.generate();

        MultitenantJdbcClientDetailsService clientService = new MultitenantJdbcClientDetailsService(jdbcTemplate);
        jdbcClientMetadataProvisioning = new JdbcClientMetadataProvisioning(clientService, jdbcTemplate);
    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void constraintViolation_WhenNoMatchingClientFound() throws Exception {
        ClientMetadata clientMetadata = createTestClientMetadata(
                randomValueStringGenerator.generate(),
                true,
                new URL("http://app.launch/url"),
                base64EncodedImg,
                createdBy);
        jdbcClientMetadataProvisioning.update(clientMetadata, IdentityZoneHolder.get().getId());
    }

    @Test
    public void createdByPadsTo36Chars() {
        String clientId = randomValueStringGenerator.generate();
        jdbcTemplate.execute(
                String.format("insert into oauth_client_details(client_id, identity_zone_id, created_by) values ('%s', '%s', '%s')",
                        clientId,
                        IdentityZone.getUaaZoneId(),
                        "abcdef")
        );

        ClientMetadata retrievedClientMetadata = jdbcClientMetadataProvisioning.retrieve(
                clientId,
                IdentityZoneHolder.get().getId());

        assertThat(retrievedClientMetadata.getCreatedBy().length(), is(36));
    }

    @Test
    public void retrieveClientMetadata() throws Exception {
        String clientId = randomValueStringGenerator.generate();
        jdbcTemplate.execute(
                String.format("insert into oauth_client_details(client_id, identity_zone_id, created_by) values ('%s', '%s', '%s')",
                        clientId,
                        IdentityZone.getUaaZoneId(),
                        createdBy)
        );
        ClientMetadata clientMetadata = createTestClientMetadata(
                clientId,
                true,
                new URL("http://app.launch/url"),
                base64EncodedImg,
                createdBy);
        ClientMetadata createdClientMetadata = jdbcClientMetadataProvisioning.update(clientMetadata, IdentityZoneHolder.get().getId());

        ClientMetadata retrievedClientMetadata = jdbcClientMetadataProvisioning.retrieve(createdClientMetadata.getClientId(), IdentityZoneHolder.get().getId());

        assertThat(retrievedClientMetadata.getClientId(), is(clientMetadata.getClientId()));
        assertThat(retrievedClientMetadata.getIdentityZoneId(), is(IdentityZone.getUaaZoneId()));
        assertThat(retrievedClientMetadata.isShowOnHomePage(), is(clientMetadata.isShowOnHomePage()));
        assertThat(retrievedClientMetadata.getAppLaunchUrl(), is(clientMetadata.getAppLaunchUrl()));
        assertThat(retrievedClientMetadata.getAppIcon(), is(clientMetadata.getAppIcon()));
        assertThat(retrievedClientMetadata.getCreatedBy(), containsString(clientMetadata.getCreatedBy()));
    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void retrieveClientMetadata_ThatDoesNotExist() {
        String clientId = randomValueStringGenerator.generate();
        jdbcClientMetadataProvisioning.retrieve(clientId, IdentityZoneHolder.get().getId());
    }

    @Test
    public void retrieveAllClientMetadata() throws Exception {
        String clientId = randomValueStringGenerator.generate();
        jdbcTemplate.execute("insert into oauth_client_details(client_id, identity_zone_id) values ('" + clientId + "', '" + IdentityZone.getUaaZoneId() + "')");
        ClientMetadata clientMetadata1 = createTestClientMetadata(
                clientId,
                true,
                new URL("http://app.launch/url"),
                base64EncodedImg,
                createdBy);
        jdbcClientMetadataProvisioning.update(clientMetadata1, IdentityZoneHolder.get().getId());
        String clientId2 = randomValueStringGenerator.generate();
        jdbcTemplate.execute("insert into oauth_client_details(client_id, identity_zone_id) values ('" + clientId2 + "', '" + IdentityZone.getUaaZoneId() + "')");
        ClientMetadata clientMetadata2 = createTestClientMetadata(
                clientId2,
                true,
                new URL("http://app.launch/url"),
                base64EncodedImg,
                createdBy);
        jdbcClientMetadataProvisioning.update(clientMetadata2, IdentityZoneHolder.get().getId());

        List<ClientMetadata> clientMetadatas = jdbcClientMetadataProvisioning.retrieveAll(IdentityZoneHolder.get().getId());


        assertThat(clientMetadatas, PredicateMatcher.has(m -> m.getClientId().equals(clientId)));
        assertThat(clientMetadatas, PredicateMatcher.has(m -> m.getClientId().equals(clientId2)));
    }

    @Test
    public void updateClientMetadata() throws Exception {
        String clientId = randomValueStringGenerator.generate();
        jdbcTemplate.execute("insert into oauth_client_details(client_id, identity_zone_id) values ('" + clientId + "', '" + IdentityZone.getUaaZoneId() + "')");
        ClientMetadata newClientMetadata = createTestClientMetadata(
                clientId,
                false,
                new URL("http://updated.app/launch/url"),
                base64EncodedImg,
                createdBy);

        ClientMetadata updatedClientMetadata = jdbcClientMetadataProvisioning.update(newClientMetadata, IdentityZoneHolder.get().getId());

        assertThat(updatedClientMetadata.getClientId(), is(clientId));
        assertThat(updatedClientMetadata.getIdentityZoneId(), is(IdentityZone.getUaaZoneId()));
        assertThat(updatedClientMetadata.isShowOnHomePage(), is(newClientMetadata.isShowOnHomePage()));
        assertThat(updatedClientMetadata.getAppLaunchUrl(), is(newClientMetadata.getAppLaunchUrl()));
        assertThat(updatedClientMetadata.getAppIcon(), is(newClientMetadata.getAppIcon()));
    }

    @Test
    public void testSetAndGetClientName() {
        String clientId = randomValueStringGenerator.generate();
        jdbcTemplate.execute("insert into oauth_client_details(client_id, identity_zone_id) values ('" + clientId + "', '" + IdentityZoneHolder.get().getId() + "')");
        ClientMetadata data = createTestClientMetadata(
                clientId,
                false,
                null,
                null,
                createdBy);
        String clientName = "clientName" + randomValueStringGenerator.generate();
        data.setClientName(clientName);
        jdbcClientMetadataProvisioning.update(data, IdentityZoneHolder.get().getId());
        data = jdbcClientMetadataProvisioning.retrieve(clientId, IdentityZoneHolder.get().getId());
        assertEquals(clientName, data.getClientName());
    }

    private static ClientMetadata createTestClientMetadata(
            final String clientId,
            final boolean showOnHomePage,
            final URL appLaunchUrl,
            final String appIcon,
            final String createdBy) {
        ClientMetadata clientMetadata = new ClientMetadata();
        clientMetadata.setClientId(clientId);
        clientMetadata.setShowOnHomePage(showOnHomePage);
        clientMetadata.setAppLaunchUrl(appLaunchUrl);
        clientMetadata.setAppIcon(appIcon);
        clientMetadata.setCreatedBy(createdBy);
        return clientMetadata;
    }

    private static final String base64EncodedImg = getResourceAsString(
            JdbcClientMetadataProvisioningTest.class,
            "base64EncodedImg");

}
