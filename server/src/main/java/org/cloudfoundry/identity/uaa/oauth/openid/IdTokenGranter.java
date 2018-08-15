package org.cloudfoundry.identity.uaa.oauth.openid;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

import static org.cloudfoundry.identity.uaa.oauth.token.TokenConstants.GRANT_TYPE_AUTHORIZATION_CODE;
import static org.cloudfoundry.identity.uaa.oauth.token.TokenConstants.GRANT_TYPE_CLIENT_CREDENTIALS;


public class IdTokenGranter {
    private static final String OPENID = "openid";
    public static final String REQUIRED_RESPONSE_TYPE = "id_token";

    public static boolean shouldSendIdToken(Collection<GrantedAuthority> clientScopes,
                                            Set<String> requestedScopes,
                                            String requestedGrantType,
                                            Set<String> requestedResponseTypes) {

        // TODO: this needs to consider user approvals / autoapproved scopes

        if (null == requestedResponseTypes) {
            return false;
        }

        // An id token may not be issued during client_credentials grants as
        // there is no user context
        if (GRANT_TYPE_CLIENT_CREDENTIALS.equals(requestedGrantType)) {
            return false;
        }

        if (GRANT_TYPE_AUTHORIZATION_CODE.equals(requestedGrantType) &&
                requestedResponseTypes.contains("code") &&
                requestedScopes != null &&
                requestedScopes.contains(OPENID)) {
            return true;
        }


        // An id token may not be issued unless the client configuration includes
        // the scope openid
        if (null == clientScopes) {
            return false;
        }
        if (clientScopes.stream()
                .filter(scope -> scope != null)
                .noneMatch(scope -> OPENID.equals(scope.getAuthority()))) {
            return false;
        }

        // If the requester specified the scope parameter in their /oauth/token request,
        // this list must contain openid.
        if (null != requestedScopes &&
            !requestedScopes.isEmpty() &&
            !requestedScopes.contains(OPENID)) {
            return false;
        }

        // Other than the isForceIdTokenCreation case above for authorization_code code flow,
        // an id token may not be issued unless id_token appears in the response types specified with
        // the response_type param.
        if (requestedResponseTypes
                .stream()
                .noneMatch(responseType -> REQUIRED_RESPONSE_TYPE.equals(responseType))) {
            return false;
        }

        return true;
    }
}