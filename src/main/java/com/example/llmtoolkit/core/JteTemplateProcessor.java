package com.example.llmtoolkit.core;

import com.example.llmtoolkit.core.annotations.PP;
import com.example.llmtoolkit.core.annotations.PT;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class JteTemplateProcessor implements TemplateProcessor {

    private final TemplateEngine templateEngine;

    public static JteTemplateProcessor create() {
        return new JteTemplateProcessor();
    }

    private JteTemplateProcessor() {
        this.templateEngine = TemplateEngine.createPrecompiled(ContentType.Plain);
    }

    private String getTemplatePath(Method method) {
        PT promptAnnotation = method.getAnnotation(PT.class);
        if (promptAnnotation == null) {
            throw new IllegalStateException("Method must be annotated with @" + PT.class.getSimpleName());
        }
        return promptAnnotation.templatePath();
    }

    @Override
    public void validateTemplate(Method method) {
        String templatePath = getTemplatePath(method);
        Map<String, Class<?>> templateParams = templateEngine.getParamInfo(templatePath);
        if (templateParams == null) {
            throw new IllegalArgumentException("Template not found: " + templatePath);
        }

        Set<String> declaredParams = Arrays.stream(method.getParameters())
                .map(p -> p.getAnnotation(PP.class))
                .filter(Objects::nonNull)
                .map(PP::value)
                .collect(Collectors.toSet());

        if (declaredParams.size() != method.getParameterCount()) {
            throw new IllegalArgumentException("All parameters must be annotated with @PP");
        }

        Set<String> missingParams = new HashSet<>(templateParams.keySet());
        missingParams.removeAll(declaredParams);

        Set<String> extraParams = new HashSet<>(declaredParams);
        extraParams.removeAll(templateParams.keySet());

        if (!missingParams.isEmpty() || !extraParams.isEmpty()) {
            StringBuilder err = new StringBuilder("Template parameter mismatch for " + templatePath + ":");
            if (!missingParams.isEmpty()) {
                err.append("\n  Missing annotated parameters in method: ").append(String.join(", ", missingParams));
            }
            if (!extraParams.isEmpty()) {
                err.append("\n  Extra parameters in method: ").append(String.join(", ", extraParams));
            }
            throw new IllegalArgumentException(err.toString());
        }
    }

    @Override
    public String preparePrompt(Method method, Object[] args) {
        String templatePath = getTemplatePath(method);
        Map<String, Object> params = extractParameters(method, args);

        StringOutput output = new StringOutput();
        templateEngine.render(templatePath, params, output);
        return output.toString();
    }

    private Map<String, Object> extractParameters(Method method, Object[] args) {
        Map<String, Object> params = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PP paramAnnotation = parameters[i].getAnnotation(PP.class);
            if (paramAnnotation != null) {
                params.put(paramAnnotation.value(), args[i]);
            }
        }
        return params;
    }
}
