package com.github.idea.ginkgo.gotest;

import com.github.idea.ginkgo.GinkgoRunConfigurationOptions;
import com.github.idea.ginkgo.scope.GinkgoScope;
import com.goide.execution.testing.GoTestRunConfiguration;
import com.goide.execution.testing.frameworks.gotest.GotestRunConfigurationProducer;
import com.goide.psi.GoCallExpr;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GinkgoGoTestRunConfigurationProducer extends GotestRunConfigurationProducer {
    public static final String GINKGO = "Ginkgo";
    public static final String WHEN = "when";

    @Override
    protected boolean doSetupConfigurationFromContext(@NotNull GoTestRunConfiguration config, @NotNull ConfigurationContext context, @NotNull PsiElement contextElement) {
        List<String> specNames = getSpecNames(context);

        GinkgoRunConfigurationOptions ginkgoOptions = new GinkgoRunConfigurationOptions();
        ginkgoOptions.setWorkingDir(context.getPsiLocation().getContainingFile().getContainingDirectory().getVirtualFile().getPath());
        ginkgoOptions.setGinkgoScope(GinkgoScope.FOCUS);
        ginkgoOptions.setTestNames(specNames);
        ginkgoOptions.setFocusTestExpression(String.join(" ", specNames));

        config.setWorkingDirectory(ginkgoOptions.getWorkingDir());
        if (ginkgoOptions.getEnvData() != null) {
            config.setCustomEnvironment(ginkgoOptions.getEnvData().getEnvs());
        }
        config.setParams(String.format("--ginkgo.focus=\"%s\"", ginkgoOptions.getFocusTestExpression()));
        if (ginkgoOptions.getTestNames().isEmpty()) {
            config.setName("All Test");
        } else {
            config.setName(String.join(" ", ginkgoOptions.getTestNames()));
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

        return specTree.isEmpty() ? Arrays.asList(GINKGO) : new ArrayList<>(specTree);
    }
}
