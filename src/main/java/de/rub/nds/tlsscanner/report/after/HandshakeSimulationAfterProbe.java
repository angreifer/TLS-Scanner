/**
 * TLS-Scanner - A TLS Configuration Analysistool based on TLS-Attacker
 *
 * Copyright 2017-2019 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsscanner.report.after;

import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.CompressionMethod;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsscanner.constants.CipherSuiteGrade;
import de.rub.nds.tlsscanner.probe.handshakeSimulation.HandshakeFailureReasons;
import de.rub.nds.tlsscanner.probe.handshakeSimulation.ConnectionInsecure;
import de.rub.nds.tlsscanner.probe.handshakeSimulation.SimulatedClientResult;
import de.rub.nds.tlsscanner.report.CiphersuiteRater;
import de.rub.nds.tlsscanner.report.SiteReport;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class HandshakeSimulationAfterProbe extends AfterProbe {

    @Override
    public void analyze(SiteReport report) {
        int isSuccessfulCounter = 0;
        int isInsecureCounter = 0;
        if (report.getSimulatedClientList() != null) {
            for (SimulatedClientResult simulatedClient : report.getSimulatedClientList()) {
                if (simulatedClient.getReceivedAlert()) {
                    checkWhyAlert(report, simulatedClient);
                } else if (simulatedClient.getReceivedAllMandatoryMessages()) {
                    checkSelectedProtocolVersion(report, simulatedClient);
                    checkIfHandshakeWouldBeSuccessful(simulatedClient);
                    if (simulatedClient.getFailReasons().isEmpty()) {
                        simulatedClient.setHandshakeSuccessful(true);
                    }
                } else {
                    checkWhyMandatoryMessagesMissing(simulatedClient);
                }
                if (Objects.equals(simulatedClient.getHandshakeSuccessful(), Boolean.TRUE)) {
                    isSuccessfulCounter++;
                    checkIfConnectionIsInsecure(report, simulatedClient);
                    if (simulatedClient.getInsecureReasons().isEmpty()) {
                        simulatedClient.setConnectionInsecure(false);
                        checkIfConnectionIsRfc7918Secure(simulatedClient);
                    } else {
                        simulatedClient.setConnectionInsecure(true);
                        isInsecureCounter++;
                    }
                } else {
                    simulatedClient.setHandshakeSuccessful(false);
                }
            }
            report.setHandshakeSuccessfulCounter(isSuccessfulCounter);
            report.setHandshakeFailedCounter(report.getSimulatedClientList().size() - isSuccessfulCounter);
            report.setConnectionInsecureCounter(isInsecureCounter);
        }
    }

    private void checkWhyAlert(SiteReport report, SimulatedClientResult simulatedClient) {
        if (isCiphersuiteMismatch(report, simulatedClient)) {
            simulatedClient.addToFailReasons(HandshakeFailureReasons.CIPHERSUITE_MISMATCH);
        }
    }

    private boolean isCiphersuiteMismatch(SiteReport report, SimulatedClientResult simulatedClient) {
        if (report.getCipherSuites() != null) {
            for (CipherSuite serverCipherSuite : report.getCipherSuites()) {
                for (CipherSuite clientCipherSuite : simulatedClient.getClientSupportedCiphersuites()) {
                    if (serverCipherSuite.equals(clientCipherSuite)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void checkSelectedProtocolVersion(SiteReport report, SimulatedClientResult simulatedClient) {
        if (report.getVersions() != null && simulatedClient.getSupportedVersionList() != null) {
            List<ProtocolVersion> commonProtocolVersions = new LinkedList<>();
            Collections.sort(report.getVersions());
            Collections.sort(simulatedClient.getSupportedVersionList());
            for (ProtocolVersion serverVersion : report.getVersions()) {
                if (simulatedClient.getSupportedVersionList().contains(serverVersion)) {
                    commonProtocolVersions.add(serverVersion);
                }
            }
            Collections.sort(commonProtocolVersions);
            simulatedClient.setCommonProtocolVersions(commonProtocolVersions);
            if (!commonProtocolVersions.isEmpty()
                    && commonProtocolVersions.get(commonProtocolVersions.size() - 1).equals(simulatedClient.getSelectedProtocolVersion())) {
                simulatedClient.setHighestPossibleProtocolVersionSeleceted(true);
            } else {
                simulatedClient.setHighestPossibleProtocolVersionSeleceted(false);
            }
        }
    }

    private void checkIfHandshakeWouldBeSuccessful(SimulatedClientResult simulatedClient) {
        if (isProtocolMismatch(simulatedClient)) {
            simulatedClient.addToFailReasons(HandshakeFailureReasons.PROTOCOL_MISMATCH);
        }
        if (isCiphersuiteForbidden(simulatedClient)) {
            simulatedClient.addToFailReasons(HandshakeFailureReasons.CIPHERSUITE_FORBIDDEN);
        }
        if (isPublicKeyLengthRsaNotAccepted(simulatedClient)) {
            simulatedClient.addToFailReasons(HandshakeFailureReasons.RSA_CERTIFICATE_MODULUS_SIZE_NOT_ACCEPTED);
        }
        if (isPublicKeyLengthDhNotAccepted(simulatedClient)) {
            simulatedClient.addToFailReasons(HandshakeFailureReasons.DHE_MODULUS_SIZE_NOT_ACCEPTED);
        }
    }

    private boolean isProtocolMismatch(SimulatedClientResult simulatedClient) {
        return simulatedClient.getCommonProtocolVersions() != null && simulatedClient.getCommonProtocolVersions().isEmpty();
    }

    private boolean isCiphersuiteForbidden(SimulatedClientResult simulatedClient) {
        if (simulatedClient.getSelectedCiphersuite().isSupportedInProtocol(simulatedClient.getSelectedProtocolVersion())) {
            return false;
        } else if (simulatedClient.getVersionAcceptForbiddenCiphersuiteList() != null
                && simulatedClient.getVersionAcceptForbiddenCiphersuiteList().contains(simulatedClient.getSelectedProtocolVersion())) {
            return false;
        }
        return true;
    }

    private boolean isPublicKeyLengthRsaNotAccepted(SimulatedClientResult simulatedClient) {
        List<Integer> supportedKeyLengths;
        Integer publicKeyLength = simulatedClient.getServerPublicKeyParameter();
        if (simulatedClient.getKeyExchangeAlgorithm().isKeyExchangeRsa() && simulatedClient.getSupportedRsaKeySizeList() != null) {
            supportedKeyLengths = simulatedClient.getSupportedRsaKeySizeList();
            if (publicKeyLength < supportedKeyLengths.get(0)
                    || supportedKeyLengths.get(supportedKeyLengths.size() - 1) < publicKeyLength) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicKeyLengthDhNotAccepted(SimulatedClientResult simulatedClient) {
        List<Integer> supportedKeyLengths;
        Integer publicKeyLength = simulatedClient.getServerPublicKeyParameter();
        if (simulatedClient.getKeyExchangeAlgorithm().isKeyExchangeDh() && simulatedClient.getSupportedDheKeySizeList() != null) {
            supportedKeyLengths = simulatedClient.getSupportedDheKeySizeList();
            if (publicKeyLength < supportedKeyLengths.get(0)
                    || supportedKeyLengths.get(supportedKeyLengths.size() - 1) < publicKeyLength) {
                return true;
            }
        }
        return false;
    }

    private void checkWhyMandatoryMessagesMissing(SimulatedClientResult simulatedClient) {
        if (isParsingError(simulatedClient)) {
            simulatedClient.addToFailReasons(HandshakeFailureReasons.PARSING_ERROR);
        }
    }

    private boolean isParsingError(SimulatedClientResult simulatedClient) {
        return simulatedClient.getReceivedUnknown();
    }

    private void checkIfConnectionIsInsecure(SiteReport report, SimulatedClientResult simulatedClient) {
        if (simulatedClient.getSelectedCiphersuite() != null && isCipherSuiteGradeLow(simulatedClient)) {
            simulatedClient.addToInsecureReasons(ConnectionInsecure.CIPHERSUITE_GRADE_LOW.getReason());
        }
        checkVulnerabilities(report, simulatedClient);
        checkPublicKeySize(simulatedClient);
    }

    private boolean isCipherSuiteGradeLow(SimulatedClientResult simulatedClient) {
        return CiphersuiteRater.getGrade(simulatedClient.getSelectedCiphersuite()).equals(CipherSuiteGrade.LOW);
    }

    private void checkVulnerabilities(SiteReport report, SimulatedClientResult simulatedClient) {
        CipherSuite cipherSuite = simulatedClient.getSelectedCiphersuite();
        if (report.getPaddingOracleVulnerable() != null && report.getPaddingOracleVulnerable()
                && cipherSuite.isCBC()) {
            simulatedClient.addToInsecureReasons(ConnectionInsecure.PADDING_ORACLE.getReason());
        }
        if (report.getBleichenbacherVulnerable() != null && report.getBleichenbacherVulnerable()
                && simulatedClient.getKeyExchangeAlgorithm().isKeyExchangeRsa()) {
            simulatedClient.addToInsecureReasons(ConnectionInsecure.BLEICHENBACHER.getReason());
        }
        if (simulatedClient.getSelectedCompressionMethod() != CompressionMethod.NULL) {
            simulatedClient.addToInsecureReasons(ConnectionInsecure.CRIME.getReason());
        }
        if (report.getSweet32Vulnerable() != null && report.getSweet32Vulnerable()) {
            if (cipherSuite.name().contains("3DES")
                    || cipherSuite.name().contains("IDEA")
                    || cipherSuite.name().contains("GOST")) {
                simulatedClient.addToInsecureReasons(ConnectionInsecure.SWEET32.getReason());
            }
        }
    }

    private void checkPublicKeySize(SimulatedClientResult simulatedClient) {
        Integer pubKey = simulatedClient.getServerPublicKeyParameter();
        Integer minRsa = 1024;
        Integer minDh = 1024;
        Integer minEcdh = 160;
        if (simulatedClient.getKeyExchangeAlgorithm().isKeyExchangeRsa() && pubKey <= minRsa) {
            simulatedClient.addToInsecureReasons(ConnectionInsecure.PUBLIC_KEY_SIZE_TOO_SMALL.getReason() + " - rsa > " + minRsa);
        } else if (simulatedClient.getKeyExchangeAlgorithm().isKeyExchangeDh() && pubKey <= minDh) {
            simulatedClient.addToInsecureReasons(ConnectionInsecure.PUBLIC_KEY_SIZE_TOO_SMALL.getReason() + " - dh > " + minDh);
        } else if (simulatedClient.getKeyExchangeAlgorithm().isKeyExchangeEcdh() && pubKey <= minEcdh) {
            simulatedClient.addToInsecureReasons(ConnectionInsecure.PUBLIC_KEY_SIZE_TOO_SMALL.getReason() + " - ecdh > " + minEcdh);
        }
    }

    private void checkIfConnectionIsRfc7918Secure(SimulatedClientResult simulatedClient) {
        boolean isRfc7918Secure = false;
        CipherSuite cipherSuite = simulatedClient.getSelectedCiphersuite();
        Integer pubKey = simulatedClient.getServerPublicKeyParameter();
        if (cipherSuite != null && pubKey != null) {
            if (isProtocolVersionWhitelisted(simulatedClient)
                    && isSymmetricCipherRfc7918Whitelisted(cipherSuite)
                    && isKeyExchangeMethodWhitelisted(simulatedClient)
                    && isKeyLengthWhitelisted(simulatedClient, pubKey)) {
                isRfc7918Secure = true;
            }
        }
        simulatedClient.setConnectionRfc7918Secure(isRfc7918Secure);
    }

    private boolean isProtocolVersionWhitelisted(SimulatedClientResult simulatedClient) {
        return Objects.equals(simulatedClient.getHighestPossibleProtocolVersionSeleceted(), Boolean.TRUE)
                && simulatedClient.getSelectedProtocolVersion() != ProtocolVersion.TLS10
                && simulatedClient.getSelectedProtocolVersion() != ProtocolVersion.TLS11;
    }

    private boolean isSymmetricCipherRfc7918Whitelisted(CipherSuite cipherSuite) {
        return cipherSuite.isGCM() || cipherSuite.isChachaPoly();
    }

    private boolean isKeyExchangeMethodWhitelisted(SimulatedClientResult simulatedClient) {
        switch (simulatedClient.getKeyExchangeAlgorithm()) {
            case DHE_DSS:
            case DHE_RSA:
            case ECDHE_ECDSA:
            case ECDHE_RSA:
                return true;
            default:
                return false;
        }
    }

    private boolean isKeyLengthWhitelisted(SimulatedClientResult simulatedClient, Integer keyLength) {
        if (simulatedClient.getKeyExchangeAlgorithm().isKeyExchangeEcdh() && simulatedClient.getSelectedCiphersuite().isEphemeral()) {
            if (keyLength >= 3072) {
                return true;
            }
        }
        if (simulatedClient.getKeyExchangeAlgorithm().isKeyExchangeEcdh() && simulatedClient.getSelectedCiphersuite().isEphemeral()) {
            if (keyLength >= 256) {
                return true;
            }
        }
        return false;
    }
}
