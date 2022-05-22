package eu.assuremoss.framework.modules;

import eu.assuremoss.framework.api.PatchCompiler;
import eu.assuremoss.framework.api.SourceCodeCollector;
import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.modules.analyzer.OpenStaticAnalyzer;
import eu.assuremoss.framework.modules.repair.ASGTransformRepair;
import lombok.Builder;

@Builder
public class VulnRepairModules {
    public SourceCodeCollector scc;
    public OpenStaticAnalyzer osa;
    public ASGTransformRepair vulnRepairer;
    public VulnerabilityDetector vulnDetector;
    public PatchCompiler patchCompiler;
}
