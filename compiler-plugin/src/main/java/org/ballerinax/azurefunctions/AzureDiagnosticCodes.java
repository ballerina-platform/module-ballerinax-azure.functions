/*
 * Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinax.azurefunctions;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import static io.ballerina.tools.diagnostics.DiagnosticSeverity.ERROR;
import static io.ballerina.tools.diagnostics.DiagnosticSeverity.WARNING;

/**
 * {@code DiagnosticCodes} is used to hold diagnostic codes.
 */
public enum AzureDiagnosticCodes {
    AF_001("AF_001", "invalid annotation type on param '%s'", ERROR),
    AF_002("AF_002", "invalid resource parameter '%s'", ERROR),
    AF_003("AF_003", "invalid type of header param '%s': One of the following types is expected: " +
            "'string','int','float','decimal','boolean', an array of the above types or a record which consists of " +
            "the above types", ERROR),
    AF_004("AF_004", "invalid union type of header param '%s': one of the 'string','int','float'," +
            "'decimal','boolean' types, an array of the above types or a record which consists of the above types can" +
            " only be union with '()'. Eg: string|() or string[]|()", ERROR),
    AF_005("AF_005", "invalid intersection type : '%s'. Only readonly type is allowed", ERROR),
    AF_006("AF_006", "rest fields are not allowed for header binding records. Use 'http:Headers' type to access " +
            "all headers", ERROR),
    AF_007("AF_007", "invalid multiple resource parameter annotations for '%s'", ERROR),
    AF_008("AF_008", "'treatNilableAsOptional' is the only @http:serviceConfig field supported by Azure " +
            "Function at the moment", WARNING),
    AF_009("AF_009", "invalid return type for '%s'", ERROR),
    AF_010("AF_010", "invalid type of query param '%s': expected one of the 'string', 'int', 'float', " +
            "'boolean', 'decimal', 'map<json>' types or the array types of them", ERROR),
    AF_011("AF_011", "invalid union type of query param '%s': 'string', 'int', 'float', 'boolean', " +
            "'decimal', 'map<json>' type or the array types of them can only be union with '()'. Eg: string? or int[]?",
            ERROR);
    private final String code;
    private final String message;
    private final DiagnosticSeverity severity;

    AzureDiagnosticCodes(String code, String message, DiagnosticSeverity severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public DiagnosticSeverity getSeverity() {
        return severity;
    }
}
