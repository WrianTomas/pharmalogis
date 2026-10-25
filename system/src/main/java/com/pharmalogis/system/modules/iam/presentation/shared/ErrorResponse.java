package com.pharmalogis.system.modules.iam.presentation.shared;

import java.util.Map;

public record ErrorResponse(int code, String error, String message, Map<String, String> details) {
}
