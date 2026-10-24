package com.smartbi.query;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CredentialResolver {
    private final Set<String> allowedNames;
    public CredentialResolver(@Value("${smartbi.credentials.allowed-environment-variables:}") String allowedNames) {
        this.allowedNames = Arrays.stream(allowedNames.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }
    public char[] resolve(String reference) {
        String name = validateReference(reference);
        String secret = System.getenv(name);
        if (secret == null || secret.isEmpty()) throw new QueryException("CREDENTIAL_UNAVAILABLE", "Credential is not available to the server");
        return secret.toCharArray();
    }
    public String validateReference(String reference) {
        if (reference == null || reference.isBlank()) throw new QueryException("CREDENTIAL_MISSING", "Credential reference is required");
        if (!reference.startsWith("env://")) throw new QueryException("CREDENTIAL_REFERENCE_INVALID", "Only env:// credential references are supported");
        String name = reference.substring(6);
        if (!name.matches("[A-Z][A-Z0-9_]{1,127}")) throw new QueryException("CREDENTIAL_REFERENCE_INVALID", "Invalid environment credential reference");
        if (!allowedNames.contains(name)) throw new QueryException("CREDENTIAL_REFERENCE_FORBIDDEN", "Credential reference is not allow-listed");
        return name;
    }
}
