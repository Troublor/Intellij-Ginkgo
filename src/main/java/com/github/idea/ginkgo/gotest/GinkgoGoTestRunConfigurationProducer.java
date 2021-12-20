package com.github.idea.ginkgo.gotest;

import com.goide.execution.GoBuildingRunConfiguration;
import com.goide.execution.testing.GoTestRunConfiguration;
import com.goide.execution.testing.frameworks.gotest.GotestRunConfigurationProducer;
import com.goide.psi.GoCallExpr;
import com.goide.psi.GoFile;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GinkgoGoTestRunConfigurationProducer extends GotestRunConfigurationProducer {
    public static final String WHEN = "when";

    @Override
    protected boolean doSetupConfigurationFromContext(@NotNull GoTestRunConfiguration config, @NotNull ConfigurationContext context, @NotNull PsiElement contextElement) {
        if (context.getPsiLocation() == null) {
            return false;
        }

        PsiFile file = context.getPsiLocation().getContainingFile();
        if (!(file instanceof GoFile)) {
            return false;
        }
        String importPath = ((GoFile) file).getImportPath(false);
        if (importPath == null) {
            return false;
        }

        List<String> specNames = getSpecNames(context);
        String testExpression = String.join(" ", specNames);
        String workingDir = file.getContainingDirectory().getVirtualFile().getPath();

        config.setKind(GoBuildingRunConfiguration.Kind.PACKAGE);
        config.setPackage(importPath);
        config.setWorkingDirectory(workingDir);
        if (!specNames.isEmpty()) {
            config.setParams(String.format("--ginkgo.focus=\"%s\"", testExpression));
        }
        if (specNames.isEmpty()) {
            config.setName("All Test");
        } else {
            config.setName(testExpression);
        }
        return true;
    }

    @Override
    protected boolean isConfigurationFromContext(@NotNull GoTestRunConfiguration configuration, @NotNull PsiElement contextElement, @NotNull ConfigurationContext context, boolean requireContext) {
        String arg = configuration.getParams();
        List<String> specNames = getSpecNames(context);
        return arg.contains(String.join(" ", specNames));
    }

    private List<String> getSpecNames(ConfigurationContext context) {
        Deque<String> specTree = new ArrayDeque<>();
        PsiElement location = context.getPsiLocation();
        while (location.getParent() != null) {
            location = location.getParent();
            if (location.getParent() instanceof GoCallExpr) {
                GoCallExpr parent = (GoCallExpr) location.getParent();
                specTree.push(parent.getArgumentList().getExpressionList().get(0).getText().replace("\"", ""));

                //Special case append when for When blocks
                if (parent.getExpression().getText().equalsIgnoreCase(WHEN)) {
                    specTree.push(WHEN);
                }
            }
        }

        return specTree.isEmpty() ? new ArrayList<>() : new ArrayList<>(specTree);
    }
}
