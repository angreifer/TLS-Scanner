/**
 * TLS-Scanner - A TLS Configuration Analysistool based on TLS-Attacker
 *
 * Copyright 2017-2019 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsscanner.probe.certificate;

public enum CertificateIssue {
    CHAIN_CONTAINS_EXPIRED("The certificate chain contains expired certificates"),
    CHAIN_CONTAINS_NOT_YET_VALID("The certificate chain contains not yet valid certificates"),
    NOT_IN_TRUST_STORE("The certificate trust anchor is not a commonly used CA"),
    CHAIN_NOT_COMPLETE("The certificate chain is not complete. The issuer of a certificate in the chain could not be found in our trust store nor is it provided by the server"),
    SELF_SIGNED("The certificate is self-signed. It is not signed by a commonly known CA"),
    INVALID_SIGNATURE("A certificate in the chain has an invalid signature"),
    FAILING_BASIC_CONSTRAINTS("A certificate in the chain is failing the basic constraints test. This means that the certificate was used for an operation (like signing another certificate), which it was not allowed to do"),
    COMMON_NAME_MISMATCH("Your server did not provide a certificate which is valid for the scanned domain"),
    REVOKED_CRL("A certificate in your chain is revoked (CRL)"),
    REVOKED_OCSP("A certificate in your chain is revoked (OCSP)"),
    WEAK_SIGNATURE_OR_HASH_ALGORITHM("A certificate in your chain has a weak signature or hash algorithm"),
    BLACKLISTED("A certificate in your chain is blacklisted"),
    UNKNOWN_CRITICAL_EXTENSION("A certificate in your chain has a criticial extension which we do not recognize"),
    DECODING_ERROR("A certificate in your chain could not be decoded by our scanner"),
    EMPTY_CHAIN("The server sent an empty certificate chain (weired)"),
    MULTIPLE_LEAFS("The server sent multiple leaf certifiates (weired)");

    private String humanReadable;

    private CertificateIssue(String humanReadable) {
        this.humanReadable = humanReadable;
    }

    public String getHumanReadable() {
        return humanReadable;
    }

}
