package eu.assuremoss;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import eu.assuremoss.framework.api.*;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.framework.modules.VulnRepairModules;
import eu.assuremoss.framework.modules.compiler.MavenPatchCompiler;
import eu.assuremoss.framework.modules.src.LocalSourceFolder;
import eu.assuremoss.utils.MLogger;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.Utils;
import eu.assuremoss.utils.factories.ToolFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static eu.assuremoss.utils.Configuration.*;


/**
 * The main driver class that runs the vulnerability repair workflow
 */
public class VulnRepairDriver {
    public static MLogger MLOG;
    private VulnRepairModules vulnRepairModules;
    private final Properties props;
    private int patchCounter = 1;
    private int vulnIndex = 0;
    private String currentTime;

    public VulnRepairDriver(Properties properties) throws IOException {
        this.props = properties;

        initResourceFiles(properties);
        initRepairModules();
        initLogger();
    }

    /**
     * Creates all resource files (directories, log files)
     * @param props - a properties object that specifies the creation path of the files
     */
    private void initResourceFiles(Properties props) {
        Utils.createDirectory(props.getProperty(RESULTS_PATH_KEY));
        Utils.createDirectory(props.getProperty(VALIDATION_RESULTS_PATH_KEY));
        Utils.createEmptyLogFile(props);
    }

    /**
     * Creates all the modules needed for the framework
     */
    private void initRepairModules() {
        vulnRepairModules = VulnRepairModules.builder()
                .scc(new LocalSourceFolder(props.getProperty(PROJECT_PATH_KEY)))
                .osa(ToolFactory.createOsa(props))
                .vulnRepairer(ToolFactory.createASGTransformRepair(props))
                .vulnDetector(ToolFactory.createOsa(props))
                .patchCompiler(new MavenPatchCompiler())
                .build();
    }

    /**
     * Creates logger to indicate the current status of the framework
     */
    private void initLogger() throws IOException {
        MLOG = new MLogger(props, "log.txt");
    }


    public void bootstrap() {
        MLOG.fInfo("Start!");

        saveCurrentTime();

        acquireSourceCode();

        List<CodeModel> codeModels = analyseSourceCode();

        List<VulnerabilityEntry> vulnerabilityLocations = locateVulnerabilities(codeModels);

        Map<String, List<JSONObject>> problemFixMap = new HashMap<>();

        repairCode(codeModels, vulnerabilityLocations, problemFixMap);

        generateVSCodeConfig(getVSCodeConfig(problemFixMap));

        archiveResults(currentTime);

        Utils.deleteIntermediatePatches(patchSavePath(props));
    }

    /**
     * Saves the current time for storing the names of the archived results
     */
    private void saveCurrentTime() {
        currentTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
    }

    /**
     * Acquire the source code that will be analysed by the framework
     */
    private void acquireSourceCode() {
        MLOG.info("Project source acquiring started");
        vulnRepairModules.scc.collectSourceCode();
    }

    /**
     * Analyse the acquired source code in order to
     */
    private List<CodeModel> analyseSourceCode() {
        MLOG.info("Code analysis started");
        return vulnRepairModules.osa.analyzeSourceCode(vulnRepairModules.scc.getSourceCodeLocation(), false);
    }

    private List<VulnerabilityEntry> locateVulnerabilities(List<CodeModel> codeModels) {
        List<VulnerabilityEntry> vulnerabilityLocations = vulnRepairModules.vulnDetector.getVulnerabilityLocations(vulnRepairModules.scc.getSourceCodeLocation(), codeModels);

        MLOG.info(String.format("Detected %d vulnerabilities", vulnerabilityLocations.size()));
        MLOG.logVulnerabilities(vulnerabilityLocations);

        return vulnerabilityLocations;
    }

    private void repairCode(List<CodeModel> codeModels, List<VulnerabilityEntry> vulnerabilityLocations, Map<String, List<JSONObject>> problemFixMap) {
        vulnerabilityLocations.forEach(vulnEntry -> {
            vulnIndex++;
            List<Pair<File, Pair<Patch<String>, String>>> repairPatches = generateRepairPatches(codeModels, vulnEntry, vulnerabilityLocations.size());
            List<Pair<File, Pair<Patch<String>, String>>> filteredPatches = applyAndCompilePatches(repairPatches, vulnerabilityLocations.size());
            List<Pair<File, Pair<Patch<String>, String>>> candidatePatches = getCandidatePatches(vulnEntry, filteredPatches, vulnerabilityLocations.size());
            savePatches(vulnerabilityLocations, problemFixMap, vulnEntry, candidatePatches);
        });
    }

    private List<Pair<File, Pair<Patch<String>, String>>> generateRepairPatches(List<CodeModel> codeModels, VulnerabilityEntry vulnEntry, int numberOfVulnerabilities) {
        MLOG.ninfo(String.format("Generating patches for %d/%d vulnerability", vulnIndex, numberOfVulnerabilities));
        return vulnRepairModules.vulnRepairer.generateRepairPatches(vulnRepairModules.scc.getSourceCodeLocation(), vulnEntry, codeModels);
    }

    private List<Pair<File, Pair<Patch<String>, String>>> applyAndCompilePatches(List<Pair<File, Pair<Patch<String>, String>>> patches, int numberOfVulnerabilities) {
        MLOG.info(String.format("Compiling patches for %d/%d vulnerability", vulnIndex, numberOfVulnerabilities));
        return vulnRepairModules.patchCompiler.applyAndCompile(vulnRepairModules.scc.getSourceCodeLocation(), patches, true);
    }

