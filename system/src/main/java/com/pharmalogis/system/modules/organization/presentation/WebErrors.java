package com.pharmalogis.system.modules.organization.presentation;

import com.pharmalogis.system.modules.organization.application.BusinessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(annotations = Controller.class)
public class WebErrors {
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    String business(BusinessException ex, Model m) {
        m.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    String invalid(Model m) {
        m.addAttribute("message", "Los filtros o identificadores no tienen un formato válido.");
        return "error";
    }
}
