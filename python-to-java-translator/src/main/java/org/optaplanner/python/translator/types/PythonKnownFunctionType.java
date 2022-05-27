package org.optaplanner.python.translator.types;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.optaplanner.python.translator.PythonFunctionSignature;

public class PythonKnownFunctionType extends PythonLikeType {
    final List<PythonFunctionSignature> overloadFunctionSignatureList;

    public PythonKnownFunctionType(String methodName, List<PythonFunctionSignature> overloadFunctionSignatureList) {
        super("function-" + methodName, PythonKnownFunctionType.class, List.of(FUNCTION_TYPE));
        this.overloadFunctionSignatureList = overloadFunctionSignatureList;
    }

    public List<PythonFunctionSignature> getOverloadFunctionSignatureList() {
        return overloadFunctionSignatureList;
    }

    public Optional<PythonFunctionSignature> getFunctionForParameters(PythonLikeType... parameters) {
        List<PythonFunctionSignature> matchingOverloads = overloadFunctionSignatureList.stream()
                .filter(signature -> signature.matchesParameters(parameters))
                .collect(Collectors.toList());

        if (matchingOverloads.isEmpty()) {
            return Optional.empty();
        }

        PythonFunctionSignature best = matchingOverloads.get(0);
        for (PythonFunctionSignature signature : matchingOverloads) {
            if (signature.moreSpecificThan(best)) {
                best = signature;
            }
        }
        return Optional.of(best);
    }
}
