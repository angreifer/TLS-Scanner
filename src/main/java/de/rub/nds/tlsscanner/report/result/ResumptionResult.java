/**
 * TLS-Scanner - A TLS Configuration Analysistool based on TLS-Attacker
 *
 * Copyright 2017-2019 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsscanner.report.result;

import de.rub.nds.tlsscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.report.SiteReport;

/**
 *
 * @author robert
 */
public class ResumptionResult extends ProbeResult {

    private Boolean supportsResumption;

    public ResumptionResult(Boolean supportsResumption) {
        super(ProbeType.RESUMPTION);
        this.supportsResumption = supportsResumption;
    }

    @Override
    public void mergeData(SiteReport report) {
        report.setSupportsSessionIds(supportsResumption);
    }

}