    private List<Pair<File, Pair<Patch<String>, String>>> getCandidatePatches(VulnerabilityEntry vulnEntry, List<Pair<File, Pair<Patch<String>, String>>> filteredPatches, int numberOfVulnerabilities) {
        MLOG.info(String.format("Verifying patches for %d/%d vulnerability", vulnIndex, numberOfVulnerabilities));

        List<Pair<File, Pair<Patch<String>, String>>> candidatePatches = new ArrayList<>();
        PatchValidator patchValidator = ToolFactory.createOsa(props);

        for (Pair<File, Pair<Patch<String>, String>> patchWithExplanation : filteredPatches) {
            Patch<String> rawPatch = patchWithExplanation.getB().getA();
            Pair<File, Patch<String>> patch = new Pair<>(patchWithExplanation.getA(), rawPatch);

            vulnRepairModules.patchCompiler.applyPatch(patch, vulnRepairModules.scc.getSourceCodeLocation());
            if (patchValidator.validatePatch(vulnRepairModules.scc.getSourceCodeLocation(), vulnEntry, patch)) {
                candidatePatches.add(patchWithExplanation);
            }
            vulnRepairModules.patchCompiler.revertPatch(patch, vulnRepairModules.scc.getSourceCodeLocation());
        }

        return candidatePatches;
    }

    private void savePatches(List<VulnerabilityEntry> vulnerabilityLocations, Map<String, List<JSONObject>> problemFixMap, VulnerabilityEntry vulnEntry, List<Pair<File, Pair<Patch<String>, String>>> candidatePatches) {
        Utils.createDirectory(patchSavePath(props));
        if (candidatePatches.isEmpty()) {
            MLOG.info("No patch candidates were found, skipping!");
            return;
        }

        MLOG.info(String.format("Writing out patch candidates patches for %d/%d vulnerability", vulnIndex, vulnerabilityLocations.size()));
        if (!problemFixMap.containsKey(vulnEntry.getType())) {
            problemFixMap.put(vulnEntry.getType(), new ArrayList());
        }
        problemFixMap.get(vulnEntry.getType()).add(generateFixEntity(vulnEntry, candidatePatches));
    }

    private void generateVSCodeConfig(JSONObject vsCodeConfig) {
        try (FileWriter fw = new FileWriter(String.valueOf(Paths.get(patchSavePath(props), "vscode-config.json")))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement element = JsonParser.parseString(vsCodeConfig.toJSONString());
            fw.write(gson.toJson(element));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getVSCodeConfig(Map<String, List<JSONObject>> problemFixMap) {
        JSONObject vsCodeConfig = new JSONObject();
        for (String problemType : problemFixMap.keySet()) {
            JSONArray fixesArray = new JSONArray();
            fixesArray.addAll(problemFixMap.get(problemType));
            vsCodeConfig.put(problemType, fixesArray);
        }

        return vsCodeConfig;
    }

    private JSONObject generateFixEntity(VulnerabilityEntry vulnEntry, List<Pair<File, Pair<Patch<String>, String>>> candidatePatches) {
        JSONArray patchesArray = new JSONArray();
        JSONObject issueObject = new JSONObject();
        for (int i = 0; i < candidatePatches.size(); i++) {
            File path = candidatePatches.get(i).getA();
            Patch<String> patch = candidatePatches.get(i).getB().getA();
            String explanation = candidatePatches.get(i).getB().getB();

            // Dump the patch and generate the necessary meta-info json as well with vulnerability/patch candidate mapping for the VS Code plug-in
            String patchName = MessageFormat.format("patch_{0}_{1}_{2}_{3}_{4}_{5}.diff", patchCounter++, vulnEntry.getType(), vulnEntry.getStartLine(), vulnEntry.getEndLine(), vulnEntry.getStartCol(), vulnEntry.getEndCol());
            try (PrintWriter patchWriter = new PrintWriter(String.valueOf(Paths.get(patchSavePath(props), patchName)))) {
                List<String> unifiedDiff =
                        UnifiedDiffUtils.generateUnifiedDiff(path.getPath(), path.getPath(),
                                Arrays.asList(Files.readString(Path.of(path.getAbsolutePath())).split("\n")), patch, 2);

                // make the path in the patch file relative to the project path
                for (int j = 0; j < 2; j++) {
                    String line = unifiedDiff.get(j);

                    String regex = props.getProperty(PROJECT_PATH_KEY);
                    regex = regex.replaceAll("\\\\", "\\\\\\\\");

                    String[] lineParts = line.split(regex);
                    if (lineParts[1].charAt(0) == '\\' || lineParts[1].charAt(0) == '/') {
                        lineParts[1] = lineParts[1].substring(1);
                    }

                    unifiedDiff.set(j, lineParts[0] + lineParts[1]);
                }

                String diffString = Joiner.on("\n").join(unifiedDiff) + "\n";
                patchWriter.write(diffString);

                JSONObject patchObject = new JSONObject();
                patchObject.put("path", patchName);
                patchObject.put("explanation", explanation);
                patchObject.put("score", 10);
                patchesArray.add(patchObject);
            } catch (IOException e) {
                MLOG.info("Failed to save candidate patch: " + patch);
            }
        }

        JSONObject textRangeObject = new JSONObject();
        textRangeObject.put("startLine", vulnEntry.getStartLine());
        textRangeObject.put("endLine", vulnEntry.getEndLine());
        textRangeObject.put("startColumn", vulnEntry.getStartCol() - 1);
        textRangeObject.put("endColumn", vulnEntry.getEndCol() - 1);

        issueObject.put("patches", patchesArray);
        issueObject.put("textRange", textRangeObject);

        return issueObject;
    }

    private void archiveResults(String currentTime) {
        if (archiveEnabled(props)) {
            Utils.archiveResults(patchSavePath(props), props.getProperty(ARCHIVE_PATH), descriptionPath(props), currentTime);
        }
    }

}
